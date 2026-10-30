package com.example.workermanager.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingResult
import com.example.workermanager.WorkerApplication
import com.example.workermanager.data.billing.BillingConnectionState
import com.example.workermanager.data.billing.BillingManager
import com.example.workermanager.data.billing.ProductQueryState
import com.example.workermanager.data.billing.PurchaseResultEvent
import com.example.workermanager.data.billing.SubscriptionSecurityUtils
import com.example.workermanager.data.repository.SubscriptionEntitlement
import com.example.workermanager.data.repository.SubscriptionRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val billingManager: BillingManager,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    val connectionState: StateFlow<BillingConnectionState> = billingManager.connectionState
    val productQueryState: StateFlow<ProductQueryState> = billingManager.productQueryState
    val purchaseEvents: SharedFlow<PurchaseResultEvent> = billingManager.purchaseEvents
    val entitlement: StateFlow<SubscriptionEntitlement> = subscriptionRepository.entitlement
    val isRefreshing: StateFlow<Boolean> = subscriptionRepository.isRefreshing

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _verificationMessage = MutableStateFlow<String?>(null)
    val verificationMessage: StateFlow<String?> = _verificationMessage.asStateFlow()

    private val _isErrorMessage = MutableStateFlow(false)
    val isErrorMessage: StateFlow<Boolean> = _isErrorMessage.asStateFlow()

    init {
        billingManager.startConnection()

        // Automatically observe client purchase events and trigger secure backend verification
        viewModelScope.launch {
            billingManager.purchaseEvents.collect { event ->
                when (event) {
                    is PurchaseResultEvent.Success -> {
                        Log.d(TAG, "Purchase succeeded for ${event.productId}. Initiating backend verification...")
                        _isErrorMessage.value = false
                        _verificationMessage.value = "Purchase completed on Google Play. Securely verifying subscription..."
                        verifyPurchaseWithBackend(event.purchaseToken, event.productId)
                    }
                    is PurchaseResultEvent.Pending -> {
                        Log.i(TAG, "Purchase pending for ${event.productId}")
                        _isErrorMessage.value = false
                        _verificationMessage.value = "Purchase is pending approval from Google Play."
                    }
                    is PurchaseResultEvent.UserCanceled -> {
                        Log.i(TAG, "User canceled purchase flow")
                        _isErrorMessage.value = false
                        _verificationMessage.value = "Purchase flow was canceled."
                    }
                    is PurchaseResultEvent.Error -> {
                        Log.e(TAG, "Purchase error: ${event.responseCode} - ${event.debugMessage}")
                        _isErrorMessage.value = true
                        _verificationMessage.value = "Google Play error (${event.responseCode}): ${event.debugMessage}"
                    }
                }
            }
        }
    }

    fun retryConnection() {
        billingManager.startConnection()
    }

    fun queryProducts() {
        billingManager.querySubscriptionProducts()
    }

    fun refreshEntitlement() {
        viewModelScope.launch {
            subscriptionRepository.refreshSubscriptionStatus()
        }
    }

    fun dismissMessage() {
        _verificationMessage.value = null
        _isErrorMessage.value = false
    }

    /**
     * Launches purchase flow with obfuscated account ID derived from authenticated Firebase UID.
     */
    fun launchPurchase(
        activity: Activity,
        productId: String,
        targetOfferId: String? = null,
        targetBasePlanId: String? = null
    ): BillingResult {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val obfuscatedAccountId = if (!uid.isNullOrBlank()) {
            SubscriptionSecurityUtils.getObfuscatedAccountId(uid)
        } else {
            null
        }

        return billingManager.launchPurchaseFlow(
            activity = activity,
            productId = productId,
            targetOfferId = targetOfferId,
            targetBasePlanId = targetBasePlanId,
            obfuscatedAccountId = obfuscatedAccountId
        )
    }

    /**
     * Verifies purchase with backend verifyGooglePlaySubscription callable function.
     */
    fun verifyPurchaseWithBackend(purchaseToken: String, productId: String) {
        viewModelScope.launch {
            val result = subscriptionRepository.verifyPurchase(purchaseToken, productId)
            result.onSuccess { ent ->
                if (ent.isPro) {
                    _isErrorMessage.value = false
                    _verificationMessage.value = "Subscription successfully verified! WorkCraft Pro features are unlocked."
                } else if (ent.acknowledgementPending) {
                    _isErrorMessage.value = false
                    _verificationMessage.value = "Subscription verified, but Google Play acknowledgement is being finalized. Pro features will activate momentarily."
                } else {
                    _isErrorMessage.value = true
                    _verificationMessage.value = "Subscription verified, but Pro access is not active (${ent.subscriptionState})."
                }
            }.onFailure { ex ->
                _isErrorMessage.value = true
                _verificationMessage.value = entErrorText(ex)
            }
        }
    }

    /**
     * Queries active Google Play purchases and verifies them with backend.
     */
    fun restorePurchases(onComplete: (Boolean, String) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.uid.isBlank()) {
            val msg = "Please sign in with Google before restoring purchases."
            _isErrorMessage.value = true
            _verificationMessage.value = msg
            onComplete(false, msg)
            return
        }

        _isRestoring.value = true
        _isErrorMessage.value = false
        _verificationMessage.value = "Checking Google Play for active subscriptions..."

        billingManager.queryPurchasesAsync { purchases ->
            viewModelScope.launch {
                try {
                    if (purchases.isEmpty()) {
                        // Check server directly in case purchases exist on another platform / previous token
                        val refreshResult = subscriptionRepository.refreshSubscriptionStatus(user.uid)
                        val ent = refreshResult.getOrNull()
                        if (ent != null && ent.isPro) {
                            val msg = "Active WorkCraft Pro subscription restored from your account!"
                            _isErrorMessage.value = false
                            _verificationMessage.value = msg
                            onComplete(true, msg)
                        } else {
                            val msg = "No active Google Play subscription found for your account."
                            _isErrorMessage.value = false
                            _verificationMessage.value = msg
                            onComplete(false, msg)
                        }
                    } else {
                        var restoredAny = false
                        var lastError: String? = null

                        for (purchase in purchases) {
                            val productId = purchase.products.firstOrNull() ?: BillingManager.PRODUCT_PRO_MONTHLY
                            val verifyResult = subscriptionRepository.verifyPurchase(
                                purchaseToken = purchase.purchaseToken,
                                productId = productId
                            )
                            if (verifyResult.isSuccess && verifyResult.getOrNull()?.isPro == true) {
                                restoredAny = true
                            } else if (verifyResult.isFailure) {
                                lastError = verifyResult.exceptionOrNull()?.localizedMessage
                            }
                        }

                        if (restoredAny) {
                            val msg = "WorkCraft Pro subscription successfully restored and verified!"
                            _isErrorMessage.value = false
                            _verificationMessage.value = msg
                            onComplete(true, msg)
                        } else if (lastError != null) {
                            val msg = "Restore failed: $lastError"
                            _isErrorMessage.value = true
                            _verificationMessage.value = msg
                            onComplete(false, msg)
                        } else {
                            val msg = "No active subscription could be verified for this account."
                            _isErrorMessage.value = true
                            _verificationMessage.value = msg
                            onComplete(false, msg)
                        }
                    }
                } finally {
                    _isRestoring.value = false
                }
            }
        }
    }

    private fun entErrorText(e: Throwable): String {
        return e.localizedMessage ?: "Verification error. Please try again."
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.endConnection()
    }

    companion object {
        private const val TAG = "SubscriptionViewModel"
    }
}

class SubscriptionViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscriptionViewModel::class.java)) {
            val app = context.applicationContext as WorkerApplication
            val billingManager = BillingManager(app, CoroutineScope(Dispatchers.Default))
            @Suppress("UNCHECKED_CAST")
            return SubscriptionViewModel(billingManager, app.subscriptionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
