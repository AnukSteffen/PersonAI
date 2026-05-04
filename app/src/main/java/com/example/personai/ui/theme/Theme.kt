package com.example.personai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 1. 定义主题枚举
enum class AppThemeMode {
    DEFAULT, // 跟随系统 (默认)
    YELLOW,  // 明亮黄色
    GREEN,   // 清新绿色
    DARK     // 强制夜间
}

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = BlueGrey40,
    tertiary = Cyan40,
    background = white0,
    surface = white0,
    onPrimary = white0,
    onSecondary = white0,
    onTertiary = white0,
    onBackground = white1,
    onSurface = white1,
)

// 黄色方案 (亮色)
private val YellowColorScheme = lightColorScheme(
    primary = YellowPrimary,
    secondary = YellowSecondary,
    tertiary = YellowTertiary,
    background = white0,
    surface = white0,
    onPrimary = white0,
    onSecondary = white0,
    onTertiary = white0,
    onBackground = white1,
    onSurface = white1,
)

// 绿色方案 (亮色)
private val GreenColorScheme = lightColorScheme(
    primary = GreenPrimary,
    secondary = GreenSecondary,
    tertiary = GreenTertiary,
    background = white0,
    surface = white0,
    onPrimary = white0,
    onSecondary = white0,
    onTertiary = white0,
    onBackground = white1,
    onSurface = white1,
)

@Composable
fun PersonAITheme(
    themeMode: AppThemeMode = AppThemeMode.DEFAULT,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.YELLOW -> YellowColorScheme
        AppThemeMode.GREEN -> GreenColorScheme
        AppThemeMode.DARK -> DarkColorScheme // 强制夜间
        AppThemeMode.DEFAULT -> {
            // 跟随系统
            if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
        }
    }

    // 状态栏颜色适配
    val isSystemDark = isSystemInDarkTheme()
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()

            val isDark = themeMode == AppThemeMode.DARK || (themeMode == AppThemeMode.DEFAULT && isSystemDark)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}