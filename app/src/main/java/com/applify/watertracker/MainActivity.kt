package com.applify.watertracker

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DrinkLog(val amount: Int, val time: Long)

class SipriseStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("siprise", Context.MODE_PRIVATE)

    fun goal() = prefs.getInt("goal", 2000).coerceIn(500, 6000)
    fun setGoal(value: Int) {
        prefs.edit { putInt("goal", value.coerceIn(500, 6000)) }
        SipriseWidget.update(appContext)
        if (remindersEnabled()) ReminderScheduler.schedule(appContext)
    }

    fun defaultGlassAmount() = prefs.getInt("default_glass", 250).takeIf { it in 100..500 && it % 50 == 0 } ?: 250
    fun setDefaultGlassAmount(value: Int) {
        prefs.edit { putInt("default_glass", value.coerceIn(100, 500)) }
    }

    fun quickPreset1() = prefs.getInt("quick_preset_1", 150).takeIf { it in 50..500 && it % 50 == 0 } ?: 150
    fun setQuickPreset1(value: Int) {
        prefs.edit { putInt("quick_preset_1", value.coerceIn(50, 500)) }
    }

    fun quickPreset2() = prefs.getInt("quick_preset_2", 500).takeIf { it in 100..1000 && it % 50 == 0 } ?: 500
    fun setQuickPreset2(value: Int) {
        prefs.edit { putInt("quick_preset_2", value.coerceIn(100, 1000)) }
    }

    fun widgetAmount() = prefs.getInt("widget_amount", 250).takeIf { it in 50..1000 && it % 50 == 0 } ?: 250
    fun setWidgetAmount(value: Int) {
        require(value in 50..1000 && value % 50 == 0)
        prefs.edit { putInt("widget_amount", value) }
        SipriseWidget.update(appContext)
    }

    fun remindersEnabled() = prefs.getBoolean("reminders", false)
    fun setRemindersEnabled(value: Boolean) = prefs.edit { putBoolean("reminders", value) }

    fun pauseOnGoal() = prefs.getBoolean("pause_on_goal", true)
    fun setPauseOnGoal(value: Boolean) {
        prefs.edit { putBoolean("pause_on_goal", value) }
        if (remindersEnabled()) ReminderScheduler.schedule(appContext)
    }

    fun interval() = prefs.getInt("interval", 60).takeIf { it in listOf(60, 90, 120, 180) } ?: 60
    fun setInterval(value: Int) {
        require(value in listOf(60, 90, 120, 180))
        prefs.edit { putInt("interval", value) }
    }

    fun reminderStart() = prefs.getInt("reminder_start", 8 * 60).takeIf { it in 0 until 24 * 60 } ?: 8 * 60
    fun reminderEnd() = prefs.getInt("reminder_end", 22 * 60).takeIf { it in 0 until 24 * 60 } ?: 22 * 60

    fun setReminderWindow(start: Int, end: Int) {
        require(start in 0 until 24 * 60 && end in 0 until 24 * 60 && start != end)
        prefs.edit {
            putInt("reminder_start", start)
            putInt("reminder_end", end)
        }
        if (remindersEnabled()) ReminderScheduler.schedule(appContext)
    }

    fun themeMode(): ThemeMode = ThemeMode.fromId(prefs.getInt("theme_mode", ThemeMode.System.id))
    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putInt("theme_mode", mode.id) }
        SipriseWidget.update(appContext)
    }

    fun colorScheme(): SipriseColorScheme = SipriseColorScheme.fromKey(prefs.getString("color_scheme", null))
    fun setColorScheme(scheme: SipriseColorScheme) {
        prefs.edit { putString("color_scheme", scheme.key) }
        SipriseWidget.update(appContext)
    }

    fun volumeUnit(): VolumeUnit = VolumeUnit.fromKey(prefs.getString("volume_unit", null))
    fun setVolumeUnit(unit: VolumeUnit) {
        prefs.edit { putString("volume_unit", unit.key) }
        SipriseWidget.update(appContext)
    }

    fun notificationPromptDismissed() = prefs.getBoolean("notif_prompt_dismissed", false)
    fun setNotificationPromptDismissed(dismissed: Boolean) = prefs.edit { putBoolean("notif_prompt_dismissed", dismissed) }

    fun logs(date: LocalDate = LocalDate.now()): List<DrinkLog> = try {
        val stored = JSONArray(prefs.getString("logs_$date", "[]"))
        (0 until stored.length()).mapNotNull { index ->
            val item = stored.optJSONObject(index) ?: return@mapNotNull null
            val amount = item.optInt("amount", 0)
            val time = item.optLong("time", 0L)
            if (amount > 0 && time > 0L) DrinkLog(amount, time) else null
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun add(amount: Int) {
        require(amount in 1..5000)
        val updated = JSONArray().apply {
            logs().forEach { put(JSONObject().put("amount", it.amount).put("time", it.time)) }
            put(JSONObject().put("amount", amount).put("time", System.currentTimeMillis()))
        }
        prefs.edit { putString("logs_${LocalDate.now()}", updated.toString()) }
        if (remindersEnabled()) ReminderScheduler.schedule(appContext)
        SipriseWidget.update(appContext)
    }

    fun undoLast(): Boolean {
        val current = logs()
        if (current.isEmpty()) return false
        val updated = JSONArray().apply {
            current.dropLast(1).forEach { put(JSONObject().put("amount", it.amount).put("time", it.time)) }
        }
        prefs.edit { putString("logs_${LocalDate.now()}", updated.toString()) }
        if (remindersEnabled()) ReminderScheduler.schedule(appContext)
        SipriseWidget.update(appContext)
        return true
    }

    fun resetToday() {
        prefs.edit { remove("logs_${LocalDate.now()}") }
        if (remindersEnabled()) ReminderScheduler.schedule(appContext)
        SipriseWidget.update(appContext)
    }

    fun clearAllHistory() {
        val logKeys = prefs.all.keys.filter { it.startsWith("logs_") }
        prefs.edit { logKeys.forEach { remove(it) } }
        if (remindersEnabled()) ReminderScheduler.schedule(appContext)
        SipriseWidget.update(appContext)
    }

    fun total(date: LocalDate) = logs(date).sumOf { it.amount }
}

class MainActivity : AppCompatActivity() {
    private val store by lazy { SipriseStore(this) }
    private var reminders by mutableStateOf(false)
    private var notificationDenied by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(true)
    private var showSettings by mutableStateOf(false)
    private var dataVersion by mutableIntStateOf(0)
    private var permissionPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reminders = store.remindersEnabled()
        hasNotificationPermission = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        WindowCompat.setDecorFitsSystemWindows(window, false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (showSettings) {
                    showSettings = false
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        setContentView(ComposeView(this).apply {
            setContent {
                var themeMode by remember { mutableStateOf(store.themeMode()) }
                var colorScheme by remember { mutableStateOf(store.colorScheme()) }
                var volumeUnit by remember { mutableStateOf(store.volumeUnit()) }

                val isDark = when (themeMode) {
                    ThemeMode.Dark -> true
                    ThemeMode.Light -> false
                    ThemeMode.System -> isSystemInDarkTheme()
                }
                val palette = colorScheme.palette(isDark)

                LaunchedEffect(isDark) {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !isDark
                        isAppearanceLightNavigationBars = !isDark
                    }
                }

                val materialColors = if (isDark) {
                    darkColorScheme(
                        primary = palette.primary,
                        onPrimary = palette.onPrimary,
                        background = palette.page,
                        surface = palette.card,
                        onBackground = palette.text,
                        onSurface = palette.text,
                    )
                } else {
                    lightColorScheme(
                        primary = palette.primary,
                        onPrimary = palette.onPrimary,
                        background = palette.page,
                        surface = palette.card,
                        onBackground = palette.text,
                        onSurface = palette.text,
                    )
                }

                MaterialTheme(colorScheme = materialColors) {
                    if (showSettings) {
                        SettingsScreen(
                            store = store,
                            palette = palette,
                            remindersEnabled = reminders,
                            notificationDenied = notificationDenied,
                            themeMode = themeMode,
                            colorScheme = colorScheme,
                            volumeUnit = volumeUnit,
                            onBack = { showSettings = false },
                            onReminderChange = ::changeReminders,
                            onThemeModeChange = {
                                themeMode = it
                                store.setThemeMode(it)
                            },
                            onColorSchemeChange = {
                                colorScheme = it
                                store.setColorScheme(it)
                            },
                            onVolumeUnitChange = {
                                volumeUnit = it
                                store.setVolumeUnit(it)
                            },
                            onDataChanged = { dataVersion++ },
                        )
                    } else {
                        SipriseScreen(
                            store = store,
                            palette = palette,
                            volumeUnit = volumeUnit,
                            remindersEnabled = reminders,
                            hasNotificationPermission = hasNotificationPermission,
                            notificationDenied = notificationDenied,
                            dataVersion = dataVersion,
                            onOpenSettings = { showSettings = true },
                            onRequestNotificationPermission = { changeReminders(true) },
                        )
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        hasNotificationPermission = granted
        if (granted) notificationDenied = false
        dataVersion++
    }

    private fun changeReminders(enabled: Boolean) {
        if (enabled &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionPending = true
            notificationDenied = false
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 20)
            return
        }
        applyReminderSetting(enabled)
    }

    private fun applyReminderSetting(enabled: Boolean) {
        store.setRemindersEnabled(enabled)
        reminders = enabled
        if (enabled) ReminderScheduler.schedule(this) else ReminderScheduler.cancel(this)
    }

    @Deprecated("Use the platform permission launcher")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 20 && permissionPending) {
            permissionPending = false
            val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            hasNotificationPermission = granted
            notificationDenied = !granted
            applyReminderSetting(granted)
        }
    }
}

@Composable
private fun SipriseScreen(
    store: SipriseStore,
    palette: SiprisePalette,
    volumeUnit: VolumeUnit,
    remindersEnabled: Boolean,
    hasNotificationPermission: Boolean,
    notificationDenied: Boolean,
    dataVersion: Int,
    onOpenSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
) {
    var logs by remember(dataVersion) { mutableStateOf(store.logs()) }
    var goal by remember(dataVersion) { mutableIntStateOf(store.goal()) }
    var defaultGlass by remember(dataVersion) { mutableIntStateOf(store.defaultGlassAmount()) }
    var preset1 by remember(dataVersion) { mutableIntStateOf(store.quickPreset1()) }
    var preset2 by remember(dataVersion) { mutableIntStateOf(store.quickPreset2()) }
    var promptDismissed by remember { mutableStateOf(store.notificationPromptDismissed()) }
    var customAmount by remember { mutableStateOf("") }
    var showCustomAmount by remember { mutableStateOf(false) }

    val total = logs.sumOf { it.amount }
    val number = remember { NumberFormat.getIntegerInstance() }
    val context = LocalContext.current
    val currentLocale = LocalConfiguration.current.locales[0]

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.page)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(palette.softChip)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center
            ) {
                Text("✦", color = palette.primary, fontSize = 21.sp)
            }
            Spacer(Modifier.width(11.dp))
            Column {
                Text("Siprise", color = palette.text, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", currentLocale)),
                    color = palette.muted,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.semantics {
                    contentDescription = if (remindersEnabled) "Reminders active, open settings" else "Reminders off, open settings"
                }
            ) {
                Icon(
                    painter = painterResource(
                        if (remindersEnabled) R.drawable.ic_notifications_rounded else R.drawable.ic_notifications_off_rounded
                    ),
                    contentDescription = null,
                    tint = if (remindersEnabled) palette.primary else palette.muted,
                    modifier = Modifier.size(21.dp)
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.semantics { contentDescription = "Open settings" }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings_rounded),
                    contentDescription = "Settings",
                    tint = palette.text,
                    modifier = Modifier.size(23.dp)
                )
            }
        }

        if (!hasNotificationPermission && !promptDismissed) {
            Spacer(Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = palette.softChip)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (notificationDenied) "Notifications disabled" else "Enable hydration reminders",
                            color = palette.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (notificationDenied) "Allow notifications in system settings for gentle nudges."
                            else "Get gentle reminders during your daytime hours.",
                            color = palette.muted,
                            fontSize = 11.sp
                        )
                    }
                    TextButton(onClick = {
                        promptDismissed = true
                        store.setNotificationPromptDismissed(true)
                    }) {
                        Text("Later", color = palette.muted, fontSize = 12.sp)
                    }
                    TextButton(onClick = {
                        if (notificationDenied) {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            )
                        } else {
                            onRequestNotificationPermission()
                        }
                    }) {
                        Text(
                            if (notificationDenied) "Settings" else "Allow",
                            color = palette.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Text("A little sip, a brighter day.", color = palette.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text("Small sips add up. Keep your rhythm going.", color = palette.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 5.dp))

        Spacer(Modifier.height(20.dp))
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = palette.card)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("TODAY'S HYDRATION", color = palette.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${number.format((total * 100 / goal).coerceAtMost(100))}%", color = palette.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(190.dp).semantics {
                        contentDescription = "Hydration progress: ${formatVolume(total, volumeUnit, number)} of ${formatVolume(goal, volumeUnit, number)}, ${((total.toFloat() / goal * 100).toInt().coerceAtMost(100))} percent completed"
                    }
                ) {
                    val fraction = (total.toFloat() / goal).coerceIn(0f, 1f)
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 15.dp.toPx()
                        drawArc(palette.track, -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round), size = Size(size.width, size.height))
                        if (fraction > 0f) drawArc(palette.primary, -90f, 360f * fraction, false, style = Stroke(stroke, cap = StrokeCap.Round), size = Size(size.width, size.height))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatVolumeNumber(total, volumeUnit, number), color = palette.text, fontSize = 35.sp, fontWeight = FontWeight.Bold)
                        Text("${volumeUnit.label} so far", color = palette.muted, fontSize = 13.sp)
                        Text("of ${formatVolume(goal, volumeUnit, number)}", color = palette.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily goal", color = palette.muted, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { goal = (goal - 250).coerceAtLeast(500); store.setGoal(goal) },
                        modifier = Modifier.semantics {
                            contentDescription = "Decrease daily goal by 250 milliliters"
                            role = Role.Button
                        }
                    ) { Text("−", fontSize = 20.sp, color = palette.primary) }
                    Text(formatVolume(goal, volumeUnit, number), color = palette.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    TextButton(
                        onClick = { goal = (goal + 250).coerceAtMost(6000); store.setGoal(goal) },
                        modifier = Modifier.semantics {
                            contentDescription = "Increase daily goal by 250 milliliters"
                            role = Role.Button
                        }
                    ) { Text("+", fontSize = 20.sp, color = palette.primary) }
                }
            }
        }

        if (total >= goal) {
            Spacer(Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = palette.goalBannerBg)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎉", fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Goal Achieved!", color = palette.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "You reached ${formatVolume(total, volumeUnit, number)} of your ${formatVolume(goal, volumeUnit, number)} target today. Keep up the rhythm!",
                            color = palette.text,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { store.add(defaultGlass); logs = store.logs() },
            modifier = Modifier.fillMaxWidth().height(58.dp).semantics {
                contentDescription = "Log a glass of water, ${formatVolume(defaultGlass, volumeUnit, number)}"
            },
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
        ) {
            Text("＋   Log a glass  ·  ${formatVolume(defaultGlass, volumeUnit, number)}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAdd("+${formatVolume(preset1, volumeUnit, number)}", palette) { store.add(preset1); logs = store.logs() }
            QuickAdd("+${formatVolume(preset2, volumeUnit, number)}", palette) { store.add(preset2); logs = store.logs() }
            QuickAdd("Custom", palette) { customAmount = ""; showCustomAmount = true }
        }

        Spacer(Modifier.height(25.dp))
        Text("This week", color = palette.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Your sips, day by day", color = palette.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp, bottom = 12.dp))
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = palette.card)) {
            Row(
                Modifier.fillMaxWidth().height(135.dp).padding(horizontal = 12.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                (6 downTo 0).forEach { daysAgo ->
                    val date = LocalDate.now().minusDays(daysAgo.toLong())
                    val amount = if (daysAgo == 0) total else store.total(date)
                    val fill = (amount.toFloat() / goal).coerceIn(0f, 1f)
                    val dayFullName = date.dayOfWeek.getDisplayName(TextStyle.FULL, currentLocale)
                    val daySummary = if (daysAgo == 0) {
                        "Today ($dayFullName): ${formatVolume(amount, volumeUnit, number)} of ${formatVolume(goal, volumeUnit, number)}"
                    } else {
                        "$dayFullName: ${formatVolume(amount, volumeUnit, number)} of ${formatVolume(goal, volumeUnit, number)}"
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.semantics { contentDescription = daySummary }
                    ) {
                        Box(Modifier.width(20.dp).height(70.dp).clip(RoundedCornerShape(12.dp)).background(palette.track), contentAlignment = Alignment.BottomCenter) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight(fill).clip(RoundedCornerShape(12.dp)).background(if (daysAgo == 0) palette.primary else palette.barMuted))
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            date.dayOfWeek.getDisplayName(TextStyle.NARROW, currentLocale),
                            color = if (daysAgo == 0) palette.primary else palette.muted,
                            fontSize = 12.sp,
                            fontWeight = if (daysAgo == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        if (logs.isNotEmpty()) {
            Spacer(Modifier.height(25.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Recent sips", color = palette.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { if (store.undoLast()) logs = store.logs() },
                    modifier = Modifier.semantics { contentDescription = "Undo last logged drink" }
                ) { Text("Undo last", color = palette.primary) }
            }
            Spacer(Modifier.height(10.dp))
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = palette.card)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    logs.takeLast(4).asReversed().forEachIndexed { index, log ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(34.dp).clip(CircleShape).background(palette.softChip), contentAlignment = Alignment.Center) {
                                Text("◌", color = palette.primary, fontSize = 20.sp)
                            }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text("Water", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(DateFormat.getTimeFormat(context).format(Date(log.time)), color = palette.muted, fontSize = 12.sp)
                            }
                            Text("+${formatVolume(log.amount, volumeUnit, number)}", color = palette.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (index < logs.takeLast(4).size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(palette.divider))
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "Sip at your own pace. Your log is saved locally.",
            color = palette.muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        )
    }

    if (showCustomAmount) {
        val parsedAmount = customAmount.toIntOrNull()
        AlertDialog(
            onDismissRequest = { showCustomAmount = false },
            title = { Text("Custom amount") },
            text = {
                OutlinedTextField(
                    value = customAmount,
                    onValueChange = { customAmount = it.filter(Char::isDigit).take(4) },
                    label = { Text("Milliliters") },
                    supportingText = { Text("Enter 1–5,000 ml") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        parsedAmount?.let { store.add(it); logs = store.logs(); showCustomAmount = false }
                    },
                    enabled = parsedAmount != null && parsedAmount in 1..5000,
                ) { Text("Log water") }
            },
            dismissButton = { TextButton(onClick = { showCustomAmount = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RowScope.QuickAdd(label: String, palette: SiprisePalette, onClick: () -> Unit) {
    val description = if (label.startsWith("+")) "Quick add $label of water" else "Log custom amount of water"
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(48.dp).semantics {
            contentDescription = description
        },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = palette.softChip, contentColor = palette.text),
    ) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
}

internal object ReminderScheduler {
    private const val CHANNEL_ID = "siprise_water_reminders"
    const val ACTION_SNOOZE = "com.applify.watertracker.SNOOZE_REMINDER"
    const val ACTION_QUICK_LOG = "com.applify.watertracker.QUICK_LOG_REMINDER"

    fun schedule(context: Context) {
        val store = SipriseStore(context)
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context)
        val next = Calendar.getInstance()
        if (store.pauseOnGoal() && store.total(LocalDate.now()) >= store.goal()) {
            next.add(Calendar.DAY_OF_YEAR, 1)
            setWindowStart(next, store.reminderStart())
        } else {
            next.add(Calendar.MINUTE, store.interval())
            if (!isWithinReminderWindow(next, store.reminderStart(), store.reminderEnd())) {
                setNextWindowStart(next, store.reminderStart(), store.reminderEnd())
            }
        }
        next.set(Calendar.SECOND, 0)
        next.set(Calendar.MILLISECOND, 0)
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
    }

    fun cancel(context: Context) = context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))

    fun snooze(context: Context) {
        val next = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        context.getSystemService(NotificationManager::class.java).cancel(1)
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pendingIntent(context))
    }

    fun isWithinReminderWindow(time: Calendar, start: Int, end: Int): Boolean {
        val minute = time.get(Calendar.HOUR_OF_DAY) * 60 + time.get(Calendar.MINUTE)
        return if (start < end) minute in start until end else minute >= start || minute < end
    }

    private fun setNextWindowStart(time: Calendar, start: Int, end: Int) {
        val minute = time.get(Calendar.HOUR_OF_DAY) * 60 + time.get(Calendar.MINUTE)
        val moveToTomorrow = when {
            start < end -> minute >= end
            else -> minute < end || minute >= start
        }
        if (moveToTomorrow) time.add(Calendar.DAY_OF_YEAR, 1)
        setWindowStart(time, start)
    }

    private fun setWindowStart(time: Calendar, start: Int) {
        time.set(Calendar.HOUR_OF_DAY, start / 60)
        time.set(Calendar.MINUTE, start % 60)
        time.set(Calendar.SECOND, 0)
        time.set(Calendar.MILLISECOND, 0)
    }

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        1,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun postNotification(context: Context) {
        val store = SipriseStore(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Hydration Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Gentle reminders to take a sip of water"
            },
        )
        try {
            val openAppIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val quickLogIntent = PendingIntent.getBroadcast(
                context,
                3,
                Intent(context, ReminderReceiver::class.java).setAction(ACTION_QUICK_LOG),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val snoozeIntent = PendingIntent.getBroadcast(
                context,
                2,
                Intent(context, ReminderReceiver::class.java).setAction(ACTION_SNOOZE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val glassLabel = "+${formatVolume(store.defaultGlassAmount(), store.volumeUnit())}"
            manager.notify(
                1,
                Notification.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Time for a little sip")
                    .setContentText("Take a moment to enjoy some water.")
                    .setContentIntent(openAppIntent)
                    .addAction(
                        Notification.Action.Builder(
                            Icon.createWithResource(context, R.drawable.ic_notification),
                            glassLabel,
                            quickLogIntent,
                        ).build(),
                    )
                    .addAction(
                        Notification.Action.Builder(
                            Icon.createWithResource(context, R.drawable.ic_notification),
                            context.getString(R.string.reminder_snooze),
                            snoozeIntent,
                        ).build(),
                    )
                    .setAutoCancel(true)
                    .build(),
            )
        } catch (_: SecurityException) {
            // Notifications can be disabled later in Android settings.
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val store = SipriseStore(context)
        if (!store.remindersEnabled()) return
        when (intent?.action) {
            ReminderScheduler.ACTION_QUICK_LOG -> {
                context.getSystemService(NotificationManager::class.java).cancel(1)
                store.add(store.defaultGlassAmount())
                return
            }
            ReminderScheduler.ACTION_SNOOZE -> {
                if (!store.pauseOnGoal() || store.total(LocalDate.now()) < store.goal()) {
                    ReminderScheduler.snooze(context)
                } else {
                    ReminderScheduler.schedule(context)
                }
                return
            }
        }
        val underGoalOrNotPaused = !store.pauseOnGoal() || store.total(LocalDate.now()) < store.goal()
        if (underGoalOrNotPaused && ReminderScheduler.isWithinReminderWindow(Calendar.getInstance(), store.reminderStart(), store.reminderEnd())) {
            ReminderScheduler.postNotification(context)
        }
        ReminderScheduler.schedule(context)
    }
}

internal fun formatReminderTime(context: Context, minutes: Int, locale: Locale = Locale.getDefault()): String = LocalTime.of(minutes / 60, minutes % 60)
    .format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale))

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && SipriseStore(context).remindersEnabled()) {
            ReminderScheduler.schedule(context)
        }
    }
}
