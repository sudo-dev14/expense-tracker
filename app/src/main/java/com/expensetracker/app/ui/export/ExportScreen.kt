package com.expensetracker.app.ui.export

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.app.AppContainer
import com.expensetracker.app.data.TransactionEntity
import com.expensetracker.app.export.ExportOptions
import com.expensetracker.app.ui.components.AppCard
import com.expensetracker.app.ui.components.FULL_DATE
import com.expensetracker.app.ui.components.RangeChips
import com.expensetracker.app.ui.components.SectionLabel
import com.expensetracker.core.analytics.DateRange
import com.expensetracker.core.analytics.DateRanges
import com.expensetracker.core.analytics.RangePreset
import com.expensetracker.core.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ExportUiState(
    val preset: RangePreset = RangePreset.LAST_3_MONTHS,
    val customRange: DateRange? = null,
    val range: DateRange? = null,
    val options: ExportOptions = ExportOptions(),
    val transactions: List<TransactionEntity> = emptyList(),
) {
    val includedCount get() = if (options.includeIncome) transactions.size else transactions.count { it.type == TransactionType.DEBIT }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModel(private val container: AppContainer) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val preset = MutableStateFlow(RangePreset.LAST_3_MONTHS)
    private val custom = MutableStateFlow<DateRange?>(null)
    private val options = MutableStateFlow(ExportOptions())

    val state = combine(preset, custom, container.repository.observeEarliestTimestamp()) { p, c, earliest ->
        val earliestDate = earliest?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        Triple(p, c, DateRanges.resolve(p, LocalDate.now(zone), c, earliestDate))
    }.flatMapLatest { (p, c, range) ->
        combine(container.repository.observeInRange(range.startMillis(zone), range.endMillisExclusive(zone)), options) { txns, opts ->
            ExportUiState(p, c, range, opts, txns)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExportUiState())

    fun selectPreset(p: RangePreset) { preset.value = p }
    fun selectCustom(r: DateRange) { custom.value = r; preset.value = RangePreset.CUSTOM }
    fun setOptions(o: ExportOptions) { options.value = o }

    val pages = state.map { container.pdfExporter.estimatePages(it.includedCount, it.options) }

    fun fileName(): String = state.value.range?.let { container.pdfExporter.fileName(it) } ?: "expenses.pdf"

    suspend fun saveTo(uri: Uri, resolver: ContentResolver): Boolean = withContext(Dispatchers.IO) {
        val s = state.value
        val range = s.range ?: return@withContext false
        val txns = container.repository.inRange(range.startMillis(zone), range.endMillisExclusive(zone))
        runCatching {
            resolver.openOutputStream(uri)?.use { container.pdfExporter.write(it, range, txns, s.options) } != null
        }.getOrDefault(false)
    }

    suspend fun shareUri(): Uri? = withContext(Dispatchers.IO) {
        val s = state.value
        val range = s.range ?: return@withContext null
        val txns = container.repository.inRange(range.startMillis(zone), range.endMillisExclusive(zone))
        container.pdfExporter.writeForSharing(range, txns, s.options)
    }
}

@Composable
fun ExportScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: ExportViewModel = viewModel { ExportViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val pages by vm.pages.collectAsStateWithLifecycle(1)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch {
            val ok = vm.saveTo(uri, context.contentResolver)
            snackbar.showSnackbar(if (ok) "Report saved" else "Couldn't save the report")
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(56.dp).padding(horizontal = 8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Text("Export report", style = MaterialTheme.typography.titleMedium)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            AppCard(Modifier.padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    Column {
                        state.range?.let {
                            Text("${it.start.format(FULL_DATE)} – ${it.endInclusive.format(FULL_DATE)}", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "${state.includedCount} transactions · about $pages page${if (pages == 1) "" else "s"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            SectionLabel("Date range", Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            RangeChips(state.preset, state.customRange, vm::selectPreset, vm::selectCustom, PaddingValues(horizontal = 20.dp))
            Spacer(Modifier.height(20.dp))
            SectionLabel("Include", Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(6.dp))
            AppCard(Modifier.padding(horizontal = 20.dp), padding = 0.dp) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    val o = state.options
                    OptionRow("Summary & totals", o.summary) { vm.setOptions(o.copy(summary = it)) }
                    OptionRow("Category chart", o.categoryChart) { vm.setOptions(o.copy(categoryChart = it)) }
                    OptionRow("Transaction list", o.transactionList) { vm.setOptions(o.copy(transactionList = it)) }
                    OptionRow("Income", o.includeIncome, last = true) { vm.setOptions(o.copy(includeIncome = it)) }
                }
            }
        }
        Column(Modifier.padding(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "The PDF is created on this phone. Sharing it sends the file to whichever app you pick.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { saveLauncher.launch(vm.fileName()) },
                    enabled = state.range != null,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                ) { Text("Save to phone", fontWeight = FontWeight.Bold) }
                Button(
                    onClick = {
                        scope.launch {
                            val uri = vm.shareUri() ?: return@launch
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(send, "Share report"))
                        }
                    },
                    enabled = state.range != null,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                ) { Text("Share…", fontWeight = FontWeight.Bold) }
            }
        }
        SnackbarHost(snackbar)
    }
}

@Composable
private fun OptionRow(label: String, checked: Boolean, last: Boolean = false, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
    if (!last) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}
