package com.witsky.moneynote.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.CalendarDay
import com.witsky.moneynote.ui.common.AppDate
import com.witsky.moneynote.ui.common.ExpenseColor
import com.witsky.moneynote.ui.common.IncomeColor
import com.witsky.moneynote.ui.common.MonthSwitcher
import com.witsky.moneynote.ui.common.TransactionRow
import com.witsky.moneynote.ui.common.formatCents

@Composable
fun CalendarScreen(
  repository: BookkeepingRepository,
  onBack: () -> Unit,
  onEditTransaction: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: CalendarViewModel = viewModel { CalendarViewModel(repository) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {},
      )
      .systemBarsPadding(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TextButton(onClick = onBack) { Text("‹ 返回") }
      Text(
        text = "日历",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.weight(1f),
      )
    }

    MonthSwitcher(
      label = state?.let { AppDate.formatMonth(it.month) } ?: "",
      onPrevious = viewModel::previousMonth,
      onNext = viewModel::nextMonth,
    )

    when (val uiState = state) {
      null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("正在读取…", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }

      else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        item(key = "grid") {
          WeekdayHeader()
          uiState.weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
              week.forEach { day ->
                DayCell(
                  day = day,
                  selected = day?.epochDay == uiState.selectedDay,
                  isToday = day?.epochDay == uiState.today,
                  onSelect = viewModel::selectDay,
                )
              }
              // 一行不足 7 格时用空白补满，否则最后一行的格子会被拉宽。
              repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
          }

          MonthSummary(income = uiState.monthIncome, expense = uiState.monthExpense)

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

          SelectedDayHeader(
            epochDay = uiState.selectedDay,
            income = uiState.selectedIncome,
            expense = uiState.selectedExpense,
          )
        }

        if (uiState.selectedTransactions.isEmpty()) {
          item(key = "empty") {
            Text(
              text = "这一天没有账目",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              textAlign = TextAlign.Center,
            )
          }
        }

        items(items = uiState.selectedTransactions, key = { it.id }) { transaction ->
          TransactionRow(
            transaction = transaction,
            categoryName = transaction.categoryId?.let { uiState.categoryById[it]?.name } ?: "转账",
            categoryIcon = transaction.categoryId?.let { uiState.categoryById[it]?.icon } ?: "🔁",
            accountName = uiState.accountById[transaction.accountId]?.name ?: "已删除账户",
            targetAccountName = transaction.toAccountId?.let { uiState.accountById[it]?.name },
            onClick = { onEditTransaction(transaction.id) },
          )
        }
      }
    }
  }
}

@Composable
private fun WeekdayHeader() {
  Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
    listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun RowScope.DayCell(
  day: CalendarDay?,
  selected: Boolean,
  isToday: Boolean,
  onSelect: (Long) -> Unit,
) {
  if (day == null) {
    Spacer(Modifier.weight(1f).height(52.dp))
    return
  }

  val background = when {
    selected -> MaterialTheme.colorScheme.primary
    else -> Color.Transparent
  }
  val dayColor = when {
    selected -> MaterialTheme.colorScheme.onPrimary
    isToday -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurface
  }

  Box(
    modifier = Modifier.weight(1f).height(52.dp).padding(2.dp),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(background, RoundedCornerShape(10.dp))
        .clickable { onSelect(day.epochDay) },
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = day.dayOfMonth.toString(),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = if (isToday || selected) FontWeight.Bold else FontWeight.Normal,
          color = dayColor,
        )
        if (day.expense > 0L) {
          Text(
            text = compactAmount(day.expense),
            fontSize = 9.sp,
            color = when {
              selected -> MaterialTheme.colorScheme.onPrimary
              else -> ExpenseColor
            },
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
private fun MonthSummary(income: Long, expense: Long) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = "本月支出",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f),
    )
    Text("${formatCents(expense)}", style = MaterialTheme.typography.labelLarge, color = ExpenseColor)
    Spacer(Modifier.size(12.dp))
    Text("${formatCents(income)}", style = MaterialTheme.typography.labelLarge, color = IncomeColor)
  }
}

@Composable
private fun SelectedDayHeader(epochDay: Long, income: Long, expense: Long) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = AppDate.formatDay(epochDay),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f),
    )
    if (income > 0L) {
      Text("+${formatCents(income)}", style = MaterialTheme.typography.labelMedium, color = IncomeColor)
    }
    if (income > 0L && expense > 0L) Spacer(Modifier.size(8.dp))
    if (expense > 0L) {
      Text("-${formatCents(expense)}", style = MaterialTheme.typography.labelMedium, color = ExpenseColor)
    }
  }
}

/**
 * 格子宽度只够放四五个字符，完整金额（1,234.56）会挤成一团。
 * 因此这里只显示整数元，过万折成 "9999+" —— 日历的用途是看出"哪天花得多"，不是核对精确金额。
 */
private fun compactAmount(cents: Long): String {
  val yuan = cents / 100
  return when {
    cents <= 0L -> ""
    yuan < 10_000L -> yuan.toString()
    else -> "9999+"
  }
}
