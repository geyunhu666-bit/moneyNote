package com.witsky.moneynote.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.local.entity.AccountEntity
import com.witsky.moneynote.data.local.entity.CategoryGroup
import com.witsky.moneynote.data.local.entity.TransactionType
import com.witsky.moneynote.ui.common.AppDate
import com.witsky.moneynote.ui.common.ExpenseColor
import com.witsky.moneynote.ui.common.IncomeColor
import com.witsky.moneynote.ui.common.TransferColor
import com.witsky.moneynote.ui.common.formatCents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditorScreen(
  repository: BookkeepingRepository,
  editingId: Long?,
  openToken: Int,
  onClose: () -> Unit,
) {
  // key 必须带上 editingId：否则「新增」与「编辑某笔」会共用同一个 ViewModel 实例，
  // 编辑完再点新增会看到上一笔的残留内容。
  val viewModel: TransactionEditorViewModel = viewModel(key = "editor-${editingId ?: 0L}") {
    TransactionEditorViewModel(repository, editingId)
  }
  val state by viewModel.state.collectAsStateWithLifecycle()

  // 每次打开都重新读库，丢掉上一次没保存的改动。
  // 用 openToken 而不是 Unit 触发：屏幕旋转时 token 不变，正在输入的内容不会被清掉。
  LaunchedEffect(openToken) { viewModel.load() }

  var showDatePicker by remember { mutableStateOf(false) }
  var confirmDelete by remember { mutableStateOf(false) }

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
      EditorTopBar(
        state = state,
        isEditing = editingId != null,
        onClose = onClose,
        onSelectType = viewModel::selectType,
        onRequestDelete = { confirmDelete = true },
      )

      AmountPanel(state = state, onFocusAmount = viewModel::focusAmount)

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        if (!state.ready) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("正在准备…", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        } else if (state.type == TransactionType.TRANSFER) {
          TransferSection(state = state, viewModel = viewModel)
        } else {
          CategorySection(state = state, viewModel = viewModel)
        }
      }

      NoteAndDateRow(
        note = state.note,
        epochDay = state.epochDay,
        minuteOfDay = state.minuteOfDay,
        onNoteChange = viewModel::setNote,
        onShiftDay = viewModel::shiftDay,
        onPickDate = { showDatePicker = true },
      )

      Keypad(
        canSave = state.canSave,
        showFeeKeys = state.type == TransactionType.TRANSFER && state.feeFocused,
        onKey = viewModel::onKeyPress,
        onBackspace = viewModel::onBackspace,
        onFeeKey = viewModel::onFeeKeyPress,
        onFeeBackspace = viewModel::onFeeBackspace,
        onToday = { viewModel.setDay(AppDate.today()) },
        onSave = {
          viewModel.save(onSaved = onClose)
        },
      )
    }
  }

  if (showDatePicker) {
    val pickerState = rememberDatePickerState(
      initialSelectedDateMillis = AppDate.utcMillisFromEpochDay(state.epochDay),
    )
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(onClick = {
          pickerState.selectedDateMillis?.let { viewModel.setDay(AppDate.epochDayFromUtcMillis(it)) }
          showDatePicker = false
        }) { Text("确定") }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text("取消") }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }

  if (confirmDelete) {
    AlertDialog(
      onDismissRequest = { confirmDelete = false },
      title = { Text("删除这笔账？") },
      text = { Text("删除后无法恢复。") },
      confirmButton = {
        TextButton(onClick = {
          confirmDelete = false
          viewModel.delete(onDeleted = onClose)
        }) { Text("删除", color = ExpenseColor) }
      },
      dismissButton = {
        TextButton(onClick = { confirmDelete = false }) { Text("取消") }
      },
    )
  }
}

