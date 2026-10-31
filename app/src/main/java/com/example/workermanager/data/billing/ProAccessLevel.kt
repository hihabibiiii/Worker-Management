package com.example.workermanager.data.billing

import com.example.workermanager.data.repository.SubscriptionEntitlement
import com.example.workermanager.data.repository.SubscriptionRepository

enum class ProAccessLevel(val displayName: String) {
    FREE("Free Plan"),
    PRO_ACTIVE("WorkCraft Pro"),
    PRO_TRIAL("Free Trial"),
    PRO_EXPIRED("Subscription Expired"),
    PRO_CANCELLED_BUT_ACTIVE("Canceling at Period End");

    val isProUnlocked: Boolean
        get() = this == PRO_ACTIVE || this == PRO_TRIAL || this == PRO_CANCELLED_BUT_ACTIVE

    companion object {
        const val FREE_SITE_LIMIT = 1
        const val FREE_WORKER_LIMIT = 5

        fun fromEntitlement(
            entitlement: SubscriptionEntitlement,
            nowMillis: Long = System.currentTimeMillis()
        ): ProAccessLevel {
            val isExpired = SubscriptionRepository.isSubscriptionExpired(entitlement.expiryTime, nowMillis)

            // If there's no active flag and no product id or empty entitlement
            if (!entitlement.isPro && entitlement.productId.isNullOrBlank() && entitlement.subscriptionState.isNullOrBlank()) {
                return FREE
            }

            if (isExpired) {
                return if (!entitlement.productId.isNullOrBlank() || entitlement.subscriptionState == "SUBSCRIPTION_STATE_EXPIRED") {
                    PRO_EXPIRED
                } else {
                    FREE
                }
            }

            // Expiry is in future (or no expiry yet), but backend says isPro = false
            if (!entitlement.isPro) {
                return if (entitlement.subscriptionState == "SUBSCRIPTION_STATE_EXPIRED") {
                    PRO_EXPIRED
                } else {
                    FREE
                }
            }

            // At this point, isPro == true and !isExpired
            val state = entitlement.subscriptionState?.uppercase() ?: ""
            return when {
                state == "SUBSCRIPTION_STATE_CANCELED" || (!entitlement.autoRenewEnabled && state == "SUBSCRIPTION_STATE_ACTIVE") -> {
                    PRO_CANCELLED_BUT_ACTIVE
                }
                state.contains("TRIAL") -> {
                    PRO_TRIAL
                }
                else -> {
                    PRO_ACTIVE
                }
            }
        }
    }
}

