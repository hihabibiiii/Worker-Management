package com.example.workermanager

import com.example.workermanager.util.BackupManager
import com.example.workermanager.util.BackupPayload
import com.example.workermanager.util.OwnershipValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerOwnershipTest {

    @Test
    fun testUserABackupWithUserALogin_IsAllowed() {
        val userAUid = "FIREBASE_UID_USER_A"
        val payload = BackupPayload(ownerUid = userAUid)

        val result = BackupManager.validateBackupOwnership(payload.ownerUid, userAUid)
        assertTrue("Restoration should be valid when owner UID matches current logged in user UID", result is OwnershipValidationResult.Valid)
    }

    @Test
    fun testUserABackupWithUserBLogin_IsRejected() {
        val userAUid = "FIREBASE_UID_USER_A"
        val userBUid = "FIREBASE_UID_USER_B"
        val payload = BackupPayload(ownerUid = userAUid)

        val result = BackupManager.validateBackupOwnership(payload.ownerUid, userBUid)
        assertTrue("Restoration should be rejected when backup belongs to another account", result is OwnershipValidationResult.Invalid)
        val errorMessage = (result as OwnershipValidationResult.Invalid).userFacingErrorMessage
        assertEquals("This backup belongs to another account. Please sign in with the original account to restore it.", errorMessage)
    }

    @Test
    fun testUserBBackupWithUserALogin_IsRejected() {
        val userAUid = "FIREBASE_UID_USER_A"
        val userBUid = "FIREBASE_UID_USER_B"
        val payload = BackupPayload(ownerUid = userBUid)

        val result = BackupManager.validateBackupOwnership(payload.ownerUid, userAUid)
        assertTrue("Restoration should be rejected when backup belongs to another account", result is OwnershipValidationResult.Invalid)
        val errorMessage = (result as OwnershipValidationResult.Invalid).userFacingErrorMessage
        assertEquals("This backup belongs to another account. Please sign in with the original account to restore it.", errorMessage)
    }

    @Test
    fun testLegacyBackupWithoutOwnerUid_IsRejected() {
        val userAUid = "FIREBASE_UID_USER_A"
        val payload = BackupPayload(ownerUid = null)

        val result = BackupManager.validateBackupOwnership(payload.ownerUid, userAUid)
        assertTrue("Legacy backup without owner UID should be rejected safely", result is OwnershipValidationResult.Invalid)
        val errorMessage = (result as OwnershipValidationResult.Invalid).userFacingErrorMessage
        assertEquals("This backup file does not contain owner account verification and cannot be restored safely.", errorMessage)
    }

    @Test
    fun testUnauthenticatedUser_IsRejected() {
        val payload = BackupPayload(ownerUid = "FIREBASE_UID_USER_A")

        val result = BackupManager.validateBackupOwnership(payload.ownerUid, null)
        assertTrue("Restoration without authenticated user should be rejected", result is OwnershipValidationResult.Invalid)
        val errorMessage = (result as OwnershipValidationResult.Invalid).userFacingErrorMessage
        assertEquals("No authenticated account found. Please sign in to restore backups.", errorMessage)
    }
}
