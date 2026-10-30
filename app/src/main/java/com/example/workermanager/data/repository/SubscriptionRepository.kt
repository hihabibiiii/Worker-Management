package com.example.workermanager.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.workermanager.data.billing.BillingManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class SubscriptionEntitlement(
    val isPro: Boolean = false,
    val productId: String? = null,
    val subscriptionState: String? = null,
    val expiryTime: String? = null,
    val autoRenewEnabled: Boolean = false,
    val acknowledged: Boolean = false,
    val isVerifying: Boolean = false,
    val lastVerifiedTime: Long = 0L,
    val errorMessage: String? = null,
    val acknowledgementPending: Boolean = false
)

class SubscriptionRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val functions: FirebaseFunctions = Firebase.functions

    private val _entitlement = MutableStateFlow(loadCachedEntitlement())
    val entitlement: StateFlow<SubscriptionEntitlement> = _entitlement.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Observe Firebase Auth state for automatic startup, login, and account-switch sync
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null && user.uid.isNotBlank()) {
                val cached = loadCachedEntitlement(user.uid)
                _entitlement.value = cached
                scope.launch {
                    refreshSubscriptionStatus(user.uid)
                }
            } else {
                clearEntitlement()
            }
        }
    }

    private fun loadCachedEntitlement(expectedUid: String? = null): SubscriptionEntitlement {
        val currentUid = expectedUid ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val cachedUid = prefs.getString(KEY_CACHED_UID, "") ?: ""
        if (currentUid.isBlank() || cachedUid != currentUid) {
            return SubscriptionEntitlement(isPro = false)
        }

        val isPro = prefs.getBoolean(KEY_IS_PRO, false)
        val productId = prefs.getString(KEY_PRODUCT_ID, null)
        val subscriptionState = prefs.getString(KEY_SUBSCRIPTION_STATE, null)
        val expiryTime = prefs.getString(KEY_EXPIRY_TIME, null)
        val autoRenewEnabled = prefs.getBoolean(KEY_AUTO_RENEW, false)
        val acknowledged = prefs.getBoolean(KEY_ACKNOWLEDGED, false)
        val lastVerifiedTime = prefs.getLong(KEY_LAST_VERIFIED, 0L)
        val acknowledgementPending = prefs.getBoolean(KEY_ACK_PENDING, false)

        // Validate expiry time locally against current time (fail-closed validation)
        val isExpired = isSubscriptionExpired(expiryTime)
        val effectiveIsPro = isPro && !isExpired

        return SubscriptionEntitlement(
            isPro = effectiveIsPro,
            productId = productId,
            subscriptionState = if (isExpired) "SUBSCRIPTION_STATE_EXPIRED" else subscriptionState,
            expiryTime = expiryTime,
            autoRenewEnabled = autoRenewEnabled,
            acknowledged = acknowledged,
            isVerifying = false,
            lastVerifiedTime = lastVerifiedTime,
            acknowledgementPending = acknowledgementPending
        )
    }

    private fun saveEntitlement(uid: String, ent: SubscriptionEntitlement) {
        prefs.edit()
            .putString(KEY_CACHED_UID, uid)
            .putBoolean(KEY_IS_PRO, ent.isPro)
            .putString(KEY_PRODUCT_ID, ent.productId)
            .putString(KEY_SUBSCRIPTION_STATE, ent.subscriptionState)
            .putString(KEY_EXPIRY_TIME, ent.expiryTime)
            .putBoolean(KEY_AUTO_RENEW, ent.autoRenewEnabled)
            .putBoolean(KEY_ACKNOWLEDGED, ent.acknowledged)
            .putLong(KEY_LAST_VERIFIED, ent.lastVerifiedTime)
            .putBoolean(KEY_ACK_PENDING, ent.acknowledgementPending)
            .apply()
    }

    fun clearEntitlement() {
        prefs.edit()
            .remove(KEY_CACHED_UID)
            .remove(KEY_IS_PRO)
            .remove(KEY_PRODUCT_ID)
            .remove(KEY_SUBSCRIPTION_STATE)
            .remove(KEY_EXPIRY_TIME)
            .remove(KEY_AUTO_RENEW)
            .remove(KEY_ACKNOWLEDGED)
            .remove(KEY_LAST_VERIFIED)
            .remove(KEY_ACK_PENDING)
            .apply()
        _entitlement.value = SubscriptionEntitlement(isPro = false)
    }

    /**
     * Retrieves subscription entitlement from the backend getMySubscription callable function.
     */
    suspend fun refreshSubscriptionStatus(explicitUid: String? = null): Result<SubscriptionEntitlement> = withContext(Dispatchers.IO) {
        val uid = explicitUid ?: FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            val empty = SubscriptionEntitlement(isPro = false)
            _entitlement.value = empty
            return@withContext Result.success(empty)
        }

        _isRefreshing.value = true
        try {
            val callable = functions.getHttpsCallable("getMySubscription")
            val result = callable.call().await()
            val data = result.data as? Map<*, *>

            val isPro = (data?.get("isPro") as? Boolean) ?: false
            val productId = data?.get("productId") as? String
            val subscriptionState = data?.get("subscriptionState") as? String
            val expiryTime = data?.get("expiryTime") as? String
            val autoRenewEnabled = (data?.get("autoRenewEnabled") as? Boolean) ?: false
            val acknowledged = (data?.get("acknowledged") as? Boolean) ?: false

            val updated = SubscriptionEntitlement(
                isPro = isPro,
                productId = productId,
                subscriptionState = subscriptionState,
                expiryTime = expiryTime,
                autoRenewEnabled = autoRenewEnabled,
                acknowledged = acknowledged,
                isVerifying = false,
                lastVerifiedTime = System.currentTimeMillis(),
                errorMessage = null
            )
            saveEntitlement(uid, updated)
            _entitlement.value = updated
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh getMySubscription", e)
            val friendlyError = mapFunctionsException(e)
            val current = _entitlement.value.copy(errorMessage = friendlyError)
            _entitlement.value = current
            Result.failure(e)
        } finally {
            _isRefreshing.value = false
        }
    }

    /**
     * Verifies purchase token and product ID with backend verifyGooglePlaySubscription callable function.
     */
    suspend fun verifyPurchase(
        purchaseToken: String,
        productId: String
    ): Result<SubscriptionEntitlement> = withContext(Dispatchers.IO) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.uid.isBlank()) {
            val err = "Please sign in before verifying your purchase."
            _entitlement.value = _entitlement.value.copy(isVerifying = false, errorMessage = err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        _entitlement.value = _entitlement.value.copy(isVerifying = true, errorMessage = null)

        try {
            val payload = mapOf(
                "purchaseToken" to purchaseToken,
                "productId" to productId
            )
            val callable = functions.getHttpsCallable("verifyGooglePlaySubscription")
            val result = callable.call(payload).await()
            val data = result.data as? Map<*, *>

            val isPro = (data?.get("isPro") as? Boolean) ?: false
            val resProductId = (data?.get("productId") as? String) ?: productId
            val subscriptionState = data?.get("subscriptionState") as? String
            val expiryTime = data?.get("expiryTime") as? String
            val autoRenewEnabled = (data?.get("autoRenewEnabled") as? Boolean) ?: false
            val acknowledgementPending = (data?.get("acknowledgementPending") as? Boolean) ?: false

            val updated = SubscriptionEntitlement(
                isPro = isPro,
                productId = resProductId,
                subscriptionState = subscriptionState,
                expiryTime = expiryTime,
                autoRenewEnabled = autoRenewEnabled,
                acknowledged = !acknowledgementPending,
                isVerifying = false,
                lastVerifiedTime = System.currentTimeMillis(),
                errorMessage = null,
                acknowledgementPending = acknowledgementPending
            )
            saveEntitlement(user.uid, updated)
            _entitlement.value = updated
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Failed in verifyGooglePlaySubscription", e)
            val friendlyError = mapFunctionsException(e)
            _entitlement.value = _entitlement.value.copy(
                isVerifying = false,
                errorMessage = friendlyError
            )
            Result.failure(e)
        }
    }

    private fun mapFunctionsException(e: Exception): String {
        if (e is FirebaseFunctionsException) {
            return when (e.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                    "Please sign in with Google to manage your subscription."
                FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                    "This Google Play subscription is already linked to another WorkCraft account."
                FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                    "Invalid subscription details or purchase token."
                FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                    "Subscription is currently inactive or expired."
                FirebaseFunctionsException.Code.UNAVAILABLE ->
                    "Could not connect to verification server. Please check your internet connection."
                else -> e.message ?: "Verification failed (${e.code}). Please try again."
            }
        }
        return e.localizedMessage ?: "Network or server error during verification. Please try again."
    }

    companion object {
        private const val TAG = "SubscriptionRepository"
        private const val PREFS_NAME = "workcraft_subscription_prefs"
        private const val KEY_CACHED_UID = "sub_cached_uid"
        private const val KEY_IS_PRO = "sub_is_pro"
        private const val KEY_PRODUCT_ID = "sub_product_id"
        private const val KEY_SUBSCRIPTION_STATE = "sub_state"
        private const val KEY_EXPIRY_TIME = "sub_expiry_time"
        private const val KEY_AUTO_RENEW = "sub_auto_renew"
        private const val KEY_ACKNOWLEDGED = "sub_acknowledged"
        private const val KEY_LAST_VERIFIED = "sub_last_verified"
        private const val KEY_ACK_PENDING = "sub_ack_pending"

        /**
         * Evaluates whether a subscription expiry timestamp is expired, missing, or malformed.
         * Enforces fail-closed validation: null, blank, malformed, or past timestamps are all treated as expired.
         */
        fun isSubscriptionExpired(expiryTime: String?, nowMillis: Long = System.currentTimeMillis()): Boolean {
            if (expiryTime.isNullOrBlank()) {
                return true
            }
            return try {
                val parsed = java.time.Instant.parse(expiryTime).toEpochMilli()
                parsed <= nowMillis
            } catch (_: Exception) {
                true
            }
        }
    }
}
