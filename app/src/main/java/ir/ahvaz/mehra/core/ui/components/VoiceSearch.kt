package ir.ahvaz.mehra.core.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Speech-recognizer intent contract as literal strings (the public constant values are
 * stable; using literals keeps this file independent of the android.speech stub on some
 * compile classpaths).
 */
private const val ACTION_RECOGNIZE_SPEECH = "android.speech.action.RECOGNIZE_SPEECH"
private const val EXTRA_RESULTS = "android.speech.extra.RESULTS"
private const val EXTRA_LANGUAGE_MODEL = "android.speech.extra.LANGUAGE_MODEL"
private const val EXTRA_LANGUAGE = "android.speech.extra.LANGUAGE"
private const val EXTRA_PROMPT = "android.speech.extra.PROMPT"
private const val LANGUAGE_MODEL_FREE_FORM = "free_form"

/**
 * Reusable voice search (v1.6.0): returns a `launch()` function that opens the system
 * speech recognizer (fa-IR, free-form) and reports the recognized phrase through
 * [onResult]. No app permission is needed — the recognizer activity runs in the
 * Google/system app. [onUnavailable] fires when no recognizer exists on the device.
 */
@Composable
fun rememberVoiceSearch(
    prompt: String = "بگویید…",
    onResult: (String) -> Unit,
    onUnavailable: (() -> Unit)? = null,
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()
            if (!spoken.isNullOrBlank()) onResult(spoken)
        }
    }
    return {
        val intent = Intent(ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(EXTRA_LANGUAGE_MODEL, LANGUAGE_MODEL_FREE_FORM)
            putExtra(EXTRA_LANGUAGE, "fa-IR")
            putExtra(EXTRA_PROMPT, prompt)
        }
        try {
            launcher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            onUnavailable?.invoke()
        }
    }
}
