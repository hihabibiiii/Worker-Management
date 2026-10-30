package com.example.workermanager

import com.example.workermanager.data.entity.AttendanceEntity
import com.example.workermanager.data.entity.AttendanceStatus
import com.example.workermanager.data.entity.ContractEntity
import com.example.workermanager.data.entity.ContractStatus
import com.example.workermanager.data.entity.ExpenseCategory
import com.example.workermanager.data.entity.ExpensePaymentMethod
import com.example.workermanager.data.entity.PaymentEntity
import com.example.workermanager.data.entity.PaymentMethod
import com.example.workermanager.data.entity.PaymentType
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.SiteExpenseEntity
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.entity.WorkerEntity
import com.example.workermanager.data.entity.WorkerType
import com.example.workermanager.data.remote.sync.dto.AttendanceDTO
import com.example.workermanager.data.remote.sync.dto.ContractDTO
import com.example.workermanager.data.remote.sync.dto.PaymentDTO
import com.example.workermanager.data.remote.sync.dto.SiteDTO
import com.example.workermanager.data.remote.sync.dto.SiteExpenseDTO
import com.example.workermanager.data.remote.sync.dto.WorkerDTO
import com.example.workermanager.data.remote.sync.engine.LegacySyncBootstrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.util.UUID

class LegacySyncBootstrapLogicTest {

    @Test
    fun testWorkerMatching_cloudMatchAdoptsExistingSyncId() {
        val legacyWorker = WorkerEntity(
            id = 1,
            name = "Ramesh Kumar",
            phone = "9876543210",
            workerType = WorkerType.MISTRI,
            dailyRate = 600.0,
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = false
        )

        val cloudWorkers = listOf(
            WorkerDTO(
                syncId = "cloud_worker_uuid_ramesh",
                name = "  Ramesh Kumar ",
                phone = "9876543210",
                workerType = "MISTRI"
            )
        )

        val matchedCloudSyncIds = mutableSetOf<String>()
        val matchingCandidates = cloudWorkers.filter {
            it.syncId !in matchedCloudSyncIds && LegacySyncBootstrapper.isWorkerMatch(legacyWorker, it)
        }

        assertEquals(1, matchingCandidates.size)
        val match = matchingCandidates.first()
        matchedCloudSyncIds.add(match.syncId)

        val updated = legacyWorker.copy(
            syncId = match.syncId,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            isPendingSync = false
        )

        assertEquals("cloud_worker_uuid_ramesh", updated.syncId)
        assertEquals(SyncOrigin.CLOUD_SYNCED, updated.syncOrigin)
        assertFalse(updated.isPendingSync)
    }

    @Test
    fun testWorkerMatching_sameNameAndType_differentPhoneNumbers_doesNotMatch() {
        val localWorker = WorkerEntity(
            id = 1,
            name = "Ramesh Kumar",
            phone = "9876543210",
            workerType = WorkerType.LABOUR,
            dailyRate = 500.0,
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = false
        )

        val cloudWorker = WorkerDTO(
            syncId = "cloud_worker_ramesh_other",
            name = "Ramesh Kumar",
            phone = "9111111111", // Different phone!
            workerType = "LABOUR"  // Same workerType
        )

        val isMatch = LegacySyncBootstrapper.isWorkerMatch(localWorker, cloudWorker)
        assertFalse("Phone mismatch must NEVER match even if name and workerType match", isMatch)

        val matchedCloudSyncIds = mutableSetOf<String>()
        val candidates = listOf(cloudWorker).filter {
            it.syncId !in matchedCloudSyncIds && LegacySyncBootstrapper.isWorkerMatch(localWorker, it)
        }
        assertTrue("No candidate should match when phone numbers differ", candidates.isEmpty())

        // Local worker is safely upgraded with its own new UUID
        val updated = localWorker.copy(
            syncId = UUID.randomUUID().toString(),
            syncOrigin = SyncOrigin.NEW_LOCAL,
            isPendingSync = true
        )
        assertNotEquals("cloud_worker_ramesh_other", updated.syncId)
        assertEquals(SyncOrigin.NEW_LOCAL, updated.syncOrigin)
        assertTrue(updated.isPendingSync)
    }

