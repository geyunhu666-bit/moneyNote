package com.witsky.moneynote

import com.witsky.moneynote.data.escapeCsvCell
import com.witsky.moneynote.data.exportFileName
import com.witsky.moneynote.data.local.entity.TransactionType
import com.witsky.moneynote.data.transactionsToCsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvExportTest {

  private val categoryNames = mapOf(1L to "餐饮")
  private val accountNames = mapOf(1L to "现金", 2L to "招行卡")

  private fun csv(transactions: List<com.witsky.moneynote.data.local.entity.TransactionEntity>) =
    transactionsToCsv(transactions, categoryNames, accountNames)

  @Test
  fun `文件头带 UTF-8 BOM 否则 Excel 打开是乱码`() {
    val text = csv(listOf(txn(1L, TransactionType.EXPENSE, 100L, dayOf(2026, 10, 5), categoryId = 1L)))

    assertTrue(text.startsWith("\uFEFF"))
  }

  @Test
  fun `表头与首行内容正确`() {
    val text = csv(
      listOf(txn(1L, TransactionType.EXPENSE, 1250L, dayOf(2026, 10, 5), categoryId = 1L, note = "午饭")),
    )
    val lines = text.removePrefix("\uFEFF").split("\r\n")

    assertEquals("日期,时间,类型,分类,账户,转入账户,金额,手续费,备注", lines[0])
    assertEquals("2026-10-05,12:00,支出,餐饮,现金,,12.50,0.00,午饭", lines[1])
    // 末尾有一个换行产生的空串
    assertEquals("", lines[2])
  }

  @Test
  fun `金额固定两位小数且不带千分位`() {
    val text = csv(listOf(txn(1L, TransactionType.INCOME, 800000L, dayOf(2026, 10, 5), categoryId = 1L)))
    val row = text.removePrefix("\uFEFF").split("\r\n")[1]

    assertTrue(row.contains("8000.00"))
    assertTrue(!row.contains("8,000.00"))
  }

  @Test
  fun `备注里的逗号与引号按 RFC4180 转义`() {
    val text = csv(
      listOf(txn(1L, TransactionType.EXPENSE, 100L, dayOf(2026, 10, 5), categoryId = 1L, note = "午饭,\"和同事\"")),
    )
    val row = text.removePrefix("\uFEFF").split("\r\n")[1]

    assertTrue(row.endsWith("\"午饭,\"\"和同事\"\"\""))
  }

  @Test
  fun `转账导出转入账户名`() {
    val text = csv(
      listOf(
        txn(1L, TransactionType.TRANSFER, 50000L, dayOf(2026, 10, 11), accountId = 1L, toAccountId = 2L),
      ),
    )
    val row = text.removePrefix("\uFEFF").split("\r\n")[1]

    assertEquals("2026-10-11,12:00,转账,,现金,招行卡,500.00,0.00,", row)
  }

  @Test
  fun `空数据只导出表头`() {
    val lines = csv(emptyList()).removePrefix("\uFEFF").split("\r\n")

    assertEquals(1, lines.count { it.isNotEmpty() })
  }

  @Test
  fun `转义函数只在必要时加引号`() {
    assertEquals("午饭", escapeCsvCell("午饭"))
    assertEquals("\"午饭,和同事\"", escapeCsvCell("午饭,和同事"))
    assertEquals("\"说\"\"你好\"\"\"", escapeCsvCell("说\"你好\""))
    assertEquals("\"第一行\n第二行\"", escapeCsvCell("第一行\n第二行"))
  }

  @Test
  fun `导出文件名带日期且月份日期补零`() {
    assertEquals("记账本-20261009.csv", exportFileName("记账本", "csv", LocalDate.of(2026, 10, 9)))
    assertEquals("moneynote-20260101.json", exportFileName("moneynote", "json", LocalDate.of(2026, 1, 1)))
  }
}
