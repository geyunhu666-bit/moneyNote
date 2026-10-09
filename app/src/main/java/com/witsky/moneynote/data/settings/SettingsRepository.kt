package com.witsky.moneynote.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) {
  SYSTEM("跟随系统"),
  LIGHT("浅色"),
  DARK("深色"),
}

data class AppSettings(
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  /** Android 12+ 的动态取色（Material You）。老系统上这个开关不起作用。 */
  val dynamicColor: Boolean = true,
  val onboardingDone: Boolean = false,
  /** 设置还没从磁盘读出来。界面据此避免先按默认主题闪一下再跳成用户选的主题。 */
  val loaded: Boolean = false,
)

/**
 * DataStore 的委托必须建在文件级。
 *
 * 若写在类里，每次构造 SettingsRepository 都会新建一个 DataStore 实例去操作同一个文件，
 * DataStore 会直接抛「同一个文件被多个实例占用」—— 这是它最常见的误用。
 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
  private val appContext = context.applicationContext

  val settings: Flow<AppSettings> = appContext.settingsDataStore.data.map { prefs ->
    AppSettings(
      themeMode = prefs[KEY_THEME_MODE].toThemeMode(),
      dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: true,
      onboardingDone = prefs[KEY_ONBOARDING_DONE] ?: false,
      loaded = true,
    )
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    appContext.settingsDataStore.edit { it[KEY_THEME_MODE] = mode.name }
  }

  suspend fun setDynamicColor(enabled: Boolean) {
    appContext.settingsDataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
  }

  suspend fun setOnboardingDone() {
    appContext.settingsDataStore.edit { it[KEY_ONBOARDING_DONE] = true }
  }

  private companion object {
    val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
  }
}

/** 存档值可能来自更早的版本或被手工改坏，认不出来就回落默认值，不要崩。 */
fun String?.toThemeMode(): ThemeMode =
  ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM
