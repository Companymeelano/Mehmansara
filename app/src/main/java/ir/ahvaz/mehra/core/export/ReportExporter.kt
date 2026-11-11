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

    /** Word (.docx) export of a report table. */
    fun exportDocx(table: ReportTable): File {
        val file = File(reportsDirectory(), "mehra-report-${table.type.key}-${System.currentTimeMillis()}.docx")
        DocxWriter.write(file, table, HOTEL_NAME)
        return file
    }

    /** PNG image export of a report table (for messengers / photo sharing). */
    fun exportPng(table: ReportTable): File {
        val file = File(reportsDirectory(), "mehra-report-${table.type.key}-${System.currentTimeMillis()}.png")
        PngTableRenderer.render(file, table, HOTEL_NAME)
        return file
    }

    /** Raw machine-readable export (CSV with BOM, opens in Excel too) — the "raw output". */
    fun exportRawCsv(table: ReportTable): File {
        val file = File(reportsDirectory(), "mehra-report-${table.type.key}-${System.currentTimeMillis()}.csv")
        val sb = StringBuilder()
        sb.append("\ufeff")
        sb.append(csvLine(listOf(table.title))).append("\n")
        val range = buildString {
            table.from?.let { append("from=${it}") }
            table.to?.let { append(if (isEmpty()) "to=${it}" else ";to=${it}") }
        }
        if (range.isNotBlank()) sb.append(csvLine(listOf(range))).append("\n")
        sb.append(csvLine(table.columns)).append("\n")
        table.rows.forEach { sb.append(csvLine(it)).append("\n") }
        table.totals?.let { sb.append(csvLine(it)).append("\n") }
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    /** Raw JSON export of a stay + its receipt (full record, no formatting). */
    fun exportStayRawJson(stay: Any, receipt: Receipt?): File {
        val file = File(reportsDirectory(), "mehra-stay-raw-${System.currentTimeMillis()}.json")
        val json = org.json.JSONObject()
        json.put("stay", org.json.JSONObject(stayToMap(stay)))
        if (receipt != null) json.put("receipt", org.json.JSONObject(receiptToMap(receipt)))
        file.writeText(json.toString(2), Charsets.UTF_8)
        return file
    }

    private fun stayToMap(stay: Any): Map<String, Any?> {
        val s = stay as? ir.ahvaz.mehra.domain.model.Stay ?: return mapOf("raw" to stay.toString())
        return mapOf(
            "id" to s.id, "reservationId" to s.reservationId, "reservationCode" to s.reservationCode,
            "guestId" to s.guestId, "guestName" to s.guestName, "guestPhone" to s.guestPhone,
            "roomId" to s.roomId, "roomNumber" to s.roomNumber,
            "checkInAt" to s.checkInAt?.toString(), "plannedCheckOut" to s.plannedCheckOut.toString(),
            "actualCheckOutAt" to s.actualCheckOutAt?.toString(), "status" to s.status.name,
            "guestsCount" to s.guestsCount, "totalRial" to s.totalRial, "paidRial" to s.paidRial,
            "balanceRial" to s.balanceRial,
            "companions" to s.companions.map { mapOf("fullName" to it.fullName, "nationalId" to it.nationalId) },
        )
    }

    private fun receiptToMap(r: Receipt): Map<String, Any?> = mapOf(
        "receiptNumber" to r.receiptNumber, "issuedAt" to r.issuedAt.toString(),
        "guestName" to r.guestName, "roomNumber" to r.roomNumber,
        "checkInAt" to r.checkInAt?.toString(), "checkOutAt" to r.checkOutAt?.toString(),
        "plannedCheckOut" to r.plannedCheckOut?.toString(), "nights" to r.nights,
        "guestsCount" to r.guestsCount, "totalRial" to r.totalRial, "paidRial" to r.paidRial,
        "balanceRial" to r.balanceRial, "paymentMethod" to r.paymentMethod?.name,
        "operatorName" to r.operatorName, "hotelName" to r.hotelName,
        "lines" to r.lines.map { mapOf("title" to it.title, "amountRial" to it.amountRial, "isCredit" to it.isCredit) },
    )

    private fun csvLine(cells: List<String>): String = cells.joinToString(",") { cell ->
        "\"" + cell.replace("\"", "\"\"") + "\""
    }

    fun share(file: File, mimeType: String, subject: String) {
        FileSharer.shareFile(context, file, mimeType, subject)
    }

    fun shareViaBluetooth(file: File, mimeType: String, subject: String) {
        FileSharer.shareViaBluetooth(context, file, mimeType, subject)
    }

    companion object {
        const val MIME_PDF = "application/pdf"
        const val MIME_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        const val MIME_DOCX =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        const val MIME_PNG = "image/png"
        const val MIME_CSV = "text/csv"
        const val MIME_JSON = "application/json"
        const val HOTEL_NAME = "مهمان‌پذیر فرهنگیان اهواز — مهرآ"
    }
}
