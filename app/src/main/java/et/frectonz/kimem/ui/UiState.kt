package et.frectonz.kimem.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import et.frectonz.kimem.core.model.RouterException
import et.frectonz.kimem.core.model.UiText
import et.frectonz.kimem.core.model.plain
import kotlinx.coroutines.CancellationException

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Error(val message: UiText) : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
}

fun Throwable.uiText(): UiText = when (this) {
    is RouterException -> text
    else -> plain(message?.takeIf { it.isNotBlank() } ?: javaClass.simpleName)
}

suspend fun <T> load(block: suspend () -> T): Load<T> = try {
    Load.Ready(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Load.Error(e.uiText())
}

@Composable
fun UiText.string(): String = resolve(LocalContext.current)
