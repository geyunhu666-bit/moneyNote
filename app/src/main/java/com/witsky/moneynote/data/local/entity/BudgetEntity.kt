package com.witsky.moneynote.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 月度预算。
 *
 * [categoryId] 用 0L 表示"整月总预算"，而不是 null。原因是 SQLite 的唯一索引把多个 NULL 视为互不相同，
 * 用 null 做"总预算"会在唯一索引下允许插入多条重复记录。分类 id 由 autoGenerate 从 1 开始，所以 0 是安全哨兵值。
 */
@Entity(tableName = "budget", indices = [Index(value = ["yearMonth", "categoryId"], unique = true)])
data class BudgetEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  /** 形如 202610。 */
  val yearMonth: Int,
  /** 0 表示月度总预算，其余为分类 id。 */
  val categoryId: Long = TOTAL_BUDGET_CATEGORY_ID,
  /** 预算金额，单位：分。 */
  val amount: Long,
) {
  companion object {
    const val TOTAL_BUDGET_CATEGORY_ID = 0L
  }
}
