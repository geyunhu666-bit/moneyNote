package com.witsky.moneynote.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.witsky.moneynote.data.local.entity.AccountEntity
import com.witsky.moneynote.data.local.entity.BudgetEntity
import com.witsky.moneynote.data.local.entity.CategoryEntity
import com.witsky.moneynote.data.local.entity.TransactionEntity

/**
 * 只服务于「恢复备份」这一件事：它必须能同时操作四张表。
 *
 * 单独建一个 DAO 而不是把清空/批量插入散到各个 DAO 里，是因为这些操作只有在
 * 「整体替换」这个语境下才合法 —— 放在各自 DAO 上会诱使别处随手 `clearAll()`。
 */
@Dao
abstract class BackupDao {

  @Insert
  abstract suspend fun insertAccounts(items: List<AccountEntity>)

  @Insert
  abstract suspend fun insertCategories(items: List<CategoryEntity>)

  @Insert
  abstract suspend fun insertTransactions(items: List<TransactionEntity>)

  @Insert
  abstract suspend fun insertBudgets(items: List<BudgetEntity>)

  @Query("DELETE FROM txn")
  abstract suspend fun clearTransactions()

  @Query("DELETE FROM budget")
  abstract suspend fun clearBudgets()

  @Query("DELETE FROM category")
  abstract suspend fun clearCategories()

  @Query("DELETE FROM account")
  abstract suspend fun clearAccounts()

  /**
   * 整体替换数据库内容。
   *
   * 必须在同一个事务里：写到一半失败会留下「账目在、分类没了」的残缺数据库，
   * 那比恢复失败本身更糟 —— 用户会以为数据都还在，直到打开流水页看到一片空白。
   *
   * 删除顺序先引用方后被引用方，插入顺序反过来，避免中间态出现悬空引用。
   */
  @Transaction
  open suspend fun replaceAll(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
  ) {
    clearTransactions()
    clearBudgets()
    clearCategories()
    clearAccounts()

    insertAccounts(accounts)
    insertCategories(categories)
    insertTransactions(transactions)
    insertBudgets(budgets)
  }
}
