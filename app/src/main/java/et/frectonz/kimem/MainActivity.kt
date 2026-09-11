package et.frectonz.kimem

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import et.frectonz.kimem.core.model.Settings
import et.frectonz.kimem.ui.KimemApp
import et.frectonz.kimem.ui.theme.KimemTheme
import et.frectonz.kimem.ui.theme.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as KimemApplication).container

        setContent {
            val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(Settings())
            val dark = settings.theme.isDark()

            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            KimemTheme(darkTheme = dark) {
                KimemApp(container.notices)
            }
        }
    }
}
