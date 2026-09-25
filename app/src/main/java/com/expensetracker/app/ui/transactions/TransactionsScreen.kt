package com.expensetracker.app.ui.transactions

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.app.AppContainer
import com.expensetracker.app.R
import com.expensetracker.app.data.TransactionEntity
import com.expensetracker.app.ui.components.DAY_HEADER
import com.expensetracker.app.ui.displayName
import com.expensetracker.app.ui.components.RangeChips
import com.expensetracker.app.ui.components.ScreenHeader
import com.expensetracker.app.ui.components.SectionLabel
import com.expensetracker.app.ui.components.TransactionRow
import com.expensetracker.core.format.Money
import com.expensetracker.core.model.Category
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun TransactionsScreen(
    container: AppContainer,
    onOpenTransaction: (Long) -> Unit,
    onAddExpense: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val vm: TransactionsViewModel = viewModel { TransactionsViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val markedMessage = stringResource(R.string.txn_marked_not_expense)
    val undoLabel = stringResource(R.string.action_undo)

    // Let search match category names in the app language as well as English.
    val categoryNames = Category.entries.associateWith { it.displayName() }
    LaunchedEffect(categoryNames) { vm.setCategoryNames(categoryNames) }

    fun hide(tx: TransactionEntity) {
        vm.markNotExpense(tx.id)
        scope.launch {
            val result = snackbar.showSnackbar(markedMessage, actionLabel = undoLabel, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) vm.undoNotExpense(tx)
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item { ScreenHeader(stringResource(R.string.txn_title), onBadgeClick = onOpenPrivacy) }
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::setQuery,
                    placeholder = { Text(stringResource(R.string.txn_search_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.txn_clear_search)) }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
                RangeChips(state.preset, state.customRange, vm::selectPreset, vm::selectCustom)
                FilterRow(state, vm)
            }
            if (!state.loading && state.groups.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.txn_empty_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.txn_empty_body),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            state.groups.forEach { group ->
                item(key = "h-${group.date}") { DayHeader(group) }
                items(group.items, key = { it.id }) { tx ->
                    SwipeableRow(tx, onClick = { onOpenTransaction(tx.id) }, onNotExpense = { hide(tx) })
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
        ) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.txn_add_expense)) }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }
}

@Composable
private fun FilterRow(state: TransactionsUiState, vm: TransactionsViewModel) {
    var categoryMenu by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
    ) {
        item {
            Box {
                FilterChip(
                    selected = state.category != null,
                    onClick = { categoryMenu = true },
                    label = { Text(state.category?.displayName() ?: stringResource(R.string.txn_filter_category)) },
                    trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
                )
                DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.txn_all_categories)) }, onClick = { vm.setCategory(null); categoryMenu = false })
                    Category.entries.forEach { c ->
                        DropdownMenuItem(text = { Text(c.displayName()) }, onClick = { vm.setCategory(c); categoryMenu = false })
                    }
                }
            }
        }
        item {
            Box {
                FilterChip(
                    selected = state.type != TypeFilter.ALL,
                    onClick = { typeMenu = true },
                    label = { Text(stringResource(state.type.labelRes)) },
                    trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
                )
                DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                    TypeFilter.entries.forEach { t ->
                        DropdownMenuItem(text = { Text(stringResource(t.labelRes)) }, onClick = { vm.setType(t); typeMenu = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(group: DayGroup) {
    val today = LocalDate.now()
    val label = when (group.date) {
        today -> stringResource(R.string.txn_today)
        today.minusDays(1) -> stringResource(R.string.txn_yesterday)
        else -> group.date.format(DAY_HEADER)
    }
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)) {
        SectionLabel(label, Modifier.weight(1f))
        SectionLabel(Money.format(group.netMinor, signed = true))
    }
}

/** Swipe left to mark "Not an expense" (with undo). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRow(tx: TransactionEntity, onClick: () -> Unit, onNotExpense: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onNotExpense()
            false // the row disappears via the data update; don't keep it swiped
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.tertiary)
                    .padding(end = 20.dp),
            ) {
                Text(stringResource(R.string.txn_not_expense), color = MaterialTheme.colorScheme.onTertiary, fontWeight = FontWeight.Bold)
            }
        },
    ) {
        TransactionRow(tx, onClick = onClick, modifier = Modifier.background(MaterialTheme.colorScheme.background))
    }
}
