package com.midnight.assistant.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Every voice/text exchange belongs to a "session" (one conversation thread). Sessions are
 * stored as plain JSON files under filesDir/chat_sessions/ so the whole feature works with
 * zero extra dependencies (no Room, no SQLite boilerplate):
 *
 *   chat_sessions/index.json      -> { currentSessionId, sessions: [ {id,title,...}, ... ] }
 *   chat_sessions/<sessionId>.json -> [ {id,role,text,timestamp}, ... ]
 *
 * All calls are suspend + IO-dispatched; callers (ChatViewModel) treat this as the single
 * source of truth for "what conversation am I in and what's in it".
 */
class ChatHistoryStore(context: Context) {

    private val rootDir: File = File(context.filesDir, "chat_sessions").apply { mkdirs() }
    private val indexFile = File(rootDir, "index.json")

    /** Returns the session the app should resume into (last-used session, or a fresh one). */
    suspend fun getOrCreateCurrentSessionId(): String = withContext(Dispatchers.IO) {
        val index = readIndex()
        val existing = index.optString("currentSessionId").takeIf { it.isNotBlank() }
        if (existing != null) return@withContext existing
        val newId = UUID.randomUUID().toString()
        index.put("currentSessionId", newId)
        upsertSessionMeta(index, newId, title = "New conversation", messageCount = 0, bumpUpdatedAt = true)
        writeIndex(index)
        newId
    }

    suspend fun setCurrentSession(sessionId: String) = withContext(Dispatchers.IO) {
        val index = readIndex()
        index.put("currentSessionId", sessionId)
        writeIndex(index)
        Unit
    }

    suspend fun startNewSession(): String = withContext(Dispatchers.IO) {
        val index = readIndex()
        val newId = UUID.randomUUID().toString()
        index.put("currentSessionId", newId)
        upsertSessionMeta(index, newId, title = "New conversation", messageCount = 0, bumpUpdatedAt = true)
        writeIndex(index)
        newId
    }

