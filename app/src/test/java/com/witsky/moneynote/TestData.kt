package com.witsky.moneynote

import com.witsky.moneynote.data.local.entity.AccountEntity
import com.witsky.moneynote.data.local.entity.AccountType
import com.witsky.moneynote.data.local.entity.CategoryEntity
import com.witsky.moneynote.data.local.entity.CategoryKind
import com.witsky.moneynote.data.local.entity.TransactionEntity
import com.witsky.moneynote.data.local.entity.TransactionType
import java.time.LocalDate

/** 单测里构造账目的公共工厂，免得每个测试文件各写一遍长长的参数列表。 */
fun txn(
  id: Long,
  type: TransactionType,
  amount: Long,
  epochDay: Long,
  categoryId: Long? = null,
  accountId: Long = 1L,
  toAccountId: Long? = null,
  note: String = "",
  fee: Long = 0L,
  minuteOfDay: Int = 12 * 60,
): TransactionEntity = TransactionEntity(
  id = id,
  type = type,
  amount = amount,
  fee = fee,
  categoryId = categoryId,
  accountId = accountId,
  toAccountId = toAccountId,
  epochDay = epochDay,
  minuteOfDay = minuteOfDay,
  note = note,
  createdAt = 1_700_000_000_000L,
  updatedAt = 1_700_000_000_000L,
)

fun dayOf(year: Int, month: Int, day: Int): Long = LocalDate.of(year, month, day).toEpochDay()

fun category(
  id: Long,
  name: String,
  kind: CategoryKind,
  parentId: Long? = null,
  icon: String = "📦",
): CategoryEntity = CategoryEntity(id = id, name = name, kind = kind, parentId = parentId, icon = icon)

fun account(id: Long, name: String, type: AccountType = AccountType.CASH): AccountEntity =
  AccountEntity(id = id, name = name, type = type)
