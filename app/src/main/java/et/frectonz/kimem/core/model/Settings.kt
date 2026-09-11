package et.frectonz.kimem.core.model

import androidx.annotation.StringRes
import et.frectonz.kimem.R

enum class ThemeMode(@StringRes val labelRes: Int) {
    System(R.string.theme_system), Light(R.string.theme_light), Dark(R.string.theme_dark)
}

data class Settings(
    val router: String = "192.168.0.1",
    val username: String = "admin",
    val password: String = "admin",
    val theme: ThemeMode = ThemeMode.System,
)
