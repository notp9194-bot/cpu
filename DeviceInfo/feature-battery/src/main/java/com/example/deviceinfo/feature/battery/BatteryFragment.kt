package com.example.deviceinfo.feature.battery

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.ui.InfoAdapter
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.core.util.DeviceUtils
import com.example.deviceinfo.core.util.ExportUtils
import com.example.deviceinfo.feature.battery.databinding.FragmentBatteryBinding

class BatteryFragment : Fragment(), ShareableFragment {
    private var _b: FragmentBatteryBinding? = null
    private val b get() = _b!!
    private var receiver: BroadcastReceiver? = null
    private var latestData: Map<String, String> = emptyMap()
    private var adapter: InfoAdapter? = null
    private var lastBatteryFullNotifPct = -1
    private var lastRamAlertPct = -1

    private val handler = Handler(Looper.getMainLooper())
    private val ramRunnable = object : Runnable {
        override fun run() {
            if (_b == null) return
            val am = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            val usedPct = if (mi.totalMem > 0)
                ((mi.totalMem - mi.availMem) * 100 / mi.totalMem).toInt()
            else 0
            b.ramChart.addDataPoint(usedPct)
            checkRamAlert(usedPct)
            handler.postDelayed(this, 2000L)
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentBatteryBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        loadData()

        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                loadData()
                checkBatteryFullAlert()
            }
        }
        requireContext().registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        b.btnExport.setOnClickListener {
            ExportUtils.exportToFile(requireContext(), "Battery", latestData)
        }

        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, cnt: Int) {}
        })
    }

    override fun onResume() { super.onResume(); handler.post(ramRunnable) }
    override fun onPause()  { super.onPause();  handler.removeCallbacks(ramRunnable) }

    private fun checkBatteryFullAlert() {
        val ctx = context ?: return
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = ctx.registerReceiver(null, filter)
        val level  = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale  = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct    = if (level >= 0 && scale > 0) level * 100 / scale else 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        if (pct >= 100 &&
            (status == BatteryManager.BATTERY_STATUS_FULL || status == BatteryManager.BATTERY_STATUS_CHARGING) &&
            lastBatteryFullNotifPct != 100
        ) {
            lastBatteryFullNotifPct = 100
            sendNotification(ctx,
                channelId = "battery_full",
                channelName = "Battery Full",
                notifId = 2001,
                title = "\u26A1 Battery Full — 100%",
                text = "Battery is fully charged. You can unplug the charger."
            )
        }
        if (pct < 95) lastBatteryFullNotifPct = -1
    }

    private fun checkRamAlert(usedPct: Int) {
        val ctx = context ?: return
        if (usedPct >= 90 && lastRamAlertPct < 90) {
            lastRamAlertPct = usedPct
            sendNotification(ctx,
                channelId = "ram_alert",
                channelName = "RAM Alerts",
                notifId = 2002,
                title = "\uD83D\uDCA1 High RAM Usage",
                text = "RAM usage is $usedPct%. Consider closing background apps."
            )
        }
        if (usedPct < 80) lastRamAlertPct = -1
    }

    private fun sendNotification(ctx: Context, channelId: String, channelName: String,
                                  notifId: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
        val n = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(notifId, n) }
        catch (e: SecurityException) { /* permission not granted */ }
    }

    private fun loadData() {
        val ctx = context ?: return
        latestData = DeviceUtils.getBatteryInfo(ctx)
        val items = latestData.entries.mapIndexed { i, (k, v) ->
            InfoItem(k, v, i % 2 == 0)
        }
        if (adapter == null) {
            adapter = InfoAdapter(items)
            b.recyclerView.layoutManager = LinearLayoutManager(ctx)
            b.recyclerView.adapter = adapter
        } else {
            adapter = InfoAdapter(items)
            b.recyclerView.adapter = adapter
        }

        // Update battery line chart
        latestData.entries.firstOrNull { it.key == "Level" }?.value?.let { level ->
            val pct = level.replace("%","").trim().toIntOrNull() ?: 0
            b.batteryChart.addDataPoint(pct)

            // Update doughnut with live percent + charging status
            val isCharging = latestData["Status"]?.contains("Charging", ignoreCase = true) == true ||
                             latestData["Status"]?.contains("Full", ignoreCase = true) == true
            b.batteryDoughnut.update(pct, isCharging)
        }
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("\uD83D\uDD0B Battery Info")
        sb.appendLine("─────────────────")
        latestData.forEach { (k, v) -> sb.appendLine("$k: $v") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        receiver?.let { ctx -> requireContext().unregisterReceiver(ctx) }
        _b = null
    }
}
