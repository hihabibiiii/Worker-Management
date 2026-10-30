package com.example.workermanager.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BillingConnectionState {
    object Disconnected : BillingConnectionState()
    object Connecting : BillingConnectionState()
    object Connected : BillingConnectionState()
    data class Error(val code: Int, val message: String) : BillingConnectionState()
}

sealed class ProductQueryState {
    object Idle : ProductQueryState()
    object Loading : ProductQueryState()
    data class Success(val productsMap: Map<String, ProductDetails>) : ProductQueryState()
    object Empty : ProductQueryState()
    data class Error(val code: Int, val message: String) : ProductQueryState()
}

sealed class PurchaseResultEvent {
    data class Success(val purchaseToken: String, val productId: String, val orderId: String?) : PurchaseResultEvent()
    data class Pending(val productId: String) : PurchaseResultEvent()
    object UserCanceled : PurchaseResultEvent()
    data class Error(val responseCode: Int, val debugMessage: String) : PurchaseResultEvent()
}

class BillingManager(
    context: Context,
    private val externalScope: CoroutineScope
) : PurchasesUpdatedListener, BillingClientStateListener {

    private val _connectionState = MutableStateFlow<BillingConnectionState>(BillingConnectionState.Disconnected)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _productQueryState = MutableStateFlow<ProductQueryState>(ProductQueryState.Idle)
    val productQueryState: StateFlow<ProductQueryState> = _productQueryState.asStateFlow()

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap.asStateFlow()

    private val _purchaseEvents = MutableSharedFlow<PurchaseResultEvent>()
    val purchaseEvents: SharedFlow<PurchaseResultEvent> = _purchaseEvents.asSharedFlow()

    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null

    private val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .build()

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(pendingPurchasesParams)
        .build()

    fun startConnection() {
        if (_connectionState.value == BillingConnectionState.Connected ||
            _connectionState.value == BillingConnectionState.Connecting) {
            return
        }
        _connectionState.value = BillingConnectionState.Connecting
        Log.d(TAG, "Starting BillingClient connection...")
        billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "BillingClient connected successfully.")
            reconnectAttempt = 0
            _connectionState.value = BillingConnectionState.Connected
            querySubscriptionProducts()
        } else {
            val err = "Billing setup failed: code=${billingResult.responseCode}, msg=${billingResult.debugMessage}"
            Log.e(TAG, err)
            _connectionState.value = BillingConnectionState.Error(billingResult.responseCode, billingResult.debugMessage)
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.w(TAG, "BillingClient service disconnected.")
        _connectionState.value = BillingConnectionState.Disconnected
        scheduleReconnectionWithBackoff()
    }

    private fun scheduleReconnectionWithBackoff() {
        if (reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Maximum BillingClient reconnect attempts ($MAX_RECONNECT_ATTEMPTS) reached.")
            return
        }
        reconnectJob?.cancel()
        reconnectJob = externalScope.launch {
            val delayMillis = BASE_RECONNECT_DELAY_MS * (1 shl reconnectAttempt)
            reconnectAttempt++
            Log.d(TAG, "Scheduling BillingClient reconnect attempt $reconnectAttempt in ${delayMillis}ms")
            delay(delayMillis)
            startConnection()
        }
    }

    fun querySubscriptionProducts() {
        if (_connectionState.value != BillingConnectionState.Connected) {
            Log.w(TAG, "Cannot query products: BillingClient not connected.")
            return
        }

        _productQueryState.value = ProductQueryState.Loading

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PRO_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PRO_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetailsList = queryProductDetailsResult.productDetailsList
                val detailsMap = productDetailsList.associateBy { it.productId }
                _productDetailsMap.value = detailsMap

                if (detailsMap.isEmpty()) {
                    _productQueryState.value = ProductQueryState.Empty
                    Log.i(TAG, "Product query returned empty list (Play Console products not configured yet).")
                } else {
                    _productQueryState.value = ProductQueryState.Success(detailsMap)
                    Log.d(TAG, "Successfully queried ${detailsMap.size} subscription products from Play Console.")
                }
            } else {
                val errMsg = billingResult.debugMessage.ifBlank { "Error query code ${billingResult.responseCode}" }
                _productQueryState.value = ProductQueryState.Error(billingResult.responseCode, errMsg)
                Log.e(TAG, "Error querying product details: $errMsg")
            }
        }
    }

    fun launchPurchaseFlow(
        activity: Activity,
        productId: String,
        targetOfferId: String? = null,
        targetBasePlanId: String? = null,
        obfuscatedAccountId: String? = null
    ): BillingResult {
        val productDetails = _productDetailsMap.value[productId]
        if (productDetails == null) {
            Log.e(TAG, "Cannot launch purchase flow: ProductDetails for $productId not available.")
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
                .setDebugMessage("ProductDetails not available")
                .build()
        }

        val offerDetailsList = productDetails.subscriptionOfferDetails
        val basePlanOffers = if (!targetBasePlanId.isNullOrBlank()) {
            offerDetailsList?.filter { it.basePlanId == targetBasePlanId } ?: emptyList()
        } else {
            offerDetailsList ?: emptyList()
        }

        val selectedOffer = if (!targetOfferId.isNullOrBlank()) {
            basePlanOffers.find { it.offerId == targetOfferId }
                ?: basePlanOffers.find { offer ->
                    offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
                }
                ?: basePlanOffers.firstOrNull()
                ?: offerDetailsList?.find { it.offerId == targetOfferId }
                ?: offerDetailsList?.firstOrNull()
        } else {
            basePlanOffers.firstOrNull() ?: offerDetailsList?.firstOrNull()
        }

        val offerToken = selectedOffer?.offerToken
        if (offerToken.isNullOrBlank()) {
            Log.e(TAG, "Cannot launch purchase flow: OfferToken for $productId unavailable.")
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR)
                .setDebugMessage("Offer token unavailable")
                .build()
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParamsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)

        if (!obfuscatedAccountId.isNullOrBlank()) {
            billingFlowParamsBuilder.setObfuscatedAccountId(obfuscatedAccountId)
        }

        return billingClient.launchBillingFlow(activity, billingFlowParamsBuilder.build())
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User canceled subscription purchase flow.")
                externalScope.launch {
                    _purchaseEvents.emit(PurchaseResultEvent.UserCanceled)
                }
            }
            else -> {
                Log.e(TAG, "Purchase error: code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
                externalScope.launch {
                    _purchaseEvents.emit(PurchaseResultEvent.Error(billingResult.responseCode, billingResult.debugMessage))
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        val productId = purchase.products.firstOrNull() ?: ""
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                Log.d(TAG, "Purchase status PURCHASED for $productId. Token ready for server verification.")
                externalScope.launch {
                    _purchaseEvents.emit(
                        PurchaseResultEvent.Success(
                            purchaseToken = purchase.purchaseToken,
                            productId = productId,
                            orderId = purchase.orderId
                        )
                    )
                }
            }
            Purchase.PurchaseState.PENDING -> {
                Log.i(TAG, "Purchase status PENDING for $productId.")
                externalScope.launch {
                    _purchaseEvents.emit(PurchaseResultEvent.Pending(productId))
                }
            }
            else -> {
                Log.w(TAG, "Purchase in unspecified state: ${purchase.purchaseState}")
            }
        }
    }

    fun queryPurchasesAsync(onPurchasesFound: (List<Purchase>) -> Unit) {
        if (_connectionState.value != BillingConnectionState.Connected) {
            Log.w(TAG, "Cannot query purchases: BillingClient not connected.")
            onPurchasesFound(emptyList())
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Found ${purchasesList.size} active subscription purchases.")
                onPurchasesFound(purchasesList)
            } else {
                Log.e(TAG, "Failed to query active purchases: ${billingResult.debugMessage}")
                onPurchasesFound(emptyList())
            }
        }
    }

    fun endConnection() {
        reconnectJob?.cancel()
        if (billingClient.isReady) {
            Log.d(TAG, "Ending BillingClient connection...")
            billingClient.endConnection()
        }
        _connectionState.value = BillingConnectionState.Disconnected
    }

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_PRO_MONTHLY = "workcraft_pro_monthly"
        const val PRODUCT_PRO_YEARLY = "workcraft_pro_yearly"
        const val BASE_PLAN_MONTHLY = "monthly-plan"
        const val BASE_PLAN_YEARLY = "yearly-plan-v2"
        const val OFFER_MONTHLY_TRIAL = "monthly-free-trial"
        const val OFFER_YEARLY_TRIAL = "yearly-free-trial"
        private const val MAX_RECONNECT_ATTEMPTS = 3
        private const val BASE_RECONNECT_DELAY_MS = 1000L
    }
}
