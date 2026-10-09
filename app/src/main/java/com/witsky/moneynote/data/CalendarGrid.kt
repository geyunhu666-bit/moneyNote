package com.witsky.moneynote.data

import java.time.YearMonth

/** 月历里的一格。月外日期不进网格，所以不需要「是否属于本月」这种标记。 */
data class CalendarDay(
  val epochDay: Long,
  val dayOfMonth: Int,
  val expense: Long,
  val income: Long,
  val hasTransactions: Boolean,
)

/**
 * 生成月历网格：每行 7 格，周一在第一列。
 *
 * 前后用 null 补空格，而不是塞上月外日期：这样界面上不存在「点到别的月份的日子」
 * 这种需要额外判断的边界情况。
 *
 * 行数按实际需要生成（4~6 行），不强行凑满 6 行 —— 空白行只会浪费屏幕高度。
 */
fun buildCalendarWeeks(month: YearMonth, snapshot: MonthSnapshot): List<List<CalendarDay?>> {
  val byDay = snapshot.groupByDay().associateBy { it.epochDay }
  // DayOfWeek.value 是 1(周一)..7(周日)，减 1 正好是周一开头时要补的空格数。
  val leadingBlanks = month.atDay(1).dayOfWeek.value - 1

  val cells = mutableListOf<CalendarDay?>()
  repeat(leadingBlanks) { cells += null }
  for (day in 1..month.lengthOfMonth()) {
    val epochDay = month.atDay(day).toEpochDay()
    val group = byDay[epochDay]
    cells += CalendarDay(
      epochDay = epochDay,
      dayOfMonth = day,
      expense = group?.expense ?: 0L,
      income = group?.income ?: 0L,
      hasTransactions = group != null,
    )
  }
  while (cells.size % 7 != 0) cells += null

  return cells.chunked(7)
}
