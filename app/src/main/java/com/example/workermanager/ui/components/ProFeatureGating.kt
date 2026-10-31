package com.example.workermanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ProGoldStart = Color(0xFFFFB300)
val ProGoldEnd = Color(0xFFFF8F00)
val ProPurpleStart = Color(0xFF7C4DFF)
val ProPurpleEnd = Color(0xFF536DFE)

/**
 * Compact, eye-catching PRO badge pill to place next to locked feature names, tabs, or buttons.
 */
@Composable
fun ProBadge(
    modifier: Modifier = Modifier,
    text: String = "PRO"
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF59E0B),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Pro Feature",
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Full-screen lock placeholder shown when a Free user attempts to access a Pro-exclusive screen (such as Reports).
 */
@Composable
fun LockedProFeatureScreen(
    title: String = "Reports & Analytics are Pro Features",
    subtitle: String = "Unlock advanced weekly, monthly, and yearly reports, trend analytics, and unlimited exports with WorkCraft Pro.",
    onUpgradeClick: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            if (onBack != null) {
                WorkCraftTopHeader(
                    screenTitle = "WorkCraft Pro",
                    screenIcon = Icons.Default.WorkspacePremium,
                    onBackClick = onBack
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Premium Lock Icon Emblem
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ProGoldStart.copy(alpha = 0.2f), ProGoldEnd.copy(alpha = 0.2f))
                        )
                    )
                    .border(2.dp, ProGoldStart, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked Feature",
                    tint = ProGoldEnd,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            ProBadge(text = "WORKCRAFT PRO EXCLUSIVE")

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Feature Highlights Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "What you get with WorkCraft Pro:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    ProFeatureItem("Weekly, monthly & yearly worker attendance analytics")
                    ProFeatureItem("Site-wise and contractor-wise expense breakdowns")
                    ProFeatureItem("Automated cost trends and advance payment summaries")
                    ProFeatureItem("One-click complete Excel (.xlsx) data export")
                    ProFeatureItem("Advanced PDF worker statements & reports")
                    ProFeatureItem("Unlimited active sites (Free plan limited to 1)")
                    ProFeatureItem("Unlimited active workers (Free plan limited to 5)")
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Upgrade Action Button
            Button(
                onClick = onUpgradeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Upgrade to WorkCraft Pro",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (onBack != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Maybe Later",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ProFeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = ProGoldEnd,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Pop-up dialog displayed when a Free user taps on a Pro action (e.g. Export to Excel or PDF statement).
 */
@Composable
fun ProUpgradeDialog(
    title: String,
    description: String,
    onDismissRequest: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ProGoldStart.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "Pro Feature",
                    tint = ProGoldEnd,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismissRequest()
                    onUpgradeClick()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Upgrade Now", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Maybe Later")
            }
        }
    )
}

