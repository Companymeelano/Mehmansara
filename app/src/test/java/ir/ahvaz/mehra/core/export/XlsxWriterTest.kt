package ir.ahvaz.mehra.core.export

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.zip.ZipFile
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class XlsxWriterTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `writes a readable workbook with persian cells`() {
        val file = File(temporaryFolder.root, "report.xlsx")
        XlsxWriter.write(
            file = file,
            sheetName = "گزارش درآمد",
            rows = listOf(
                listOf("تاریخ", "مبلغ"),
                listOf("۱۴۰۳/۰۱/۰۱", "۱٬۲۵۰٬۰۰۰"),
            ),
        )

        assertThat(file.exists()).isTrue()
        assertThat(file.length()).isGreaterThan(0L)

        ZipFile(file).use { zip ->
            val entries = zip.entries().toList().map { it.name }
            assertThat(entries).contains("[Content_Types].xml")
            assertThat(entries).contains("xl/workbook.xml")
            assertThat(entries).contains("xl/worksheets/sheet1.xml")
            val sheet = zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml"))
                .bufferedReader().readText()
            assertThat(sheet).contains("تاریخ")
            assertThat(sheet).contains("rightToLeft=\"1\"")
        }
    }

    @Test
    fun `escapes xml special characters`() {
        val file = File(temporaryFolder.root, "escaped.xlsx")
        XlsxWriter.write(file, "sheet", listOf(listOf("A&B <C>")))
        ZipFile(file).use { zip ->
            val sheet = zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml"))
                .bufferedReader().readText()
            assertThat(sheet).contains("A&amp;B &lt;C&gt;")
        }
    }
}
