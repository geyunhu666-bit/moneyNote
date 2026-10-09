package com.witsky.moneynote.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * 输入一个金额的对话框，预算设置与编辑都用它。
 *
 * 这里刻意不用记账键盘：改预算是低频操作，系统键盘顺手且能直接清空重填。
 */
@Composable
fun AmountInputDialog(
  title: String,
  initialCents: Long?,
  onDismiss: () -> Unit,
  onConfirm: (Long) -> Unit,
  onClear: (() -> Unit)? = null,
  label: String = "金额（元）",
) {
  var input by remember { mutableStateOf(initialCents?.let { centsToInput(it) }.orEmpty()) }
  val parsed = parseAmountToCents(input)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column {
        OutlinedTextField(
          value = input,
          onValueChange = { input = it },
          label = { Text(label) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
        )
        if (input.isNotEmpty() && parsed == null) {
          Spacer(Modifier.height(6.dp))
          Text(
            text = "请填一个不超过两位小数的正数",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        enabled = parsed != null && parsed > 0L,
        onClick = { parsed?.let(onConfirm) },
      ) { Text("保存") }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (onClear != null) {
          TextButton(onClick = onClear) { Text("清除") }
        }
        TextButton(onClick = onDismiss) { Text("取消") }
      }
    },
  )
}
