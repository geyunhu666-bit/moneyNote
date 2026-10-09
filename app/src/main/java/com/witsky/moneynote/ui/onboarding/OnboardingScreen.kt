package com.witsky.moneynote.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 首次启动的一次性说明页。
 *
 * 只讲三件用户真正会关心的事（怎么记、能看什么、数据在哪），
 * 不做多页引导 —— 记账本的价值在记第一笔账，不在看引导。
 */
@Composable
fun OnboardingScreen(onStart: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = "记账本",
      style = MaterialTheme.typography.displaySmall,
      fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(6.dp))
    Text(
      text = "一本只存在这台手机里的账簿",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(32.dp))

    Point(icon = "✍️", title = "几秒记一笔", body = "自带金额键盘和二级分类，不用切输入法")
    Point(icon = "📊", title = "知道钱花在哪", body = "分类统计、日历回顾，还能设月度预算")
    Point(icon = "🔒", title = "数据不出手机", body = "不联网、不要账号；记得偶尔导出一份备份")

    Spacer(Modifier.height(40.dp))

    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
      Text("开始记账", fontSize = 16.sp)
    }
  }
}

@Composable
private fun Point(icon: String, title: String, body: String) {
  Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
    Text(icon, fontSize = 22.sp)
    Spacer(Modifier.width(14.dp))
    Column {
      Text(title, style = MaterialTheme.typography.titleSmall)
      Spacer(Modifier.height(2.dp))
      Text(
        text = body,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
