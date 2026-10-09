package com.witsky.moneynote.ui.common

import androidx.compose.ui.graphics.Color

/** 收支的颜色语义只在这里定义一次，避免各页面各写一套红绿。 */
val ExpenseColor = Color(0xFFD0483A)
val IncomeColor = Color(0xFF2E8B57)
val TransferColor = Color(0xFF3A6FB0)

/** 接近上限的橙色，用于预算进度预警。 */
val WarningColor = Color(0xFFE8A33D)

/**
 * 预算进度的配色：超支红、用掉八成以上橙、其余绿。
 * 流水页的预算条与预算页共用这一处判断，避免同一个数字在两处显示成不同颜色。
 */
fun budgetColor(fraction: Float): Color = when {
  fraction > 1f -> ExpenseColor
  fraction >= 0.8f -> WarningColor
  else -> IncomeColor
}

/** 图表用的分类配色，按下标循环取用。 */
val ChartColors = listOf(
  Color(0xFF5B8FF9),
  Color(0xFF61DDAA),
  Color(0xFFF6BD16),
  Color(0xFF7262FD),
  Color(0xFF78D3F8),
  Color(0xFF9661BC),
  Color(0xFFF6903D),
  Color(0xFF008685),
  Color(0xFFF08BB4),
  Color(0xFFE8684A),
)

fun chartColorAt(index: Int): Color = ChartColors[index.mod(ChartColors.size)]