    suspend fun loadMessages(sessionId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        val file = File(rootDir, "$sessionId.json")
        if (!file.exists()) return@withContext emptyList()
        try {
            val array = JSONArray(file.readText())
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val roleStr = obj.optString("role")
                val role = when (roleStr) {
                    "USER" -> Role.USER
                    "ASSISTANT" -> Role.ASSISTANT
                    else -> Role.SYSTEM
                }
                ChatMessage(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    role = role,
                    text = obj.optString("text"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    suspend fun saveMessages(sessionId: String, messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        messages.forEach { msg ->
            array.put(
                JSONObject().apply {
                    put("id", msg.id)
                    put("role", msg.role.name)
                    put("text", msg.text)
                    put("timestamp", msg.timestamp)
                }
            )
        }
        File(rootDir, "$sessionId.json").writeText(array.toString())

        val index = readIndex()
        val firstUserMessage = messages.firstOrNull { it.role == Role.USER }?.text
        val title = firstUserMessage?.take(48)?.let { if (it.length == 48) "$it…" else it }
            ?: "New conversation"
        upsertSessionMeta(index, sessionId, title = title, messageCount = messages.size, bumpUpdatedAt = true)
        writeIndex(index)
        Unit
    }

    suspend fun listSessions(): List<ChatSessionMeta> = withContext(Dispatchers.IO) {
        val index = readIndex()
        val sessions = index.optJSONArray("sessions") ?: JSONArray()
        (0 until sessions.length()).mapNotNull { i ->
            val obj = sessions.optJSONObject(i) ?: return@mapNotNull null
            val id = obj.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (obj.optInt("messageCount", 0) <= 0) return@mapNotNull null
            ChatSessionMeta(
                id = id,
                title = obj.optString("title", "Conversation"),
                createdAt = obj.optLong("createdAt", 0L),
                updatedAt = obj.optLong("updatedAt", 0L),
                messageCount = obj.optInt("messageCount", 0),
                preview = obj.optString("preview", "")
            )
        }.sortedByDescending { it.updatedAt }
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        File(rootDir, "$sessionId.json").delete()
        val index = readIndex()
        val sessions = index.optJSONArray("sessions") ?: JSONArray()
        val kept = JSONArray()
        for (i in 0 until sessions.length()) {
            val obj = sessions.optJSONObject(i) ?: continue
            if (obj.optString("id") != sessionId) kept.put(obj)
        }
        index.put("sessions", kept)
        writeIndex(index)
        Unit
    }

    /** Bundles every conversation (including the currently-open one) into a single JSON
     *  document suitable for writing to a user-picked file via Storage Access Framework. */
    suspend fun exportAllToJson(): String = withContext(Dispatchers.IO) {
        val index = readIndex()
        val sessionMetas = index.optJSONArray("sessions") ?: JSONArray()
        val exportSessions = JSONArray()

        for (i in 0 until sessionMetas.length()) {
            val meta = sessionMetas.optJSONObject(i) ?: continue
            val id = meta.optString("id").takeIf { it.isNotBlank() } ?: continue
            val messagesFile = File(rootDir, "$id.json")
            val messagesArray = if (messagesFile.exists()) {
                try {
                    JSONArray(messagesFile.readText())
                } catch (t: Throwable) {
                    JSONArray()
                }
            } else {
                JSONArray()
            }
            exportSessions.put(
                JSONObject().apply {
                    put("id", id)
                    put("title", meta.optString("title", "Conversation"))
                    put("createdAt", meta.optLong("createdAt", System.currentTimeMillis()))
                    put("updatedAt", meta.optLong("updatedAt", System.currentTimeMillis()))
                    put("messages", messagesArray)
                }
            )
        }

        JSONObject().apply {
            put("app", "Solace")
            put("exportFormatVersion", 1)
            put("exportedAt", System.currentTimeMillis())
            put("sessions", exportSessions)
        }.toString(2)
    }

    /** Imports a JSON document produced by [exportAllToJson]. Imported sessions get fresh
     *  ids so they're added alongside existing history rather than overwriting it. Returns
     *  the number of sessions imported. */
    suspend fun importAllFromJson(json: String): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(json)
        val sessions = root.optJSONArray("sessions") ?: return@withContext 0
        val index = readIndex()
        var imported = 0

        for (i in 0 until sessions.length()) {
            val session = sessions.optJSONObject(i) ?: continue
            val messages = session.optJSONArray("messages") ?: JSONArray()
            if (messages.length() == 0) continue

            val newId = UUID.randomUUID().toString()
            File(rootDir, "$newId.json").writeText(messages.toString())

            val originalTitle = session.optString("title", "Imported conversation")
            upsertSessionMeta(
                index,
                newId,
                title = if (originalTitle.endsWith("(imported)")) originalTitle else "$originalTitle (imported)",
                messageCount = messages.length(),
                bumpUpdatedAt = true
            )
            imported++
        }

        if (imported > 0) {
            writeIndex(index)
        }
        imported
    }

    // ---- internal helpers ----

    private fun readIndex(): JSONObject {
        if (!indexFile.exists()) return JSONObject().put("sessions", JSONArray())
        return try {
            JSONObject(indexFile.readText())
        } catch (t: Throwable) {
            JSONObject().put("sessions", JSONArray())
        }
    }

    private fun writeIndex(index: JSONObject) {
        indexFile.writeText(index.toString())
    }

    private fun upsertSessionMeta(
        index: JSONObject,
        sessionId: String,
        title: String,
        messageCount: Int,
        bumpUpdatedAt: Boolean
    ) {
        val sessions = index.optJSONArray("sessions") ?: JSONArray().also { index.put("sessions", it) }
        val now = System.currentTimeMillis()
        var found = false
        for (i in 0 until sessions.length()) {
            val obj = sessions.optJSONObject(i) ?: continue
            if (obj.optString("id") == sessionId) {
                obj.put("title", title)
                obj.put("messageCount", messageCount)
                if (bumpUpdatedAt) obj.put("updatedAt", now)
                if (!obj.has("createdAt")) obj.put("createdAt", now)
                found = true
                break
            }
        }
        if (!found) {
            sessions.put(
                JSONObject().apply {
                    put("id", sessionId)
                    put("title", title)
                    put("createdAt", now)
                    put("updatedAt", now)
                    put("messageCount", messageCount)
                }
            )
        }
    }
}
