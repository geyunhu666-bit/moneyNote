package com.witsky.moneynote.data

import com.witsky.moneynote.data.local.entity.TransactionEntity
import com.witsky.moneynote.data.local.entity.TransactionType
import java.time.LocalDate
import java.time.YearMonth

/**
 * 搜索的时间范围。
 *
 * 做成四个快捷选项而不是两个日期选择器：查账时绝大多数场景是「最近怎么样」，
 * 让用户为了找上周的账去点两次日历是没必要的。真要精确区间，筛选结果还能再排序看。
 */
enum class DateRangeOption(val label: String) {
  ALL("不限"),
  THIS_MONTH("本月"),
  LAST_MONTH("上月"),
  LAST_THREE_MONTHS("近三月"),
  ;

  /** 返回「起, 止」（epochDay），null 表示该端不限。 */
  fun dayRange(today: LocalDate): Pair<Long?, Long?> = when (this) {
    ALL -> null to null
    THIS_MONTH -> YearMonth.from(today).atDay(1).toEpochDay() to today.toEpochDay()
    LAST_MONTH -> YearMonth.from(today).minusMonths(1).let {
      it.atDay(1).toEpochDay() to it.atEndOfMonth().toEpochDay()
    }
    // 含当月，所以往前推两个月。数据本来就只到「今天」，上界不必留空。
    LAST_THREE_MONTHS -> YearMonth.from(today).minusMonths(2).atDay(1).toEpochDay() to today.toEpochDay()
  }
}

/**
 * 搜索条件。所有字段为空/空串即「不限」，因此默认值就是一个什么都不筛的查询，
 * 界面不需要为「清空筛选」写额外分支。
 */
data class SearchFilters(
  val keyword: String = "",
  val type: TransactionType? = null,
  val categoryId: Long? = null,
  val accountId: Long? = null,
  val minAmount: Long? = null,
  val maxAmount: Long? = null,
  val startDay: Long? = null,
  val endDay: Long? = null,
) {
  val isUnfiltered: Boolean
    get() = keyword.isBlank() && type == null && categoryId == null && accountId == null &&
      minAmount == null && maxAmount == null && startDay == null && endDay == null
}

/**
 * 在内存里过滤账目。
 *
 * 个人记账总量在几千条量级，全量扫一遍远快于拼接动态 SQL；而且条件彼此独立，
 * 写成纯函数后每条边界都能直接单测。
 *
 * [categoryName] / [accountName] 让关键词也能命中分类名与账户名：用户搜「餐饮」时想找的
 * 是这个分类下的账，而不是备注里恰好写了「餐饮」两个字的那几笔。
 */
fun List<TransactionEntity>.filterBy(
  filters: SearchFilters,
  categoryName: Map<Long, String>,
  accountName: Map<Long, String>,
): List<TransactionEntity> {
  val keyword = filters.keyword.trim().lowercase()

  return filter { txn ->
    if (filters.type != null && txn.type != filters.type) return@filter false
    if (filters.categoryId != null && txn.categoryId != filters.categoryId) return@filter false
    // 按账户筛选时，转账的两端都算「涉及该账户」，否则从招行转出的钱搜不到。
    if (filters.accountId != null &&
      txn.accountId != filters.accountId &&
      txn.toAccountId != filters.accountId
    ) {
      return@filter false
    }
    if (filters.minAmount != null && txn.amount < filters.minAmount) return@filter false
    if (filters.maxAmount != null && txn.amount > filters.maxAmount) return@filter false
    if (filters.startDay != null && txn.epochDay < filters.startDay) return@filter false
    if (filters.endDay != null && txn.epochDay > filters.endDay) return@filter false

    if (keyword.isNotEmpty()) {
      val haystack = buildString {
        append(txn.note.lowercase())
        append(' ')
        txn.categoryId?.let { append(categoryName[it].orEmpty().lowercase()) }
        append(' ')
        append(accountName[txn.accountId].orEmpty().lowercase())
        txn.toAccountId?.let { append(accountName[it].orEmpty().lowercase()) }
      }
      if (!haystack.contains(keyword)) return@filter false
    }
    true
  }
}
