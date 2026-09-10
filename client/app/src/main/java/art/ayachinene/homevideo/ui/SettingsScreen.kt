package art.ayachinene.homevideo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import art.ayachinene.homevideo.ui.theme.CSGOColors

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CSGOColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CSGOColors.Primary
                )
                Button(
                    onClick = onBack,
                    modifier = Modifier.focusRequester(focusRequester),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.colors(
                        containerColor = CSGOColors.Surface,
                        contentColor = CSGOColors.OnBackground,
                        focusedContainerColor = CSGOColors.Secondary.copy(alpha = 0.3f),
                        focusedContentColor = Color.White
                    )
                ) {
                    Text("Back")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    SectionHeader("Server")
                }

                item {
                    if (uiState.isEditingServerIp) {
                        EditServerIpSection(
                            editIp = uiState.editServerIp,
                            isTesting = uiState.isTestingConnection,
                            error = uiState.connectionError,
                            onIpChange = { viewModel.updateEditServerIp(it) },
                            onSave = { viewModel.saveServerIp(onBack) },
                            onCancel = { viewModel.cancelEditServerIp() }
                        )
                    } else {
                        SettingsListItem(
                            title = "Server Address",
                            subtitle = uiState.serverIp.ifBlank { "Not configured" },
                            onClick = { viewModel.startEditServerIp() }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                item {
                    SectionHeader("Playback")
                }

                item {
                    SpeedSettingItem(
                        currentSpeed = uiState.defaultPlaybackSpeed,
                        onSpeedChange = { viewModel.setDefaultPlaybackSpeed(it) }
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                item {
                    SectionHeader("Subtitles")
                }

                item {
                    SubtitleSizeItem(
                        currentSize = uiState.subtitleSize,
                        onSizeChange = { viewModel.setSubtitleSize(it) }
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                item {
                    SectionHeader("Data")
                }

                item {
                    SettingsListItem(
                        title = "Clear Watch History",
                        subtitle = "Remove all playback history",
                        onClick = { viewModel.clearHistory() }
                    )
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }

                item {
                    Text(
                        "HomeVideo v0.1.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = CSGOColors.OnSurface
                    )
                }

                item { Spacer(modifier = Modifier.height(48.dp)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = CSGOColors.Secondary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsListItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        selected = false,
        onClick = onClick,
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = CSGOColors.Surface,
            selectedContainerColor = CSGOColors.Surface
        ),
        headlineContent = {
            Text(title, color = CSGOColors.OnBackground)
        },
        supportingContent = {
            Text(subtitle, color = CSGOColors.OnSurface)
        }
    )
}

@Composable
private fun EditServerIpSection(
    editIp: String,
    isTesting: Boolean,
    error: String?,
    onIpChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = editIp,
            onValueChange = onIpChange,
            placeholder = { Text("192.168.31.73", color = CSGOColors.OnSurface) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            colors = TextFieldDefaults.colors(
                focusedTextColor = CSGOColors.OnBackground,
                unfocusedTextColor = CSGOColors.OnBackground,
                focusedContainerColor = CSGOColors.Surface,
                unfocusedContainerColor = CSGOColors.Surface.copy(alpha = 0.5f),
                focusedIndicatorColor = CSGOColors.Primary,
                unfocusedIndicatorColor = CSGOColors.OnSurface,
                cursorColor = CSGOColors.Primary
            )
        )
        if (error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(error, color = CSGOColors.Error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onSave,
                enabled = !isTesting,
                shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.colors(
                    containerColor = CSGOColors.Primary,
                    contentColor = Color.White,
                    focusedContainerColor = CSGOColors.Primary.copy(alpha = 0.85f),
                    focusedContentColor = Color.White,
                    disabledContainerColor = CSGOColors.OnSurfaceDisabled,
                    disabledContentColor = CSGOColors.OnSurfaceDisabled
                )
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.width(20.dp).height(20.dp)
                    )
                } else {
                    Text("Save")
                }
            }
            Button(
                onClick = onCancel,
                shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.colors(
                    containerColor = CSGOColors.Surface,
                    contentColor = CSGOColors.OnBackground,
                    focusedContainerColor = CSGOColors.Secondary.copy(alpha = 0.3f),
                    focusedContentColor = Color.White
                )
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun SpeedSettingItem(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Default Playback Speed", color = CSGOColors.OnBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            speeds.forEach { speed ->
                val isSelected = speed == currentSpeed
                Button(
                    onClick = { onSpeedChange(speed) },
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.colors(
                        containerColor = if (isSelected) CSGOColors.Primary else CSGOColors.Surface,
                        contentColor = if (isSelected) Color.White else CSGOColors.OnSurface,
                        focusedContainerColor = if (isSelected) CSGOColors.Primary.copy(alpha = 0.85f) else CSGOColors.Secondary.copy(alpha = 0.3f),
                        focusedContentColor = Color.White
                    )
                ) {
                    Text("${speed}x")
                }
            }
        }
    }
}

@Composable
private fun SubtitleSizeItem(
    currentSize: String,
    onSizeChange: (String) -> Unit
) {
    val sizes = listOf("small" to "Small", "medium" to "Medium", "large" to "Large")
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Subtitle Size", color = CSGOColors.OnBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sizes.forEach { (value, label) ->
                val isSelected = value == currentSize
                Button(
                    onClick = { onSizeChange(value) },
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.colors(
                        containerColor = if (isSelected) CSGOColors.Primary else CSGOColors.Surface,
                        contentColor = if (isSelected) Color.White else CSGOColors.OnSurface,
                        focusedContainerColor = if (isSelected) CSGOColors.Primary.copy(alpha = 0.85f) else CSGOColors.Secondary.copy(alpha = 0.3f),
                        focusedContentColor = Color.White
                    )
                ) {
                    Text(label)
                }
            }
        }
    }
}
