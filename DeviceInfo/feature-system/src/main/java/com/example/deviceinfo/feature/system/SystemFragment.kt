package com.cpua.deviceinfo.feature.system

import android.app.ActivityManager
import android.content.Context
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cpua.deviceinfo.core.model.InfoItem
import com.cpua.deviceinfo.core.ui.InfoAdapter
import com.cpua.deviceinfo.core.ui.ShareableFragment
import com.cpua.deviceinfo.core.util.ExportUtils
import com.cpua.deviceinfo.feature.system.databinding.FragmentSystemBinding
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SystemFragment : Fragment(), ShareableFragment {
    private var _b: FragmentSystemBinding? = null
    private val b get() = _b!!
    private var latestData: LinkedHashMap<String, String> = linkedMapOf()
    private var adapter: InfoAdapter? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val handler  = Handler(Looper.getMainLooper())

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentSystemBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)

        val up   = SystemClock.elapsedRealtime()
        val days = TimeUnit.MILLISECONDS.toDays(up)
        val hrs  = TimeUnit.MILLISECONDS.toHours(up) % 24
        val min  = TimeUnit.MILLISECONDS.toMinutes(up) % 60
        val sec  = TimeUnit.MILLISECONDS.toSeconds(up) % 60

        val gpv = try {
            requireContext().packageManager.getPackageInfo("com.google.android.gms", 0).versionName ?: "Unknown"
        } catch (e: Exception) { "Unknown" }

        val locale = Locale.getDefault()
        val tz = TimeZone.getDefault()
        val tzOffset = tz.rawOffset / 3600000
        val tzSign = if (tzOffset >= 0) "+" else ""

        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentIme = imm.currentInputMethodSubtype?.languageTag?.takeIf { it.isNotBlank() } ?: locale.language

        val stat = StatFs(Environment.getDataDirectory().path)
        val storageTotalBytes = stat.totalBytes
        val storageFreeBytes  = stat.availableBytes
        val storageUsedBytes  = storageTotalBytes - storageFreeBytes
        val storageTotalGb    = storageTotalBytes / (1024f * 1024f * 1024f)
        val storageUsedGb     = storageUsedBytes  / (1024f * 1024f * 1024f)

        val am = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val ramTotalGb = mi.totalMem / (1024f * 1024f * 1024f)
        val ramUsedGb  = (mi.totalMem - mi.availMem) / (1024f * 1024f * 1024f)

        b.storageDoughnut.update(storageTotalGb, storageUsedGb, ramTotalGb, ramUsedGb)

        latestData = linkedMapOf(
            "Android Version"   to Build.VERSION.RELEASE,
            "API Level"         to Build.VERSION.SDK_INT.toString(),
            "Security Patch"    to Build.VERSION.SECURITY_PATCH,
            "Bootloader"        to Build.BOOTLOADER,
            "Build ID"          to Build.DISPLAY,
            "Build Fingerprint" to Build.FINGERPRINT,
            "Java VM"           to (System.getProperty("java.vm.version") ?: "ART"),
            "Kernel"            to System.getProperty("os.version", "Unknown"),
            "Up Time"           to "${days}d ${hrs}h ${min}m ${sec}s",
            "Language"          to locale.displayLanguage,
            "Region"            to locale.country,
            "Timezone"          to "${tz.id} (UTC${tzSign}${tzOffset})",
            "Keyboard"          to currentIme,
            "Google Play Svc"   to gpv,
            "Storage Total"     to "${"%.1f".format(storageTotalGb)} GB",
            "Storage Used"      to "${"%.1f".format(storageUsedGb)} GB (${"%.0f".format(storageUsedGb / storageTotalGb * 100)}%)",
            "Storage Free"      to "${"%.1f".format(storageFreeBytes / (1024f*1024f*1024f))} GB",
            "RAM Total"         to "${"%.1f".format(ramTotalGb)} GB",
            "RAM Used"          to "${"%.1f".format(ramUsedGb)} GB",
            "RAM Available"     to "${"%.1f".format(mi.availMem / (1024f*1024f*1024f))} GB"
        )

        val items = latestData.entries.mapIndexed { i, (k, v) -> InfoItem(k, v, i % 2 == 0) }
        adapter = InfoAdapter(items, "SYSTEM")
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter

        b.btnExport.setOnClickListener {
            ExportUtils.exportToFile(requireContext(), "System", latestData)
        }

        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, cnt: Int) {}
        })

        loadStorageCategories(storageTotalBytes, storageFreeBytes)
    }

    private fun loadStorageCategories(totalBytes: Long, freeBytes: Long) {
        executor.execute {
            val images    = dirSize(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))   +
                            dirSize(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_SCREENSHOTS)) +
                            dirSize(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))
            val videos    = dirSize(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES))
            val audio     = dirSize(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC))      +
                            dirSize(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS))   +
                            dirSize(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES))
            val downloads = dirSize(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            val docs      = dirSize(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))

            val usedBytes = totalBytes - freeBytes
            val knownDirs = images + videos + audio + downloads + docs
            val apps      = (usedBytes - knownDirs).coerceAtLeast(0L)
            val other     = (usedBytes - apps - images - videos - audio - downloads - docs).coerceAtLeast(0L)

            val cats = buildList {
                if (apps > 0)      add(StorageCategoryBarView.CategoryItem("Apps",      apps,      0xFF7B2FBE.toInt()))
                if (images > 0)    add(StorageCategoryBarView.CategoryItem("Images",    images,    0xFF2196F3.toInt()))
                if (videos > 0)    add(StorageCategoryBarView.CategoryItem("Videos",    videos,    0xFFE91E63.toInt()))
                if (audio > 0)     add(StorageCategoryBarView.CategoryItem("Audio",     audio,     0xFF4CAF50.toInt()))
                if (downloads > 0) add(StorageCategoryBarView.CategoryItem("Downloads", downloads, 0xFFFF9800.toInt()))
                if (docs > 0)      add(StorageCategoryBarView.CategoryItem("Documents", docs,      0xFF009688.toInt()))
                if (other > 0)     add(StorageCategoryBarView.CategoryItem("Other",     other,     0xFF78909C.toInt()))
                add(StorageCategoryBarView.CategoryItem("Free", freeBytes, 0xFFE0E0E0.toInt()))
            }

            handler.post {
                if (_b == null) return@post
                b.storageCategoryBar.setCategories(cats, totalBytes)
            }
        }
    }

    private fun dirSize(dir: File?): Long {
        if (dir == null || !dir.exists() || !dir.isDirectory) return 0L
        var size = 0L
        try {
            val stack = ArrayDeque<File>()
            stack.addLast(dir)
            while (stack.isNotEmpty()) {
                val f = stack.removeLast()
                if (f.isFile) size += f.length()
                else if (f.isDirectory) f.listFiles()?.forEach { stack.addLast(it) }
            }
        } catch (_: Exception) { }
        return size
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("\ud83d\udcf1 System Info")
        sb.appendLine("\u2500".repeat(17))
        latestData.forEach { (k, v) -> sb.appendLine("$k: $v") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun getExportData(): Map<String, String> = latestData

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