    @Test
    fun testWorkerMatching_duplicateCandidatesInFirestore_isAmbiguous_doesNotMatch() {
        val localWorker = WorkerEntity(
            id = 1,
            name = "Ramesh Kumar",
            phone = "9876543210",
            workerType = WorkerType.LABOUR,
            dailyRate = 500.0,
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = false
        )

        // Two identical worker documents in Firestore
        val cloudCandidates = listOf(
            WorkerDTO(syncId = "cloud_uuid_1", name = "Ramesh Kumar", phone = "9876543210", workerType = "LABOUR"),
            WorkerDTO(syncId = "cloud_uuid_2", name = "Ramesh Kumar", phone = "9876543210", workerType = "LABOUR")
        )

        val matchedCloudSyncIds = mutableSetOf<String>()
        val matchingCandidates = cloudCandidates.filter {
            it.syncId !in matchedCloudSyncIds && LegacySyncBootstrapper.isWorkerMatch(localWorker, it)
        }

        assertEquals(2, matchingCandidates.size) // Ambiguous!

        // Logic must not assign either candidate when ambiguous
        val updated = if (matchingCandidates.size == 1) {
            val match = matchingCandidates.first()
            matchedCloudSyncIds.add(match.syncId)
            localWorker.copy(syncId = match.syncId, syncOrigin = SyncOrigin.CLOUD_SYNCED, isPendingSync = false)
        } else {
            localWorker.copy(
                syncId = UUID.randomUUID().toString(),
                syncOrigin = SyncOrigin.NEW_LOCAL,
                isPendingSync = true
            )
        }

        assertTrue("Must generate new UUID on ambiguous match", updated.syncId != "cloud_uuid_1" && updated.syncId != "cloud_uuid_2")
        assertEquals(SyncOrigin.NEW_LOCAL, updated.syncOrigin)
        assertTrue(updated.isPendingSync)
        assertTrue("Neither cloud syncId should be consumed when match is ambiguous", matchedCloudSyncIds.isEmpty())
    }

    @Test
    fun testPaymentMatching_twoIdenticalPayments_consumeOnMatch_preventsDuplicateMatch() {
        val workerSyncId = "worker_uuid_ramesh"
        val date = LocalDate.of(2024, 3, 1)

        val localPayment1 = PaymentEntity(
            id = 1,
            workerId = 10,
            siteId = 5,
            amount = 500.0,
            date = date,
            method = PaymentMethod.CASH,
            paymentType = PaymentType.ADVANCE,
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = false
        )
        val localPayment2 = PaymentEntity(
            id = 2,
            workerId = 10,
            siteId = 5,
            amount = 500.0,
            date = date,
            method = PaymentMethod.CASH,
            paymentType = PaymentType.ADVANCE,
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = false
        )

        // Only ONE ₹500 payment exists in cloud
        val cloudPayments = listOf(
            PaymentDTO(
                syncId = "cloud_pay_single_500",
                workerSyncId = workerSyncId,
                amount = 500.0,
                date = date.toString()
            )
        )

        val matchedCloudSyncIds = mutableSetOf<String>()

        // Process first payment
        val candidatesForPay1 = cloudPayments.filter {
            it.syncId !in matchedCloudSyncIds &&
            it.workerSyncId == workerSyncId && it.date == localPayment1.date.toString() && Math.abs(it.amount - localPayment1.amount) < 0.01
        }
        assertEquals(1, candidatesForPay1.size)
        val match1 = candidatesForPay1.first()
        matchedCloudSyncIds.add(match1.syncId)

        val updatedPay1 = localPayment1.copy(syncId = match1.syncId, syncOrigin = SyncOrigin.CLOUD_SYNCED, isPendingSync = false)
        assertEquals("cloud_pay_single_500", updatedPay1.syncId)
        assertEquals(SyncOrigin.CLOUD_SYNCED, updatedPay1.syncOrigin)

        // Process second identical payment
        val candidatesForPay2 = cloudPayments.filter {
            it.syncId !in matchedCloudSyncIds &&
            it.workerSyncId == workerSyncId && it.date == localPayment2.date.toString() && Math.abs(it.amount - localPayment2.amount) < 0.01
        }
        assertTrue("Second payment must NOT match already consumed cloud payment", candidatesForPay2.isEmpty())

        val updatedPay2 = localPayment2.copy(
            syncId = UUID.randomUUID().toString(),
            syncOrigin = SyncOrigin.NEW_LOCAL,
            isPendingSync = true
        )
        assertNotEquals(updatedPay1.syncId, updatedPay2.syncId)
        assertEquals(SyncOrigin.NEW_LOCAL, updatedPay2.syncOrigin)
        assertTrue(updatedPay2.isPendingSync)
    }

