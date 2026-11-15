package ir.ahvaz.mehra.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.ahvaz.mehra.core.common.PLATE_LENGTH
import ir.ahvaz.mehra.core.common.PLATE_LETTERS
import ir.ahvaz.mehra.core.common.normalizePlate
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.ui.theme.MehraTheme

/**
 * Visual Iranian licence plate input (v2.2.0, enlarged in 2.2.1): the plate is drawn like a
 * real plate — a blue «ایران» band on the left holding the 2-digit province code, then the
 * 3-digit serial, the letter (gold-tinted cell) and the final 2 digits. Every character has
 * its OWN cell; tapping a cell opens a small picker so digits and the letter always land in
 * their correct section. The stored value is the canonical 8-char string
 * (province + serial + letter + duo).
 */
@Composable
fun IranPlateInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MehraTheme.colors
    val chars = normalizePlate(value).padEnd(PLATE_LENGTH, ' ').toCharArray()
    var editing by remember { mutableStateOf(-1) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(
                    2.dp,
                    Brush.linearGradient(
                        listOf(colors.accentStart, colors.accentMid, colors.accentEnd, colors.accentMid, colors.accentStart),
                    ),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Blue «ایران» band with the province-code cells.
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0B3AA0), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ایران",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                PlateCell(ch = chars[0], onClick = { if (enabled) editing = 0 }, onBlue = true)
                                PlateCell(ch = chars[1], onClick = { if (enabled) editing = 1 }, onBlue = true)
                            }
                        }
                    }
                    // Main serial: 3 digits, letter, 2 digits.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (pos in 2..4) {
                            PlateCell(ch = chars[pos], onClick = { if (enabled) editing = pos }, onBlue = false)
                        }
                        PlateCell(
                            ch = chars[5],
                            onClick = { if (enabled) editing = 5 },
                            onBlue = false,
                            letter = true,
                        )
                        for (pos in 6..7) {
                            PlateCell(ch = chars[pos], onClick = { if (enabled) editing = pos }, onBlue = false)
                        }
                    }
                }
            }
        }
    }

    if (editing in 0 until PLATE_LENGTH) {
        val isLetter = editing == 5
        val options: List<String> =
            if (isLetter) PLATE_LETTERS else (0..9).map { it.toString() }
        AlertDialog(
            onDismissRequest = { editing = -1 },
            title = {
                Text(
                    text = if (isLetter) "انتخاب حرف پلاک" else "انتخاب رقم پلاک",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.chunked(5).forEach { rowOptions ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowOptions.forEach { opt ->
                                TextButton(
                                    onClick = {
                                        chars[editing] = opt[0]
                                        onValueChange(chars.concatToString().trim())
                                        editing = -1
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = opt.toPersianDigits(),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editing = -1 }) { Text("بستن") }
            },
        )
    }
}

@Composable
private fun PlateCell(
    ch: Char,
    onClick: () -> Unit,
    onBlue: Boolean,
    letter: Boolean = false,
) {
    val bg = when {
        onBlue -> Color.White.copy(alpha = 0.92f)
        letter -> Color(0xFFEAD9AE)
        else -> Color(0xFFF7F3EA)
    }
    val fg = if (onBlue) Color(0xFF0B3AA0) else Color(0xFF1F2937)
    Box(
        modifier = Modifier
            .size(width = if (letter) 30.dp else 27.dp, height = 42.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .then(
                if (!onBlue) Modifier.border(BorderStroke(1.dp, Color(0xFF9CA3AF)), RoundedCornerShape(4.dp)) else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (ch == ' ') "" else ch.toString().toPersianDigits(),
            color = fg,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
