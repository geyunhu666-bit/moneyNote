package com.witsky.moneynote.data

import com.witsky.moneynote.data.local.entity.TransactionEntity
import com.witsky.moneynote.data.local.entity.TransactionType
import java.time.YearMonth

/**
 * 某个月的账目快照。
 *
 * 刻意做成"纯函数产物"：不碰数据库、不碰 Android，因此可以直接用 JVM 单元测试锁住
 * 收支汇总与按日分组这两处最容易算错的逻辑。
 */
data class MonthSnapshot(
  val yearMonth: YearMonth,
  /** 当月全部账目，已按日期与时间倒序。 */
  val transactions: List<TransactionEntity>,
  val income: Long,
  val expense: Long,
  val expenseByCategory: Map<Long, Long>,
  val incomeByCategory: Map<Long, Long>,
) {
  /** 结余，单位：分。 */
  val balance: Long get() = income - expense

  fun amountOf(categoryId: Long, kind: TransactionType): Long =
    when (kind) {
      TransactionType.EXPENSE -> expenseByCategory[categoryId] ?: 0L
      TransactionType.INCOME -> incomeByCategory[categoryId] ?: 0L
      TransactionType.TRANSFER -> 0L
    }
}

/** 同一天的账目与当天小计。 */
data class DayGroup(
  val epochDay: Long,
  val transactions: List<TransactionEntity>,
  val expense: Long,
  val income: Long,
)

fun buildMonthSnapshot(yearMonth: YearMonth, all: List<TransactionEntity>): MonthSnapshot {
  val firstDay = yearMonth.atDay(1).toEpochDay()
  val lastDay = yearMonth.atEndOfMonth().toEpochDay()

  val inMonth = all
    .filter { it.epochDay in firstDay..lastDay }
    .sortedWith(compareByDescending<TransactionEntity> { it.epochDay }
      .thenByDescending { it.minuteOfDay }
      .thenByDescending { it.id })

  var income = 0L
  var expense = 0L
  val expenseByCategory = mutableMapOf<Long, Long>()
  val incomeByCategory = mutableMapOf<Long, Long>()

  for (txn in inMonth) {
    val categoryId = txn.categoryId
    when (txn.type) {
      TransactionType.EXPENSE -> {
        expense += txn.amount
        if (categoryId != null) expenseByCategory.merge(categoryId, txn.amount, Long::plus)
      }
      TransactionType.INCOME -> {
        income += txn.amount
        if (categoryId != null) incomeByCategory.merge(categoryId, txn.amount, Long::plus)
      }
      // 转账只是账户之间搬钱，不是收入也不是支出，因此不计入收支统计。
      // 注意：转账手续费只从账户余额里扣，不进"支出"——这是刻意的口径，改口径要同时改这里。
      TransactionType.TRANSFER -> Unit
    }
  }

  return MonthSnapshot(
    yearMonth = yearMonth,
    transactions = inMonth,
    income = income,
    expense = expense,
    expenseByCategory = expenseByCategory,
    incomeByCategory = incomeByCategory,
  )
}

/** 按天分组，保持传入顺序（已倒序），因此最新的一天在最上面。 */
fun MonthSnapshot.groupByDay(): List<DayGroup> =
  transactions.groupBy { it.epochDay }
    .map { (day, list) ->
      DayGroup(
        epochDay = day,
        transactions = list,
        expense = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
        income = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
      )
    }
    .sortedByDescending { it.epochDay }
