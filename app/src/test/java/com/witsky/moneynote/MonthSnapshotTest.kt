package com.witsky.moneynote

import com.witsky.moneynote.data.buildMonthSnapshot
import com.witsky.moneynote.data.groupByDay
import com.witsky.moneynote.data.local.entity.TransactionEntity
import com.witsky.moneynote.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthSnapshotTest {

  private val october = YearMonth.of(2026, 10)

  private fun day(dayOfMonth: Int): Long = LocalDate.of(2026, 10, dayOfMonth).toEpochDay()

  private fun expense(cents: Long, categoryId: Long, dayOfMonth: Int, minute: Int = 600) =
    TransactionEntity(
      type = TransactionType.EXPENSE,
      amount = cents,
      categoryId = categoryId,
      accountId = 1L,
      epochDay = day(dayOfMonth),
      minuteOfDay = minute,
    )

  private fun income(cents: Long, categoryId: Long, dayOfMonth: Int) =
    TransactionEntity(
      type = TransactionType.INCOME,
      amount = cents,
      categoryId = categoryId,
      accountId = 2L,
      epochDay = day(dayOfMonth),
      minuteOfDay = 600,
    )

  private fun transfer(cents: Long, dayOfMonth: Int) =
    TransactionEntity(
      type = TransactionType.TRANSFER,
      amount = cents,
      categoryId = null,
      accountId = 2L,
      toAccountId = 3L,
      epochDay = day(dayOfMonth),
      minuteOfDay = 600,
    )

  @Test
  fun `只统计当月的账目`() {
    val rows = listOf(
      expense(1000L, 1L, 5),
      income(5000L, 10L, 6),
      // 9 月与 11 月都不该进来
      TransactionEntity(
        type = TransactionType.EXPENSE, amount = 999L, categoryId = 1L, accountId = 1L,
        epochDay = LocalDate.of(2026, 9, 30).toEpochDay(), minuteOfDay = 0,
      ),
      TransactionEntity(
        type = TransactionType.EXPENSE, amount = 888L, categoryId = 1L, accountId = 1L,
        epochDay = LocalDate.of(2026, 11, 1).toEpochDay(), minuteOfDay = 0,
      ),
    )

    val snapshot = buildMonthSnapshot(october, rows)

    assertEquals(2, snapshot.transactions.size)
    assertEquals(1000L, snapshot.expense)
    assertEquals(5000L, snapshot.income)
    assertEquals(4000L, snapshot.balance)
  }

  @Test
  fun `转账不计入收支但仍保留在流水里`() {
    val rows = listOf(expense(1000L, 1L, 5), transfer(3000L, 7))

    val snapshot = buildMonthSnapshot(october, rows)

    assertEquals(1000L, snapshot.expense)
    assertEquals(0L, snapshot.income)
    assertEquals(2, snapshot.transactions.size)
  }

  @Test
  fun `按分类累计支出`() {
    val rows = listOf(
      expense(1000L, 1L, 5),
      expense(2000L, 1L, 6),
      expense(500L, 2L, 6),
    )

    val snapshot = buildMonthSnapshot(october, rows)

    assertEquals(3000L, snapshot.expenseByCategory[1L])
    assertEquals(500L, snapshot.expenseByCategory[2L])
    assertEquals(3500L, snapshot.expense)
  }

  @Test
  fun `按日分组并算出当天小计且最新一天在最前`() {
    val rows = listOf(
      expense(1000L, 1L, 5),
      expense(2000L, 1L, 6),
      income(5000L, 10L, 6),
    )

    val groups = buildMonthSnapshot(october, rows).groupByDay()

    assertEquals(2, groups.size)
    assertEquals(day(6), groups[0].epochDay)
    assertEquals(2000L, groups[0].expense)
    assertEquals(5000L, groups[0].income)
    assertEquals(day(5), groups[1].epochDay)
    assertEquals(1000L, groups[1].expense)
    assertEquals(0L, groups[1].income)
  }

  @Test
  fun `同一天内按时间倒序`() {
    val rows = listOf(
      expense(1000L, 1L, 5, minute = 480),
      expense(2000L, 1L, 5, minute = 1200),
    )

    val snapshot = buildMonthSnapshot(october, rows)

    assertEquals(2000L, snapshot.transactions[0].amount)
    assertEquals(1000L, snapshot.transactions[1].amount)
  }

  @Test
  fun `空月份不报错且各计为零`() {
    val snapshot = buildMonthSnapshot(october, emptyList())

    assertEquals(0L, snapshot.expense)
    assertEquals(0L, snapshot.income)
    assertEquals(0L, snapshot.balance)
    assertTrue(snapshot.groupByDay().isEmpty())
  }
}
