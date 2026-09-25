package com.expensetracker.app.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextUtils
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.expensetracker.app.data.TransactionEntity
import com.expensetracker.app.data.categoryEnum
import com.expensetracker.app.data.displayMerchant
import com.expensetracker.app.data.toPoint
import com.expensetracker.core.analytics.Analytics
import com.expensetracker.core.analytics.DateRange
import com.expensetracker.core.format.Money
import com.expensetracker.core.model.TransactionType
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ExportOptions(
    val summary: Boolean = true,
    val categoryChart: Boolean = true,
    val transactionList: Boolean = true,
    val includeIncome: Boolean = false,
)

/**
 * Builds the report with Android's built-in [PdfDocument]: no library, no network.
 * The file only leaves the phone if the user explicitly shares it.
 */
class PdfExporter(private val context: Context) {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun write(out: OutputStream, range: DateRange, all: List<TransactionEntity>, options: ExportOptions) {
        val txns = if (options.includeIncome) all else all.filter { it.type == TransactionType.DEBIT }
        val doc = PdfDocument()
        val writer = PageWriter(doc)
        writer.header(range)
        if (options.summary) writer.summary(all, options.includeIncome)
        if (options.categoryChart) writer.categories(all)
        if (options.transactionList) writer.table(txns)
        writer.finish()
        doc.writeTo(out)
        doc.close()
    }

    /** Writes the report into the app cache and returns a shareable content:// URI. */
    fun writeForSharing(range: DateRange, txns: List<TransactionEntity>, options: ExportOptions): android.net.Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // keep only the latest export
        val file = File(dir, fileName(range))
        file.outputStream().use { write(it, range, txns, options) }
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    fun fileName(range: DateRange): String =
        "expenses_${range.start.format(FILE_DATE)}_${range.endInclusive.format(FILE_DATE)}.pdf"

    /** Rough page count for the preview line. */
    fun estimatePages(txnCount: Int, options: ExportOptions): Int {
        var firstPageRows = ROWS_PER_PAGE - 6
        if (options.summary) firstPageRows -= 5
        if (options.categoryChart) firstPageRows -= 12
        val onFirst = firstPageRows.coerceAtLeast(0)
        if (!options.transactionList || txnCount <= onFirst) return 1
        return 1 + (txnCount - onFirst + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE
    }

    private inner class PageWriter(private val doc: PdfDocument) {
        private var pageNumber = 0
        private lateinit var page: PdfDocument.Page
        private lateinit var canvas: Canvas
        private var y = 0f

        private val ink = Color.rgb(0x17, 0x19, 0x1A)
        private val muted = Color.rgb(0x5B, 0x60, 0x60)
        private val line = Color.rgb(0xE3, 0xE1, 0xD9)
        private val accent = Color.rgb(0x0E, 0x5A, 0x52)

        private val title = text(22f, ink, Typeface.create(Typeface.SERIF, Typeface.BOLD))
        private val h2 = text(13f, ink, Typeface.DEFAULT_BOLD)
        private val body = text(10f, ink, Typeface.DEFAULT)
        private val bodyBold = text(10f, ink, Typeface.DEFAULT_BOLD)
        private val small = text(8.5f, muted, Typeface.DEFAULT)
        private val big = text(18f, ink, Typeface.create(Typeface.SERIF, Typeface.BOLD))
        private val rule = Paint().apply { color = line; strokeWidth = 0.8f }

        private fun text(size: Float, color: Int, face: Typeface) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; this.color = color; typeface = face
        }

        init { newPage() }

        private fun newPage() {
            if (pageNumber > 0) finishPage()
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
        }

        private fun finishPage() {
            canvas.drawText("Created on this phone · Page $pageNumber", MARGIN, PAGE_H - 24f, small)
            doc.finishPage(page)
        }

        fun finish() = finishPage()

        private fun ensureSpace(h: Float) {
            if (y + h > PAGE_H - MARGIN - 10) newPage()
        }

        fun header(range: DateRange) {
            y += 22f
            canvas.drawText("Expense report", MARGIN, y, title)
            y += 18f
            canvas.drawText(
                "${range.start.format(LONG_DATE)} – ${range.endInclusive.format(LONG_DATE)}",
                MARGIN, y, body.apply { color = muted },
            )
            body.color = ink
            val generated = "Generated ${LocalDate.now(zone).format(LONG_DATE)}"
            canvas.drawText(generated, PAGE_W - MARGIN - small.measureText(generated), y, small)
            y += 14f
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, rule)
            y += 22f
        }

