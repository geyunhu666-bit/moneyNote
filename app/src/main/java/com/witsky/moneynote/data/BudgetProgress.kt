package com.witsky.moneynote.data

import com.witsky.moneynote.data.local.entity.CategoryEntity
import com.witsky.moneynote.data.local.entity.CategoryKind
import java.time.YearMonth

/** 预算表用 Int 存年月（如 202610），比字符串更容易建索引、做范围查询。 */
fun YearMonth.toBudgetKey(): Int = year * 100 + monthValue

fun budgetKeyToYearMonth(key: Int): YearMonth = YearMonth.of(key / 100, key % 100)

/**
 * 某个分类（含其全部子分类）在当月的实际支出，单位：分。
 *
 * 预算必须与统计保持同一口径：用户给「餐饮」设了预算，期望的是早餐/午餐/晚餐都算进餐饮，
 * 而不是只算直接挂在一级分类上的那几笔。
 */
fun monthSpentIn(snapshot: MonthSnapshot, categoryId: Long, childIds: List<Long>): Long {
  var total = snapshot.expenseByCategory[categoryId] ?: 0L
  for (child in childIds) total += snapshot.expenseByCategory[child] ?: 0L
  return total
}

/**
 * 可以设置预算的分类：只列支出类的一级分类。
 *
 * 二级分类不单独设预算。否则「餐饮 1000」和「午餐 300」会同时存在两个互相矛盾的上限，
 * 用户看到哪个超支都不清楚该怪谁。
 */
fun List<CategoryEntity>.budgetableCategories(): List<CategoryEntity> =
  filter { it.kind == CategoryKind.EXPENSE && it.parentId == null }
    .sortedWith(compareBy({ it.sortOrder }, { it.id }))

fun List<CategoryEntity>.childIdsOf(parentId: Long): List<Long> =
  filter { it.parentId == parentId }.map { it.id }
