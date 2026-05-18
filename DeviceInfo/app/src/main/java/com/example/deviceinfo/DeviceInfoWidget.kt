package com.cpua.deviceinfo

  import android.app.ActivityManager
  import android.app.PendingIntent
  import android.appwidget.AppWidgetManager
  import android.appwidget.AppWidgetProvider
  import android.content.Context
  import android.content.Intent
  import android.content.IntentFilter
  import android.os.BatteryManager
  import android.widget.RemoteViews

  class DeviceInfoWidget : AppWidgetProvider() {

      override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
          ids.forEach { updateWidget(context, mgr, it) }
      }

      companion object {
          fun updateWidget(context: Context, mgr: AppWidgetManager, widgetId: Int) {
              val views = RemoteViews(context.packageName, R.layout.widget_device_info)

              // Battery
              val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
              val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
              val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
              val pct = if (level >= 0 && scale > 0) level * 100 / scale else 0
              val status = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                  BatteryManager.BATTERY_STATUS_CHARGING    -> "Charging \u26A1"
                  BatteryManager.BATTERY_STATUS_FULL        -> "Full \u2705"
                  BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                  else -> "Unknown"
              }
              views.setTextViewText(R.id.widgetBattery, "$pct%")
              views.setTextViewText(R.id.widgetBatteryStatus, status)

              // RAM
              val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
              val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
              val usedPct = if (mi.totalMem > 0) ((mi.totalMem - mi.availMem) * 100 / mi.totalMem).toInt() else 0
              val totalGb = "%.1f".format(mi.totalMem / 1024f / 1024f / 1024f)
              val usedGb  = "%.1f".format((mi.totalMem - mi.availMem) / 1024f / 1024f / 1024f)
              views.setTextViewText(R.id.widgetRam, "$usedPct%")
              views.setTextViewText(R.id.widgetRamDetail, "$usedGb / $totalGb GB")

              // Tap to open app
              val pi = PendingIntent.getActivity(
                  context, 0,
                  Intent(context, MainActivity::class.java),
                  PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
              )
              views.setOnClickPendingIntent(R.id.widgetTitle, pi)

              mgr.updateAppWidget(widgetId, views)
          }
      }
  }