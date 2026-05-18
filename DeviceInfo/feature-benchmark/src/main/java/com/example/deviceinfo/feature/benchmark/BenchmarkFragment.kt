package com.example.deviceinfo.feature.benchmark

// ──────────────────────────────────────────────────────────────────────────────
// REPLACE the existing BenchmarkFragment.kt with this file.
// This version adds Memory, Disk, and Crypto result cards on top of
// the existing CPU gauge + history chart. Uses the updated BenchmarkEngine v2.
// ──────────────────────────────────────────────────────────────────────────────

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.ui.InfoAdapter
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.feature.benchmark.databinding.FragmentBenchmarkBinding
import java.util.concurrent.Executors

class BenchmarkFragment : Fragment(), ShareableFragment {

    private var _b: FragmentBenchmarkBinding? = null
    private val b get() = _b!!
    private val handler  = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var isRunning = false
    private var latestResult: BenchmarkEngine.BenchmarkResult? = null
    private var latestItems: List<InfoItem> = emptyList()
    private var adapter: InfoAdapter? = null
    private var runCount = 0

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentBenchmarkBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        updateInfoList(null)
        b.btnRunBenchmark.setOnClickListener { if (!isRunning) startBenchmark() }
        b.btnClearHistory.setOnClickListener  { b.historyChart.clearHistory() }
    }

    private fun startBenchmark() {
        isRunning = true
        b.btnRunBenchmark.isEnabled = false
        b.btnRunBenchmark.text = "Running…"
        b.stGauge.setAnimating(true)
        b.mtGauge.setAnimating(true)
        b.statusText.text = "Warming up…"
        b.progressBar.progress = 0
        // Reset cards
        b.tvMemWrite.text = "W: …"
        b.tvMemRead.text  = "R: …"
        b.tvDiskWrite.text = "W: …"
        b.tvDiskRead.text  = "R: …"
        b.tvCryptoMbps.text  = "…"
        b.tvCryptoClass.text = "AES-256"

        val ctx = requireContext().applicationContext
        executor.execute {
            val result = BenchmarkEngine.run(ctx, object : BenchmarkEngine.ProgressCallback {
                override fun onProgress(phase: String, percent: Int) {
                    handler.post {
                        if (_b == null) return@post
                        b.statusText.text = "$phase ($percent%)"
                        b.progressBar.progress = percent
                    }
                }
            })
            handler.post {
                if (_b == null) return@post
                isRunning = false
                b.btnRunBenchmark.isEnabled = true
                b.btnRunBenchmark.text = "Run Benchmark"
                b.stGauge.setAnimating(false)
                b.mtGauge.setAnimating(false)
                b.progressBar.progress = 100
                b.statusText.text = "Done in ${result.durationMs}ms · ${result.coreCount} cores"

                // CPU gauges
                b.stGauge.update(result.singleThreadScore, 10000, "Single-Thread")
                b.mtGauge.update(result.multiThreadScore,  10000, "Multi-Thread")

                // Memory mini-card
                b.tvMemWrite.text = "W: ${"%.0f".format(result.memoryWriteMbps)} MB/s"
                b.tvMemRead.text  = "R: ${"%.0f".format(result.memoryReadMbps)} MB/s"

                // Disk mini-card
                b.tvDiskWrite.text = if (result.diskWriteMbps > 0f)
                    "W: ${"%.1f".format(result.diskWriteMbps)} MB/s" else "W: N/A"
                b.tvDiskRead.text  = if (result.diskReadMbps > 0f)
                    "R: ${"%.1f".format(result.diskReadMbps)} MB/s"  else "R: N/A"

                // Crypto mini-card
                b.tvCryptoMbps.text  = if (result.cryptoMbps > 0f)
                    "${"%.0f".format(result.cryptoMbps)} MB/s" else "N/A"
                b.tvCryptoClass.text = when {
                    result.cryptoMbps >= 1000 -> "HW-AES 🚀"
                    result.cryptoMbps >= 400  -> "Accel. ⚡"
                    result.cryptoMbps > 0f    -> "SW-AES ✅"
                    else                      -> "AES-256"
                }

                // History
                runCount++
                b.historyChart.addResult(
                    BenchmarkHistoryChartView.RunResult(
                        stScore = result.singleThreadScore,
                        mtScore = result.multiThreadScore,
                        label   = "Run $runCount"
                    )
                )

                latestResult = result
                updateInfoList(result)
            }
        }
    }

    private fun updateInfoList(result: BenchmarkEngine.BenchmarkResult?) {
        val cores = Runtime.getRuntime().availableProcessors()
        val items = mutableListOf<InfoItem>()

        items.add(InfoItem("DEVICE", "", true))
        items.add(InfoItem("CPU Cores",    "$cores"))
        items.add(InfoItem("Architecture", android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown", true))
        items.add(InfoItem("Android API",  android.os.Build.VERSION.SDK_INT.toString()))

        if (result != null) {
            items.add(InfoItem("CPU BENCHMARK", "", true))
            items.add(InfoItem("Single-Thread Score", "%,d".format(result.singleThreadScore), true))
            items.add(InfoItem("Multi-Thread Score",  "%,d".format(result.multiThreadScore),  true))
            items.add(InfoItem("MT/ST Speedup", "${"%.2f".format(
                result.multiThreadScore.toFloat() / result.singleThreadScore.toFloat())}×"))
            items.add(InfoItem("Duration",   "${result.durationMs} ms"))
            items.add(InfoItem("Cores Used", result.coreCount.toString()))
            val cpuClass = when {
                result.singleThreadScore >= 8000 -> "Extreme 🚀"
                result.singleThreadScore >= 6000 -> "Fast ⚡"
                result.singleThreadScore >= 4000 -> "Good ✅"
                result.singleThreadScore >= 2000 -> "Fair 🟡"
                else                             -> "Slow 🔴"
            }
            items.add(InfoItem("CPU Class", cpuClass, true))

            items.add(InfoItem("MEMORY BANDWIDTH", "", true))
            items.add(InfoItem("Write Speed", "${"%.0f".format(result.memoryWriteMbps)} MB/s", true))
            items.add(InfoItem("Read Speed",  "${"%.0f".format(result.memoryReadMbps)} MB/s"))
            val memClass = when {
                result.memoryWriteMbps >= 20000 -> "Excellent 🚀"
                result.memoryWriteMbps >= 10000 -> "Fast ⚡"
                result.memoryWriteMbps >= 5000  -> "Good ✅"
                result.memoryWriteMbps >= 2000  -> "Average 🟡"
                else                            -> "Slow 🔴"
            }
            items.add(InfoItem("Memory Class", memClass, true))

            items.add(InfoItem("DISK I/O (Internal)", "", true))
            if (result.diskWriteMbps > 0f) {
                items.add(InfoItem("Write Speed", "${"%.1f".format(result.diskWriteMbps)} MB/s", true))
                items.add(InfoItem("Read Speed",  "${"%.1f".format(result.diskReadMbps)} MB/s"))
                val diskClass = when {
                    result.diskWriteMbps >= 500 -> "UFS 3.1+ 🚀"
                    result.diskWriteMbps >= 300 -> "UFS 3.0 ⚡"
                    result.diskWriteMbps >= 150 -> "UFS 2.1 ✅"
                    result.diskWriteMbps >= 80  -> "UFS 2.0 🟡"
                    else                        -> "eMMC / Slow 🔴"
                }
                items.add(InfoItem("Storage Class", diskClass, true))
            } else {
                items.add(InfoItem("Disk Test", "N/A on this device"))
            }

            items.add(InfoItem("CRYPTO (AES-256-CBC)", "", true))
            if (result.cryptoMbps > 0f) {
                items.add(InfoItem("AES Throughput", "${"%.0f".format(result.cryptoMbps)} MB/s", true))
                val cryptoClass = when {
                    result.cryptoMbps >= 1000 -> "Hardware AES 🚀"
                    result.cryptoMbps >= 400  -> "Accelerated ⚡"
                    result.cryptoMbps >= 100  -> "Software AES ✅"
                    else                      -> "Slow 🔴"
                }
                items.add(InfoItem("Crypto Class", cryptoClass))
            }
        } else {
            items.add(InfoItem("CPU BENCHMARK", "", true))
            items.add(InfoItem("Single / Multi-Thread", "Tap Run to start"))
            items.add(InfoItem("MEMORY BANDWIDTH", "", true))
            items.add(InfoItem("Write / Read", "Tap Run to start"))
            items.add(InfoItem("DISK I/O", "", true))
            items.add(InfoItem("Write / Read", "Tap Run to start"))
            items.add(InfoItem("CRYPTO AES-256", "", true))
            items.add(InfoItem("Throughput", "Tap Run to start"))
        }

        items.add(InfoItem("ABOUT BENCHMARK v2", "", true))
        items.add(InfoItem("CPU Test",    "Primes, Float Math, Fibonacci, Merge Sort"))
        items.add(InfoItem("Memory Test", "16 MB sequential JVM heap write + read"))
        items.add(InfoItem("Disk Test",   "4 MB file write + read (cache dir, no permissions)"))
        items.add(InfoItem("Crypto Test", "AES-256-CBC via javax.crypto (2 MB blocks)"))
        items.add(InfoItem("Note",        "Results vary with throttling and system load"))

        latestItems = items
        adapter = InfoAdapter(items, "BENCHMARK")
        if (_b != null) {
            b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
            b.recyclerView.adapter = adapter
        }
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("⚡ Benchmark v2 Results")
        sb.appendLine("─────────────────")
        val r = latestResult
        if (r != null) {
            sb.appendLine("CPU Single-Thread: %,d".format(r.singleThreadScore))
            sb.appendLine("CPU Multi-Thread:  %,d".format(r.multiThreadScore))
            sb.appendLine("MT/ST Speedup: ${"%.2f".format(r.multiThreadScore.toFloat() / r.singleThreadScore.toFloat())}×")
            sb.appendLine("Memory Write: ${"%.0f".format(r.memoryWriteMbps)} MB/s")
            sb.appendLine("Memory Read:  ${"%.0f".format(r.memoryReadMbps)} MB/s")
            sb.appendLine("Disk Write:   ${"%.1f".format(r.diskWriteMbps)} MB/s")
            sb.appendLine("Disk Read:    ${"%.1f".format(r.diskReadMbps)} MB/s")
            sb.appendLine("AES-256:      ${"%.0f".format(r.cryptoMbps)} MB/s")
            sb.appendLine("Duration:     ${r.durationMs} ms  |  ${r.coreCount} cores")
        } else {
            sb.appendLine("No runs yet.")
        }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun getExportData(): Map<String, String> =
        latestItems.filter { it.value.isNotEmpty() }.associate { it.label to it.value }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
