package com.midnight.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.midnight.assistant.ui.components.GlassCard
import com.midnight.assistant.ui.theme.MidnightColors
import com.midnight.assistant.ui.theme.MidnightSpacing
import com.midnight.assistant.viewmodel.ChatViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onSessionSelected: () -> Unit
) {
    val historyState by viewModel.historyState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshHistory()
    }

    Scaffold(
        containerColor = MidnightColors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Conversations", style = MaterialTheme.typography.headlineMedium, color = MidnightColors.onSurface)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MidnightColors.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightColors.background)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MidnightColors.background)
                .padding(padding)
        ) {
            when {
                historyState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MidnightColors.tertiary
                    )
                }
                historyState.sessions.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MidnightSpacing.marginMobile),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.ChatBubbleOutline,
                            contentDescription = null,
                            tint = MidnightColors.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            "No conversations yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MidnightColors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            "Talk to the assistant on the main screen and it'll show up here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MidnightColors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = MidnightSpacing.marginMobile,
                            vertical = MidnightSpacing.stackMd
                        ),
                        verticalArrangement = Arrangement.spacedBy(MidnightSpacing.stackSm)
                    ) {
                        items(historyState.sessions, key = { it.id }) { session ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.openSession(session.id)
                                        onSessionSelected()
                                    },
                                contentPadding = 16.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            session.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = MidnightColors.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${session.messageCount} messages · ${formatDate(session.updatedAt)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MidnightColors.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteSession(session.id) }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete conversation",
                                            tint = MidnightColors.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(millis: Long): String {
    if (millis <= 0L) return ""
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
}
