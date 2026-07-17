package com.midnight.assistant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.midnight.assistant.data.GatewayModel
import com.midnight.assistant.data.KiloDefaults
import com.midnight.assistant.ui.components.GlassCard
import com.midnight.assistant.ui.theme.MidnightColors
import com.midnight.assistant.ui.theme.MidnightRadius
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
    var showModelPicker by remember { mutableStateOf(false) }
    var autoSpeak by remember(settingsState.settings.autoSpeak) { mutableStateOf(settingsState.settings.autoSpeak) }
    var allowVoiceInterrupt by remember(settingsState.settings.allowVoiceInterrupt) {
        mutableStateOf(settingsState.settings.allowVoiceInterrupt)
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

                    Surface(
                        onClick = { showModelPicker = true },
                        shape = MaterialTheme.shapes.small,
                        color = MidnightColors.surfaceContainerLow.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MidnightColors.ghostBorderStrong),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    modelName.ifBlank { "Select a model" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (modelName.isBlank()) MidnightColors.onSurfaceVariant else MidnightColors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (modelId.isNotBlank() && modelId != modelName) {
                                    Text(
                                        modelId,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MidnightColors.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = "Choose model",
                                tint = MidnightColors.onSurfaceVariant
                            )
                        }
                    }

                    if (showModelPicker) {
                        ModelPickerDialog(
                            models = settingsState.availableModels,
                            currentModelId = modelId,
                            onDismiss = { showModelPicker = false },
                            onSelect = { model ->
                                modelId = model.id
                                modelName = model.displayName
                                showModelPicker = false
                            }
                        )
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
                        if (settingsState.availableModels.isNotEmpty() && !settingsState.isLoadingModels) {
                            Text(
                                "${settingsState.availableModels.size} cached",
                                style = MaterialTheme.typography.labelMedium,
                                color = MidnightColors.onSurfaceVariant
                            )
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

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow voice interruption", style = MaterialTheme.typography.bodyLarge, color = MidnightColors.onSurface)
                        Text(
                            "In Voice Mode, start talking while the assistant is speaking to interrupt it. " +
                                "Best-effort — may occasionally misfire on speakerphone; turn off if it interrupts itself.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MidnightColors.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = allowVoiceInterrupt,
                        onCheckedChange = {
                            allowVoiceInterrupt = it
                            viewModel.saveAllowVoiceInterrupt(it)
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

/**
 * Full searchable model picker shown as a dialog instead of a dropdown. An earlier version
 * nested the search field inside a Material3 ExposedDropdownMenu (a Popup) — typing into it
 * didn't reliably reflect because that popup's focus/dismiss handling doesn't play well with
 * a second focusable field inside it. A plain Dialog with its own always-focusable TextField
 * doesn't have that problem, and gives a lot more room to actually read a long model catalog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerDialog(
    models: List<GatewayModel>,
    currentModelId: String,
    onDismiss: () -> Unit,
    onSelect: (GatewayModel) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(models, query) {
        if (query.isBlank()) {
            models
        } else {
            models.filter {
                it.displayName.contains(query, ignoreCase = true) || it.id.contains(query, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(MidnightRadius.md),
            color = MidnightColors.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text(
                    "Select model",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MidnightColors.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search models…") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = MidnightColors.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = MidnightColors.onSurfaceVariant)
                            }
                        }
                    },
                    colors = midnightFieldColors(),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                when {
                    models.isEmpty() -> {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "No models fetched yet. Close this and tap \"Fetch available models\" first.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MidnightColors.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                    filtered.isEmpty() -> {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "No models match “$query”",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MidnightColors.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            items(filtered, key = { it.id }) { model ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelect(model) }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            model.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MidnightColors.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            model.id,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MidnightColors.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (model.id == currentModelId) {
                                        Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MidnightColors.tertiary)
                                    }
                                }
                                HorizontalDivider(color = MidnightColors.ghostBorder)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close", color = MidnightColors.onSurfaceVariant)
                }
            }
        }
    }
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
