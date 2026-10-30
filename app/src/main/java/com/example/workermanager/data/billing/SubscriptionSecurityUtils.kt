package com.example.workermanager.data.billing

import java.security.MessageDigest

object SubscriptionSecurityUtils {
    /**
     * Generates a deterministic SHA-256 obfuscated account identifier matching backend implementation.
     * Formula: SHA-256("workcraft_user_" + uid) in lowercase hexadecimal format.
     */
    fun getObfuscatedAccountId(uid: String): String {
        val input = "workcraft_user_$uid"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

