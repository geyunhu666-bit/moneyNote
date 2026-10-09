package com.witsky.moneynote.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witsky.moneynote.data.local.entity.TransactionEntity
import com.witsky.moneynote.data.local.entity.TransactionType

/**
 * 一条流水。流水页、日历页、搜索结果三处共用，因此把「怎么显示一条账」的判断
 * 收在这一个文件里 —— 否则三处的颜色和正负号迟早会不一致。
 *
 * [showDate] 让跨月份的搜索结果带上日期；流水页按日分组时日期已经在分组标题上，就不必重复。
 */
@Composable
fun TransactionRow(
  transaction: TransactionEntity,
  categoryName: String,
  categoryIcon: String,
  accountName: String,
  targetAccountName: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  showDate: Boolean = false,
) {
  Column(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier.size(38.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(categoryIcon, fontSize = 18.sp)
      }

      Spacer(Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (transaction.type == TransactionType.TRANSFER && targetAccountName != null) {
            "$accountName → $targetAccountName"
          } else {
            categoryName
          },
          style = MaterialTheme.typography.bodyLarge,
          maxLines = 1,
        )
        val subtitle = buildSubtitle(transaction, accountName, targetAccountName, showDate)
        if (subtitle.isNotEmpty()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
        }
      }

      Spacer(Modifier.width(12.dp))

      Text(
        text = amountText(transaction),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = amountColor(transaction.type),
      )
    }
    HorizontalDivider(
      modifier = Modifier.padding(start = 66.dp),
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
  }
}

private fun amountText(transaction: TransactionEntity): String = when (transaction.type) {
  TransactionType.EXPENSE -> "-${formatCents(transaction.amount)}"
  TransactionType.INCOME -> "+${formatCents(transaction.amount)}"
  TransactionType.TRANSFER -> formatCents(transaction.amount)
}

private fun amountColor(type: TransactionType) = when (type) {
  TransactionType.EXPENSE -> ExpenseColor
  TransactionType.INCOME -> IncomeColor
  TransactionType.TRANSFER -> TransferColor
}

private fun buildSubtitle(
  transaction: TransactionEntity,
  accountName: String,
  targetAccountName: String?,
  showDate: Boolean,
): String {
  val parts = mutableListOf<String>()
  if (showDate) parts += AppDate.formatDay(transaction.epochDay)
  if (transaction.type != TransactionType.TRANSFER || targetAccountName == null) {
    parts += accountName
  }
  parts += AppDate.formatMinuteOfDay(transaction.minuteOfDay)
  if (transaction.note.isNotBlank()) parts += transaction.note
  if (transaction.fee > 0L) parts += "手续费 ${formatCents(transaction.fee)}"
  return parts.joinToString(" · ")
}
