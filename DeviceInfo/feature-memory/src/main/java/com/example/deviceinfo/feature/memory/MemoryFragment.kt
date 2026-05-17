package com.example.deviceinfo.feature.memory

import android.app.ActivityManager
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
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.ui.InfoAdapter
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.feature.memory.databinding.FragmentMemoryBinding

class MemoryFragment : Fragment(), ShareableFragment {

    private var _b: FragmentMemoryBinding? = null
    private val b get() = _b!!
    private var latestItems: List<InfoItem> = emptyList()
    private var adapter: InfoAdapter? = null
    private var lastHighRamNotifPct = -1

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (_b == null) return
            refreshLiveData()
            handler.postDelayed(this, 2000L)
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentMemoryBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        loadStaticData()
        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    override fun onResume() { super.onResume(); handler.post(refreshRunnable) }
    override fun onPause()  { super.onPause();  handler.removeCallbacks(refreshRunnable) }

    private fun refreshLiveData() {
        val ctx = context ?: return
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }

        val totalMb  = mi.totalMem / (1024f * 1024f)
        val availMb  = mi.availMem / (1024f * 1024f)
        val usedMb   = totalMb - availMb
        val usedPct  = if (mi.totalMem > 0) ((mi.totalMem - mi.availMem) * 100 / mi.totalMem).toInt() else 0
        val threshMb = mi.threshold / (1024f * 1024f)

        b.memoryLineChart.setTotalRam(totalMb)
        b.memoryLineChart.addDataPoint(usedMb, availMb)
        b.memorySegmentBar.update(usedMb, availMb, totalMb)

        // Alert if RAM > 90%
        if (usedPct >= 90 && lastHighRamNotifPct < 90) {
            lastHighRamNotifPct = usedPct
            sendHighRamNotification(ctx, usedPct)
        }
        if (usedPct < 80) lastHighRamNotifPct = -1

