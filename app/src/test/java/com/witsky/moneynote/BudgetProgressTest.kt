package com.witsky.moneynote

import com.witsky.moneynote.data.buildMonthSnapshot
import com.witsky.moneynote.data.budgetKeyToYearMonth
import com.witsky.moneynote.data.budgetableCategories
import com.witsky.moneynote.data.childIdsOf
import com.witsky.moneynote.data.local.entity.CategoryKind
import com.witsky.moneynote.data.local.entity.TransactionType
import com.witsky.moneynote.data.monthSpentIn
import com.witsky.moneynote.data.toBudgetKey
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class BudgetProgressTest {

  private val october = YearMonth.of(2026, 10)

  @Test
  fun `年月与预算表 key 互转`() {
    assertEquals(202610, october.toBudgetKey())
    assertEquals(october, budgetKeyToYearMonth(202610))
    // 单位数月份不能因为缺前导零而串位
    assertEquals(202601, YearMonth.of(2026, 1).toBudgetKey())
    assertEquals(YearMonth.of(2026, 1), budgetKeyToYearMonth(202601))
  }

  @Test
  fun `分类支出要把子分类一起算进去`() {
    val categories = listOf(
      category(1L, "餐饮", CategoryKind.EXPENSE),
      category(11L, "早餐", CategoryKind.EXPENSE, parentId = 1L),
      category(12L, "午餐", CategoryKind.EXPENSE, parentId = 1L),
      category(2L, "交通", CategoryKind.EXPENSE),
    )
    val rows = listOf(
      txn(1L, TransactionType.EXPENSE, 500L, dayOf(2026, 10, 5), categoryId = 1L),
      txn(2L, TransactionType.EXPENSE, 300L, dayOf(2026, 10, 6), categoryId = 11L),
      txn(3L, TransactionType.EXPENSE, 700L, dayOf(2026, 10, 7), categoryId = 12L),
      txn(4L, TransactionType.EXPENSE, 200L, dayOf(2026, 10, 8), categoryId = 2L),
      // 收入不该被算进支出
      txn(5L, TransactionType.INCOME, 9999L, dayOf(2026, 10, 9), categoryId = 1L),
    )
    val snapshot = buildMonthSnapshot(october, rows)

    val dinning = monthSpentIn(snapshot, 1L, categories.childIdsOf(1L))
    assertEquals(1500L, dinning)

    val transport = monthSpentIn(snapshot, 2L, categories.childIdsOf(2L))
    assertEquals(200L, transport)

    // 没花过钱的分类返回 0，而不是 null 或抛异常
    assertEquals(0L, monthSpentIn(snapshot, 99L, emptyList()))
  }

  @Test
  fun `只有支出类的一级分类可以设预算`() {
    val categories = listOf(
      category(1L, "餐饮", CategoryKind.EXPENSE),
      category(11L, "早餐", CategoryKind.EXPENSE, parentId = 1L),
      category(2L, "交通", CategoryKind.EXPENSE),
      category(5L, "工资", CategoryKind.INCOME),
    )

    val result = categories.budgetableCategories().map { it.name }

    assertEquals(listOf("餐饮", "交通"), result)
  }

  @Test
  fun `子分类查询只认直接子级`() {
    val categories = listOf(
      category(1L, "餐饮", CategoryKind.EXPENSE),
      category(11L, "早餐", CategoryKind.EXPENSE, parentId = 1L),
      category(12L, "午餐", CategoryKind.EXPENSE, parentId = 1L),
      category(2L, "交通", CategoryKind.EXPENSE),
    )

    assertEquals(listOf(11L, 12L), categories.childIdsOf(1L))
    assertEquals(emptyList<Long>(), categories.childIdsOf(2L))
  }
}
