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

    /** Export all sections to a single TXT file */
    fun exportAllToFile(context: Context, allData: Map<String, Map<String, String>>) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val content = buildString {
            appendLine("=== CPU-A Device Info — Full Report ===")
            appendLine("Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("Device  : ${Build.MODEL}")
            appendLine("Android : ${Build.VERSION.RELEASE}")
            appendLine()
            allData.forEach { (section, data) ->
                appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                appendLine("  $section")
                appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                data.forEach { (k, v) -> appendLine("  $k: $v") }
                appendLine()
            }
        }
        writeToDownloads(context, "DeviceInfo_FullReport_$timestamp.txt", content, "text/plain")
    }

    /** Export a single section to TXT */
    fun exportToFile(context: Context, sectionName: String, data: Map<String, String>) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
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
        writeToDownloads(context, "DeviceInfo_${sectionName}_$timestamp.txt", content, "text/plain")
    }

    /**
     * NEW: Export all sections as JSON to Downloads.
     * Format: { "meta": {...}, "data": { "SOC": {...}, "Battery": {...} } }
     */
    fun exportAllToJson(context: Context, allData: Map<String, Map<String, String>>) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "DeviceInfo_FullReport_$timestamp.json"

        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"meta\": {\n")
        sb.append("    \"exported\": \"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\",\n")
        sb.append("    \"device\": ${jsonString(Build.MODEL)},\n")
        sb.append("    \"android\": ${jsonString(Build.VERSION.RELEASE)},\n")
        sb.append("    \"app\": \"CPU-A Device Info\"\n")
        sb.append("  },\n")
        sb.append("  \"data\": {\n")

        val sectionEntries = allData.entries.toList()
        sectionEntries.forEachIndexed { si, (section, data) ->
            sb.append("    ${jsonString(section)}: {\n")
            val dataEntries = data.entries.toList()
            dataEntries.forEachIndexed { di, (k, v) ->
                val comma = if (di < dataEntries.size - 1) "," else ""
                sb.append("      ${jsonString(k)}: ${jsonString(v)}$comma\n")
            }
            val sectionComma = if (si < sectionEntries.size - 1) "," else ""
            sb.append("    }$sectionComma\n")
        }

        sb.append("  }\n")
        sb.append("}\n")

        writeToDownloads(context, fileName, sb.toString(), "application/json")
    }

    /** Export single section as JSON */
    fun exportToJson(context: Context, sectionName: String, data: Map<String, String>) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"meta\": {\n")
        sb.append("    \"section\": ${jsonString(sectionName)},\n")
        sb.append("    \"exported\": \"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\",\n")
        sb.append("    \"device\": ${jsonString(Build.MODEL)},\n")
        sb.append("    \"android\": ${jsonString(Build.VERSION.RELEASE)}\n")
        sb.append("  },\n")
        sb.append("  \"data\": {\n")
        val entries = data.entries.toList()
        entries.forEachIndexed { i, (k, v) ->
            val comma = if (i < entries.size - 1) "," else ""
            sb.append("    ${jsonString(k)}: ${jsonString(v)}$comma\n")
        }
        sb.append("  }\n")
        sb.append("}\n")
        writeToDownloads(context, "DeviceInfo_${sectionName}_$timestamp.json", sb.toString(), "application/json")
    }

    /** Escape a string for JSON */
    private fun jsonString(s: String): String {
        val escaped = s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t")
        return "\"$escaped\""
    }

    private fun writeToDownloads(context: Context, fileName: String, content: String, mimeType: String) {
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
                resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                FileOutputStream(File(dir, fileName)).use { it.write(content.toByteArray()) }
            }
            Toast.makeText(context, "Exported: $fileName\n(Downloads folder)", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