        // Update recycler live rows
        loadStaticData()
    }

    private fun loadStaticData() {
        val ctx = context ?: return
        val am  = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi  = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }

        val totalMb  = mi.totalMem / (1024f * 1024f)
        val availMb  = mi.availMem / (1024f * 1024f)
        val usedMb   = totalMb - availMb
        val usedPct  = if (mi.totalMem > 0) ((mi.totalMem - mi.availMem) * 100 / mi.totalMem).toInt() else 0
        val threshMb = mi.threshold / (1024f * 1024f)
        val ramClassMb = am.memoryClass
        val largeHeapMb = am.largeMemoryClass
        val isLowMem = mi.lowMemory

        fun fmtMb(mb: Float): String = if (mb >= 1024f)
            "${"%.2f".format(mb / 1024f)} GB (${"%.0f".format(mb)} MB)"
        else "${"%.0f".format(mb)} MB"

        val items = mutableListOf<InfoItem>()

        items.add(InfoItem("LIVE RAM", "", true))
        items.add(InfoItem("Total RAM",     fmtMb(totalMb), true))
        items.add(InfoItem("Used RAM",      "${fmtMb(usedMb)} ($usedPct%)", true))
        items.add(InfoItem("Available RAM", fmtMb(availMb)))
        items.add(InfoItem("Low Memory",    if (isLowMem) "⚠️ Yes" else "No"))
        items.add(InfoItem("Low Mem Threshold", fmtMb(threshMb), true))

        items.add(InfoItem("APP MEMORY LIMITS", "", true))
        items.add(InfoItem("App Heap Limit",       "$ramClassMb MB", true))
        items.add(InfoItem("Large Heap (manifest)", "$largeHeapMb MB"))

        // JVM heap info
        val rt = Runtime.getRuntime()
        val jvmMaxMb   = rt.maxMemory() / (1024f * 1024f)
        val jvmTotalMb = rt.totalMemory() / (1024f * 1024f)
        val jvmFreeMb  = rt.freeMemory() / (1024f * 1024f)
        val jvmUsedMb  = jvmTotalMb - jvmFreeMb
        items.add(InfoItem("JVM Max Heap",   "${"%.0f".format(jvmMaxMb)} MB", true))
        items.add(InfoItem("JVM Used Heap",  "${"%.1f".format(jvmUsedMb)} MB"))
        items.add(InfoItem("JVM Free Heap",  "${"%.1f".format(jvmFreeMb)} MB"))

        items.add(InfoItem("DEVICE CAPABILITIES", "", true))
        // RAM speed class heuristic based on total RAM
        val ramTier = when {
            totalMb >= 12 * 1024 -> "High-end (12 GB+)"
            totalMb >= 8  * 1024 -> "Flagship (8 GB)"
            totalMb >= 6  * 1024 -> "Premium (6 GB)"
            totalMb >= 4  * 1024 -> "Mid-range (4 GB)"
            else                 -> "Entry-level (< 4 GB)"
        }
        items.add(InfoItem("RAM Tier", ramTier, true))

        // Swap / zRAM detection (read from /proc/meminfo)
        var swapTotalKb = 0L
        var swapFreeKb  = 0L
        var zramKb = 0L
        try {
            java.io.BufferedReader(java.io.FileReader("/proc/meminfo")).use { reader ->
                reader.lineSequence().forEach { line ->
                    when {
                        line.startsWith("SwapTotal:") -> swapTotalKb = line.split(Regex("\\s+"))[1].toLongOrNull() ?: 0L
                        line.startsWith("SwapFree:")  -> swapFreeKb  = line.split(Regex("\\s+"))[1].toLongOrNull() ?: 0L
                    }
                }
            }
        } catch (_: Exception) {}
        // zRAM block device check
        try {
            val zramFile = java.io.File("/sys/block/zram0/disksize")
            if (zramFile.exists()) zramKb = (zramFile.readText().trim().toLongOrNull() ?: 0L) / 1024L
        } catch (_: Exception) {}

        if (swapTotalKb > 0) {
            val swapUsed = swapTotalKb - swapFreeKb
            items.add(InfoItem("SWAP / ZRAM", "", true))
            items.add(InfoItem("Swap Total", "${"%.0f".format(swapTotalKb / 1024f)} MB", true))
            items.add(InfoItem("Swap Used",  "${"%.0f".format(swapUsed  / 1024f)} MB"))
            items.add(InfoItem("Swap Free",  "${"%.0f".format(swapFreeKb / 1024f)} MB"))
        }
        if (zramKb > 0) {
            items.add(InfoItem("zRAM Size", "${"%.0f".format(zramKb / 1024f)} MB", true))
        }

        items.add(InfoItem("CPU CORES (context)", "", true))
        items.add(InfoItem("Available Processors", Runtime.getRuntime().availableProcessors().toString()))

        latestItems = items
        if (adapter == null) {
            adapter = InfoAdapter(items)
            b.recyclerView.layoutManager = LinearLayoutManager(ctx)
            b.recyclerView.adapter = adapter
        } else {
            adapter = InfoAdapter(items)
            b.recyclerView.adapter = adapter
        }
        val q = b.searchBar.etSearch.text?.toString() ?: ""
        if (q.isNotBlank()) adapter?.filter(q)
    }

    private fun sendHighRamNotification(ctx: Context, usedPct: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("ram_alert_mem", "RAM Alerts",
                NotificationManager.IMPORTANCE_DEFAULT)
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
        val n = NotificationCompat.Builder(ctx, "ram_alert_mem")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💡 High RAM Usage — $usedPct%")
            .setContentText("RAM usage is critical. Close background apps to free memory.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(3001, n) }
        catch (_: SecurityException) {}
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("💾 Memory Info")
        sb.appendLine("─────────────────")
        latestItems.filter { it.value.isNotEmpty() && !it.isHighlighted }
            .forEach { sb.appendLine("${it.label}: ${it.value}") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun getExportData(): Map<String, String> =
        latestItems.filter { it.value.isNotEmpty() }.associate { it.label to it.value }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _b = null
    }
}
