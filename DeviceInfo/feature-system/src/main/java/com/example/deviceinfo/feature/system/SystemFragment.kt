package com.example.deviceinfo.feature.system

import android.os.Bundle
import android.os.Build
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.feature.system.databinding.FragmentSystemBinding
import java.util.concurrent.TimeUnit

class SystemFragment : Fragment() {

    private var _binding: FragmentSystemBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSystemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val items = buildSystemInfo()
        val adapter = SystemAdapter(items)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun buildSystemInfo(): List<InfoItem> {
        val items = mutableListOf<InfoItem>()
        val uptime = SystemClock.elapsedRealtime()
        val days = TimeUnit.MILLISECONDS.toDays(uptime)
        val hours = TimeUnit.MILLISECONDS.toHours(uptime) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(uptime) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(uptime) % 60

        val pm = requireContext().packageManager
        val googlePlayVersion = try {
            pm.getPackageInfo("com.google.android.gms", 0).versionName ?: "Unknown"
        } catch (e: Exception) { "Unknown" }

        items.add(InfoItem("Android Version", Build.VERSION.RELEASE))
        items.add(InfoItem("API Level", Build.VERSION.SDK_INT.toString()))
        items.add(InfoItem("Security Patch Level", Build.VERSION.SECURITY_PATCH))
        items.add(InfoItem("Bootloader", Build.BOOTLOADER))
        items.add(InfoItem("Build ID", Build.DISPLAY))
        items.add(InfoItem("Java VM", System.getProperty("java.vm.version") ?: "ART"))
        items.add(InfoItem("OpenGL ES", "3.2"))
        items.add(InfoItem("Kernel Architecture", Build.SUPPORTED_ABIS.firstOrNull() ?: "aarch64"))
        items.add(InfoItem("Kernel Version", System.getProperty("os.version") ?: "Unknown"))
        items.add(InfoItem("Root Access", "No"))
        items.add(InfoItem("Google Play Services", googlePlayVersion))
        items.add(InfoItem("System Uptime", "$days days, ${String.format("%02d:%02d:%02d", hours, minutes, seconds)}"))

        return items
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
