package com.witsky.moneynote

import com.witsky.moneynote.data.BackupCodec
import com.witsky.moneynote.data.BackupFormatException
import com.witsky.moneynote.data.BackupPayload
import com.witsky.moneynote.data.local.entity.AccountEntity
import com.witsky.moneynote.data.local.entity.AccountType
import com.witsky.moneynote.data.local.entity.BudgetEntity
import com.witsky.moneynote.data.local.entity.CategoryKind
import com.witsky.moneynote.data.local.entity.TransactionType
import com.witsky.moneynote.data.toEntity
import com.witsky.moneynote.data.toRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {

  private val accounts = listOf(
    AccountEntity(id = 1L, name = "现金", type = AccountType.CASH, initialBalance = 12345L, icon = "💵"),
    AccountEntity(id = 2L, name = "招行卡", type = AccountType.DEBIT, initialBalance = -500L, icon = "🏦"),
  )
  private val categories = listOf(
    category(1L, "餐饮", CategoryKind.EXPENSE),
    category(11L, "早餐", CategoryKind.EXPENSE, parentId = 1L),
  )
  private val transactions = listOf(
    txn(1L, TransactionType.EXPENSE, 1250L, dayOf(2026, 10, 5), categoryId = 11L, note = "包子,豆浆"),
    // 转账的分类是 null，必须能原样往返
    txn(2L, TransactionType.TRANSFER, 50000L, dayOf(2026, 10, 11), accountId = 2L, toAccountId = 1L, fee = 200L),
  )
  private val budgets = listOf(
    BudgetEntity(id = 1L, yearMonth = 202610, categoryId = 0L, amount = 300000L),
    BudgetEntity(id = 2L, yearMonth = 202610, categoryId = 1L, amount = 100000L),
  )

  private fun payload() = BackupPayload(
    exportedAt = 1_700_000_000_000L,
    accounts = accounts.map { it.toRecord() },
    categories = categories.map { it.toRecord() },
    transactions = transactions.map { it.toRecord() },
    budgets = budgets.map { it.toRecord() },
  )

  @Test
  fun `备份往返内容完全一致`() {
    val original = payload()

    val decoded = BackupCodec.decode(BackupCodec.encode(original))

    assertEquals(original, decoded)
    // 2 账户 + 2 分类 + 2 账目 + 2 预算
    assertEquals(8, decoded.totalRecords)
  }

  @Test
  fun `记录还原成实体后与原始实体一致`() {
    val decoded = payload().let { BackupCodec.decode(BackupCodec.encode(it)) }

    assertEquals(accounts, decoded.accounts.map { it.toEntity() })
    assertEquals(categories, decoded.categories.map { it.toEntity() })
    assertEquals(transactions, decoded.transactions.map { it.toEntity() })
    assertEquals(budgets, decoded.budgets.map { it.toEntity() })
  }

  @Test
  fun `备注里的逗号引号换行不会破坏文件`() {
    val tricky = listOf(
      txn(1L, TransactionType.EXPENSE, 100L, dayOf(2026, 10, 5), categoryId = 1L, note = "说\"你好\"，\n再来一行"),
    )
    val payload = BackupPayload(transactions = tricky.map { it.toRecord() })

    val decoded = BackupCodec.decode(BackupCodec.encode(payload))

    assertEquals("说\"你好\"，\n再来一行", decoded.transactions.single().note)
  }

  @Test
  fun `认不出来的枚举值回落到默认值而不是让整个备份打不开`() {
    val json = """
      {
        "format": "moneynote-backup-v1",
        "accounts": [
          {"id":1,"name":"未来账户","type":"CRYPTO_WALLET","initialBalance":0,
           "icon":"x","includeInNetWorth":true,"isArchived":false,"sortOrder":0}
        ]
      }
    """.trimIndent()

    val decoded = BackupCodec.decode(json)

    assertEquals(AccountType.CASH, decoded.accounts.single().toEntity().type)
    assertEquals("未来账户", decoded.accounts.single().name)
  }

  @Test
  fun `多出来的未知字段被忽略`() {
    val json = """
      {
        "format": "moneynote-backup-v1",
        "somethingFromTheFuture": {"nested": [1,2,3]}
      }
    """.trimIndent()

    val decoded = BackupCodec.decode(json)

    assertEquals(0, decoded.totalRecords)
  }

  @Test
  fun `格式版本不认识时报错而不是静默导入`() {
    val json = """{"format":"some-other-app-v9"}"""

    try {
      BackupCodec.decode(json)
      fail("应该抛出 BackupFormatException")
    } catch (e: BackupFormatException) {
      assertTrue(e.message!!.contains("some-other-app-v9"))
    }
  }

  @Test
  fun `内容损坏时报错而不是导入半截数据`() {
    try {
      BackupCodec.decode("这不是 JSON")
      fail("应该抛出 BackupFormatException")
    } catch (e: BackupFormatException) {
      assertTrue(e.message!!.isNotEmpty())
    }
  }
}
