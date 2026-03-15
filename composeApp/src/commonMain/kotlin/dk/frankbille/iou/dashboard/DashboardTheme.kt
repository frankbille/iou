@file:Suppress("FunctionName")

package dk.frankbille.iou.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val Sand = Color(0xFFF6EFE3)
internal val Linen = Color(0xFFFBF7F1)
internal val Ink = Color(0xFF202826)
internal val Mist = Color(0xFFE6D8C2)
internal val Pine = Color(0xFF2C6B63)
internal val PineSoft = Color(0xFFCFE3DE)
internal val Clay = Color(0xFFB65E3D)
internal val ClaySoft = Color(0xFFF4D3C2)
internal val Gold = Color(0xFF9C7C2C)
internal val GoldSoft = Color(0xFFF0E3B6)
internal val Fog = Color(0xFF6F6A61)

private val IouColors =
    lightColorScheme(
        primary = Pine,
        onPrimary = Linen,
        primaryContainer = PineSoft,
        secondary = Clay,
        secondaryContainer = ClaySoft,
        tertiary = Gold,
        tertiaryContainer = GoldSoft,
        background = Sand,
        surface = Linen,
        surfaceVariant = Mist,
        outline = Color(0xFFB7A58E),
        onBackground = Ink,
        onSurface = Ink,
        onSurfaceVariant = Fog,
    )

private val IouTypography =
    Typography(
        displayMedium =
            TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 38.sp,
                lineHeight = 42.sp,
                letterSpacing = (-0.6).sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.3).sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 27.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.4.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                letterSpacing = 1.1.sp,
            ),
    )

@Composable
fun IouTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IouColors,
        typography = IouTypography,
        content = content,
    )
}
