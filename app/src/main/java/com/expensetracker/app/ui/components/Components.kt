package com.expensetracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.data.TransactionEntity
import com.expensetracker.app.data.accountLabel
import com.expensetracker.app.data.categoryEnum
import com.expensetracker.app.data.displayMerchant
import com.expensetracker.core.analytics.Bucket
import com.expensetracker.core.analytics.DateRange
import com.expensetracker.core.analytics.RangePreset
import com.expensetracker.core.format.Money
import com.expensetracker.core.model.Category
import com.expensetracker.core.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

val Category.color: Color get() = Color(colorArgb)

@Composable
fun isDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Category colour made readable as text on the current background. */
@Composable
fun Category.textColor(): Color = if (isDark()) lerp(color, Color.White, 0.5f) else color

/** The lock + "Offline" pill shown in every top bar. */
@Composable
fun OfflineBadge(onClick: (() -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .height(32.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = "Offline. Your data stays on this phone." },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
            Text("Offline", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ScreenHeader(title: String, onBadgeClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        OfflineBadge(onBadgeClick)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun AppCard(modifier: Modifier = Modifier, padding: Dp = 18.dp, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(padding),
    ) { content() }
}

/** Chip row: This month · 3M · 6M · 1Y · 3Y · All · Custom. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeChips(
    selected: RangePreset,
    customRange: DateRange?,
    onSelect: (RangePreset) -> Unit,
    onCustomRange: (DateRange) -> Unit,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
) {
    var showPicker by remember { mutableStateOf(false) }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = contentPadding) {
        items(RangePreset.entries) { preset ->
            val label = if (preset == RangePreset.CUSTOM && selected == RangePreset.CUSTOM && customRange != null) {
                "${customRange.start.format(SHORT_DATE)} – ${customRange.endInclusive.format(SHORT_DATE)}"
            } else preset.label
            FilterChip(
                selected = preset == selected,
                onClick = { if (preset == RangePreset.CUSTOM) showPicker = true else onSelect(preset) },
                label = { Text(label) },
                shape = RoundedCornerShape(18.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                    selectedLabelColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    }
    if (showPicker) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = customRange?.start?.toUtcMillis(),
            initialSelectedEndDateMillis = customRange?.endInclusive?.toUtcMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        val start = state.selectedStartDateMillis!!.utcToLocalDate()
                        val end = (state.selectedEndDateMillis ?: state.selectedStartDateMillis!!).utcToLocalDate()
                        onCustomRange(DateRange(start, maxOf(start, end)))
                        showPicker = false
                    },
                ) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) {
            DateRangePicker(state = state, modifier = Modifier.weight(1f))
        }
    }
}

private fun LocalDate.toUtcMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.utcToLocalDate() = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Rounded square with the merchant's initials, tinted by category. */
@Composable
fun MerchantAvatar(name: String, category: Category, size: Dp = 40.dp) {
    val initials = name.split(' ', '.', '-').filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
        .let { if (it.length == 1 && name.length > 1) name.take(2).uppercase() else it }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(category.color.copy(alpha = if (isDark()) 0.28f else 0.14f)),
    ) {
        Text(initials, color = category.textColor(), fontWeight = FontWeight.Bold, fontSize = (size.value * 0.33f).sp)
    }
}

@Composable
fun TransactionRow(tx: TransactionEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val credit = tx.type == TransactionType.CREDIT
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        MerchantAvatar(tx.displayMerchant, tx.categoryEnum)
        Column(Modifier.weight(1f)) {
            Text(tx.displayMerchant, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(tx.categoryEnum.label, tx.accountLabel, tx.channel.label.takeIf { tx.channel.name != "OTHER" })
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            Money.format(if (credit) tx.amountMinor else -tx.amountMinor, signed = true),
            fontWeight = FontWeight.Bold,
            color = if (credit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

data class DonutSlice(val color: Color, val fraction: Float)

@Composable
fun DonutChart(slices: List<DonutSlice>, modifier: Modifier = Modifier, strokeWidth: Dp = 18.dp) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)
        drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        var start = -90f
        val gap = if (slices.size > 1) 1.5f else 0f
        slices.forEach { s ->
            val sweep = 360f * s.fraction
            if (sweep > gap) drawArc(s.color, start, sweep - gap, false, topLeft, arcSize, style = Stroke(stroke))
            start += sweep
        }
    }
}

/** Simple bar chart; the tallest bar is highlighted in amber. */
@Composable
fun SpendingBars(buckets: List<Bucket>, modifier: Modifier = Modifier) {
    val bar = MaterialTheme.colorScheme.primary
    val peak = MaterialTheme.colorScheme.tertiary
    val baseline = MaterialTheme.colorScheme.outlineVariant
    val max = buckets.maxOfOrNull { it.amountMinor } ?: 0L
    Canvas(modifier) {
        drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
        if (buckets.isEmpty() || max == 0L) return@Canvas
        val slot = size.width / buckets.size
        val barWidth = (slot * 0.7f).coerceAtMost(28.dp.toPx())
        buckets.forEachIndexed { i, b ->
            if (b.amountMinor == 0L) return@forEachIndexed
            val h = (b.amountMinor.toFloat() / max * size.height).coerceAtLeast(2.dp.toPx())
            drawRoundRect(
                color = if (b.amountMinor == max) peak else bar,
                topLeft = Offset(i * slot + (slot - barWidth) / 2, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            )
        }
    }
}

@Composable
fun VerticalSpace(height: Dp) = Spacer(Modifier.height(height))

@Composable
fun HorizontalSpace(width: Dp) = Spacer(Modifier.width(width))

val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
val FULL_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)
val DAY_HEADER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun Long.formatDateTime(zone: ZoneId = ZoneId.systemDefault()): String = Instant.ofEpochMilli(this).atZone(zone).format(DATE_TIME)
