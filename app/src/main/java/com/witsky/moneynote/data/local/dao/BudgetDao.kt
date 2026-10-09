package com.witsky.moneynote.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.witsky.moneynote.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
  @Query("SELECT * FROM budget WHERE yearMonth = :yearMonth")
  fun observeMonth(yearMonth: Int): Flow<List<BudgetEntity>>

  /** 预算表本来就小（每月几条），一次全取再在内存里按月过滤，省掉按需重建 Flow 的麻烦。 */
  @Query("SELECT * FROM budget")
  fun observeAll(): Flow<List<BudgetEntity>>

  @Query("SELECT * FROM budget")
  suspend fun loadAll(): List<BudgetEntity>

  /** 预算按 (yearMonth, categoryId) 唯一，重复设置即覆盖，故用 Upsert 而非 Insert。 */
  @Upsert
  suspend fun upsert(budget: BudgetEntity)

  @Query("DELETE FROM budget WHERE yearMonth = :yearMonth AND categoryId = :categoryId")
  suspend fun delete(yearMonth: Int, categoryId: Long)
}
