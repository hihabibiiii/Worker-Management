package com.example.workermanager.ui.screens.subscription

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.data.billing.BillingManager
import com.example.workermanager.data.billing.ProductQueryState
import com.example.workermanager.data.billing.PurchaseResultEvent
import com.example.workermanager.viewmodel.SubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val queryState by viewModel.productQueryState.collectAsState()
    var selectedProductId by remember { mutableStateOf(BillingManager.PRODUCT_PRO_YEARLY) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isStatusError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.purchaseEvents.collect { event ->
            when (event) {
                is PurchaseResultEvent.Success -> {
                    isStatusError = false
                    statusMessage = "Purchase successful! Token received. Awaiting secure server verification before granting entitlement."
                }
                is PurchaseResultEvent.Pending -> {
                    isStatusError = false
                    statusMessage = "Purchase is pending approval from Google Play."
                }
                is PurchaseResultEvent.UserCanceled -> {
                    isStatusError = false
                    statusMessage = "Purchase flow canceled."
                }
                is PurchaseResultEvent.Error -> {
                    isStatusError = true
                    statusMessage = "Purchase error (${event.responseCode}): ${event.debugMessage}"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WorkCraft Pro", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Upgrade to WorkCraft Pro", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Unlock Advanced PDF Reports, Analytics & Enterprise Features", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }

            // Status Message Banner
            statusMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isStatusError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isStatusError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = msg,
                            fontSize = 13.sp,
                            color = if (isStatusError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Query State Banner
            when (val state = queryState) {
                is ProductQueryState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Loading Google Play subscription products...")
                    }
                }
                is ProductQueryState.Empty -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Play Console Setup Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Google Play subscription products (workcraft_pro_monthly & workcraft_pro_yearly) have not been created in Google Play Console yet. Prices will be loaded automatically once products are active.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is ProductQueryState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = "Failed to query Play Store products: ${state.message}",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp
                        )
                    }
                }
                else -> {}
            }

            // Plan Selection Cards
            Text("Select Subscription Plan", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            PlanCard(
                title = "Monthly Pro",
                subtitle = "Billed monthly via Google Play",
                productId = BillingManager.PRODUCT_PRO_MONTHLY,
                targetOfferId = "monthly-free-trial",
                isSelected = selectedProductId == BillingManager.PRODUCT_PRO_MONTHLY,
                queryState = queryState,
                onSelect = { selectedProductId = BillingManager.PRODUCT_PRO_MONTHLY }
            )

            PlanCard(
                title = "Yearly Pro",
                subtitle = "Billed annually via Google Play",
                productId = BillingManager.PRODUCT_PRO_YEARLY,
                targetOfferId = "yearly-free-trial",
                isSelected = selectedProductId == BillingManager.PRODUCT_PRO_YEARLY,
                queryState = queryState,
                badgeText = "Best Value",
                onSelect = { selectedProductId = BillingManager.PRODUCT_PRO_YEARLY }
            )

            // Subscribe Button
            Button(
                onClick = {
                    if (activity != null) {
                        val targetOfferId = if (selectedProductId == BillingManager.PRODUCT_PRO_MONTHLY) "monthly-free-trial" else "yearly-free-trial"
                        val targetBasePlanId = if (selectedProductId == BillingManager.PRODUCT_PRO_MONTHLY) "monthly-plan" else "yearly-plan"
                        val res = viewModel.launchPurchase(activity, selectedProductId, targetOfferId, targetBasePlanId)
                        if (res.responseCode != 0) {
                            statusMessage = "Cannot start purchase: ${res.debugMessage}"
                            isStatusError = true
                        }
                    } else {
                        Toast.makeText(context, "Activity reference not available", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Subscribe Selected Plan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Restore & Manage Subscriptions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.restorePurchases { count ->
                            val msg = if (count > 0) {
                                "Found $count active purchase(s) on Google Play. Secure server-side verification is required to verify entitlement."
                            } else {
                                "No active subscription purchases found on Google Play."
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manage", fontSize = 12.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Feature Comparison Table
            Text("Free vs Pro Features", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            FeatureRow(feature = "Worker & Site Management", free = true, pro = true)
            FeatureRow(feature = "Attendance & Payment Tracking", free = true, pro = true)
            FeatureRow(feature = "Basic Export", free = true, pro = true)
            FeatureRow(feature = "Advanced PDF Reports", free = false, pro = true)
            FeatureRow(feature = "Full Excel Export", free = false, pro = true)
            FeatureRow(feature = "Business Analytics", free = false, pro = true)
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    subtitle: String,
    productId: String,
    targetOfferId: String,
    isSelected: Boolean,
    queryState: ProductQueryState,
    badgeText: String? = null,
    onSelect: () -> Unit
) {
    val details = (queryState as? ProductQueryState.Success)?.productsMap?.get(productId)
    val matchingOffer = details?.subscriptionOfferDetails?.find { it.offerId == targetOfferId }
        ?: details?.subscriptionOfferDetails?.firstOrNull()
    val pricingPhase = matchingOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()
    val priceText = pricingPhase?.formattedPrice ?: "Price unavailable from Play Store"
    val hasTrial = matchingOffer?.pricingPhases?.pricingPhaseList?.any { it.priceAmountMicros == 0L } == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isSelected, onClick = onSelect)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val displayBadge = if (hasTrial) "28 Days Free Trial" else badgeText
                    displayBadge?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(it, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 40.dp))
            }
            Text(priceText, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun FeatureRow(feature: String, free: Boolean, pro: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(feature, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (free) "Free" else "-", fontSize = 12.sp, color = if (free) MaterialTheme.colorScheme.primary else Color.Gray)
            Text(if (pro) "Pro" else "-", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (pro) MaterialTheme.colorScheme.primary else Color.Gray)
        }
    }
}
