package com.example.appjardin.model

import androidx.compose.ui.graphics.Color
import com.example.appjardin.R
import com.example.appjardin.ui.theme.ColorVerdeAlegre

const val FIXED_HEADER_ALPHA = 0.52f

enum class AppTheme(
    val id: String,
    val titleRes: Int,
    val assetPath: String?,
    val headerColorFixed: Color?,
    val buttonColorFixed: Color?,
    val buttonContentColor: Color = Color.White,
    val isDynamic: Boolean = false,
    val navSelectedTextColor: Color = Color.White,
    val navIndicatorColor: Color = Color.White.copy(alpha = 0.28f),
    val navUnselectedColor: Color = Color.White.copy(alpha = 0.85f)
) {
    SELVA(
        id = "selva",
        titleRes = R.string.theme_selva,
        assetPath = "img/fe2.png",
        headerColorFixed = ColorVerdeAlegre.copy(alpha = FIXED_HEADER_ALPHA),
        buttonColorFixed = ColorVerdeAlegre,
        isDynamic = false
    ),
    ATARDECER(
        id = "atardecer",
        titleRes = R.string.theme_atardecer,
        assetPath = "img/at.png",
        headerColorFixed = Color(0xFFE65100).copy(alpha = FIXED_HEADER_ALPHA),
        buttonColorFixed = Color(0xFFE65100),
        isDynamic = false
    ),
    ANOCHECER(
        id = "anochecer",
        titleRes = R.string.theme_anochecer,
        assetPath = "img/an.png",
        headerColorFixed = Color(0xFF1A237E).copy(alpha = FIXED_HEADER_ALPHA),
        buttonColorFixed = Color(0xFF1A237E),
        isDynamic = false
    ),
    PLAYA(
        id = "playa",
        titleRes = R.string.theme_playa,
        assetPath = "img/pl.png",
        headerColorFixed = Color(0xFF00838F).copy(alpha = FIXED_HEADER_ALPHA),
        buttonColorFixed = Color(0xFF00838F),
        isDynamic = false
    ),
    SEMAFORO(
        id = "semaforo",
        titleRes = R.string.theme_semaforo,
        assetPath = null,
        headerColorFixed = null,
        buttonColorFixed = null,
        isDynamic = true
    );

    fun getHeaderColor(activeColor: Color): Color {
        return if (isDynamic) activeColor else (headerColorFixed ?: ColorVerdeAlegre.copy(alpha = FIXED_HEADER_ALPHA))
    }

    fun getButtonColor(activeColor: Color): Color {
        return if (isDynamic) activeColor else (buttonColorFixed ?: ColorVerdeAlegre)
    }

    companion object {
        fun fromId(id: String?): AppTheme {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: SELVA
        }
    }
}

data class BottomBarColors(
    val containerColor: Color,
    val estadoSelectedIconColor: Color,
    val estadoSelectedTextColor: Color,
    val estadoIndicatorColor: Color,
    val estadoUnselectedIconColor: Color,
    val estadoUnselectedTextColor: Color,
    val otherSelectedIconColor: Color,
    val otherSelectedTextColor: Color,
    val otherIndicatorColor: Color,
    val otherUnselectedIconColor: Color,
    val otherUnselectedTextColor: Color
)

fun computeBottomBarColors(
    theme: AppTheme,
    currentRoute: String?,
    activeColor: Color
): BottomBarColors {
    val isMainActive = currentRoute == "main"

    if (!isMainActive) {
        return BottomBarColors(
            containerColor = Color.White,
            estadoSelectedIconColor = Color.White,
            estadoSelectedTextColor = ColorVerdeAlegre,
            estadoIndicatorColor = ColorVerdeAlegre,
            estadoUnselectedIconColor = Color.Gray,
            estadoUnselectedTextColor = Color.Gray,
            otherSelectedIconColor = Color.White,
            otherSelectedTextColor = activeColor,
            otherIndicatorColor = activeColor,
            otherUnselectedIconColor = Color.Gray,
            otherUnselectedTextColor = Color.Gray
        )
    }

    return when (theme) {
        AppTheme.SELVA -> BottomBarColors(
            containerColor = Color.White,
            estadoSelectedIconColor = Color.White,
            estadoSelectedTextColor = ColorVerdeAlegre,
            estadoIndicatorColor = ColorVerdeAlegre,
            estadoUnselectedIconColor = Color.Gray,
            estadoUnselectedTextColor = Color.Gray,
            otherSelectedIconColor = Color.White,
            otherSelectedTextColor = activeColor,
            otherIndicatorColor = activeColor,
            otherUnselectedIconColor = Color.Gray,
            otherUnselectedTextColor = Color.Gray
        )
        AppTheme.ATARDECER, AppTheme.ANOCHECER, AppTheme.PLAYA -> {
            val barBackground = theme.buttonColorFixed ?: Color.White
            val selectedTextColor = theme.navSelectedTextColor
            val indicatorColor = theme.navIndicatorColor
            val unselectedColor = theme.navUnselectedColor

            BottomBarColors(
                containerColor = barBackground,
                estadoSelectedIconColor = Color.White,
                estadoSelectedTextColor = selectedTextColor,
                estadoIndicatorColor = indicatorColor,
                estadoUnselectedIconColor = unselectedColor,
                estadoUnselectedTextColor = unselectedColor,
                otherSelectedIconColor = Color.White,
                otherSelectedTextColor = activeColor,
                otherIndicatorColor = activeColor,
                otherUnselectedIconColor = unselectedColor,
                otherUnselectedTextColor = unselectedColor
            )
        }
        AppTheme.SEMAFORO -> BottomBarColors(
            containerColor = Color.White,
            estadoSelectedIconColor = Color.White,
            estadoSelectedTextColor = activeColor,
            estadoIndicatorColor = activeColor,
            estadoUnselectedIconColor = Color.Gray,
            estadoUnselectedTextColor = Color.Gray,
            otherSelectedIconColor = Color.White,
            otherSelectedTextColor = activeColor,
            otherIndicatorColor = activeColor,
            otherUnselectedIconColor = Color.Gray,
            otherUnselectedTextColor = Color.Gray
        )
    }
}
