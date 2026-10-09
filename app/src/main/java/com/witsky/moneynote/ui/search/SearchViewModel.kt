package com.witsky.moneynote.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.DateRangeOption
import com.witsky.moneynote.data.SearchFilters
import com.witsky.moneynote.data.filterBy
import com.witsky.moneynote.data.local.entity.AccountEntity
import com.witsky.moneynote.data.local.entity.CategoryEntity
import com.witsky.moneynote.data.local.entity.TransactionEntity
import com.witsky.moneynote.data.local.entity.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class SearchUiState(
  val filters: SearchFilters,
  val dateRange: DateRangeOption,
  val results: List<TransactionEntity>,
  val expenseTotal: Long,
  val incomeTotal: Long,
  /** 只列一级分类：二级分类做成筛选项会是一长串，用户真正想按类目筛时脑子里想的是"餐饮"这一层。 */
  val topCategories: List<CategoryEntity>,
  val accounts: List<AccountEntity>,
  val categoryById: Map<Long, CategoryEntity>,
  val accountById: Map<Long, AccountEntity>,
)

class SearchViewModel(private val repository: BookkeepingRepository) : ViewModel() {

  private val filters = MutableStateFlow(SearchFilters())
  private val dateRange = MutableStateFlow(DateRangeOption.ALL)

  val uiState: StateFlow<SearchUiState?> = combine(
    repository.observeTransactions(),
    repository.observeCategories(),
    repository.observeAccounts(),
    filters,
    dateRange,
  ) { transactions, categories, accounts, currentFilters, currentRange ->
    val categoryName = categories.associate { it.id to it.name }
    val accountName = accounts.associate { it.id to it.name }
    val results = transactions.filterBy(currentFilters, categoryName, accountName)

    SearchUiState(
      filters = currentFilters,
      dateRange = currentRange,
      results = results,
      // 合计只算收入与支出：转账是账户间搬钱，计入合计会让"本次筛选花了多少"这个数字失真。
      expenseTotal = results.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
      incomeTotal = results.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
      topCategories = categories.filter { it.parentId == null }.sortedWith(compareBy({ it.kind }, { it.sortOrder })),
      accounts = accounts,
      categoryById = categories.associateBy { it.id },
      accountById = accounts.associateBy { it.id },
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  fun setKeyword(value: String) = filters.update { it.copy(keyword = value) }

  /** 传 null 表示不限，与 chips 里「全部」对应。 */
  fun setType(value: TransactionType?) = filters.update { it.copy(type = value) }

  fun setCategory(value: Long?) = filters.update { it.copy(categoryId = value) }

  fun setAccount(value: Long?) = filters.update { it.copy(accountId = value) }

  fun setAmountRange(min: Long?, max: Long?) = filters.update { it.copy(minAmount = min, maxAmount = max) }

  fun setDateRange(option: DateRangeOption) {
    val (start, end) = option.dayRange(LocalDate.now())
    dateRange.value = option
    filters.update { it.copy(startDay = start, endDay = end) }
  }

  fun clearAll() {
    filters.value = SearchFilters()
    dateRange.value = DateRangeOption.ALL
  }
}
