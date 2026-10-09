package com.witsky.moneynote.ui.bills

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.DayGroup
import com.witsky.moneynote.ui.common.AppDate
import com.witsky.moneynote.ui.common.ExpenseColor
import com.witsky.moneynote.ui.common.IncomeColor
import com.witsky.moneynote.ui.common.MonthSwitcher
import com.witsky.moneynote.ui.common.ProgressBar
import com.witsky.moneynote.ui.common.TransactionRow
import com.witsky.moneynote.ui.common.budgetColor
import com.witsky.moneynote.ui.common.formatCents
import com.witsky.moneynote.ui.common.fractionOf

@Composable
fun BillsScreen(
  repository: BookkeepingRepository,
  onEditTransaction: (Long) -> Unit,
  onOpenCalendar: () -> Unit,
  onOpenSearch: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: BillsViewModel = viewModel { BillsViewModel(repository) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  BillsContent(
    state = state,
    onPreviousMonth = viewModel::previousMonth,
    onNextMonth = viewModel::nextMonth,
    onEditTransaction = onEditTransaction,
    onOpenCalendar = onOpenCalendar,
    onOpenSearch = onOpenSearch,
    modifier = modifier,
  )
}

@Composable
private fun BillsContent(
  state: BillsUiState?,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onEditTransaction: (Long) -> Unit,
  onOpenCalendar: () -> Unit,
  onOpenSearch: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize()) {
    // 日历与搜索放在月份标题两侧：它们都是"换个角度看同一个月"的入口，
    // 放进底部导航会多出两个一级 Tab，而它们并不值得占据那个位置。
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TextButton(onClick = onOpenCalendar) { Text("📅", fontSize = 18.sp) }
      MonthSwitcher(
        label = state?.let { AppDate.formatMonth(it.month) } ?: "",
        onPrevious = onPreviousMonth,
        onNext = onNextMonth,
        modifier = Modifier.weight(1f),
      )
      TextButton(onClick = onOpenSearch) { Text("🔍", fontSize = 18.sp) }
    }

    SummaryCard(
      income = state?.income ?: 0L,
      expense = state?.expense ?: 0L,
      balance = state?.balance ?: 0L,
    )

    state?.totalBudget?.let { budget ->
      BudgetStrip(budget = budget, spent = state.expense)
    }

    when {
      state == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("正在读取…", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }

      state.dayGroups.isEmpty() -> Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("这个月还没有账目", style = MaterialTheme.typography.titleMedium)
          Spacer(Modifier.height(6.dp))
          Text(
            "点右下角「记一笔」开始",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      else -> LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
      ) {
        state.dayGroups.forEach { group ->
          item(key = "day-${group.epochDay}") {
            DayHeader(group = group)
          }
          items(items = group.transactions, key = { it.id }) { transaction ->
            TransactionRow(
              transaction = transaction,
              categoryName = transaction.categoryId
                ?.let { state.categoryById[it]?.name } ?: "转账",
              categoryIcon = transaction.categoryId
                ?.let { state.categoryById[it]?.icon } ?: "🔁",
              accountName = state.accountById[transaction.accountId]?.name ?: "已删除账户",
              targetAccountName = transaction.toAccountId
                ?.let { state.accountById[it]?.name },
              onClick = { onEditTransaction(transaction.id) },
            )
          }
        }
      }
    }
  }
}

/** 只在设了本月总预算时出现，给日常翻流水的人一条"还能花多少"的余量提示。 */
@Composable
private fun BudgetStrip(budget: Long, spent: Long) {
  val fraction = fractionOf(spent, budget)
  val remaining = budget - spent

  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = "本月预算 ${formatCents(budget)}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f),
      )
      Text(
        text = if (remaining >= 0L) "剩余 ${formatCents(remaining)}" else "超支 ${formatCents(-remaining)}",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = if (remaining >= 0L) IncomeColor else ExpenseColor,
      )
    }
    Spacer(Modifier.height(6.dp))
    ProgressBar(fraction = fraction, color = budgetColor(fraction), height = 6.dp)
  }
}

@Composable
private fun SummaryCard(income: Long, expense: Long, balance: Long) {
  Card(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
      SummaryCell(title = "支出", amount = expense, color = ExpenseColor, modifier = Modifier.weight(1f))
      SummaryCell(title = "收入", amount = income, color = IncomeColor, modifier = Modifier.weight(1f))
      SummaryCell(
        title = "结余",
        amount = balance,
        color = if (balance < 0) ExpenseColor else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun SummaryCell(
  title: String,
  amount: Long,
  color: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
    Text(
      text = formatCents(amount),
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      color = color,
    )
  }
}

@Composable
private fun DayHeader(group: DayGroup) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = AppDate.formatDay(group.epochDay),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f),
    )
    if (group.income > 0L) {
      Text("+${formatCents(group.income)}", style = MaterialTheme.typography.labelMedium, color = IncomeColor)
    }
    if (group.income > 0L && group.expense > 0L) Spacer(Modifier.width(8.dp))
    if (group.expense > 0L) {
      Text("-${formatCents(group.expense)}", style = MaterialTheme.typography.labelMedium, color = ExpenseColor)
    }
  }
}