@Composable
private fun EditorTopBar(
  state: EditorUiState,
  isEditing: Boolean,
  onClose: () -> Unit,
  onSelectType: (TransactionType) -> Unit,
  onRequestDelete: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    TextButton(onClick = onClose) { Text("取消") }

    Row(
      modifier = Modifier.weight(1f),
      horizontalArrangement = Arrangement.Center,
    ) {
      TransactionType.entries.forEach { type ->
        TypeChip(
          label = type.label,
          selected = state.type == type,
          color = when (type) {
            TransactionType.EXPENSE -> ExpenseColor
            TransactionType.INCOME -> IncomeColor
            TransactionType.TRANSFER -> TransferColor
          },
          onClick = { onSelectType(type) },
        )
        Spacer(Modifier.width(6.dp))
      }
    }

    if (isEditing) {
      TextButton(onClick = onRequestDelete) { Text("删除", color = ExpenseColor) }
    } else {
      Spacer(Modifier.width(64.dp))
    }
  }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(50),
    color = if (selected) color.copy(alpha = 0.14f) else Color.Transparent,
    contentColor = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
  ) {
    Text(
      text = label,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
      style = MaterialTheme.typography.labelLarge,
      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
    )
  }
}

@Composable
private fun AmountPanel(state: EditorUiState, onFocusAmount: () -> Unit) {
  val focusable = state.type == TransactionType.TRANSFER
  val amountFocused = !focusable || !state.feeFocused
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(enabled = focusable, onClick = onFocusAmount)
      .background(
        if (focusable && amountFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
        else Color.Transparent,
      )
      .padding(horizontal = 20.dp, vertical = 10.dp),
  ) {
    Text(
      text = state.summaryLabel,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
    )
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        text = "¥",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
      )
      Spacer(Modifier.width(6.dp))
      Text(
        text = state.amountInput.ifEmpty { "0" },
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = when (state.type) {
          TransactionType.EXPENSE -> ExpenseColor
          TransactionType.INCOME -> IncomeColor
          TransactionType.TRANSFER -> TransferColor
        },
      )
    }
  }
}

@Composable
private fun CategorySection(state: EditorUiState, viewModel: TransactionEditorViewModel) {
  Column(modifier = Modifier.fillMaxSize()) {
    LazyVerticalGrid(
      columns = GridCells.Fixed(5),
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentPadding = PaddingValues(8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      items(items = state.groups, key = { it.parent.id }) { group ->
        val selected = state.selectedCategoryId == group.parent.id ||
          group.children.any { it.id == state.selectedCategoryId }
        CategoryCell(
          icon = group.parent.icon,
          name = group.parent.name,
          selected = selected,
          onClick = { viewModel.selectParent(group) },
        )
      }
    }

    if (state.expandedChildren.isNotEmpty()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 12.dp, vertical = 4.dp),
      ) {
        state.expandedChildren.forEach { child ->
          ChildChip(
            label = "${child.icon} ${child.name}",
            selected = state.selectedCategoryId == child.id,
            onClick = { viewModel.selectChild(child) },
          )
          Spacer(Modifier.width(8.dp))
        }
      }
    }

    AccountChipsRow(
      accounts = state.accounts,
      selectedId = state.accountId,
      onSelect = viewModel::selectAccount,
    )
  }
}

