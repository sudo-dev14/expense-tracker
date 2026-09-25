package com.expensetracker.app

import android.app.Application
import android.content.Context
import com.expensetracker.app.data.AppDatabase
import com.expensetracker.app.data.Prefs
import com.expensetracker.app.data.TransactionRepository
import com.expensetracker.app.export.PdfExporter
import com.expensetracker.app.sms.SmsImporter
import com.expensetracker.app.sms.SmsReader
import com.expensetracker.core.analytics.DateRange
import com.expensetracker.core.analytics.RangePreset
import com.expensetracker.core.model.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

class ExpenseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Manual dependency wiring; small enough that a DI framework would add more than it saves. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db: AppDatabase = AppDatabase.create(context)
    val prefs = Prefs(context)
    val repository = TransactionRepository(db)
    val smsReader = SmsReader(context)
    val importer = SmsImporter(smsReader, repository, prefs, appScope)
    val pdfExporter = PdfExporter(context)
    val filters = FilterState()
}

/** Date range and drill-down filters shared by Home and Transactions, so they stay in sync. */
class FilterState {
    val preset = MutableStateFlow(RangePreset.THIS_MONTH)
    val customRange = MutableStateFlow<DateRange?>(null)
    val category = MutableStateFlow<Category?>(null)
}
