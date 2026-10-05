package com.example.workermanager.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.workermanager.WorkerApplication
import com.example.workermanager.util.ExcelExporter
import com.example.workermanager.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as WorkerApplication
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportedUri by remember { mutableStateOf<Uri?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let {
            scope.launch {
                isExporting = true
                try {
                    val (workers, attendance, payments) = viewModel.getAllData()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            ExcelExporter.export(workers, attendance, payments, outputStream)
                        }
                    }
                    exportedUri = it
                    Toast.makeText(context, "Excel file exported successfully", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isExporting = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("General", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            ListItem(
                headlineContent = { Text("App Version") },
                supportingContent = { Text("1.0.0") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Reports & Export", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("Export to Excel") },
                supportingContent = { Text("Generate a spreadsheet with all data") },
                leadingContent = { 
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Description, contentDescription = null)
                    }
                },
                modifier = Modifier.clickable(enabled = !isExporting) {
                    createDocumentLauncher.launch(ExcelExporter.generateFileName())
                }
            )

            if (exportedUri != null) {
                ListItem(
                    headlineContent = { Text("Share Last Export") },
                    supportingContent = { Text("Share the generated Excel file") },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            putExtra(Intent.EXTRA_STREAM, exportedUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Excel Report"))
                    }
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text("Data Management", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            
            ListItem(
                headlineContent = { Text("Clear All Data") },
                supportingContent = { Text("Delete all workers, attendance and payments") },
                leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { showDeleteDialog = true }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Everything?") },
                text = { Text("This will permanently delete ALL workers, attendance records, and payment history. This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                app.database.clearAllTables()
                                showDeleteDialog = false
                                onBack()
                            }
                        }
                    ) {
                        Text("Delete Everything", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
