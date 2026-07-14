package com.midnight.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.midnight.assistant.data.KiloDefaults
import com.midnight.assistant.ui.components.GlassCard
import com.midnight.assistant.ui.theme.MidnightColors
import com.midnight.assistant.ui.theme.MidnightSpacing
import com.midnight.assistant.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val settingsState by viewModel.settingsState.collectAsState()
    val clipboard = LocalClipboardManager.current

    var apiKey by remember(settingsState.settings.apiKey) { mutableStateOf(settingsState.settings.apiKey) }
    var baseUrl by remember(settingsState.settings.baseUrl) { mutableStateOf(settingsState.settings.baseUrl) }
    var systemPrompt by remember(settingsState.settings.systemPrompt) { mutableStateOf(settingsState.settings.systemPrompt) }
    var modelId by remember(settingsState.settings.modelId) { mutableStateOf(settingsState.settings.modelId) }
    var modelName by remember(settingsState.settings.modelDisplayName) { mutableStateOf(settingsState.settings.modelDisplayName) }
    var showKey by remember { mutableStateOf(false) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var autoSpeak by remember(settingsState.settings.autoSpeak) { mutableStateOf(settingsState.settings.autoSpeak) }

    LaunchedEffect(Unit) {
        if (settingsState.settings.apiKey.isNotBlank()) {
            viewModel.fetchModels()
        }
    }

    Scaffold(
        containerColor = MidnightColors.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineMedium, color = MidnightColors.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MidnightColors.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightColors.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MidnightColors.background)
                .padding(padding)
                .padding(horizontal = MidnightSpacing.marginMobile)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MidnightSpacing.stackMd)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SectionLabel("Kilo Gateway")

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(MidnightSpacing.stackSm)) {
                    Text(
                        "API key",
                        style = MaterialTheme.typography.labelMedium,
                        color = MidnightColors.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        placeholder = { Text("Paste your Kilo Gateway API key") },
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = {
                                    clipboard.getText()?.text?.let { apiKey = it }
                                }) {
                                    Icon(Icons.Filled.ContentPaste, contentDescription = "Paste", tint = MidnightColors.onSurfaceVariant)
                                }
                                IconButton(onClick = { showKey = !showKey }) {
                                    Icon(
                                        if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = "Toggle visibility",
                                        tint = MidnightColors.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = midnightFieldColors(),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Base URL",
                        style = MaterialTheme.typography.labelMedium,
                        color = MidnightColors.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        singleLine = true,
                        placeholder = { Text(KiloDefaults.BASE_URL) },
                        colors = midnightFieldColors(),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Model",
                        style = MaterialTheme.typography.labelMedium,
                        color = MidnightColors.onSurfaceVariant
                    )

                    ExposedDropdownMenuBox(
                        expanded = modelMenuExpanded,
                        onExpandedChange = { modelMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = modelName,
                            onValueChange = {
                                modelName = it
                                modelId = it
                            },
                            placeholder = { Text(KiloDefaults.MODEL) },
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded) },
                            colors = midnightFieldColors(),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = modelMenuExpanded && settingsState.availableModels.isNotEmpty(),
                            onDismissRequest = { modelMenuExpanded = false }
                        ) {
                            settingsState.availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model.displayName) },
                                    onClick = {
                                        modelId = model.id
                                        modelName = model.displayName
                                        modelMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { viewModel.fetchModels(baseUrl, apiKey) }) {
                            if (settingsState.isLoadingModels) {
                                CircularProgressIndicator(modifier = Modifier.height(16.dp), color = MidnightColors.tertiary, strokeWidth = 2.dp)
                            } else {
                                Text("Fetch available models", color = MidnightColors.tertiary)
                            }
                        }
                    }
                    settingsState.modelLoadError?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MidnightColors.error)
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Speak replies aloud", style = MaterialTheme.typography.bodyLarge, color = MidnightColors.onSurface)
                        Text(
                            "Automatically read assistant responses using text-to-speech",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MidnightColors.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoSpeak,
                        onCheckedChange = {
                            autoSpeak = it
                            viewModel.saveAutoSpeak(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MidnightColors.onPrimary,
                            checkedTrackColor = MidnightColors.tertiary,
                            uncheckedTrackColor = MidnightColors.surfaceContainerHigh
                        )
                    )
                }
            }

            SectionLabel("Assistant behavior")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(MidnightSpacing.stackSm)) {
                    Text("System prompt", style = MaterialTheme.typography.labelMedium, color = MidnightColors.onSurfaceVariant)
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        minLines = 3,
                        colors = midnightFieldColors(),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            settingsState.testStatus?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (it.startsWith("✓")) MidnightColors.tertiary else MidnightColors.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MidnightSpacing.stackSm)
            ) {
                Button(
                    onClick = { viewModel.testConnection(baseUrl, apiKey, modelId) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MidnightColors.surfaceContainerHigh,
                        contentColor = MidnightColors.onSurface
                    ),
                    enabled = !settingsState.isTesting
                ) {
                    if (settingsState.isTesting) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp), color = MidnightColors.tertiary, strokeWidth = 2.dp)
                    } else {
                        Text("Test connection")
                    }
                }
                Button(
                    onClick = {
                        viewModel.saveApiKey(apiKey)
                        viewModel.saveBaseUrl(baseUrl.ifBlank { KiloDefaults.BASE_URL })
                        viewModel.saveModel(modelId.ifBlank { KiloDefaults.MODEL }, modelName.ifBlank { KiloDefaults.MODEL })
                        viewModel.saveSystemPrompt(systemPrompt)
                        onBack()
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MidnightColors.tertiary,
                        contentColor = MidnightColors.onTertiary
                    )
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(MidnightSpacing.stackLg))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MidnightColors.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun midnightFieldColors() = OutlinedTextFieldDefaults.colors(
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
