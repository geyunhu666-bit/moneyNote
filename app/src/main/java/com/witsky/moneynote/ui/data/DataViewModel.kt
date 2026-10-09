package com.witsky.moneynote.ui.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witsky.moneynote.data.BackupCodec
import com.witsky.moneynote.data.BackupFormatException
import com.witsky.moneynote.data.BackupPayload
import com.witsky.moneynote.data.BookkeepingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DataViewModel(private val repository: BookkeepingRepository) : ViewModel() {

  private val _message = MutableStateFlow<String?>(null)
  val message: StateFlow<String?> = _message.asStateFlow()

  /** 生成导出内容。落盘由界面层做 —— 那是 Android 的 IO 边界，不该让 ViewModel 持有 Context。 */
  fun buildCsv(onReady: (String) -> Unit) {
    viewModelScope.launch {
      val text = withContext(Dispatchers.Default) { repository.exportTransactionsCsv() }
      onReady(text)
    }
  }

  fun buildBackup(onReady: (String) -> Unit) {
    viewModelScope.launch {
      // 与 parseBackup 同口径：CSV 拼接和 JSON 序列化都是 CPU 活，别压在主线程上。
      val text = withContext(Dispatchers.Default) {
        BackupCodec.encode(repository.exportBackup())
      }
      onReady(text)
    }
  }

  /**
   * 解析备份文本。解析在后台线程上做：一个几万条记录的备份文件解析起来不是瞬间的事，
   * 而这个操作在用户选定文件后立刻发生，卡在主线程上就是一次可见的掉帧。
   */
  fun parseBackup(text: String, onReady: (BackupPayload) -> Unit) {
    viewModelScope.launch {
      val result = withContext(Dispatchers.Default) {
        try {
          Result.success(BackupCodec.decode(text))
        } catch (e: BackupFormatException) {
          Result.failure(e)
        }
      }
      result.fold(
        onSuccess = onReady,
        onFailure = { _message.value = it.message ?: "备份文件无法读取。" },
      )
    }
  }

  fun restore(payload: BackupPayload, onDone: () -> Unit) {
    viewModelScope.launch {
      try {
        repository.restoreBackup(payload)
        _message.value = "已恢复：账目 ${payload.transactions.size} 笔、" +
          "分类 ${payload.categories.size} 个、账户 ${payload.accounts.size} 个、" +
          "预算 ${payload.budgets.size} 条。"
        onDone()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        // 手工编辑过的备份可能触发唯一索引冲突等约束异常。replaceAll 在一个事务里，
        // 失败会整体回滚、当前数据原样未动 —— 这里只负责把失败说出口，而不是让应用崩掉。
        _message.value = "恢复失败：备份内容与当前版本不兼容，当前数据未被改动。"
      }
    }
  }

  fun report(text: String) {
    _message.value = text
  }

  fun dismissMessage() {
    _message.value = null
  }
}
