package com.witsky.moneynote.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.witsky.moneynote.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
  @Query("SELECT * FROM category ORDER BY kind ASC, sortOrder ASC, id ASC")
  fun observeAll(): Flow<List<CategoryEntity>>

  /** 备份导出与 CSV 的分类名对照表都需要全量读取。 */
  @Query("SELECT * FROM category")
  suspend fun loadAll(): List<CategoryEntity>

  @Insert
  suspend fun insert(category: CategoryEntity): Long

  @Update
  suspend fun update(category: CategoryEntity)

  @Delete
  suspend fun delete(category: CategoryEntity)

  @Query("SELECT COUNT(*) FROM txn WHERE categoryId = :categoryId")
  suspend fun countTransactions(categoryId: Long): Int

  @Query("SELECT COUNT(*) FROM category WHERE parentId = :parentId")
  suspend fun countChildren(parentId: Long): Int
}
