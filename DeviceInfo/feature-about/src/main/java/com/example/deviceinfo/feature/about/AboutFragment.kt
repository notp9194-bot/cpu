package com.example.deviceinfo.feature.about

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
        try {
            val pi = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            b.tvVersion.text = "Version ${pi.versionName}"
        } catch (e: Exception) {
            b.tvVersion.text = "Version 2.0"
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
