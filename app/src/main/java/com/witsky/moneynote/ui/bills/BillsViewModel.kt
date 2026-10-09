package com.witsky.moneynote.ui.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.DayGroup
import com.witsky.moneynote.data.buildMonthSnapshot
import com.witsky.moneynote.data.groupByDay
import com.witsky.moneynote.data.local.entity.AccountEntity
import com.witsky.moneynote.data.local.entity.BudgetEntity
import com.witsky.moneynote.data.local.entity.CategoryEntity
import com.witsky.moneynote.data.toBudgetKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class BillsUiState(
  val month: YearMonth,
  val dayGroups: List<DayGroup>,
  val income: Long,
  val expense: Long,
  /** 本月总预算；null 表示没设，此时流水页不显示预算条。 */
  val totalBudget: Long?,
  val categoryById: Map<Long, CategoryEntity>,
  val accountById: Map<Long, AccountEntity>,
) {
  val balance: Long get() = income - expense
}

class BillsViewModel(private val repository: BookkeepingRepository) : ViewModel() {

  private val month = MutableStateFlow(YearMonth.now())

  /** null 表示首次数据还没到，界面显示加载态而不是闪烁一下的空列表。 */
  val uiState: StateFlow<BillsUiState?> = combine(
    repository.observeTransactions(),
    repository.observeCategories(),
    repository.observeAccounts(),
    repository.observeAllBudgets(),
    month,
  ) { transactions, categories, accounts, budgets, selectedMonth ->
    val snapshot = buildMonthSnapshot(selectedMonth, transactions)
    val budgetKey = selectedMonth.toBudgetKey()

    BillsUiState(
      month = selectedMonth,
      dayGroups = snapshot.groupByDay(),
      income = snapshot.income,
      expense = snapshot.expense,
      totalBudget = budgets
        .firstOrNull { it.yearMonth == budgetKey && it.categoryId == BudgetEntity.TOTAL_BUDGET_CATEGORY_ID }
        ?.amount,
      categoryById = categories.associateBy { it.id },
      accountById = accounts.associateBy { it.id },
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  fun previousMonth() {
    month.value = month.value.minusMonths(1)
  }

  fun nextMonth() {
    month.value = month.value.plusMonths(1)
  }
}
