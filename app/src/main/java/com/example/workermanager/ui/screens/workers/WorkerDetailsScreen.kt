package com.example.workermanager.ui.screens.workers

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.workermanager.data.entity.*
import com.example.workermanager.util.PdfReportGenerator
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder
import java.time.LocalDate

enum class WorkerHistoryTab(val title: String) {
    SITE("Site History"),
    PAYMENT("Payment History"),
    ATTENDANCE("Attendance History")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDetailsScreen(
    workerId: Long,
    viewModel: WorkerViewModel,
    attendanceViewModel: AttendanceViewModel,
    paymentViewModel: PaymentViewModel,
    siteViewModel: SiteViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val worker by viewModel.getWorkerById(workerId).collectAsState(initial = null)
    val stats by viewModel.getWorkerStats(workerId).collectAsState(initial = WorkerStats(0, 0, 0, 0.0, 0.0, 0.0, 0.0))
    val attendanceHistory by attendanceViewModel.getAttendanceByWorker(workerId).collectAsState(initial = emptyList())
    val paymentHistory by paymentViewModel.getPaymentsByWorker(workerId).collectAsState(initial = emptyList())
    val siteHistory by viewModel.getWorkerSiteHistory(workerId).collectAsState(initial = emptyList())
    val sites by siteViewModel.allSites.collectAsState()

    var showTransferDialog by remember { mutableStateOf(false) }
    var paymentDialogType by remember { mutableStateOf<PaymentType?>(null) }
    var showShareReportDialog by remember { mutableStateOf(false) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(WorkerHistoryTab.ATTENDANCE) }

    var paymentToEdit by remember { mutableStateOf<PaymentEntity?>(null) }
    var paymentToDelete by remember { mutableStateOf<PaymentEntity?>(null) }

    val scope = rememberCoroutineScope()
    var tempCameraFilePath by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && !tempCameraFilePath.isNullOrBlank()) {
            val capturedFile = File(tempCameraFilePath!!)
            if (capturedFile.exists() && capturedFile.length() > 0) {
                viewModel.updateWorkerPhoto(workerId, capturedFile.absolutePath)
                Toast.makeText(context, "Photo captured successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val launchCamera = {
        try {
            val photoDir = File(context.filesDir, "worker_photos").apply { if (!exists()) mkdirs() }
            val photoFile = File(photoDir, "worker_${workerId}_${System.currentTimeMillis()}.jpg")
            tempCameraFilePath = photoFile.absolutePath
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch(Dispatchers.IO) {
                try {
                    val photoDir = File(context.filesDir, "worker_photos").apply { if (!exists()) mkdirs() }
                    val photoFile = File(photoDir, "worker_${workerId}_${System.currentTimeMillis()}.jpg")

                    context.contentResolver.openInputStream(selectedUri)?.use { input ->
                        photoFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (photoFile.exists() && photoFile.length() > 0) {
                        viewModel.updateWorkerPhoto(workerId, photoFile.absolutePath)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Photo updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to save photo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val screenBg = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF1F6FD)

    Scaffold(
        topBar = {
            WorkerDetailsHeader(
                workerName = worker?.name ?: "Worker Details",
                onBack = onBack,
                onTransferClick = { showTransferDialog = true }
            )
        },
        containerColor = screenBg
    ) { padding ->
        if (worker == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1976ED))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(screenBg),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. WORKER PROFILE CARD
                item {
                    WorkerProfileCard(
                        worker = worker!!,
                        sites = sites,
                        onPickPhoto = { showPhotoOptionsDialog = true }
                    )
                }

                // 2. ATTENDANCE SUMMARY CARDS
                item {
                    AttendanceSummaryRow(stats = stats)
                }

                // 3. FINANCIAL SUMMARY CARD
                item {
                    FinancialSummaryCard(stats = stats)
                }

                // 4. QUICK ACTION BUTTONS
                item {
                    QuickActionButtonsRow(
                        onAdvanceClick = { paymentDialogType = PaymentType.ADVANCE },
                        onPaymentClick = { paymentDialogType = PaymentType.PAYMENT },
                        onWhatsAppClick = { showShareReportDialog = true }
                    )
                }

                // 5. HISTORY TABS
                item {
                    HistoryTabsContainer(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }

                // 6. HISTORY CONTENT ITEMS
                if (selectedTab == WorkerHistoryTab.SITE) {
                    if (siteHistory.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No site history recorded.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF596580)
                                )
                            }
                        }
                    } else {
                        items(siteHistory) { history ->
                            val siteName = sites.find { it.id == history.siteId }?.name ?: "Unknown Site"
                            SiteHistoryCard(history = history, siteName = siteName)
                        }
                    }
                }

                if (selectedTab == WorkerHistoryTab.ATTENDANCE) {
                    if (attendanceHistory.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No attendance history recorded.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF596580)
                                )
                            }
                        }
                    } else {
                        items(attendanceHistory) { attendance ->
                            val siteName = sites.find { it.id == attendance.siteId }?.name ?: "Unknown Site"
                            AttendanceHistoryCard(attendance = attendance, siteName = siteName)
                        }
                    }
                }

