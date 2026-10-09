package com.witsky.moneynote.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 账户。余额不落库，由 [initialBalance] 加流水聚合实时算出，避免多处写入对不上账。
 * 因此这里只有一个"期初余额"，没有"当前余额"字段。
 */
@Entity(tableName = "account")
data class AccountEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val type: AccountType,
  /** 期初余额，单位：分。 */
  val initialBalance: Long = 0L,
  val icon: String = "💵",
  /** 信用卡等负债账户也计入净资产（余额为负即负债）。 */
  val includeInNetWorth: Boolean = true,
  val isArchived: Boolean = false,
  val sortOrder: Int = 0,
)
