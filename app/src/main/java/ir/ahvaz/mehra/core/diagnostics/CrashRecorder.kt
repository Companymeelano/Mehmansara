package ir.ahvaz.mehra.core.diagnostics

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records any uncaught exception to a text file in the public Downloads folder
 * (e.g. `Download/mehra-crash-20260201-143000.txt`) before the process dies.
 *
 * Purpose: field diagnostics. On a device where the app closes immediately on
 * launch there is no logcat handy, so the app captures its own crash report;
 * the operator can copy the file over USB / MTP and attach it to a bug report.
 *
 * On Android 10+ (API 29+) the MediaStore insert needs no storage permission.
 * On older devices the legacy path needs WRITE_EXTERNAL_STORAGE (declared in the
 * manifest with maxSdkVersion=28).
 *
 * Installation is idempotent; the previous default handler is always invoked so
 * behaviour (process death, system crash dialog) is unchanged.
 */
object CrashRecorder {

    private const val TAG = "MehraCrash"

    @Volatile
    private var installed = false

    @Synchronized
    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrashFile(app, thread, throwable)
            } catch (t: Throwable) {
                Log.e(TAG, "failed to write crash file", t)
            }
            previous?.uncaughtException(thread, throwable)
        }
        Log.i(TAG, "crash recorder installed")
    }

    private fun writeCrashFile(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val name = "mehra-crash-$ts.txt"
        val text = buildString {
            append("MEHRA crash report\n")
            append("time: $ts\n")
            append("thread: ${thread.name}\n")
            append("android: ${Build.VERSION.RELEASE} (SDK ${Build.SDK_INT})\n")
            append("device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})\n")
            append("abi: ${Build.SUPPORTED_ABIS.joinToString()}\n\n")
            append(sw.toString())
        }
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out -> out.write(text.toByteArray()) }
                val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                context.contentResolver.update(uri, done, null, null)
            } else {
                Log.e(TAG, "MediaStore insert returned null")
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dir?.mkdirs() == true || dir?.isDirectory == true) {
                File(dir, name).writeText(text)
            }
        }
        Log.e(TAG, "crash recorded to Downloads/$name", throwable)
    }
}
