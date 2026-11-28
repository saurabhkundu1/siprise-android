package com.applify.watertracker

import android.app.TimePickerDialog
import android.content.Intent
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat

@Composable
fun SettingsScreen(
    store: SipriseStore,
    palette: SiprisePalette,
    remindersEnabled: Boolean,
    notificationDenied: Boolean,
    themeMode: ThemeMode,
    colorScheme: SipriseColorScheme,
    volumeUnit: VolumeUnit,
    onBack: () -> Unit,
    onReminderChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onColorSchemeChange: (SipriseColorScheme) -> Unit,
    onVolumeUnitChange: (VolumeUnit) -> Unit,
    onDataChanged: () -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val number = remember { NumberFormat.getIntegerInstance() }

    var interval by remember { mutableIntStateOf(store.interval()) }
    var reminderStart by remember { mutableIntStateOf(store.reminderStart()) }
    var reminderEnd by remember { mutableIntStateOf(store.reminderEnd()) }
    var pauseOnGoal by remember { mutableStateOf(store.pauseOnGoal()) }
    var goal by remember { mutableIntStateOf(store.goal()) }
    var defaultGlass by remember { mutableIntStateOf(store.defaultGlassAmount()) }
    var preset1 by remember { mutableIntStateOf(store.quickPreset1()) }
    var preset2 by remember { mutableIntStateOf(store.quickPreset2()) }
    var widgetAmount by remember { mutableIntStateOf(store.widgetAmount()) }

    var showResetTodayDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.page)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Back to main screen" }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back_rounded),
                    contentDescription = "Back",
                    tint = palette.text
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Settings",
                color = palette.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        // 1. REMINDERS
        SectionTitle("Reminders", palette)
        SettingsCard(palette) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Hydration reminders", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Gentle nudges during your active hours", color = palette.muted, fontSize = 12.sp)
                }
                Switch(
                    checked = remindersEnabled,
                    onCheckedChange = onReminderChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.onPrimary,
                        checkedTrackColor = palette.primary
                    )
                )
            }

            if (notificationDenied) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.softChip)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Notifications are blocked in system settings.",
                        color = palette.text,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    }) {
                        Text("Open settings", color = palette.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = palette.divider)
            Spacer(Modifier.height(14.dp))

            Text("Reminder interval", color = palette.muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(60 to "1 hr", 90 to "1.5 hrs", 120 to "2 hrs", 180 to "3 hrs").forEach { (minutes, label) ->
                    val selected = interval == minutes
                    Button(
                        onClick = {
                            interval = minutes
                            store.setInterval(minutes)
                            if (remindersEnabled) ReminderScheduler.schedule(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) palette.primary else palette.softChip,
                            contentColor = if (selected) palette.onPrimary else palette.text
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("Active hours (quiet outside window)", color = palette.muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        TimePickerDialog(context, { _, hour, minute ->
                            val newStart = hour * 60 + minute
                            if (newStart == reminderEnd) {
                                Toast.makeText(context, R.string.reminder_hours_same, Toast.LENGTH_SHORT).show()
                            } else {
                                reminderStart = newStart
                                store.setReminderWindow(newStart, reminderEnd)
                            }
                        }, reminderStart / 60, reminderStart % 60, DateFormat.is24HourFormat(context)).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.softChip, contentColor = palette.text),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start: ${formatReminderTime(context, reminderStart, locale)}", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        TimePickerDialog(context, { _, hour, minute ->
                            val newEnd = hour * 60 + minute
                            if (reminderStart == newEnd) {
                                Toast.makeText(context, R.string.reminder_hours_same, Toast.LENGTH_SHORT).show()
                            } else {
                                reminderEnd = newEnd
                                store.setReminderWindow(reminderStart, newEnd)
                            }
                        }, reminderEnd / 60, reminderEnd % 60, DateFormat.is24HourFormat(context)).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.softChip, contentColor = palette.text),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("End: ${formatReminderTime(context, reminderEnd, locale)}", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = palette.divider)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Pause when goal reached", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Stop notifications once daily target is met", color = palette.muted, fontSize = 12.sp)
                }
                Switch(
                    checked = pauseOnGoal,
                    onCheckedChange = {
                        pauseOnGoal = it
                        store.setPauseOnGoal(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.onPrimary,
                        checkedTrackColor = palette.primary
                    )
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // 2. HYDRATION & UNITS
        SectionTitle("Hydration & Units", palette)
        SettingsCard(palette) {
            Text("Display unit", color = palette.muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                VolumeUnit.entries.forEachIndexed { index, unit ->
                    SegmentedButton(
                        selected = volumeUnit == unit,
                        onClick = { onVolumeUnitChange(unit) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = VolumeUnit.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = palette.softChip,
                            activeContentColor = palette.primary,
                            inactiveContainerColor = palette.card,
                            inactiveContentColor = palette.text,
                            activeBorderColor = palette.primary,
                            inactiveBorderColor = palette.divider
                        )
                    ) {
                        Text(unit.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = palette.divider)
            Spacer(Modifier.height(8.dp))

            StepperRow(
                title = "Daily goal",
                valueText = formatVolume(goal, volumeUnit, number),
                canDecrease = goal > 500,
                canIncrease = goal < 6000,
                palette = palette,
                onDecrease = { goal = (goal - 250).coerceAtLeast(500); store.setGoal(goal); onDataChanged() },
                onIncrease = { goal = (goal + 250).coerceAtMost(6000); store.setGoal(goal); onDataChanged() }
            )

            StepperRow(
                title = "Default glass size",
                valueText = formatVolume(defaultGlass, volumeUnit, number),
                canDecrease = defaultGlass > 100,
                canIncrease = defaultGlass < 500,
                palette = palette,
                onDecrease = { defaultGlass = (defaultGlass - 50).coerceAtLeast(100); store.setDefaultGlassAmount(defaultGlass); onDataChanged() },
                onIncrease = { defaultGlass = (defaultGlass + 50).coerceAtMost(500); store.setDefaultGlassAmount(defaultGlass); onDataChanged() }
            )

            StepperRow(
                title = "Quick-add preset 1",
                valueText = formatVolume(preset1, volumeUnit, number),
                canDecrease = preset1 > 50,
                canIncrease = preset1 < 500,
                palette = palette,
                onDecrease = { preset1 = (preset1 - 50).coerceAtLeast(50); store.setQuickPreset1(preset1); onDataChanged() },
                onIncrease = { preset1 = (preset1 + 50).coerceAtMost(500); store.setQuickPreset1(preset1); onDataChanged() }
            )

            StepperRow(
                title = "Quick-add preset 2",
                valueText = formatVolume(preset2, volumeUnit, number),
                canDecrease = preset2 > 100,
                canIncrease = preset2 < 1000,
                palette = palette,
                onDecrease = { preset2 = (preset2 - 50).coerceAtLeast(100); store.setQuickPreset2(preset2); onDataChanged() },
                onIncrease = { preset2 = (preset2 + 50).coerceAtMost(1000); store.setQuickPreset2(preset2); onDataChanged() }
            )

            StepperRow(
                title = "Widget increment",
                valueText = formatVolume(widgetAmount, volumeUnit, number),
                canDecrease = widgetAmount > 50,
                canIncrease = widgetAmount < 1000,
                palette = palette,
                onDecrease = { widgetAmount = (widgetAmount - 50).coerceAtLeast(50); store.setWidgetAmount(widgetAmount); onDataChanged() },
                onIncrease = { widgetAmount = (widgetAmount + 50).coerceAtMost(1000); store.setWidgetAmount(widgetAmount); onDataChanged() }
            )
        }

        Spacer(Modifier.height(18.dp))

        // 3. APPEARANCE
        SectionTitle("Appearance", palette)
        SettingsCard(palette) {
            Text("Theme mode", color = palette.muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = palette.softChip,
                            activeContentColor = palette.primary,
                            inactiveContainerColor = palette.card,
                            inactiveContentColor = palette.text,
                            activeBorderColor = palette.primary,
                            inactiveBorderColor = palette.divider
                        )
                    ) {
                        Text(mode.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Color scheme", color = palette.muted, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SipriseColorScheme.entries.forEach { scheme ->
                    val selected = colorScheme == scheme
                    val schemeLight = scheme.palette(palette.isDark)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .then(
                                if (selected) Modifier.border(2.dp, palette.text, CircleShape)
                                else Modifier
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .clickable { onColorSchemeChange(scheme) }
                            .semantics { contentDescription = "${scheme.label} color scheme" },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawArc(
                                color = schemeLight.primary,
                                startAngle = 135f,
                                sweepAngle = 180f,
                                useCenter = true
                            )
                            drawArc(
                                color = schemeLight.softChip,
                                startAngle = 315f,
                                sweepAngle = 180f,
                                useCenter = true
                            )
                            drawCircle(
                                color = schemeLight.primary.copy(alpha = 0.25f),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(schemeLight.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check_rounded),
                                    contentDescription = null,
                                    tint = schemeLight.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Active: ${colorScheme.label}",
                color = palette.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(18.dp))

        // 4. DATA
        SectionTitle("Data", palette)
        SettingsCard(palette) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showResetTodayDialog = true }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Reset today's intake", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Clear all water logs recorded today", color = palette.muted, fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = palette.divider)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showClearAllDialog = true }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Clear all history", color = Color(0xFFD32F2F), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Permanently remove all past hydration records", color = palette.muted, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }

    if (showResetTodayDialog) {
        AlertDialog(
            onDismissRequest = { showResetTodayDialog = false },
            title = { Text("Reset today's intake?") },
            text = { Text("This will remove all water entries logged today. Past days will remain untouched.") },
            confirmButton = {
                TextButton(onClick = {
                    store.resetToday()
                    onDataChanged()
                    showResetTodayDialog = false
                    Toast.makeText(context, "Today's intake reset", Toast.LENGTH_SHORT).show()
                }) { Text("Reset", color = Color(0xFFD32F2F)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetTodayDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear all history?") },
            text = { Text("This will permanently delete your entire hydration history across all days. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    store.clearAllHistory()
                    onDataChanged()
                    showClearAllDialog = false
                    Toast.makeText(context, "All history cleared", Toast.LENGTH_SHORT).show()
                }) { Text("Clear all", color = Color(0xFFD32F2F)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String, palette: SiprisePalette) {
    Text(
        text = title,
        color = palette.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(palette: SiprisePalette, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = palette.card),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun StepperRow(
    title: String,
    valueText: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    palette: SiprisePalette,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = palette.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        TextButton(enabled = canDecrease, onClick = onDecrease) {
            Text("−", fontSize = 18.sp, color = if (canDecrease) palette.primary else palette.muted)
        }
        Text(valueText, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        TextButton(enabled = canIncrease, onClick = onIncrease) {
            Text("+", fontSize = 18.sp, color = if (canIncrease) palette.primary else palette.muted)
        }
    }
}
