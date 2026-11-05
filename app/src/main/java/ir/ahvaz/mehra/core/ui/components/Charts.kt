package ir.ahvaz.mehra.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.ahvaz.mehra.core.common.formatThousands
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.ui.theme.MehraTheme

data class DonutSegment(val value: Float, val color: Color, val label: String)

/** Ring chart with a centred total — used for occupancy and payment method splits. */
@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
    centerValue: String? = null,
    centerLabel: String? = null,
    thickness: Dp = 18.dp,
) {
    val emptyColor = MehraTheme.colors.chartGrid
    val total = segments.sumOf { it.value.toDouble() }.toFloat()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = thickness.toPx())
            val inset = thickness.toPx() / 2f
            val arcSize = Size(size.width - thickness.toPx(), size.height - thickness.toPx())
            drawArc(
                color = emptyColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
            if (total <= 0f) return@Canvas
            var startAngle = -90f
            segments.forEach { segment ->
                val sweep = (segment.value / total) * 360f
                if (sweep > 0f) {
                    drawArc(
                        color = segment.color,
                        startAngle = startAngle,
                        sweepAngle = sweep - if (segments.size > 1) 2f else 0f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                        size = arcSize,
                        style = stroke,
                    )
                }
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (centerValue != null) {
                Text(
                    text = centerValue,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (centerLabel != null) {
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ChartLegend(segments: List<DonutSegment>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        segments.forEach { segment ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(segment.color),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = segment.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatThousands(segment.value.toLong()).toPersianDigits(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Simple column chart; values are fractions of [maxValue]. */
@Composable
fun BarSeriesChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = MehraTheme.colors.chartPrimary,
    barHeight: Dp = 110.dp,
    showValueOnTop: Boolean = false,
) {
    val maxValue = (values.maxOrNull() ?: 1f).coerceAtLeast(0.0001f)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, value ->
            val fraction = (value / maxValue).coerceIn(0.02f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showValueOnTop) {
                    Text(
                        text = value.toInt().toString().toPersianDigits(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Box(
                    modifier = Modifier.height(barHeight),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(barHeight * fraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(barColor.copy(alpha = if (index == values.lastIndex) 1f else 0.65f)),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = labels.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Labelled progress rows (payment methods, occupancy by room type, …). */
@Composable
fun HorizontalBarList(
    items: List<Pair<String, Long>>,
    modifier: Modifier = Modifier,
    valueFormatter: (Long) -> String = { formatThousands(it).toPersianDigits() },
    barColor: Color = MehraTheme.colors.chartSecondary,
) {
    val max = (items.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(1L)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { (label, value) ->
            val fraction = (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = valueFormatter(value),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MehraTheme.colors.chartGrid),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor),
                    )
                }
            }
        }
    }
}

/** Compact sparkline of daily revenue (no axes, decorative but honest about trend). */
@Composable
fun TrendBarStrip(
    values: List<Long>,
    modifier: Modifier = Modifier,
    color: Color = MehraTheme.colors.chartSecondary,
    height: Dp = 48.dp,
) {
    val max = (values.maxOrNull() ?: 1L).coerceAtLeast(1L)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        values.forEach { value ->
            val fraction = (value.toFloat() / max.toFloat()).coerceIn(0.03f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(height * fraction)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(color.copy(alpha = 0.8f)),
            )
        }
    }
}
