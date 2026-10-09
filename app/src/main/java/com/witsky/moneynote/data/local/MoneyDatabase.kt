package com.witsky.moneynote.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.witsky.moneynote.data.local.dao.AccountDao
import com.witsky.moneynote.data.local.dao.BackupDao
import com.witsky.moneynote.data.local.dao.BudgetDao
import com.witsky.moneynote.data.local.dao.CategoryDao
import com.witsky.moneynote.data.local.dao.TransactionDao
import com.witsky.moneynote.data.local.entity.AccountEntity
import com.witsky.moneynote.data.local.entity.BudgetEntity
import com.witsky.moneynote.data.local.entity.CategoryEntity
import com.witsky.moneynote.data.local.entity.TransactionEntity

@Database(
  entities = [
    AccountEntity::class,
    CategoryEntity::class,
    TransactionEntity::class,
    BudgetEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class MoneyDatabase : RoomDatabase() {
  abstract fun accountDao(): AccountDao
  abstract fun categoryDao(): CategoryDao
  abstract fun transactionDao(): TransactionDao
  abstract fun budgetDao(): BudgetDao

  /** 新增 DAO 不改变表结构，因此 version 保持 1，不需要迁移。 */
  abstract fun backupDao(): BackupDao

  companion object {
    private const val NAME = "moneynote.db"

    fun build(context: Context): MoneyDatabase =
      Room.databaseBuilder(context.applicationContext, MoneyDatabase::class.java, NAME)
        .addCallback(object : Callback() {
          override fun onCreate(db: SupportSQLiteDatabase) {
            // 必须同步写完：协程插入会让首屏出现"分类还没准备好"的空窗。
            SeedData.statements().forEach(db::execSQL)
          }
        })
        .build()
  }
}
