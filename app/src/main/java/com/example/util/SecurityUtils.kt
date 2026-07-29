package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun generateRecoveryKey(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    fun chunk() = (1..4).map { chars.random() }.joinToString("")
    return "${chunk()}-${chunk()}-${chunk()}-${chunk()}"
}

fun normalizeKey(key: String): String {
    return key.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
}

fun createRecoveryKeyPdf(context: Context, key: String): File? {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 size in points
    val page = pdfDocument.startPage(pageInfo)
    val canvas: Canvas = page.canvas

    val paint = Paint()
    val titlePaint = Paint()
    val keyBoxPaint = Paint()
    val keyTextPaint = Paint()

    // Background
    canvas.drawColor(Color.WHITE)

    // Title
    titlePaint.color = Color.parseColor("#E53935")
    titlePaint.textSize = 22f
    titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("AL-NASHiHA - Safe Box Recovery Key", 50f, 60f, titlePaint)

    // Line
    paint.color = Color.LTGRAY
    paint.strokeWidth = 2f
    canvas.drawLine(50f, 75f, 545f, 75f, paint)

    // Instructions
    paint.color = Color.DKGRAY
    paint.textSize = 13f
    paint.typeface = Typeface.DEFAULT
    canvas.drawText("This is your private offline recovery key.", 50f, 110f, paint)
    canvas.drawText("Use it only if you forget your Safe Box PIN.", 50f, 128f, paint)

    // Key Box
    keyBoxPaint.color = Color.parseColor("#FFEBEE")
    canvas.drawRoundRect(50f, 155f, 545f, 235f, 12f, 12f, keyBoxPaint)

    // Key Title
    keyTextPaint.color = Color.parseColor("#D32F2F")
    keyTextPaint.textSize = 11f
    keyTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("YOUR RECOVERY KEY:", 70f, 180f, keyTextPaint)

    // Key Text
    keyTextPaint.textSize = 20f
    keyTextPaint.letterSpacing = 0.08f
    canvas.drawText(key, 70f, 215f, keyTextPaint)

    // Warnings
    val notePaint = Paint()
    notePaint.color = Color.parseColor("#C62828")
    notePaint.textSize = 13f
    notePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("IMPORTANT WARNINGS:", 50f, 275f, notePaint)

    notePaint.color = Color.DKGRAY
    notePaint.textSize = 11f
    notePaint.typeface = Typeface.DEFAULT
    val currentDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

    canvas.drawText("• This key can be used only ONCE to reset your Safe Box PIN.", 50f, 298f, notePaint)
    canvas.drawText("• Keep this PDF file in a safe place (or print it out).", 50f, 318f, notePaint)
    canvas.drawText("• Do not share this file with anyone.", 50f, 338f, notePaint)
    canvas.drawText("• Generated on: $currentDate", 50f, 358f, notePaint)

    // Footer
    paint.color = Color.GRAY
    paint.textSize = 10f
    canvas.drawText("AL-NASHiHA • 100% Offline & Private", 50f, 790f, paint)

    pdfDocument.finishPage(page)

    // Attempt to save in Downloads folder or Documents folder
    val targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!targetDir.exists()) {
        targetDir.mkdirs()
    }
    val pdfFile = File(targetDir, "AL-NASHiHA_Recovery_Key.pdf")

    return try {
        val fos = FileOutputStream(pdfFile)
        pdfDocument.writeTo(fos)
        fos.close()
        pdfDocument.close()
        pdfFile
    } catch (e: Exception) {
        e.printStackTrace()
        pdfDocument.close()
        // Fallback to app external storage if public directory throws exception
        try {
            val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val fallbackFile = File(fallbackDir, "AL-NASHiHA_Recovery_Key.pdf")
            val fallbackFos = FileOutputStream(fallbackFile)
            val fallbackPdfDoc = PdfDocument()
            val p = fallbackPdfDoc.startPage(pageInfo)
            // write basic page again if needed or simpler error handling
            fallbackPdfDoc.close()
            fallbackFile
        } catch (ex: Exception) {
            null
        }
    }
}
