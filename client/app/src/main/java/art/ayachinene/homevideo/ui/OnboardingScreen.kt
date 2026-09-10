package art.ayachinene.homevideo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import art.ayachinene.homevideo.data.remote.ApiClient
import art.ayachinene.homevideo.ui.theme.CSGOColors
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onConnected: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var mode by remember { mutableStateOf("choice") }
    var manualIp by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CSGOColors.Background,
                        Color(0xFF0E1A2B)
                    )
                )
            )
    ) {
        when (mode) {
            "choice" -> ChoiceMode(
                onAutoScan = {
                    mode = "scanning"
                    scope.launch {
                        val result = viewModel.tryConnect("192.168.31.73")
                        if (result) {
                            onConnected()
                        } else {
                            mode = "manual"
                        }
                    }
                },
                onManualInput = { mode = "manual" }
            )
            "scanning" -> ScanningMode()
            "manual" -> ManualMode(
                manualIp = manualIp,
                onIpChange = { manualIp = it },
                uiState = uiState,
                onConnect = {
                    val ip = manualIp.ifBlank { "192.168.31.73" }
                    scope.launch {
                        if (viewModel.tryConnect(ip)) {
                            onConnected()
                        }
                    }
                },
                onBack = { mode = "choice" }
            )
        }
    }
}

@Composable
private fun ChoiceMode(
    onAutoScan: () -> Unit,
    onManualInput: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "HomeVideo",
            style = MaterialTheme.typography.displayLarge,
            color = CSGOColors.Primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Your personal home cinema",
            style = MaterialTheme.typography.titleMedium,
            color = CSGOColors.Secondary
        )
        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = onAutoScan,
            modifier = Modifier
                .width(280.dp)
                .height(56.dp)
                .focusRequester(focusRequester),
            shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
            colors = ButtonDefaults.colors(
                containerColor = CSGOColors.Primary,
                contentColor = Color.White,
                focusedContainerColor = CSGOColors.Primary.copy(alpha = 0.85f),
                focusedContentColor = Color.White
            )
        ) {
            Text("Auto Connect", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onManualInput,
            modifier = Modifier
                .width(280.dp)
                .height(56.dp),
            shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
            colors = ButtonDefaults.colors(
                containerColor = CSGOColors.Surface,
                contentColor = CSGOColors.OnBackground,
                focusedContainerColor = CSGOColors.Secondary.copy(alpha = 0.3f),
                focusedContentColor = Color.White
            )
        ) {
            Text("Manual Input", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ScanningMode() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = CSGOColors.Primary,
            modifier = Modifier.width(48.dp).height(48.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Connecting to server...",
            style = MaterialTheme.typography.titleMedium,
            color = CSGOColors.OnBackground
        )
    }
}

@Composable
private fun ManualMode(
    manualIp: String,
    onIpChange: (String) -> Unit,
    uiState: HomeUiState,
    onConnect: () -> Unit,
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Enter Server Address",
            style = MaterialTheme.typography.headlineMedium,
            color = CSGOColors.OnBackground
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = manualIp,
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

        if (uiState.connectionState is ConnectionState.Error) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = (uiState.connectionState as ConnectionState.Error).message,
                color = CSGOColors.Error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onConnect,
            modifier = Modifier
                .width(200.dp)
                .height(48.dp),
            enabled = uiState.connectionState !is ConnectionState.Loading,
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
            if (uiState.connectionState is ConnectionState.Loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.width(24.dp).height(24.dp)
                )
            } else {
                Text("Connect", style = MaterialTheme.typography.titleSmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.width(200.dp).height(48.dp),
            shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
            colors = ButtonDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = CSGOColors.OnSurface,
                focusedContainerColor = CSGOColors.Surface.copy(alpha = 0.5f),
                focusedContentColor = CSGOColors.OnBackground
            )
        ) {
            Text("Back")
        }
    }
}
