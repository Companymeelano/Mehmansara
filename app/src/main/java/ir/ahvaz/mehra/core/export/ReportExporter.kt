package ir.ahvaz.mehra.core.export

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.ahvaz.mehra.core.date.toJalali
import ir.ahvaz.mehra.domain.model.Receipt
import ir.ahvaz.mehra.domain.model.ReportTable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates report/receipt files and hands them to the share sheet.
 * Every generated artefact lives in the app cache (and is deleteable) — never in public storage.
 */
@Singleton
class ReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val pdfWriter = ReportPdfWriter(context)

    fun reportsDirectory(): File = File(context.cacheDir, "reports").apply { mkdirs() }

    fun receiptsDirectory(): File = File(context.cacheDir, "receipts").apply { mkdirs() }

    fun exportPdf(table: ReportTable, generatedBy: String?): File =
        pdfWriter.writeReport(table, HOTEL_NAME, generatedBy, reportsDirectory())

    fun exportXlsx(table: ReportTable): File {
        val file = File(reportsDirectory(), "mehra-report-${table.type.key}-${System.currentTimeMillis()}.xlsx")
        val header = table.columns
        val rows = buildList {
            add(listOf(table.title))
            val range = buildString {
                table.from?.let { append("از ${it.toJalali().formatShortLatin()} ") }
                table.to?.let { append("تا ${it.toJalali().formatShortLatin()}") }
            }
            if (range.isNotBlank()) add(listOf(range))
            add(emptyList())
            add(header)
            addAll(table.rows)
            if (!table.totals.isNullOrEmpty()) add(table.totals)
        }
        XlsxWriter.write(file, table.type.titleFa, rows)
        return file
    }

    fun receiptPdf(receipt: Receipt): File = pdfWriter.writeReceipt(receipt, receiptsDirectory())

    fun share(file: File, mimeType: String, subject: String) {
        FileSharer.shareFile(context, file, mimeType, subject)
    }

    companion object {
        const val MIME_PDF = "application/pdf"
        const val MIME_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        const val HOTEL_NAME = "مهمان‌پذیر فرهنگیان اهواز — مهرآ"
    }
}
