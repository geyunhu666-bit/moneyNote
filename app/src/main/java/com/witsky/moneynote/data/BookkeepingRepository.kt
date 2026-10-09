package com.witsky.moneynote.data

import com.witsky.moneynote.data.local.dao.AccountDao
import com.witsky.moneynote.data.local.dao.BackupDao
import com.witsky.moneynote.data.local.dao.BudgetDao
import com.witsky.moneynote.data.local.dao.CategoryDao
import com.witsky.moneynote.data.local.dao.TransactionDao
import com.witsky.moneynote.data.local.entity.AccountEntity
import com.witsky.moneynote.data.local.entity.BudgetEntity
import com.witsky.moneynote.data.local.entity.CategoryEntity
import com.witsky.moneynote.data.local.entity.CategoryKind
import com.witsky.moneynote.data.local.entity.TransactionEntity
import com.witsky.moneynote.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** 删除操作的三种结局：成功、被引用而拒绝、还有子项而拒绝。 */
enum class DeleteOutcome { DELETED, REFUSED_IN_USE, REFUSED_HAS_CHILDREN }

/**
 * 记账本唯一的写入口。视图层不直接碰 DAO。
 *
 * 余额与收支汇总都是**推导值**，没有任何冗余存储：账目是唯一事实来源，
 * 因此不存在"余额字段和流水对不上"这类需要修复的数据。
 */
