package com.example.deviceinfo.feature.benchmark

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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class BenchmarkFragment : Fragment(), ShareableFragment {
    private var _b: FragmentBenchmarkBinding? = null
    private val b get() = _b!!
    private val handler = Handler(Looper.getMainLooper())
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
        updateInfo(null)

        b.btnRunBenchmark.setOnClickListener {
            if (!isRunning) startBenchmark()
        }
        b.btnClearHistory.setOnClickListener {
            b.historyChart.clearHistory()
        }
    }

    private fun startBenchmark() {
        isRunning = true
        b.btnRunBenchmark.isEnabled = false
        b.btnRunBenchmark.text = "Running…"
        b.stGauge.setAnimating(true)
        b.mtGauge.setAnimating(true)
        b.statusText.text = "Warming up…"

        executor.execute {
            val result = BenchmarkEngine.run(object : BenchmarkEngine.ProgressCallback {
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
                b.statusText.text = "Completed in ${result.durationMs}ms · ${result.coreCount} cores"
                latestResult = result
                runCount++

                // Update gauges
                b.stGauge.update(result.singleThreadScore, 10000, "Single-Thread")
                b.mtGauge.update(result.multiThreadScore,  10000, "Multi-Thread")

                // Add to history chart
                val timeLabel = "Run $runCount"
                b.historyChart.addResult(
                    BenchmarkHistoryChartView.RunResult(
                        stScore = result.singleThreadScore,
                        mtScore = result.multiThreadScore,
                        label   = timeLabel
                    )
                )

                // Update info list
                updateInfo(result)
            }
        }
    }

    private fun updateInfo(result: BenchmarkEngine.BenchmarkResult?) {
        val cores = Runtime.getRuntime().availableProcessors()
        val items = mutableListOf<InfoItem>()

        items.add(InfoItem("DEVICE", "", true))
        items.add(InfoItem("CPU Cores", "$cores"))
        items.add(InfoItem("Architecture", android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown", true))
        items.add(InfoItem("Android API", android.os.Build.VERSION.SDK_INT.toString()))

        if (result != null) {
            items.add(InfoItem("BENCHMARK RESULTS", "", true))
            items.add(InfoItem("Single-Thread Score", "%,d".format(result.singleThreadScore), true))
            items.add(InfoItem("Multi-Thread Score",  "%,d".format(result.multiThreadScore),  true))
            items.add(InfoItem("MT / ST Speedup",     "${"%.2f".format(result.multiThreadScore.toFloat() / result.singleThreadScore.toFloat())}×"))
            items.add(InfoItem("Duration", "${result.durationMs} ms"))
            items.add(InfoItem("Cores Used", result.coreCount.toString()))

            val rating = when {
                result.singleThreadScore >= 8000 -> "Extreme 🚀"
                result.singleThreadScore >= 6000 -> "Fast ⚡"
                result.singleThreadScore >= 4000 -> "Good ✅"
                result.singleThreadScore >= 2000 -> "Fair 🟡"
                else                             -> "Slow 🔴"
            }
            items.add(InfoItem("Performance Class", rating, true))

            items.add(InfoItem("ABOUT THIS BENCHMARK", "", true))
            items.add(InfoItem("Workloads", "Prime Sieve, Float Math, Fibonacci, Merge Sort"))
            items.add(InfoItem("Method", "Pure JVM — no native code"))
            items.add(InfoItem("Note", "Scores may vary by thermal throttling and OS load"))
        } else {
            items.add(InfoItem("BENCHMARK RESULTS", "", true))
            items.add(InfoItem("Single-Thread Score", "— (not run yet)"))
            items.add(InfoItem("Multi-Thread Score",  "— (not run yet)"))
            items.add(InfoItem("ABOUT THIS BENCHMARK", "", true))
            items.add(InfoItem("Workloads", "Prime Sieve, Float Math, Fibonacci, Merge Sort"))
            items.add(InfoItem("Method", "Pure JVM — no native code, no root needed"))
            items.add(InfoItem("Tip", "For best results, plug in charger and close background apps"))
        }

        latestItems = items
        adapter = InfoAdapter(items)
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("⚡ Benchmark Results")
        sb.appendLine("─────────────────")
        val r = latestResult
        if (r != null) {
            sb.appendLine("Single-Thread: %,d".format(r.singleThreadScore))
            sb.appendLine("Multi-Thread:  %,d".format(r.multiThreadScore))
            sb.appendLine("Duration: ${r.durationMs}ms")
            sb.appendLine("Cores: ${r.coreCount}")
        } else {
            sb.appendLine("No benchmark run yet.")
        }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun getExportData(): Map<String, String> =
        latestItems.associate { it.label to it.value }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
