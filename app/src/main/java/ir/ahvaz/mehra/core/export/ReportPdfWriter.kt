package ir.ahvaz.mehra.core.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import ir.ahvaz.mehra.R
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.date.formatJalaliLongFa
import ir.ahvaz.mehra.core.date.toJalali
import ir.ahvaz.mehra.domain.model.ReportTable
import ir.ahvaz.mehra.domain.model.Receipt
import java.io.File
import java.io.FileOutputStream
import java.time.Instant

/**
 * Renders Persian reports to A4 PDFs.
 *
 * Text is laid out with [StaticLayout] and [TextDirectionHeuristics.RTL] so Arabic shaping
 * and bidirectional ordering are handled by the platform text engine — drawing raw strings
 * on a canvas would break joins and bidi.
 */
class ReportPdfWriter(private val context: Context) {

    private val regular: Typeface = ResourcesCompat.getFont(context, R.font.vazirmatn_regular) ?: Typeface.SANS_SERIF
    private val bold: Typeface = ResourcesCompat.getFont(context, R.font.vazirmatn_semibold) ?: Typeface.SANS_SERIF

    fun writeReport(
        table: ReportTable,
        hotelName: String,
        generatedBy: String?,
        targetDirectory: File,
    ): File {
        val file = File(targetDirectory, "mehra-report-${table.type.key}-${System.currentTimeMillis()}.pdf")
        targetDirectory.mkdirs()
        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(pageInfo(pageNumber))
        var canvas = page.canvas
        var y = drawHeader(canvas, table, hotelName, generatedBy, pageNumber)

        val columnCount = table.columns.size.coerceAtLeast(1)
        val available = PAGE_WIDTH - 2 * MARGIN
        val columnWidth = available / columnCount
        val bodySize = when {
            columnCount <= 5 -> 9f
            columnCount <= 8 -> 8f
            else -> 7f
        }

        // Table header
        y = drawTableHeader(canvas, table.columns, y, columnWidth, bodySize)
        var rowIndex = 0
        for (row in table.rows) {
            val rowHeight = rowHeight(row, columnWidth, bodySize)
            if (y + rowHeight > PAGE_HEIGHT - MARGIN - 28) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(pageInfo(pageNumber))
                canvas = page.canvas
                y = drawHeader(canvas, table, hotelName, generatedBy, pageNumber)
                y = drawTableHeader(canvas, table.columns, y, columnWidth, bodySize)
            }
            y = drawRow(canvas, row, y, columnWidth, bodySize, alternate = rowIndex % 2 == 1)
            rowIndex += 1
        }

