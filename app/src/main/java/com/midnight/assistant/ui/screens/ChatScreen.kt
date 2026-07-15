package com.midnight.assistant.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.midnight.assistant.ui.components.AiOrb
import com.midnight.assistant.ui.components.MessageBubble
import com.midnight.assistant.ui.theme.MidnightColors
import com.midnight.assistant.ui.theme.MidnightSpacing
import com.midnight.assistant.viewmodel.ChatViewModel
import com.midnight.assistant.viewmodel.OrbState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.chatState.collectAsState()
    val listState = rememberLazyListState()

    var typedText by remember { mutableStateOf("") }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) viewModel.startListening()
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    fun sendTyped() {
        val text = typedText.trim()
        if (text.isNotEmpty()) {
            viewModel.sendTypedMessage(text)
            typedText = ""
        }
    }

    Scaffold(
        containerColor = MidnightColors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Midnight Assistant",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MidnightColors.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Filled.History, contentDescription = "Conversation history", tint = MidnightColors.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.startNewConversation() }) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "New conversation", tint = MidnightColors.onSurfaceVariant)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MidnightColors.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightColors.background,
                    titleContentColor = MidnightColors.onSurface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MidnightColors.surfaceContainerLowest,
                            MidnightColors.background
                        )
                    )
                )
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Conversation transcript — every message in the current session lives here;
                // switch conversations from the history screen (top-bar clock icon).
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        horizontal = MidnightSpacing.marginMobile,
                        vertical = MidnightSpacing.stackMd
                    ),
                    verticalArrangement = Arrangement.spacedBy(MidnightSpacing.stackSm)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(message = message)
                    }
                }

                // Orb + status + mic control, anchored toward the bottom per design.md
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = MidnightSpacing.stackMd),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.liveTranscript.isNotBlank()) {
                        Text(
                            text = state.liveTranscript,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MidnightColors.onSurface,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                        )
                    }

                    AiOrb(
                        state = state.orbState,
                        micLevel = state.micLevel,
                        modifier = Modifier
                    )

                    Spacer(modifier = Modifier.height(MidnightSpacing.stackSm))

                    Text(
                        text = state.statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MidnightColors.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(MidnightSpacing.stackMd))

                    // Typed-message row — shows a live preview of what's being typed,
                    // and doubles as a fallback input alongside voice.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MidnightSpacing.marginMobile),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MidnightSpacing.stackSm)
                    ) {
                        OutlinedTextField(
                            value = typedText,
                            onValueChange = { typedText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message…") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { sendTyped() }),
                            trailingIcon = {
                                IconButton(onClick = { sendTyped() }, enabled = typedText.isNotBlank()) {
                                    Icon(
                                        Icons.Filled.Send,
                                        contentDescription = "Send",
                                        tint = if (typedText.isNotBlank()) MidnightColors.tertiary else MidnightColors.onSurfaceVariant
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MidnightColors.onSurface,
                                unfocusedTextColor = MidnightColors.onSurface,
                                focusedBorderColor = MidnightColors.secondary,
                                unfocusedBorderColor = MidnightColors.ghostBorderStrong,
                                cursorColor = MidnightColors.secondary,
                                focusedContainerColor = MidnightColors.surfaceContainerLow.copy(alpha = 0.4f),
                                unfocusedContainerColor = MidnightColors.surfaceContainerLow.copy(alpha = 0.25f),
                                focusedPlaceholderColor = MidnightColors.onSurfaceVariant,
                                unfocusedPlaceholderColor = MidnightColors.onSurfaceVariant
                            )
                        )

                        val micActive = state.orbState == OrbState.LISTENING
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = if (micActive) {
                                            listOf(MidnightColors.error, MidnightColors.errorContainer)
                                        } else {
                                            listOf(MidnightColors.tertiary, MidnightColors.secondary)
                                        }
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    when {
                                        micActive -> viewModel.stopListening()
                                        hasMicPermission -> viewModel.startListening()
                                        else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = if (micActive) Icons.Filled.MicOff else Icons.Filled.Mic,
                                    contentDescription = if (micActive) "Stop listening" else "Start listening",
                                    tint = MidnightColors.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
