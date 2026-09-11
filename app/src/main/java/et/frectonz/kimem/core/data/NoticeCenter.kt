package et.frectonz.kimem.core.data

import et.frectonz.kimem.core.model.UiText
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NoticeCenter {
    private val flow = MutableSharedFlow<UiText>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val notices: SharedFlow<UiText> = flow.asSharedFlow()

    fun post(text: UiText) {
        flow.tryEmit(text)
    }
}
