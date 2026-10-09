package com.witsky.moneynote.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.settings.SettingsRepository
import com.witsky.moneynote.ui.assets.AssetsScreen
import com.witsky.moneynote.ui.bills.BillsScreen
import com.witsky.moneynote.ui.calendar.CalendarScreen
import com.witsky.moneynote.ui.edit.TransactionEditorScreen
import com.witsky.moneynote.ui.mine.MineScreen
import com.witsky.moneynote.ui.search.SearchScreen
import com.witsky.moneynote.ui.stats.StatsScreen

/**
 * 四个一级 Tab。用状态驱动切换而不是引入导航库：这里的导航图是固定的一层，
 * 没有深链、没有回退栈嵌套，导航库换不来任何东西，只会多一层需要跟进的 API。
 */
enum class MainTab(val label: String, val icon: String) {
  BILLS("流水", "🧾"),
  STATS("统计", "📊"),
  ASSETS("资产", "💰"),
  MINE("我的", "⚙️"),
}

/**
 * 全屏覆盖页。日历与搜索互斥，用枚举表达就不会出现两个布尔同时为真的怪状态。
 * 枚举可被 [androidx.compose.runtime.saveable.rememberSaveable] 直接保存，旋转屏幕后覆盖页不丢。
 */
enum class Overlay { Calendar, Search }

/** 编辑器的目标：新增，或编辑已有账目。 */
sealed interface EditorTarget {
  data object New : EditorTarget
  data class Existing(val transactionId: Long) : EditorTarget
}

@Composable
fun MoneyNoteApp(
  repository: BookkeepingRepository,
  settingsRepository: SettingsRepository,
) {
  var tab by rememberSaveable { mutableStateOf(MainTab.BILLS) }
  var overlay by rememberSaveable { mutableStateOf<Overlay?>(null) }

  // 编辑器刻意与 overlay 分开成两层：从日历里点开一笔账再返回时，
  // 日历的月份和选中日期都还在，不会被重建 —— 记完账接着看同一天是常见动作。
  // 存 id 而不是 EditorTarget：sealed 类进不了 Bundle。0 是「新增」的哨兵（账目 id 自增从 1 起）。
  var editorTargetId by rememberSaveable { mutableStateOf<Long?>(null) }

  // 每次打开编辑器都自增。编辑器据此重新读库，丢掉上一次没保存的改动；
  // 而旋转屏幕时这个值不变，正在输入的内容不会被清空。
  var editorToken by rememberSaveable { mutableStateOf(0) }

  val editorTarget: EditorTarget? = when (val id = editorTargetId) {
    null -> null
    0L -> EditorTarget.New
    else -> EditorTarget.Existing(id)
  }

  val openEditor: (Long) -> Unit = { id ->
    editorToken++
    editorTargetId = id
  }
  val openNewEditor: () -> Unit = {
    editorToken++
    editorTargetId = 0L
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
      bottomBar = {
        NavigationBar {
          MainTab.entries.forEach { entry ->
            NavigationBarItem(
              selected = tab == entry,
              onClick = { tab = entry },
              icon = { Text(text = entry.icon, fontSize = 20.sp) },
              label = { Text(text = entry.label, fontSize = 11.sp) },
            )
          }
        }
      },
      floatingActionButton = {
        if (tab == MainTab.BILLS) {
          ExtendedFloatingActionButton(onClick = openNewEditor) {
            Text("记一笔")
          }
        }
      },
    ) { innerPadding ->
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        when (tab) {
          MainTab.BILLS -> BillsScreen(
            repository = repository,
            onEditTransaction = openEditor,
            onOpenCalendar = { overlay = Overlay.Calendar },
            onOpenSearch = { overlay = Overlay.Search },
          )
          MainTab.STATS -> StatsScreen(repository = repository)
          MainTab.ASSETS -> AssetsScreen(repository = repository)
          MainTab.MINE -> MineScreen(
            repository = repository,
            settingsRepository = settingsRepository,
          )
        }
      }
    }

    // 覆盖页与编辑器都画在 Scaffold 之外，因此会盖住底部导航，是完整的一屏。

    // 系统返回键逐层退出：编辑器 > 覆盖页，都不在场时才轮到系统默认行为（退出应用）。
    BackHandler(enabled = editorTarget != null) { editorTargetId = null }
    BackHandler(enabled = editorTarget == null && overlay != null) { overlay = null }

    when (overlay) {
      Overlay.Calendar -> CalendarScreen(
        repository = repository,
        onBack = { overlay = null },
        onEditTransaction = openEditor,
      )
      Overlay.Search -> SearchScreen(
        repository = repository,
        onBack = { overlay = null },
        onEditTransaction = openEditor,
      )
      null -> Unit
    }

    editorTarget?.let { target ->
      val editingId = (target as? EditorTarget.Existing)?.transactionId
      TransactionEditorScreen(
        repository = repository,
        editingId = editingId,
        openToken = editorToken,
        onClose = { editorTargetId = null },
      )
    }
  }
}
