package com.witsky.moneynote.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.local.entity.AccountEntity
import com.witsky.moneynote.data.local.entity.CategoryEntity
import com.witsky.moneynote.data.local.entity.CategoryGroup
import com.witsky.moneynote.data.local.entity.CategoryKind
import com.witsky.moneynote.data.local.entity.TransactionEntity
import com.witsky.moneynote.data.local.entity.TransactionType
import com.witsky.moneynote.data.local.entity.toCategoryTree
import com.witsky.moneynote.ui.common.AppDate
import com.witsky.moneynote.ui.common.appendAmountKey
import com.witsky.moneynote.ui.common.backspaceAmount
import com.witsky.moneynote.ui.common.centsToInput
import com.witsky.moneynote.ui.common.parseAmountToCents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
  val type: TransactionType = TransactionType.EXPENSE,
  /** 键盘当前拼出的文本，如 "12.5"。真正入库时才转成分。 */
  val amountInput: String = "",
  val feeInput: String = "",
  /** 转账模式下键盘是否聚焦手续费输入；false 表示聚焦金额。 */
  val feeFocused: Boolean = false,
  /** 当前展开的一级分类；为 null 表示没展开任何一级分类。 */
  val expandedParentId: Long? = null,
  val selectedCategoryId: Long? = null,
  val accountId: Long? = null,
  val toAccountId: Long? = null,
  val note: String = "",
  val epochDay: Long = AppDate.today(),
  val minuteOfDay: Int = AppDate.currentMinuteOfDay(),
  val accounts: List<AccountEntity> = emptyList(),
  val categories: List<CategoryEntity> = emptyList(),
  val ready: Boolean = false,
  /** 保存写库进行中。写库是异步的，期间再点一次保存会插出两笔一模一样的账。 */
  val saving: Boolean = false,
) {
  val kind: CategoryKind
    get() = if (type == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE

  val groups: List<CategoryGroup> get() = categories.toCategoryTree(kind)

  val expandedChildren: List<CategoryEntity>
    get() = groups.firstOrNull { it.parent.id == expandedParentId }?.children.orEmpty()

  val amountCents: Long? get() = parseAmountToCents(amountInput)

  /** 保存按钮的启用条件集中在这里，界面只读不算。 */
  val canSave: Boolean
    get() {
      if (saving) return false
      val amount = amountCents ?: return false
      if (amount <= 0L) return false
      val source = accountId ?: return false
      return when (type) {
        TransactionType.TRANSFER -> toAccountId != null && toAccountId != source
        else -> selectedCategoryId != null
      }
    }

  val summaryLabel: String
    get() = when (type) {
      TransactionType.TRANSFER -> {
        val from = accounts.firstOrNull { it.id == accountId }?.let { "${it.icon} ${it.name}" } ?: "选择转出账户"
        val to = accounts.firstOrNull { it.id == toAccountId }?.let { "${it.icon} ${it.name}" } ?: "选择转入账户"
        "$from → $to"
      }
      else -> {
        val category = selectedCategoryId
          ?.let { id -> categories.firstOrNull { it.id == id } }
          ?.let { "${it.icon} ${it.name}" }
          ?: "选择分类"
        val account = accounts.firstOrNull { it.id == accountId }?.let { "${it.icon} ${it.name}" } ?: "选择账户"
        "$category · $account"
      }
    }
}

class TransactionEditorViewModel(
  private val repository: BookkeepingRepository,
  private val editingId: Long?,
) : ViewModel() {

  private val _state = MutableStateFlow(EditorUiState())
  val state: StateFlow<EditorUiState> = _state.asStateFlow()

  /**
   * 读一次账目并重置状态。
   *
   * 刻意不放在 init 里：ViewModel 会按 id 复用，若沿用上次的状态，
   * 用户「改到一半按取消」再打开同一笔，会看到那些根本没保存的改动。
   * 改由界面在每次打开时调用（见 openToken）。
   */
  fun load() {
    viewModelScope.launch {
      val categories = repository.observeCategories().first()
      val accounts = repository.observeAccounts().first()
      val existing = editingId?.let { repository.findTransaction(it) }

      _state.value = EditorUiState().let { base ->
        if (existing == null) {
          base.copy(
            categories = categories,
            accounts = accounts,
            accountId = accounts.firstOrNull()?.id,
            ready = true,
          )
        } else {
          base.copy(
            type = existing.type,
            amountInput = centsToInput(existing.amount),
            feeInput = if (existing.fee > 0L) centsToInput(existing.fee) else "",
            selectedCategoryId = existing.categoryId,
            expandedParentId = existing.categoryId
              ?.let { id -> categories.firstOrNull { it.id == id } }
              ?.parentId,
            accountId = existing.accountId,
            toAccountId = existing.toAccountId,
            note = existing.note,
            epochDay = existing.epochDay,
            minuteOfDay = existing.minuteOfDay,
            categories = categories,
            accounts = accounts,
            ready = true,
          )
        }
      }
    }
  }

  fun onKeyPress(key: String) = _state.update { it.copy(amountInput = appendAmountKey(it.amountInput, key)) }

  fun onBackspace() = _state.update { it.copy(amountInput = backspaceAmount(it.amountInput)) }

  fun onFeeKeyPress(key: String) = _state.update { it.copy(feeInput = appendAmountKey(it.feeInput, key)) }

  fun onFeeBackspace() = _state.update { it.copy(feeInput = backspaceAmount(it.feeInput)) }

  /** 转账模式下把键盘焦点切回金额。 */
  fun focusAmount() = _state.update { it.copy(feeFocused = false) }

  /** 转账模式下把键盘焦点切到手续费。 */
  fun focusFee() = _state.update { it.copy(feeFocused = true) }

  /** 切换收支类型时清空分类选择：支出分类不能留作收入分类。 */
  fun selectType(type: TransactionType) = _state.update {
    if (it.type == type) {
      it
    } else {
      it.copy(
        type = type,
        expandedParentId = null,
        selectedCategoryId = null,
        feeFocused = false,
        feeInput = if (type == TransactionType.TRANSFER) it.feeInput else "",
      )
    }
  }

  /**
   * 点一级分类：有子分类就只展开、不选中，避免用户误存一个"大类"；
   * 没有子分类（比如"其他支出"）则直接选中。
   */
  fun selectParent(group: CategoryGroup) = _state.update {
    if (group.children.isEmpty()) {
      it.copy(expandedParentId = null, selectedCategoryId = group.parent.id)
    } else if (it.expandedParentId == group.parent.id) {
      it.copy(expandedParentId = null)
    } else {
      it.copy(expandedParentId = group.parent.id, selectedCategoryId = null)
    }
  }

  fun selectChild(child: CategoryEntity) = _state.update { it.copy(selectedCategoryId = child.id) }

  fun selectAccount(id: Long) = _state.update { it.copy(accountId = id) }

  fun selectTargetAccount(id: Long) = _state.update { it.copy(toAccountId = id) }

  fun setNote(note: String) = _state.update { it.copy(note = note) }

  fun shiftDay(deltaDays: Long) = _state.update { it.copy(epochDay = it.epochDay + deltaDays) }

  fun setDay(epochDay: Long) = _state.update { it.copy(epochDay = epochDay) }

  fun save(onSaved: () -> Unit) {
    val snapshot = _state.value
    if (snapshot.saving || !snapshot.canSave) return
    val amount = snapshot.amountCents ?: return
    val sourceAccountId = snapshot.accountId ?: return
    val isTransfer = snapshot.type == TransactionType.TRANSFER

    // 先锁再发协程：锁进状态里，保存键随之禁用，双击不会插出两笔。
    _state.update { it.copy(saving = true) }
    viewModelScope.launch {
      try {
        // 编辑时保留原始创建时间与图片，只更新账目内容。
        val existing = editingId?.let { repository.findTransaction(it) }
        repository.saveTransaction(
          TransactionEntity(
            id = editingId ?: 0L,
            type = snapshot.type,
            amount = amount,
            fee = if (isTransfer) parseAmountToCents(snapshot.feeInput) ?: 0L else 0L,
            categoryId = if (isTransfer) null else snapshot.selectedCategoryId,
            accountId = sourceAccountId,
            toAccountId = if (isTransfer) snapshot.toAccountId else null,
            epochDay = snapshot.epochDay,
            minuteOfDay = snapshot.minuteOfDay,
            note = snapshot.note.trim(),
            photoUri = existing?.photoUri,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
          ),
        )
        onSaved()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        // 写库失败（存储空间不足等）：解锁让用户能再点保存，而不是永远灰着。
        _state.update { it.copy(saving = false) }
      }
    }
  }

  fun delete(onDeleted: () -> Unit) {
    val id = editingId ?: return
    viewModelScope.launch {
      repository.deleteTransaction(id)
      onDeleted()
    }
  }
}
