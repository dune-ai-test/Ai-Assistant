package com.midnight.assistant.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin client for Kilo Gateway (https://kilo.ai/gateway), an OpenAI-compatible
 * routing layer in front of 500+ models. Two endpoints are used:
 *   GET  {baseUrl}/models             -> list available models
 *   POST {baseUrl}/chat/completions   -> standard OpenAI chat-completions body
 * Auth is a plain bearer token: "Authorization: Bearer <apiKey>".
 */
class KiloGatewayClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun listModels(baseUrl: String, apiKey: String): GatewayResult<List<GatewayModel>> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext GatewayResult.Failure("Add your Kilo Gateway API key first.")
            try {
                val request = Request.Builder()
                    .url("$baseUrl/models")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext GatewayResult.Failure(
                            extractError(bodyString) ?: "Request failed (HTTP ${response.code})"
                        )
                    }
                    GatewayResult.Success(parseModels(bodyString))
                }
            } catch (io: IOException) {
                GatewayResult.Failure(io.message ?: "Network error while fetching models.")
            } catch (t: Throwable) {
                GatewayResult.Failure(t.message ?: "Unexpected error while fetching models.")
            }
        }

    suspend fun sendChat(
        baseUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        history: List<ChatMessage>
    ): GatewayResult<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext GatewayResult.Failure("Add your Kilo Gateway API key in Settings first.")
        try {
            val messages = JSONArray()
            if (systemPrompt.isNotBlank()) {
                messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
            }
            history.forEach { msg ->
                val role = when (msg.role) {
                    Role.USER -> "user"
                    Role.ASSISTANT -> "assistant"
                    Role.SYSTEM -> "system"
                }
                messages.put(JSONObject().put("role", role).put("content", msg.text))
            }

            val payload = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("stream", false)
                put("temperature", 0.7)
            }

            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext GatewayResult.Failure(
                        extractError(bodyString) ?: "Request failed (HTTP ${response.code})"
                    )
                }
                val reply = parseChatReply(bodyString)
                    ?: return@withContext GatewayResult.Failure("The gateway returned an empty response.")
                GatewayResult.Success(reply)
            }
        } catch (io: IOException) {
            GatewayResult.Failure(io.message ?: "Network error while contacting Kilo Gateway.")
        } catch (t: Throwable) {
            GatewayResult.Failure(t.message ?: "Unexpected error while contacting Kilo Gateway.")
        }
    }

    private fun parseChatReply(body: String): String? {
        return try {
            val json = JSONObject(body)
            val choices = json.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val message = choices.getJSONObject(0).optJSONObject("message") ?: return null
            message.optString("content").takeIf { it.isNotBlank() }
        } catch (t: Throwable) {
            null
        }
    }

    private fun parseModels(body: String): List<GatewayModel> {
        return try {
            val json = JSONObject(body)
            val data = json.optJSONArray("data") ?: JSONArray()
            (0 until data.length()).mapNotNull { i ->
                val entry = data.optJSONObject(i) ?: return@mapNotNull null
                val id = entry.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val name = entry.optString("name").takeIf { it.isNotBlank() } ?: id
                val context = entry.optJSONObject("context_window")?.let { null }
                    ?: entry.optInt("context_length", -1).takeIf { it > 0 }
                GatewayModel(id = id, displayName = name, contextWindow = context)
            }.sortedBy { it.displayName.lowercase() }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    private fun extractError(body: String): String? {
        return try {
            val json = JSONObject(body)
            json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
        } catch (t: Throwable) {
            null
        }
    }
}
