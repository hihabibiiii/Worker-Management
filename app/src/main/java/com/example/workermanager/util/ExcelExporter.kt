package com.example.workermanager.util

import android.util.Log
import com.example.workermanager.data.entity.AttendanceEntity
import com.example.workermanager.data.entity.PaymentEntity
import com.example.workermanager.data.entity.WorkerEntity
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ExcelExporter {
    private const val TAG = "ExcelExport"

    fun generateFileName(): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
        return "Worker_Attendance_${current.format(formatter)}.xlsx"
    }

    fun export(
        workers: List<WorkerEntity>,
        attendance: List<AttendanceEntity>,
        payments: List<PaymentEntity>,
        outputStream: OutputStream
    ) {
        Log.d(TAG, "Starting export. Workers: ${workers.size}, Attendance: ${attendance.size}, Payments: ${payments.size}")
        
        try {
            XSSFWorkbook().use { workbook ->
                // =========================
                // HEADER STYLE
                // =========================
                val headerStyle = workbook.createCellStyle().apply {
                    val font = workbook.createFont().apply {
                        bold = true
                        color = IndexedColors.WHITE.index
                        fontHeightInPoints = 11
                    }
                    setFont(font)
                    fillForegroundColor = IndexedColors.DARK_BLUE.index
                    fillPattern = FillPatternType.SOLID_FOREGROUND
                }

                // =========================
                // WORKERS SHEET
                // =========================
                val workerSheet = workbook.createSheet("Workers")
                val workerHeaders = listOf("ID", "Name", "Phone", "Daily Wage", "Status")
                val workerHeaderRow = workerSheet.createRow(0)
                workerHeaders.forEachIndexed { index, title ->
                    val cell = workerHeaderRow.createCell(index)
                    cell.setCellValue(title)
                    cell.setCellStyle(headerStyle)
                }

                workers.forEachIndexed { index, worker ->
                    val row = workerSheet.createRow(index + 1)
                    row.createCell(0).setCellValue(worker.id.toDouble())
                    row.createCell(1).setCellValue(worker.name)
                    row.createCell(2).setCellValue(worker.phone)
                    row.createCell(3).setCellValue(worker.dailyRate)
                    row.createCell(4).setCellValue(if (worker.isActive) "Active" else "Inactive")
                }
                
                // Fixed column widths
                workerSheet.setColumnWidth(0, 10 * 256)
                workerSheet.setColumnWidth(1, 25 * 256)
                workerSheet.setColumnWidth(2, 20 * 256)
                workerSheet.setColumnWidth(3, 15 * 256)
                workerSheet.setColumnWidth(4, 15 * 256)
                workerSheet.createFreezePane(0, 1)

                // =========================
                // ATTENDANCE SHEET
                // =========================
                val attendanceSheet = workbook.createSheet("Attendance")
                val attendanceHeaders = listOf("Date", "Worker Name", "Worker ID", "Status", "Amount")
                val attendanceHeaderRow = attendanceSheet.createRow(0)
                attendanceHeaders.forEachIndexed { index, title ->
                    val cell = attendanceHeaderRow.createCell(index)
                    cell.setCellValue(title)
                    cell.setCellStyle(headerStyle)
                }

                attendance.forEachIndexed { index, record ->
                    val row = attendanceSheet.createRow(index + 1)
                    row.createCell(0).setCellValue(Utils.formatDate(record.date))
                    val workerName = workers.find { it.id == record.workerId }?.name ?: "Unknown"
                    row.createCell(1).setCellValue(workerName)
                    row.createCell(2).setCellValue(record.workerId.toDouble())
                    row.createCell(3).setCellValue(record.status.name)
                    row.createCell(4).setCellValue(record.amount)
                }
                
                attendanceSheet.setColumnWidth(0, 15 * 256)
                attendanceSheet.setColumnWidth(1, 25 * 256)
                attendanceSheet.setColumnWidth(2, 10 * 256)
                attendanceSheet.setColumnWidth(3, 15 * 256)
                attendanceSheet.setColumnWidth(4, 15 * 256)
                attendanceSheet.createFreezePane(0, 1)

                // =========================
                // PAYMENTS SHEET
                // =========================
                val paymentSheet = workbook.createSheet("Payments")
                val paymentHeaders = listOf("Date", "Worker Name", "Worker ID", "Method", "Amount", "Note")
                val paymentHeaderRow = paymentSheet.createRow(0)
                paymentHeaders.forEachIndexed { index, title ->
                    val cell = paymentHeaderRow.createCell(index)
                    cell.setCellValue(title)
                    cell.setCellStyle(headerStyle)
                }

                payments.forEachIndexed { index, payment ->
                    val row = paymentSheet.createRow(index + 1)
                    row.createCell(0).setCellValue(Utils.formatDate(payment.date))
                    val workerName = workers.find { it.id == payment.workerId }?.name ?: "Unknown"
                    row.createCell(1).setCellValue(workerName)
                    row.createCell(2).setCellValue(payment.workerId.toDouble())
                    row.createCell(3).setCellValue(payment.method.name)
                    row.createCell(4).setCellValue(payment.amount)
                    row.createCell(5).setCellValue(payment.note)
                }
                
                paymentSheet.setColumnWidth(0, 15 * 256)
                paymentSheet.setColumnWidth(1, 25 * 256)
                paymentSheet.setColumnWidth(2, 10 * 256)
                paymentSheet.setColumnWidth(3, 15 * 256)
                paymentSheet.setColumnWidth(4, 15 * 256)
                paymentSheet.setColumnWidth(5, 30 * 256)
                paymentSheet.createFreezePane(0, 1)

                // =========================
                // SUMMARY SHEET
                // =========================
                val summarySheet = workbook.createSheet("Summary")
                val summaryHeaders = listOf("Worker Name", "Total Earned", "Total Paid", "Pending Balance")
                val summaryHeaderRow = summarySheet.createRow(0)
                summaryHeaders.forEachIndexed { index, title ->
                    val cell = summaryHeaderRow.createCell(index)
                    cell.setCellValue(title)
                    cell.setCellStyle(headerStyle)
                }

                workers.forEachIndexed { index, worker ->
                    val totalEarned = attendance.filter { it.workerId == worker.id }.sumOf { it.amount }
                    val totalPaid = payments.filter { it.workerId == worker.id }.sumOf { it.amount }
                    val balance = totalEarned - totalPaid

                    val row = summarySheet.createRow(index + 1)
                    row.createCell(0).setCellValue(worker.name)
                    row.createCell(1).setCellValue(totalEarned)
                    row.createCell(2).setCellValue(totalPaid)
                    row.createCell(3).setCellValue(balance)
                }
                
                summarySheet.setColumnWidth(0, 30 * 256)
                summarySheet.setColumnWidth(1, 20 * 256)
                summarySheet.setColumnWidth(2, 20 * 256)
                summarySheet.setColumnWidth(3, 20 * 256)
                summarySheet.createFreezePane(0, 1)

                workbook.write(outputStream)
                outputStream.flush()
                Log.d(TAG, "Excel export completed successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting Excel", e)
            throw e
        }
    }
}
