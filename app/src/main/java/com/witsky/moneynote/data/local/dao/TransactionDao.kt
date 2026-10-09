package com.witsky.moneynote.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.witsky.moneynote.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
  // 全量观察 + 内存过滤：月份切换、搜索、日历都在内存里做，排序口径只此一处。
  @Query("SELECT * FROM txn ORDER BY epochDay DESC, minuteOfDay DESC, id DESC")
  fun observeAll(): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM txn WHERE id = :id")
  suspend fun findById(id: Long): TransactionEntity?

  @Insert
  suspend fun insert(transaction: TransactionEntity): Long

  @Update
  suspend fun update(transaction: TransactionEntity)

  @Query("DELETE FROM txn WHERE id = :id")
  suspend fun deleteById(id: Long)

  /** 跨全部时间的累计，用于算账户余额；不做分页，数据量级按个人记账估算。 */
  @Query("SELECT * FROM txn")
  suspend fun loadAll(): List<TransactionEntity>
}
