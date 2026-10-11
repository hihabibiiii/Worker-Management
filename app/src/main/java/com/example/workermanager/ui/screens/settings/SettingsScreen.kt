package com.example.workermanager.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workermanager.WorkerApplication
import com.example.workermanager.data.preference.ThemeMode
import com.example.workermanager.util.ExcelExporter
import com.example.workermanager.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenRecycleBin: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as WorkerApplication
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportedUri by remember { mutableStateOf<Uri?>(null) }
    val themeMode by viewModel.themeMode.collectAsState()

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
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                "Appearance", 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { 
                    Text("Theme", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) 
                },
                supportingContent = { 
                    Text(
                        when (themeMode) {
                            ThemeMode.SYSTEM -> "System Default (follow phone's theme)"
                            ThemeMode.LIGHT -> "Light theme"
                            ThemeMode.DARK -> "Dark theme"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    ) 
                },
                leadingContent = { 
                    Icon(
                        imageVector = Icons.Default.DarkMode, 
                        contentDescription = "Theme", 
                        tint = MaterialTheme.colorScheme.primary
                    ) 
                },
                modifier = Modifier.clickable { showThemeDialog = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Text(
                "General", 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            ListItem(
                headlineContent = { 
                    Text("App Version", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) 
                },
                supportingContent = { 
                    Text("1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium) 
                },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Text(
                "Reports & Export", 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { 
                    Text("Export to Excel", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) 
                },
                supportingContent = { 
                    Text("Generate a spreadsheet with all data", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium) 
                },
                leadingContent = { 
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                modifier = Modifier.clickable(enabled = !isExporting) {
                    createDocumentLauncher.launch(ExcelExporter.generateFileName())
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            if (exportedUri != null) {
                ListItem(
                    headlineContent = { 
                        Text("Share Last Export", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) 
                    },
                    supportingContent = { 
                        Text("Share the generated Excel file", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium) 
                    },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            putExtra(Intent.EXTRA_STREAM, exportedUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Excel Report"))
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            
            Text(
                "Data Management", 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { 
                    Text("Recycle Bin", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) 
                },
                supportingContent = { 
                    Text("View and restore deleted items", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium) 
                },
                leadingContent = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable { onOpenRecycleBin() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            
            ListItem(
                headlineContent = { 
                    Text("Clear All Data", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) 
                },
                supportingContent = { 
                    Text("Delete all workers, attendance and payments", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium) 
                },
                leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFC62828)) },
                modifier = Modifier.clickable { showDeleteDialog = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }

        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setThemeMode(mode)
                                        showThemeDialog = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (themeMode == mode),
                                    onClick = {
                                        viewModel.setThemeMode(mode)
                                        showThemeDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        mode.title,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        mode.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    }
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Everything?", fontWeight = FontWeight.Bold) },
                text = { 
                    Text(
                        "This will permanently delete ALL workers, attendance records, and payment history. This action cannot be undone.",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    ) 
                },
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
                        Text("Delete Everything", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    }
                }
            )
        }
    }
}
