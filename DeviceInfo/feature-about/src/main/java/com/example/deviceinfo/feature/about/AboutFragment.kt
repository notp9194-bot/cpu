package com.cpua.deviceinfo.feature.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cpua.deviceinfo.core.ui.ShareableFragment
import com.cpua.deviceinfo.core.util.ExportUtils
import com.cpua.deviceinfo.feature.about.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {
    private var _b: FragmentAboutBinding? = null
    private val b get() = _b!!

    // TODO: Replace with your actual hosted Privacy Policy URL before publishing
    private val PRIVACY_POLICY_URL = "https://yourwebsite.com/privacy-policy"

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentAboutBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        try {
            val pi = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            b.tvVersion.text = "Version ${pi.versionName}"
        } catch (e: Exception) {
            b.tvVersion.text = "Version 21.0"
        }

        b.btnExportAll.setOnClickListener { exportAllTabs() }

        // Privacy Policy - mandatory for Play Store compliance
        b.btnPrivacyPolicy?.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Could not open browser", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportAllTabs() {
        val fm = requireActivity().supportFragmentManager
        val allData = linkedMapOf<String, Map<String, String>>()
        val tabNames = listOf(
            "Favorites", "SOC", "Device", "System", "Battery", "Thermal",
            "Sensors", "Network", "Camera", "Audio", "Display", "Codec",
            "Benchmark", "Connectivity", "Power", "GPU", "Memory", "Build",
            "Input", "Health"
        )
        for (pos in tabNames.indices) {
            val fragment = fm.findFragmentByTag("f$pos")
            if (fragment is ShareableFragment) {
                allData[tabNames[pos]] = fragment.getExportData()
            }
        }
        if (allData.isEmpty()) {
            Toast.makeText(requireContext(),
                "Please visit each tab once before exporting all data.", Toast.LENGTH_LONG).show()
            return
        }
        ExportUtils.exportAllToFile(requireContext(), allData)
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
