package com.witsky.moneynote.ui.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.DeleteOutcome
import com.witsky.moneynote.data.local.entity.AccountEntity
import com.witsky.moneynote.data.local.entity.AccountType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AssetItem(val account: AccountEntity, val balance: Long)

data class AssetsUiState(val netWorth: Long, val items: List<AssetItem>)

class AssetsViewModel(private val repository: BookkeepingRepository) : ViewModel() {

  val uiState: StateFlow<AssetsUiState?> =
    combine(repository.observeAccounts(), repository.observeAccountBalances()) { accounts, balances ->
      val active = accounts.filter { !it.isArchived }
      AssetsUiState(
        // 信用卡余额为负，因此"净资产 = 各项余额之和"这一条公式天然把负债算进去了。
        netWorth = active.filter { it.includeInNetWorth }.sumOf { balances[it.id] ?: it.initialBalance },
        items = active.map { AssetItem(it, balances[it.id] ?: it.initialBalance) },
      )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  fun addAccount(
    name: String,
    type: AccountType,
    initialBalance: Long,
    icon: String,
    onDone: () -> Unit,
  ) {
    if (name.isBlank()) return
    viewModelScope.launch {
      repository.addAccount(name, type, initialBalance, icon)
      onDone()
    }
  }

  fun updateAccount(
    account: AccountEntity,
    name: String,
    type: AccountType,
    initialBalance: Long,
    icon: String,
    onDone: () -> Unit,
  ) {
    if (name.isBlank()) return
    viewModelScope.launch {
      repository.updateAccount(
        account.copy(name = name.trim(), type = type, initialBalance = initialBalance, icon = icon),
      )
      onDone()
    }
  }

  fun deleteAccount(account: AccountEntity, onResult: (DeleteOutcome) -> Unit) {
    viewModelScope.launch { onResult(repository.deleteAccount(account)) }
  }
}
