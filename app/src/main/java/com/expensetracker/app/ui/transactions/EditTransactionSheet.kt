package com.expensetracker.app.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.app.AppContainer
import com.expensetracker.app.R
import com.expensetracker.app.data.Source
import com.expensetracker.app.data.Status
import com.expensetracker.app.data.TransactionEntity
import com.expensetracker.app.data.accountLabel
import com.expensetracker.app.data.categoryEnum
import com.expensetracker.app.ui.NEW_TRANSACTION
import com.expensetracker.app.ui.displayName
import com.expensetracker.app.ui.components.MerchantAvatar
import com.expensetracker.app.ui.components.formatDateTime
import com.expensetracker.core.model.Category
import com.expensetracker.core.model.TransactionType
import com.expensetracker.core.parser.Extractors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

data class EditState(
    val loaded: Boolean = false,
    val original: TransactionEntity? = null,
    val amountText: String = "",
    val type: TransactionType = TransactionType.DEBIT,
    val merchant: String = "",
    val category: Category = Category.OTHER,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val applyToMerchant: Boolean = false,
    val otherWithMerchant: Int = 0,
) {
    val isNew get() = original == null
    val amountMinor: Long? get() = Extractors.parseMinor(amountText.trim())?.takeIf { it > 0 }
    val canApplyRule get() = type == TransactionType.DEBIT && merchant.isNotBlank()
}

class EditTransactionViewModel(private val container: AppContainer, private val id: Long) : ViewModel() {
    private val _state = MutableStateFlow(EditState())
    val state: StateFlow<EditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (id == NEW_TRANSACTION) {
                _state.value = EditState(loaded = true)
                return@launch
            }
            val tx = container.db.transactions().getById(id) ?: return@launch
            _state.value = EditState(
                loaded = true,
                original = tx,
                amountText = (tx.amountMinor / 100.0).let { if (tx.amountMinor % 100 == 0L) (tx.amountMinor / 100).toString() else "%.2f".format(Locale.ROOT, it) },
                type = tx.type,
                merchant = tx.merchant.orEmpty(),
                category = tx.categoryEnum,
                note = tx.note.orEmpty(),
                timestamp = tx.timestamp,
                otherWithMerchant = container.repository.countOthersWithMerchant(tx),
                applyToMerchant = tx.merchant != null && tx.type == TransactionType.DEBIT,
            )
        }
    }

    fun update(block: (EditState) -> EditState) = _state.update(block)

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val amount = s.amountMinor ?: return
        viewModelScope.launch {
            val original = s.original
            if (original == null) {
                container.repository.addManual(amount, s.type, s.merchant, s.category, s.timestamp, s.note)
            } else {
                container.repository.saveEdits(
                    original.copy(
                        amountMinor = amount,
                        type = s.type,
                        merchant = s.merchant.trim().ifEmpty { null },
                        category = if (s.type == TransactionType.CREDIT && original.type != s.type) Category.INCOME.key else s.category.key,
                        note = s.note.trim().ifEmpty { null },
                        timestamp = s.timestamp,
                    ),
                    applyToMerchant = s.applyToMerchant && s.canApplyRule,
                )
            }
            onDone()
        }
    }

    fun markNotExpense(onDone: () -> Unit) = viewModelScope.launch {
        container.repository.setStatus(id, Status.IGNORED)
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        _state.value.original?.let { container.repository.delete(it) }
        onDone()
    }
}

private const val CLOSE_DRAG_FRACTION = 0.35f

