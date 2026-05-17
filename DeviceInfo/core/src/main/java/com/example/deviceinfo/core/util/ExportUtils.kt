package com.example.deviceinfo.core.util

  import android.content.ContentValues
  import android.content.Context
  import android.graphics.Canvas
  import android.graphics.Color
  import android.graphics.Paint
  import android.graphics.pdf.PdfDocument
  import android.os.Build
  import android.os.Environment
  import android.provider.MediaStore
  import android.widget.Toast
  import java.io.File
  import java.io.FileOutputStream
  import java.io.OutputStream
  import java.text.SimpleDateFormat
  import java.util.Date
  import java.util.Locale

  object ExportUtils {

      fun exportToFile(context: Context, tabName: String, data: Map<String, String>) {
          val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
          val filename = "DeviceInfo_${tabName}_$ts.txt"
          val content = buildString {
              appendLine("CPU-A Device Info — $tabName")
              appendLine("Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
              appendLine("─".repeat(40))
              data.forEach { (k, v) -> appendLine("$k: $v") }
          }
          try {
              val stream = openOutputStream(context, filename, "text/plain", Environment.DIRECTORY_DOCUMENTS)
              stream?.use { it.write(content.toByteArray()) }
              Toast.makeText(context, "Exported: $filename", Toast.LENGTH_LONG).show()
          } catch (e: Exception) {
              Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
          }
      }

      fun exportToPdf(context: Context, tabName: String, data: Map<String, String>) {
          val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
          val filename = "DeviceInfo_${tabName}_$ts.pdf"

          val pageWidth = 595   // A4 width in points
          val pageHeight = 842  // A4 height in points
          val margin = 48f
          val lineHeight = 22f

          val doc = PdfDocument()
          var pageNum = 1
          var yPos = margin + 80f

          val paintTitle = Paint().apply {
              color = Color.parseColor("#006064")
              textSize = 20f
              isFakeBoldText = true
              isAntiAlias = true
          }
          val paintHeader = Paint().apply {
              color = Color.parseColor("#0097A7")
              textSize = 11f
              isFakeBoldText = true
              isAntiAlias = true
          }
          val paintLabel = Paint().apply {
              color = Color.parseColor("#546E7A")
              textSize = 12f
              isAntiAlias = true
          }
          val paintValue = Paint().apply {
              color = Color.parseColor("#1A1A2E")
              textSize = 12f
              isFakeBoldText = true
              isAntiAlias = true
          }
          val paintDivider = Paint().apply {
              color = Color.parseColor("#B2EBF2")
              strokeWidth = 1f
          }

          fun newPage(): Canvas {
              val pi = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum++).create()
              val page = doc.startPage(pi)
              return page.canvas
          }

          var canvas = newPage()
          var currentPage = doc.pages.last()

          // Title
          canvas.drawText("CPU-A Device Info", margin, margin + 30f, paintTitle)
          canvas.drawText("Tab: $tabName", margin, margin + 52f, paintHeader)
          val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
          canvas.drawText("Generated: $dateStr", margin, margin + 70f, paintLabel)
          canvas.drawLine(margin, margin + 76f, pageWidth - margin, margin + 76f, paintDivider)

          for ((key, value) in data) {
              if (yPos + lineHeight > pageHeight - margin) {
                  doc.finishPage(currentPage)
                  canvas = newPage()
                  currentPage = doc.pages.last()
                  yPos = margin + 20f
              }
              canvas.drawText(key, margin, yPos, paintLabel)
              val valueX = pageWidth / 2f
              canvas.drawText(value, valueX, yPos, paintValue)
              canvas.drawLine(margin, yPos + 4f, pageWidth - margin, yPos + 4f, paintDivider)
              yPos += lineHeight
          }
          doc.finishPage(currentPage)

          try {
              val stream = openOutputStream(context, filename, "application/pdf", Environment.DIRECTORY_DOWNLOADS)
              stream?.use { doc.writeTo(it) }
              Toast.makeText(context, "PDF saved: $filename", Toast.LENGTH_LONG).show()
          } catch (e: Exception) {
              Toast.makeText(context, "PDF failed: ${e.message}", Toast.LENGTH_SHORT).show()
          } finally {
              doc.close()
          }
      }

      private fun openOutputStream(context: Context, filename: String, mimeType: String, dir: String): OutputStream? {
          return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              val values = ContentValues().apply {
                  put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                  put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                  put(MediaStore.MediaColumns.RELATIVE_PATH, dir)
              }
              val collection = if (mimeType == "application/pdf")
                  MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
              else
                  MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
              context.contentResolver.insert(collection, values)?.let {
                  context.contentResolver.openOutputStream(it)
              }
          } else {
              @Suppress("DEPRECATION")
              val folder = Environment.getExternalStoragePublicDirectory(dir)
              folder.mkdirs()
              FileOutputStream(File(folder, filename))
          }
      }
  }