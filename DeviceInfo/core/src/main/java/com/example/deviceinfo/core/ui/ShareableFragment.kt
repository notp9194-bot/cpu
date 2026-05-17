package com.example.deviceinfo.core.ui

/**
 * Fragments that want to provide real data to the Share button implement this.
 */
interface ShareableFragment {
    /** Return a human-readable plain-text summary of the current tab's data. */
    fun getShareText(): String

    /**
     * Return a key→value map for the Export All report.
     * Default implementation parses getShareText() — override for accuracy.
     */
    fun getExportData(): Map<String, String> {
        val lines = getShareText().lines()
            .drop(2)                              // skip emoji header + divider line
            .filter { it.contains(":") && !it.startsWith("Shared") }
        val map = linkedMapOf<String, String>()
        lines.forEach { line ->
            val idx = line.indexOf(":")
            if (idx > 0) {
                val k = line.substring(0, idx).trim()
                val v = line.substring(idx + 1).trim()
                if (k.isNotBlank() && v.isNotBlank()) map[k] = v
            }
        }
        return map
    }
}
