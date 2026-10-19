package com.example.workermanager.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.R

val HeaderGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF0756C9), Color(0xFF0789F5))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkCraftTopHeader(
    screenTitle: String = "",
    screenIcon: ImageVector? = null,
    onNotificationClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderGradient)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // BRANDING ROW (Logo, WorkCraft Title, Subtitle, Notification Bell / Back)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Image(
                        painter = painterResource(id = R.drawable.dashlogo),
                        contentDescription = "WorkCraft Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Work", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                            Text("Craft", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFFFFD54F))
                        }
                        Text(
                            "Smart Workforce Management",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Notification Bell Icon with Badge
                IconButton(onClick = onNotificationClick) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = Color(0xFFFFD54F)) {
                                Text("•", color = Color.Black)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