class BookkeepingRepository(
  private val accountDao: AccountDao,
  private val categoryDao: CategoryDao,
  private val transactionDao: TransactionDao,
  private val budgetDao: BudgetDao,
  private val backupDao: BackupDao,
) {

  fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

  fun observeAccounts(): Flow<List<AccountEntity>> = accountDao.observeAll()

  /**
   * 全部账目。界面按月切换时在内存里过滤，而不是每月发一次 SQL：
   * 个人记账量级下这让月份切换是瞬时的，也让账单页与统计页共用同一份数据口径。
   */
  fun observeTransactions(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

  /**
   * 账户余额 = 期初余额 + 全部流水的影响。
   *
   * 这里在内存里聚合而不是写聚合 SQL：个人记账的数据量级（几千条）完全撑得住，
   * 换来的是"余额口径"只有 [TransactionEntity.signedAmountForSource] 一处定义。
   * 若某天数据量真的涨到几万条，再把聚合下推到 SQL。
   */
  fun observeAccountBalances(): Flow<Map<Long, Long>> =
    combine(accountDao.observeAll(), transactionDao.observeAll()) { accounts, transactions ->
      val balances = accounts.associate { it.id to it.initialBalance }.toMutableMap()
      for (txn in transactions) {
        balances.merge(txn.accountId, txn.signedAmountForSource, Long::plus)
        if (txn.type == TransactionType.TRANSFER) {
          txn.toAccountId?.let { balances.merge(it, txn.amount, Long::plus) }
        }
      }
      balances
    }

  fun observeBudgets(yearMonth: Int): Flow<List<BudgetEntity>> = budgetDao.observeMonth(yearMonth)

  /**
   * 全部月份的预算。预算表本来就是每月几条的小表，一次全取再在内存里按当前月份过滤，
   * 比让界面在每次切换月份时重建一个 Flow 简单得多，也不会漏掉正在编辑的那个月。
   */
  fun observeAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.observeAll()

  // ---- 账目 ----

  /** id 为 0 视为新增，否则更新。返回账目 id。 */
  suspend fun saveTransaction(transaction: TransactionEntity): Long {
    val now = System.currentTimeMillis()
    return if (transaction.id == 0L) {
      transactionDao.insert(transaction.copy(createdAt = now, updatedAt = now))
    } else {
      transactionDao.update(transaction.copy(updatedAt = now))
      transaction.id
    }
  }

  suspend fun findTransaction(id: Long): TransactionEntity? = transactionDao.findById(id)

  suspend fun deleteTransaction(id: Long) = transactionDao.deleteById(id)

  // ---- 分类 ----

  suspend fun addCategory(
    name: String,
    kind: CategoryKind,
    parentId: Long?,
    icon: String,
  ): Long = categoryDao.insert(
    CategoryEntity(name = name.trim(), kind = kind, parentId = parentId, icon = icon, sortOrder = 999),
  )

  suspend fun renameCategory(category: CategoryEntity, name: String, icon: String) =
    categoryDao.update(category.copy(name = name.trim(), icon = icon))

  /**
   * 分类被账目引用或还有子分类时拒绝删除。返回结局而不是抛异常，
   * 让界面能给出"该分类下还有 3 笔账"这类具体提示。
   */
  suspend fun deleteCategory(category: CategoryEntity): DeleteOutcome {
    if (categoryDao.countChildren(category.id) > 0) return DeleteOutcome.REFUSED_HAS_CHILDREN
    if (categoryDao.countTransactions(category.id) > 0) return DeleteOutcome.REFUSED_IN_USE
    categoryDao.delete(category)
    return DeleteOutcome.DELETED
  }

  // ---- 账户 ----

  suspend fun addAccount(
    name: String,
    type: com.witsky.moneynote.data.local.entity.AccountType,
    initialBalance: Long,
    icon: String,
  ): Long = accountDao.insert(
    AccountEntity(name = name.trim(), type = type, initialBalance = initialBalance, icon = icon, sortOrder = 999),
  )

  suspend fun updateAccount(account: AccountEntity) = accountDao.update(account)

  suspend fun deleteAccount(account: AccountEntity): DeleteOutcome {
    if (accountDao.countTransactions(account.id) > 0) return DeleteOutcome.REFUSED_IN_USE
    accountDao.delete(account)
    return DeleteOutcome.DELETED
  }

  // ---- 预算 ----

  suspend fun setBudget(yearMonth: Int, categoryId: Long, amount: Long) =
    budgetDao.upsert(BudgetEntity(yearMonth = yearMonth, categoryId = categoryId, amount = amount))

  suspend fun clearBudget(yearMonth: Int, categoryId: Long) =
    budgetDao.delete(yearMonth, categoryId)

  // ---- 导出、备份与恢复 ----

  /**
   * 导出账目明细为 CSV 文本。
   *
   * 放在仓储里而不是界面层：界面只需要拿到一段字符串去写文件，
   * 「导出内容长什么样」属于数据口径，和「选哪个文件保存」是两件事。
   */
  suspend fun exportTransactionsCsv(): String = transactionsToCsv(
    transactions = transactionDao.loadAll(),
    categoryName = categoryDao.loadAll().associate { it.id to it.name },
    accountName = accountDao.loadAll().associate { it.id to it.name },
  )

  suspend fun exportBackup(): BackupPayload = BackupPayload(
    exportedAt = System.currentTimeMillis(),
    accounts = accountDao.loadAll().map { it.toRecord() },
    categories = categoryDao.loadAll().map { it.toRecord() },
    transactions = transactionDao.loadAll().map { it.toRecord() },
    budgets = budgetDao.loadAll().map { it.toRecord() },
  )

  /**
   * 用备份整体覆盖当前数据。
   *
   * 这是全工程破坏性最强的一次写入，所以依然从这唯一的写入口走 ——
   * 界面层不直接握 DAO，"谁能改数据"始终只有一个答案。
   */
  suspend fun restoreBackup(payload: BackupPayload) = backupDao.replaceAll(
    accounts = payload.accounts.map { it.toEntity() },
    categories = payload.categories.map { it.toEntity() },
    transactions = payload.transactions.map { it.toEntity() },
    budgets = payload.budgets.map { it.toEntity() },
  )
}

// 这里曾有一个 MonthSnapshot.spentIn(categoryId)：它只统计该分类自身的支出，
// 不含子分类。预算口径需要把子分类一起算进来，已由 data/BudgetProgress.kt 的
// monthSpentIn 取代。留着旧的那个只会诱使后来者用错口径，所以删掉。
