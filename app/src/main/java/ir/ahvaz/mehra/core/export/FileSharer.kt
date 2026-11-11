package ir.ahvaz.mehra.core.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Shares generated documents through the app-private FileProvider. */
object FileSharer {

    fun authority(context: Context): String = "${context.packageName}.fileprovider"

    fun shareFile(context: Context, file: File, mimeType: String, subject: String) {
        val uri = FileProvider.getUriForFile(context, authority(context), file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "ارسال فایل").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Sends a file through Android's Bluetooth share flow (com.android.bluetooth OBEX).
     * Falls back to the generic share sheet when no Bluetooth handler is present.
     */
    fun shareViaBluetooth(context: Context, file: File, mimeType: String, subject: String) {
        val uri = FileProvider.getUriForFile(context, authority(context), file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.android.bluetooth")
        }
        val started = runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        if (!started) shareFile(context, file, mimeType, subject)
    }

    fun openFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, authority(context), file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { shareFile(context, file, mimeType, file.name) }
    }
}