@Composable
private fun CategoryCell(icon: String, name: String, selected: Boolean, onClick: () -> Unit) {
  Column(
    modifier = Modifier
      .clickable(onClick = onClick)
      .padding(vertical = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .background(
          color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
          else MaterialTheme.colorScheme.surfaceVariant,
          shape = CircleShape,
        )
        .then(
          if (selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
          else Modifier,
        ),
      contentAlignment = Alignment.Center,
    ) {
      Text(text = icon, fontSize = 20.sp)
    }
    Spacer(Modifier.height(4.dp))
    Text(
      text = name,
      style = MaterialTheme.typography.labelSmall,
      maxLines = 1,
      color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun ChildChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun AccountChipsRow(
  accounts: List<AccountEntity>,
  selectedId: Long?,
  onSelect: (Long) -> Unit,
  label: String? = null,
) {
  Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
    if (label != null) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
      )
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
      accounts.forEach { account ->
        ChildChip(
          label = "${account.icon} ${account.name}",
          selected = selectedId == account.id,
          onClick = { onSelect(account.id) },
        )
        Spacer(Modifier.width(8.dp))
      }
    }
  }
}

@Composable
private fun TransferSection(state: EditorUiState, viewModel: TransactionEditorViewModel) {
  Column(modifier = Modifier.fillMaxSize()) {
    AccountChipsRow(
      accounts = state.accounts,
      selectedId = state.accountId,
      onSelect = viewModel::selectAccount,
      label = "转出账户",
    )
    AccountChipsRow(
      accounts = state.accounts,
      selectedId = state.toAccountId,
      onSelect = viewModel::selectTargetAccount,
      label = "转入账户",
    )
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = viewModel::focusFee)
        .background(
          if (state.feeFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
          else Color.Transparent,
        )
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        "手续费",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (state.feeFocused) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier.weight(1f),
      )
      Text(
        text = "¥ ${state.feeInput.ifEmpty { "0" }}",
        style = MaterialTheme.typography.bodyMedium,
        color = if (state.feeFocused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (state.feeInput.isNotEmpty()) {
        TextButton(onClick = viewModel::onFeeBackspace) { Text("⌫") }
      } else {
        TextButton(onClick = viewModel::focusFee) { Text("填写") }
      }
    }
  }
}

@Composable
private fun NoteAndDateRow(
  note: String,
  epochDay: Long,
  minuteOfDay: Int,
  onNoteChange: (String) -> Unit,
  onShiftDay: (Long) -> Unit,
  onPickDate: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
    OutlinedTextField(
      value = note,
      onValueChange = onNoteChange,
      modifier = Modifier.fillMaxWidth(),
      label = { Text("备注") },
      singleLine = true,
      textStyle = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      TextButton(onClick = { onShiftDay(-1L) }) { Text("‹") }
      Surface(
        onClick = onPickDate,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
      ) {
        Text(
          text = "📅 ${AppDate.formatDay(epochDay)}  ${AppDate.formatMinuteOfDay(minuteOfDay)}",
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          style = MaterialTheme.typography.bodyMedium,
        )
      }
      TextButton(onClick = { onShiftDay(1L) }) { Text("›") }
    }
  }
}

@Composable
private fun Keypad(
  canSave: Boolean,
  showFeeKeys: Boolean,
  onKey: (String) -> Unit,
  onBackspace: () -> Unit,
  onFeeKey: (String) -> Unit,
  onFeeBackspace: () -> Unit,
  onToday: () -> Unit,
  onSave: () -> Unit,
) {
  val digit: (String) -> Unit = if (showFeeKeys) onFeeKey else onKey
  val erase: () -> Unit = if (showFeeKeys) onFeeBackspace else onBackspace

  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
    KeypadRow {
      KeypadKey("1", Modifier.weight(1f)) { digit("1") }
      KeypadKey("2", Modifier.weight(1f)) { digit("2") }
      KeypadKey("3", Modifier.weight(1f)) { digit("3") }
      KeypadKey("⌫", Modifier.weight(1f), accent = true, onClick = erase)
    }
    KeypadRow {
      KeypadKey("4", Modifier.weight(1f)) { digit("4") }
      KeypadKey("5", Modifier.weight(1f)) { digit("5") }
      KeypadKey("6", Modifier.weight(1f)) { digit("6") }
      KeypadKey("今天", Modifier.weight(1f), accent = true, onClick = onToday)
    }
    KeypadRow {
      KeypadKey("7", Modifier.weight(1f)) { digit("7") }
      KeypadKey("8", Modifier.weight(1f)) { digit("8") }
      KeypadKey("9", Modifier.weight(1f)) { digit("9") }
      KeypadKey(".", Modifier.weight(1f)) { digit(".") }
    }
    KeypadRow {
      KeypadKey("0", Modifier.weight(1f)) { digit("0") }
      KeypadKey(
        text = "保存",
        modifier = Modifier.weight(3f),
        enabled = canSave,
        primary = true,
        onClick = onSave,
      )
    }
  }
}

@Composable
private fun KeypadRow(content: @Composable () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    content()
  }
}

@Composable
private fun KeypadKey(
  text: String,
  modifier: Modifier = Modifier,
  accent: Boolean = false,
  primary: Boolean = false,
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  val background = when {
    primary && enabled -> MaterialTheme.colorScheme.primary
    primary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    accent -> MaterialTheme.colorScheme.surfaceVariant
    else -> MaterialTheme.colorScheme.surface
  }
  val contentColor = when {
    primary -> MaterialTheme.colorScheme.onPrimary
    else -> MaterialTheme.colorScheme.onSurface
  }

  Box(
    modifier = modifier
      .height(50.dp)
      .background(background, RoundedCornerShape(10.dp))
      .clickable(enabled = enabled, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal,
      color = contentColor,
    )
  }
}
