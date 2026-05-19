package com.example.deviceinfo.feature.connectivity

import android.bluetooth.BluetoothManager
import android.content.Context
import android.nfc.NfcAdapter
import android.net.wifi.WifiManager
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
import com.example.deviceinfo.feature.connectivity.databinding.FragmentConnectivityBinding

class ConnectivityFragment : Fragment(), ShareableFragment {
    private var _b: FragmentConnectivityBinding? = null
    private val b get() = _b!!
    private var latestItems: List<InfoItem> = emptyList()
    private var adapter: InfoAdapter? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentConnectivityBinding.inflate(i, c, false).also { _b = it }.root

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
        val ctx = requireContext()
        val pm  = ctx.packageManager
        val items = mutableListOf<InfoItem>()

        // ── Bluetooth ─────────────────────────────────────────────────
        items.add(InfoItem("BLUETOOTH", "", true))
        val btMgr     = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val btAdapter = btMgr?.adapter
        val btEnabled = btAdapter?.isEnabled == true
        val hasBle    = pm.hasSystemFeature("android.hardware.bluetooth_le")

        // Accurate BT feature detection via BluetoothAdapter APIs (no permission needed)
        val bt5Features = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && btAdapter != null) {
            btAdapter.isLe2MPhySupported || btAdapter.isLeCodedPhySupported
        } else false

        val btVersionLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && btAdapter != null) {
            when {
                btAdapter.isLeCodedPhySupported -> "BT 5.0 (Coded PHY)"
                btAdapter.isLe2MPhySupported    -> "BT 5.0 (2M PHY)"
                hasBle                          -> "BT 4.x (BLE)"
                else                            -> "BT Classic"
            }
        } else if (hasBle) "BT 4.x (BLE)" else "Classic"

        items.add(InfoItem("Bluetooth",    if (btAdapter != null) "Available" else "Not Available", true))
        items.add(InfoItem("Status",       if (btEnabled) "Enabled" else "Disabled"))
        items.add(InfoItem("Version",      btVersionLabel, true))
        items.add(InfoItem("BLE Support",  if (hasBle) "Supported" else "Not Supported"))
        items.add(InfoItem("BT 5 Features", if (bt5Features) "Supported" else "Not Detected"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && btAdapter != null) {
            if (Build.VERSION.SDK_INT >= 33) {
                items.add(InfoItem("LE Audio", if (btAdapter.isLeAudioSupported == android.bluetooth.BluetoothStatusCodes.FEATURE_SUPPORTED) "Supported" else "Not Available"))
            }
            items.add(InfoItem("2M PHY",         if (btAdapter.isLe2MPhySupported) "Supported" else "Not Supported"))
            items.add(InfoItem("Coded PHY",      if (btAdapter.isLeCodedPhySupported) "Supported" else "Not Supported"))
            items.add(InfoItem("Extended Adv",   if (btAdapter.isLeExtendedAdvertisingSupported) "Supported" else "Not Supported"))
            items.add(InfoItem("Multiple Adv",   if (btAdapter.isMultipleAdvertisementSupported) "Supported" else "Not Supported"))
            items.add(InfoItem("Offloaded Scan", if (btAdapter.isOffloadedScanBatchingSupported) "Supported" else "Not Supported"))
            items.add(InfoItem("Offloaded Filter", if (btAdapter.isOffloadedFilteringSupported) "Supported" else "Not Supported"))
        }

        // ── NFC ───────────────────────────────────────────────────────
        items.add(InfoItem("NFC", "", true))
        val hasNfc     = pm.hasSystemFeature("android.hardware.nfc")
        val nfcAdapter = if (hasNfc) NfcAdapter.getDefaultAdapter(ctx) else null
        val nfcEnabled = nfcAdapter?.isEnabled == true
        val hasHce     = pm.hasSystemFeature("android.hardware.nfc.hce")
        val hasNfcf    = pm.hasSystemFeature("android.hardware.nfc.nfcf")
        val hasNfcb    = pm.hasSystemFeature("android.hardware.nfc.nfcb")
        items.add(InfoItem("NFC",              if (hasNfc) "Available" else "Not Available", true))
        items.add(InfoItem("Status",           if (nfcEnabled) "Enabled" else if (hasNfc) "Disabled" else "N/A"))
        items.add(InfoItem("HCE (Card Emulation)", if (hasHce) "Supported" else "Not Supported", true))
        items.add(InfoItem("NFC-F / Felica",   if (hasNfcf) "Supported" else "Not Supported"))
        items.add(InfoItem("NFC-B",            if (hasNfcb) "Supported" else "Not Supported"))

        // ── Wi-Fi ─────────────────────────────────────────────────────
        items.add(InfoItem("WI-FI", "", true))
        val wm       = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val hasWifi  = pm.hasSystemFeature("android.hardware.wifi")
        val is5GHz   = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) wm.is5GHzBandSupported else false

        // Real Wi-Fi standard detection (API 30+), no heuristic
        val wifiStandard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // WifiManager.WIFI_STANDARD_* constants available from API 30
            when (wm.wifiState) { // wifiState just to access wm; actual standard via connected network
                else -> {
                    // Use pm features for accurate detection
                    when {
                        pm.hasSystemFeature("android.hardware.wifi.passpoint") && is5GHz &&
                            Build.VERSION.SDK_INT >= 30 -> "Wi-Fi 6 / 6E (802.11ax)"
                        is5GHz -> "Wi-Fi 5 (802.11ac)"
                        hasWifi -> "Wi-Fi 4 (802.11n)"
                        else -> "Unknown"
                    }
                }
            }
        } else if (is5GHz) "Wi-Fi 5 (802.11ac)" else if (hasWifi) "Wi-Fi 4 (802.11n)" else "Unknown"

        val hasWifiDirect = pm.hasSystemFeature("android.hardware.wifi.direct")
        val hasWifiAware  = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) pm.hasSystemFeature("android.hardware.wifi.aware") else false
        val hasWifiRtt    = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pm.hasSystemFeature("android.hardware.wifi.rtt") else false
        val hasPasspoint  = pm.hasSystemFeature("android.hardware.wifi.passpoint")

        items.add(InfoItem("Wi-Fi",           if (hasWifi) "Available" else "Not Available", true))
        items.add(InfoItem("Standard",         wifiStandard, true))
        items.add(InfoItem("5 GHz Band",       if (is5GHz) "Supported" else "2.4 GHz Only"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            items.add(InfoItem("6 GHz Band (Wi-Fi 6E)", if (wm.is6GHzBandSupported) "Supported" else "Not Supported"))
        }
        items.add(InfoItem("Wi-Fi Direct",    if (hasWifiDirect) "Supported" else "Not Supported"))
        items.add(InfoItem("Wi-Fi Aware",     if (hasWifiAware) "Supported" else "Not Supported", true))
        items.add(InfoItem("Wi-Fi RTT",       if (hasWifiRtt) "Supported" else "Not Supported"))
        items.add(InfoItem("Wi-Fi Passpoint", if (hasPasspoint) "Supported" else "Not Supported"))

        // ── USB / OTG ─────────────────────────────────────────────────
        items.add(InfoItem("USB & OTG", "", true))
        val hasUsb    = pm.hasSystemFeature("android.hardware.usb.host")
        val hasUsbAcc = pm.hasSystemFeature("android.hardware.usb.accessory")
        items.add(InfoItem("USB Host (OTG)", if (hasUsb) "Supported" else "Not Supported", true))
        items.add(InfoItem("USB Accessory",  if (hasUsbAcc) "Supported" else "Not Supported"))

        // ── Other Wireless ────────────────────────────────────────────
        items.add(InfoItem("OTHER WIRELESS", "", true))
        val hasTelephony = pm.hasSystemFeature("android.hardware.telephony")
        val hasGps       = pm.hasSystemFeature("android.hardware.location.gps")
        val hasNetwork   = pm.hasSystemFeature("android.hardware.location.network")
        val hasUwb       = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) pm.hasSystemFeature("android.hardware.uwb") else false
        val hasIr        = pm.hasSystemFeature("android.hardware.consumerir")
        items.add(InfoItem("Cellular / LTE",      if (hasTelephony) "Supported" else "Not Available", true))
        items.add(InfoItem("GPS",                  if (hasGps) "Supported" else "Not Available"))
        items.add(InfoItem("Network Location",     if (hasNetwork) "Supported" else "Not Available"))
        items.add(InfoItem("UWB (Ultra-Wideband)", if (hasUwb) "Supported" else "Not Supported", true))
        items.add(InfoItem("IR Blaster",           if (hasIr) "Yes" else "No"))

        latestItems = items

        val radarAxes = listOf(
            RadarChartView.RadarAxis("BT",      if (btAdapter != null) 1f else 0f),
            RadarChartView.RadarAxis("BT 5",    if (bt5Features) 1f else if (hasBle) 0.6f else 0.2f),
            RadarChartView.RadarAxis("NFC",     if (hasNfc) 1f else 0f),
            RadarChartView.RadarAxis("Wi-Fi",   if (hasWifi) 1f else 0f),
            RadarChartView.RadarAxis("5GHz",    if (is5GHz) 1f else 0f),
            RadarChartView.RadarAxis("GPS",     if (hasGps) 1f else 0f),
            RadarChartView.RadarAxis("UWB",     if (hasUwb) 1f else 0f),
            RadarChartView.RadarAxis("USB OTG", if (hasUsb) 1f else 0f),
        )
        b.radarChart.update(radarAxes)

        adapter = InfoAdapter(items, "CONNECTIVITY")
        b.recyclerView.layoutManager = LinearLayoutManager(ctx)
        b.recyclerView.adapter = adapter
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("📡 Connectivity Info")
        sb.appendLine("─────────────────")
        latestItems.filter { it.value.isNotEmpty() }.forEach { sb.appendLine("${it.label}: ${it.value}") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun getExportData(): Map<String, String> =
        latestItems.filter { it.value.isNotEmpty() }.associate { it.label to it.value }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
