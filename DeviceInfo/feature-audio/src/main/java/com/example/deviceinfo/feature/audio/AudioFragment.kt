package com.example.deviceinfo.feature.audio

import android.bluetooth.BluetoothManager
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
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
import com.example.deviceinfo.feature.audio.databinding.FragmentAudioBinding

class AudioFragment : Fragment(), ShareableFragment {
    private var _b: FragmentAudioBinding? = null
    private val b get() = _b!!
    private var latestItems: List<InfoItem> = emptyList()
    private var adapter: InfoAdapter? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentAudioBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val items = buildAudioInfo()
        latestItems = items
        adapter = InfoAdapter(items)
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter

        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, cnt: Int) {}
        })
    }

    private fun buildAudioInfo(): List<InfoItem> {
        val items = mutableListOf<InfoItem>()
        val am = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // ── Output ──────────────────────────────────────────────
        items.add(InfoItem("── Output ──", "", true))

        val isSpeakerOn = am.isSpeakerphoneOn
        items.add(InfoItem("Speakerphone", if (isSpeakerOn) "On" else "Off"))

        val isWiredHeadset = am.isWiredHeadsetOn
        items.add(InfoItem("Wired Headset", if (isWiredHeadset) "Connected" else "Not Connected"))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            items.add(InfoItem("Fixed Volume",  if (am.isVolumeFixed) "Yes" else "No"))
        }

        // Volume levels (no permission needed — just reading state)
        val streams = listOf(
            AudioManager.STREAM_MUSIC   to "Media Volume",
            AudioManager.STREAM_RING    to "Ring Volume",
            AudioManager.STREAM_ALARM   to "Alarm Volume",
            AudioManager.STREAM_NOTIFICATION to "Notification Volume"
        )
        streams.forEach { (stream, label) ->
            val cur = am.getStreamVolume(stream)
            val max = am.getStreamMaxVolume(stream)
            items.add(InfoItem(label, "$cur / $max"))
        }

        // ── Bluetooth Audio ──────────────────────────────────────
        items.add(InfoItem("── Bluetooth Audio ──", "", true))
        val btManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val btAdapter = btManager?.adapter
        if (btAdapter != null) {
            items.add(InfoItem("Bluetooth",     if (btAdapter.isEnabled) "Enabled" else "Disabled"))
            items.add(InfoItem("BLE Support",   if (requireContext().packageManager.hasSystemFeature("android.hardware.bluetooth_le")) "Yes" else "No"))
        } else {
            items.add(InfoItem("Bluetooth", "Not available"))
        }
        items.add(InfoItem("Bluetooth SCO", if (am.isBluetoothScoOn) "Active" else "Inactive"))
        items.add(InfoItem("BT A2DP",       if (am.isBluetoothA2dpOn) "Connected" else "Not Connected"))

        // ── Codec / Format Support ──────────────────────────────
        items.add(InfoItem("── Audio Formats ──", "", true))

        // Check supported audio encodings
        val encodings = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val encodingMap = mapOf(
                AudioFormat.ENCODING_PCM_16BIT  to "PCM 16-bit",
                AudioFormat.ENCODING_PCM_8BIT   to "PCM 8-bit",
                AudioFormat.ENCODING_PCM_FLOAT  to "PCM Float",
                AudioFormat.ENCODING_AC3        to "Dolby AC3",
                AudioFormat.ENCODING_E_AC3      to "Dolby E-AC3",
                AudioFormat.ENCODING_DTS        to "DTS",
                AudioFormat.ENCODING_DTS_HD     to "DTS-HD",
                AudioFormat.ENCODING_MP3        to "MP3",
                AudioFormat.ENCODING_AAC_LC     to "AAC-LC",
                AudioFormat.ENCODING_AAC_HE_V1  to "AAC-HE v1",
                AudioFormat.ENCODING_AAC_HE_V2  to "AAC-HE v2",
            )
            encodingMap.forEach { (encoding, name) ->
                if (AudioFormat.isEncodingLinearPcm(encoding) ||
                    try { AudioFormat.Builder().setEncoding(encoding); true } catch (e: Exception) { false }) {
                    encodings.add(name)
                }
            }
        }
        items.add(InfoItem("Supported Encodings",
            if (encodings.isNotEmpty()) encodings.joinToString(", ") else "PCM 16-bit, AAC"))

        // ── Microphone ─────────────────────────────────────────
        items.add(InfoItem("── Microphone ──", "", true))
        val hasMic = requireContext().packageManager.hasSystemFeature("android.hardware.microphone")
        items.add(InfoItem("Microphone", if (hasMic) "Available" else "Not Available"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val mics = am.microphones
            items.add(InfoItem("Mic Count", mics.size.toString()))
            mics.forEachIndexed { i, mic ->
                items.add(InfoItem("Mic $i Location",
                    when (mic.location) {
                        android.media.MicrophoneInfo.LOCATION_MAINBODY          -> "Main Body"
                        android.media.MicrophoneInfo.LOCATION_MAINBODY_MOVABLE  -> "Movable"
                        android.media.MicrophoneInfo.LOCATION_PERIPHERAL        -> "Peripheral"
                        else -> "Unknown"
                    }
                ))
            }
        }

        // ── Low-Latency ──────────────────────────────────────────
        items.add(InfoItem("── Performance ──", "", true))
        val hasLowLatency = requireContext().packageManager.hasSystemFeature("android.hardware.audio.low_latency")
        val hasPro        = requireContext().packageManager.hasSystemFeature("android.hardware.audio.pro")
        items.add(InfoItem("Low Latency Audio", if (hasLowLatency) "Supported" else "Not Supported"))
        items.add(InfoItem("Pro Audio",         if (hasPro) "Supported" else "Not Supported"))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val sampleRate   = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) ?: "Unknown"
            val framesPerBuf = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER) ?: "Unknown"
            items.add(InfoItem("Output Sample Rate",    "$sampleRate Hz"))
            items.add(InfoItem("Frames Per Buffer",     framesPerBuf))
        }

        return items
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("🔊 Audio Info")
        sb.appendLine("─────────────────")
        latestItems.filter { it.label.isNotEmpty() }.forEach { sb.appendLine("${it.label}: ${it.value}") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
