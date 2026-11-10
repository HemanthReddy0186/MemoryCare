package com.memorycare.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memorycare.app.model.UserRole

/**
 * Role-based UI dimensions and typography scale.
 * Patient view: Extra-large typography (24sp-34sp), huge touch targets (64dp-80dp+), high contrast.
 * Caretaker view: Compact typography (13sp-22sp), efficient data layout (44dp-52dp).
 */
data class RoleUiDimensions(
    val titleSize: TextUnit,
    val headingSize: TextUnit,
    val bodySize: TextUnit,
    val secondarySize: TextUnit,
    val buttonTextSize: TextUnit,
    val minButtonHeight: Dp,
    val cardPadding: Dp,
    val cornerRadius: Dp,
    val iconSize: Dp,
    val gameCardSize: Dp
)

object UiScale {
    val Patient = RoleUiDimensions(
        titleSize = 32.sp,
        headingSize = 26.sp,
        bodySize = 22.sp,
        secondarySize = 18.sp,
        buttonTextSize = 24.sp,
        minButtonHeight = 68.dp,
        cardPadding = 20.dp,
        cornerRadius = 22.dp,
        iconSize = 44.dp,
        gameCardSize = 88.dp
    )

    val Caretaker = RoleUiDimensions(
        titleSize = 22.sp,
        headingSize = 18.sp,
        bodySize = 15.sp,
        secondarySize = 13.sp,
        buttonTextSize = 15.sp,
        minButtonHeight = 48.dp,
        cardPadding = 14.dp,
        cornerRadius = 14.dp,
        iconSize = 28.dp,
        gameCardSize = 64.dp
    )

    fun forRole(role: UserRole): RoleUiDimensions {
        return if (role == UserRole.CARETAKER) Caretaker else Patient
    }
}
