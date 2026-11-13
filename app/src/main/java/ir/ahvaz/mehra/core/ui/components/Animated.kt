package ir.ahvaz.mehra.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.date.TehranTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/**
 * Micro-interaction helpers (v1.5.0): count-up numbers for the live dashboard and a
 * Tehran-zone clock that ticks every second — they make the home screen feel alive.
 */

/** Animates an integer from its previous value to [target] (count-up effect). */
@Composable
fun AnimatedInt(target: Int, durationMillis: Int = 900): Int {
    val animated by animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
        label = "countUp",
    )
    return animated
}

/** Current Tehran time, re-emitted every [tickMillis] so the UI redraws like a real clock. */
@Composable
fun rememberTehranClock(tickMillis: Long = 1000L): ZonedDateTime {
    var now by remember { mutableStateOf(TehranTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(tickMillis)
            now = TehranTime.now()
        }
    }
    return now
}

private val CLOCK_PATTERN = DateTimeFormatter.ofPattern("HH:mm:ss")

/** "14:05:09" rendered with Persian digits. */
fun ZonedDateTime.clockFa(): String = format(CLOCK_PATTERN).toPersianDigits()

/** Persian time-of-day greeting used by the dashboard hero. */
fun timeOfDayGreetingFa(hour: Int): String = when {
    hour < 5 -> "شب بخیر"
    hour < 12 -> "صبح بخیر"
    hour < 15 -> "ظهر بخیر"
    hour < 19 -> "عصر بخیر"
    else -> "شب بخیر"
}
