package com.witsky.moneynote.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.DateRangeOption
import com.witsky.moneynote.data.local.entity.TransactionType
import com.witsky.moneynote.ui.common.ExpenseColor
import com.witsky.moneynote.ui.common.IncomeColor
import com.witsky.moneynote.ui.common.TransactionRow
import com.witsky.moneynote.ui.common.formatCents
import com.witsky.moneynote.ui.common.parseAmountToCents

@Composable
fun SearchScreen(
  repository: BookkeepingRepository,
  onBack: () -> Unit,
  onEditTransaction: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: SearchViewModel = viewModel { SearchViewModel(repository) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  // 金额区间的原始输入留在界面这一层：非法输入（"1.2.3"）不该被塞进筛选条件里，
  // 但用户还得能看见自己刚敲了什么。
  // 用 rememberSaveable：金额区间本身存在 VM 里（覆盖层关闭再打开、旋转都不会清空），
  // 若这两行文本用 remember，就会变成「结果仍被过滤、输入框却空了」的自相矛盾。
  var minText by rememberSaveable { mutableStateOf("") }
  var maxText by rememberSaveable { mutableStateOf("") }

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
        text = "搜索",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.weight(1f),
      )
      TextButton(onClick = {
        minText = ""
        maxText = ""
        viewModel.clearAll()
      }) { Text("清空条件") }
    }

    when (val uiState = state) {
      null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("正在读取…", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }

      else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "filters") {
          Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            OutlinedTextField(
              value = uiState.filters.keyword,
              onValueChange = viewModel::setKeyword,
              label = { Text("关键词（备注 / 分类 / 账户）") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("类型")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Chip("全部", uiState.filters.type == null) { viewModel.setType(null) }
              listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER).forEach { type ->
                Chip(type.label, uiState.filters.type == type) { viewModel.setType(type) }
              }
            }

            SectionLabel("分类")
            ChipScroller {
              Chip("全部", uiState.filters.categoryId == null) { viewModel.setCategory(null) }
              uiState.topCategories.forEach { category ->
                Chip(
                  label = "${category.icon} ${category.name}",
                  selected = uiState.filters.categoryId == category.id,
                  onClick = { viewModel.setCategory(category.id) },
                )
              }
            }

            SectionLabel("账户")
            ChipScroller {
              Chip("全部", uiState.filters.accountId == null) { viewModel.setAccount(null) }
              uiState.accounts.forEach { account ->
                Chip(
                  label = "${account.icon} ${account.name}",
                  selected = uiState.filters.accountId == account.id,
                  onClick = { viewModel.setAccount(account.id) },
                )
              }
            }

            SectionLabel("金额区间（元）")
            Row(verticalAlignment = Alignment.CenterVertically) {
              AmountField(
                value = minText,
                label = "最低",
                onValueChange = {
                  minText = it
                  viewModel.setAmountRange(parseAmountToCents(it), parseAmountToCents(maxText))
                },
                modifier = Modifier.weight(1f),
              )
              Spacer(Modifier.width(10.dp))
              AmountField(
                value = maxText,
                label = "最高",
                onValueChange = {
                  maxText = it
                  viewModel.setAmountRange(parseAmountToCents(minText), parseAmountToCents(it))
                },
                modifier = Modifier.weight(1f),
              )
            }

            SectionLabel("时间")
            ChipScroller {
              DateRangeOption.entries.forEach { option ->
                Chip(option.label, uiState.dateRange == option) { viewModel.setDateRange(option) }
              }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "共 ${uiState.results.size} 笔",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = "支出 ${formatCents(uiState.expenseTotal)}",
                style = MaterialTheme.typography.labelMedium,
                color = ExpenseColor,
              )
              Spacer(Modifier.width(10.dp))
              Text(
                text = "收入 ${formatCents(uiState.incomeTotal)}",
                style = MaterialTheme.typography.labelMedium,
                color = IncomeColor,
              )
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          }
        }

        if (uiState.results.isEmpty()) {
          item(key = "empty") {
            Text(
              text = if (uiState.filters.isUnfiltered) "还没有账目可以搜" else "没有符合条件的账目",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth().padding(32.dp),
            )
          }
        }

        items(items = uiState.results, key = { it.id }) { transaction ->
          TransactionRow(
            transaction = transaction,
            categoryName = transaction.categoryId?.let { uiState.categoryById[it]?.name } ?: "转账",
            categoryIcon = transaction.categoryId?.let { uiState.categoryById[it]?.icon } ?: "🔁",
            accountName = uiState.accountById[transaction.accountId]?.name ?: "已删除账户",
            targetAccountName = transaction.toAccountId?.let { uiState.accountById[it]?.name },
            onClick = { onEditTransaction(transaction.id) },
            // 搜索结果会横跨多个月，日期必须显示出来
            showDate = true,
          )
        }
      }
    }
  }
}

@Composable
private fun SectionLabel(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
  )
}

@Composable
private fun ChipScroller(content: @Composable () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    content()
  }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(50),
    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    else MaterialTheme.colorScheme.surfaceVariant,
    contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
  ) {
    Text(
      text = label,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      style = MaterialTheme.typography.labelMedium,
    )
  }
}

@Composable
private fun AmountField(
  value: String,
  label: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    modifier = modifier,
  )
}
