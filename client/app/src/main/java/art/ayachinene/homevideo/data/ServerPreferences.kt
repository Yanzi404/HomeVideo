package art.ayachinene.homevideo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ServerPreferences(private val context: Context) {

    companion object {
        private val KEY_SERVER_IP = stringPreferencesKey("server_ip")
        private val KEY_DEFAULT_SPEED = stringPreferencesKey("default_playback_speed")
        private val KEY_SUBTITLE_SIZE = stringPreferencesKey("subtitle_size")
    }

    val serverIp: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVER_IP] ?: ""
    }

    val defaultPlaybackSpeed: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_SPEED]?.toFloatOrNull() ?: 1.0f
    }

    val subtitleSize: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SUBTITLE_SIZE] ?: "medium"
    }

    suspend fun getServerIpSync(): String {
        return serverIp.first()
    }

    suspend fun saveServerIp(ip: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_IP] = ip
        }
    }

    suspend fun saveDefaultPlaybackSpeed(speed: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_SPEED] = speed.toString()
        }
    }

    suspend fun saveSubtitleSize(size: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SUBTITLE_SIZE] = size
        }
    }
}
