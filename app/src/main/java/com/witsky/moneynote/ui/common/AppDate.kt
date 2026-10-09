package com.witsky.moneynote.ui.common

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * 日期工具。账目里日期存 epochDay、时刻存 minuteOfDay，这里只负责与人类可读文本互转。
 * minSdk 26 起 java.time 可直接用，无需 desugaring。
 */
object AppDate {

  private val weekdayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

  fun today(): Long = LocalDate.now().toEpochDay()

  fun currentMinuteOfDay(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

  fun toDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

  fun toYearMonth(epochDay: Long): YearMonth = YearMonth.from(toDate(epochDay))

  fun firstDayOf(month: YearMonth): Long = month.atDay(1).toEpochDay()

  fun lastDayOf(month: YearMonth): Long = month.atEndOfMonth().toEpochDay()

  /** "10月09日 周五"，今天与昨天额外标注。 */
  fun formatDay(epochDay: Long): String {
    val date = toDate(epochDay)
    val label = "${date.monthValue}月${date.dayOfMonth.toString().padStart(2, '0')}日 " +
      weekdayNames[date.dayOfWeek.value - 1]
    return when (epochDay) {
      today() -> "今天 · $label"
      today() - 1 -> "昨天 · $label"
      else -> label
    }
  }

  /** "2026年10月"。 */
  fun formatMonth(month: YearMonth): String = "${month.year}年${month.monthValue}月"

  /** minuteOfDay -> "14:05"。 */
  fun formatMinuteOfDay(minuteOfDay: Int): String {
    val safe = minuteOfDay.coerceIn(0, 24 * 60 - 1)
    return "${(safe / 60).toString().padStart(2, '0')}:${(safe % 60).toString().padStart(2, '0')}"
  }

  fun monthLabel(epochDay: Long): String = formatMonth(toYearMonth(epochDay))

  /**
   * 把某个真实时刻转成"年月日 + 当天第几分钟"。日期选择器返回的是 UTC 毫秒，
   * 直接 toEpochDay 会因时区偏移错一天，所以必须经由 LocalDate 转换。
   */
  fun epochDayFromUtcMillis(utcMillis: Long): Long =
    java.time.Instant.ofEpochMilli(utcMillis)
      .atZone(java.time.ZoneId.systemDefault())
      .toLocalDate()
      .toEpochDay()

  /** 日期选择器需要的反向转换：当天本地零点对应的 UTC 毫秒。 */
  fun utcMillisFromEpochDay(epochDay: Long): Long =
    toDate(epochDay)
      .atStartOfDay(java.time.ZoneId.systemDefault())
      .toInstant()
      .toEpochMilli()
}
