package art.ayachinene.homevideo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import art.ayachinene.homevideo.HomeVideoApp
import art.ayachinene.homevideo.data.ServerPreferences
import art.ayachinene.homevideo.data.model.DirectoryItem
import art.ayachinene.homevideo.data.model.HistoryItem
import art.ayachinene.homevideo.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ConnectionState {
    object Loading : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class FolderRow(
    val folderName: String,
    val folderPath: String,
    val items: List<DirectoryItem>
)

data class HomeUiState(
    val connectionState: ConnectionState = ConnectionState.Loading,
    val currentPath: String = "/",
    val files: List<DirectoryItem> = emptyList(),
    val isLoadingFiles: Boolean = false,
    val isSearchMode: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<DirectoryItem> = emptyList(),
    val recentHistory: List<HistoryItem> = emptyList(),
    val pathStack: List<String> = emptyList(),
    val folderRows: List<FolderRow> = emptyList(),
    val isLoadingRows: Boolean = false,
    val heroItem: HistoryItem? = null
)

class HomeViewModel : ViewModel() {

    private val prefs: ServerPreferences = HomeVideoApp.instance.serverPreferences
    private val api = ApiClient.apiService

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    suspend fun initialize() {
        val savedIp = prefs.getServerIpSync()
        if (savedIp.isBlank()) {
            _uiState.value = _uiState.value.copy(connectionState = ConnectionState.Error("No server configured"))
            return
        }
        ApiClient.updateBaseUrl(savedIp)
        checkConnection()
    }

    private fun checkConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(connectionState = ConnectionState.Loading)
            try {
                val result = api.health()
                if (result["status"] == "ok") {
                    _uiState.value = _uiState.value.copy(connectionState = ConnectionState.Connected)
                    loadNetflixHome()
                } else {
                    _uiState.value = _uiState.value.copy(
                        connectionState = ConnectionState.Error("Server response error")
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    connectionState = ConnectionState.Error(e.message ?: "Connection failed")
                )
            }
        }
    }

    private fun loadNetflixHome() {
        viewModelScope.launch {
            try {
                var history = emptyList<HistoryItem>()
                try {
                    history = api.getHistory()
                } catch (_: Exception) {
                    // history endpoint may not be available yet
                }
                val heroItem = history.firstOrNull()

                val library = api.getLibrary()
                val rows = library.folders.map { folder ->
                    FolderRow(
                        folderName = folder.name,
                        folderPath = folder.path,
                        items = folder.videos
                    )
                }

                val effectiveHero = heroItem ?: rows.firstOrNull()?.items?.firstOrNull()?.let { first ->
                    val name = first.path.substringAfterLast("/")
                    HistoryItem(
                        videoPath = first.path,
                        videoName = name,
                        position = 0,
                        duration = 0,
                        lastPlayed = 0
                    )
                }

                _uiState.value = _uiState.value.copy(
                    connectionState = ConnectionState.Connected,
                    recentHistory = history.take(10),
                    heroItem = effectiveHero,
                    folderRows = rows,
                    isLoadingRows = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    connectionState = ConnectionState.Error("Load failed: ${e.message}")
                )
            }
        }
    }

    fun loadFiles(path: String) {
        val apiPath = if (path.startsWith("/")) path else "/$path"
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingFiles = true)
            try {
                val response = api.listFiles(path = apiPath)
                val sorted = response.items.sortedWith(
                    compareBy<DirectoryItem> { if (it.type == "folder") 0 else 1 }
                        .thenBy { it.name.lowercase() }
                )
                _uiState.value = _uiState.value.copy(
                    currentPath = response.path,
                    files = sorted,
                    isLoadingFiles = false,
                    isSearchMode = false,
                    searchQuery = "",
                    searchResults = emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingFiles = false,
                    connectionState = ConnectionState.Error("Load failed: ${e.message}")
                )
            }
        }
    }

    fun enterFolder(item: DirectoryItem) {
        val currentStack = _uiState.value.pathStack
        val currentPath = _uiState.value.currentPath
        _uiState.value = _uiState.value.copy(pathStack = currentStack + currentPath)
        loadFiles(item.path)
    }

    fun goBack(): Boolean {
        val stack = _uiState.value.pathStack
        if (stack.isEmpty()) return false
        val prevPath = stack.last()
        _uiState.value = _uiState.value.copy(pathStack = stack.dropLast(1))
        loadFiles(prevPath)
        return true
    }

    fun startSearch() {
        _uiState.value = _uiState.value.copy(isSearchMode = true, searchQuery = "", searchResults = emptyList())
    }

    fun stopSearch() {
        _uiState.value = _uiState.value.copy(isSearchMode = false, searchQuery = "", searchResults = emptyList())
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }
        viewModelScope.launch {
            try {
                val results = api.searchFiles(query)
                _uiState.value = _uiState.value.copy(searchResults = results)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(searchResults = emptyList())
            }
        }
    }

    fun removeHistoryItem(videoPath: String) {
        viewModelScope.launch {
            try {
                api.deleteHistory(videoPath)
                loadRecentHistory()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                api.deleteHistory()
                _uiState.value = _uiState.value.copy(recentHistory = emptyList())
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun loadRecentHistory() {
        viewModelScope.launch {
            try {
                val history = api.getHistory()
                _uiState.value = _uiState.value.copy(recentHistory = history.take(10))
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    suspend fun tryConnect(ip: String): Boolean {
        _uiState.value = _uiState.value.copy(connectionState = ConnectionState.Loading)
        return try {
            ApiClient.updateBaseUrl(ip)
            val result = api.health()
            if (result["status"] == "ok") {
                prefs.saveServerIp(ip)
                _uiState.value = _uiState.value.copy(connectionState = ConnectionState.Connected)
                true
            } else {
                _uiState.value = _uiState.value.copy(
                    connectionState = ConnectionState.Error("Server response error")
                )
                false
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                connectionState = ConnectionState.Error("Cannot connect: ${e.message}")
            )
            false
        }
    }

    fun retry() {
        viewModelScope.launch { initialize() }
    }

    fun refreshHistory() {
        loadRecentHistory()
    }
}
