package ir.ahvaz.mehra.core.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ir.ahvaz.mehra.core.common.CurrencyUnit
import ir.ahvaz.mehra.core.common.formatMoneyFa
import ir.ahvaz.mehra.core.common.parseLongInput
import ir.ahvaz.mehra.core.common.toLatinDigits
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.date.JalaliDate
import ir.ahvaz.mehra.core.ui.theme.MehraTheme

@Composable
fun MehraTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    numeric: Boolean = false,
    password: Boolean = false,
    leadingIcon: ImageVector? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        label = { Text(label) },
        placeholder = if (placeholder != null) ({ Text(placeholder) }) else null,
        isError = error != null,
        supportingText = when {
            error != null -> ({ Text(error) })
            hint != null -> ({ Text(hint) })
            else -> null
        },
        leadingIcon = if (leadingIcon != null) {
            ({ Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp)) })
        } else null,
        trailingIcon = if (password) {
            ({
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (passwordVisible) "پنهان کردن رمز" else "نمایش رمز",
                        modifier = Modifier.size(20.dp),
                    )
                }
            })
        } else null,
        shape = MaterialTheme.shapes.medium,
        visualTransformation = if (password && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
            imeAction = imeAction,
        ),
    )
}

@Composable
fun MehraSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "جست‌وجو…",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (value.isNotBlank()) {
            ({ IconButton(onClick = { onValueChange("") }) { Icon(Icons.Rounded.Close, contentDescription = "پاک کردن") } })
        } else null,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

/** Read-only styled field that opens a picker (date, dropdown, …). */
@Composable
fun PickerField(
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "انتخاب کنید",
    icon: ImageVector = Icons.Rounded.CalendarMonth,
    enabled: Boolean = true,
    error: String? = null,
) {
    val colors = MehraTheme.colors
    val borderColor = when {
        error != null -> colors.danger
        else -> MaterialTheme.colorScheme.outline
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value?.takeIf { it.isNotBlank() } ?: placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (value.isNullOrBlank()) FontWeight.Normal else FontWeight.Medium,
                        color = if (value.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.labelSmall,
                color = colors.danger,
                modifier = Modifier.padding(start = 14.dp, top = 4.dp),
            )
        }
    }
}

@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    placeholder: String = "انتخاب کنید",
    enabled: Boolean = true,
    error: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        PickerField(
            label = label,
            value = selected?.let(optionLabel),
            onClick = { if (enabled) expanded = true },
            placeholder = placeholder,
            icon = Icons.Rounded.ChevronRight,
            enabled = enabled,
            error = error,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (options.isEmpty()) {
                DropdownMenuItem(text = { Text("گزینه‌ای موجود نیست") }, onClick = { expanded = false }, enabled = false)
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
fun JalaliDateField(
    label: String,
    value: JalaliDate?,
    onDateSelected: (JalaliDate) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    PickerField(
        label = label,
        value = value?.formatLongFa(),
        onClick = { if (enabled) showPicker = true },
        placeholder = "انتخاب تاریخ",
        icon = Icons.Rounded.CalendarMonth,
        enabled = enabled,
        error = error,
        modifier = modifier,
    )
    if (showPicker) {
        JalaliDatePickerDialog(
            initialDate = value,
            onDismiss = { showPicker = false },
            onConfirm = {
                showPicker = false
                onDateSelected(it)
            },
        )
    }
}

@Composable
fun AmountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    unit: CurrencyUnit = CurrencyUnit.RIAL,
    error: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
) {
    val parsed = parseLongInput(value)
    MehraTextField(
        label = label,
        value = value,
        onValueChange = { onValueChange(it.toLatinDigits().filter { ch -> ch.isDigit() }) },
        modifier = modifier,
        numeric = true,
        error = error,
        hint = hint,
        enabled = enabled,
        leadingIcon = null,
        imeAction = ImeAction.Done,
    )
    if (parsed != null && parsed > 0) {
        Text(
            text = formatMoneyFa(parsed, unit),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 14.dp, top = 2.dp),
        )
    }
}

@Composable
fun <T> FilterChipRow(
    items: List<T>,
    selected: T?,
    onSelect: (T?) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    allLabel: String? = "همه",
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp),
    ) {
        if (allLabel != null) {
            item {
                FilterChip(
                    selected = selected == null,
                    onClick = { onSelect(null) },
                    label = { Text(allLabel) },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }
        }
        items(items.size) { index ->
            val item = items[index]
            FilterChip(
                selected = item == selected,
                onClick = { onSelect(if (item == selected) null else item) },
                label = { Text(label(item)) },
            )
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
fun NumberStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 20,
    label: String? = null,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (label != null) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
        IconButton(onClick = { if (value > min) onValueChange(value - 1) }, enabled = value > min) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "کاهش")
        }
        Text(
            text = value.toString().toPersianDigits(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = { if (value < max) onValueChange(value + 1) }, enabled = value < max) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "افزایش")
        }
    }
}

/**
 * Jalali (Solar Hijri) date picker. Gregorian pickers are unusable for Iranian operators,
 * and the Material date picker cannot show a Solar Hijri calendar — so the grid is drawn
 * here using the verified [JalaliDate] conversion.
 */
@Composable
fun JalaliDatePickerDialog(
    initialDate: JalaliDate? = null,
    onDismiss: () -> Unit,
    onConfirm: (JalaliDate) -> Unit,
) {
    val today = remember { JalaliDate.today() }
    var viewYear by remember { mutableStateOf(initialDate?.year ?: today.year) }
    var viewMonth by remember { mutableStateOf(initialDate?.month ?: today.month) }
    val selected = initialDate

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MehraTheme.colors.cardBorder),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Month navigation
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewYear -= 1 }) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "سال قبل")
                    }
                    IconButton(onClick = { if (viewMonth == 1) { viewMonth = 12; viewYear -= 1 } else viewMonth -= 1 }) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "ماه قبل")
                    }
                    Text(
                        text = "${JalaliDate.MONTH_NAMES_FA[viewMonth - 1]} ${viewYear.toString().toPersianDigits()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                    IconButton(onClick = { if (viewMonth == 12) { viewMonth = 1; viewYear += 1 } else viewMonth += 1 }) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "ماه بعد")
                    }
                    IconButton(onClick = { viewYear += 1 }) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "سال بعد")
                    }
                }

                // Weekday header (Saturday first)
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                        )
                    }
                }

                val firstOfMonth = JalaliDate(viewYear, viewMonth, 1)
                val leadingBlanks = firstOfMonth.dayOfWeek
                val monthLength = firstOfMonth.lengthOfMonth
                val cells = leadingBlanks + monthLength
                val rows = (cells + 6) / 7
                for (row in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val index = row * 7 + col
                            val dayNumber = index - leadingBlanks + 1
                            if (dayNumber in 1..monthLength) {
                                DayCell(
                                    day = dayNumber,
                                    isToday = viewYear == today.year && viewMonth == today.month && dayNumber == today.day,
                                    isSelected = selected != null && selected.year == viewYear &&
                                        selected.month == viewMonth && selected.day == dayNumber,
                                    onClick = { onConfirm(JalaliDate(viewYear, viewMonth, dayNumber)) },
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { onConfirm(today) }) { Text("امروز") }
                    Row {
                        TextButton(onClick = onDismiss) { Text("انصراف") }
                        TextButton(onClick = { onConfirm(JalaliDate(viewYear, viewMonth, 1)) }) { Text("انتخاب ماه") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MehraTheme.colors
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .padding(2.dp)
            .height(38.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = RoundedCornerShape(10.dp), color = background) {
            Text(
                text = day.toString().toPersianDigits(),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

