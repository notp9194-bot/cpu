package com.example.deviceinfo.feature.system

import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.ui.InfoAdapter
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.core.util.ExportUtils
import com.example.deviceinfo.feature.system.databinding.FragmentSystemBinding
import java.util.*
import java.util.concurrent.TimeUnit

class SystemFragment : Fragment(), ShareableFragment {
    private var _b: FragmentSystemBinding? = null
    private val b get() = _b!!
    private var latestData: LinkedHashMap<String, String> = linkedMapOf()
    private var adapter: InfoAdapter? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentSystemBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val up  = SystemClock.elapsedRealtime()
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

        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentIme = imm.currentInputMethodSubtype?.languageTag?.takeIf { it.isNotBlank() } ?: locale.language

        latestData = linkedMapOf(
            "Android Version"     to Build.VERSION.RELEASE,
            "API Level"           to Build.VERSION.SDK_INT.toString(),
            "Security Patch"      to Build.VERSION.SECURITY_PATCH,
            "Bootloader"          to Build.BOOTLOADER,
            "Build ID"            to Build.DISPLAY,
            "Build Fingerprint"   to Build.FINGERPRINT,
            "Java VM"             to (System.getProperty("java.vm.version") ?: "ART"),
            "Kernel Architecture" to (Build.SUPPORTED_ABIS.firstOrNull() ?: "aarch64"),
            "Kernel Version"      to (System.getProperty("os.version") ?: "Unknown"),
            "Google Play Svc"     to gpv,
            "System Uptime"       to "$days days ${String.format("%02d:%02d:%02d", hrs, min, sec)}",
            "Language"            to locale.displayLanguage,
            "Region"              to locale.displayCountry,
            "Locale"              to locale.toString(),
            "Timezone"            to tz.id,
            "UTC Offset"          to "UTC$tzSign${tzOffset}:00",
            "Keyboard Language"   to currentIme
        )

        adapter = InfoAdapter(latestData.map { (k, v) -> InfoItem(k, v) })
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
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("🖥️ System Info")
        sb.appendLine("─────────────────")
        latestData.forEach { (k, v) -> sb.appendLine("$k: $v") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
