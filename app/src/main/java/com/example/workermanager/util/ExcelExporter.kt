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
        outputStream: OutputStream,
        context: android.content.Context? = null
    ) {
        if (context != null) {
            val app = context.applicationContext as? com.example.workermanager.WorkerApplication
            val isPro = app?.subscriptionRepository?.canAccessProFeatures() == true
            if (!isPro) {
                throw SecurityException("Export to Excel requires an active WorkCraft Pro subscription.")
            }
        }
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

    fun generateContractorMaterialFileName(): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
        return "WorkCraft_Material_Report_${current.format(formatter)}.xlsx"
    }

    fun exportContractorMaterials(
        inventory: List<com.example.workermanager.viewmodel.ContractorMaterialStockModel>,
        siteStocks: List<com.example.workermanager.viewmodel.SiteMaterialStockModel>,
        transactions: List<com.example.workermanager.viewmodel.ContractorMaterialTxUiModel>,
        outputStream: OutputStream,
        context: android.content.Context? = null
    ) {
        if (context != null) {
            val app = context.applicationContext as? com.example.workermanager.WorkerApplication
            val isPro = app?.subscriptionRepository?.canAccessProFeatures() == true
            if (!isPro) {
                throw SecurityException("Contractor Material Excel export requires an active WorkCraft Pro subscription.")
            }
        }
        try {
            XSSFWorkbook().use { workbook ->
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

                // SHEET 1: INVENTORY SUMMARY
                val invSheet = workbook.createSheet("Complete Inventory")
                val invHeaders = listOf("Material Name", "Category", "Unit", "Total Added", "Available (Central)", "At Sites", "Broken Qty", "Lost Qty")
                val invHeaderRow = invSheet.createRow(0)
                invHeaders.forEachIndexed { index, title ->
                    val cell = invHeaderRow.createCell(index)
                    cell.setCellValue(title)
                    cell.setCellStyle(headerStyle)
                }

                inventory.forEachIndexed { index, item ->
                    val row = invSheet.createRow(index + 1)
                    row.createCell(0).setCellValue(item.material.name)
                    row.createCell(1).setCellValue(item.material.category.displayName)
                    row.createCell(2).setCellValue(item.material.unit.displayName)
                    row.createCell(3).setCellValue(item.totalAddedQuantity)
                    row.createCell(4).setCellValue(item.availableQuantity)
                    row.createCell(5).setCellValue(item.quantityAtSites)
                    row.createCell(6).setCellValue(item.brokenQuantity)
                    row.createCell(7).setCellValue(item.lostQuantity)
                }
                (0..7).forEach { invSheet.setColumnWidth(it, 20 * 256) }
                invSheet.createFreezePane(0, 1)

                // SHEET 2: SITE-WISE MATERIAL STOCK
                val siteSheet = workbook.createSheet("Site-wise Report")
                val siteHeaders = listOf("Site Name", "Material Name", "Unit", "Total Sent", "Total Returned", "Broken at Site", "Lost at Site", "Current Site Qty")
                val siteHeaderRow = siteSheet.createRow(0)
                siteHeaders.forEachIndexed { index, title ->
                    val cell = siteHeaderRow.createCell(index)
                    cell.setCellValue(title)
                    cell.setCellStyle(headerStyle)
                }

                siteStocks.forEachIndexed { index, item ->
                    val row = siteSheet.createRow(index + 1)
                    row.createCell(0).setCellValue(item.siteName)
                    row.createCell(1).setCellValue(item.materialName)
                    row.createCell(2).setCellValue(item.unit)
                    row.createCell(3).setCellValue(item.totalSent)
                    row.createCell(4).setCellValue(item.totalReturned)
                    row.createCell(5).setCellValue(item.brokenAtSite)
                    row.createCell(6).setCellValue(item.lostAtSite)
                    row.createCell(7).setCellValue(item.currentSiteQuantity)
                }
                (0..7).forEach { siteSheet.setColumnWidth(it, 20 * 256) }
                siteSheet.createFreezePane(0, 1)

                // SHEET 3: TRANSACTION HISTORY
                val txSheet = workbook.createSheet("Transaction History")
                val txHeaders = listOf("Date", "Material Name", "Transaction Type", "Site Name", "Quantity", "Notes")
                val txHeaderRow = txSheet.createRow(0)
                txHeaders.forEachIndexed { index, title ->
                    val cell = txHeaderRow.createCell(index)
                    cell.setCellValue(title)
                    cell.setCellStyle(headerStyle)
                }

                transactions.forEachIndexed { index, item ->
                    val row = txSheet.createRow(index + 1)
                    row.createCell(0).setCellValue(Utils.formatDate(item.transaction.transactionDate))
                    row.createCell(1).setCellValue(item.materialName)
                    row.createCell(2).setCellValue(item.transaction.transactionType.displayName)
                    row.createCell(3).setCellValue(item.siteName ?: "Central / N/A")
                    row.createCell(4).setCellValue("${item.transaction.quantity} ${item.transaction.unit}")
                    row.createCell(5).setCellValue(item.transaction.reasonOrNotes)
                }
                (0..5).forEach { txSheet.setColumnWidth(it, 22 * 256) }
                txSheet.createFreezePane(0, 1)

                workbook.write(outputStream)
                outputStream.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting Contractor Material Excel Report", e)
            throw e
        }
    }
}
