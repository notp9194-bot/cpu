package com.example.deviceinfo

import android.app.ActivityManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.widget.RemoteViews
import com.example.deviceinfo.core.util.DeviceUtils
import java.text.SimpleDateFormat
import java.util.*

class DeviceInfoWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    companion object {
        fun updateWidget(context: Context, awm: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_device_info)

            // Time
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            views.setTextViewText(R.id.widgetTime, time)

            // Battery
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, filter)
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct   = if (level >= 0 && scale > 0) level * 100 / scale else 0
            val isCharging = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ==
                             BatteryManager.BATTERY_STATUS_CHARGING
            views.setTextViewText(R.id.widgetBattery, "$pct%${if (isCharging) " ⚡" else ""}")

            // CPU (max freq of core 0)
            val maxFreq = DeviceUtils.getMaxCpuFreq(0)
            views.setTextViewText(R.id.widgetCpu, if (maxFreq > 0) "$maxFreq MHz" else "N/A")

            // RAM
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            val freeMb = mi.availMem / (1024 * 1024)
            views.setTextViewText(R.id.widgetRam, "$freeMb MB free")

            // Storage
            val sf = StatFs(Environment.getDataDirectory().path)
            val freeGb = sf.availableBytes / (1024 * 1024 * 1024)
            views.setTextViewText(R.id.widgetStorage, "$freeGb GB free")

            // Tap widget → open app
            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(android.R.id.content, pendingIntent)

            awm.updateAppWidget(widgetId, views)
        }

        /** Call this to force-refresh all widgets */
        fun refreshAll(context: Context) {
            val awm = AppWidgetManager.getInstance(context)
            val ids = awm.getAppWidgetIds(ComponentName(context, DeviceInfoWidget::class.java))
            DeviceInfoWidget().onUpdate(context, awm, ids)
        }
    }
}
