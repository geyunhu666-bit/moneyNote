package com.witsky.moneynote.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.CalendarDay
import com.witsky.moneynote.data.buildCalendarWeeks
import com.witsky.moneynote.data.buildMonthSnapshot
import com.witsky.moneynote.data.groupByDay
import com.witsky.moneynote.data.local.entity.AccountEntity
import com.witsky.moneynote.data.local.entity.CategoryEntity
import com.witsky.moneynote.data.local.entity.TransactionEntity
import com.witsky.moneynote.ui.common.AppDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class CalendarUiState(
  val month: YearMonth,
  val weeks: List<List<CalendarDay?>>,
  val today: Long,
  val selectedDay: Long,
  val selectedIncome: Long,
  val selectedExpense: Long,
  val selectedTransactions: List<TransactionEntity>,
  val monthIncome: Long,
  val monthExpense: Long,
  val categoryById: Map<Long, CategoryEntity>,
  val accountById: Map<Long, AccountEntity>,
)

class CalendarViewModel(private val repository: BookkeepingRepository) : ViewModel() {

  private val month = MutableStateFlow(YearMonth.now())

  /** null 表示还没手动点过某一天，此时跟随「今天 / 本月 1 号」。 */
  private val pickedDay = MutableStateFlow<Long?>(null)

  val uiState: StateFlow<CalendarUiState?> = combine(
    repository.observeTransactions(),
    repository.observeCategories(),
    repository.observeAccounts(),
    month,
    pickedDay,
  ) { transactions, categories, accounts, selectedMonth, picked ->
    val snapshot = buildMonthSnapshot(selectedMonth, transactions)
    val today = AppDate.today()
    // 翻了月份就落到那个月的 1 号：把上个月选中的日期带过来会落到一个当月不存在的日子。
    val defaultDay = if (AppDate.toYearMonth(today) == selectedMonth) {
      today
    } else {
      selectedMonth.atDay(1).toEpochDay()
    }
    val day = picked ?: defaultDay
    val group = snapshot.groupByDay().firstOrNull { it.epochDay == day }

    CalendarUiState(
      month = selectedMonth,
      weeks = buildCalendarWeeks(selectedMonth, snapshot),
      today = today,
      selectedDay = day,
      selectedIncome = group?.income ?: 0L,
      selectedExpense = group?.expense ?: 0L,
      selectedTransactions = group?.transactions.orEmpty(),
      monthIncome = snapshot.income,
      monthExpense = snapshot.expense,
      categoryById = categories.associateBy { it.id },
      accountById = accounts.associateBy { it.id },
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  fun previousMonth() {
    month.value = month.value.minusMonths(1)
    pickedDay.value = null
  }

  fun nextMonth() {
    month.value = month.value.plusMonths(1)
    pickedDay.value = null
  }

  fun selectDay(epochDay: Long) {
    pickedDay.value = epochDay
  }
}
