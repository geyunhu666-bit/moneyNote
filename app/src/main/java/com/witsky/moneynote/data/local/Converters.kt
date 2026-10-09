package com.witsky.moneynote.data.local

import androidx.room.TypeConverter
import com.witsky.moneynote.data.local.entity.AccountType
import com.witsky.moneynote.data.local.entity.CategoryKind
import com.witsky.moneynote.data.local.entity.TransactionType

/**
 * 枚举以 name 字符串落库，可读性优先，且后续增删枚举值不会静默错位。
 *
 * 读取时认不出就回落默认值（与 BackupPayload 的解析同口径）：
 * `valueOf` 遇到未知字符串会抛异常，那会让每一次普通查询都崩掉整个应用 ——
 * 比如用户降级安装了旧版本，库里留着新版本写入的枚举名。
 */
class Converters {
  @TypeConverter
  fun toAccountType(value: String): AccountType =
    AccountType.entries.firstOrNull { it.name == value } ?: AccountType.CASH

  @TypeConverter
  fun fromAccountType(value: AccountType): String = value.name

  @TypeConverter
  fun toCategoryKind(value: String): CategoryKind =
    CategoryKind.entries.firstOrNull { it.name == value } ?: CategoryKind.EXPENSE

  @TypeConverter
  fun fromCategoryKind(value: CategoryKind): String = value.name

  @TypeConverter
  fun toTransactionType(value: String): TransactionType =
    TransactionType.entries.firstOrNull { it.name == value } ?: TransactionType.EXPENSE

  @TypeConverter
  fun fromTransactionType(value: TransactionType): String = value.name
}
