package com.example.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BahanItem
import com.example.data.model.Pengaturan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DocumentExporterTest {

    private val sampleSettings = Pengaturan(
        judul = "STOK GUDANG SINAR MAS",
        kolom1 = "TOKO A",
        kolom2 = "TOKO B",
        kolom3 = "GUDANG C",
        kolom4 = "RUSAK",
        kolom5 = "TOTAL"
    )

    private val sampleItems = listOf(
        BahanItem(id = "1", nama = "PIPA 6mm", nilai1 = "10", nilai2 = "20", nilai3 = "30", nilai4 = "0", nilai5 = "60"),
        BahanItem(id = "2", nama = "HOLO 10x14", nilai1 = "5", nilai2 = "15", nilai3 = "25", nilai4 = "1", nilai5 = "46")
    )

    @Test
    fun exportToPdf_generatesPdfFileWithCorrectFolderAndMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = DocumentExporter.exportToPdf(context, sampleSettings, sampleItems)

        assertTrue(result.success)
        assertEquals("Download/SinarMasAluminium", result.folderPath)
        assertTrue(result.fileName.startsWith("Laporan_Stok_Sinar_Mas_"))
        assertTrue(result.fileName.endsWith(".pdf"))
        assertNotNull(result.file)
        assertTrue(result.file!!.exists())
        assertTrue(result.file!!.length() > 0)
    }

    @Test
    fun exportToExcel_generatesCsvWithUtf8BomAndHeaders() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = DocumentExporter.exportToExcel(context, sampleSettings, sampleItems)

        assertTrue(result.success)
        assertEquals("Download/SinarMasAluminium", result.folderPath)
        assertTrue(result.fileName.startsWith("Data_Stok_Sinar_Mas_"))
        assertTrue(result.fileName.endsWith(".csv"))
        assertNotNull(result.file)
        assertTrue(result.file!!.exists())

        val content = result.file!!.readText(Charsets.UTF_8)
        // Check for UTF-8 BOM
        assertTrue(content.startsWith("\uFEFF"))
        // Check title and columns
        assertTrue(content.contains("STOK GUDANG SINAR MAS"))
        assertTrue(content.contains("NO"))
        assertTrue(content.contains("NAMA BAHAN"))
        assertTrue(content.contains("TOKO A"))
        assertTrue(content.contains("PIPA 6mm"))
        assertTrue(content.contains("HOLO 10x14"))
    }
}
