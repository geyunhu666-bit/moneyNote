package com.witsky.moneynote

import android.content.Context
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.local.MoneyDatabase
import com.witsky.moneynote.data.settings.SettingsRepository

/**
 * 手写的极简依赖容器。
 *
 * 单模块、单仓储、无编译期注入需求，所以不引入 Hilt —— 那会多出一条
 * "Hilt + KSP 版本是否兼容 AGP 9 / Kotlin 2.3"的兼容轴，而这里换不来任何东西。
 * 哪天依赖图真的复杂起来，再换也不迟。
 */
class AppContainer(context: Context) {
  private val database = MoneyDatabase.build(context)

  val repository: BookkeepingRepository = BookkeepingRepository(
    accountDao = database.accountDao(),
    categoryDao = database.categoryDao(),
    transactionDao = database.transactionDao(),
    budgetDao = database.budgetDao(),
    backupDao = database.backupDao(),
  )

  val settingsRepository: SettingsRepository = SettingsRepository(context)
}
