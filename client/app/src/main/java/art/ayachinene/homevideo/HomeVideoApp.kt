package art.ayachinene.homevideo

import android.app.Application

class HomeVideoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: HomeVideoApp
            private set
    }
}
