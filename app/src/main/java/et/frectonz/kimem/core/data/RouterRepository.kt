package et.frectonz.kimem.core.data

import et.frectonz.kimem.core.network.Router
import et.frectonz.kimem.core.network.RouterClient
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first

class RouterRepository(
    private val settings: SettingsRepository,
    private val client: RouterClient,
) {
    private val inboxChangesFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val inboxChanges: SharedFlow<Unit> = inboxChangesFlow.asSharedFlow()

    suspend fun open(): Router {
        val current = settings.settings.first()
        return client.open(current.router, current.username, current.password)
    }

    suspend fun <T> session(block: suspend Router.() -> T): T = open().session(block)

    fun notifyInboxChanged() {
        inboxChangesFlow.tryEmit(Unit)
    }
}
