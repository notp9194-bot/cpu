package com.cpua.deviceinfo.feature.benchmark

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Enhanced CPU + Memory + Disk + Crypto benchmark engine (v2).
 * No native code, no root, no special permissions.
 * All workloads use standard Kotlin/JVM/Android APIs.
 * Play Store safe ✅
 *
 * SCORES:
 *   Single-Thread:  sequential prime sieve + float math + fibonacci + sort
 *   Multi-Thread:   parallel float math + primes + fibonacci across all cores
 *   Memory:         sequential array write/read speed (MB/s)
 *   Disk:           internal storage write/read speed (MB/s)
 *   Crypto:         AES-256 encryption throughput (MB/s)
 */
object BenchmarkEngine {

    data class BenchmarkResult(
        val singleThreadScore: Int,
        val multiThreadScore:  Int,
        val memoryWriteMbps:   Float,
        val memoryReadMbps:    Float,
        val diskWriteMbps:     Float,
        val diskReadMbps:      Float,
        val cryptoMbps:        Float,
        val durationMs:        Long,
        val coreCount:         Int
    )

    interface ProgressCallback {
        fun onProgress(phase: String, percent: Int)
    }

    /** Runs the full benchmark. Call from a background thread. */
    fun run(context: Context, callback: ProgressCallback? = null): BenchmarkResult {
        val cores = Runtime.getRuntime().availableProcessors()
        val t0 = System.currentTimeMillis()

        // Phase 1: Single-thread CPU
        callback?.onProgress("Single-Thread CPU", 0)
        val stScore = runSingleThread(callback)

        // Phase 2: Multi-thread CPU
        callback?.onProgress("Multi-Thread CPU", 20)
        val mtScore = runMultiThread(cores, callback)

        // Phase 3: Memory bandwidth
        callback?.onProgress("Memory Bandwidth", 50)
        val (memWriteMbps, memReadMbps) = runMemoryBandwidth()
        callback?.onProgress("Memory Read done", 65)

        // Phase 4: Disk I/O
        callback?.onProgress("Disk Write", 68)
        val (diskWriteMbps, diskReadMbps) = runDiskBenchmark(context)
        callback?.onProgress("Disk Read done", 82)

        // Phase 5: Crypto
        callback?.onProgress("AES Crypto", 85)
        val cryptoMbps = runCryptoBenchmark()
        callback?.onProgress("Complete", 100)

        val duration = System.currentTimeMillis() - t0
        return BenchmarkResult(
            singleThreadScore = stScore,
            multiThreadScore  = mtScore,
            memoryWriteMbps   = memWriteMbps,
            memoryReadMbps    = memReadMbps,
            diskWriteMbps     = diskWriteMbps,
            diskReadMbps      = diskReadMbps,
            cryptoMbps        = cryptoMbps,
            durationMs        = duration,
            coreCount         = cores
        )
    }

    // ── Phase 1: Single-thread ──────────────────────────────────────────
    private fun runSingleThread(callback: ProgressCallback?): Int {
        val start = System.nanoTime()
        var ops = 0L

        val primes = sieveOfEratosthenes(50_000)
        ops += primes.size
        callback?.onProgress("ST: Primes", 5)

        var acc = 0.0
        for (i in 1..200_000) {
            acc += sqrt(i.toDouble()) * ln(i.toDouble() + 1.0) * sin(i * 0.001)
            ops++
        }
        callback?.onProgress("ST: Float Math", 12)

        var a = 0L; var b = 1L
        repeat(500_000) { val c = a + b; a = b; b = c; ops++ }
        callback?.onProgress("ST: Fibonacci", 17)

        val arr = IntArray(5000) { (Math.random() * 100_000).toInt() }
        mergeSort(arr, 0, arr.size - 1)
        ops += arr.size
        callback?.onProgress("ST: Sort", 19)

        val elapsedMs = (System.nanoTime() - start) / 1_000_000L
        return ((ops.toDouble() / elapsedMs.toDouble()) * 0.8).toInt().coerceIn(100, 9999)
    }

    // ── Phase 2: Multi-thread ───────────────────────────────────────────
    private fun runMultiThread(cores: Int, callback: ProgressCallback?): Int {
        val start   = System.nanoTime()
        val threads = mutableListOf<Thread>()
        val results = LongArray(cores)

        for (t in 0 until cores) {
            val thread = Thread {
                var ops = 0L
                var acc = 0.0
                for (i in 1..150_000) {
                    acc += sqrt(i.toDouble() + t) * ln(i.toDouble() + t + 1.0)
                    ops++
                }
                ops += sieveOfEratosthenes(30_000).size
                var x = 0L; var y = 1L
                repeat(300_000) { val c = x + y; x = y; y = c; ops++ }
                results[t] = ops
            }
            threads.add(thread)
        }

        threads.forEach { it.start() }
        threads.forEachIndexed { i, th ->
            th.join()
            callback?.onProgress("MT: Core $i", 20 + (i * 25 / cores))
        }

        val totalOps  = results.sum()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000L
        callback?.onProgress("Multi-Thread", 48)
        return ((totalOps.toDouble() / elapsedMs.toDouble()) * 0.9).toInt().coerceIn(100, 9999)
    }

