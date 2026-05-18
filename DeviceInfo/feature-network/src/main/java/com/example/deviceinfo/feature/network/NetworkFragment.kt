package com.cpua.deviceinfo.feature.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cpua.deviceinfo.core.model.InfoItem
import com.cpua.deviceinfo.core.ui.InfoAdapter
import com.cpua.deviceinfo.core.ui.ShareableFragment
import com.cpua.deviceinfo.core.util.DeviceUtils
import com.cpua.deviceinfo.feature.network.databinding.FragmentNetworkBinding
import java.net.URL
import java.util.concurrent.Executors

class NetworkFragment : Fragment(), ShareableFragment {
    private var _b: FragmentNetworkBinding? = null
    private val b get() = _b!!
    private val handler = Handler(Looper.getMainLooper())
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var latestData: LinkedHashMap<String, String> = linkedMapOf()
    private var adapter: InfoAdapter? = null
    private val executor = Executors.newSingleThreadExecutor()

    private var publicIp: String = "Fetching\u2026"
    private var publicIpFetched = false

    // TrafficStats for speed measurement
    private var lastRxBytes = TrafficStats.getTotalRxBytes()
    private var lastTxBytes = TrafficStats.getTotalTxBytes()
    private var lastSpeedTs = System.currentTimeMillis()

    private val speedRunnable = object : Runnable {
        override fun run() {
            if (_b == null) return
            val now    = System.currentTimeMillis()
            val rxNow  = TrafficStats.getTotalRxBytes()
            val txNow  = TrafficStats.getTotalTxBytes()
            val elapsedSec = ((now - lastSpeedTs) / 1000f).coerceAtLeast(0.1f)
            val rxKbps = ((rxNow - lastRxBytes) / 1024f / elapsedSec).coerceAtLeast(0f)
            val txKbps = ((txNow - lastTxBytes) / 1024f / elapsedSec).coerceAtLeast(0f)
            lastRxBytes = rxNow; lastTxBytes = txNow; lastSpeedTs = now
            b.networkSpeedChart.addDataPoint(rxKbps, txKbps)
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentNetworkBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        loadData()
        registerNetworkCallback()
        if (!publicIpFetched) fetchPublicIp()
        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, cnt: Int) {}
        })
    }

    override fun onResume() { super.onResume(); handler.post(speedRunnable) }
    override fun onPause()  { super.onPause();  handler.removeCallbacks(speedRunnable) }

    private fun fetchPublicIp() {
        executor.execute {
            val result = try {
                URL("https://api.ipify.org").readText(Charsets.UTF_8).trim()
            } catch (e: Exception) { "Unavailable" }
            publicIp = result; publicIpFetched = true
            handler.post { loadData() }
        }
    }

    private fun registerNetworkCallback() {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handler.post { loadData(); if (!publicIpFetched) fetchPublicIp() }
            }
            override fun onLost(network: Network) {
                handler.post { publicIpFetched = false; publicIp = "Fetching\u2026"; loadData() }
            }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                handler.post { loadData() }
            }
        }
        cm.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback!!)
    }

    private fun loadData() {
        if (_b == null) return
        val networkData = DeviceUtils.getNetworkInfo(requireContext())
        latestData = LinkedHashMap(networkData)
        latestData["Public IP"] = publicIp
        val items = latestData.map { (k, v) ->
            val highlight = k.startsWith("DNS") || k == "WiFi SSID" || k == "Connection Type" || k == "Public IP"
            InfoItem(k, v, highlight)
        }
        val query = b.searchBar.etSearch.text?.toString() ?: ""
        adapter = InfoAdapter(items, "NETWORK")
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter
        if (query.isNotBlank()) adapter?.filter(query)
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("\ud83c\udf10 Network Info")
        sb.appendLine("\u2500".repeat(17))
        latestData.forEach { (k, v) -> sb.appendLine("$k: $v") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun getExportData(): Map<String, String> = latestData

    override fun onDestroyView() {
        try {
            val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            networkCallback?.let { runCatching { cm?.unregisterNetworkCallback(it) } }
        } catch (e: Exception) { /* ignore */ }
        networkCallback = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
        _b = null
    }
}
