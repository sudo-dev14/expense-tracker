package com.expensetracker.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.app.AppContainer
import com.expensetracker.app.R
import com.expensetracker.app.ui.displayName
import com.expensetracker.app.ui.components.AppCard
import com.expensetracker.app.ui.components.DonutChart
import com.expensetracker.app.ui.components.DonutSlice
import com.expensetracker.app.ui.components.MerchantAvatar
import com.expensetracker.app.ui.components.RangeChips
import com.expensetracker.app.ui.components.ScreenHeader
import com.expensetracker.app.ui.components.SpendingBars
import com.expensetracker.app.ui.components.TransactionRow
import com.expensetracker.app.ui.components.color
import com.expensetracker.app.ui.theme.AmountStyle
import com.expensetracker.core.analytics.Bucket
import com.expensetracker.core.analytics.Granularity
import com.expensetracker.core.analytics.RangePreset
import com.expensetracker.core.format.Money
import com.expensetracker.core.model.Category
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenTransaction: (Long) -> Unit,
    onAddExpense: () -> Unit,
    onOpenReview: () -> Unit,
    onSeeAll: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val vm: HomeViewModel = viewModel { HomeViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item { ScreenHeader(stringResource(R.string.home_title), onBadgeClick = onOpenPrivacy) }
            item {
                RangeChips(state.preset, state.customRange, vm::selectPreset, vm::selectCustom)
                Spacer(Modifier.height(16.dp))
            }
            item { SummaryCard(state) }
            if (state.reviewCount > 0) {
                item { ReviewBanner(state.reviewCount, onOpenReview) }
            }
            if (!state.autoTrackingOn && !state.loading) {
                item { AutoTrackingOffBanner(onOpenPrivacy) }
            }
            if (!state.loading && state.summary.count == 0) {
                item { EmptyState(onAddExpense) }
            }
            if (state.categories.isNotEmpty()) {
                item {
                    CategoryCard(state) { category ->
                        vm.filterCategory(category)
                        onSeeAll()
                    }
                }
                item { TrendCard(state) }
            }
            if (state.merchants.isNotEmpty()) {
                item { MerchantsCard(state) }
            }
            if (state.recent.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 20.dp),
                    ) {
                        Text(stringResource(R.string.home_recent), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            vm.filterCategory(null)
                            onSeeAll()
                        }) { Text(stringResource(R.string.home_see_all)) }
                    }
                }
                state.recent.forEach { tx ->
                    item(key = tx.id) { TransactionRow(tx, onClick = { onOpenTransaction(tx.id) }) }
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
        FloatingActionButton(
            onClick = onAddExpense,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.home_add_expense)) }
    }
}

// Created at use time, not cached: month names follow the current app language.
private fun monthFormatter(): DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())

/** Bucket dates formatted in the app language, same patterns as core's English `Bucket.label`. */
private fun Bucket.displayLabel(granularity: Granularity): String = when (granularity) {
    Granularity.DAY -> start.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    Granularity.MONTH -> start.format(DateTimeFormatter.ofPattern("MMM yy", Locale.getDefault()))
    Granularity.YEAR -> start.year.toString()
}

@Composable
private fun SummaryCard(state: HomeUiState) {
    val title = when (state.preset) {
        RangePreset.THIS_MONTH -> state.range?.start?.format(monthFormatter())
            ?.let { stringResource(R.string.home_spent_in_month, it) }
            ?: stringResource(R.string.home_spent_this_month)
        RangePreset.ALL_TIME -> stringResource(R.string.home_spent_total)
        else -> stringResource(R.string.home_spent)
    }
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(22.dp),
    ) {
        // Sage teal card: dark text for the amount, teal-tinted labels, darker amber for "up"
        // so it stays readable on the light card.
        val onHero = MaterialTheme.colorScheme.onSurface
        val heroMuted = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        val upColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFFA5620F) else MaterialTheme.colorScheme.tertiary
        Text(title, color = heroMuted, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(Money.format(state.summary.spentMinor), style = AmountStyle, fontSize = 40.sp, color = onHero)
            state.changePercent?.let { change ->
                val up = change > 0
                val comparison = stringResource(state.comparisonLabelRes)
                val changeDescription = stringResource(
                    if (up) R.string.home_change_up_cd else R.string.home_change_down_cd,
                    abs(change),
                    comparison,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .semantics { contentDescription = changeDescription },
                ) {
                    Icon(
                        if (up) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                        contentDescription = null,
                        tint = if (up) upColor else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        stringResource(R.string.home_change_percent, abs(change), comparison),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (up) upColor else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 14.dp), color = onHero.copy(alpha = 0.12f))
        Row {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_income), fontSize = 12.sp, color = heroMuted)
                Text(Money.format(state.summary.incomeMinor), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = onHero)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (state.summary.netMinor >= 0) R.string.home_net_saved else R.string.home_net_overspent), fontSize = 12.sp, color = heroMuted)
                Text(
                    Money.format(state.summary.netMinor, signed = true),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (state.summary.netMinor >= 0) MaterialTheme.colorScheme.primary else upColor,
                )
            }
        }
    }
}

@Composable
private fun ReviewBanner(count: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary),
        ) { Text("$count", color = MaterialTheme.colorScheme.onTertiary, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        Text(
            pluralStringResource(R.plurals.home_review_banner, count),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

@Composable
private fun AutoTrackingOffBanner(onOpenSettings: () -> Unit) {
    AppCard(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp).clickable(onClick = onOpenSettings)) {
        Column {
            Text(stringResource(R.string.home_autotrack_off_title), fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.home_autotrack_off_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
    ) {
        Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.home_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onAdd) { Text(stringResource(R.string.home_add_manually)) }
    }
}

@Composable
private fun CategoryCard(state: HomeUiState, onCategory: (Category) -> Unit) {
    AppCard(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
        Column {
            Text(stringResource(R.string.home_by_category), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            val donutDescription = state.categories
                .map { stringResource(R.string.home_category_share_cd, it.category.displayName(), (it.share * 100).toInt()) }
                .joinToString()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                DonutChart(
                    slices = state.categories.map { DonutSlice(it.category.color, it.share) },
                    modifier = Modifier.size(132.dp).semantics {
                        contentDescription = donutDescription
                    },
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    state.categories.take(6).forEach { c ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onCategory(c.category) }
                                .padding(vertical = 5.dp),
                        ) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(c.category.color))
                            Text(c.category.displayName(), fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1)
                            Text("${(c.share * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendCard(state: HomeUiState) {
    val peak = state.buckets.maxByOrNull { it.amountMinor }
    AppCard(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(stringResource(R.string.home_trend_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (peak != null && peak.amountMinor > 0) {
                    Text(
                        stringResource(R.string.home_peak, Money.compact(peak.amountMinor), peak.displayLabel(state.bucketGranularity)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            SpendingBars(state.buckets, Modifier.fillMaxWidth().height(100.dp))
            if (state.buckets.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text(state.buckets.first().displayLabel(state.bucketGranularity), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text(state.buckets.last().displayLabel(state.bucketGranularity), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MerchantsCard(state: HomeUiState) {
    AppCard(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), padding = 0.dp) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
            Text(stringResource(R.string.home_top_merchants), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 10.dp))
            state.merchants.forEach { m ->
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 10.dp),
                ) {
                    MerchantAvatar(m.name, Category.OTHER, size = 36.dp)
                    Column(Modifier.weight(1f)) {
                        Text(m.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            pluralStringResource(R.plurals.home_payments, m.count, m.count),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(Money.format(m.amountMinor), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
