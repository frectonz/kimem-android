package et.frectonz.kimem.core.model

import android.content.Context
import androidx.annotation.StringRes
import et.frectonz.kimem.R

sealed interface UiText {
    data class Plain(val value: String) : UiText
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    fun resolve(context: Context): String = when (this) {
        is Plain -> value
        is Res -> context.getString(id, *args.map { if (it is UiText) it.resolve(context) else it }.toTypedArray())
    }
}

fun res(@StringRes id: Int, vararg args: Any): UiText = UiText.Res(id, args.toList())
fun plain(value: String): UiText = UiText.Plain(value)
fun orDash(value: String): UiText = if (value.isEmpty()) res(R.string.dash) else plain(value)
fun yesNo(value: Boolean): UiText = res(if (value) R.string.yes else R.string.no)

class RouterException(val text: UiText, cause: Throwable? = null) : Exception(text.toString(), cause)

sealed interface Report {
    val title: UiText

    data class KeyValues(override val title: UiText, val rows: List<Pair<UiText, UiText>>) : Report

    data class Records(
        override val title: UiText,
        val columns: List<UiText>,
        val rows: List<List<UiText>>,
    ) : Report

    data class Text(override val title: UiText, val text: String) : Report
}

interface Show {
    fun table(): Report
}

class TextShow(private val title: UiText, private val text: String) : Show {
    override fun table(): Report = Report.Text(title, text)
}

enum class ActionKind(@StringRes val resultTitle: Int, @StringRes val done: Int) {
    Send(R.string.result_send, R.string.sent),
    Delete(R.string.result_delete, R.string.deleted),
    Mark(R.string.result_mark, R.string.marked_as_read),
}

class ActionResult(private val kind: ActionKind, val result: String) : Show {
    override fun table(): Report = Report.KeyValues(res(kind.resultTitle), listOf(res(R.string.result) to plain(result)))

    fun notice(): UiText =
        if (result.equals("success", ignoreCase = true)) res(kind.done)
        else res(R.string.action_failed, res(kind.done), result)
}

internal fun rows(vararg pairs: Pair<Int, UiText>): List<Pair<UiText, UiText>> = pairs.map { (k, v) -> res(k) to v }
