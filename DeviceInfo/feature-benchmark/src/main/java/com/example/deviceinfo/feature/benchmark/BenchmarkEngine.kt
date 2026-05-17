package com.example.deviceinfo.feature.benchmark

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.*

object BenchmarkEngine {

    data class BenchmarkResult(
        val singleCore: Int,      // score 0-1000
        val multiCore: Int,       // score 0-1000
        val memoryScore: Int,     // score 0-1000
        val storageReadScore: Int,// score 0-1000
        val storageWriteScore: Int,// score 0-1000
        val totalScore: Int,
        val singleMs: Long,
        val multiMs: Long,
        val memMs: Long,
        val storageReadMBps: Double,
        val storageWriteMBps: Double
    )

    /** Run all benchmarks. Call from IO dispatcher. */
    suspend fun run(
        context: Context,
        onProgress: (String) -> Unit
    ): BenchmarkResult = withContext(Dispatchers.Default) {

        // ── Single-core: Fibonacci iterative ──────────────────
        onProgress("Running single-core…")
        val singleMs = measureMs {
            var a = 0L; var b = 1L
            repeat(8_000_000) { val c = a + b; a = b; b = c }
        }

        // ── Multi-core: parallel Fibonacci across all cores ───
        onProgress("Running multi-core…")
        val cores = Runtime.getRuntime().availableProcessors()
        val multiMs = measureMs {
            val jobs = (0 until cores).map {
                async(Dispatchers.Default) {
                    var a = 0L; var b = 1L
                    repeat(8_000_000) { val c = a + b; a = b; b = c }
                    b
                }
            }
            jobs.awaitAll()
        }

        // ── Memory: large array alloc + sequential read/write ─
        onProgress("Running memory test…")
        val memMs = measureMs {
            val arr = IntArray(4 * 1024 * 1024) // 16 MB
            for (i in arr.indices) arr[i] = i
            var sum = 0L
            for (v in arr) sum += v
            sum
        }

        // ── Storage: write + read 8 MB temp file ──────────────
        onProgress("Running storage test…")
        val testFile = File(context.cacheDir, "bench_tmp.bin")
        val testData = ByteArray(8 * 1024 * 1024) { it.toByte() }

        val writeMs = withContext(Dispatchers.IO) {
            measureMs { testFile.writeBytes(testData) }
        }
        val readMs = withContext(Dispatchers.IO) {
            measureMs { testFile.readBytes() }
        }
        runCatching { testFile.delete() }

        val writeMBps = (testData.size / 1024.0 / 1024.0) / (writeMs / 1000.0)
        val readMBps  = (testData.size / 1024.0 / 1024.0) / (readMs  / 1000.0)

        // ── Score calculation ─────────────────────────────────
        // Baseline: mid-range 2023 device ~700ms single-core, ~300ms multi (8-core), 100ms mem
        val singleScore  = scoreFrom(singleMs, baseLow = 400L, baseHigh = 1200L)
        val multiScore   = scoreFrom(multiMs,  baseLow = 100L, baseHigh = 600L)
        val memScore     = scoreFrom(memMs,    baseLow = 50L,  baseHigh = 400L)
        val storWScore   = scoreFromThroughput(writeMBps, base = 150.0, cap = 2000.0)
        val storRScore   = scoreFromThroughput(readMBps,  base = 200.0, cap = 3000.0)
        val total = (singleScore * 0.30 + multiScore * 0.35 + memScore * 0.15 +
                     storWScore * 0.10 + storRScore * 0.10).toInt()

        BenchmarkResult(
            singleCore = singleScore, multiCore = multiScore,
            memoryScore = memScore,
            storageReadScore = storRScore, storageWriteScore = storWScore,
            totalScore = total,
            singleMs = singleMs, multiMs = multiMs, memMs = memMs,
            storageReadMBps = readMBps, storageWriteMBps = writeMBps
        )
    }

    private inline fun measureMs(block: () -> Unit): Long {
        val t = System.currentTimeMillis(); block(); return System.currentTimeMillis() - t
    }

    /** Lower ms = better. Map [baseLow..baseHigh] ms → score 1000..0 */
    private fun scoreFrom(ms: Long, baseLow: Long, baseHigh: Long): Int {
        if (ms <= baseLow)  return 1000
        if (ms >= baseHigh) return 100
        val frac = (ms - baseLow).toDouble() / (baseHigh - baseLow)
        return (1000 - (900 * frac)).toInt().coerceIn(100, 1000)
    }

    /** Higher MBps = better */
    private fun scoreFromThroughput(mbps: Double, base: Double, cap: Double): Int {
        if (mbps >= cap)  return 1000
        if (mbps <= 0)    return 50
        return ((mbps / cap) * 1000).toInt().coerceIn(50, 1000)
    }
}
