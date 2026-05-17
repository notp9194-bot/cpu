package com.example.deviceinfo.feature.system

import android.os.*
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.feature.system.databinding.FragmentSystemBinding
import java.util.concurrent.TimeUnit

class SystemFragment : Fragment() {
    private var _b: FragmentSystemBinding? = null
    private val b get() = _b!!
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentSystemBinding.inflate(i, c, false).also { _b = it }.root
    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val up = SystemClock.elapsedRealtime()
        val days = TimeUnit.MILLISECONDS.toDays(up)
        val hrs = TimeUnit.MILLISECONDS.toHours(up) % 24
        val min = TimeUnit.MILLISECONDS.toMinutes(up) % 60
        val sec = TimeUnit.MILLISECONDS.toSeconds(up) % 60
        val gpv = try { requireContext().packageManager.getPackageInfo("com.google.android.gms", 0).versionName ?: "Unknown" } catch (e: Exception) { "Unknown" }
        val items = listOf(
            InfoItem("Android Version", Build.VERSION.RELEASE),
            InfoItem("API Level", Build.VERSION.SDK_INT.toString()),
            InfoItem("Security Patch Level", Build.VERSION.SECURITY_PATCH),
            InfoItem("Bootloader", Build.BOOTLOADER),
            InfoItem("Build ID", Build.DISPLAY),
            InfoItem("Java VM", System.getProperty("java.vm.version") ?: "ART"),
            InfoItem("OpenGL ES", "3.2"),
            InfoItem("Kernel Architecture", Build.SUPPORTED_ABIS.firstOrNull() ?: "aarch64"),
            InfoItem("Kernel Version", System.getProperty("os.version") ?: "Unknown"),
            InfoItem("Root Access", "No"),
            InfoItem("Google Play Services", gpv),
            InfoItem("System Uptime", "$days days, ${String.format("%02d:%02d:%02d", hrs, min, sec)}")
        )
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = SystemAdapter(items)
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
