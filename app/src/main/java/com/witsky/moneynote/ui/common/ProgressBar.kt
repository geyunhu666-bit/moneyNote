package com.witsky.moneynote.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 通用进度条。预算页与流水页的预算条共用，避免两处各写一套宽高比。
 * [fraction] 超过 1 会被夹住：超支时进度条拉满即可，不该溢出容器。
 */
@Composable
fun ProgressBar(
  fraction: Float,
  color: Color,
  modifier: Modifier = Modifier,
  height: Dp = 8.dp,
) {
  val shape = RoundedCornerShape(height / 2)
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), shape),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(fraction.coerceIn(0f, 1f))
        .height(height)
        .background(color, shape),
    )
  }
}