        fun summary(txns: List<TransactionEntity>, includeIncome: Boolean) {
            val s = Analytics.summary(txns.map { it.toPoint() })
            val cells = buildList {
                add("Spent" to Money.format(s.spentMinor))
                if (includeIncome) {
                    add("Income" to Money.format(s.incomeMinor))
                    add("Net" to Money.format(s.netMinor, signed = true))
                }
                add("Transactions" to s.count.toString())
            }
            ensureSpace(60f)
            val w = (PAGE_W - 2 * MARGIN) / cells.size
            cells.forEachIndexed { i, (label, value) ->
                val x = MARGIN + i * w
                canvas.drawText(label, x, y, small)
                canvas.drawText(value, x, y + 22f, big)
            }
            y += 48f
        }

        fun categories(txns: List<TransactionEntity>) {
            val cats = Analytics.byCategory(txns.map { it.toPoint() })
            if (cats.isEmpty()) return
            ensureSpace(170f)
            canvas.drawText("Spending by category", MARGIN, y, h2)
            y += 14f
            val size = 120f
            val oval = RectF(MARGIN + 8, y + 6, MARGIN + 8 + size, y + 6 + size)
            val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 18f }
            var start = -90f
            cats.forEach { c ->
                arc.color = c.category.colorArgb.toInt()
                val sweep = 360f * c.share
                canvas.drawArc(oval, start, sweep, false, arc)
                start += sweep
            }
            var ly = y + 16f
            val lx = MARGIN + size + 48f
            val swatch = Paint(Paint.ANTI_ALIAS_FLAG)
            cats.take(10).forEach { c ->
                swatch.color = c.category.colorArgb.toInt()
                canvas.drawRoundRect(RectF(lx, ly - 8, lx + 9, ly + 1), 2f, 2f, swatch)
                canvas.drawText(c.category.label, lx + 16, ly, body)
                val pct = "${(c.share * 100).toInt()}%"
                val amt = Money.format(c.amountMinor)
                canvas.drawText(pct, PAGE_W - MARGIN - 110f, ly, small)
                canvas.drawText(amt, PAGE_W - MARGIN - bodyBold.measureText(amt), ly, bodyBold)
                ly += 15f
            }
            y += maxOf(size + 22f, ly - y) + 8f
        }

        fun table(txns: List<TransactionEntity>) {
            if (txns.isEmpty()) return
            ensureSpace(60f)
            canvas.drawText("Transactions", MARGIN, y, h2)
            y += 18f
            columnHeaders()
            txns.forEach { tx ->
                if (y + ROW_H > PAGE_H - MARGIN - 10) {
                    newPage()
                    columnHeaders()
                }
                val date = Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate().format(ROW_DATE)
                val credit = tx.type == TransactionType.CREDIT
                val amount = Money.format(if (credit) tx.amountMinor else -tx.amountMinor, showPaise = true, signed = true)
                canvas.drawText(date, COL_DATE, y, body)
                canvas.drawText(ellipsize(tx.displayMerchant, 190f), COL_PAYEE, y, body)
                canvas.drawText(ellipsize(tx.categoryEnum.label, 110f), COL_CATEGORY, y, small)
                bodyBold.color = if (credit) accent else ink
                canvas.drawText(amount, PAGE_W - MARGIN - bodyBold.measureText(amount), y, bodyBold)
                bodyBold.color = ink
                y += 5f
                canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, rule)
                y += ROW_H - 5f
            }
        }

        private fun columnHeaders() {
            canvas.drawText("DATE", COL_DATE, y, small)
            canvas.drawText("PAID TO / FROM", COL_PAYEE, y, small)
            canvas.drawText("CATEGORY", COL_CATEGORY, y, small)
            canvas.drawText("AMOUNT", PAGE_W - MARGIN - small.measureText("AMOUNT"), y, small)
            y += 6f
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, rule)
            y += ROW_H - 4f
        }

        private fun ellipsize(s: String, width: Float): String =
            TextUtils.ellipsize(s, body, width, TextUtils.TruncateAt.END).toString()
    }

    private companion object {
        const val PAGE_W = 595 // A4 in points
        const val PAGE_H = 842
        const val MARGIN = 40f
        const val ROW_H = 20f
        const val ROWS_PER_PAGE = 36
        const val COL_DATE = MARGIN
        const val COL_PAYEE = MARGIN + 70f
        const val COL_CATEGORY = MARGIN + 280f
        val LONG_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
        val ROW_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yy", Locale.ENGLISH)
        val FILE_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)
    }
}
