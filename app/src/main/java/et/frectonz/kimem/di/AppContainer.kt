package et.frectonz.kimem.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import et.frectonz.kimem.KimemApplication
import et.frectonz.kimem.core.data.NoticeCenter
import et.frectonz.kimem.core.data.RouterRepository
import et.frectonz.kimem.core.data.SettingsRepository
import et.frectonz.kimem.core.data.settingsDataStore
import et.frectonz.kimem.core.network.RouterClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settingsRepository = SettingsRepository(context.settingsDataStore)
    val routerRepository = RouterRepository(settingsRepository, RouterClient())
    val notices = NoticeCenter()
}

val CreationExtras.container: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KimemApplication).container
