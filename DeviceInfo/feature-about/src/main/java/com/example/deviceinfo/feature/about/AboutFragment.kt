package com.example.deviceinfo.feature.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.example.deviceinfo.feature.about.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {
    private var _b: FragmentAboutBinding? = null
    private val b get() = _b!!
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentAboutBinding.inflate(i, c, false).also { _b = it }.root
    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val ver = try { requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName } catch (e: Exception) { "1.0" }
        b.tvVersion.text = "Version $ver"
        b.btnOnlineValidation.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cpuid.com"))) }
        b.btnHelp.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cpuid.com"))) }
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
