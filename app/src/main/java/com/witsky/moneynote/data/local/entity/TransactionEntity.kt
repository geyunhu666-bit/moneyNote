package com.witsky.moneynote.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一条账目。
 *
 * 金额单位统一为"分"，用 Long 存，绝不用浮点 —— 记账本最忌讳 0.1+0.2 这类误差。
 * 日期存 [epochDay]（自 1970-01-01 起的天数）与 [minuteOfDay]（当天第几分钟），
 * 按月/按日分组就是纯整数运算，不涉及时区与字符串解析。
 */
@Entity(
  tableName = "txn",
  indices = [Index("epochDay"), Index("accountId"), Index("categoryId"), Index("toAccountId")],
)
data class TransactionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val type: TransactionType,
  /** 金额，单位：分，恒为正数，方向由 [type] 决定。 */
  val amount: Long,
  /** 手续费，单位：分。目前仅转账使用。 */
  val fee: Long = 0L,
  /** 转账没有分类，此字段为 null。 */
  val categoryId: Long? = null,
  /** 支出/收入的账户；转账时为转出账户。 */
  val accountId: Long,
  /** 仅转账使用：转入账户。 */
  val toAccountId: Long? = null,
  val epochDay: Long,
  val minuteOfDay: Int,
  val note: String = "",
  val photoUri: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
) {
  /** 这笔账对该 [accountId] 账户余额的影响，单位：分。 */
  val signedAmountForSource: Long
    get() = when (type) {
      TransactionType.EXPENSE -> -(amount + fee)
      TransactionType.INCOME -> amount
      TransactionType.TRANSFER -> -(amount + fee)
    }
}
