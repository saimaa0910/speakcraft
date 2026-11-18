package com.example.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AdaptiveWindowTier {
    COMPACT,
    MEDIUM,
    EXPANDED;

    val isTabletOrWide: Boolean
        get() = this != COMPACT

    val isExpandedTablet: Boolean
        get() = this == EXPANDED
}

object SpeakCraftSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val minTouchTarget = 48.dp
    val maxContentWidth = 840.dp
    val maxDashboardWidth = 880.dp
    val maxTabletWidth = 1160.dp
    val maxDialogContentWidth = 520.dp

    fun tierForWidth(availableWidth: Dp): AdaptiveWindowTier = when {
        availableWidth >= 840.dp -> AdaptiveWindowTier.EXPANDED
        availableWidth >= 600.dp -> AdaptiveWindowTier.MEDIUM
        else -> AdaptiveWindowTier.COMPACT
    }

    fun contentMaxWidthFor(tier: AdaptiveWindowTier): Dp = when (tier) {
        AdaptiveWindowTier.EXPANDED -> maxTabletWidth
        AdaptiveWindowTier.MEDIUM -> maxDashboardWidth
        AdaptiveWindowTier.COMPACT -> maxContentWidth
    }

    fun moduleColumnsFor(availableWidth: Dp): Int = when {
        availableWidth >= 900.dp -> 3
        availableWidth >= 520.dp -> 2
        else -> 2
    }
}
