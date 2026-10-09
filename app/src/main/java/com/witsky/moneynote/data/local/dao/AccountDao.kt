package com.witsky.moneynote.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.witsky.moneynote.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
  @Query("SELECT * FROM account WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC")
  fun observeActive(): Flow<List<AccountEntity>>

  @Query("SELECT * FROM account ORDER BY sortOrder ASC, id ASC")
  fun observeAll(): Flow<List<AccountEntity>>

  /** 备份导出需要一次性把所有账户（含已归档）拿出来。 */
  @Query("SELECT * FROM account")
  suspend fun loadAll(): List<AccountEntity>

  @Insert
  suspend fun insert(account: AccountEntity): Long

  @Update
  suspend fun update(account: AccountEntity)

  @Delete
  suspend fun delete(account: AccountEntity)

  /** 账户一旦有流水就不能删，否则历史账目会指向不存在的账户。 */
  @Query("SELECT COUNT(*) FROM txn WHERE accountId = :accountId OR toAccountId = :accountId")
  suspend fun countTransactions(accountId: Long): Int
}
