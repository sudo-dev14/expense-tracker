package com.expensetracker.app.sms

import com.expensetracker.app.data.IngestResult
import com.expensetracker.app.data.Prefs
import com.expensetracker.app.data.TransactionEntity
import com.expensetracker.app.data.TransactionRepository
import com.expensetracker.core.model.SmsMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportState(
    val running: Boolean = false,
    val scanned: Int = 0,
    val total: Int = 0,
    val found: Int = 0,
    val oldestDate: Long? = null,
    val recent: List<TransactionEntity> = emptyList(),
    val finished: Boolean = false,
) {
    val progress: Float get() = if (total == 0) (if (finished) 1f else 0f) else scanned.toFloat() / total
}

/**
 * Scans the inbox on the device and feeds each message through the parser. Runs in the
 * application scope so it keeps going when the user leaves the import screen.
 * Only messages newer than the last scan are read, so re-running is cheap.
 */
class SmsImporter(
    private val reader: SmsReader,
    private val repository: TransactionRepository,
    private val prefs: Prefs,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = _state.asStateFlow()

    private var job: Job? = null

    fun start(fromScratch: Boolean = false) {
        if (job?.isActive == true) return
        if (!reader.hasPermission() || !prefs.autoTracking.value) return
        job = scope.launch(Dispatchers.IO) {
            val since = if (fromScratch) 0L else prefs.lastImportedSmsDate
            val total = reader.countSince(since)
            _state.value = ImportState(running = true, total = total)

            var newest = since
            var scanned = 0
            reader.readSince(since) { sms ->
                val result = repository.ingest(sms)
                scanned++
                newest = maxOf(newest, sms.timestamp)
                onResult(sms, result, scanned)
                if (scanned % 200 == 0) prefs.lastImportedSmsDate = newest
            }
            prefs.lastImportedSmsDate = newest
            _state.update { it.copy(running = false, scanned = scanned, finished = true) }
        }
    }

    private fun onResult(sms: SmsMessage, result: IngestResult, scanned: Int) {
        _state.update { s ->
            val added = (result as? IngestResult.Added)?.transaction
            s.copy(
                scanned = scanned,
                found = s.found + if (added != null) 1 else 0,
                oldestDate = s.oldestDate ?: sms.timestamp,
                recent = if (added != null) (listOf(added) + s.recent).take(4) else s.recent,
            )
        }
    }
}
