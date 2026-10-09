package com.witsky.moneynote.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.DeleteOutcome
import com.witsky.moneynote.data.local.entity.CategoryEntity
import com.witsky.moneynote.data.local.entity.CategoryKind
import com.witsky.moneynote.data.settings.AppSettings
import com.witsky.moneynote.data.settings.SettingsRepository
import com.witsky.moneynote.data.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MineUiState(
  val categories: List<CategoryEntity>,
  val settings: AppSettings,
  val message: String?,
)

class MineViewModel(
  private val repository: BookkeepingRepository,
  private val settingsRepository: SettingsRepository,
) : ViewModel() {

  private val message = MutableStateFlow<String?>(null)

  val uiState: StateFlow<MineUiState> = combine(
    repository.observeCategories(),
    settingsRepository.settings,
    message,
  ) { categories, settings, current ->
    MineUiState(categories = categories, settings = settings, message = current)
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5_000),
    MineUiState(emptyList(), AppSettings(), null),
  )

  fun addCategory(
    name: String,
    kind: CategoryKind,
    parentId: Long?,
    icon: String,
    onDone: () -> Unit,
  ) {
    if (name.isBlank()) return
    viewModelScope.launch {
      repository.addCategory(name, kind, parentId, icon.ifBlank { "📦" })
      onDone()
    }
  }

  fun updateCategory(category: CategoryEntity, name: String, icon: String, onDone: () -> Unit) {
    if (name.isBlank()) return
    viewModelScope.launch {
      repository.renameCategory(category, name, icon.ifBlank { category.icon })
      onDone()
    }
  }

  /** 删除失败必须说清原因，否则用户只会看到「点了没反应」。 */
  fun deleteCategory(category: CategoryEntity) {
    viewModelScope.launch {
      message.value = when (repository.deleteCategory(category)) {
        DeleteOutcome.DELETED -> "已删除「${category.name}」"
        DeleteOutcome.REFUSED_IN_USE -> "「${category.name}」下还有账目，无法删除。"
        DeleteOutcome.REFUSED_HAS_CHILDREN -> "「${category.name}」下还有子分类，请先删子分类。"
      }
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { settingsRepository.setThemeMode(mode) }
  }

  fun setDynamicColor(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
  }

  fun dismissMessage() {
    message.value = null
  }
}
