package com.witsky.moneynote.ui.budget

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.local.entity.CategoryEntity
import com.witsky.moneynote.ui.common.AmountInputDialog
import com.witsky.moneynote.ui.common.AppDate
import com.witsky.moneynote.ui.common.ExpenseColor
import com.witsky.moneynote.ui.common.IncomeColor
import com.witsky.moneynote.ui.common.MonthSwitcher
import com.witsky.moneynote.ui.common.ProgressBar
import com.witsky.moneynote.ui.common.budgetColor
import com.witsky.moneynote.ui.common.formatCents
import com.witsky.moneynote.ui.common.fractionOf

/** 新加分类预算时的默认额度，加完立刻能在列表里改。 */
private const val DEFAULT_CATEGORY_BUDGET = 100_000L

@Composable
fun BudgetScreen(
  repository: BookkeepingRepository,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: BudgetViewModel = viewModel { BudgetViewModel(repository) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  var editingTotal by remember { mutableStateOf(false) }
  var editingLine by remember { mutableStateOf<BudgetLine?>(null) }
  var pickingCategory by remember { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TextButton(onClick = onBack) { Text("‹ 返回") }
      Text(
        text = "预算管理",
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

      else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "total") {
          TotalBudgetCard(
            totalBudget = uiState.totalBudget,
            spent = uiState.spentSoFar,
            onEdit = { editingTotal = true },
          )
        }

        item(key = "category-header") {
          Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "分类预算",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { pickingCategory = true }) { Text("＋ 添加") }
          }
        }

        if (uiState.lines.isEmpty()) {
          item(key = "empty") {
            Text(
              text = "还没有分类预算。给「餐饮」「交通」这类常超支的类目单独设个上限，比只盯着总数有用。",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
          }
        }

        items(items = uiState.lines, key = { it.categoryId }) { line ->
          BudgetLineRow(line = line, onClick = { editingLine = line })
        }
      }
    }
  }

  if (editingTotal) {
    val current = state?.totalBudget
    AmountInputDialog(
      title = "本月总预算",
      initialCents = current,
      onDismiss = { editingTotal = false },
      onConfirm = {
        viewModel.setTotalBudget(it)
        editingTotal = false
      },
      onClear = if (current != null) {
        {
          viewModel.clearTotalBudget()
          editingTotal = false
        }
      } else {
        null
      },
    )
  }

  editingLine?.let { line ->
    AmountInputDialog(
      title = "${line.icon} ${line.name} 的预算",
      initialCents = line.budget,
      onDismiss = { editingLine = null },
      onConfirm = {
        viewModel.setCategoryBudget(line.categoryId, it)
        editingLine = null
      },
      onClear = {
        viewModel.clearCategoryBudget(line.categoryId)
        editingLine = null
      },
    )
  }

  if (pickingCategory) {
    CategoryPickerDialog(
      candidates = state?.candidates.orEmpty(),
      onDismiss = { pickingCategory = false },
      onPick = { category ->
        pickingCategory = false
        // 先落一个默认额度，用户马上能在列表里看到并改，比先弹输入框再决定更顺。
        viewModel.setCategoryBudget(category.id, DEFAULT_CATEGORY_BUDGET)
      },
    )
  }
}

@Composable
private fun TotalBudgetCard(totalBudget: Long?, spent: Long, onEdit: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp).clickable(onClick = onEdit),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "本月总预算",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text = totalBudget?.let { "¥ ${formatCents(it)}" } ?: "未设置",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
          )
        }
        Text("修改", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
      }

      Spacer(Modifier.height(10.dp))

      if (totalBudget == null) {
        Text(
          text = "本月已支出 ¥ ${formatCents(spent)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        val fraction = fractionOf(spent, totalBudget)
        ProgressBar(fraction = fraction, color = budgetColor(fraction))
        Spacer(Modifier.height(8.dp))
        Row {
          Text(
            text = "已用 ${formatCents(spent)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
          )
          val remaining = totalBudget - spent
          Text(
            text = if (remaining >= 0L) {
              "剩余 ${formatCents(remaining)}"
            } else {
              "超支 ${formatCents(-remaining)}"
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (remaining >= 0L) IncomeColor else ExpenseColor,
          )
        }
      }
    }
  }
}

@Composable
private fun BudgetLineRow(line: BudgetLine, onClick: () -> Unit) {
  val fraction = fractionOf(line.spent, line.budget)

  Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(line.icon, fontSize = 18.sp)
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(line.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
          Text(
            text = "${formatCents(line.spent)} / ${formatCents(line.budget)}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (line.isOverspent) ExpenseColor else MaterialTheme.colorScheme.onSurface,
          )
        }
        Spacer(Modifier.height(6.dp))
        ProgressBar(fraction = fraction, color = budgetColor(fraction), height = 6.dp)
        Spacer(Modifier.height(4.dp))
        Text(
          text = if (line.isOverspent) {
            "已超支 ${formatCents(-line.remaining)}"
          } else {
            "还剩 ${formatCents(line.remaining)}"
          },
          style = MaterialTheme.typography.labelSmall,
          color = if (line.isOverspent) ExpenseColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    HorizontalDivider(
      modifier = Modifier.padding(start = 46.dp),
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    )
  }
}

@Composable
private fun CategoryPickerDialog(
  candidates: List<CategoryEntity>,
  onDismiss: () -> Unit,
  onPick: (CategoryEntity) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("给哪个类目设预算") },
    text = {
      if (candidates.isEmpty()) {
        Text("所有支出类目都已经设过预算了。")
      } else {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
          items(items = candidates, key = { it.id }) { category ->
            Surface(
              onClick = { onPick(category) },
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(category.icon, fontSize = 17.sp)
                Spacer(Modifier.width(10.dp))
                Text(category.name, style = MaterialTheme.typography.bodyLarge)
              }
            }
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
  )
}
