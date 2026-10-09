package com.witsky.moneynote

import com.witsky.moneynote.data.DateRangeOption
import com.witsky.moneynote.data.SearchFilters
import com.witsky.moneynote.data.filterBy
import com.witsky.moneynote.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SearchFilterTest {

  private val dinning = txn(1L, TransactionType.EXPENSE, 1200L, dayOf(2026, 10, 5), categoryId = 1L, note = "和同事午饭")
  private val transport = txn(2L, TransactionType.EXPENSE, 300L, dayOf(2026, 10, 6), categoryId = 2L, accountId = 2L)
  private val salary = txn(3L, TransactionType.INCOME, 800000L, dayOf(2026, 10, 10), categoryId = 9L)
  private val moveToBank = txn(
    4L, TransactionType.TRANSFER, 50000L, dayOf(2026, 10, 11),
    accountId = 2L, toAccountId = 3L, note = "还信用卡",
  )

  private val all = listOf(dinning, transport, salary, moveToBank)
  private val categoryNames = mapOf(1L to "餐饮", 2L to "交通", 9L to "工资")
  private val accountNames = mapOf(1L to "现金", 2L to "招行卡", 3L to "信用卡")

  private fun search(filters: SearchFilters) = all.filterBy(filters, categoryNames, accountNames).map { it.id }

  @Test
  fun `默认条件返回全部`() {
    val filters = SearchFilters()
    assertTrue(filters.isUnfiltered)
    assertEquals(listOf(1L, 2L, 3L, 4L), search(filters))
  }

  @Test
  fun `关键词命中备注`() {
    assertEquals(listOf(1L), search(SearchFilters(keyword = "午饭")))
  }

  @Test
  fun `关键词也能命中分类名与账户名`() {
    // 搜"餐饮"要能找到这个分类下的账，哪怕备注里没写这两个字
    assertEquals(listOf(1L), search(SearchFilters(keyword = "餐饮")))
    assertEquals(listOf(2L, 4L), search(SearchFilters(keyword = "招行")))
  }

  @Test
  fun `关键词大小写不敏感且忽略首尾空格`() {
    val upper = txn(9L, TransactionType.EXPENSE, 100L, dayOf(2026, 10, 5), categoryId = 1L, note = "Taxi")
    val result = (all + upper).filterBy(SearchFilters(keyword = "  taxi "), categoryNames, accountNames)

    assertEquals(listOf(9L), result.map { it.id })
  }

  @Test
  fun `按类型筛选`() {
    assertEquals(listOf(1L, 2L), search(SearchFilters(type = TransactionType.EXPENSE)))
    assertEquals(listOf(3L), search(SearchFilters(type = TransactionType.INCOME)))
    assertEquals(listOf(4L), search(SearchFilters(type = TransactionType.TRANSFER)))
  }

  @Test
  fun `按分类筛选`() {
    assertEquals(listOf(2L), search(SearchFilters(categoryId = 2L)))
  }

  @Test
  fun `按账户筛选时转账的两端都算`() {
    // 从招行卡转出，搜招行卡要能看到；转入信用卡，搜信用卡也要能看到
    assertEquals(listOf(2L, 4L), search(SearchFilters(accountId = 2L)))
    assertEquals(listOf(4L), search(SearchFilters(accountId = 3L)))
  }

  @Test
  fun `按金额区间筛选`() {
    // 300(交通) 与 1200(餐饮) 落在区间内；50000 的转账与 800000 的工资都超了
    assertEquals(listOf(1L, 2L), search(SearchFilters(minAmount = 300L, maxAmount = 1200L)))
    // 只给下限时：1200(餐饮)、50000(转账)、800000(工资) 都够，300(交通) 不够
    assertEquals(listOf(1L, 3L, 4L), search(SearchFilters(minAmount = 1000L)))
  }

  @Test
  fun `按日期区间筛选`() {
    val filters = SearchFilters(startDay = dayOf(2026, 10, 6), endDay = dayOf(2026, 10, 10))
    assertEquals(listOf(2L, 3L), search(filters))
  }

  @Test
  fun `条件之间是与关系`() {
    val filters = SearchFilters(
      type = TransactionType.EXPENSE,
      accountId = 2L,
      minAmount = 100L,
    )
    assertEquals(listOf(2L), search(filters))
  }

  @Test
  fun `条件不满足时返回空列表而不是报错`() {
    assertEquals(emptyList<Long>(), search(SearchFilters(keyword = "不存在的词")))
  }

  @Test
  fun `时间范围快捷选项换算成日期区间`() {
    val today = LocalDate.of(2026, 10, 9)

    assertEquals(null to null, DateRangeOption.ALL.dayRange(today))

    val (thisStart, thisEnd) = DateRangeOption.THIS_MONTH.dayRange(today)
    assertEquals(LocalDate.of(2026, 10, 1).toEpochDay(), thisStart)
    assertEquals(today.toEpochDay(), thisEnd)

    // 上月要整月覆盖，跨月边界（9/30）不能漏
    val (lastStart, lastEnd) = DateRangeOption.LAST_MONTH.dayRange(today)
    assertEquals(LocalDate.of(2026, 9, 1).toEpochDay(), lastStart)
    assertEquals(LocalDate.of(2026, 9, 30).toEpochDay(), lastEnd)

    // 近三月含当月，所以从 8/1 起
    val (threeStart, threeEnd) = DateRangeOption.LAST_THREE_MONTHS.dayRange(today)
    assertEquals(LocalDate.of(2026, 8, 1).toEpochDay(), threeStart)
    assertEquals(today.toEpochDay(), threeEnd)
  }

  @Test
  fun `一月时上月范围会跨到上一年`() {
    val today = LocalDate.of(2026, 1, 15)

    val (start, end) = DateRangeOption.LAST_MONTH.dayRange(today)

    assertEquals(LocalDate.of(2025, 12, 1).toEpochDay(), start)
    assertEquals(LocalDate.of(2025, 12, 31).toEpochDay(), end)
  }
}
