package ir.ahvaz.mehra.core.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import ir.ahvaz.mehra.domain.model.ReportTable
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a [ReportTable] to a PNG image (gold-on-navy luxury report card) so reports can be
 * sent as a picture to Telegram/WhatsApp/e-mail or Bluetooth. Text is drawn right-to-left
 * from the right edge; columns share the width evenly.
 */
object PngTableRenderer {

    private const val WIDTH = 1600
    private const val MARGIN = 48
    private const val HEADER_H = 190
    private const val COLHEADER_H = 64
    private const val ROW_H = 58

    private val navy = Color.parseColor("#16233D")
    private val navyLight = Color.parseColor("#24344F")
    private val gold = Color.parseColor("#B08D3E")
    private val cream = Color.parseColor("#F6EFE0")
    private val goldSoft = Color.parseColor("#E8D6AE")
    private val ink = Color.parseColor("#1B2436")

    fun render(file: File, table: ReportTable, hotelName: String): File {
        file.parentFile?.mkdirs()
        val rowCount = table.rows.size.coerceAtMost(400)
        val height = HEADER_H + COLHEADER_H + rowCount * ROW_H + (if (table.totals != null) ROW_H else 0) + MARGIN
        val bmp = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // background
        c.drawColor(navy)
        val band = Paint().apply { color = navyLight }
        c.drawRect(0f, 0f, WIDTH.toFloat(), (HEADER_H + COLHEADER_H).toFloat(), band)
        val goldLine = Paint().apply { color = gold; strokeWidth = 6f }
        c.drawLine(0f, (HEADER_H + COLHEADER_H).toFloat(), WIDTH.toFloat(), (HEADER_H + COLHEADER_H).toFloat(), goldLine)

        // header text
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldSoft; textSize = 52f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cream; textSize = 30f; textAlign = Paint.Align.RIGHT
        }
        val hotelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold; textSize = 30f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT
        }
        c.drawText(hotelName, (WIDTH - MARGIN).toFloat(), 70f, hotelPaint)
        c.drawText(table.title.ifBlank { "گزارش" }, (WIDTH - MARGIN).toFloat(), 128f, titlePaint)
        val range = listOfNotNull(table.from?.toString(), table.to?.toString()).joinToString(" تا ")
        if (range.isNotBlank()) c.drawText(range, (WIDTH - MARGIN).toFloat(), 168f, subPaint)

        // body card
        val bodyTop = HEADER_H + COLHEADER_H + 24f
        val card = Paint().apply { color = cream }
        c.drawRoundRect(RectF(MARGIN.toFloat(), bodyTop - 16f, (WIDTH - MARGIN).toFloat(), height - MARGIN.toFloat() + 8f), 28f, 28f, card)

        val colW = (WIDTH - 2 * MARGIN) / table.columns.size.coerceAtLeast(1).toFloat()
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textSize = 27f; textAlign = Paint.Align.RIGHT }
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gold; textSize = 29f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT }

        // column headers (drawn on the navy band, below the title area)
        val colTop = HEADER_H.toFloat()
        table.columns.forEachIndexed { i, col ->
            val right = WIDTH - MARGIN - i * colW
            c.drawText(col, right - 14f, colTop + 44f, headPaint)
        }

        // rows
        var y = bodyTop
        table.rows.take(400).forEachIndexed { index, row ->
            if (index % 2 == 1) {
                val alt = Paint().apply { color = Color.parseColor("#EFE6D2") }
                c.drawRect(MARGIN.toFloat(), y - 6f, (WIDTH - MARGIN).toFloat(), y + ROW_H - 6f, alt)
            }
            row.forEachIndexed { i, cell ->
                val right = WIDTH - MARGIN - i * colW
                c.drawText(cell, right - 14f, y + 34f, cellPaint)
            }
            y += ROW_H
        }
        table.totals?.let { totals ->
            val totalPaint = Paint().apply { color = gold; textSize = 29f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT }
            totals.forEachIndexed { i, cell ->
                val right = WIDTH - MARGIN - i * colW
                c.drawText(cell, right - 14f, y + 34f, totalPaint)
            }
        }

        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return file
    }
}
