package art.ayachinene.homevideo

import android.app.Application
import art.ayachinene.homevideo.data.ServerPreferences
import art.ayachinene.homevideo.data.remote.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HomeVideoApp : Application() {

    lateinit var serverPreferences: ServerPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        serverPreferences = ServerPreferences(this)

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            val savedIp = serverPreferences.getServerIpSync()
            if (savedIp.isNotBlank()) {
                ApiClient.updateBaseUrl(savedIp)
            }
        }
    }

    companion object {
        lateinit var instance: HomeVideoApp
            private set
    }
}