    // ── Phase 3: Memory Bandwidth ───────────────────────────────────────
    /**
     * Allocates a 16 MB byte array, writes sequentially, then reads sequentially.
     * Reports MB/s for each. Pure JVM heap — no permissions needed.
     */
    private fun runMemoryBandwidth(): Pair<Float, Float> {
        val sizeMb  = 16
        val buf     = ByteArray(sizeMb * 1024 * 1024)
        val fillVal = 0xAB.toByte()

        // Write
        val writeStart = System.nanoTime()
        buf.fill(fillVal)
        val writeMs = ((System.nanoTime() - writeStart) / 1_000_000.0).coerceAtLeast(1.0)
        val writeMbps = (sizeMb / (writeMs / 1000.0)).toFloat()

        // Read (sum all bytes — prevents JIT from optimizing away)
        val readStart = System.nanoTime()
        var checksum = 0L
        for (byte in buf) checksum += byte
        val readMs = ((System.nanoTime() - readStart) / 1_000_000.0).coerceAtLeast(1.0)
        val readMbps = (sizeMb / (readMs / 1000.0)).toFloat()

        return writeMbps to readMbps
    }

    // ── Phase 4: Disk I/O ───────────────────────────────────────────────
    /**
     * Writes then reads a 4 MB temporary file to internal storage.
     * No WRITE_EXTERNAL_STORAGE permission needed — uses app's cache dir.
     * File is deleted after test. Play Store safe ✅
     */
    private fun runDiskBenchmark(context: Context): Pair<Float, Float> {
        val testFile = File(context.cacheDir, "benchmark_io_test.tmp")
        val sizeMb   = 4
        val data     = ByteArray(sizeMb * 1024 * 1024) { it.toByte() }

        var writeMbps = 0f
        var readMbps  = 0f

        try {
            // Write
            val writeStart = System.nanoTime()
            FileOutputStream(testFile).use { it.write(data) }
            val writeMs = ((System.nanoTime() - writeStart) / 1_000_000.0).coerceAtLeast(1.0)
            writeMbps = (sizeMb / (writeMs / 1000.0)).toFloat()

            // Read
            val readBuf = ByteArray(sizeMb * 1024 * 1024)
            val readStart = System.nanoTime()
            FileInputStream(testFile).use { it.read(readBuf) }
            val readMs = ((System.nanoTime() - readStart) / 1_000_000.0).coerceAtLeast(1.0)
            readMbps = (sizeMb / (readMs / 1000.0)).toFloat()
        } catch (_: Exception) {
            // Silently fail — disk test result stays 0f
        } finally {
            testFile.delete()
        }

        return writeMbps to readMbps
    }

    // ── Phase 5: Crypto (AES-256) ───────────────────────────────────────
    /**
     * Encrypts 2 MB of data with AES-256-CBC using javax.crypto (Android standard API).
     * No special permission required. Reports MB/s throughput.
     * Play Store safe ✅
     */
    private fun runCryptoBenchmark(): Float {
        return try {
            val keyGen = KeyGenerator.getInstance("AES").apply { init(256) }
            val key    = keyGen.generateKey()
            val iv     = IvParameterSpec(ByteArray(16) { it.toByte() })
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key, iv)

            val sizeMb  = 2
            val data    = ByteArray(sizeMb * 1024 * 1024) { it.toByte() }

            val start = System.nanoTime()
            // Process in 64KB blocks (realistic pattern)
            val blockSize = 64 * 1024
            var offset = 0
            while (offset + blockSize <= data.size) {
                cipher.update(data, offset, blockSize)
                offset += blockSize
            }
            if (offset < data.size) cipher.doFinal(data, offset, data.size - offset)
            else cipher.doFinal()

            val ms = ((System.nanoTime() - start) / 1_000_000.0).coerceAtLeast(1.0)
            (sizeMb / (ms / 1000.0)).toFloat()
        } catch (_: Exception) { 0f }
    }

    // ── Utilities ───────────────────────────────────────────────────────
    private fun sieveOfEratosthenes(limit: Int): List<Int> {
        val isComposite = BooleanArray(limit + 1)
        val primes = mutableListOf<Int>()
        for (i in 2..limit) {
            if (!isComposite[i]) {
                primes.add(i)
                var j = i.toLong() * i
                while (j <= limit) { isComposite[j.toInt()] = true; j += i }
            }
        }
        return primes
    }

    private fun mergeSort(arr: IntArray, left: Int, right: Int) {
        if (left >= right) return
        val mid = (left + right) / 2
        mergeSort(arr, left, mid); mergeSort(arr, mid + 1, right)
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
