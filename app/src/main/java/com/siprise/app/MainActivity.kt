package com.siprise.app

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

private val Ink = Color(0xFF163C39)
private val Teal = Color(0xFF087F72)
private val Mint = Color(0xFFE4F4ED)
private val Page = Color(0xFFF6F8F5)
private val Muted = Color(0xFF71817D)

data class DrinkLog(val amount: Int, val time: Long)

class SipriseStore(context: Context) {
    private val prefs = context.getSharedPreferences("siprise", Context.MODE_PRIVATE)

    fun goal() = prefs.getInt("goal", 2000).coerceIn(500, 6000)
    fun setGoal(value: Int) = prefs.edit().putInt("goal", value.coerceIn(500, 6000)).apply()
    fun remindersEnabled() = prefs.getBoolean("reminders", false)
    fun setRemindersEnabled(value: Boolean) = prefs.edit().putBoolean("reminders", value).apply()
    fun interval() = prefs.getInt("interval", 60).takeIf { it in listOf(60, 90, 120) } ?: 60

    fun setInterval(value: Int) {
        require(value in listOf(60, 90, 120))
        prefs.edit().putInt("interval", value).apply()
    }

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
        prefs.edit().putString("logs_${LocalDate.now()}", updated.toString()).apply()
    }

    fun total(date: LocalDate) = logs(date).sumOf { it.amount }
}

class MainActivity : AppCompatActivity() {
    private val store by lazy { SipriseStore(this) }
    private var reminders by mutableStateOf(false)
    private var notificationDenied by mutableStateOf(false)
    private var permissionPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reminders = store.remindersEnabled()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContentView(ComposeView(this).apply {
            setContent {
                SipriseScreen(
                    store = store,
                    remindersEnabled = reminders,
                    notificationDenied = notificationDenied,
                    onReminderChange = ::changeReminders,
                )
            }
        })
    }

    private fun changeReminders(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= 33 &&
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
            notificationDenied = !granted
            applyReminderSetting(granted)
        }
    }
}

