package com.applify.watertracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import java.text.NumberFormat
import java.time.LocalDate

class SipriseWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        updateWidget(context, manager, widgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                update(context)
            }
        }
    }

    companion object {
        const val ACTION_ADD = "com.applify.watertracker.WIDGET_ADD"

        fun update(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SipriseWidget::class.java))
            if (ids.isNotEmpty()) updateWidget(context, manager, ids)
        }
    }
}

private fun updateWidget(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
    val store = SipriseStore(context)
    val goal = store.goal()
    val widgetAmount = store.widgetAmount()
    val total = store.total(LocalDate.now())
    val unit = store.volumeUnit()
    val number = NumberFormat.getIntegerInstance()

    val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val isDark = when (store.themeMode()) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemDark
    }
    val palette = store.colorScheme().palette(isDark)

    val views = RemoteViews(context.packageName, R.layout.siprise_widget).apply {
        setColorStateList(R.id.widget_root, "setBackgroundTintList", ColorStateList.valueOf(palette.card.toArgb()))
        setTextColor(R.id.widget_amount, palette.text.toArgb())
        setTextColor(R.id.widget_goal, palette.muted.toArgb())
        setColorStateList(R.id.widget_add_button, "setBackgroundTintList", ColorStateList.valueOf(palette.primary.toArgb()))
        setTextColor(R.id.widget_add_button, palette.onPrimary.toArgb())
        setColorStateList(R.id.widget_progress, "setProgressTintList", ColorStateList.valueOf(palette.primary.toArgb()))
        setColorStateList(R.id.widget_progress, "setProgressBackgroundTintList", ColorStateList.valueOf(palette.track.toArgb()))

        setTextViewText(R.id.widget_amount, formatVolume(total, unit, number))
        setTextViewText(R.id.widget_goal, "of ${formatVolume(goal, unit, number)}")
        setProgressBar(R.id.widget_progress, goal, total, false)
        setContentDescription(
            R.id.widget_progress,
            context.getString(R.string.widget_progress_description, number.format(total), number.format(goal)),
        )
        setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        setOnClickPendingIntent(
            R.id.widget_add_button,
            PendingIntent.getBroadcast(
                context,
                2,
                Intent(context, SipriseWidgetActionReceiver::class.java).setAction(SipriseWidget.ACTION_ADD),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        setTextViewText(R.id.widget_add_button, "＋ ${formatVolume(widgetAmount, unit, number)}")
        setContentDescription(R.id.widget_add_button, context.getString(R.string.widget_add_description, number.format(widgetAmount)))
    }
    widgetIds.forEach { manager.updateAppWidget(it, views) }
}

class SipriseWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == SipriseWidget.ACTION_ADD) {
            val store = SipriseStore(context)
            store.add(store.widgetAmount())
        }
    }
}
