package com.example.workermanager.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.workermanager.viewmodel.WorkerMonthlyReport
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

object ShareUtils {
    fun shareWorkerReportToWhatsApp(context: Context, report: WorkerMonthlyReport) {
        try {
            // 1. Create a reports directory in the cache directory
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }

            // 2. Format file name
            val safeWorkerName = report.workerName.replace(Regex("[^a-zA-Z0-9]"), "_")
            val fileName = "${safeWorkerName}_Report_${LocalDate.now()}.csv"
            val file = File(reportsDir, fileName)

            // 3. Write CSV content (Handling UTF-8 for special characters)
            val csvContent = buildString {
                append("\uFEFF") // BOM for UTF-8 Excel support
                append("Worker Report\n")
                append("Worker Name,${escapeCsv(report.workerName)}\n")
                if (report.phone.isNotEmpty()) {
                    append("Mobile Number,${escapeCsv(report.phone)}\n")
                }
                append("Total Working Days,${report.presentDays + report.halfDays}\n")
                append("Present Days,${report.presentDays}\n")
                append("Half Days,${report.halfDays}\n")
                append("Absent Days,${report.absentDays}\n")
                append("Overtime Hours,${report.overtimeHours}\n")
                append("Total Earned,${report.totalEarned}\n")
                append("Total Paid,${report.totalPaid}\n")
                append("Remaining Due,${report.pendingBalance}\n")
            }

            FileOutputStream(file).use { fos ->
                fos.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            // 4. Get Content URI using FileProvider
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // 5. Create intent
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                
                // Set the text of the message if applicable
                val message = "Worker Report for ${report.workerName}"
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra(Intent.EXTRA_TITLE, message)
            }

            // If phone number is available, attempt to target WhatsApp directly
            if (report.phone.isNotBlank()) {
                val waNumber = report.phone.replace(Regex("[^0-9]"), "")
                // Add whatsapp package
                intent.setPackage("com.whatsapp")
                intent.putExtra("jid", "$waNumber@s.whatsapp.net")
                
                // Fallback to WA Business if normal is not installed or just intent chooser
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        intent.setPackage("com.whatsapp.w4b")
                        context.startActivity(intent)
                    } catch (e2: Exception) {
                        // WhatsApp not installed, use standard chooser
                        intent.setPackage(null)
                        intent.removeExtra("jid")
                        val chooser = Intent.createChooser(intent, "Share Report")
                        context.startActivity(chooser)
                    }
                }
            } else {
                // No phone number, standard chooser
                val chooser = Intent.createChooser(intent, "Share Report")
                context.startActivity(chooser)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export report: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun escapeCsv(value: String): String {
        var escaped = value
        if (escaped.contains("\"") || escaped.contains(",") || escaped.contains("\n")) {
            escaped = escaped.replace("\"", "\"\"")
            escaped = "\"$escaped\""
        }
        return escaped
    }
}
