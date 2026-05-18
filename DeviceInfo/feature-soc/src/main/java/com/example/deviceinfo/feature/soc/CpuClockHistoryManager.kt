package com.example.deviceinfo.feature.soc

/**
 * In-memory circular ring buffer storing CPU clock snapshots.
 *
 * Each snapshot = one reading per core (in MHz), taken every 2 seconds.
 * Stores last MAX_POINTS readings → 60 seconds of history at 2s interval.
 *
 * No permissions, no disk, no root. Play Store safe ✅
 */
object CpuClockHistoryManager {

    const val MAX_POINTS = 30        // 30 × 2s = 60 seconds
    const val SAMPLE_INTERVAL_MS = 2000L

    /**
     * One snapshot: list of per-core MHz values (index = core number).
     * If a core is offline its value is 0.
     */
    data class Snapshot(val coreFreqsMhz: List<Long>)

    private val ring = ArrayDeque<Snapshot>(MAX_POINTS + 1)

    /** Add a new snapshot. Oldest entry dropped when buffer is full. */
    fun push(snapshot: Snapshot) {
        if (ring.size >= MAX_POINTS) ring.removeFirst()
        ring.addLast(snapshot)
    }

    /** Returns a copy of the ring buffer, oldest first. */
    fun getHistory(): List<Snapshot> = ring.toList()

    /** Number of cores seen in the latest snapshot (0 if empty). */
    fun coreCount(): Int = ring.lastOrNull()?.coreFreqsMhz?.size ?: 0

    /** Clear all history (e.g. on fragment destroy). */
    fun clear() = ring.clear()
}
