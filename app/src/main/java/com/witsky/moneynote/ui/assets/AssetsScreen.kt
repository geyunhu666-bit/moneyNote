package com.witsky.moneynote.ui.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.DeleteOutcome
import com.witsky.moneynote.data.local.entity.AccountEntity
import com.witsky.moneynote.data.local.entity.AccountType
import com.witsky.moneynote.ui.common.ExpenseColor
import com.witsky.moneynote.ui.common.centsToInput
import com.witsky.moneynote.ui.common.formatCents
import com.witsky.moneynote.ui.common.parseAmountToCents

@Composable
fun AssetsScreen(repository: BookkeepingRepository, modifier: Modifier = Modifier) {
  val viewModel: AssetsViewModel = viewModel { AssetsViewModel(repository) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  var editing by remember { mutableStateOf<AccountEntity?>(null) }
  var creating by remember { mutableStateOf(false) }
  var notice by remember { mutableStateOf<String?>(null) }

  Column(modifier = modifier.fillMaxSize()) {
    NetWorthCard(netWorth = state?.netWorth ?: 0L)

    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "账户",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.weight(1f),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      TextButton(onClick = { creating = true }) { Text("＋ 添加账户") }
    }

    val items = state?.items.orEmpty()
    if (items.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("还没有账户", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    } else {
      LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        items(items = items, key = { it.account.id }) { item ->
          AccountRow(item = item, onClick = { editing = item.account })
        }
      }
    }
  }

  notice?.let { text ->
    AlertDialog(
      onDismissRequest = { notice = null },
      title = { Text("无法删除") },
      text = { Text(text) },
      confirmButton = { TextButton(onClick = { notice = null }) { Text("知道了") } },
    )
  }

  if (creating) {
    AccountEditorDialog(
      initial = null,
      onDismiss = { creating = false },
      onConfirm = { name, type, initialBalance, icon ->
        viewModel.addAccount(name, type, initialBalance, icon) { creating = false }
      },
      onDelete = null,
    )
  }

  editing?.let { account ->
    AccountEditorDialog(
      initial = account,
      onDismiss = { editing = null },
      onConfirm = { name, type, initialBalance, icon ->
        viewModel.updateAccount(account, name, type, initialBalance, icon) { editing = null }
      },
      onDelete = {
        viewModel.deleteAccount(account) { outcome ->
          editing = null
          notice = when (outcome) {
            DeleteOutcome.DELETED -> null
            DeleteOutcome.REFUSED_IN_USE -> "该账户下还有账目，先删除或改到别的账户再试。"
            DeleteOutcome.REFUSED_HAS_CHILDREN -> "该账户下有子项，无法删除。"
          }
        }
      },
    )
  }
}

@Composable
private fun NetWorthCard(netWorth: Long) {
  Card(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(
        text = "净资产",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text = "¥ ${formatCents(netWorth)}",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = if (netWorth < 0) ExpenseColor else MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun AccountRow(item: AssetItem, onClick: () -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(item.account.icon, fontSize = 18.sp)
      }
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(item.account.name, style = MaterialTheme.typography.bodyLarge)
        Text(
          text = item.account.type.label,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = formatCents(item.balance),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = if (item.balance < 0) ExpenseColor else MaterialTheme.colorScheme.onSurface,
      )
    }
    HorizontalDivider(
      modifier = Modifier.padding(start = 68.dp),
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
  }
}

@Composable
private fun AccountEditorDialog(
  initial: AccountEntity?,
  onDismiss: () -> Unit,
  onConfirm: (name: String, type: AccountType, initialBalanceCents: Long, icon: String) -> Unit,
  onDelete: (() -> Unit)?,
) {
  var name by remember { mutableStateOf(initial?.name.orEmpty()) }
  var type by remember { mutableStateOf(initial?.type ?: AccountType.EWALLET) }
  var icon by remember { mutableStateOf(initial?.icon ?: AccountType.EWALLET.icon) }
  var balanceInput by remember {
    mutableStateOf(initial?.let { centsToInput(it.initialBalance) }.orEmpty())
  }
  val parsedBalance = parseAmountToCents(balanceInput)
  // 空输入视为 0（合法）；非空但解析不了（"1.2.3"）才算错，不该静默存成 0。
  val balanceError = balanceInput.isNotEmpty() && parsedBalance == null

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "添加账户" else "编辑账户") },
    text = {
      Column {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("名称") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text("类型", style = MaterialTheme.typography.labelMedium)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
        ) {
          AccountType.entries.forEach { candidate ->
            Surface(
              onClick = {
                type = candidate
                if (initial == null) icon = candidate.icon
              },
              shape = RoundedCornerShape(50),
              color = if (type == candidate) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
              else MaterialTheme.colorScheme.surfaceVariant,
              contentColor = if (type == candidate) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(end = 6.dp),
            ) {
              Text(
                text = candidate.label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
              )
            }
          }
        }
        OutlinedTextField(
          value = balanceInput,
          onValueChange = { balanceInput = it },
          label = { Text("期初余额（元）") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
        )
        if (balanceError) {
          Spacer(Modifier.height(6.dp))
          Text(
            text = "请填一个不超过两位小数的正数",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
          value = icon,
          onValueChange = { icon = it },
          label = { Text("图标（一个 emoji）") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(
        enabled = name.isNotBlank() && !balanceError,
        onClick = {
          onConfirm(name.trim(), type, parseAmountToCents(balanceInput) ?: 0L, icon.ifBlank { type.icon })
        },
      ) { Text("保存") }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (onDelete != null) {
          TextButton(onClick = onDelete) { Text("删除", color = ExpenseColor) }
        }
        TextButton(onClick = onDismiss) { Text("取消") }
      }
    },
  )
}
