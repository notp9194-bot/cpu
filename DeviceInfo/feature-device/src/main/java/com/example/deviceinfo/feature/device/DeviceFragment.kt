package com.example.deviceinfo.feature.device

import android.os.Bundle
import android.os.Build
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.feature.device.databinding.FragmentDeviceBinding

class DeviceFragment : Fragment() {

    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val items = buildDeviceInfo()
        val adapter = DeviceAdapter(items)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun buildDeviceInfo(): List<InfoItem> {
        val items = mutableListOf<InfoItem>()
        val metrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(metrics)

        val activityManager = requireContext().getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem / (1024 * 1024)
        val availRam = memInfo.availMem / (1024 * 1024)
        val usedPct = 100 - (availRam * 100 / totalRam)

        val statFs = StatFs(android.os.Environment.getDataDirectory().path)
        val totalStorage = statFs.totalBytes / (1024 * 1024 * 1024)
        val availStorage = statFs.availableBytes / (1024 * 1024 * 1024)
        val usedStoragePct = 100 - (availStorage * 100 / totalStorage)

        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val dpi = metrics.densityDpi
        val xdpi = metrics.xdpi
        val screenInches = Math.sqrt(
            Math.pow((screenWidth / xdpi).toDouble(), 2.0) +
            Math.pow((screenHeight / metrics.ydpi).toDouble(), 2.0)
        )

        items.add(InfoItem("Model", "${Build.MODEL} (${Build.DEVICE})"))
        items.add(InfoItem("Manufacturer", Build.MANUFACTURER))
        items.add(InfoItem("Board", Build.BOARD))
        items.add(InfoItem("Hardware", Build.HARDWARE))
        items.add(InfoItem("Screen Size", String.format("%.2f inches", screenInches)))
        items.add(InfoItem("Screen Resolution", "${screenWidth} x ${screenHeight} pixels"))
        items.add(InfoItem("Screen Density", "$dpi dpi"))
        items.add(InfoItem("Total RAM", "$totalRam MB"))
        items.add(InfoItem("Available RAM", "$availRam MB ($usedPct%)", true))
        items.add(InfoItem("Internal Storage", "$totalStorage GB"))
        items.add(InfoItem("Available Storage", "$availStorage GB ($usedStoragePct%)", true))

        return items
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