@Composable
private fun SipriseScreen(
    store: SipriseStore,
    remindersEnabled: Boolean,
    notificationDenied: Boolean,
    onReminderChange: (Boolean) -> Unit,
) {
    var logs by remember { mutableStateOf(store.logs()) }
    var goal by remember { mutableIntStateOf(store.goal()) }
    var interval by remember { mutableIntStateOf(store.interval()) }
    val total = logs.sumOf { it.amount }
    val number = remember { NumberFormat.getIntegerInstance() }
    val context = LocalContext.current

    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, onPrimary = Color.White, background = Page, surface = Color.White)) {
        Column(
            Modifier.fillMaxSize().background(Page).windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
                    Text("✦", color = Teal, fontSize = 21.sp)
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text("Siprise", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())), color = Muted, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Text("💧", fontSize = 22.sp)
            }

            Spacer(Modifier.height(26.dp))
            Text("A little sip, a brighter day.", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("Small sips add up. Keep your rhythm going.", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 5.dp))

            Spacer(Modifier.height(20.dp))
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("TODAY'S HYDRATION", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                        Spacer(Modifier.weight(1f))
                        Text("${number.format((total * 100 / goal).coerceAtMost(100))}%", color = Teal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(190.dp)) {
                        val fraction = (total.toFloat() / goal).coerceIn(0f, 1f)
                        Canvas(Modifier.fillMaxSize()) {
                            val stroke = 15.dp.toPx()
                            drawArc(Color(0xFFEAF0EC), -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round), size = Size(size.width, size.height))
                            if (fraction > 0f) drawArc(Teal, -90f, 360f * fraction, false, style = Stroke(stroke, cap = StrokeCap.Round), size = Size(size.width, size.height))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${number.format(total)}", color = Ink, fontSize = 35.sp, fontWeight = FontWeight.Bold)
                            Text("ml so far", color = Muted, fontSize = 13.sp)
                            Text("of ${number.format(goal)} ml", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Daily goal", color = Muted, fontSize = 13.sp)
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { goal = (goal - 250).coerceAtLeast(500); store.setGoal(goal) }) { Text("−", fontSize = 20.sp) }
                        Text("${number.format(goal)} ml", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        TextButton(onClick = { goal = (goal + 250).coerceAtMost(6000); store.setGoal(goal) }) { Text("+", fontSize = 20.sp) }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { store.add(250); logs = store.logs() },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
            ) {
                Text("＋   Log a glass  ·  250 ml", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAdd("+150 ml") { store.add(150); logs = store.logs() }
                QuickAdd("+500 ml") { store.add(500); logs = store.logs() }
            }

            Spacer(Modifier.height(25.dp))
            Text("This week", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Your sips, day by day", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp, bottom = 12.dp))
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(
                    Modifier.fillMaxWidth().height(135.dp).padding(horizontal = 12.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    (6 downTo 0).forEach { daysAgo ->
                        val date = LocalDate.now().minusDays(daysAgo.toLong())
                        val amount = if (daysAgo == 0) total else store.total(date)
                        val fill = (amount.toFloat() / goal).coerceIn(0f, 1f)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Box(Modifier.width(20.dp).height(70.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEAF0EC)), contentAlignment = Alignment.BottomCenter) {
                                Box(Modifier.fillMaxWidth().fillMaxHeight(fill).clip(RoundedCornerShape(12.dp)).background(if (daysAgo == 0) Teal else Color(0xFF74BEB0)))
                            }
                            Spacer(Modifier.height(7.dp))
                            Text(date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), color = if (daysAgo == 0) Teal else Muted, fontSize = 12.sp, fontWeight = if (daysAgo == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            Spacer(Modifier.height(25.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Gentle reminders", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("A quiet nudge while you're awake", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
                }
                Spacer(Modifier.weight(1f))
                Switch(checked = remindersEnabled, onCheckedChange = onReminderChange)
            }
            if (notificationDenied) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Allow notifications to receive reminders.", color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    }) { Text("Open settings") }
                }
            }
            if (remindersEnabled) {
                Spacer(Modifier.height(12.dp))
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Remind me every", color = Muted, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(60, 90, 120).forEach { minutes ->
                                val selected = interval == minutes
                                Button(
                                    onClick = {
                                        interval = minutes
                                        store.setInterval(minutes)
                                        if (remindersEnabled) ReminderScheduler.schedule(context)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (selected) Teal else Mint, contentColor = if (selected) Color.White else Ink),
                                    shape = RoundedCornerShape(13.dp),
                                    modifier = Modifier.weight(1f),
                                ) { Text(if (minutes == 60) "1 hour" else if (minutes == 90) "1.5 hours" else "2 hours", fontSize = 12.sp) }
                            }
                        }
                        Text("Reminders run from 9 am to 9 pm.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }

            if (logs.isNotEmpty()) {
                Spacer(Modifier.height(25.dp))
                Text("Recent sips", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        logs.takeLast(4).asReversed().forEachIndexed { index, log ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(34.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) { Text("◌", color = Teal, fontSize = 20.sp) }
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text("Water", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).format(java.time.Instant.ofEpochMilli(log.time).atZone(java.time.ZoneId.systemDefault())), color = Muted, fontSize = 12.sp)
                                }
                                Text("+${log.amount} ml", color = Teal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (index < logs.takeLast(4).size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF0F3F1)))
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Text("Sip at your own pace. Your log is saved locally.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp))
        }
    }
}

@Composable
private fun RowScope.QuickAdd(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink),
    ) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
}

private object ReminderScheduler {
    private const val CHANNEL_ID = "siprise_water_reminders"

    fun schedule(context: Context) {
        val it = SipriseStore(context).interval()
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context)
        val next = Calendar.getInstance().apply { add(Calendar.MINUTE, it) }
        when {
            next.get(Calendar.HOUR_OF_DAY) >= 21 -> {
                next.add(Calendar.DAY_OF_YEAR, 1)
                next.set(Calendar.HOUR_OF_DAY, 9)
                next.set(Calendar.MINUTE, 0)
            }
            next.get(Calendar.HOUR_OF_DAY) < 9 -> {
                next.set(Calendar.HOUR_OF_DAY, 9)
                next.set(Calendar.MINUTE, 0)
            }
        }
        next.set(Calendar.SECOND, 0)
        next.set(Calendar.MILLISECOND, 0)
        // shortcut: Android may defer these during Doze; use exact alarms only if minute-level timing is needed.
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
    }

    fun cancel(context: Context) = context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        1,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun postNotification(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Water reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Gentle reminders to take a sip of water"
            },
        )
        try {
            val notification = Notification.Builder(context, CHANNEL_ID)
            manager.notify(
                1,
                notification.setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Time for a little sip")
                    .setContentText("Take a moment to enjoy some water.")
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
        if (!SipriseStore(context).remindersEnabled()) return
        val now = LocalTime.now()
        if (now.hour in 9..20) ReminderScheduler.postNotification(context)
        ReminderScheduler.schedule(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && SipriseStore(context).remindersEnabled()) {
            ReminderScheduler.schedule(context)
        }
    }
}
