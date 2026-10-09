package com.witsky.moneynote

import com.witsky.moneynote.data.buildCalendarWeeks
import com.witsky.moneynote.data.buildMonthSnapshot
import com.witsky.moneynote.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class CalendarGridTest {

  /** 2026-01-01 是周四（由 2000-01-01 是周六推算），所以这一列正好能验出"周一开头"的补格数。 */
  private val january = YearMonth.of(2026, 1)

  private fun weeksOf(month: YearMonth, transactions: List<com.witsky.moneynote.data.local.entity.TransactionEntity> = emptyList()) =
    buildCalendarWeeks(month, buildMonthSnapshot(month, transactions))

  @Test
  fun `每周固定七格且行数按需`() {
    val weeks = weeksOf(january)

    assertTrue(weeks.all { it.size == 7 })
    // 周四开头补 3 格，31 天 => 34 格 => 5 行；不应该硬凑 6 行留一堆空白
    assertEquals(5, weeks.size)
  }

  @Test
  fun `一月一日落在周四那一列`() {
    val weeks = weeksOf(january)

    assertNull(weeks[0][0])
    assertNull(weeks[0][1])
    assertNull(weeks[0][2])
    assertEquals(1, weeks[0][3]?.dayOfMonth)
  }

  @Test
  fun `日期连续且天数与当月一致`() {
    val days = weeksOf(january).flatten().filterNotNull().map { it.dayOfMonth }

    assertEquals(31, days.size)
    assertEquals((1..31).toList(), days)
  }

  @Test
  fun `月初就是周一时不补空格`() {
    // 2026-06-01 是周一：网格第一格应当直接是 1 号
    val weeks = weeksOf(YearMonth.of(2026, 6))

    assertEquals(1, weeks[0][0]?.dayOfMonth)
    assertEquals(30, weeks.flatten().count { it != null })
  }

  @Test
  fun `格子带上当天收支且没账目的日子为零`() {
    val month = YearMonth.of(2026, 10)
    val rows = listOf(
      txn(1L, TransactionType.EXPENSE, 1200L, dayOf(2026, 10, 5), categoryId = 1L),
      txn(2L, TransactionType.INCOME, 800L, dayOf(2026, 10, 5), categoryId = 9L),
      txn(3L, TransactionType.EXPENSE, 300L, dayOf(2026, 10, 6), categoryId = 1L),
    )

    val days = buildCalendarWeeks(month, buildMonthSnapshot(month, rows)).flatten().filterNotNull()

    val fifth = days.first { it.dayOfMonth == 5 }
    assertEquals(1200L, fifth.expense)
    assertEquals(800L, fifth.income)
    assertTrue(fifth.hasTransactions)

    val sixth = days.first { it.dayOfMonth == 6 }
    assertEquals(300L, sixth.expense)
    assertEquals(0L, sixth.income)

    val seventh = days.first { it.dayOfMonth == 7 }
    assertEquals(0L, seventh.expense)
    assertTrue(!seventh.hasTransactions)
  }

  @Test
  fun `上个月的账目不会漏进本月格子`() {
    val month = YearMonth.of(2026, 10)
    val rows = listOf(txn(1L, TransactionType.EXPENSE, 999L, dayOf(2026, 9, 30), categoryId = 1L))

    val days = buildCalendarWeeks(month, buildMonthSnapshot(month, rows)).flatten().filterNotNull()

    assertEquals(0L, days.sumOf { it.expense })
  }
}