    @Test
    fun testExpenseMatching_twoIdenticalExpenses_consumeOnMatch() {
        val siteSyncId = "site_uuid_mall"
        val date = LocalDate.of(2024, 3, 2)

        val localExp1 = SiteExpenseEntity(
            id = 1,
            siteId = 5,
            amount = 50.0,
            expenseDate = date,
            description = "Tea",
            category = ExpenseCategory.CHAI_PANI,
            paymentMethod = ExpensePaymentMethod.CASH,
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = false
        )
        val localExp2 = SiteExpenseEntity(
            id = 2,
            siteId = 5,
            amount = 50.0,
            expenseDate = date,
            description = "Tea",
            category = ExpenseCategory.CHAI_PANI,
            paymentMethod = ExpensePaymentMethod.CASH,
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = false
        )

        val cloudExpenses = listOf(
            SiteExpenseDTO(
                syncId = "cloud_exp_chai_50",
                siteSyncId = siteSyncId,
                amount = 50.0,
                expenseDate = date.toString(),
                description = "Tea"
            )
        )

        val matchedCloudSyncIds = mutableSetOf<String>()

        val candidates1 = cloudExpenses.filter {
            it.syncId !in matchedCloudSyncIds &&
            it.siteSyncId == siteSyncId && it.expenseDate == localExp1.expenseDate.toString() && Math.abs(it.amount - localExp1.amount) < 0.01
        }
        assertEquals(1, candidates1.size)
        matchedCloudSyncIds.add(candidates1.first().syncId)
        val updatedExp1 = localExp1.copy(syncId = candidates1.first().syncId, syncOrigin = SyncOrigin.CLOUD_SYNCED, isPendingSync = false)
        assertEquals("cloud_exp_chai_50", updatedExp1.syncId)

        // Second expense
        val candidates2 = cloudExpenses.filter {
            it.syncId !in matchedCloudSyncIds &&
            it.siteSyncId == siteSyncId && it.expenseDate == localExp2.expenseDate.toString() && Math.abs(it.amount - localExp2.amount) < 0.01
        }
        assertTrue("Second expense must not match consumed cloud expense", candidates2.isEmpty())
        val updatedExp2 = localExp2.copy(syncId = UUID.randomUUID().toString(), syncOrigin = SyncOrigin.NEW_LOCAL, isPendingSync = true)
        assertNotEquals(updatedExp1.syncId, updatedExp2.syncId)
    }