                if (selectedTab == WorkerHistoryTab.PAYMENT) {
                    if (paymentHistory.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No payment history recorded.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF596580)
                                )
                            }
                        }
                    } else {
                        items(paymentHistory) { payment ->
                            val siteName = sites.find { it.id == payment.siteId }?.name ?: "Unknown Site"
                            PaymentHistoryCard(
                                payment = payment,
                                siteName = siteName,
                                onEdit = { paymentToEdit = payment },
                                onDelete = { paymentToDelete = payment }
                            )
                        }
                    }
                }
            }
        }

        // DIALOGS
        if (showTransferDialog && worker != null) {
            TransferWorkerDialog(
                worker = worker!!,
                sites = sites,
                onDismiss = { showTransferDialog = false },
                onConfirm = { newSiteId, date ->
                    viewModel.transferWorker(workerId, newSiteId, date)
                    showTransferDialog = false
                }
            )
        }

        if (paymentDialogType != null && worker != null) {
            RecordWorkerPaymentDialog(
                worker = worker!!,
                sites = sites,
                initialType = paymentDialogType!!,
                onDismiss = { paymentDialogType = null },
                onConfirm = { siteId, amount, method, type, date, note ->
                    paymentViewModel.addPayment(
                        workerId = worker!!.id,
                        siteId = siteId,
                        amount = amount,
                        method = method,
                        type = type,
                        date = date,
                        note = note,
                        onSuccess = {
                            paymentDialogType = null
                            if (method == PaymentMethod.UPI) {
                                com.example.workermanager.util.UpiUtils.launchUpiApp(context)
                            }
                        },
                        onError = { error ->
                            Toast.makeText(context, "Failed to save payment: ${error.localizedMessage ?: "Database error"}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }

        if (paymentToEdit != null) {
            com.example.workermanager.ui.screens.payments.EditPaymentDialog(
                payment = paymentToEdit!!,
                sites = sites,
                onDismiss = { paymentToEdit = null },
                onConfirm = { updatedPayment ->
                    paymentViewModel.updatePayment(
                        updatedPayment,
                        onSuccess = {
                            paymentToEdit = null
                            if (updatedPayment.method == PaymentMethod.UPI) {
                                com.example.workermanager.util.UpiUtils.launchUpiApp(context)
                            }
                        },
                        onError = { error ->
                            Toast.makeText(context, "Failed to update payment: ${error.localizedMessage ?: "Database error"}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }

        if (paymentToDelete != null) {
            AlertDialog(
                onDismissRequest = { paymentToDelete = null },
                title = { Text("Delete Payment?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete this payment?", color = MaterialTheme.colorScheme.onSurface) },
                confirmButton = {
                    TextButton(onClick = {
                        paymentViewModel.deletePayment(paymentToDelete!!)
                        paymentToDelete = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { paymentToDelete = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    }
                }
            )
        }

        if (showShareReportDialog && worker != null) {
            ShareWorkerReportDialog(
                worker = worker!!,
                stats = stats,
                attendanceHistory = attendanceHistory,
                paymentHistory = paymentHistory,
                sites = sites,
                onDismiss = { showShareReportDialog = false }
            )
        }

        if (showPhotoOptionsDialog && worker != null) {
            AlertDialog(
                onDismissRequest = { showPhotoOptionsDialog = false },
                title = { Text("Select Worker Photo", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                showPhotoOptionsDialog = false
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Choose Photo from Gallery", fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            onClick = {
                                showPhotoOptionsDialog = false
                                launchCamera()
                            },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Open Camera.", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!worker!!.photoPath.isNullOrBlank()) {
                            Surface(
                                onClick = {
                                    showPhotoOptionsDialog = false
                                    viewModel.updateWorkerPhoto(workerId, null)
                                    Toast.makeText(context, "Photo removed.", Toast.LENGTH_SHORT).show()
                                },
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Remove Photo", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showPhotoOptionsDialog = false }) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT 1: HEADER
// -----------------------------------------------------------------------------
@Composable
fun WorkerDetailsHeader(
    workerName: String,
    onBack: () -> Unit,
    onTransferClick: () -> Unit
) {
    Surface(
        color = Color(0xFF1976ED),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = workerName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            Surface(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onTransferClick() },
                shape = CircleShape,
                color = Color(0xFF3986E8),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Transfer",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Transfer",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT 2: WORKER PROFILE CARD
// -----------------------------------------------------------------------------
@Composable
fun WorkerProfileCard(
    worker: WorkerEntity,
    sites: List<SiteEntity>,
    onPickPhoto: () -> Unit
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val primaryTextColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF101426)
    val currentSiteName = sites.find { it.id == worker.currentSiteId }?.name ?: "Not Assigned"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color(0xFFE0E7F2))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with camera badge
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.clickable { onPickPhoto() }
            ) {
                WorkerAvatarLarge(
                    photoPath = worker.photoPath,
                    name = worker.name,
                    modifier = Modifier.size(110.dp)
                )

                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = Color(0xFF1976ED),
                    contentColor = Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(2.dp, cardBg)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change Photo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Worker Name & Active Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = worker.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(10.dp))

                ActiveStatusBadge(isActive = worker.isActive)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Worker Info Grid (Type, Phone, Site, Daily Wage)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WorkerDetailChip(
                        icon = Icons.Default.Badge,
                        label = "Type",
                        value = worker.workerType.name.lowercase().replaceFirstChar { it.uppercase() },
                        modifier = Modifier.weight(1f)
                    )

                    WorkerDetailChip(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = if (worker.phone.isNotBlank()) worker.phone else "No Phone",
                        modifier = Modifier.weight(1f),
                        onClick = if (worker.phone.isNotBlank()) {
                            {
                                try {
                                    val phoneNumber = worker.phone.trim()
                                    if (phoneNumber.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                                        context.startActivity(intent)
                                    }
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Unable to open dialer", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else null
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WorkerDetailChip(
                        icon = Icons.Default.LocationOn,
                        label = "Current Site",
                        value = currentSiteName,
                        modifier = Modifier.weight(1f)
                    )

                    val wageText = if (worker.overtimeRate > 0) {
                        "${Utils.formatCurrency(worker.dailyRate)}/day (${Utils.formatCurrency(worker.overtimeRate)}/h OT)"
                    } else {
                        "${Utils.formatCurrency(worker.dailyRate)}/day"
                    }
                    WorkerDetailChip(
                        icon = Icons.Default.Payments,
                        label = "Daily Wage",
                        value = wageText,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun WorkerAvatarLarge(
    photoPath: String?,
    name: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(photoPath) {
        if (!photoPath.isNullOrBlank()) {
            val file = File(photoPath)
            if (file.exists() && file.length() > 0) {
                try {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(photoPath, options)
                    var sampleSize = 1
                    while (options.outWidth / sampleSize > 400 || options.outHeight / sampleSize > 400) {
                        sampleSize *= 2
                    }
                    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    BitmapFactory.decodeFile(photoPath, decodeOptions)
                } catch (_: Exception) {
                    null
                }
            } else null
        } else null
    }

    Surface(
        modifier = modifier.clip(CircleShape),
        shape = CircleShape,
        color = Color(0xFFDFEBFF),
        contentColor = Color(0xFF1557C0),
        border = BorderStroke(2.dp, Color(0xFF1557C0).copy(alpha = 0.2f))
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = name.trim().take(1).uppercase(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1557C0)
                )
            }
        }
    }
}

@Composable
fun ActiveStatusBadge(isActive: Boolean) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bgColor = if (isActive) {
        if (isDark) Color(0xFF0F3818) else Color(0xFFDDFBE7)
    } else {
        if (isDark) Color(0xFF381014) else Color(0xFFFFE3E7)
    }
    val textColor = if (isActive) Color(0xFF087A2D) else Color(0xFFD7192D)
    val dotColor = if (isActive) Color(0xFF079B3B) else Color(0xFFD7192D)

    Surface(
        color = bgColor,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Text(
                text = if (isActive) "Active" else "Inactive",
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun WorkerDetailChip(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F6FD)
    val primaryText = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF101426)
    val secondaryText = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF596580)

    Surface(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape = RoundedCornerShape(14.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color(0xFF1976ED).copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFF1976ED),
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = secondaryText,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    fontSize = 12.sp,
                    color = primaryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT 3: ATTENDANCE SUMMARY CARDS
// -----------------------------------------------------------------------------
@Composable
fun AttendanceSummaryRow(stats: WorkerStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AttendanceSummaryCard(
            title = "Present",
            count = stats.presentDays.toString(),
            icon = Icons.Default.CheckCircle,
            bgColor = Color(0xFFF4FCF8),
            darkBgColor = Color(0xFF0F2618),
            iconBgColor = Color(0xFFE4F9ED),
            contentColor = Color(0xFF0A963D),
            borderColor = Color(0xFFD9F0E4),
            modifier = Modifier.weight(1f)
        )

        AttendanceSummaryCard(
            title = "Half Day",
            count = stats.halfDays.toString(),
            icon = Icons.Default.Schedule,
            bgColor = Color(0xFFFFFBF2),
            darkBgColor = Color(0xFF28200F),
            iconBgColor = Color(0xFFFFF1D2),
            contentColor = Color(0xFFD99000),
            borderColor = Color(0xFFF6EBD4),
            modifier = Modifier.weight(1f)
        )

        AttendanceSummaryCard(
            title = "Absent",
            count = stats.absentDays.toString(),
            icon = Icons.Default.Cancel,
            bgColor = Color(0xFFFFF6F7),
            darkBgColor = Color(0xFF2B1015),
            iconBgColor = Color(0xFFFFE3E7),
            contentColor = Color(0xFFD7192D),
            borderColor = Color(0xFFF7DDE1),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AttendanceSummaryCard(
    title: String,
    count: String,
    icon: ImageVector,
    bgColor: Color,
    darkBgColor: Color,
    iconBgColor: Color,
    contentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val background = if (isDark) darkBgColor else bgColor
    val border = if (isDark) contentColor.copy(alpha = 0.3f) else borderColor
    val labelColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF596580)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = background,
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (isDark) contentColor.copy(alpha = 0.2f) else iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor,
                maxLines = 1
            )

            Text(
                text = count,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT 4: FINANCIAL SUMMARY CARD
// -----------------------------------------------------------------------------
@Composable
fun FinancialSummaryCard(stats: WorkerStats) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF1B2735) else Color(0xFFEDF5FF)
    val borderColor = if (isDark) Color(0xFF2C3E55) else Color(0xFFC9DDFB)
    val labelColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF596580)
    val valueColor = if (isDark) Color(0xFF90CAF9) else Color(0xFF1557C0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FinancialRow(
                icon = Icons.Default.AccountBalanceWallet,
                iconColor = Color(0xFF1976ED),
                label = "Total Earned",
                value = Utils.formatCurrency(stats.totalEarned),
                labelColor = labelColor,
                valueColor = valueColor
            )

            FinancialRow(
                icon = Icons.Default.CreditCard,
                iconColor = Color(0xFF0A963D),
                label = "Total Paid",
                value = Utils.formatCurrency(stats.totalPaid),
                labelColor = labelColor,
                valueColor = valueColor
            )

            FinancialRow(
                icon = Icons.Default.Receipt,
                iconColor = Color(0xFF8B36E8),
                label = "Total Advance",
                value = Utils.formatCurrency(stats.totalAdvance),
                labelColor = labelColor,
                valueColor = valueColor
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = if (isDark) Color(0xFF2C3E55) else Color(0xFFD7E5F8),
                thickness = 1.dp
            )

            FinancialRow(
                icon = Icons.Default.PieChart,
                iconColor = Color(0xFFC91D2E),
                label = "Pending Balance",
                value = Utils.formatCurrency(stats.pendingBalance),
                labelColor = Color(0xFFC91D2E),
                valueColor = Color(0xFFC91D2E),
                isBold = true
            )
        }
    }
}

@Composable
fun FinancialRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
                color = labelColor
            )
        }

        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

// -----------------------------------------------------------------------------
// COMPONENT 5: QUICK ACTION BUTTONS
// -----------------------------------------------------------------------------
@Composable
fun QuickActionButtonsRow(
    onAdvanceClick: () -> Unit,
    onPaymentClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionButton(
            label = "ADVANCE",
            icon = Icons.Default.Payments,
            bgColor = Color(0xFF1976ED),
            onClick = onAdvanceClick,
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            label = "PAYMENT",
            icon = Icons.Default.AccountBalanceWallet,
            bgColor = Color(0xFF1976ED),
            onClick = onPaymentClick,
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            label = "WHATSAPP",
            icon = Icons.Default.Share,
            bgColor = Color(0xFF514B63),
            onClick = onWhatsAppClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = bgColor, contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT 6: HISTORY TABS
// -----------------------------------------------------------------------------
@Composable
fun HistoryTabsContainer(
    selectedTab: WorkerHistoryTab,
    onTabSelected: (WorkerHistoryTab) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val border = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color(0xFFE0E7F2)
    val unselectedBg = if (isDark) Color(0xFF2A2D3A) else Color(0xFFE8E6F1)
    val unselectedText = if (isDark) Color(0xFFA0A5BD) else Color(0xFF565A70)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = containerBg,
        border = BorderStroke(1.dp, border),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WorkerHistoryTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(CircleShape)
                        .clickable { onTabSelected(tab) },
                    shape = CircleShape,
                    color = if (isSelected) Color(0xFF1976ED) else unselectedBg
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = tab.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else unselectedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT 7: HISTORY CONTENT CARDS
// -----------------------------------------------------------------------------
@Composable
fun AttendanceHistoryCard(
    attendance: AttendanceEntity,
    siteName: String
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val borderColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color(0xFFE0E7F2)
    val primaryText = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF101426)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = Utils.formatDate(attendance.date),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Site",
                        tint = Color(0xFF1976ED),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = siteName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1976ED)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AttendanceStatusBadge(status = attendance.status)

                Text(
                    text = Utils.formatCurrency(attendance.amount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText
                )
            }
        }
    }
}

@Composable
fun AttendanceStatusBadge(status: AttendanceStatus) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val (bgColor, textColor) = when (status) {
        AttendanceStatus.PRESENT -> Pair(if (isDark) Color(0xFF0F3818) else Color(0xFFDDFBE7), Color(0xFF087A2D))
        AttendanceStatus.HALF_DAY -> Pair(if (isDark) Color(0xFF382C0F) else Color(0xFFFFF1D2), Color(0xFFD99000))
        AttendanceStatus.ABSENT -> Pair(if (isDark) Color(0xFF381014) else Color(0xFFFFE3E7), Color(0xFFD7192D))
        AttendanceStatus.OVERTIME -> Pair(if (isDark) Color(0xFF0F2638) else Color(0xFFE3F2FD), Color(0xFF1565C0))
    }

    Surface(
        color = bgColor,
        shape = CircleShape
    ) {
        Text(
            text = status.name.replace("_", " "),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SiteHistoryCard(
    history: WorkerSiteHistoryEntity,
    siteName: String
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val borderColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color(0xFFE0E7F2)
    val primaryText = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF101426)
    val secondaryText = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF596580)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF1976ED),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = siteName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText
                )
            }

            val dateText = if (history.endDate == null) {
                "${Utils.formatDate(history.startDate)} → Present"
            } else {
                "${Utils.formatDate(history.startDate)} → ${Utils.formatDate(history.endDate)}"
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = secondaryText,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = dateText,
                    fontSize = 12.sp,
                    color = secondaryText,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaymentHistoryCard(
    payment: PaymentEntity,
    siteName: String,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val borderColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color(0xFFE0E7F2)
    val primaryText = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF101426)
    val secondaryText = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF596580)

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { menuExpanded = true }
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = Utils.formatDate(payment.date),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                    Text(
                        text = "${payment.paymentType.name} · $siteName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryText
                    )
                    if (payment.note.isNotEmpty()) {
                        Text(
                            text = "Note: ${payment.note}",
                            fontSize = 12.sp,
                            color = primaryText,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = Color(0xFF1976ED).copy(alpha = 0.12f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = payment.method.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976ED),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = Utils.formatCurrency(payment.amount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A963D)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    menuExpanded = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = Color(0xFFC62828)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    menuExpanded = false
                    onDelete()
                }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// HELPER COMPOSABLES & DIALOGS
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferWorkerDialog(
    worker: WorkerEntity,
    sites: List<SiteEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, LocalDate) -> Unit
) {
    var selectedSiteId by remember { mutableStateOf<Long?>(null) }
    var transferDate by remember { mutableStateOf(LocalDate.now()) }
    var expanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            transferDate = LocalDate.of(year, month + 1, dayOfMonth)
        },
        transferDate.year,
        transferDate.monthValue - 1,
        transferDate.dayOfMonth
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Worker", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Worker: ${worker.name}", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = sites.find { it.id == selectedSiteId }?.name ?: "Select Site",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Transfer To") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        sites.filter { it.id != worker.currentSiteId }.forEach { site ->
                            DropdownMenuItem(
                                text = { Text(site.name, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedSiteId = site.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    "Date: ${Utils.formatDate(transferDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(onClick = { datePickerDialog.show() }) {
                    Text("Select Transfer Date", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedSiteId?.let { onConfirm(it, transferDate) } },
                enabled = selectedSiteId != null
            ) {
                Text("Transfer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordWorkerPaymentDialog(
    worker: WorkerEntity,
    sites: List<SiteEntity>,
    initialType: PaymentType,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double, PaymentMethod, PaymentType, LocalDate, String) -> Unit
) {
    var selectedSiteId by remember { mutableStateOf<Long>(worker.currentSiteId ?: sites.firstOrNull()?.id ?: 0L) }
    var amount by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    val selectedType = initialType
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf("") }
    var siteExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
        },
        selectedDate.year,
        selectedDate.monthValue - 1,
        selectedDate.dayOfMonth
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (selectedType == PaymentType.ADVANCE) "Record Advance for ${worker.name}" else "Record Payment for ${worker.name}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Type: ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        Text(
                            text = if (selectedType == PaymentType.ADVANCE) "Advance" else "Payment",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selectedType == PaymentType.ADVANCE) {
                                if (isDark) Color(0xFFFFB74D) else Color(0xFFEF6C00)
                            } else {
                                if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0)
                            }
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = siteExpanded,
                    onExpandedChange = { siteExpanded = !siteExpanded }
                ) {
                    OutlinedTextField(
                        value = sites.find { it.id == selectedSiteId }?.name ?: "Select Site",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Site") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = siteExpanded, onDismissRequest = { siteExpanded = false }) {
                        sites.forEach { site ->
                            DropdownMenuItem(
                                text = { Text(site.name, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedSiteId = site.id
                                    siteExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = Utils.formatDate(selectedDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date *") },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Payment Method", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMethod.entries.forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method.name, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            var isSaving by remember { mutableStateOf(false) }
            val buttonText = if (selectedMethod == PaymentMethod.UPI) "Save & Open UPI App" else "Save"
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull() ?: 0.0
                    if (selectedSiteId != 0L && amountVal > 0 && !isSaving) {
                        isSaving = true
                        onConfirm(selectedSiteId, amountVal, selectedMethod, selectedType, selectedDate, note)
                    }
                },
                enabled = !isSaving && selectedSiteId != 0L && (amount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareWorkerReportDialog(
    worker: WorkerEntity,
    stats: WorkerStats,
    attendanceHistory: List<AttendanceEntity>,
    paymentHistory: List<PaymentEntity>,
    sites: List<SiteEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var phoneInput by remember { mutableStateOf(worker.phone) }
    val currentSiteName = sites.find { it.id == worker.currentSiteId }?.name ?: "N/A"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Share Worker Report",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(
                    modifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Worker: ${worker.name}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Recipient Phone Number (WhatsApp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Report Summary Preview:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("• Site: $currentSiteName", style = MaterialTheme.typography.bodySmall)
                            Text("• Working Days: ${stats.presentDays + stats.halfDays} Days (Present: ${stats.presentDays}, Half: ${stats.halfDays}, Absent: ${stats.absentDays})", style = MaterialTheme.typography.bodySmall)
                            Text("• Total Earned: ${Utils.formatCurrency(stats.totalEarned)}", style = MaterialTheme.typography.bodySmall)
                            Text("• Total Paid: ${Utils.formatCurrency(stats.totalPaid)}", style = MaterialTheme.typography.bodySmall)
                            Text("• Pending Balance: ${Utils.formatCurrency(stats.pendingBalance)}", style = MaterialTheme.typography.bodySmall)
                            Text("• Records: ${attendanceHistory.size} Attendance entries, ${paymentHistory.size} Payment entries", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            shareWorkerReport(
                                context = context,
                                worker = worker,
                                phone = phoneInput,
                                currentSiteName = currentSiteName,
                                stats = stats,
                                attendanceHistory = attendanceHistory,
                                paymentHistory = paymentHistory,
                                sites = sites,
                                targetWhatsApp = true
                            )
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send on WhatsApp", fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            shareWorkerReport(
                                context = context,
                                worker = worker,
                                phone = phoneInput,
                                currentSiteName = currentSiteName,
                                stats = stats,
                                attendanceHistory = attendanceHistory,
                                paymentHistory = paymentHistory,
                                sites = sites,
                                targetWhatsApp = false
                            )
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF Report", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

fun shareWorkerReport(
    context: Context,
    worker: WorkerEntity,
    phone: String,
    currentSiteName: String,
    stats: WorkerStats,
    attendanceHistory: List<AttendanceEntity>,
    paymentHistory: List<PaymentEntity>,
    sites: List<SiteEntity>,
    targetWhatsApp: Boolean
) {
    try {
        val pdfFile = PdfReportGenerator.generateWorkerReportPdf(
            context = context,
            worker = worker,
            siteName = currentSiteName,
            stats = stats,
            attendanceHistory = attendanceHistory,
            paymentHistory = paymentHistory,
            sites = sites
        )

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val totalWorkingDays = stats.presentDays + stats.halfDays
        val messageText = """
            *Worker Statement: ${worker.name}*
            Phone: ${if (worker.phone.isBlank()) "N/A" else worker.phone}
            Current Site: $currentSiteName
            
            *Summary:*
            • Present Days: ${stats.presentDays}
            • Half Days: ${stats.halfDays}
            • Absent Days: ${stats.absentDays}
            • Total Working Days: $totalWorkingDays
            • Total Earned: ${Utils.formatCurrency(stats.totalEarned)}
            • Total Paid: ${Utils.formatCurrency(stats.totalPaid)}
            • Pending Balance: ${Utils.formatCurrency(stats.pendingBalance)}
            
            Attached complete PDF report for full details.
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, messageText)
            putExtra(Intent.EXTRA_SUBJECT, "Worker Statement - ${worker.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val formattedPhone = phone.replace(Regex("[^0-9]"), "")

        if (targetWhatsApp && formattedPhone.isNotBlank()) {
            val waPhone = if (formattedPhone.length == 10) "91$formattedPhone" else formattedPhone
            try {
                intent.setPackage("com.whatsapp")
                intent.putExtra("jid", "$waPhone@s.whatsapp.net")
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    intent.setPackage("com.whatsapp.w4b")
                    context.startActivity(intent)
                } catch (_: Exception) {
                    val waUri = Uri.parse("https://api.whatsapp.com/send?phone=$waPhone&text=${URLEncoder.encode(messageText, "UTF-8")}")
                    val waIntent = Intent(Intent.ACTION_VIEW, waUri)
                    try {
                        context.startActivity(waIntent)
                    } catch (_: Exception) {
                        intent.setPackage(null)
                        intent.removeExtra("jid")
                        val chooser = Intent.createChooser(intent, "Share Worker Report")
                        context.startActivity(chooser)
                    }
                }
            }
        } else {
            val chooser = Intent.createChooser(intent, "Share Worker Report")
            context.startActivity(chooser)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to generate report: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
