package com.example.deviceinfo.core.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportUtils {

    /** Export as plain .txt to Downloads. No dangerous permissions on API 29+. */
    fun exportToFile(context: Context, sectionName: String, data: Map<String, String>) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "DeviceInfo_${sectionName}_$timestamp.txt"
        val content = buildString {
            appendLine("=== CPU-A Device Info Export ===")
            appendLine("Section : $sectionName")
            appendLine("Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("Device  : ${Build.MODEL}")
            appendLine("Android : ${Build.VERSION.RELEASE}")
            appendLine()
            appendLine("--- $sectionName ---")
            data.forEach { (k, v) -> appendLine("$k: $v") }
        }
        writeToDownloads(context, fileName, "text/plain", content.toByteArray())
    }

    /** Export as .json to Downloads. Same MediaStore approach — no extra permission needed. */
    fun exportToJson(context: Context, sectionName: String, data: Map<String, String>) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "DeviceInfo_${sectionName}_$timestamp.json"
        val content = buildString {
            appendLine("{")
            appendLine("  \"app\": \"CPU-A Device Info\",")
            appendLine("  \"section\": \"$sectionName\",")
            appendLine("  \"exported\": \"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\",")
            appendLine("  \"device\": \"${Build.MODEL}\",")
            appendLine("  \"android\": \"${Build.VERSION.RELEASE}\",")
            appendLine("  \"data\": {")
            val entries = data.entries.toList()
            entries.forEachIndexed { index, (k, v) ->
                val comma = if (index < entries.size - 1) "," else ""
                appendLine("    ${jsonString(k)}: ${jsonString(v)}$comma")
            }
            appendLine("  }")
            append("}")
        }
        writeToDownloads(context, fileName, "application/json", content.toByteArray())
    }

    private fun jsonString(s: String): String {
        val escaped = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    private fun writeToDownloads(context: Context, fileName: String, mimeType: String, bytes: ByteArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw Exception("Could not create file")
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                FileOutputStream(File(dir, fileName)).use { it.write(bytes) }
            }
            Toast.makeText(context, "Exported: $fileName\n(Downloads folder)", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
