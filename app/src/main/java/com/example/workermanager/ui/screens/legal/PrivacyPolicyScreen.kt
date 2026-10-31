package com.example.workermanager.ui.screens.legal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.ui.components.WorkCraftTopHeader

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryBlue = Color(0xFF1677F8)
    val screenBg = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF7F9FE)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A)
    val textSecondary = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)

    Scaffold(
        topBar = {
            WorkCraftTopHeader(
                screenTitle = "Privacy Policy",
                screenIcon = Icons.Default.PrivacyTip,
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
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("WorkCraft Privacy Policy", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryBlue)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Effective Date: 31 October 2026", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                    Text("Developer: Habibullah Salmani", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                    Text("Contact Email: hshabibullah@gmail.com", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = primaryBlue)
                }
            }

            // Legal Content Body Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LegalSection(
                        title = "1. Introduction",
                        content = "WorkCraft (\"Application\", \"we\", \"our\", or \"us\") is developed and operated by Habibullah Salmani. This Privacy Policy explains how we collect, use, store, and protect your information when you use the WorkCraft mobile application. By signing in, accessing, or using WorkCraft, you acknowledge that you have read and understood this Privacy Policy."
                    )

                    LegalSection(
                        title = "2. Information We Collect",
                        content = "WorkCraft collects information required to provide workforce management, site tracking, and payment recording services:\n\n" +
                                "• Account Information: Display name, email address, user ID, and profile picture retrieved securely through Google Sign-In.\n" +
                                "• Workforce Data: Worker names, contact phone numbers, worker category/trade, daily wage rates, and overtime rates entered by you.\n" +
                                "• Work Site Data: Site names, addresses, manager details, and site activity status.\n" +
                                "• Attendance Records: Daily attendance status (Present, Half-Day, Absent, Overtime hours), dates, and calculated daily wage amounts.\n" +
                                "• Payments & Financial Records: Wage payments, advances, bonuses, client contract details, and site expense records entered in the application.\n" +
                                "• Technical Diagnostics: Basic app diagnostic state and crash telemetry necessary for troubleshooting."
                    )

                    LegalSection(
                        title = "3. How We Use Information",
                        content = "We use the collected information solely to operate and improve WorkCraft functionality:\n\n" +
                                "• User Authentication: Authenticating your identity via Google Sign-In and securing your account.\n" +
                                "• Core Features: Managing worker records, organizing work sites, recording daily attendance, and generating payment calculations.\n" +
                                "• Multi-Device Cloud Sync: Synchronizing your business data securely across your signed-in Android devices using Firebase Cloud Firestore.\n" +
                                "• Subscription & Quota Management: Verifying subscription status through Google Play Billing and enforcing account plan limits.\n" +
                                "• Customer Support: Responding to support inquiries submitted to hshabibullah@gmail.com."
                    )

                    LegalSection(
                        title = "4. Data Storage & Architecture",
                        content = "WorkCraft uses an isolated, multi-layer data architecture:\n\n" +
                                "• Local Storage: Your active business data is stored locally on your device in an encrypted/app-private SQLite database managed by Android Room.\n" +
                                "• Cloud Infrastructure: Account authentication and cloud data synchronization are provided through Google Firebase (Firebase Authentication and Cloud Firestore).\n" +
                                "• User Isolation: Each authenticated user's data is stored in isolated, user-specific database files and Firestore document collections scoped strictly to their verified Firebase User ID."
                    )

                    LegalSection(
                        title = "5. Data Sharing & Third-Party Services",
                        content = "WorkCraft does NOT sell, rent, or trade your personal or business data to third parties or advertising networks. Information is processed exclusively through trusted infrastructure service providers:\n\n" +
                                "• Google Firebase: Provides secure Google Authentication, Cloud Firestore synchronization, and backend server infrastructure.\n" +
                                "• Google Play Billing: Processes in-app subscription purchases. Payment card details and financial transactions are handled directly by Google Play; WorkCraft does not collect or store credit card numbers."
                    )

                    LegalSection(
                        title = "6. Data Security",
                        content = "We implement industry-standard technical and organizational security measures to protect your data, including HTTPS/TLS encryption in transit, user-scoped Firestore security access controls, and Android app-private local storage isolation. While we take every reasonable precaution, no mobile transmission or electronic storage method is 100% immune to all potential security threats."
                    )

                    LegalSection(
                        title = "7. Data Retention & Account Deletion Requests",
                        content = "Your data is retained for as long as your account remains active or as needed to provide app services. You have complete control over your records:\n\n" +
                                "• Local Data Removal: You can delete individual sites, workers, or payments within the app or clear app data via Android Settings.\n" +
                                "• Full Account & Cloud Data Deletion: To request the permanent deletion of your WorkCraft account and all associated cloud data from Firebase servers, please email developer Habibullah Salmani at hshabibullah@gmail.com from your registered Google email address. Account deletion requests are processed within 14 business days."
                    )

                    LegalSection(
                        title = "8. Subscriptions and Payments",
                        content = "WorkCraft offers optional Pro subscription plans (Monthly and Yearly). All subscription billing, free trials, auto-renewals, and cancellations are processed by Google Play in accordance with the Google Play Terms of Service. You can manage or cancel your subscription at any time via Google Play Store > Payments & subscriptions."
                    )

                    LegalSection(
                        title = "9. Children's Privacy",
                        content = "WorkCraft is designed for professional contractors, business managers, and adult workforce administrators. WorkCraft does not knowingly collect or solicit personal information from children under the age of 13. If you believe a child has provided us with personal information, please contact hshabibullah@gmail.com to request data removal."
                    )

                    LegalSection(
                        title = "10. Policy Updates & Contact Information",
                        content = "We may update this Privacy Policy periodically to reflect app updates or regulatory changes. The updated policy will be accessible in the application with a revised effective date.\n\n" +
                                "Developer: Habibullah Salmani\n" +
                                "Support Email: hshabibullah@gmail.com\n" +
                                "App: WorkCraft"
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
internal fun LegalSection(
    title: String,
    content: String
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textPrimary = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A)
    val textSecondary = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF334155)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = content,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = textSecondary,
            lineHeight = 19.sp
        )
    }
}
