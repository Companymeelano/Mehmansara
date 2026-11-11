package ir.ahvaz.mehra.core.export

import ir.ahvaz.mehra.domain.model.ReportTable
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Minimal but valid Word (.docx) writer: a single OOXML package
 * ([Content_Types].xml + _rels + word/document.xml) containing the report as a
 * bordered table with a gold-shaded header row. Opens in Word / WPS / LibreOffice.
 */
object DocxWriter {

    fun write(file: File, table: ReportTable, hotelName: String) {
        file.parentFile?.mkdirs()
        val title = table.title.ifBlank { hotelName }
        val range = listOfNotNull(table.from?.toString(), table.to?.toString()).joinToString(" تا ")

        val rowsXml = buildString {
            // header row
            append(tr(shade = "B08D3E", bold = true, cells = table.columns))
            table.rows.forEachIndexed { i, row ->
                append(tr(shade = if (i % 2 == 0) "F6EFE0" else "FFFFFF", bold = false, cells = row))
            }
            table.totals?.let { append(tr(shade = "E8D6AE", bold = true, cells = it)) }
        }

        val document = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p><w:r><w:t xml:space="preserve">$hotelName</w:t></w:r></w:p>
                <w:p><w:r><w:rPr><w:b/><w:color w:val="B08D3E"/></w:rPr><w:t xml:space="preserve">$title</w:t></w:r></w:p>
                ${if (range.isNotBlank()) """<w:p><w:r><w:t xml:space="preserve">$range</w:t></w:r></w:p>""" else ""}
                <w:tbl>
                  <w:tblPr><w:tblBorders>
                    <w:top w:val="single" w:sz="4" w:color="B08D3E"/>
                    <w:left w:val="single" w:sz="4" w:color="B08D3E"/>
                    <w:bottom w:val="single" w:sz="4" w:color="B08D3E"/>
                    <w:right w:val="single" w:sz="4" w:color="B08D3E"/>
                    <w:insideH w:val="single" w:sz="4" w:color="D9C9A0"/>
                    <w:insideV w:val="single" w:sz="4" w:color="D9C9A0"/>
                  </w:tblBorders></w:tblPr>
                  $rowsXml
                </w:tbl>
                ${table.notes.joinToString("") { "<w:p><w:r><w:t xml:space=\"preserve\">• ${esc(it)}</w:t></w:r></w:p>" }}
              </w:body>
            </w:document>
        """.trimIndent()

        ZipOutputStream(FileOutputStream(file)).use { zip ->
            put(zip, "[Content_Types].xml", CONTENT_TYPES)
            put(zip, "_rels/.rels", RELS)
            put(zip, "word/document.xml", document)
        }
    }

    private fun tr(shade: String, bold: Boolean, cells: List<String>): String = buildString {
        append("<w:tr>")
        cells.forEach { cell ->
            append("<w:tc><w:tcPr><w:shd w:val=\"clear\" w:fill=\"$shade\"/></w:tcPr>")
            append("<w:p><w:r>")
            if (bold) append("<w:rPr><w:b/></w:rPr>")
            append("<w:t xml:space=\"preserve\">${esc(cell)}</w:t></w:r></w:p></w:tc>")
        }
        append("</w:tr>")
    }

    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun put(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private const val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

    private const val RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
}