@OptIn(ExperimentalMaterial3Api::class)
private fun draggedFarEnoughToClose(state: SheetState, restOffset: Float, sheetHeight: Float): Boolean {
    val offset = runCatching { state.requireOffset() }.getOrNull() ?: return true
    if (restOffset.isNaN() || sheetHeight <= 0f) return true
    val dragged = offset - restOffset
    // Not dragged at all: Back or a tap outside the sheet, which still close it.
    if (dragged < 1f) return true
    return dragged >= sheetHeight * CLOSE_DRAG_FRACTION
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTransactionSheet(container: AppContainer, transactionId: Long, onDismiss: () -> Unit) {
    // A fresh key per opening, so "Add" never shows the previous entry's leftovers.
    val vmKey = remember(transactionId) { "edit-$transactionId-${System.nanoTime()}" }
    val vm: EditTransactionViewModel = viewModel(key = vmKey) { EditTransactionViewModel(container, transactionId) }
    val state by vm.state.collectAsStateWithLifecycle()
    // Swiping down only closes the sheet once it has been dragged at least 35% of its height;
    // shorter drags and quick flicks snap it back. Back and tapping outside still close it.
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }
    var restOffsetPx by remember { mutableFloatStateOf(Float.NaN) }
    lateinit var sheetState: SheetState
    sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            target != SheetValue.Hidden || draggedFarEnoughToClose(sheetState, restOffsetPx, sheetHeightPx)
        },
    )
    // Remember where the sheet rests when fully open, so a drag can be measured from there.
    LaunchedEffect(sheetState, sheetHeightPx) {
        restOffsetPx = Float.NaN
        snapshotFlow { runCatching { sheetState.requireOffset() }.getOrNull() }.collect { offset ->
            if (offset != null && sheetState.currentValue == SheetValue.Expanded && (restOffsetPx.isNaN() || offset < restOffsetPx)) {
                restOffsetPx = offset
            }
        }
    }
    var showSms by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        if (!state.loaded) return@ModalBottomSheet
        Column(
            Modifier
                .onSizeChanged { sheetHeightPx = it.height.toFloat() }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            val original = state.original
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val newAvatar = stringResource(R.string.edit_new_avatar)
                MerchantAvatar(state.merchant.ifBlank { if (state.isNew) newAvatar else "?" }, state.category, size = 52.dp)
                Column(Modifier.weight(1f)) {
                    val unknownPayee = stringResource(R.string.edit_unknown_payee)
                    Text(if (state.isNew) stringResource(R.string.edit_add_title) else state.merchant.ifBlank { unknownPayee }, style = MaterialTheme.typography.titleLarge)
                    Text(
                        state.timestamp.formatDateTime(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { showDatePicker = true },
                    )
                }
            }
            Spacer(Modifier.height(18.dp))

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(TransactionType.DEBIT to stringResource(R.string.txn_type_spent), TransactionType.CREDIT to stringResource(R.string.txn_type_received)).forEachIndexed { i, (t, label) ->
                    SegmentedButton(
                        selected = state.type == t,
                        onClick = { vm.update { it.copy(type = t, category = if (t == TransactionType.CREDIT) Category.INCOME else if (it.category == Category.INCOME) Category.OTHER else it.category) } },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                    ) { Text(label) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = { v -> vm.update { it.copy(amountText = v.filter { c -> c.isDigit() || c == '.' || c == ',' }) } },
                    label = { Text(stringResource(R.string.edit_amount)) },
                    singleLine = true,
                    isError = state.amountText.isNotEmpty() && state.amountMinor == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.merchant,
                    onValueChange = { v -> vm.update { it.copy(merchant = v) } },
                    label = { Text(stringResource(if (state.type == TransactionType.CREDIT) R.string.edit_from else R.string.edit_paid_to)) },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f),
                )
            }

            if (state.type == TransactionType.DEBIT) {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.edit_category), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Category.spendCategories.forEach { c ->
                        FilterChip(
                            selected = state.category == c,
                            onClick = { vm.update { it.copy(category = c) } },
                            label = { Text(c.displayName()) },
                            shape = RoundedCornerShape(18.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
            }

            if (state.canApplyRule && !state.isNew) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { vm.update { it.copy(applyToMerchant = !it.applyToMerchant) } }
                        .padding(end = 14.dp, top = 4.dp, bottom = 4.dp),
                ) {
                    Checkbox(checked = state.applyToMerchant, onCheckedChange = { c -> vm.update { it.copy(applyToMerchant = c) } })
                    val merchant = state.merchant.trim()
                    val categoryName = state.category.displayName()
                    Text(
                        if (state.otherWithMerchant > 0) {
                            pluralStringResource(
                                R.plurals.edit_rule_with_updates,
                                state.otherWithMerchant,
                                merchant,
                                categoryName,
                                state.otherWithMerchant,
                            )
                        } else {
                            stringResource(R.string.edit_rule, merchant, categoryName)
                        },
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (original != null && (original.accountLabel != null || original.channel.name != "OTHER")) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    original.accountLabel?.let { InfoBox(stringResource(R.string.edit_account), it, Modifier.weight(1f)) }
                    if (original.channel.name != "OTHER") InfoBox(stringResource(R.string.edit_paid_via), original.channel.displayName(), Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.note,
                onValueChange = { v -> vm.update { it.copy(note = v) } },
                label = { Text(stringResource(R.string.edit_note)) },
                modifier = Modifier.fillMaxWidth(),
            )

            original?.rawSms?.let { sms ->
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showSms = !showSms }
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            original.sender?.let { stringResource(R.string.edit_original_message_from, it) }
                                ?: stringResource(R.string.edit_original_message),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(if (showSms) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                    }
                    if (showSms) {
                        Spacer(Modifier.height(8.dp))
                        Text(sms, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    original == null -> {}
                    original.source == Source.MANUAL -> OutlinedButton(
                        onClick = { vm.delete(onDismiss) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                    ) { Text(stringResource(R.string.action_delete)) }
                    else -> OutlinedButton(
                        onClick = { vm.markNotExpense(onDismiss) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                    ) { Text(stringResource(R.string.txn_not_expense)) }
                }
                Button(
                    onClick = { vm.save(onDismiss) },
                    enabled = state.amountMinor != null,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                ) { Text(stringResource(if (state.isNew) R.string.edit_add else R.string.action_save), fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val zone = ZoneId.systemDefault()
        val current = Instant.ofEpochMilli(state.timestamp).atZone(zone)
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = current.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        val date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        val time: LocalTime = current.toLocalTime()
                        vm.update { it.copy(timestamp = date.atTime(time).atZone(zone).toInstant().toEpochMilli()) }
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(pickerState) }
    }
}

@Composable
private fun InfoBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
