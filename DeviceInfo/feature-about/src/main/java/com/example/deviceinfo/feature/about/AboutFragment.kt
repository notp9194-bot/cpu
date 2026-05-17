package com.example.deviceinfo.feature.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.deviceinfo.feature.about.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pm = requireContext().packageManager
        val versionName = try {
            pm.getPackageInfo(requireContext().packageName, 0).versionName
        } catch (e: Exception) { "1.0" }

        binding.tvVersion.text = "Version $versionName"
        binding.tvDescription.text = "DeviceInfo is a free system information app."

        binding.btnOnlineValidation.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cpuid.com/softwares/cpu-z.html")))
        }

        binding.btnSettings.setOnClickListener {
            // Open settings (placeholder)
        }

        binding.btnHelp.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cpuid.com/softwares/cpu-z.html")))
        }

        binding.btnRemoveAds.setOnClickListener {
            // In-app purchase flow placeholder
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
