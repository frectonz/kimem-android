package et.frectonz.kimem.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Mono = FontFamily.Monospace

val KimemTypography: Typography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.copy(fontFamily = Mono),
        displayMedium = t.displayMedium.copy(fontFamily = Mono),
        displaySmall = t.displaySmall.copy(fontFamily = Mono),
        headlineLarge = t.headlineLarge.copy(fontFamily = Mono),
        headlineMedium = t.headlineMedium.copy(fontFamily = Mono),
        headlineSmall = t.headlineSmall.copy(fontFamily = Mono, fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontFamily = Mono, fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontFamily = Mono, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        titleSmall = t.titleSmall.copy(fontFamily = Mono, fontWeight = FontWeight.Bold),
        bodyLarge = t.bodyLarge.copy(fontFamily = Mono),
        bodyMedium = t.bodyMedium.copy(fontFamily = Mono),
        bodySmall = t.bodySmall.copy(fontFamily = Mono),
        labelLarge = t.labelLarge.copy(fontFamily = Mono, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        labelMedium = t.labelMedium.copy(fontFamily = Mono, letterSpacing = 1.sp),
        labelSmall = t.labelSmall.copy(fontFamily = Mono, letterSpacing = 1.5.sp),
    )
}
