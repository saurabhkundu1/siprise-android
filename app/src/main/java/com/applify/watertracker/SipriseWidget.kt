package com.applify.watertracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
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
    val number = NumberFormat.getIntegerInstance()
    val views = RemoteViews(context.packageName, R.layout.siprise_widget).apply {
        setTextViewText(R.id.widget_amount, "${number.format(total)} ml")
        setTextViewText(R.id.widget_goal, "of ${number.format(goal)} ml")
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
        setTextViewText(R.id.widget_add_button, context.getString(R.string.widget_add_amount, number.format(widgetAmount)))
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
