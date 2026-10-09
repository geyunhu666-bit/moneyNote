package com.witsky.moneynote.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.witsky.moneynote.data.BookkeepingRepository
import com.witsky.moneynote.data.local.entity.TransactionType
import com.witsky.moneynote.ui.common.AppDate
import com.witsky.moneynote.ui.common.ExpenseColor
import com.witsky.moneynote.ui.common.IncomeColor
import com.witsky.moneynote.ui.common.MonthSwitcher
import com.witsky.moneynote.ui.common.chartColorAt
import com.witsky.moneynote.ui.common.formatCents
import com.witsky.moneynote.ui.common.fractionOf

@Composable
fun StatsScreen(repository: BookkeepingRepository, modifier: Modifier = Modifier) {
  val viewModel: StatsViewModel = viewModel { StatsViewModel(repository) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  Column(modifier = modifier.fillMaxSize()) {
    MonthSwitcher(
      label = state?.let { AppDate.formatMonth(it.month) } ?: "",
      onPrevious = viewModel::previousMonth,
      onNext = viewModel::nextMonth,
    )

    KindSelector(
      selected = state?.kind ?: TransactionType.EXPENSE,
      onSelect = viewModel::selectKind,
    )

    val slices = state?.slices.orEmpty()
    val total = state?.total ?: 0L

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
      item {
        DonutPanel(slices = slices, total = total, kind = state?.kind ?: TransactionType.EXPENSE)
      }

      if (state != null && slices.isEmpty()) {
        item {
          Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("这个月没有可统计的账目", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }

      itemsIndexed(items = slices, key = { _, slice -> slice.categoryId }) { index, slice ->
        RankingRow(index = index, slice = slice, total = total)
      }
    }
  }
}

@Composable
private fun KindSelector(selected: TransactionType, onSelect: (TransactionType) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.Center,
  ) {
    listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
      val active = selected == type
      Surface(
        onClick = { onSelect(type) },
        shape = RoundedCornerShape(50),
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (active) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(end = 8.dp),
      ) {
        Text(
          text = "${type.label}构成",
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}

@Composable
private fun DonutPanel(slices: List<CategorySlice>, total: Long, kind: TransactionType) {
  Card(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
      Box(contentAlignment = Alignment.Center) {
        DonutChart(
          slices = slices,
          total = total,
          trackColor = trackColor,
          modifier = Modifier.size(180.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = if (kind == TransactionType.INCOME) "收入合计" else "支出合计",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text = "¥ ${formatCents(total)}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (kind == TransactionType.INCOME) IncomeColor else ExpenseColor,
          )
        }
      }
    }
  }
}

@Composable
private fun DonutChart(
  slices: List<CategorySlice>,
  total: Long,
  trackColor: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val strokeWidth = size.minDimension * 0.20f
    val diameter = size.minDimension - strokeWidth
    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
    val arcSize = Size(diameter, diameter)

    if (total <= 0L || slices.isEmpty()) {
      drawArc(
        color = trackColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth),
      )
      return@Canvas
    }

    var startAngle = -90f
    slices.forEachIndexed { index, slice ->
      val sweep = slice.amount.toFloat() / total.toFloat() * 360f
      drawArc(
        color = chartColorAt(index),
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth),
      )
      startAngle += sweep
    }
  }
}

@Composable
private fun RankingRow(index: Int, slice: CategorySlice, total: Long) {
  val fraction = fractionOf(slice.amount, total)

  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier.size(32.dp).background(chartColorAt(index), CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Text(slice.icon, fontSize = 15.sp)
    }
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(slice.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
          text = formatCents(slice.amount),
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
        )
      }
      Spacer(Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier.weight(1f).height(5.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp)),
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(fraction)
              .height(5.dp)
              .background(chartColorAt(index), RoundedCornerShape(3.dp)),
          )
        }
        Spacer(Modifier.width(10.dp))
        Text(
          text = "${(fraction * 100).toInt()}%",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
