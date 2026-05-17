package com.example.deviceinfo.feature.benchmark

import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.feature.benchmark.databinding.FragmentBenchmarkBinding
import kotlinx.coroutines.*

class BenchmarkFragment : Fragment(), ShareableFragment {
    private var _b: FragmentBenchmarkBinding? = null
    private val b get() = _b!!
    private var lastResult: BenchmarkEngine.BenchmarkResult? = null
    private var isRunning = false

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentBenchmarkBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)

        b.tvDeviceInfo.text = "${Build.MODEL}  •  ${Runtime.getRuntime().availableProcessors()} cores  •  Android ${Build.VERSION.RELEASE}"
        b.tvStatus.text = "Tap RUN to start benchmark"

        b.btnRun.setOnClickListener {
            if (!isRunning) startBenchmark()
        }
    }

    private fun startBenchmark() {
        isRunning = true
        b.btnRun.isEnabled = false
        b.progressBar.visibility = View.VISIBLE
        b.tvStatus.text = "Preparing…"
        b.scoreGaugeTotal.setScore(0, "Running…")

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                BenchmarkEngine.run(requireContext()) { msg ->
                    launch(Dispatchers.Main) { b.tvStatus.text = msg }
                }
            }
            lastResult = result
            displayResult(result)
            isRunning = false
            b.btnRun.isEnabled = true
            b.progressBar.visibility = View.GONE
        }
    }

    private fun displayResult(r: BenchmarkEngine.BenchmarkResult) {
        if (_b == null) return

        b.scoreGaugeTotal.setScore(r.totalScore, "TOTAL")
        b.scoreGaugeSingle.setScore(r.singleCore, "Single")
        b.scoreGaugeMulti.setScore(r.multiCore, "Multi")
        b.scoreGaugeMem.setScore(r.memoryScore, "Memory")

        b.tvStatus.text = "Benchmark complete!"

        b.tvDetails.text = buildString {
            appendLine("📊 Detailed Results")
            appendLine("─────────────────────────────")
            appendLine("Single-Core   ${r.singleCore}/1000   (${r.singleMs} ms)")
            appendLine("Multi-Core    ${r.multiCore}/1000   (${r.multiMs} ms)")
            appendLine("Memory        ${r.memoryScore}/1000   (${r.memMs} ms)")
            appendLine("Storage Read  ${r.storageReadScore}/1000   (${String.format("%.1f", r.storageReadMBps)} MB/s)")
            appendLine("Storage Write ${r.storageWriteScore}/1000   (${String.format("%.1f", r.storageWriteMBps)} MB/s)")
            appendLine()
            appendLine("Device: ${Build.MODEL}")
            appendLine("SoC:    ${Build.HARDWARE}")
            appendLine("Cores:  ${Runtime.getRuntime().availableProcessors()}")
            appendLine("ABI:    ${Build.SUPPORTED_ABIS.firstOrNull()}")
            appendLine("Android ${Build.VERSION.RELEASE}  (API ${Build.VERSION.SDK_INT})")
        }
    }

    override fun getShareText(): String {
        val r = lastResult ?: return "No benchmark run yet."
        return buildString {
            appendLine("⚡ CPU-A Benchmark Results")
            appendLine("─────────────────────────────")
            appendLine("Total Score:   ${r.totalScore}/1000")
            appendLine("Single-Core:   ${r.singleCore}/1000")
            appendLine("Multi-Core:    ${r.multiCore}/1000")
            appendLine("Memory:        ${r.memoryScore}/1000")
            appendLine("Storage Read:  ${r.storageReadScore}/1000  (${String.format("%.1f", r.storageReadMBps)} MB/s)")
            appendLine("Storage Write: ${r.storageWriteScore}/1000  (${String.format("%.1f", r.storageWriteMBps)} MB/s)")
            appendLine()
            appendLine("Device: ${Build.MODEL}  •  Android ${Build.VERSION.RELEASE}")
            appendLine("Shared from CPU-A Device Info app")
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
