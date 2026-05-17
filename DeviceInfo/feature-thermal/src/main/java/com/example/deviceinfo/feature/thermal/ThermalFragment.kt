package com.example.deviceinfo.feature.thermal

  import android.app.NotificationChannel
  import android.app.NotificationManager
  import android.content.Context
  import android.os.Build
  import android.os.Bundle
  import android.os.Handler
  import android.os.Looper
  import android.text.Editable
  import android.text.TextWatcher
  import android.view.*
  import androidx.core.app.NotificationCompat
  import androidx.core.app.NotificationManagerCompat
  import androidx.fragment.app.Fragment
  import androidx.recyclerview.widget.LinearLayoutManager
  import com.example.deviceinfo.core.ui.ShareableFragment
  import com.example.deviceinfo.core.util.DeviceUtils
  import com.example.deviceinfo.core.util.ExportUtils
  import com.example.deviceinfo.feature.thermal.databinding.FragmentThermalBinding

  class ThermalFragment : Fragment(), ShareableFragment {
      private var _b: FragmentThermalBinding? = null
      private val b get() = _b!!
      private var latestData: List<Pair<String, Float>> = emptyList()
      private var adapter: ThermalAdapter? = null
      private var lastThermalAlertTemp = -1f

      private val handler = Handler(Looper.getMainLooper())
      private val refreshRunnable = object : Runnable {
          override fun run() {
              if (_b == null) return
              loadData()
              handler.postDelayed(this, 5000L)
          }
      }

      override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
          FragmentThermalBinding.inflate(i, c, false).also { _b = it }.root

      override fun onViewCreated(view: View, s: Bundle?) {
          super.onViewCreated(view, s)
          loadData()

          b.btnExport.setOnClickListener {
              val map = latestData.associate { (k, v) -> k to String.format("%.1f \u00B0C", v) }
              ExportUtils.exportToFile(requireContext(), "Thermal", map)
          }

          b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
              override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
              override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
              override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
          })
      }

      override fun onResume() { super.onResume(); handler.post(refreshRunnable) }
      override fun onPause()  { super.onPause();  handler.removeCallbacks(refreshRunnable) }

      private fun loadData() {
          latestData = DeviceUtils.getThermalInfo()
          val thermalItems = if (latestData.isNotEmpty())
              latestData.map { (n, t) -> ThermalItem(n, t) }
          else listOf(ThermalItem("Thermal", 0f))

          adapter = ThermalAdapter(thermalItems)
          b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
          b.recyclerView.adapter = adapter

          // Check for high temperature alert
          val maxTemp = latestData.maxOfOrNull { it.second } ?: 0f
          checkThermalAlert(maxTemp)
      }

      private fun checkThermalAlert(maxTemp: Float) {
          val ctx = context ?: return
          val threshold = 50f
          if (maxTemp >= threshold && lastThermalAlertTemp < threshold) {
              lastThermalAlertTemp = maxTemp
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                  val ch = NotificationChannel("thermal_alert", "Thermal Alerts", NotificationManager.IMPORTANCE_HIGH)
                      .apply { description = "Alerts when device temperature is too high" }
                  (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                      .createNotificationChannel(ch)
              }
              val n = NotificationCompat.Builder(ctx, "thermal_alert")
                  .setSmallIcon(android.R.drawable.ic_dialog_alert)
                  .setContentTitle("\uD83C\uDF21\uFE0F Device Overheating")
                  .setContentText("Temperature is ${"%.1f".format(maxTemp)}\u00B0C. Let your device cool down.")
                  .setPriority(NotificationCompat.PRIORITY_HIGH)
                  .setAutoCancel(true)
                  .build()
              try { NotificationManagerCompat.from(ctx).notify(3001, n) }
              catch (e: SecurityException) { /* permission not granted */ }
          }
          if (maxTemp < threshold - 5f) lastThermalAlertTemp = -1f // reset
      }

      override fun getShareText(): String {
          val sb = StringBuilder()
          sb.appendLine("\uD83C\uDF21\uFE0F Thermal Info")
          sb.appendLine("─────────────────")
          latestData.forEach { (name, temp) -> sb.appendLine("$name: ${"%.1f".format(temp)} \u00B0C") }
          sb.appendLine("\nShared from CPU-A Device Info app")
          return sb.toString()
      }

      override fun onDestroyView() { super.onDestroyView(); _b = null }
  }