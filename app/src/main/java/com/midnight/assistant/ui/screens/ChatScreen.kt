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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.chatState.collectAsState()
    val listState = rememberLazyListState()

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
                    IconButton(onClick = { viewModel.clearConversation() }) {
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
                // Conversation transcript
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
                        .padding(bottom = MidnightSpacing.stackLg),
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

                    val micActive = state.orbState == OrbState.LISTENING
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
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
                                modifier = Modifier.size(76.dp)
                            ) {
                                Icon(
                                    imageVector = if (micActive) Icons.Filled.MicOff else Icons.Filled.Mic,
                                    contentDescription = if (micActive) "Stop listening" else "Start listening",
                                    tint = MidnightColors.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
