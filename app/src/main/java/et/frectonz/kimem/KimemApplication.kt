package et.frectonz.kimem

import android.app.Application
import et.frectonz.kimem.di.AppContainer

class KimemApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
