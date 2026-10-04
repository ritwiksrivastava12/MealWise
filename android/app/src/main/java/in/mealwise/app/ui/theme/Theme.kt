package `in`.mealwise.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Seed = Color(0xFF2E7D32) // fresh-meal green; warm accent below
private val Accent = Color(0xFFFF8F00)

@Composable
fun MealWiseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Seed, secondary = Accent), content = content)
}
