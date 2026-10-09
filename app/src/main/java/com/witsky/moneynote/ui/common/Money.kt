package com.witsky.moneynote.ui.common

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 金额显示与解析。全工程金额一律以"分"为单位的 Long 传递，
 * 只有进出界面这两处才发生单位换算，且换算必须走 BigDecimal，不走 Double。
 */
fun formatCents(cents: Long): String {
  val negative = cents < 0
  val absolute = if (negative) -cents else cents
  val yuan = absolute / 100
  val fen = absolute % 100
  val grouped = yuan.toString().reversed().chunked(3).joinToString(",").reversed()
  val text = if (fen == 0L) grouped else "$grouped.${fen.toString().padStart(2, '0')}"
  return if (negative) "-$text" else text
}

/** 带正负号的展示，用于流水行：支出显示 "-12.00"。 */
fun formatCentsSigned(cents: Long): String = when {
  cents > 0L -> "+${formatCents(cents)}"
  else -> formatCents(cents)
}

/**
 * 解析金额输入框的文本为"分"。非法输入（空、只有一个点、多个点、负数）返回 null。
 * 用 BigDecimal 而不是 toDouble：0.07 这类值在二进制浮点里无法精确表示。
 */
fun parseAmountToCents(input: String): Long? {
  val trimmed = input.trim()
  if (trimmed.isEmpty()) return null
  val value = trimmed.toBigDecimalOrNull() ?: return null
  if (value.signum() < 0) return null
  return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()
}

/** 把已存的"分"回填到输入框，去掉无意义的小数尾巴：1200 -> "12"。 */
fun centsToInput(cents: Long): String {
  val yuan = cents / 100
  val fen = cents % 100
  return if (fen == 0L) yuan.toString() else "$yuan.${fen.toString().padStart(2, '0')}"
}

/** 供图表使用的比例，0f..1f；总量为 0 时返回 0。 */
fun fractionOf(part: Long, total: Long): Float =
  if (total <= 0L) 0f else (part.toDouble() / total.toDouble()).toFloat()

/** 记账键盘用：把当前输入与按下的一位按键合并，并限制非法形态。 */
fun appendAmountKey(current: String, key: String): String {
  if (key == ".") {
    if (current.contains('.')) return current
    return if (current.isEmpty()) "0." else current + "."
  }
  // 整数部分最多 9 位，小数最多 2 位，避免出现天文数字把布局撑破。
  val dot = current.indexOf('.')
  if (dot >= 0) {
    if (current.length - dot - 1 >= 2) return current
  } else if (current.length >= 9 && current != "0") {
    return current
  }
  return if (current == "0") key else current + key
}

/** 记账键盘用：退格。 */
fun backspaceAmount(current: String): String =
  if (current.length <= 1) "" else current.dropLast(1)

/** 记账键盘用：键盘上直接敲出 "12.5" 这类值时补全成两位小数再入库。 */
fun BigDecimal.toDisplayCents(): String = setScale(2, RoundingMode.HALF_UP).toPlainString()
