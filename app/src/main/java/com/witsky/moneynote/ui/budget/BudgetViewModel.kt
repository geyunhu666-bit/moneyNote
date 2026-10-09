package com.witsky.moneynote.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.buildMonthSnapshot
import com.witsky.moneynote.data.budgetableCategories
import com.witsky.moneynote.data.childIdsOf
import com.witsky.moneynote.data.local.entity.BudgetEntity
import com.witsky.moneynote.data.local.entity.CategoryEntity
import com.witsky.moneynote.data.monthSpentIn
import com.witsky.moneynote.data.toBudgetKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

data class BudgetLine(
  val categoryId: Long,
  val name: String,
  val icon: String,
  val budget: Long,
  /** 该分类含子分类在当月的实际支出。 */
  val spent: Long,
) {
  val remaining: Long get() = budget - spent
  val isOverspent: Boolean get() = spent > budget
}

data class BudgetUiState(
  val month: YearMonth,
  /** null 表示这个月还没设总预算。 */
  val totalBudget: Long?,
  val spentSoFar: Long,
  val lines: List<BudgetLine>,
  /** 还没设过预算的一级支出分类，用来填充「添加分类预算」的选择列表。 */
  val candidates: List<CategoryEntity>,
) {
}

class BudgetViewModel(private val repository: BookkeepingRepository) : ViewModel() {

  private val month = MutableStateFlow(YearMonth.now())

  val uiState: StateFlow<BudgetUiState?> = combine(
    repository.observeTransactions(),
    repository.observeCategories(),
    repository.observeAllBudgets(),
    month,
  ) { transactions, categories, budgets, selectedMonth ->
    val snapshot = buildMonthSnapshot(selectedMonth, transactions)
    val budgetByCategory = budgets
      .filter { it.yearMonth == selectedMonth.toBudgetKey() }
      .associate { it.categoryId to it.amount }

    val lines = budgetByCategory
      .filterKeys { it != BudgetEntity.TOTAL_BUDGET_CATEGORY_ID }
      .mapNotNull { (categoryId, amount) ->
        val category = categories.firstOrNull { it.id == categoryId } ?: return@mapNotNull null
        BudgetLine(
          categoryId = category.id,
          name = category.name,
          icon = category.icon,
          budget = amount,
          spent = monthSpentIn(snapshot, category.id, categories.childIdsOf(category.id)),
        )
      }
      // 花得多的排前面：预算页是用来发现"哪个类目快超了"的，不是按名字索引的通讯录。
      .sortedByDescending { it.spent }

    BudgetUiState(
      month = selectedMonth,
      totalBudget = budgetByCategory[BudgetEntity.TOTAL_BUDGET_CATEGORY_ID],
      spentSoFar = snapshot.expense,
      lines = lines,
      candidates = categories.budgetableCategories().filter { budgetByCategory[it.id] == null },
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  fun previousMonth() {
    month.value = month.value.minusMonths(1)
  }

  fun nextMonth() {
    month.value = month.value.plusMonths(1)
  }

  fun setTotalBudget(cents: Long) = writeBudget(BudgetEntity.TOTAL_BUDGET_CATEGORY_ID, cents)

  fun setCategoryBudget(categoryId: Long, cents: Long) = writeBudget(categoryId, cents)

  fun clearTotalBudget() = writeBudget(BudgetEntity.TOTAL_BUDGET_CATEGORY_ID, 0L)

  fun clearCategoryBudget(categoryId: Long) = writeBudget(categoryId, 0L)

  /** 金额填 0 即等同于删除这条预算，省得界面上再多一个「删除」按钮和确认框。 */
  private fun writeBudget(categoryId: Long, cents: Long) {
    val key = month.value.toBudgetKey()
    viewModelScope.launch {
      if (cents <= 0L) repository.clearBudget(key, categoryId) else repository.setBudget(key, categoryId, cents)
    }
  }
}
