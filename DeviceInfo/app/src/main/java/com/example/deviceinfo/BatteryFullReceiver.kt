package com.example.deviceinfo

  import android.app.NotificationChannel
  import android.app.NotificationManager
  import android.content.BroadcastReceiver
  import android.content.Context
  import android.content.Intent
  import android.content.IntentFilter
  import android.os.BatteryManager
  import android.os.Build
  import androidx.core.app.NotificationCompat
  import androidx.core.app.NotificationManagerCompat

  class BatteryFullReceiver : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
          if (intent.action != Intent.ACTION_POWER_CONNECTED) return
          // Check current battery level
          val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
          val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
          val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
          val pct = if (level >= 0 && scale > 0) level * 100 / scale else 0
          val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

          // Only notify when 100% and still charging
          if (pct >= 100 && status == BatteryManager.BATTERY_STATUS_FULL) {
              createChannel(context)
              val n = NotificationCompat.Builder(context, CHANNEL_ID)
                  .setSmallIcon(android.R.drawable.ic_menu_compass)
                  .setContentTitle("\u26A1 Battery Full")
                  .setContentText("Battery is fully charged (100%). You can unplug now.")
                  .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                  .setAutoCancel(true)
                  .build()
              try { NotificationManagerCompat.from(context).notify(NOTIF_ID, n) }
              catch (e: SecurityException) { /* POST_NOTIFICATIONS not granted */ }
          }
      }

      private fun createChannel(context: Context) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              val ch = NotificationChannel(CHANNEL_ID, "Battery Full Alert", NotificationManager.IMPORTANCE_DEFAULT)
                  .apply { description = "Notifies when battery is fully charged" }
              (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                  .createNotificationChannel(ch)
          }
      }

      companion object {
          const val CHANNEL_ID = "battery_full_alert"
          const val NOTIF_ID   = 1002
      }
  }