package art.ayachinene.homevideo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import art.ayachinene.homevideo.HomeVideoApp
import art.ayachinene.homevideo.data.model.VideoInfo
import art.ayachinene.homevideo.data.remote.ApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val videoPath: String = "",
    val videoTitle: String = "",
    val videoInfo: VideoInfo? = null,
    val isLoadingInfo: Boolean = true,
    val showControls: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showEndCard: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val selectedSubtitleIndex: Int = -1,
    val errorMessage: String? = null,
    val resumePosition: Long = 0L
)

class PlayerViewModel : ViewModel() {

    private val api = ApiClient.apiService
    private val prefs = HomeVideoApp.instance.serverPreferences

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var historyJob: Job? = null

    fun init(videoPath: String, videoTitle: String) {
        _uiState.value = _uiState.value.copy(videoPath = videoPath, videoTitle = videoTitle)
        loadVideoInfo(videoPath)
        loadDefaultSpeed()
    }

    private fun loadVideoInfo(path: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingInfo = true)
            try {
                val info = api.getVideoInfo(path)
                _uiState.value = _uiState.value.copy(videoInfo = info, isLoadingInfo = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingInfo = false,
                    errorMessage = "Failed to load video info: ${e.message}"
                )
            }

            try {
                val history = api.getHistory()
                val match = history.find { it.videoPath == path }
                if (match != null && match.position > 30000 && match.duration - match.position > 30000) {
                    _uiState.value = _uiState.value.copy(resumePosition = match.position)
                }
            } catch (_: Exception) {
                // history endpoint may not be available
            }
        }
    }

    private fun loadDefaultSpeed() {
        viewModelScope.launch {
            prefs.defaultPlaybackSpeed.collect { speed ->
                _uiState.value = _uiState.value.copy(playbackSpeed = speed)
            }
        }
    }

    fun toggleControls() {
        _uiState.value = _uiState.value.copy(showControls = !_uiState.value.showControls, showEndCard = false)
    }

    fun hideControls() {
        _uiState.value = _uiState.value.copy(showControls = false, showEndCard = false)
    }

    fun showEndCard() {
        _uiState.value = _uiState.value.copy(showEndCard = true, showControls = false)
    }

    fun toggleSettingsDialog() {
        _uiState.value = _uiState.value.copy(
            showSettingsDialog = !_uiState.value.showSettingsDialog,
            showControls = false
        )
    }

    fun updatePosition(position: Long) {
        _uiState.value = _uiState.value.copy(currentPosition = position)
    }

    fun updateDuration(duration: Long) {
        _uiState.value = _uiState.value.copy(duration = duration)
    }

    fun updatePlaying(playing: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = playing)
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun setSelectedSubtitle(index: Int) {
        _uiState.value = _uiState.value.copy(selectedSubtitleIndex = index)
    }

    fun startHistoryTracking() {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                val state = _uiState.value
                if (state.isPlaying && state.currentPosition > 0 && state.duration > 0) {
                    try {
                        api.saveHistory(
                            mapOf(
                                "videoPath" to state.videoPath,
                                "position" to state.currentPosition,
                                "duration" to state.duration
                            )
                        )
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
    }

    fun stopHistoryTracking() {
        historyJob?.cancel()
        historyJob = null
    }

    fun saveFinalPosition() {
        val state = _uiState.value
        if (state.currentPosition > 30000 && state.duration > 0) {
            viewModelScope.launch {
                try {
                    api.saveHistory(
                        mapOf(
                            "videoPath" to state.videoPath,
                            "position" to state.currentPosition,
                            "duration" to state.duration
                        )
                    )
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopHistoryTracking()
    }
}
