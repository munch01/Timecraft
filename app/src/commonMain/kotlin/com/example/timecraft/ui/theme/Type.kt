package com.example.timecraft.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import timecraft.app.generated.resources.Res
import timecraft.app.generated.resources.*

@Composable
fun getLexendFamily() = FontFamily(
    Font(Res.font.lexend_thin, FontWeight.Thin),
    Font(Res.font.lexend_extralight, FontWeight.ExtraLight),
    Font(Res.font.lexend_light, FontWeight.Light),
    Font(Res.font.lexend_regular, FontWeight.Normal),
    Font(Res.font.lexend_medium, FontWeight.Medium),
    Font(Res.font.lexend_semibold, FontWeight.SemiBold),
    Font(Res.font.lexend_bold, FontWeight.Bold),
    Font(Res.font.lexend_extrabold, FontWeight.ExtraBold),
    Font(Res.font.lexend_black, FontWeight.Black)
)

@Composable
fun getTypography(): Typography {
    val lexendFamily = getLexendFamily()
    return Typography(
        displayLarge = TextStyle(fontFamily = lexendFamily, fontWeight = FontWeight.Bold),
        displayMedium = TextStyle(fontFamily = lexendFamily, fontWeight = FontWeight.Bold),
        displaySmall = TextStyle(fontFamily = lexendFamily, fontWeight = FontWeight.Bold),
        headlineLarge = TextStyle(fontFamily = lexendFamily, fontWeight = FontWeight.Bold),
        headlineMedium = TextStyle(fontFamily = lexendFamily, fontWeight = FontWeight.Bold),
        headlineSmall = TextStyle(fontFamily = lexendFamily, fontWeight = FontWeight.Bold),
        titleLarge = TextStyle(fontFamily = lexendFamily, fontWeight = FontWeight.Bold),
        titleMedium = TextStyle(fontFamily = lexendFamily, fontWeight = FontWeight.SemiBold),
        titleSmall = TextStyle(fontFamily = lexendFamily, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontFamily = lexendFamily, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontFamily = lexendFamily, fontWeight = FontWeight.Normal),
        bodySmall = TextStyle(fontFamily = lexendFamily, fontWeight = FontWeight.Normal),
        labelLarge = TextStyle(fontFamily = lexendFamily, fontWeight = FontWeight.Medium),
        labelMedium = TextStyle(fontFamily = lexendFamily, fontWeight = FontWeight.Medium),
        labelSmall = TextStyle(fontFamily = lexendFamily, fontWeight = FontWeight.Medium)
    )
}
