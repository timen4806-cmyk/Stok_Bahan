package com.example.util

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.BahanItem
import com.example.data.model.Pengaturan
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportResult(
    val success: Boolean,
    val fileName: String,
    val folderPath: String = "Download/SinarMasAluminium",
    val uri: Uri?,
    val file: File?,
    val mimeType: String,
    val message: String
)

object DocumentExporter {

    private const val EXPORT_FOLDER_NAME = "SinarMasAluminium"
    private val displayDateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Generates a multi-page A4 PDF report and saves it directly to Internal Storage (Download/SinarMasAluminium).
     */
    fun exportToPdf(
        context: Context,
        settings: Pengaturan,
        items: List<BahanItem>
    ): ExportResult {
        val document = PdfDocument()
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points

        val headerPaint = Paint().apply {
            color = Color.parseColor("#14213D")
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtextPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 9f
            isAntiAlias = true
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.parseColor("#2457D6")
            isAntiAlias = true
        }

        val tableHeaderTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val rowTextPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 7f
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val altRowPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }

        val rowsPerPage = 26
        val totalPages = maxOf(1, (items.size + rowsPerPage - 1) / rowsPerPage)
        val colWidths = floatArrayOf(25f, 150f, 75f, 75f, 75f, 75f, 75f)
        val startX = 20f

