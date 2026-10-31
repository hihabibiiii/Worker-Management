package com.example.workermanager.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workermanager.WorkerApplication
import com.example.workermanager.data.preference.AutoBackupPreferenceManager
import com.example.workermanager.data.preference.ThemeMode
import com.example.workermanager.util.BackupManager
import com.example.workermanager.util.BackupPayload
import com.example.workermanager.util.OwnershipValidationResult
import com.example.workermanager.util.ExcelExporter
import com.example.workermanager.ui.components.ProBadge
import com.example.workermanager.ui.components.ProUpgradeDialog
import com.example.workermanager.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenRecycleBin: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSubscription: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as WorkerApplication
    val scope = rememberCoroutineScope()

    val autoBackupPrefManager = remember { AutoBackupPreferenceManager(context) }
    val isAutoBackupEnabled by autoBackupPrefManager.isAutoBackupEnabled.collectAsState()
    val lastBackupTime by autoBackupPrefManager.lastBackupTime.collectAsState()
    val lastBackupStatus by autoBackupPrefManager.lastBackupStatus.collectAsState()
    val backupUri by autoBackupPrefManager.backupUri.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportedUri by remember { mutableStateOf<Uri?>(null) }
    val proAccessLevel by app.subscriptionRepository.proAccessLevel.collectAsState()
    val isProUnlocked = proAccessLevel.isProUnlocked
    var showProExportDialog by remember { mutableStateOf(false) }
    val themeMode by viewModel.themeMode.collectAsState()

    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var pendingRestorePayload by remember { mutableStateOf<BackupPayload?>(null) }
    var isManualSyncing by remember { mutableStateOf(false) }

    val selectFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Permission take error handling
            }
            autoBackupPrefManager.setBackupUri(it.toString())
            Toast.makeText(context, "Backup folder configured!", Toast.LENGTH_SHORT).show()
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                isBackingUp = true
                try {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        BackupManager.exportBackup(app.database, stream)
                    }
                    Toast.makeText(context, "Backup created successfully!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isBackingUp = false
                }
            }
        }
    }

    val openRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                isRestoring = true
                try {
                    val payload = context.contentResolver.openInputStream(it)?.use { stream ->
                        BackupManager.validateAndParseBackup(stream)
                    }
                    if (payload != null) {
                        val validation = BackupManager.validateBackupOwnership(payload)
                        if (validation is OwnershipValidationResult.Invalid) {
                            Toast.makeText(context, validation.userFacingErrorMessage, Toast.LENGTH_LONG).show()
                        } else {
                            pendingRestorePayload = payload
                        }
                    } else {
                        Toast.makeText(context, "Invalid backup file format.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Restore error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isRestoring = false
                }
            }
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (!isProUnlocked) {
            showProExportDialog = true
            return@rememberLauncherForActivityResult
        }
        uri?.let {
            scope.launch {
                isExporting = true
                try {
                    val (workers, attendance, payments) = viewModel.getAllData()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            ExcelExporter.export(workers, attendance, payments, outputStream, context)
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
            com.example.workermanager.ui.components.WorkCraftTopHeader(
                screenTitle = "Settings",
                screenIcon = Icons.Default.Settings,
                onBackClick = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                "Account", 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { 
                    Text("My Profile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) 
                },
                supportingContent = { 
                    Text(
                        "Manage display name and Google Account logout",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    ) 
                },
                leadingContent = { 
                    Icon(
                        imageVector = Icons.Default.Person, 
                        contentDescription = "Profile", 
                        tint = MaterialTheme.colorScheme.primary
                    ) 
                },
                modifier = Modifier.clickable { onOpenProfile() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(4.dp))

            ListItem(
                headlineContent = { 
                    Text("WorkCraft Pro Subscription", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) 
                },
                supportingContent = { 
                    Text(
                        "Manage monthly and yearly Pro subscription plans",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    ) 
                },
                leadingContent = { 
                    Icon(
                        imageVector = Icons.Default.Star, 
                        contentDescription = "Subscription", 
                        tint = MaterialTheme.colorScheme.primary
                    ) 
                },
                modifier = Modifier.clickable { onOpenSubscription() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(4.dp))

            ListItem(
                headlineContent = { 
                    Text("Sync Cloud Data Now", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) 
                },
                supportingContent = { 
                    Text(
                        "Upload new local records and download cloud changes",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    ) 
                },
                leadingContent = { 
                    if (isManualSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudSync, 
                            contentDescription = "Sync Now", 
                            tint = MaterialTheme.colorScheme.primary
                        ) 
                    }
                },
                modifier = Modifier.clickable(enabled = !isManualSyncing) {
                    scope.launch {
                        isManualSyncing = true
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null && currentUser.uid.isNotBlank()) {
                            val session = app.userSessionManager.getOrCreateSession(currentUser.uid)
                            val res = com.example.workermanager.data.remote.sync.engine.MasterSyncEngine.performFullSync(session.database)
                            isManualSyncing = false
                            if (res.isSuccess) {
                                Toast.makeText(context, "Cloud sync completed successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Sync error: ${res.exceptionOrNull()?.localizedMessage ?: "Network error"}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            isManualSyncing = false
                            Toast.makeText(context, "Please sign in to sync cloud data.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
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
                    Text("2.1.1", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Export to Excel", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        ProBadge()
                    }
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
                    if (!isProUnlocked) {
                        showProExportDialog = true
                    } else {
                        createDocumentLauncher.launch(ExcelExporter.generateFileName())
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            if (exportedUri != null && isProUnlocked) {
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
                "Automatic & Shared Backup", 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { 
                    Text("Automatic Backup", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) 
                },
                supportingContent = { 
                    Text("Periodically save backups to Documents/WorkCraftBackups", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium) 
                },
                leadingContent = { Icon(Icons.Default.Autorenew, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = {
                    Switch(
                        checked = isAutoBackupEnabled,
                        onCheckedChange = { autoBackupPrefManager.setAutoBackupEnabled(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            ListItem(
                headlineContent = { 
                    Text("Backup Folder", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) 
                },
                supportingContent = { 
                    Text(
                        if (backupUri != null) "Folder Configured (Documents/WorkCraftBackups)" else "Default: Documents/WorkCraftBackups",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    ) 
                },
                leadingContent = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable { selectFolderLauncher.launch(null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            ListItem(
                headlineContent = { 
                    Text("Backup Now", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) 
                },
                supportingContent = { 
                    val timeStr = if (lastBackupTime > 0) SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(lastBackupTime)) else "Never"
                    Text("Status: $lastBackupStatus | Last: $timeStr", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium) 
                },
                leadingContent = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable {
                    scope.launch {
                        val success = BackupManager.performAutoBackup(context, app.database, autoBackupPrefManager)
                        if (success) {
                            Toast.makeText(context, "Automatic Backup completed successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            val statusMsg = autoBackupPrefManager.lastBackupStatus.value
                            Toast.makeText(context, statusMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

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
                    Text("Backup Data", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) 
                },
                supportingContent = { 
                    Text("Save a backup copy of all application records", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium) 
                },
                leadingContent = { 
                    if (isBackingUp) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                modifier = Modifier.clickable(enabled = !isBackingUp) {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    createBackupLauncher.launch("WorkCraft_Backup_$timestamp.json")
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            ListItem(
                headlineContent = { 
                    Text("Restore Data", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) 
                },
                supportingContent = { 
                    Text("Restore records from a previously saved backup file", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium) 
                },
                leadingContent = { 
                    if (isRestoring) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.RestorePage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                modifier = Modifier.clickable(enabled = !isRestoring) {
                    openRestoreLauncher.launch(arrayOf("application/json", "*/*"))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

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

            Spacer(modifier = Modifier.height(96.dp))
        }

        if (pendingRestorePayload != null) {
            val payload = pendingRestorePayload!!
            AlertDialog(
                onDismissRequest = { pendingRestorePayload = null },
                title = { Text("Restore Backup?", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Are you sure you want to restore records from this backup file?",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Backup Date: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(payload.timestamp))}", style = MaterialTheme.typography.bodySmall)
                        Text("• Workers: ${payload.workers.size}", style = MaterialTheme.typography.bodySmall)
                        Text("• Sites: ${payload.sites.size}", style = MaterialTheme.typography.bodySmall)
                        Text("• Attendance Records: ${payload.attendance.size}", style = MaterialTheme.typography.bodySmall)
                        Text("• Payments: ${payload.payments.size}", style = MaterialTheme.typography.bodySmall)
                        Text("• Contracts: ${payload.contracts.size}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "A temporary safety backup of your current database will be created before restoration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val targetPayload = payload
                            pendingRestorePayload = null
                            scope.launch {
                                isRestoring = true
                                try {
                                    BackupManager.createSafetyBackupInternal(context, app.database)
                                    BackupManager.restoreBackup(app.database, targetPayload)
                                    Toast.makeText(context, "Data restored successfully!", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Restoration failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isRestoring = false
                                }
                            }
                        }
                    ) {
                        Text("Restore Data", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRestorePayload = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
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

        var isClearingData by remember { mutableStateOf(false) }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { if (!isClearingData) showDeleteDialog = false },
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
                        enabled = !isClearingData,
                        onClick = {
                            scope.launch {
                                isClearingData = true
                                val success = viewModel.clearAllData(app.database)
                                isClearingData = false
                                showDeleteDialog = false
                                if (success) {
                                    android.widget.Toast.makeText(context, "All data deleted successfully.", android.widget.Toast.LENGTH_SHORT).show()
                                    onBack()
                                } else {
                                    android.widget.Toast.makeText(context, "Failed to clear data. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        if (isClearingData) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFFC62828))
                        } else {
                            Text("Delete Everything", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isClearingData,
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }

        if (showProExportDialog) {
            ProUpgradeDialog(
                title = "Excel Export is a Pro Feature",
                description = "Upgrade to WorkCraft Pro to export complete site data, worker attendance, payments, and expense logs directly to Microsoft Excel (.xlsx).",
                onDismissRequest = { showProExportDialog = false },
                onUpgradeClick = {
                    showProExportDialog = false
                    onOpenSubscription()
                }
            )
        }
    }
}
