package com.cpua.deviceinfo

  import android.app.NotificationChannel
  import android.app.NotificationManager
  import android.content.BroadcastReceiver
  import android.content.Context
  import android.content.Intent
  import android.os.Build
  import androidx.core.app.NotificationCompat
  import androidx.core.app.NotificationManagerCompat

  class BatteryAlertReceiver : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
          if (intent.action != Intent.ACTION_BATTERY_LOW) return
          createChannel(context)
          val n = NotificationCompat.Builder(context, CHANNEL_ID)
              .setSmallIcon(android.R.drawable.ic_dialog_alert)
              .setContentTitle("\u26A0\uFE0F Battery Low")
              .setContentText("Battery is running low. Please charge your device.")
              .setPriority(NotificationCompat.PRIORITY_HIGH)
              .setAutoCancel(true)
              .build()
          try { NotificationManagerCompat.from(context).notify(NOTIF_ID, n) }
          catch (e: SecurityException) { /* POST_NOTIFICATIONS not granted */ }
      }

      private fun createChannel(context: Context) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              val ch = NotificationChannel(CHANNEL_ID, "Battery Alerts", NotificationManager.IMPORTANCE_HIGH)
                  .apply { description = "Alerts for low and full battery" }
              (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                  .createNotificationChannel(ch)
          }
      }

      companion object {
          const val CHANNEL_ID = "battery_alert"
          const val NOTIF_ID   = 1001
      }
  }