        val nowStr = displayDateFormat.format(Date())

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Header Banner
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 64f, headerPaint)
            canvas.drawText("SINAR MAS ALUMINIUM", startX, 28f, textPaint)
            canvas.drawText(
                "${settings.judul} — Dicetak: $nowStr",
                startX,
                46f,
                subtextPaint
            )

            // Table Columns Header
            val tableY = 80f
            val headerHeight = 22f
            canvas.drawRect(startX, tableY, pageWidth - startX, tableY + headerHeight, tableHeaderPaint)

            val headers = listOf(
                "NO",
                "NAMA BAHAN",
                settings.kolom1.ifBlank { "KOLOM 1" },
                settings.kolom2.ifBlank { "KOLOM 2" },
                settings.kolom3.ifBlank { "KOLOM 3" },
                settings.kolom4.ifBlank { "KOLOM 4" },
                settings.kolom5.ifBlank { "KOLOM 5" }
            )

            var currentX = startX
            for (i in headers.indices) {
                val headerText = headers[i]
                canvas.drawText(headerText, currentX + 3f, tableY + 15f, tableHeaderTextPaint)
                currentX += colWidths[i]
            }

            // Table Rows
            var currentY = tableY + headerHeight
            val rowHeight = 20f
            val startIndex = pageIndex * rowsPerPage
            val endIndex = minOf(startIndex + rowsPerPage, items.size)

            for (rowIndex in startIndex until endIndex) {
                val item = items[rowIndex]
                val isAlt = (rowIndex % 2 == 1)
                if (isAlt) {
                    canvas.drawRect(startX, currentY, pageWidth - startX, currentY + rowHeight, altRowPaint)
                }

                // Row border
                canvas.drawRect(startX, currentY, pageWidth - startX, currentY + rowHeight, borderPaint)

                val values = listOf(
                    (rowIndex + 1).toString(),
                    item.nama,
                    item.nilai1,
                    item.nilai2,
                    item.nilai3,
                    item.nilai4,
                    item.nilai5
                )

                var x = startX
                for (c in values.indices) {
                    val rawVal = values[c]
                    val safeVal = if (rawVal.length > 24) rawVal.take(22) + "…" else rawVal
                    canvas.drawText(safeVal, x + 3f, currentY + 13f, rowTextPaint)
                    x += colWidths[c]
                }

                currentY += rowHeight
            }

            // Footer
            val footerPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 8f
                isAntiAlias = true
            }
            canvas.drawText(
                "Halaman ${pageIndex + 1} dari $totalPages • Total Data: ${items.size} bahan",
                startX,
                pageHeight - 20f,
                footerPaint
            )

            document.finishPage(page)
        }

        val timeStamp = fileTimestampFormat.format(Date())
        val fileName = "Laporan_Stok_Sinar_Mas_$timeStamp.pdf"

        // 1. Save local copy for FileProvider & PrintManager fallback
        val cacheExportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val cacheFile = File(cacheExportsDir, fileName)
        FileOutputStream(cacheFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        // 2. Save directly to Internal Storage: Download/SinarMasAluminium
        var publicUri: Uri? = null
        val targetFolder = "Download/$EXPORT_FOLDER_NAME"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$EXPORT_FOLDER_NAME")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { output ->
                    FileInputStream(cacheFile).use { input ->
                        input.copyTo(output)
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                publicUri = uri
            }
        } else {
            // Android 9 and lower
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val customFolder = File(downloadsDir, EXPORT_FOLDER_NAME).apply { mkdirs() }
            val publicFile = File(customFolder, fileName)
            cacheFile.copyTo(publicFile, overwrite = true)
            MediaScannerConnection.scanFile(context, arrayOf(publicFile.absolutePath), arrayOf("application/pdf"), null)
            publicUri = Uri.fromFile(publicFile)
        }

        return ExportResult(
            success = true,
            fileName = fileName,
            folderPath = targetFolder,
            uri = publicUri ?: FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile),
            file = cacheFile,
            mimeType = "application/pdf",
            message = "PDF tersimpan di folder $targetFolder/$fileName"
        )
    }

    /**
     * Native Android PrintManager integration for instant PrintView and physical printing.
     */
    fun printReport(
        context: Context,
        settings: Pengaturan,
        items: List<BahanItem>
    ) {
        val exportResult = exportToPdf(context, settings, items)
        val pdfFile = exportResult.file ?: return
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("Laporan_Stok_Sinar_Mas.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    val inStream = FileInputStream(pdfFile)
                    val outStream = FileOutputStream(destination?.fileDescriptor)
                    val buf = ByteArray(1024)
                    var bytesRead: Int
                    while (inStream.read(buf).also { bytesRead = it } > 0) {
                        outStream.write(buf, 0, bytesRead)
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    inStream.close()
                    outStream.close()
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        printManager.print("Laporan Stok Sinar Mas", printAdapter, PrintAttributes.Builder().build())
    }

    /**
     * Exports inventory data to Excel CSV format with UTF-8 BOM, saved directly to Internal Storage (Download/SinarMasAluminium).
     */
    fun exportToExcel(
        context: Context,
        settings: Pengaturan,
        items: List<BahanItem>
    ): ExportResult {
        val timeStamp = fileTimestampFormat.format(Date())
        val fileName = "Data_Stok_Sinar_Mas_$timeStamp.csv"

        // 1. Write local cache copy
        val cacheExportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val cacheFile = File(cacheExportsDir, fileName)

        val writer = FileWriter(cacheFile)
        // UTF-8 BOM so Microsoft Excel & Google Sheets parse headers and characters properly
        writer.write("\uFEFF")

        // Title row
        writer.write("\"${settings.judul.replace("\"", "\"\"")}\"\n")
        writer.write("\"Dicetak: ${displayDateFormat.format(Date())}\"\n\n")

        // Header row
        val headers = listOf(
            "NO",
            "NAMA BAHAN",
            settings.kolom1.ifBlank { "KOLOM 1" },
            settings.kolom2.ifBlank { "KOLOM 2" },
            settings.kolom3.ifBlank { "KOLOM 3" },
            settings.kolom4.ifBlank { "KOLOM 4" },
            settings.kolom5.ifBlank { "KOLOM 5" }
        )
        writer.write(headers.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")

        // Data rows
        for (i in items.indices) {
            val item = items[i]
            val row = listOf(
                (i + 1).toString(),
                item.nama,
                item.nilai1,
                item.nilai2,
                item.nilai3,
                item.nilai4,
                item.nilai5
            )
            writer.write(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
        }

        writer.flush()
        writer.close()

        // 2. Save directly to Internal Storage: Download/SinarMasAluminium
        var publicUri: Uri? = null
        val targetFolder = "Download/$EXPORT_FOLDER_NAME"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$EXPORT_FOLDER_NAME")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { output ->
                    FileInputStream(cacheFile).use { input ->
                        input.copyTo(output)
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                publicUri = uri
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val customFolder = File(downloadsDir, EXPORT_FOLDER_NAME).apply { mkdirs() }
            val publicFile = File(customFolder, fileName)
            cacheFile.copyTo(publicFile, overwrite = true)
            MediaScannerConnection.scanFile(context, arrayOf(publicFile.absolutePath), arrayOf("text/csv"), null)
            publicUri = Uri.fromFile(publicFile)
        }

        return ExportResult(
            success = true,
            fileName = fileName,
            folderPath = targetFolder,
            uri = publicUri ?: FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile),
            file = cacheFile,
            mimeType = "text/csv",
            message = "Excel/CSV tersimpan di folder $targetFolder/$fileName"
        )
    }

    /**
     * Opens the exported file with user's installed PDF reader or Excel viewer.
     */
    fun openFile(context: Context, result: ExportResult) {
        val uriToOpen = if (result.file != null && result.file.exists()) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", result.file)
        } else {
            result.uri
        } ?: return

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriToOpen, result.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Buka Berkas ${result.fileName}"))
        } catch (_: Exception) {
            Toast.makeText(context, "Tidak ada aplikasi pembuka file untuk ${result.mimeType}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Opens Android system Downloads file manager.
     */
    fun openDownloadsFolder(context: Context) {
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "Buka aplikasi Pengelola File / File Saya > Folder Download > $EXPORT_FOLDER_NAME",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Shares file via FileProvider to WhatsApp, Telegram, Gmail, etc.
     */
    fun shareFile(context: Context, result: ExportResult, subject: String = "Laporan Sinar Mas Aluminium") {
        val uri: Uri = if (result.file != null && result.file.exists()) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", result.file)
        } else {
            result.uri
        } ?: return

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = result.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Bagikan Dokumen"))
    }

    /**
     * Legacy share method for backwards compatibility.
     */
    fun shareFile(context: Context, file: File, mimeType: String, subject: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Bagikan Dokumen"))
    }

    /**
     * Shares inventory analysis text to social media.
     */
    fun shareAnalysisText(context: Context, analysisText: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, analysisText)
            putExtra(Intent.EXTRA_SUBJECT, "Analisis Stok Sinar Mas Aluminium")
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Analisis"))
    }
}
