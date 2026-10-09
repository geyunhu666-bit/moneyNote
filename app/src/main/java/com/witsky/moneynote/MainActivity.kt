package com.witsky.moneynote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.witsky.moneynote.data.settings.AppSettings
import com.witsky.moneynote.data.settings.ThemeMode
import com.witsky.moneynote.theme.MoneyNoteTheme
import com.witsky.moneynote.ui.MoneyNoteApp
import com.witsky.moneynote.ui.onboarding.OnboardingScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val container = (application as MoneyNoteApplication).container

    setContent {
      val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())

      val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
      }

      MoneyNoteTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          val scope = rememberCoroutineScope()

          when {
            // 设置还没从磁盘读出来时先留白：否则会先按默认主题渲染一帧，
            // 用户选了深色的话能看见明显的闪白。
            !settings.loaded -> Box(modifier = Modifier.fillMaxSize())

            !settings.onboardingDone -> OnboardingScreen(
              onStart = { scope.launch { container.settingsRepository.setOnboardingDone() } },
            )

            else -> MoneyNoteApp(
              repository = container.repository,
              settingsRepository = container.settingsRepository,
            )
          }
        }
      }
    }
  }
}
