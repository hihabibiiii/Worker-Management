package com.example.workermanager.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingResult
import com.example.workermanager.data.billing.BillingConnectionState
import com.example.workermanager.data.billing.BillingManager
import com.example.workermanager.data.billing.ProductQueryState
import com.example.workermanager.data.billing.PurchaseResultEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class SubscriptionViewModel(
    private val billingManager: BillingManager
) : ViewModel() {

    val connectionState: StateFlow<BillingConnectionState> = billingManager.connectionState
    val productQueryState: StateFlow<ProductQueryState> = billingManager.productQueryState
    val purchaseEvents: SharedFlow<PurchaseResultEvent> = billingManager.purchaseEvents

    init {
        billingManager.startConnection()
    }

    fun retryConnection() {
        billingManager.startConnection()
    }

    fun queryProducts() {
        billingManager.querySubscriptionProducts()
    }

    fun launchPurchase(
        activity: Activity,
        productId: String,
        targetOfferId: String? = null,
        targetBasePlanId: String? = null
    ): BillingResult {
        return billingManager.launchPurchaseFlow(activity, productId, targetOfferId, targetBasePlanId)
    }

    fun restorePurchases(onComplete: (Int) -> Unit) {
        billingManager.queryPurchasesAsync { purchases ->
            onComplete(purchases.size)
        }
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.endConnection()
    }
}

class SubscriptionViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscriptionViewModel::class.java)) {
            val billingManager = BillingManager(context.applicationContext, CoroutineScope(Dispatchers.Default))
            @Suppress("UNCHECKED_CAST")
            return SubscriptionViewModel(billingManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
