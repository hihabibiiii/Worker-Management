package com.example.workermanager.ui.screens.subscription

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
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
import com.example.workermanager.viewmodel.SubscriptionViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val queryState by viewModel.productQueryState.collectAsState()
    val entitlement by viewModel.entitlement.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isRestoring by viewModel.isRestoring.collectAsState()
    val verificationMessage by viewModel.verificationMessage.collectAsState()
    val isErrorMessage by viewModel.isErrorMessage.collectAsState()

    var selectedProductId by remember { mutableStateOf(BillingManager.PRODUCT_PRO_YEARLY) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WorkCraft Pro", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshEntitlement() },
                        enabled = !isRefreshing && !entitlement.isVerifying
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh status")
                        }
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
            // Active Subscription Card or Hero Banner
            if (entitlement.isPro) {
                ActiveProCard(
                    productId = entitlement.productId,
                    expiryTime = entitlement.expiryTime,
                    autoRenew = entitlement.autoRenewEnabled,
                    onManage = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))
                        context.startActivity(intent)
                    }
                )
            } else {
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
            }

            // Verification or Restoration Progress Banner
            if (entitlement.isVerifying || isRestoring) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Column {
                            val loadingText = if (entitlement.isVerifying) {
                                "Verifying subscription with Google Play server..."
                            } else {
                                "Restoring active purchases from Google Play..."
                            }
                            Text(loadingText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("This may take a few moments", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Status Message Banner
            verificationMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isErrorMessage) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isErrorMessage) Icons.Default.Info else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isErrorMessage) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = msg,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                            color = if (isErrorMessage) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        IconButton(onClick = { viewModel.dismissMessage() }, modifier = Modifier.size(20.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = if (isErrorMessage) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Product Query Status
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
                                "Google Play subscription products (${BillingManager.PRODUCT_PRO_MONTHLY} & ${BillingManager.PRODUCT_PRO_YEARLY}) will display localized pricing once active in Google Play Console.",
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

            // Plan Selection Cards (Available to subscribe or switch)
            Text(if (entitlement.isPro) "Manage Your Plan" else "Select Subscription Plan", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            PlanCard(
                title = "Monthly Pro",
                subtitle = "Billed monthly via Google Play",
                productId = BillingManager.PRODUCT_PRO_MONTHLY,
                targetBasePlanId = BillingManager.BASE_PLAN_MONTHLY,
                targetOfferId = BillingManager.OFFER_MONTHLY_TRIAL,
                isSelected = selectedProductId == BillingManager.PRODUCT_PRO_MONTHLY,
                isCurrentPlan = entitlement.isPro && entitlement.productId == BillingManager.PRODUCT_PRO_MONTHLY,
                queryState = queryState,
                onSelect = { selectedProductId = BillingManager.PRODUCT_PRO_MONTHLY }
            )

            PlanCard(
                title = "Yearly Pro",
                subtitle = "Billed annually via Google Play",
                productId = BillingManager.PRODUCT_PRO_YEARLY,
                targetBasePlanId = BillingManager.BASE_PLAN_YEARLY,
                targetOfferId = BillingManager.OFFER_YEARLY_TRIAL,
                isSelected = selectedProductId == BillingManager.PRODUCT_PRO_YEARLY,
                isCurrentPlan = entitlement.isPro && entitlement.productId == BillingManager.PRODUCT_PRO_YEARLY,
                queryState = queryState,
                badgeText = "Best Value",
                onSelect = { selectedProductId = BillingManager.PRODUCT_PRO_YEARLY }
            )

            // Subscribe Button
            Button(
                onClick = {
                    if (activity != null) {
                        val targetOfferId = if (selectedProductId == BillingManager.PRODUCT_PRO_MONTHLY) {
                            BillingManager.OFFER_MONTHLY_TRIAL
                        } else {
                            BillingManager.OFFER_YEARLY_TRIAL
                        }
                        val targetBasePlanId = if (selectedProductId == BillingManager.PRODUCT_PRO_MONTHLY) {
                            BillingManager.BASE_PLAN_MONTHLY
                        } else {
                            BillingManager.BASE_PLAN_YEARLY
                        }
                        val res = viewModel.launchPurchase(activity, selectedProductId, targetOfferId, targetBasePlanId)
                        if (res.responseCode != 0) {
                            Toast.makeText(context, "Cannot start purchase: ${res.debugMessage}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Activity reference not available", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !entitlement.isVerifying && !isRestoring
            ) {
                val buttonText = if (entitlement.isPro) {
                    if (entitlement.productId == selectedProductId) "Plan Currently Active" else "Switch Plan"
                } else {
                    "Subscribe Selected Plan"
                }
                Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Restore & Manage Subscriptions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.restorePurchases { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    enabled = !entitlement.isVerifying && !isRestoring
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
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
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
private fun ActiveProCard(
    productId: String?,
    expiryTime: String?,
    autoRenew: Boolean,
    onManage: () -> Unit
) {
    val planName = when (productId) {
        BillingManager.PRODUCT_PRO_YEARLY -> "Yearly Pro"
        BillingManager.PRODUCT_PRO_MONTHLY -> "Monthly Pro"
        else -> "WorkCraft Pro"
    }
    val formattedDate = formatIsoDate(expiryTime)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified Pro",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text("WorkCraft Pro Active", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text("ACTIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Plan: $planName", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                if (autoRenew) "Next renewal: $formattedDate" else "Expires on: $formattedDate",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onManage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Manage in Google Play")
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    subtitle: String,
    productId: String,
    targetBasePlanId: String? = null,
    targetOfferId: String? = null,
    isSelected: Boolean,
    isCurrentPlan: Boolean = false,
    queryState: ProductQueryState,
    badgeText: String? = null,
    onSelect: () -> Unit
) {
    val details = (queryState as? ProductQueryState.Success)?.productsMap?.get(productId)
    val offersForBasePlan = if (!targetBasePlanId.isNullOrBlank()) {
        details?.subscriptionOfferDetails?.filter { it.basePlanId == targetBasePlanId } ?: emptyList()
    } else {
        details?.subscriptionOfferDetails ?: emptyList()
    }

    val matchingOffer = if (!targetOfferId.isNullOrBlank()) {
        offersForBasePlan.find { it.offerId == targetOfferId }
            ?: offersForBasePlan.find { offer ->
                offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
            }
            ?: offersForBasePlan.firstOrNull()
            ?: details?.subscriptionOfferDetails?.find { it.offerId == targetOfferId }
            ?: details?.subscriptionOfferDetails?.firstOrNull()
    } else {
        offersForBasePlan.firstOrNull() ?: details?.subscriptionOfferDetails?.firstOrNull()
    }

    val pricingPhases = matchingOffer?.pricingPhases?.pricingPhaseList ?: emptyList()
    val trialPhase = pricingPhases.find { it.priceAmountMicros == 0L }
    val recurringPhase = pricingPhases.find { it.priceAmountMicros > 0L } ?: pricingPhases.lastOrNull()
    val hasTrial = trialPhase != null

    val billingPeriodSuffix = if (productId == BillingManager.PRODUCT_PRO_MONTHLY) "/month" else "/year"
    val priceText = if (recurringPhase != null) {
        "${recurringPhase.formattedPrice}$billingPeriodSuffix"
    } else {
        "Price loaded via Play Store"
    }

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
                    val displayBadge = if (isCurrentPlan) "Current Plan" else if (hasTrial) "28-Day Free Trial" else badgeText
                    displayBadge?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isCurrentPlan) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        ) {
                            Text(it, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                val trialNotice = if (hasTrial && recurringPhase != null) {
                    "28 days free, then $priceText"
                } else {
                    subtitle
                }
                Text(trialNotice, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 40.dp))
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

private fun formatIsoDate(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "Active"
    return try {
        val instant = Instant.parse(isoString)
        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        formatter.format(instant.atZone(zone))
    } catch (_: Exception) {
        isoString
    }
}
