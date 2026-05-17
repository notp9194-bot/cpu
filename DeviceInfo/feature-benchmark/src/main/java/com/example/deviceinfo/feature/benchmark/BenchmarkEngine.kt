package com.example.deviceinfo.feature.benchmark

import kotlin.math.sqrt
import kotlin.math.ln
import kotlin.math.sin

/**
 * Pure-math CPU benchmark engine.
 * No native code, no root, no special permissions.
 * Play Store safe — uses only standard Kotlin/JVM math.
 *
 * Single-threaded score: sequential prime sieve + float math on Thread.currentThread()
 * Multi-threaded score:  parallel workloads on N cores, measures speedup
 */
object BenchmarkEngine {

    data class BenchmarkResult(
        val singleThreadScore: Int,
        val multiThreadScore: Int,
        val durationMs: Long,
        val coreCount: Int
    )

    interface ProgressCallback {
        fun onProgress(phase: String, percent: Int)
    }

    /** Runs the full benchmark. Call from a background thread. */
    fun run(callback: ProgressCallback? = null): BenchmarkResult {
        val cores = Runtime.getRuntime().availableProcessors()
        val t0 = System.currentTimeMillis()

        // ── Phase 1: Single-threaded ──────────────────────────────────
        callback?.onProgress("Single-Thread", 0)
        val stScore = runSingleThread(callback)

        // ── Phase 2: Multi-threaded ───────────────────────────────────
        callback?.onProgress("Multi-Thread", 50)
        val mtScore = runMultiThread(cores, callback)

        val duration = System.currentTimeMillis() - t0
        return BenchmarkResult(stScore, mtScore, duration, cores)
    }

    // ── Single-thread workload ────────────────────────────────────────
    private fun runSingleThread(callback: ProgressCallback?): Int {
        val start = System.nanoTime()
        var ops = 0L

        // Workload 1: Prime sieve up to 50,000
        val primes = sieveOfEratosthenes(50_000)
        ops += primes.size
        callback?.onProgress("ST: Primes", 10)

        // Workload 2: Float-point math (sqrt, ln, sin)
        var acc = 0.0
        for (i in 1..200_000) {
            acc += sqrt(i.toDouble()) * ln(i.toDouble() + 1.0) * sin(i * 0.001)
            ops++
        }
        callback?.onProgress("ST: Float Math", 25)

        // Workload 3: Fibonacci (matrix method) — integer ops
        var a = 0L; var b = 1L
        repeat(500_000) {
            val c = a + b; a = b; b = c; ops++
        }
        callback?.onProgress("ST: Fibonacci", 40)

        // Workload 4: Sorting (merge sort) on 5000 elements
        val arr = IntArray(5000) { (Math.random() * 100_000).toInt() }
        mergeSort(arr, 0, arr.size - 1)
        ops += arr.size
        callback?.onProgress("ST: Sort", 48)

        val elapsedMs = (System.nanoTime() - start) / 1_000_000L
        // Score: ops per ms, scaled to ~5000 range for single thread
        return ((ops.toDouble() / elapsedMs.toDouble()) * 0.8).toInt().coerceIn(100, 9999)
    }

    // ── Multi-thread workload ─────────────────────────────────────────
    private fun runMultiThread(cores: Int, callback: ProgressCallback?): Int {
        val start = System.nanoTime()
        val threads = mutableListOf<Thread>()
        val results = LongArray(cores)

        // Each thread does independent float math
        for (t in 0 until cores) {
            val thread = Thread {
                var ops = 0L
                var acc = 0.0
                for (i in 1..150_000) {
                    acc += sqrt(i.toDouble() + t) * ln(i.toDouble() + t + 1.0)
                    ops++
                }
                // Prime sieve per thread
                val sieve = sieveOfEratosthenes(30_000)
                ops += sieve.size
                // Fibonacci
                var a = 0L; var b = 1L
                repeat(300_000) { val c = a + b; a = b; b = c; ops++ }
                results[t] = ops
            }
            threads.add(thread)
        }

        threads.forEach { it.start() }
        threads.forEachIndexed { i, t ->
            t.join()
            callback?.onProgress("MT: Core $i done", 50 + (i * 40 / cores))
        }

        val totalOps = results.sum()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000L
        callback?.onProgress("Multi-Thread", 95)

        return ((totalOps.toDouble() / elapsedMs.toDouble()) * 0.9).toInt().coerceIn(100, 9999)
    }

    // ── Utility: Sieve of Eratosthenes ───────────────────────────────
    private fun sieveOfEratosthenes(limit: Int): List<Int> {
        val isComposite = BooleanArray(limit + 1)
        val primes = mutableListOf<Int>()
        for (i in 2..limit) {
            if (!isComposite[i]) {
                primes.add(i)
                var j = i.toLong() * i
                while (j <= limit) {
                    isComposite[j.toInt()] = true
                    j += i
                }
            }
        }
        return primes
    }

    // ── Utility: Merge Sort ───────────────────────────────────────────
    private fun mergeSort(arr: IntArray, left: Int, right: Int) {
        if (left >= right) return
        val mid = (left + right) / 2
        mergeSort(arr, left, mid)
        mergeSort(arr, mid + 1, right)
        merge(arr, left, mid, right)
    }

    private fun merge(arr: IntArray, left: Int, mid: Int, right: Int) {
        val l = arr.copyOfRange(left, mid + 1)
        val r = arr.copyOfRange(mid + 1, right + 1)
        var i = 0; var j = 0; var k = left
        while (i < l.size && j < r.size) {
            if (l[i] <= r[j]) arr[k++] = l[i++] else arr[k++] = r[j++]
        }
        while (i < l.size) arr[k++] = l[i++]
        while (j < r.size) arr[k++] = r[j++]
    }
}
