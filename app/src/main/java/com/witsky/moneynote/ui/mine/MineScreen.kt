package com.witsky.moneynote.ui.mine

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.local.entity.CategoryEntity
import com.witsky.moneynote.data.local.entity.CategoryKind
import com.witsky.moneynote.data.local.entity.toCategoryRows
import com.witsky.moneynote.data.settings.AppSettings
import com.witsky.moneynote.data.settings.SettingsRepository
import com.witsky.moneynote.data.settings.ThemeMode
import com.witsky.moneynote.ui.budget.BudgetScreen
import com.witsky.moneynote.ui.common.ExpenseColor
import com.witsky.moneynote.ui.data.DataScreen

/** 「我的」里的二级页。用枚举而不是一串布尔，避免出现两个页面同时"开着"的怪状态。 */
private enum class MinePage { ROOT, CATEGORIES, BUDGET, DATA }

@Composable
fun MineScreen(
  repository: BookkeepingRepository,
  settingsRepository: SettingsRepository,
  modifier: Modifier = Modifier,
) {
  val viewModel: MineViewModel = viewModel { MineViewModel(repository, settingsRepository) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  var page by rememberSaveable { mutableStateOf(MinePage.ROOT) }

  // 二级页里按系统返回键回到「我的」，而不是直接退出应用。
  BackHandler(enabled = page != MinePage.ROOT) { page = MinePage.ROOT }

  when (page) {
    MinePage.ROOT -> MineRoot(
      settings = state.settings,
      onOpenCategories = { page = MinePage.CATEGORIES },
      onOpenBudget = { page = MinePage.BUDGET },
      onOpenData = { page = MinePage.DATA },
      onSetThemeMode = viewModel::setThemeMode,
      onSetDynamicColor = viewModel::setDynamicColor,
      modifier = modifier,
    )

    MinePage.CATEGORIES -> CategoryManageScreen(
      state = state,
      viewModel = viewModel,
      onBack = {
        page = MinePage.ROOT
        viewModel.dismissMessage()
      },
      modifier = modifier,
    )

    MinePage.BUDGET -> BudgetScreen(
      repository = repository,
      onBack = { page = MinePage.ROOT },
      modifier = modifier,
    )

    MinePage.DATA -> DataScreen(
      repository = repository,
      onBack = { page = MinePage.ROOT },
      modifier = modifier,
    )
  }
}

@Composable
private fun MineRoot(
  settings: AppSettings,
  onOpenCategories: () -> Unit,
  onOpenBudget: () -> Unit,
  onOpenData: () -> Unit,
  onSetThemeMode: (ThemeMode) -> Unit,
  onSetDynamicColor: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showThemeDialog by remember { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxSize()) {
    Text(
      text = "我的",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )

    SettingRow(
      icon = "🗂️",
      title = "分类管理",
      subtitle = "增删收支分类、改名称与图标",
      onClick = onOpenCategories,
    )
    SettingRow(
      icon = "🎯",
      title = "预算管理",
      subtitle = "月度总预算与分类预算",
      onClick = onOpenBudget,
    )
    SettingRow(
      icon = "💾",
      title = "数据与备份",
      subtitle = "导出 CSV、导出备份、从备份恢复",
      onClick = onOpenData,
    )
    SettingRow(
      icon = "🎨",
      title = "主题",
      subtitle = if (settings.dynamicColor) "${settings.themeMode.label} · 动态取色" else settings.themeMode.label,
      onClick = { showThemeDialog = true },
    )

    Card(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("关于数据", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Text(
          text = "所有账目只存在这台手机里，没有账号、没有云端、不联网。" +
            "金额以「分」为单位整数存储，不会出现小数误差。" +
            "账户余额由期初余额加全部流水实时算出，没有冗余的余额字段。",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }

  if (showThemeDialog) {
    ThemeDialog(
      settings = settings,
      onDismiss = { showThemeDialog = false },
      onSetThemeMode = onSetThemeMode,
      onSetDynamicColor = onSetDynamicColor,
    )
  }
}

@Composable
private fun ThemeDialog(
  settings: AppSettings,
  onDismiss: () -> Unit,
  onSetThemeMode: (ThemeMode) -> Unit,
  onSetDynamicColor: (Boolean) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("主题") },
    text = {
      Column {
        ThemeMode.entries.forEach { mode ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSetThemeMode(mode) }
              .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(mode.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (settings.themeMode == mode) {
              Text("✓", color = MaterialTheme.colorScheme.primary)
            }
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("动态取色", style = MaterialTheme.typography.bodyLarge)
            Text(
              text = "用壁纸的主色调作为主题色（需要 Android 12 及以上）",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(checked = settings.dynamicColor, onCheckedChange = onSetDynamicColor)
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
  )
}

@Composable
private fun SettingRow(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier.size(38.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(icon, fontSize = 18.sp)
      }
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(
      modifier = Modifier.padding(start = 66.dp),
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
  }
}

@Composable
private fun CategoryManageScreen(
  state: MineUiState,
  viewModel: MineViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var addingKind by remember { mutableStateOf<CategoryKind?>(null) }
  var editing by remember { mutableStateOf<CategoryEntity?>(null) }
  var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }

  val expenseRows = state.categories.toCategoryRows(CategoryKind.EXPENSE)
  val incomeRows = state.categories.toCategoryRows(CategoryKind.INCOME)

  Column(modifier = modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TextButton(onClick = onBack) { Text("‹ 返回") }
      Text(
        text = "分类管理",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.weight(1f),
      )
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
      categorySection(
        title = "支出分类",
        rows = expenseRows,
        onAdd = { addingKind = CategoryKind.EXPENSE },
        onEdit = { editing = it },
      )
      categorySection(
        title = "收入分类",
        rows = incomeRows,
        onAdd = { addingKind = CategoryKind.INCOME },
        onEdit = { editing = it },
      )
    }
  }

  state.message?.let { text ->
    AlertDialog(
      onDismissRequest = viewModel::dismissMessage,
      title = { Text("分类操作") },
      text = { Text(text) },
      confirmButton = { TextButton(onClick = viewModel::dismissMessage) { Text("知道了") } },
    )
  }

  addingKind?.let { kind ->
    CategoryEditorDialog(
      title = if (kind == CategoryKind.EXPENSE) "新增支出分类" else "新增收入分类",
      initialName = "",
      initialIcon = "📦",
      parentOptions = state.categories.filter { it.kind == kind && it.parentId == null },
      initialParentId = null,
      allowParentChoice = true,
      onDismiss = { addingKind = null },
      onConfirm = { name, icon, parentId ->
        viewModel.addCategory(name, kind, parentId, icon) { addingKind = null }
      },
      onDelete = null,
    )
  }

  editing?.let { category ->
    CategoryEditorDialog(
      title = "编辑分类",
      initialName = category.name,
      initialIcon = category.icon,
      parentOptions = emptyList(),
      initialParentId = category.parentId,
      allowParentChoice = false,
      onDismiss = { editing = null },
      onConfirm = { name, icon, _ ->
        viewModel.updateCategory(category, name, icon) { editing = null }
      },
      onDelete = {
        editing = null
        pendingDelete = category
      },
    )
  }

  pendingDelete?.let { category ->
    AlertDialog(
      onDismissRequest = { pendingDelete = null },
      title = { Text("删除「${category.name}」？") },
      text = { Text("如果该分类下还有账目或子分类，将无法删除。") },
      confirmButton = {
        TextButton(onClick = {
          pendingDelete = null
          viewModel.deleteCategory(category)
        }) { Text("删除", color = ExpenseColor) }
      },
      dismissButton = {
        TextButton(onClick = { pendingDelete = null }) { Text("取消") }
      },
    )
  }
}

private fun androidx.compose.foundation.lazy.LazyListScope.categorySection(
  title: String,
  rows: List<com.witsky.moneynote.data.local.entity.CategoryRow>,
  onAdd: () -> Unit,
  onEdit: (CategoryEntity) -> Unit,
) {
  item(key = "header-$title") {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f),
      )
      TextButton(onClick = onAdd) { Text("＋ 添加") }
    }
  }
  items(items = rows, key = { it.category.id }) { row ->
    CategoryRowItem(row = row, onClick = { onEdit(row.category) })
  }
}

@Composable
private fun CategoryRowItem(
  row: com.witsky.moneynote.data.local.entity.CategoryRow,
  onClick: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = if (row.isChild) 40.dp else 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(row.category.icon, fontSize = 17.sp)
      Spacer(Modifier.width(12.dp))
      Text(
        text = row.category.name,
        style = if (row.isChild) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
        modifier = Modifier.weight(1f),
      )
      if (row.category.isSystem) {
        Text(
          text = "预置",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    HorizontalDivider(
      modifier = Modifier.padding(start = if (row.isChild) 40.dp else 16.dp),
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    )
  }
}

@Composable
private fun CategoryEditorDialog(
  title: String,
  initialName: String,
  initialIcon: String,
  parentOptions: List<CategoryEntity>,
  initialParentId: Long?,
  allowParentChoice: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (name: String, icon: String, parentId: Long?) -> Unit,
  onDelete: (() -> Unit)?,
) {
  var name by remember { mutableStateOf(initialName) }
  var icon by remember { mutableStateOf(initialIcon) }
  var parentId by remember { mutableStateOf(initialParentId) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
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
        OutlinedTextField(
          value = icon,
          onValueChange = { icon = it },
          label = { Text("图标（一个 emoji）") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        if (allowParentChoice && parentOptions.isNotEmpty()) {
          Spacer(Modifier.height(10.dp))
          Text("归属", style = MaterialTheme.typography.labelMedium)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
          ) {
            Chip(label = "一级分类", selected = parentId == null, onClick = { parentId = null })
            Spacer(Modifier.width(6.dp))
            parentOptions.forEach { parent ->
              Chip(
                label = "${parent.icon} ${parent.name}",
                selected = parentId == parent.id,
                onClick = { parentId = parent.id },
              )
              Spacer(Modifier.width(6.dp))
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = { onConfirm(name, icon, parentId) }) { Text("保存") }
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
