package com.witsky.moneynote

import com.witsky.moneynote.data.settings.AppSettings
import com.witsky.moneynote.data.settings.ThemeMode
import com.witsky.moneynote.data.settings.toThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {

  @Test
  fun `主题存档值能正确解析`() {
    assertEquals(ThemeMode.DARK, "DARK".toThemeMode())
    assertEquals(ThemeMode.LIGHT, "LIGHT".toThemeMode())
    assertEquals(ThemeMode.SYSTEM, "SYSTEM".toThemeMode())
  }

  @Test
  fun `存档值损坏时回落跟随系统`() {
    assertEquals(ThemeMode.SYSTEM, "dark".toThemeMode())
    assertEquals(ThemeMode.SYSTEM, "某个没见过的值".toThemeMode())
    assertEquals(ThemeMode.SYSTEM, (null as String?).toThemeMode())
  }

  @Test
  fun `默认设置是不打扰用户的组合`() {
    val defaults = AppSettings()

    assertEquals(ThemeMode.SYSTEM, defaults.themeMode)
    assertTrue(defaults.dynamicColor)
    assertFalse(defaults.onboardingDone)
    // 还没从磁盘读出来，界面据此避免按默认主题闪一下
    assertFalse(defaults.loaded)
  }
}
