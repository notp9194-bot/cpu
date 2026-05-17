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
import java.util.concurrent.Executors
import kotlin.math.*

class BenchmarkFragment : Fragment(), ShareableFragment {
    private var _b: FragmentBenchmarkBinding? = null
    private val b get() = _b!!
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    private var isRunning = false
    private var latestResults: List<InfoItem> = emptyList()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentBenchmarkBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.btnRun.setOnClickListener { if (!isRunning) startBenchmark() }
    }

    private fun startBenchmark() {
        isRunning = true
        b.btnRun.isEnabled = false
        b.progressBar.visibility = View.VISIBLE
        b.tvStatus.text = "Running single-core test…"
        b.tvSingleScore.text = "…"
        b.tvMultiScore.text = "…"
        b.tvMemScore.text = "…"

        executor.execute {
            // ── Single-core: integer + float math ─────────────────────
            val singleScore = benchmarkSingleCore()
            handler.post {
                if (_b == null) return@post
                b.tvSingleScore.text = singleScore.toString()
                b.tvStatus.text = "Running multi-core test…"
            }

            // ── Multi-core: parallel across all cores ──────────────────
            val cores = Runtime.getRuntime().availableProcessors()
            val multiScore = benchmarkMultiCore(cores)
            handler.post {
                if (_b == null) return@post
                b.tvMultiScore.text = multiScore.toString()
                b.tvStatus.text = "Running memory test…"
            }

            // ── Memory bandwidth ──────────────────────────────────────
            val memBandwidthMbs = benchmarkMemory()
            handler.post {
                if (_b == null) return@post
                b.tvMemScore.text = "$memBandwidthMbs MB/s"
                b.tvStatus.text = "Benchmark complete ✓"
                b.progressBar.visibility = View.INVISIBLE
                b.btnRun.isEnabled = true
                isRunning = false

                val tier = scoreTier(singleScore)
                val results = mutableListOf(
                    InfoItem("── Results ──", "", true),
                    InfoItem("Single-Core Score",  singleScore.toString(), true),
                    InfoItem("Multi-Core Score",   multiScore.toString(), true),
                    InfoItem("Memory Bandwidth",   "$memBandwidthMbs MB/s", true),
                    InfoItem("CPU Cores Used",     cores.toString()),
                    InfoItem("Performance Tier",   tier),
                    InfoItem("── How it Works ──", "", true),
                    InfoItem("Single-Core",  "Integer ops, float math, sqrt loops (1s)"),
                    InfoItem("Multi-Core",   "Parallel execution across all cores"),
                    InfoItem("Memory",       "Array fill + read bandwidth (64 MB)"),
                    InfoItem("Score Base",   "Operations per millisecond × scaling factor"),
                )
                latestResults = results
                val adapter = InfoAdapter(results)
                b.recyclerView.adapter = adapter
            }
        }
    }

    /** Single-core: integer math + trigonometry + sqrt in 1 second */
    private fun benchmarkSingleCore(): Int {
        val endTime = System.currentTimeMillis() + 1000L
        var ops = 0L
        var x = 1.0
        while (System.currentTimeMillis() < endTime) {
            x = sqrt(x * 1.000001 + sin(x) * cos(x) + 1.0)
            ops++
        }
        return (ops / 1000).toInt()  // score in kilo-ops/s
    }

    /** Multi-core: run single-core benchmark on N threads simultaneously */
    private fun benchmarkMultiCore(cores: Int): Int {
        val futures = (0 until cores).map {
            java.util.concurrent.Callable { benchmarkSingleCore() }
        }
        val results = executor.invokeAll(futures)
        return results.sumOf { it.get() }
    }

    /** Memory bandwidth: fill + read 64 MB array, measure throughput */
    private fun benchmarkMemory(): Int {
        val SIZE = 64 * 1024 * 1024 / 8  // 64 MB in longs
        val arr = LongArray(SIZE)
        val start = System.nanoTime()
        // Write pass
        for (i in arr.indices) arr[i] = i.toLong()
        // Read pass
        var sum = 0L
        for (i in arr.indices) sum += arr[i]
        val elapsed = System.nanoTime() - start
        val bytesTransferred = SIZE.toLong() * 8 * 2  // write + read
        val mbPerSec = (bytesTransferred * 1_000_000_000L / elapsed / (1024 * 1024)).toInt()
        return if (sum != 0L) mbPerSec else 0 // use sum to prevent dead-code elimination
    }

    private fun scoreTier(single: Int): String = when {
        single >= 3000 -> "🔥 Flagship"
        single >= 2000 -> "⚡ High-End"
        single >= 1200 -> "✅ Mid-Range"
        single >= 600  -> "📱 Budget"
        else           -> "🐢 Entry-Level"
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("🏎️ CPU Benchmark")
        sb.appendLine("─────────────────")
        latestResults.filter { it.value.isNotEmpty() }.forEach { sb.appendLine("${it.label}: ${it.value}") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
