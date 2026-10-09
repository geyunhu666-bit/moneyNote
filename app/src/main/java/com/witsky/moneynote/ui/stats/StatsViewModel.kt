package com.witsky.moneynote.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.buildMonthSnapshot
import com.witsky.moneynote.data.local.entity.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class CategorySlice(
  val categoryId: Long,
  val name: String,
  val icon: String,
  val amount: Long,
)

data class StatsUiState(
  val month: YearMonth,
  val kind: TransactionType,
  val income: Long,
  val expense: Long,
  val slices: List<CategorySlice>,
) {
  val balance: Long get() = income - expense
  val total: Long get() = slices.sumOf { it.amount }
}

class StatsViewModel(private val repository: BookkeepingRepository) : ViewModel() {

  private val month = MutableStateFlow(YearMonth.now())
  private val kind = MutableStateFlow(TransactionType.EXPENSE)

  val uiState: StateFlow<StatsUiState?> = combine(
    repository.observeTransactions(),
    repository.observeCategories(),
    month,
    kind,
  ) { transactions, categories, selectedMonth, selectedKind ->
    val snapshot = buildMonthSnapshot(selectedMonth, transactions)
    val byCategory = when (selectedKind) {
      TransactionType.INCOME -> snapshot.incomeByCategory
      else -> snapshot.expenseByCategory
    }

    // 按一级分类聚合。用户想看的是"吃饭占了多少"，不是"早餐占了多少"，
    // 若按叶子分类直接画，41 个分类的饼图没人看得懂。
    val parentOf = categories.associate { it.id to (it.parentId ?: it.id) }
    val aggregated = mutableMapOf<Long, Long>()
    byCategory.forEach { (categoryId, amount) ->
      aggregated.merge(parentOf[categoryId] ?: categoryId, amount, Long::plus)
    }

    val slices = aggregated.entries
      .sortedByDescending { it.value }
      .map { (categoryId, amount) ->
        val category = categories.firstOrNull { it.id == categoryId }
        CategorySlice(
          categoryId = categoryId,
          name = category?.name ?: "已删除分类",
          icon = category?.icon ?: "❓",
          amount = amount,
        )
      }

    StatsUiState(
      month = selectedMonth,
      kind = selectedKind,
      income = snapshot.income,
      expense = snapshot.expense,
      slices = slices,
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  fun previousMonth() {
    month.value = month.value.minusMonths(1)
  }

  fun nextMonth() {
    month.value = month.value.plusMonths(1)
  }

  fun selectKind(value: TransactionType) {
    if (value == TransactionType.TRANSFER) return
    kind.value = value
  }
}