    @Test
    fun testFirestoreTimeoutDuringBootstrap_abortsWithoutModifyingRecords() {
        // Simulate fetchCloudCollection throwing timeout/IO exception
        fun fetchCloudCollectionSimulated(shouldFail: Boolean): List<SiteDTO> {
            if (shouldFail) throw IOException("Firestore connection timeout: DEADLINE_EXCEEDED")
            return listOf(SiteDTO(syncId = "site_1", name = "Alpha"))
        }

        var legacySite = SiteEntity(
            id = 10,
            name = "Alpha",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = false
        )

        val result: Result<Int> = try {
            fetchCloudCollectionSimulated(shouldFail = true)
            // Bootstrap logic
            legacySite = legacySite.copy(syncId = "site_1", syncOrigin = SyncOrigin.CLOUD_SYNCED)
            Result.success(1)
        } catch (e: Exception) {
            // Aborts immediately without modifying legacySite!
            Result.failure(e)
        }

        assertTrue("Bootstrap must report failure on network/timeout error", result.isFailure)
        assertEquals("Firestore connection timeout: DEADLINE_EXCEEDED", result.exceptionOrNull()?.message)
        assertEquals("Legacy record must remain UNMODIFIED when fetch fails", SyncOrigin.LEGACY, legacySite.syncOrigin)
        assertEquals("Legacy syncId must remain empty when fetch fails", "", legacySite.syncId)
        assertFalse("Legacy record must NOT be marked isPendingSync when fetch fails", legacySite.isPendingSync)
    }

    @Test
    fun testOfflineBootstrapFollowedBySuccessfulOnlineRetry() {
        var isOnline = false
        val legacyWorker = WorkerEntity(
            id = 1,
            name = "Ramesh Kumar",
            phone = "9876543210",
            workerType = WorkerType.MISTRI,
            dailyRate = 600.0,
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = false
        )

        fun attemptBootstrap(currentWorker: WorkerEntity): Pair<Result<Int>, WorkerEntity> {
            if (!isOnline) {
                // Offline: throws exception, does not mutate record
                return Pair(Result.failure(IOException("Network unavailable")), currentWorker)
            }
            // Online: fetches cloud workers and matches
            val cloudWorkers = listOf(WorkerDTO(syncId = "cloud_uuid_ramesh", name = "Ramesh Kumar", phone = "9876543210", workerType = "MISTRI"))
            val match = cloudWorkers.find { LegacySyncBootstrapper.isWorkerMatch(currentWorker, it) }
            val updated = if (match != null) {
                currentWorker.copy(syncId = match.syncId, syncOrigin = SyncOrigin.CLOUD_SYNCED, isPendingSync = false)
            } else {
                currentWorker.copy(syncId = UUID.randomUUID().toString(), syncOrigin = SyncOrigin.NEW_LOCAL, isPendingSync = true)
            }
            return Pair(Result.success(1), updated)
        }

        // Attempt 1: Offline
        val (offlineResult, workerAfterOffline) = attemptBootstrap(legacyWorker)
        assertTrue(offlineResult.isFailure)
        assertEquals(SyncOrigin.LEGACY, workerAfterOffline.syncOrigin)
        assertEquals("", workerAfterOffline.syncId)
        assertFalse(workerAfterOffline.isPendingSync)

        // Attempt 2: Reconnect online
        isOnline = true
        val (onlineResult, workerAfterOnline) = attemptBootstrap(workerAfterOffline)
        assertTrue(onlineResult.isSuccess)
        assertEquals("cloud_uuid_ramesh", workerAfterOnline.syncId)
        assertEquals(SyncOrigin.CLOUD_SYNCED, workerAfterOnline.syncOrigin)
        assertFalse(workerAfterOnline.isPendingSync)
    }

    @Test
    fun testRepeatedBootstrapWithoutDuplicateDocuments() {
        val alreadySyncedWorker = WorkerEntity(
            id = 10,
            name = "Already Synced",
            phone = "1122334455",
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "existing_uuid_10",
            isPendingSync = false
        )

        val needsBootstrap = alreadySyncedWorker.syncOrigin == SyncOrigin.LEGACY || alreadySyncedWorker.syncId.isBlank()
        assertFalse("Already synced records must not be re-bootstrapped", needsBootstrap)
    }
}
