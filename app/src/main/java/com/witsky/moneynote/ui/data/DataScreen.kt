package com.witsky.moneynote.ui.data

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.witsky.moneynote.data.BackupPayload
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.exportFileName
import com.witsky.moneynote.ui.common.ExpenseColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun DataScreen(
  repository: BookkeepingRepository,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: DataViewModel = viewModel { DataViewModel(repository) }
  val message by viewModel.message.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var pendingRestore by remember { mutableStateOf<BackupPayload?>(null) }

  // 导出：先让用户选保存位置，选定后再生成内容写入。
  val csvSaver = rememberLauncherForActivityResult(
    ActivityResultContracts.CreateDocument("text/csv"),
  ) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    viewModel.buildCsv { text ->
      scope.launch {
        val ok = writeText(context, uri, text)
        viewModel.report(if (ok) "CSV 已导出。" else "写入文件失败，请换个位置再试。")
      }
    }
  }

  val backupSaver = rememberLauncherForActivityResult(
    ActivityResultContracts.CreateDocument("application/json"),
  ) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    viewModel.buildBackup { text ->
      scope.launch {
        val ok = writeText(context, uri, text)
        viewModel.report(if (ok) "备份已导出。把它存到网盘或电脑上更稳妥。" else "写入文件失败，请换个位置再试。")
      }
    }
  }

  val backupPicker = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    scope.launch {
      val text = withContext(Dispatchers.IO) { readText(context, uri) }
      if (text == null) {
        viewModel.report("这个文件读不出来。")
      } else {
        viewModel.parseBackup(text) { payload -> pendingRestore = payload }
      }
    }
  }

  Column(modifier = modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TextButton(onClick = onBack) { Text("‹ 返回") }
      Text(
        text = "数据与备份",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.weight(1f),
      )
    }

    Card(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("数据只在这台手机上", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Text(
          text = "应用不联网、不上传。手机丢了或应用被卸载，数据就没了 —— " +
            "所以建议每隔一段时间导出一份备份，存到电脑或网盘。",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    ActionRow(
      icon = "📄",
      title = "导出账目 CSV",
      subtitle = "所有流水导出成表格，Excel / WPS 可直接打开",
      onClick = {
        csvSaver.launch(exportFileName("记账本流水", "csv", LocalDate.now()))
      },
    )

    ActionRow(
      icon = "💾",
      title = "导出完整备份",
      subtitle = "账目、分类、账户、预算全量导出为 JSON 文件",
      onClick = {
        backupSaver.launch(exportFileName("moneynote-backup", "json", LocalDate.now()))
      },
    )

    ActionRow(
      icon = "📥",
      title = "从备份恢复",
      subtitle = "用备份文件覆盖当前全部数据",
      danger = true,
      onClick = {
        backupPicker.launch(arrayOf("application/json", "*/*"))
      },
    )
  }

  pendingRestore?.let { payload ->
    AlertDialog(
      onDismissRequest = { pendingRestore = null },
      title = { Text("确认覆盖导入？") },
      text = {
        Text(
          "备份里有：账目 ${payload.transactions.size} 笔、分类 ${payload.categories.size} 个、" +
            "账户 ${payload.accounts.size} 个、预算 ${payload.budgets.size} 条。\n\n" +
            "导入会清空并替换当前的全部数据，且无法撤销。如果不确定，先导出一份现在的备份。",
        )
      },
      confirmButton = {
        TextButton(onClick = {
          val toRestore = payload
          pendingRestore = null
          viewModel.restore(toRestore) { }
        }) { Text("覆盖导入", color = ExpenseColor) }
      },
      dismissButton = {
        TextButton(onClick = { pendingRestore = null }) { Text("取消") }
      },
    )
  }

  message?.let { text ->
    AlertDialog(
      onDismissRequest = viewModel::dismissMessage,
      title = { Text("提示") },
      text = { Text(text) },
      confirmButton = { TextButton(onClick = viewModel::dismissMessage) { Text("知道了") } },
    )
  }
}

@Composable
private fun ActionRow(
  icon: String,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  danger: Boolean = false,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(icon, fontSize = 20.sp)
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyLarge,
          color = if (danger) ExpenseColor else MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      TextButton(onClick = onClick) { Text(if (danger) "选择文件" else "导出") }
    }
    HorizontalDivider(
      modifier = Modifier.padding(start = 16.dp),
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
  }
}

/** 文件读写走 IO 线程：导出几千条账目虽然不大，但也不该占着主线程。 */
private suspend fun writeText(context: Context, uri: Uri, text: String): Boolean =
  withContext(Dispatchers.IO) {
    try {
      context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(text.toByteArray(Charsets.UTF_8))
        stream.flush()
      } != null
    } catch (e: Exception) {
      false
    }
  }

private fun readText(context: Context, uri: Uri): String? = try {
  context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
} catch (e: Exception) {
  null
}
