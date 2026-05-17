package com.example.deviceinfo.feature.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.ui.InfoAdapter
import com.example.deviceinfo.core.util.DeviceUtils
import com.example.deviceinfo.feature.network.databinding.FragmentNetworkBinding

class NetworkFragment : Fragment() {
    private var _b: FragmentNetworkBinding? = null
    private val b get() = _b!!
    private val handler = Handler(Looper.getMainLooper())

    // BUG FIX #2: Use NetworkCallback instead of deprecated CONNECTIVITY_ACTION broadcast
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentNetworkBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        loadData()
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { handler.post { loadData() } }
            override fun onLost(network: Network)      { handler.post { loadData() } }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                handler.post { loadData() }
            }
        }
        val request = NetworkRequest.Builder().build()
        cm.registerNetworkCallback(request, networkCallback!!)
    }

    private fun loadData() {
        if (_b == null) return
        val items = DeviceUtils.getNetworkInfo(requireContext()).map { (k, v) -> InfoItem(k, v) }
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = InfoAdapter(items)
    }

    override fun onDestroyView() {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback?.let { runCatching { cm.unregisterNetworkCallback(it) } }
        networkCallback = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
        _b = null
    }
}
