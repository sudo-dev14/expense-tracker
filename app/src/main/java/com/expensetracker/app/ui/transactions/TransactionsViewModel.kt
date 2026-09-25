package com.expensetracker.app.ui.transactions

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.AppContainer
import com.expensetracker.app.R
import com.expensetracker.app.data.Status
import com.expensetracker.app.data.TransactionEntity
import com.expensetracker.app.data.accountLabel
import com.expensetracker.app.data.categoryEnum
import com.expensetracker.app.ui.home.selectedRange
import com.expensetracker.core.analytics.DateRange
import com.expensetracker.core.analytics.RangePreset
import com.expensetracker.core.format.Money
import com.expensetracker.core.model.Category
import com.expensetracker.core.model.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class TypeFilter(@StringRes val labelRes: Int) {
    ALL(R.string.txn_type_all),
    SPENT(R.string.txn_type_spent),
    RECEIVED(R.string.txn_type_received),
}

data class DayGroup(val date: LocalDate, val netMinor: Long, val items: List<TransactionEntity>)

data class TransactionsUiState(
    val preset: RangePreset = RangePreset.THIS_MONTH,
    val customRange: DateRange? = null,
    val query: String = "",
    val category: Category? = null,
    val type: TypeFilter = TypeFilter.ALL,
    val groups: List<DayGroup> = emptyList(),
    val loading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(private val container: AppContainer) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val query = MutableStateFlow("")
    private val type = MutableStateFlow(TypeFilter.ALL)

    /** Category names in the app language, supplied by the UI so search also matches them. */
    private val categoryNames = MutableStateFlow<Map<Category, String>>(emptyMap())

    private val rangeTxns = container.selectedRange().flatMapLatest { (preset, custom, range) ->
        container.repository.observeInRange(range.startMillis(zone), range.endMillisExclusive(zone))
            .map { Triple(preset, custom, it) }
    }

    val state: StateFlow<TransactionsUiState> = combine(
        rangeTxns, query, container.filters.category, type, categoryNames,
    ) { (preset, custom, txns), q, category, typeFilter, names ->
        val filtered = txns.filter { tx ->
            (category == null || tx.categoryEnum == category) &&
                when (typeFilter) {
                    TypeFilter.ALL -> true
                    TypeFilter.SPENT -> tx.type == TransactionType.DEBIT
                    TypeFilter.RECEIVED -> tx.type == TransactionType.CREDIT
                } &&
                matches(tx, q, names)
        }
        TransactionsUiState(
            preset = preset,
            customRange = custom,
            query = q,
            category = category,
            type = typeFilter,
            groups = filtered
                .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
                .map { (date, items) ->
                    DayGroup(date, items.sumOf { if (it.type == TransactionType.CREDIT) it.amountMinor else -it.amountMinor }, items)
                },
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    private fun matches(tx: TransactionEntity, q: String, names: Map<Category, String>): Boolean {
        if (q.isBlank()) return true
        val needle = q.trim().lowercase()
        val amountText = Money.format(tx.amountMinor).replace(",", "")
        return listOfNotNull(tx.merchant, tx.note, tx.accountLabel, tx.categoryEnum.label, names[tx.categoryEnum], amountText, (tx.amountMinor / 100).toString())
            .any { needle in it.lowercase().replace(",", "") }
    }

    fun setQuery(q: String) { query.value = q }
    fun setType(t: TypeFilter) { type.value = t }
    fun setCategoryNames(names: Map<Category, String>) { categoryNames.value = names }
    fun setCategory(c: Category?) { container.filters.category.value = c }
    fun selectPreset(p: RangePreset) { container.filters.preset.value = p }
    fun selectCustom(r: DateRange) {
        container.filters.customRange.value = r
        container.filters.preset.value = RangePreset.CUSTOM
    }

    fun markNotExpense(id: Long) = viewModelScope.launch { container.repository.setStatus(id, Status.IGNORED) }

    fun undoNotExpense(tx: TransactionEntity) = viewModelScope.launch { container.repository.setStatus(tx.id, tx.status) }
}
