package art.ayachinene.homevideo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import art.ayachinene.homevideo.HomeVideoApp
import art.ayachinene.homevideo.data.ServerPreferences
import art.ayachinene.homevideo.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverIp: String = "",
    val editServerIp: String = "",
    val isEditingServerIp: Boolean = false,
    val defaultPlaybackSpeed: Float = 1.0f,
    val subtitleSize: String = "medium",
    val isTestingConnection: Boolean = false,
    val connectionError: String? = null
)

class SettingsViewModel : ViewModel() {

    private val prefs: ServerPreferences = HomeVideoApp.instance.serverPreferences
    private val api = ApiClient.apiService

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val ip = prefs.serverIp.first()
            val speed = prefs.defaultPlaybackSpeed.first()
            val size = prefs.subtitleSize.first()
            _uiState.value = _uiState.value.copy(
                serverIp = ip,
                editServerIp = ip,
                defaultPlaybackSpeed = speed,
                subtitleSize = size
            )
        }
    }

    fun startEditServerIp() {
        _uiState.value = _uiState.value.copy(
            isEditingServerIp = true,
            editServerIp = _uiState.value.serverIp,
            connectionError = null
        )
    }

    fun cancelEditServerIp() {
        _uiState.value = _uiState.value.copy(isEditingServerIp = false, connectionError = null)
    }

    fun updateEditServerIp(ip: String) {
        _uiState.value = _uiState.value.copy(editServerIp = ip)
    }

    fun saveServerIp(onSuccess: () -> Unit) {
        val ip = _uiState.value.editServerIp.trim()
        if (ip.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingConnection = true, connectionError = null)
            try {
                ApiClient.updateBaseUrl(ip)
                val result = api.health()
                if (result["status"] == "ok") {
                    prefs.saveServerIp(ip)
                    _uiState.value = _uiState.value.copy(
                        serverIp = ip,
                        isEditingServerIp = false,
                        isTestingConnection = false,
                        connectionError = null
                    )
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isTestingConnection = false,
                        connectionError = "Server response error"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTestingConnection = false,
                    connectionError = e.message ?: "Connection failed"
                )
            }
        }
    }

    fun setDefaultPlaybackSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(defaultPlaybackSpeed = speed)
        viewModelScope.launch { prefs.saveDefaultPlaybackSpeed(speed) }
    }

    fun setSubtitleSize(size: String) {
        _uiState.value = _uiState.value.copy(subtitleSize = size)
        viewModelScope.launch { prefs.saveSubtitleSize(size) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                api.deleteHistory()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
