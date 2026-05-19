package com.example.deviceinfo.feature.security

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.ui.InfoAdapter
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.feature.security.databinding.FragmentBuildBinding

class BuildFragment : Fragment(), ShareableFragment {

    private var _b: FragmentBuildBinding? = null
    private val b get() = _b!!
    private var latestItems: List<InfoItem> = emptyList()
    private var adapter: InfoAdapter? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentBuildBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        loadData()
        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    private fun loadData() {
        val items = mutableListOf<InfoItem>()

        // ── Security Patch ────────────────────────────────────────────
        val patchDate = Build.VERSION.SECURITY_PATCH   // "yyyy-MM-dd"
        b.securityTimeline.update(patchDate)

        // ── Build Info ────────────────────────────────────────────────
        items.add(InfoItem("BUILD INFO", "", true))
        items.add(InfoItem("Security Patch",    patchDate, true))
        items.add(InfoItem("Android Version",   Build.VERSION.RELEASE, true))
        items.add(InfoItem("API Level",         Build.VERSION.SDK_INT.toString()))
        items.add(InfoItem("Codename",          Build.VERSION.CODENAME))
        items.add(InfoItem("Incremental",       Build.VERSION.INCREMENTAL, true))
        items.add(InfoItem("Build ID",          Build.ID))
        items.add(InfoItem("Display Build",     Build.DISPLAY, true))
        items.add(InfoItem("Build Type",        Build.TYPE))
        items.add(InfoItem("Build Tags",        Build.TAGS))
        items.add(InfoItem("Fingerprint",       Build.FINGERPRINT, true))

        // ── Device Identifiers (non-sensitive) ────────────────────────
        items.add(InfoItem("DEVICE", "", true))
        items.add(InfoItem("Manufacturer",      Build.MANUFACTURER, true))
        items.add(InfoItem("Brand",             Build.BRAND))
        items.add(InfoItem("Model",             Build.MODEL, true))
        items.add(InfoItem("Device",            Build.DEVICE))
        items.add(InfoItem("Product",           Build.PRODUCT, true))
        items.add(InfoItem("Hardware",          Build.HARDWARE))
        items.add(InfoItem("Board",             Build.BOARD, true))
        items.add(InfoItem("Bootloader",        Build.BOOTLOADER))

        // ── CPU ABI ───────────────────────────────────────────────────
        items.add(InfoItem("CPU / ABI", "", true))
        val abis = Build.SUPPORTED_ABIS.joinToString(", ")
        val abis32 = Build.SUPPORTED_32_BIT_ABIS.joinToString(", ").ifBlank { "None" }
        val abis64 = Build.SUPPORTED_64_BIT_ABIS.joinToString(", ").ifBlank { "None" }
        items.add(InfoItem("Supported ABIs",    abis, true))
        items.add(InfoItem("64-bit ABIs",       abis64))
        items.add(InfoItem("32-bit ABIs",       abis32, true))

        // ── Kernel ────────────────────────────────────────────────────
        items.add(InfoItem("KERNEL", "", true))
        val kernelVersion = System.getProperty("os.version") ?: "Unknown"
        val javaVersion   = System.getProperty("java.version") ?: "Unknown"
        val vmVersion     = System.getProperty("java.vm.version") ?: "Unknown"
        val vmName        = System.getProperty("java.vm.name") ?: "Unknown"
        items.add(InfoItem("Kernel Version",    kernelVersion, true))
        items.add(InfoItem("Java VM",           "$vmName $vmVersion"))
        items.add(InfoItem("Java Version",      javaVersion, true))

        // SELinux status
        val selinuxStatus = try {
            val result = Runtime.getRuntime().exec(arrayOf("getenforce"))
                .inputStream.bufferedReader().readLine() ?: "Unknown"
            result
        } catch (_: Exception) { "Unknown" }
        items.add(InfoItem("SELinux", selinuxStatus, true))

        // ── Treble / GSI ──────────────────────────────────────────────
        items.add(InfoItem("SYSTEM ARCHITECTURE", "", true))
        val trebleEnabled = try {
            val p = Runtime.getRuntime().exec(arrayOf("getprop", "ro.treble.enabled"))
            p.inputStream.bufferedReader().readLine()?.trim() == "true"
        } catch (_: Exception) { false }

        val vndk = try {
            Runtime.getRuntime().exec(arrayOf("getprop", "ro.vndk.version"))
                .inputStream.bufferedReader().readLine()?.trim() ?: "N/A"
        } catch (_: Exception) { "N/A" }

        items.add(InfoItem("Project Treble",    if (trebleEnabled) "Enabled ✅" else "Not Detected", true))
        items.add(InfoItem("VNDK Version",      vndk))
        items.add(InfoItem("64-bit System",     if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) "Yes ✅" else "No"))

        // ── Play Protect / Security ───────────────────────────────────
        items.add(InfoItem("SECURITY NOTES", "", true))
        val isDebug     = Build.TYPE == "eng" || Build.TYPE == "userdebug"
        val hasTestKeys = Build.TAGS.contains("test-keys")
        items.add(InfoItem("Build Type",        "${Build.TYPE} ${if (isDebug) "⚠️ (debug)" else "✅"}", true))
        items.add(InfoItem("Test Keys",         if (hasTestKeys) "Yes ⚠️ (unofficial)" else "No ✅"))
        items.add(InfoItem("API Level",         "${Build.VERSION.SDK_INT} (Android ${Build.VERSION.RELEASE})", true))

        val apiNote = when {
            Build.VERSION.SDK_INT >= 34 -> "Android 14+ — Fully supported ✅"
            Build.VERSION.SDK_INT >= 33 -> "Android 13 — Supported ✅"
            Build.VERSION.SDK_INT >= 31 -> "Android 12 — Support ending"
            Build.VERSION.SDK_INT >= 30 -> "Android 11 — Limited support"
            else                        -> "Android < 11 — EOL ⚠️"
        }
        items.add(InfoItem("Support Status",    apiNote))

        latestItems = items
        adapter = InfoAdapter(items, "BUILD")
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("🔐 Build & Security Info")
        sb.appendLine("─────────────────")
        latestItems.filter { it.value.isNotEmpty() && !(it.isHighlighted && it.value.isEmpty()) }
            .forEach { sb.appendLine("${it.label}: ${it.value}") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun getExportData(): Map<String, String> =
        latestItems.filter { it.value.isNotEmpty() }.associate { it.label to it.value }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