        if (!table.totals.isNullOrEmpty()) {
            val height = rowHeight(table.totals, columnWidth, bodySize)
            if (y + height > PAGE_HEIGHT - MARGIN - 28) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(pageInfo(pageNumber))
                canvas = page.canvas
                y = drawHeader(canvas, table, hotelName, generatedBy, pageNumber)
            }
            y = drawRow(canvas, table.totals, y, columnWidth, bodySize, alternate = false, emphasized = true)
        }

        drawFooter(canvas, pageNumber, table, generatedBy)
        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    /** Simple A5-style settlement receipt used at check-out. */
    fun writeReceipt(receipt: Receipt, targetDirectory: File): File {
        val file = File(targetDirectory, "mehra-receipt-${receipt.receiptNumber}-${System.currentTimeMillis()}.pdf")
        targetDirectory.mkdirs()
        val document = PdfDocument()
        val page = document.startPage(
            PdfDocument.PageInfo.Builder(RECEIPT_WIDTH, RECEIPT_HEIGHT, 1).create()
        )
        val canvas = page.canvas
        var y = 40f
        y = drawText(canvas, receipt.hotelName, y, RECEIPT_WIDTH - 2 * 24f, 24f, alignCenter = true, bold = true, offsetX = 24f)
        y = drawText(canvas, "رسید تسویه حساب", y + 4, RECEIPT_WIDTH - 2 * 24f, 18f, alignCenter = true, bold = true, offsetX = 24f)
        y = drawText(canvas, "شماره رسید: ${receipt.receiptNumber.toPersianDigits()}", y + 8, RECEIPT_WIDTH - 2 * 24f, 13f, offsetX = 24f)
        y = drawText(canvas, "تاریخ صدور: ${receipt.issuedAt.formatJalaliDateTime()}", y + 4, RECEIPT_WIDTH - 2 * 24f, 13f, offsetX = 24f)
        y = drawText(canvas, "مهمان: ${receipt.guestName}", y + 4, RECEIPT_WIDTH - 2 * 24f, 13f, offsetX = 24f)
        y = drawText(canvas, "اتاق: ${receipt.roomNumber.toPersianDigits()} • نفرات: ${receipt.guestsCount.toString().toPersianDigits()} • شب‌ها: ${receipt.nights.toString().toPersianDigits()}", y + 4, RECEIPT_WIDTH - 2 * 24f, 13f, offsetX = 24f)
        y += 12
        y = drawRule(canvas, y, RECEIPT_WIDTH - 2 * 24f, 24f)
        receipt.lines.forEach { line ->
            val text = "${line.title} — ${ir.ahvaz.mehra.core.common.formatMoneyFa(line.amountRial)}"
            y = drawText(canvas, text, y + 4, RECEIPT_WIDTH - 2 * 24f, 13f, offsetX = 24f)
        }
        y = drawRule(canvas, y + 6, RECEIPT_WIDTH - 2 * 24f, 24f)
        y = drawText(canvas, "مبلغ کل: ${ir.ahvaz.mehra.core.common.formatMoneyFa(receipt.totalRial)}", y + 6, RECEIPT_WIDTH - 2 * 24f, 13f, offsetX = 24f)
        y = drawText(canvas, "پرداخت‌شده: ${ir.ahvaz.mehra.core.common.formatMoneyFa(receipt.paidRial)}", y + 4, RECEIPT_WIDTH - 2 * 24f, 13f, offsetX = 24f)
        y = drawText(
            canvas,
            "مانده: ${ir.ahvaz.mehra.core.common.formatMoneyFa(receipt.balanceRial)}",
            y + 4,
            RECEIPT_WIDTH - 2 * 24f,
            14f,
            bold = true,
            offsetX = 24f,
        )
        if (receipt.paymentMethod != null) {
            y = drawText(canvas, "روش پرداخت: ${receipt.paymentMethod.labelFa}", y + 8, RECEIPT_WIDTH - 2 * 24f, 13f, offsetX = 24f)
        }
        receipt.operatorName?.let {
            y = drawText(canvas, "صادرکننده: $it", y + 4, RECEIPT_WIDTH - 2 * 24f, 12f, offsetX = 24f)
        }
        drawText(
            canvas,
            "این رسید به‌صورت خودکار توسط سامانه مهرآ صادر شده است.",
            RECEIPT_HEIGHT - 52f,
            RECEIPT_WIDTH - 2 * 24f,
            11f,
            alignCenter = true,
            offsetX = 24f,
        )
        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun pageInfo(pageNumber: Int) =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()

    private fun drawHeader(
        canvas: Canvas,
        table: ReportTable,
        hotelName: String,
        generatedBy: String?,
        pageNumber: Int,
    ): Float {
        var y = MARGIN
        y = drawText(canvas, hotelName, y, PAGE_WIDTH - 2 * MARGIN, 18f, alignCenter = true, bold = true, offsetX = MARGIN)
        y = drawText(canvas, table.title, y + 2, PAGE_WIDTH - 2 * MARGIN, 15f, alignCenter = true, bold = true, offsetX = MARGIN)
        val range = buildString {
            table.from?.let { append("از ${it.toJalali().formatShortFa()} ") }
            table.to?.let { append("تا ${it.toJalali().formatShortFa()}") }
        }
        if (range.isNotBlank()) {
            y = drawText(canvas, range, y + 2, PAGE_WIDTH - 2 * MARGIN, 11f, alignCenter = true, offsetX = MARGIN)
        }
        val meta = buildString {
            append("تاریخ تولید: ${table.generatedAt.formatJalaliDateTime()}")
            if (!generatedBy.isNullOrBlank()) append(" • تولیدکننده: $generatedBy")
            append(" • صفحه ${pageNumber.toString().toPersianDigits()}")
        }
        y = drawText(canvas, meta, y + 2, PAGE_WIDTH - 2 * MARGIN, 9f, alignCenter = true, offsetX = MARGIN)
        return y + 10
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, table: ReportTable, generatedBy: String?) {
        val note = table.notes.firstOrNull()
        val footer = buildString {
            append("مهرآ — مهمان‌پذیر فرهنگیان اهواز")
            if (!note.isNullOrBlank()) append(" • $note")
            append(" • صفحه ${pageNumber.toString().toPersianDigits()}")
        }
        drawText(
            canvas,
            footer,
            PAGE_HEIGHT - MARGIN + 4,
            PAGE_WIDTH - 2 * MARGIN,
            8f,
            alignCenter = true,
            offsetX = MARGIN,
        )
    }

    private fun drawTableHeader(
        canvas: Canvas,
        columns: List<String>,
        startY: Float,
        columnWidth: Float,
        fontSize: Float,
    ): Float {
        val height = 24f
        val paint = Paint().apply { color = Color.parseColor("#F0EBE0") }
        canvas.drawRect(MARGIN, startY, MARGIN + columnWidth * columns.size, startY + height, paint)
        val stroke = Paint().apply {
            color = Color.parseColor("#CFC3AC")
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }
        canvas.drawRect(MARGIN, startY, MARGIN + columnWidth * columns.size, startY + height, stroke)
        var x = MARGIN + columnWidth * columns.size
        columns.forEach { column ->
            x -= columnWidth
            drawTextInBox(canvas, column, x, startY + 4, columnWidth, fontSize, bold = true)
        }
        return startY + height
    }

    private fun rowHeight(row: List<String>, columnWidth: Float, fontSize: Float): Float {
        var maxHeight = 18f
        row.forEach { cell ->
            val layout = buildLayout(cell, columnWidth - 6f, fontSize, false)
            maxHeight = maxOf(maxHeight, layout.height + 8f)
        }
        return maxHeight
    }

    private fun drawRow(
        canvas: Canvas,
        row: List<String>,
        startY: Float,
        columnWidth: Float,
        fontSize: Float,
        alternate: Boolean,
        emphasized: Boolean = false,
    ): Float {
        val height = rowHeight(row, columnWidth, fontSize)
        if (alternate) {
            val paint = Paint().apply { color = Color.parseColor("#FAF7F1") }
            canvas.drawRect(MARGIN, startY, MARGIN + columnWidth * row.size, startY + height, paint)
        }
        val stroke = Paint().apply {
            color = Color.parseColor("#E2DACB")
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        canvas.drawRect(MARGIN, startY, MARGIN + columnWidth * row.size, startY + height, stroke)
        var x = MARGIN + columnWidth * row.size
        row.forEach { cell ->
            x -= columnWidth
            drawTextInBox(canvas, cell, x, startY + 4, columnWidth, fontSize, bold = emphasized)
        }
        return startY + height
    }

    private fun drawTextInBox(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        fontSize: Float,
        bold: Boolean = false,
    ) {
        val layout = buildLayout(text, width - 6f, fontSize, bold)
        canvas.save()
        canvas.translate(x + 3f, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun buildLayout(text: String, width: Float, fontSize: Float, bold: Boolean): StaticLayout {
        val paint = TextPaint().apply {
            this.textSize = fontSize
            typeface = if (bold) this@ReportPdfWriter.bold else regular
            color = Color.parseColor("#1F2126")
            isAntiAlias = true
        }
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(24))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(false)
            .build()
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        y: Float,
        width: Float,
        fontSize: Float,
        alignCenter: Boolean = false,
        bold: Boolean = false,
        offsetX: Float = MARGIN,
    ): Float {
        val layout = buildLayout(text, width, fontSize, bold)
        canvas.save()
        val totalWidth = RectF(offsetX, y, offsetX + width, y + layout.height)
        if (alignCenter) {
            val dx = totalWidth.centerX() - layout.width / 2f
            canvas.translate(dx, y)
        } else {
            canvas.translate(offsetX, y)
        }
        layout.draw(canvas)
        canvas.restore()
        return y + layout.height
    }

    private fun drawRule(canvas: Canvas, y: Float, width: Float, offsetX: Float): Float {
        val paint = Paint().apply {
            color = Color.parseColor("#D9D0BF")
            strokeWidth = 0.8f
        }
        canvas.drawLine(offsetX, y, offsetX + width, y, paint)
        return y
    }

    private fun Instant.formatJalaliDateTime(): String {
        val zoned = atZone(ir.ahvaz.mehra.core.date.TehranTime.zone)
        val date = zoned.toLocalDate().formatJalaliLongFa()
        val time = zoned.toLocalTime().toString().take(5).toPersianDigits()
        return "$date — $time"
    }

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 28f
        private const val RECEIPT_WIDTH = 420
        private const val RECEIPT_HEIGHT = 640
    }
}
