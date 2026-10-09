package com.witsky.moneynote.data

import com.witsky.moneynote.data.local.entity.TransactionEntity
import java.time.LocalDate
import java.time.LocalTime

/**
 * 导出账目明细为 CSV。
 *
 * 两个刻意的取舍：
 * 1. 文件头带 UTF-8 BOM。中文 Windows 的 Excel 默认按本地代码页解析 CSV，
 *    没有 BOM 时中文分类名会整片变成乱码 —— 这是导出功能最常见的投诉。
 * 2. 金额导出成「元」的两位小数，且恒为正数、方向由「类型」列表达。
 *    这张表是给人看、给 Excel 算的，不是给程序读的；带正负号会让 SUM 求和把转账也算进去。
 */
fun transactionsToCsv(
  transactions: List<TransactionEntity>,
  categoryName: Map<Long, String>,
  accountName: Map<Long, String>,
): String {
  val builder = StringBuilder()
  builder.append('\uFEFF')
  builder.append(CSV_HEADER)
  builder.append(CRLF)

  for (txn in transactions) {
    val cells = listOf(
      LocalDate.ofEpochDay(txn.epochDay).toString(),
      LocalTime.of(txn.minuteOfDay / 60, txn.minuteOfDay % 60).toString(),
      txn.type.label,
      txn.categoryId?.let { categoryName[it] }.orEmpty(),
      accountName[txn.accountId].orEmpty(),
      txn.toAccountId?.let { accountName[it] }.orEmpty(),
      yuanText(txn.amount),
      yuanText(txn.fee),
      txn.note,
    )
    builder.append(cells.joinToString(",") { escapeCsvCell(it) })
    builder.append(CRLF)
  }

  return builder.toString()
}

/** 导出文件名带日期，同一天多次导出也会被系统自动改名而不是静默覆盖。 */
fun exportFileName(prefix: String, extension: String, today: LocalDate): String {
  val month = today.monthValue.toString().padStart(2, '0')
  val day = today.dayOfMonth.toString().padStart(2, '0')
  return "$prefix-${today.year}$month$day.$extension"
}

private const val CRLF = "\r\n"

private val CSV_HEADER = listOf(
  "日期", "时间", "类型", "分类", "账户", "转入账户", "金额", "手续费", "备注",
).joinToString(",")

/**
 * RFC 4180：字段里出现逗号、双引号或换行时必须整体加引号，内部的双引号要翻倍。
 * 备注里带逗号是最常见的情况（"午饭,和同事"），不处理会把一行拆成两格。
 */
fun escapeCsvCell(value: String): String {
  val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
  if (!needsQuoting) return value
  return buildString {
    append('"')
    append(value.replace("\"", "\"\""))
    append('"')
  }
}

/** 分 -> "12.50"。不使用 Locale 相关的格式化，避免某些区域设置把小数点写成逗号而破坏 CSV。 */
private fun yuanText(cents: Long): String {
  val negative = cents < 0
  val absolute = if (negative) -cents else cents
  val text = "${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
  return if (negative) "-$text" else text
}
