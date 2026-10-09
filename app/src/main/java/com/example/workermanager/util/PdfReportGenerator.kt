package com.example.workermanager.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.workermanager.data.entity.AttendanceEntity
import com.example.workermanager.data.entity.PaymentEntity
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.WorkerEntity
import com.example.workermanager.viewmodel.WorkerStats
import java.io.File
import java.io.FileOutputStream

object PdfReportGenerator {

    fun generateWorkerReportPdf(
        context: Context,
        worker: WorkerEntity,
        siteName: String,
        stats: WorkerStats,
        attendanceHistory: List<AttendanceEntity>,
        paymentHistory: List<PaymentEntity>,
        sites: List<SiteEntity>
    ): File {
        val pdfDocument = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val contentWidth = pageWidth - 2 * margin

        val titlePaint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            color = Color.rgb(74, 20, 140) // Purple accent
        }
        val headerPaint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
            color = Color.BLACK
        }
        val bodyPaint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 10f
            color = Color.DKGRAY
        }
        val boldBodyPaint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
            color = Color.BLACK
        }
        val linePaint = Paint().apply {
            isAntiAlias = true
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var y = margin + 20f

        fun checkPageOverflow(neededHeight: Float) {
            if (y + neededHeight > pageHeight - margin) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = margin + 20f
            }
        }

        // Header Title
        canvas.drawText("WORKCRAFT - WORKER STATEMENT", margin, y, titlePaint)
        y += 24f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 16f

        // Worker Profile Details
        canvas.drawText("WORKER INFORMATION", margin, y, headerPaint)
        y += 16f

        val details = listOf(
            "Worker Name: ${worker.name}" to "Mobile: ${if (worker.phone.isBlank()) "N/A" else worker.phone}",
            "Type: ${worker.workerType.name}" to "Current Site: $siteName",
            "Daily Wage: ${Utils.formatCurrency(worker.dailyRate)}" to "Overtime Rate: ${Utils.formatCurrency(worker.overtimeRate)}/hr"
        )
        for ((left, right) in details) {
            canvas.drawText(left, margin, y, bodyPaint)
            canvas.drawText(right, margin + contentWidth / 2, y, bodyPaint)
            y += 14f
        }
        y += 8f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 16f

        // Financial & Attendance Summary
        canvas.drawText("SUMMARY OVERVIEW", margin, y, headerPaint)
        y += 16f

        val totalWorkingDays = stats.presentDays + stats.halfDays
        val summaryRows = listOf(
            "Present Days: ${stats.presentDays}" to "Half Days: ${stats.halfDays}",
            "Absent Days: ${stats.absentDays}" to "Total Working Days: $totalWorkingDays",
            "Total Earned: ${Utils.formatCurrency(stats.totalEarned)}" to "Total Paid: ${Utils.formatCurrency(stats.totalPaid)}",
            "Pending Balance: ${Utils.formatCurrency(stats.pendingBalance)}" to "Overtime Hours: ${stats.overtimeHours} hrs"
        )
        for ((left, right) in summaryRows) {
            canvas.drawText(left, margin, y, boldBodyPaint)
            canvas.drawText(right, margin + contentWidth / 2, y, boldBodyPaint)
            y += 14f
        }
        y += 8f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 16f

        // Attendance History Table
        checkPageOverflow(60f)
        canvas.drawText("ATTENDANCE HISTORY", margin, y, headerPaint)
        y += 16f

        // Table Header
        canvas.drawRect(margin, y, pageWidth - margin, y + 18f, Paint().apply { color = Color.parseColor("#F0F0F0") })
        canvas.drawText("Date", margin + 6f, y + 12f, boldBodyPaint)
        canvas.drawText("Site", margin + 90f, y + 12f, boldBodyPaint)
        canvas.drawText("Status", margin + 280f, y + 12f, boldBodyPaint)
        canvas.drawText("Amount", margin + 400f, y + 12f, boldBodyPaint)
        y += 22f

        if (attendanceHistory.isEmpty()) {
            canvas.drawText("No attendance records found.", margin + 6f, y, bodyPaint)
            y += 16f
        } else {
            for (att in attendanceHistory) {
                checkPageOverflow(18f)
                val attSite = sites.find { it.id == att.siteId }?.name ?: "Unknown"
                canvas.drawText(Utils.formatDate(att.date), margin + 6f, y, bodyPaint)
                canvas.drawText(attSite.take(25), margin + 90f, y, bodyPaint)
                canvas.drawText(att.status.name, margin + 280f, y, bodyPaint)
                canvas.drawText(Utils.formatCurrency(att.amount), margin + 400f, y, bodyPaint)
                y += 16f
            }
        }
        y += 10f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 16f

        // Payment & Advance History Table
        checkPageOverflow(60f)
        canvas.drawText("PAYMENT & ADVANCE HISTORY", margin, y, headerPaint)
        y += 16f

        canvas.drawRect(margin, y, pageWidth - margin, y + 18f, Paint().apply { color = Color.parseColor("#F0F0F0") })
        canvas.drawText("Date", margin + 6f, y + 12f, boldBodyPaint)
        canvas.drawText("Type / Method", margin + 90f, y + 12f, boldBodyPaint)
        canvas.drawText("Note / Site", margin + 250f, y + 12f, boldBodyPaint)
        canvas.drawText("Amount", margin + 400f, y + 12f, boldBodyPaint)
        y += 22f

        if (paymentHistory.isEmpty()) {
            canvas.drawText("No payment records found.", margin + 6f, y, bodyPaint)
            y += 16f
        } else {
            for (pmt in paymentHistory) {
                checkPageOverflow(18f)
                val pmtSite = sites.find { it.id == pmt.siteId }?.name ?: ""
                val noteOrSite = pmt.note.ifBlank { pmtSite }.take(22)
                canvas.drawText(Utils.formatDate(pmt.date), margin + 6f, y, bodyPaint)
                canvas.drawText("${pmt.paymentType.name} (${pmt.method.name})", margin + 90f, y, bodyPaint)
                canvas.drawText(noteOrSite, margin + 250f, y, bodyPaint)
                canvas.drawText(Utils.formatCurrency(pmt.amount), margin + 400f, y, bodyPaint)
                y += 16f
            }
        }

        pdfDocument.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        val safeName = worker.name.replace(Regex("[^a-zA-Z0-9]"), "_")
        val pdfFile = File(reportsDir, "${safeName}_Report.pdf")

        FileOutputStream(pdfFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        return pdfFile
    }
}
