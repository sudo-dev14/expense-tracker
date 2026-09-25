package com.expensetracker.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.AppContainer
import com.expensetracker.app.data.TransactionEntity
import com.expensetracker.app.data.toPoint
import com.expensetracker.core.analytics.Analytics
import com.expensetracker.core.analytics.Bucket
import com.expensetracker.core.analytics.CategoryTotal
import com.expensetracker.core.analytics.DateRange
import com.expensetracker.core.analytics.DateRanges
import com.expensetracker.core.analytics.MerchantTotal
import com.expensetracker.core.analytics.RangePreset
import com.expensetracker.core.analytics.Summary
import com.expensetracker.core.model.Category
import com.expensetracker.core.model.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val loading: Boolean = true,
    val preset: RangePreset = RangePreset.THIS_MONTH,
    val customRange: DateRange? = null,
    val range: DateRange? = null,
    val summary: Summary = Summary(0, 0, 0),
    val changePercent: Int? = null,
    val comparisonLabel: String = "",
    val categories: List<CategoryTotal> = emptyList(),
    val buckets: List<Bucket> = emptyList(),
    val merchants: List<MerchantTotal> = emptyList(),
    val recent: List<TransactionEntity> = emptyList(),
    val reviewCount: Int = 0,
    val autoTrackingOn: Boolean = true,
)

/** Resolves the shared filter into a concrete date range. Used by several screens. */
@OptIn(ExperimentalCoroutinesApi::class)
fun AppContainer.selectedRange() = combine(
    filters.preset,
    filters.customRange,
    repository.observeEarliestTimestamp(),
) { preset, custom, earliest ->
    val zone = ZoneId.systemDefault()
    val earliestDate = earliest?.let { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
    Triple(preset, custom, DateRanges.resolve(preset, LocalDate.now(zone), custom, earliestDate))
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val zone: ZoneId = ZoneId.systemDefault()

    val state: StateFlow<HomeUiState> = container.selectedRange()
        .flatMapLatest { (preset, custom, range) ->
            val previous = DateRanges.previous(preset, range)
            combine(
                container.repository.observeInRange(range.startMillis(zone), range.endMillisExclusive(zone)),
                container.repository.observeInRange(previous.startMillis(zone), previous.endMillisExclusive(zone)),
                container.repository.observeNeedsReviewCount(),
                container.prefs.autoTracking,
            ) { current, prior, reviewCount, autoTracking ->
                val points = current.map { it.toPoint() }
                val summary = Analytics.summary(points)
                val priorSpent = prior.filter { it.type == TransactionType.DEBIT }.sumOf { it.amountMinor }
                HomeUiState(
                    loading = false,
                    preset = preset,
                    customRange = custom,
                    range = range,
                    summary = summary,
                    // "All time" has nothing before it to compare with.
                    changePercent = if (preset == RangePreset.ALL_TIME) null else Analytics.percentChange(summary.spentMinor, priorSpent),
                    comparisonLabel = DateRanges.comparisonLabel(preset),
                    categories = Analytics.byCategory(points),
                    buckets = Analytics.buckets(points, range, zone),
                    merchants = Analytics.topMerchants(points, 3),
                    recent = current.take(5),
                    reviewCount = reviewCount,
                    autoTrackingOn = autoTracking && container.smsReader.hasPermission(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectPreset(preset: RangePreset) {
        container.filters.preset.value = preset
    }

    fun selectCustom(range: DateRange) {
        container.filters.customRange.value = range
        container.filters.preset.value = RangePreset.CUSTOM
    }

    /** Drill-down: open Transactions filtered to one category. */
    fun filterCategory(category: Category?) {
        container.filters.category.value = category
    }
}
