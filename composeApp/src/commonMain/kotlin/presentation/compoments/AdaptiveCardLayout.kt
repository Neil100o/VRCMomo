package io.github.vrcmteam.vrcm.presentation.compoments

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 卡片列表的宽度分级。
 *
 * 阈值沿用 VRCM 的 Compact / Medium / Expanded 思路，但卡片列表在超宽窗口
 * 继续增加一列，避免平板和桌面模拟器上出现两张被过度拉伸的大卡片。
 */
fun mediaCardColumnCount(availableWidth: Dp): Int = mediaCardColumnCount(availableWidth.value)

internal fun mediaCardColumnCount(availableWidthDp: Float): Int = when {
    availableWidthDp >= 1080f -> 4
    availableWidthDp >= 840f -> 3
    else -> 2
}

fun friendCardColumnCount(availableWidth: Dp): Int = friendCardColumnCount(availableWidth.value)

internal fun friendCardColumnCount(availableWidthDp: Float): Int = when {
    availableWidthDp >= 1080f -> 4
    availableWidthDp >= 840f -> 3
    availableWidthDp >= 520f -> 2
    else -> 1
}
