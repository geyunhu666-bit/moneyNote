package com.witsky.moneynote.data

import com.witsky.moneynote.data.local.entity.AccountEntity
import com.witsky.moneynote.data.local.entity.AccountType
import com.witsky.moneynote.data.local.entity.BudgetEntity
import com.witsky.moneynote.data.local.entity.CategoryEntity
import com.witsky.moneynote.data.local.entity.CategoryKind
import com.witsky.moneynote.data.local.entity.TransactionEntity
import com.witsky.moneynote.data.local.entity.TransactionType
import kotlinx.serialization.Serializable

/**
 * 备份文件的结构。
 *
 * 刻意不直接序列化 Room 实体，而是多一层 DTO：
 * 实体上挂着 `@Entity`、默认值又依赖 `System.currentTimeMillis()`，直接序列化会把
 * 「数据库结构」和「文件格式」绑死，将来改表就必须同时兼容旧备份文件。
 * 有这层 DTO，文件格式由它说了算，改表时只改映射。
 *
 * 枚举一律存字符串，且解析时容忍未知值：将来新增账户类型后，旧版本 App 仍能打开新备份
 * （未知类型回落到默认值），而不是整个文件解析失败、用户以为备份坏了。
 */
@Serializable
data class BackupPayload(
  val format: String = CURRENT_FORMAT,
  val exportedAt: Long = 0L,
  val accounts: List<AccountRecord> = emptyList(),
  val categories: List<CategoryRecord> = emptyList(),
  val transactions: List<TransactionRecord> = emptyList(),
  val budgets: List<BudgetRecord> = emptyList(),
) {
  val totalRecords: Int get() = accounts.size + categories.size + transactions.size + budgets.size

  companion object {
    const val CURRENT_FORMAT = "moneynote-backup-v1"
  }
}

@Serializable
data class AccountRecord(
  val id: Long,
  val name: String,
  val type: String,
  val initialBalance: Long,
  val icon: String,
  val includeInNetWorth: Boolean,
  val isArchived: Boolean,
  val sortOrder: Int,
)

@Serializable
data class CategoryRecord(
  val id: Long,
  val name: String,
  val kind: String,
  val parentId: Long? = null,
  val icon: String,
  val sortOrder: Int,
  val isSystem: Boolean,
)

@Serializable
data class TransactionRecord(
  val id: Long,
  val type: String,
  val amount: Long,
  val fee: Long,
  val categoryId: Long? = null,
  val accountId: Long,
  val toAccountId: Long? = null,
  val epochDay: Long,
  val minuteOfDay: Int,
  val note: String,
  val photoUri: String? = null,
  val createdAt: Long,
  val updatedAt: Long,
)

@Serializable
data class BudgetRecord(
  val id: Long,
  val yearMonth: Int,
  val categoryId: Long,
  val amount: Long,
)

// ---- 实体 -> 记录 ----

fun AccountEntity.toRecord(): AccountRecord = AccountRecord(
  id = id,
  name = name,
  type = type.name,
  initialBalance = initialBalance,
  icon = icon,
  includeInNetWorth = includeInNetWorth,
  isArchived = isArchived,
  sortOrder = sortOrder,
)

fun CategoryEntity.toRecord(): CategoryRecord = CategoryRecord(
  id = id,
  name = name,
  kind = kind.name,
  parentId = parentId,
  icon = icon,
  sortOrder = sortOrder,
  isSystem = isSystem,
)

fun TransactionEntity.toRecord(): TransactionRecord = TransactionRecord(
  id = id,
  type = type.name,
  amount = amount,
  fee = fee,
  categoryId = categoryId,
  accountId = accountId,
  toAccountId = toAccountId,
  epochDay = epochDay,
  minuteOfDay = minuteOfDay,
  note = note,
  photoUri = photoUri,
  createdAt = createdAt,
  updatedAt = updatedAt,
)

fun BudgetEntity.toRecord(): BudgetRecord = BudgetRecord(
  id = id,
  yearMonth = yearMonth,
  categoryId = categoryId,
  amount = amount,
)

// ---- 记录 -> 实体 ----

fun AccountRecord.toEntity(): AccountEntity = AccountEntity(
  id = id,
  name = name,
  type = accountTypeOf(type),
  initialBalance = initialBalance,
  icon = icon,
  includeInNetWorth = includeInNetWorth,
  isArchived = isArchived,
  sortOrder = sortOrder,
)

fun CategoryRecord.toEntity(): CategoryEntity = CategoryEntity(
  id = id,
  name = name,
  kind = categoryKindOf(kind),
  parentId = parentId,
  icon = icon,
  sortOrder = sortOrder,
  isSystem = isSystem,
)

fun TransactionRecord.toEntity(): TransactionEntity = TransactionEntity(
  id = id,
  type = transactionTypeOf(type),
  amount = amount,
  fee = fee,
  categoryId = categoryId,
  accountId = accountId,
  toAccountId = toAccountId,
  epochDay = epochDay,
  minuteOfDay = minuteOfDay,
  note = note,
  photoUri = photoUri,
  createdAt = createdAt,
  updatedAt = updatedAt,
)

fun BudgetRecord.toEntity(): BudgetEntity = BudgetEntity(
  id = id,
  yearMonth = yearMonth,
  categoryId = categoryId,
  amount = amount,
)

// 认不出来的枚举名回落到默认值：宁可少一项语义，也不要让整个备份打不开。

private fun accountTypeOf(raw: String): AccountType =
  AccountType.entries.firstOrNull { it.name == raw } ?: AccountType.CASH

private fun categoryKindOf(raw: String): CategoryKind =
  CategoryKind.entries.firstOrNull { it.name == raw } ?: CategoryKind.EXPENSE

private fun transactionTypeOf(raw: String): TransactionType =
  TransactionType.entries.firstOrNull { it.name == raw } ?: TransactionType.EXPENSE
