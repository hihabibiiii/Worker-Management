package com.example.workermanager.ui.screens.legal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
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
fun TermsAndConditionsScreen(
    onBack: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryBlue = Color(0xFF1677F8)
    val screenBg = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF7F9FE)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textSecondary = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)

    Scaffold(
        topBar = {
            WorkCraftTopHeader(
                screenTitle = "Terms & Conditions",
                screenIcon = Icons.Default.Gavel,
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
                    Text("WorkCraft Terms & Conditions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryBlue)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Effective Date: 31 October 2026", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                    Text("Developer: Habibullah Salmani", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                    Text("Support Contact: hshabibullah@gmail.com", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = primaryBlue)
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
                        content = "Welcome to WorkCraft (\"Application\"), a mobile workforce management tool developed and operated by Habibullah Salmani. These Terms & Conditions (\"Terms\") govern your access to and use of the WorkCraft application, features, and related services."
                    )

                    LegalSection(
                        title = "2. Acceptance of Terms",
                        content = "By downloading, installing, signing in, or using WorkCraft, you agree to be bound by these Terms and our Privacy Policy. If you do not agree to these Terms, you must not access or use the Application."
                    )

                    LegalSection(
                        title = "3. User Account and Authentication",
                        content = "To use WorkCraft, you must sign in using a valid Google Account authenticated via Firebase Authentication. You are solely responsible for maintaining the confidentiality and security of your Google credentials and for all activities conducted under your account."
                    )

                    LegalSection(
                        title = "4. User Responsibilities",
                        content = "You agree to use WorkCraft in compliance with all applicable local, national, and international laws. You are responsible for ensuring that all worker, site, attendance, and wage information entered into the Application is accurate and lawfully obtained."
                    )

                    LegalSection(
                        title = "5. Worker, Site and Attendance Management",
                        content = "WorkCraft provides digital organizational tools for tracking work sites, worker attendance, and wage calculations. WorkCraft acts strictly as an administrative record-keeping tool and is not an employer, employment agency, or payroll guarantor for your workers."
                    )

                    LegalSection(
                        title = "6. Payment and Contract Records",
                        content = "Financial records, wage advances, site expenses, and client contract entries stored in WorkCraft are entered at the user's discretion for informational tracking. You remain independently responsible for complying with applicable labor laws, statutory minimum wages, tax obligations, and contractual agreements."
                    )

                    LegalSection(
                        title = "7. Data Accuracy and Backup Responsibility",
                        content = "While WorkCraft provides automatic cloud synchronization via Firebase and local database storage via Room, you are encouraged to periodically generate local JSON backups via Settings to safeguard your operational data against device damage or loss."
                    )

                    LegalSection(
                        title = "8. Free and Pro Plans",
                        content = "WorkCraft offers a Free tier with core management features subject to plan limits (e.g., maximum 1 active site and 5 active workers) and an optional WorkCraft Pro subscription tier providing unlocked site/worker capacity, advanced PDF reports, and business analytics."
                    )

                    LegalSection(
                        title = "9. Subscription and Billing",
                        content = "WorkCraft Pro subscriptions (Monthly and Yearly) are processed exclusively through Google Play Billing. All pricing, billing periods, accepted payment methods, and applicable taxes are managed directly by Google Play in accordance with the Google Play Terms of Service."
                    )

                    LegalSection(
                        title = "10. Free Trial and Auto-Renewal",
                        content = "Eligible users may receive a 28-day free trial when subscribing to a WorkCraft Pro plan. Subscriptions automatically renew at the specified plan price at the end of each billing period unless canceled at least 24 hours before the renewal date via Google Play Store > Payments & subscriptions."
                    )

                    LegalSection(
                        title = "11. Acceptable Use",
                        content = "You agree not to modify, reverse engineer, decompile, or attempt to extract source code from WorkCraft, nor use the Application to distribute malicious software, fraudulent records, or unauthorized communications."
                    )

                    LegalSection(
                        title = "12. Intellectual Property",
                        content = "All rights, titles, and interests in and to WorkCraft, including app design, code, logos, trademarks, and user interface elements, are the exclusive intellectual property of developer Habibullah Salmani."
                    )

                    LegalSection(
                        title = "13. Service Availability",
                        content = "We strive to ensure continuous service availability. However, WorkCraft services may be temporarily interrupted for scheduled maintenance, updates, or technical reasons beyond our control. We do not guarantee uninterrupted or error-free operation."
                    )

                    LegalSection(
                        title = "14. Limitation of Liability",
                        content = "To the maximum extent permitted by applicable law, Habibullah Salmani shall not be liable for any indirect, incidental, consequential, or special damages, including lost profits or business interruption, arising out of your use or inability to use WorkCraft."
                    )

                    LegalSection(
                        title = "15. Account Suspension or Termination",
                        content = "We reserve the right to suspend or terminate account access if a user violates these Terms or engages in unauthorized or illegal activity. You may discontinue using the Application at any time."
                    )

                    LegalSection(
                        title = "16. Changes to Terms",
                        content = "We reserve the right to modify these Terms at any time. Updated Terms will be posted in the Application with a revised effective date. Continued use of WorkCraft following updates constitutes acceptance of the revised Terms."
                    )

                    LegalSection(
                        title = "17. Governing Law – India",
                        content = "These Terms & Conditions shall be governed by and construed in accordance with the laws of the Republic of India. Any disputes arising under these Terms shall be subject to the jurisdiction of the competent courts in India."
                    )

                    LegalSection(
                        title = "18. Contact Information",
                        content = "For questions, support, or legal inquiries regarding these Terms & Conditions, please contact:\n\n" +
                                "Developer: Habibullah Salmani\n" +
                                "Support Email: hshabibullah@gmail.com\n" +
                                "Application: WorkCraft"
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
