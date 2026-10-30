package com.example.workermanager

import com.example.workermanager.data.billing.SubscriptionSecurityUtils
import com.example.workermanager.data.repository.SubscriptionEntitlement
import com.example.workermanager.data.repository.SubscriptionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SubscriptionSecurityUtilsTest {

    @Test
    fun testObfuscatedAccountIdDeterministicAndMatchesBackend() {
        val uid = "test_firebase_uid_123"
        val expectedHash = "895a84baff5399d774aaaedb3bf93e7d78815ce08293b4c843e3e7de3966c017"
        val result = SubscriptionSecurityUtils.getObfuscatedAccountId(uid)

        assertEquals(expectedHash, result)
        assertEquals(64, result.length)
    }

    @Test
    fun testObfuscatedAccountIdDiffersForDifferentUsers() {
        val uid1 = "user_alpha"
        val uid2 = "user_beta"

        val hash1 = SubscriptionSecurityUtils.getObfuscatedAccountId(uid1)
        val hash2 = SubscriptionSecurityUtils.getObfuscatedAccountId(uid2)

        assertNotEquals(hash1, hash2)
        assertEquals(64, hash1.length)
        assertEquals(64, hash2.length)
    }

    @Test
    fun testSubscriptionEntitlementDefaultsToFree() {
        val ent = SubscriptionEntitlement()
        assertFalse(ent.isPro)
        assertFalse(ent.autoRenewEnabled)
        assertFalse(ent.isVerifying)
        assertEquals(null, ent.productId)
        assertEquals(null, ent.subscriptionState)
    }

    @Test
    fun testSubscriptionEntitlementActivePro() {
        val futureExpiry = Instant.now().plusSeconds(86400 * 30).toString()
        val ent = SubscriptionEntitlement(
            isPro = true,
            productId = "workcraft_pro_yearly",
            subscriptionState = "SUBSCRIPTION_STATE_ACTIVE",
            expiryTime = futureExpiry,
            autoRenewEnabled = true,
            acknowledged = true
        )

        assertTrue(ent.isPro)
        assertTrue(ent.autoRenewEnabled)
        assertTrue(ent.acknowledged)
        assertEquals("workcraft_pro_yearly", ent.productId)
        assertEquals("SUBSCRIPTION_STATE_ACTIVE", ent.subscriptionState)
    }

    // =========================================================================
    // Fail-Closed Expiry Validation Tests
    // =========================================================================

    @Test
    fun testMissingExpiryIsTreatedAsExpired() {
        // null expiry must fail-closed
        assertTrue(SubscriptionRepository.isSubscriptionExpired(null))
    }

    @Test
    fun testBlankExpiryIsTreatedAsExpired() {
        // Empty or whitespace-only expiry must fail-closed
        assertTrue(SubscriptionRepository.isSubscriptionExpired(""))
        assertTrue(SubscriptionRepository.isSubscriptionExpired("   "))
        assertTrue(SubscriptionRepository.isSubscriptionExpired("\t\n"))
    }

    @Test
    fun testMalformedExpiryIsTreatedAsExpired() {
        // Any non-RFC3339/ISO string must fail-closed
        assertTrue(SubscriptionRepository.isSubscriptionExpired("invalid-timestamp"))
        assertTrue(SubscriptionRepository.isSubscriptionExpired("not_a_date"))
        assertTrue(SubscriptionRepository.isSubscriptionExpired("2026-99-99T99:99:99Z"))
        assertTrue(SubscriptionRepository.isSubscriptionExpired("123456789"))
    }

    @Test
    fun testExpiredTimestampIsTreatedAsExpired() {
        val now = Instant.parse("2026-10-29T12:00:00Z")
        val nowMillis = now.toEpochMilli()

        // 1 hour in the past
        val past = now.minusSeconds(3600).toString()
        assertTrue(SubscriptionRepository.isSubscriptionExpired(past, nowMillis))

        // Exactly equal to current time (boundary condition)
        assertTrue(SubscriptionRepository.isSubscriptionExpired(now.toString(), nowMillis))
    }

    @Test
    fun testValidFutureExpiryIsNotExpired() {
        val now = Instant.parse("2026-10-29T12:00:00Z")
        val nowMillis = now.toEpochMilli()

        // 30 days in the future
        val future = now.plusSeconds(86400 * 30).toString()
        assertFalse(SubscriptionRepository.isSubscriptionExpired(future, nowMillis))

        // 1 second in the future
        val nextSecond = now.plusSeconds(1).toString()
        assertFalse(SubscriptionRepository.isSubscriptionExpired(nextSecond, nowMillis))
    }

    // =========================================================================
    // Billing Product, Base Plan, and Offer Configuration Tests
    // =========================================================================

    @Test
    fun testMonthlyProductAndBasePlanConfiguration() {
        assertEquals("workcraft_pro_monthly", com.example.workermanager.data.billing.BillingManager.PRODUCT_PRO_MONTHLY)
        assertEquals("monthly-plan", com.example.workermanager.data.billing.BillingManager.BASE_PLAN_MONTHLY)
        assertEquals("monthly-free-trial", com.example.workermanager.data.billing.BillingManager.OFFER_MONTHLY_TRIAL)
    }

    @Test
    fun testYearlyProductAndUpdatedBasePlanV2Configuration() {
        assertEquals("workcraft_pro_yearly", com.example.workermanager.data.billing.BillingManager.PRODUCT_PRO_YEARLY)
        assertEquals("yearly-plan-v2", com.example.workermanager.data.billing.BillingManager.BASE_PLAN_YEARLY)
        assertEquals("yearly-free-trial", com.example.workermanager.data.billing.BillingManager.OFFER_YEARLY_TRIAL)
    }
}
