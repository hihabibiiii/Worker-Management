package com.example.workermanager.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.ui.components.WorkCraftTopHeader
import com.example.workermanager.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val userProfile by authViewModel.userProfile.collectAsState()

    var displayName by remember(userProfile) { mutableStateOf(userProfile?.displayName ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val primaryBlue = Color(0xFF1677F8)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val screenBg = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF7F9FE)
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val textPrimary = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A)
    val textSecondary = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)

    Scaffold(
        topBar = {
            WorkCraftTopHeader(
                screenTitle = "My Profile",
                screenIcon = Icons.Default.Person,
                onBackClick = onBack
            )
        },
        containerColor = screenBg
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = Color(0xFFEBF3FF),
                        border = BorderStroke(2.dp, primaryBlue.copy(alpha = 0.25f))
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            val initial = displayName.trim().take(1).uppercase().ifBlank { "U" }
                            Text(
                                text = initial,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))

                    Spacer(modifier = Modifier.height(20.dp))

                    // Form Fields
                    // 1. Display Name
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = {
                            displayName = it
                            nameError = null
                        },
                        label = { Text("Display Name") },
                        isError = nameError != null,
                        supportingText = nameError?.let { err -> { Text(err, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Google Email (Read-Only)
                    OutlinedTextField(
                        value = userProfile?.email ?: "Google Account Unlinked",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Google Account Email") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Read Only",
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Save Button
                    Button(
                        onClick = {
                            if (displayName.isBlank()) {
                                nameError = "Display name cannot be empty"
                                return@Button
                            }
                            isSaving = true
                            authViewModel.updateProfile(
                                newDisplayName = displayName,
                                newPhotoUrl = userProfile?.photoUrl ?: "",
                                onSuccess = {
                                    isSaving = false
                                    Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                },
                                onError = { err ->
                                    isSaving = false
                                    nameError = err
                                }
                            )
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryBlue,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = if (isSaving) "Saving..." else "Save Changes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Logout Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { showLogoutDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC62828),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(23.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("Logout", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Notice Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFEBF3FF),
                border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = primaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Customizing your WorkCraft display name updates your local profile without changing your Google Account settings.",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) textPrimary else Color(0xFF1E3A8A),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // LOGOUT CONFIRMATION DIALOG
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to log out of your WorkCraft account?",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            authViewModel.logout()
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Yes, Logout", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showLogoutDialog = false },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("No, Stay Logged In", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    }
}
