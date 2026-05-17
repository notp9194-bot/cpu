package com.example.deviceinfo.core.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportUtils {

    /**
     * Exports ALL sections to a single .txt file in Downloads.
     * Called from AboutFragment with data from all tabs.
     */
    fun exportAllToFile(context: Context, allData: Map<String, Map<String, String>>) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "DeviceInfo_FullReport_$timestamp.txt"

        val content = buildString {
            appendLine("=== CPU-A Device Info — Full Report ===")
            appendLine("Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("Device  : ${android.os.Build.MODEL}")
            appendLine("Android : ${android.os.Build.VERSION.RELEASE}")
            appendLine()
            allData.forEach { (section, data) ->
                appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                appendLine("  $section")
                appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                data.forEach { (k, v) -> appendLine("  $k: $v") }
                appendLine()
            }
        }
        writeToDownloads(context, fileName, content)
    }

    /**
     * Exports a map of label→value pairs to a .txt file in Downloads.
     * Uses MediaStore on API 29+, legacy path on older.
     * No dangerous permissions needed on API 29+.
     */
    fun exportToFile(context: Context, sectionName: String, data: Map<String, String>) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "DeviceInfo_${sectionName}_$timestamp.txt"

        val content = buildString {
            appendLine("=== CPU-A Device Info Export ===")
            appendLine("Section : $sectionName")
            appendLine("Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("Device  : ${android.os.Build.MODEL}")
            appendLine("Android : ${android.os.Build.VERSION.RELEASE}")
            appendLine()
            appendLine("--- $sectionName ---")
            data.forEach { (k, v) -> appendLine("$k: $v") }
        }
        writeToDownloads(context, fileName, content)
    }

    private fun writeToDownloads(context: Context, fileName: String, content: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw Exception("Could not create file")
                resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { it.write(content.toByteArray()) }
            }
            Toast.makeText(context, "Exported: $fileName\n(Downloads folder)", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
