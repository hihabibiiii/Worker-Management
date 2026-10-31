package com.example.workermanager.ui.screens.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phonelink
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.R
import com.example.workermanager.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToTermsAndConditions: () -> Unit = {}
) {
    val context = LocalContext.current
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryBlue = Color(0xFF1677F8)

    val bgGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFEBF3FF), Color(0xFFF7F9FE)))
    }

    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A)
    val textSecondary = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)

    var localError by remember { mutableStateOf<String?>(null) }

    val appVersion = remember(context) {
        try {
            val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName?.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrBlank()) {
                authViewModel.signInWithIdToken(
                    idToken = idToken,
                    email = account.email ?: "",
                    displayName = account.displayName ?: (account.email?.substringBefore("@") ?: "User"),
                    photoUrl = account.photoUrl?.toString() ?: "",
                    onSuccess = onLoginSuccess,
                    onError = { err ->
                        authViewModel.setLoading(false)
                        localError = err
                    }
                )
            } else {
                authViewModel.setLoading(false)
                localError = "Failed to obtain Google ID Token from Google Sign-In intent."
            }
        } catch (e: ApiException) {
            authViewModel.setLoading(false)
            localError = when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign-in cancelled."
                10 -> "Google Sign-In configuration error (Developer Error 10). Please verify your SHA-1 fingerprint in Firebase Console."
                12500 -> "Google Sign-In failed (Code 12500). Please check your Google account in Settings."
                else -> "Google Sign-In failed (Code ${e.statusCode}): ${e.localizedMessage ?: "Intent error"}"
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { err ->
            localError = err
            Toast.makeText(context.applicationContext, err, Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // A. APP BRANDING HEADER
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color(0xFFEBF3FF),
                    border = BorderStroke(1.5.dp, primaryBlue.copy(alpha = 0.3f)),
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "WorkCraft Logo",
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "WorkCraft",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryBlue,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "Smart Work Management",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // B. WELCOME & MAIN LOGIN CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Welcome to WorkCraft",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Manage your entire workforce, track attendance and control payments — all in one place.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // C. FEATURE HIGHLIGHTS (3 Compact Cards)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FeatureHighlightCard(
                                icon = Icons.Default.Groups,
                                title = "Worker Management",
                                description = "Manage your workforce easily.",
                                isDark = isDark,
                                accentColor = primaryBlue
                            )

                            FeatureHighlightCard(
                                icon = Icons.Default.LocationOn,
                                title = "Site Management",
                                description = "Organize and track multiple work sites.",
                                isDark = isDark,
                                accentColor = Color(0xFF0BAA75)
                            )

                            FeatureHighlightCard(
                                icon = Icons.Default.Payments,
                                title = "Smart Payments",
                                description = "Track wages, advances and pending payments.",
                                isDark = isDark,
                                accentColor = Color(0xFF6938E8)
                            )
                        }

                        // Local Error Banner
                        localError?.let { errText ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = errText,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // D. GOOGLE SIGN-IN BUTTON
                        Button(
                            onClick = {
                                localError = null
                                authViewModel.signInWithGoogle(
                                    context = context,
                                    onSuccess = onLoginSuccess,
                                    onError = { err ->
                                        localError = err
                                    },
                                    onFallbackToIntent = {
                                        try {
                                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                .requestIdToken(AuthViewModel.DEFAULT_WEB_CLIENT_ID)
                                                .requestEmail()
                                                .build()
                                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                        } catch (e: Exception) {
                                            authViewModel.setLoading(false)
                                            localError = "Failed to launch Google Sign-In intent: ${e.localizedMessage}"
                                        }
                                    }
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryBlue,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_google_logo),
                                        contentDescription = "Google Logo",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Sign in with Google",
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Legal Disclaimer & Clickable Links
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "By continuing, you agree to our",
                                fontSize = 11.5.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "Privacy Policy",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryBlue,
                                    modifier = Modifier.clickable { onNavigateToPrivacyPolicy() }
                                )
                                Text(
                                    text = "  •  ",
                                    fontSize = 11.5.sp,
                                    color = textSecondary
                                )
                                Text(
                                    text = "Terms & Conditions",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryBlue,
                                    modifier = Modifier.clickable { onNavigateToTermsAndConditions() }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // E. TRUST SECTION
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrustBadge(icon = Icons.Default.Shield, label = "Secure Auth", isDark = isDark)
                            TrustBadge(icon = Icons.Default.CloudDone, label = "Cloud Sync", isDark = isDark)
                            TrustBadge(icon = Icons.Default.Phonelink, label = "Access Anywhere", isDark = isDark)
                        }
                    }
                }
            }

            // F. BOTTOM SECTION (App Name & Version)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WorkCraft • Smart Work Management",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary,
                    textAlign = TextAlign.Center
                )
                if (!appVersion.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "v$appVersion",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureHighlightCard(
    icon: ImageVector,
    title: String,
    description: String,
    isDark: Boolean,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = description,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun TrustBadge(
    icon: ImageVector,
    label: String,
    isDark: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF1677F8),
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
    }
}
