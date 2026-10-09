package com.witsky.moneynote

import com.witsky.moneynote.ui.common.appendAmountKey
import com.witsky.moneynote.ui.common.backspaceAmount
import com.witsky.moneynote.ui.common.centsToInput
import com.witsky.moneynote.ui.common.formatCents
import com.witsky.moneynote.ui.common.parseAmountToCents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatTest {

  @Test
  fun `格式化整数金额不带小数尾`() {
    assertEquals("0", formatCents(0L))
    assertEquals("12", formatCents(1200L))
    assertEquals("100", formatCents(10000L))
  }

  @Test
  fun `格式化带分金额补足两位`() {
    assertEquals("0.50", formatCents(50L))
    assertEquals("12.05", formatCents(1205L))
  }

  @Test
  fun `千分位分组`() {
    assertEquals("1,234.56", formatCents(123456L))
    assertEquals("1,000,000", formatCents(100000000L))
  }

  @Test
  fun `负数金额用于支出展示`() {
    assertEquals("-0.50", formatCents(-50L))
    assertEquals("-1,234.56", formatCents(-123456L))
  }

  @Test
  fun `解析金额输入为分`() {
    assertEquals(1200L, parseAmountToCents("12")!!)
    assertEquals(1250L, parseAmountToCents("12.5")!!)
    assertEquals(7L, parseAmountToCents("0.07")!!)
    assertEquals(0L, parseAmountToCents("0")!!)
  }

  @Test
  fun `非法金额输入返回空`() {
    assertNull(parseAmountToCents(""))
    assertNull(parseAmountToCents("."))
    assertNull(parseAmountToCents("1.2.3"))
    assertNull(parseAmountToCents("abc"))
  }

  @Test
  fun `金额回填去掉多余小数尾`() {
    assertEquals("12", centsToInput(1200L))
    assertEquals("12.05", centsToInput(1205L))
  }

  @Test
  fun `键盘输入限制小数位与重复小数点`() {
    assertEquals("1", appendAmountKey("", "1"))
    assertEquals("0.", appendAmountKey("", "."))
    assertEquals("12", appendAmountKey("1", "2"))
    // 非空且无小数点时按下点号，应拼接小数点
    assertEquals("12.", appendAmountKey("12", "."))
    // 已有小数点时再按点号无效
    assertEquals("1.2", appendAmountKey("1.2", "."))
    // 小数最多两位
    assertEquals("1.23", appendAmountKey("1.23", "4"))
    // 前导 0 被替换而不是拼成 01
    assertEquals("5", appendAmountKey("0", "5"))
  }

  @Test
  fun `退格清空到空串`() {
    assertEquals("", backspaceAmount("1"))
    assertEquals("1", backspaceAmount("12"))
  }
}
