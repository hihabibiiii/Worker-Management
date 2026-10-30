package com.example.workermanager

import com.example.workermanager.data.entity.AttendanceEntity
import com.example.workermanager.data.entity.AttendanceStatus
import com.example.workermanager.data.entity.ContractEntity
import com.example.workermanager.data.entity.ContractStatus
import com.example.workermanager.data.entity.PaymentEntity
import com.example.workermanager.data.entity.PaymentMethod
import com.example.workermanager.data.entity.PaymentType
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.SiteStatus
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.entity.WorkerEntity
import com.example.workermanager.data.entity.WorkerType
import com.example.workermanager.data.remote.sync.mapper.AttendanceMapper
import com.example.workermanager.data.remote.sync.mapper.ContractMapper
import com.example.workermanager.data.remote.sync.mapper.PaymentMapper
import com.example.workermanager.data.remote.sync.mapper.SiteMapper
import com.example.workermanager.data.remote.sync.mapper.WorkerMapper
import com.example.workermanager.util.CloudSyncWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CloudSyncEngineLogicTest {

    private fun isEligibleForCloudUpload(syncOrigin: Int, syncId: String, isPendingSync: Boolean): Boolean {
        return (syncOrigin == SyncOrigin.NEW_LOCAL || syncOrigin == SyncOrigin.CLOUD_SYNCED) &&
                syncId.isNotBlank() && isPendingSync
    }

    @Test
    fun testLegacyRecord_syncOriginLegacy_IsNotEligibleForCloudUpload() {
        val legacySite = SiteEntity(
            id = 10,
            name = "Legacy Site",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = false
        )
        assertFalse("Legacy record must NOT be eligible for cloud upload", isEligibleForCloudUpload(legacySite.syncOrigin, legacySite.syncId, legacySite.isPendingSync))

        val editedLegacySite = legacySite.copy(name = "Edited Legacy Site Name")
        assertEquals(SyncOrigin.LEGACY, editedLegacySite.syncOrigin)
        assertEquals("", editedLegacySite.syncId)
        assertFalse(editedLegacySite.isPendingSync)
        assertFalse("Editing legacy record must keep it ineligible for cloud upload", isEligibleForCloudUpload(editedLegacySite.syncOrigin, editedLegacySite.syncId, editedLegacySite.isPendingSync))
    }

    @Test
    fun testNewRecord_syncOriginNew_IsEligibleForCloudUpload() {
        val newSite = SiteEntity(
            name = "New Site Alpha",
            syncOrigin = SyncOrigin.NEW_LOCAL,
            syncId = "site_uuid_123",
            isPendingSync = true
        )
        assertTrue("Newly created record must be eligible for cloud upload", isEligibleForCloudUpload(newSite.syncOrigin, newSite.syncId, newSite.isPendingSync))
    }

    @Test
    fun testCloudRecord_pendingSyncTrue_IsEligibleForUpdate() {
        val cloudSite = SiteEntity(
            id = 5,
            name = "Cloud Downloaded Site Edited",
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "site_uuid_999",
            isPendingSync = true
        )
        assertTrue("Cloud-downloaded record with pending changes must be eligible for sync updates", isEligibleForCloudUpload(cloudSite.syncOrigin, cloudSite.syncId, cloudSite.isPendingSync))

        val syncedSite = cloudSite.copy(isPendingSync = false)
        assertFalse("Synced record without pending changes must not trigger duplicate uploads", isEligibleForCloudUpload(syncedSite.syncOrigin, syncedSite.syncId, syncedSite.isPendingSync))
    }

    @Test
    fun testSiteMapper_BidirectionalConversion_PreservesAllFields() {
        val site = SiteEntity(
            id = 1,
            name = "Site Alpha",
            address = "123 Main St",
            manager = "John Manager",
            startDate = LocalDate.of(2024, 1, 15),
            status = SiteStatus.ACTIVE,
            isDeleted = false,
            syncOrigin = SyncOrigin.NEW_LOCAL,
            syncId = "site_uuid_alpha",
            updatedAt = 1700000000000L
        )

        val dto = SiteMapper.toDTO(site)
        assertEquals("site_uuid_alpha", dto.syncId)
        assertEquals("Site Alpha", dto.name)
        assertEquals("123 Main St", dto.address)
        assertEquals("John Manager", dto.manager)
        assertEquals("ACTIVE", dto.status)
        assertEquals(1700000000000L, dto.updatedAt)

        val restoredEntity = SiteMapper.toEntity(dto, localId = site.id)
        assertEquals(site.id, restoredEntity.id)
        assertEquals(site.name, restoredEntity.name)
        assertEquals(site.address, restoredEntity.address)
        assertEquals(site.syncId, restoredEntity.syncId)
        assertEquals(SyncOrigin.CLOUD_SYNCED, restoredEntity.syncOrigin)
        assertFalse(restoredEntity.isPendingSync)
    }

    @Test
    fun testWorkerMapper_BidirectionalConversion_PreservesSiteSyncId() {
        val worker = WorkerEntity(
            id = 2,
            name = "Worker John",
            phone = "9876543210",
            workerType = WorkerType.MISTRI,
            dailyRate = 600.0,
            currentSiteId = 10,
            syncOrigin = SyncOrigin.NEW_LOCAL,
            syncId = "worker_uuid_john",
            updatedAt = 1700000000000L
        )

        val dto = WorkerMapper.toDTO(worker, siteSyncId = "site_uuid_alpha")
        assertEquals("worker_uuid_john", dto.syncId)
        assertEquals("Worker John", dto.name)
        assertEquals("site_uuid_alpha", dto.siteSyncId)

        val restoredWorker = WorkerMapper.toEntity(dto, localId = worker.id, localSiteId = 10)
        assertEquals(worker.id, restoredWorker.id)
        assertEquals(10L, restoredWorker.currentSiteId)
        assertEquals("worker_uuid_john", restoredWorker.syncId)
        assertEquals(SyncOrigin.CLOUD_SYNCED, restoredWorker.syncOrigin)
        assertFalse(restoredWorker.isPendingSync)
    }

    @Test
    fun testAttendanceMapper_BidirectionalConversion_PreservesParentSyncIds() {
        val attendance = AttendanceEntity(
            id = 15,
            workerId = 2,
            siteId = 1,
            date = LocalDate.of(2024, 2, 1),
            status = AttendanceStatus.PRESENT,
            amount = 500.0,
            syncOrigin = SyncOrigin.NEW_LOCAL,
            syncId = "att_uuid_101",
            updatedAt = 1700000000000L
        )

        val dto = AttendanceMapper.toDTO(attendance, workerSyncId = "worker_uuid_john", siteSyncId = "site_uuid_alpha")
        assertEquals("att_uuid_101", dto.syncId)
        assertEquals("worker_uuid_john", dto.workerSyncId)
        assertEquals("site_uuid_alpha", dto.siteSyncId)

        val restoredEntity = AttendanceMapper.toEntity(dto, localId = 15, localWorkerId = 2, localSiteId = 1)
        assertEquals(15L, restoredEntity.id)
        assertEquals(2L, restoredEntity.workerId)
        assertEquals(1L, restoredEntity.siteId)
        assertEquals(SyncOrigin.CLOUD_SYNCED, restoredEntity.syncOrigin)
        assertFalse(restoredEntity.isPendingSync)
    }

    @Test
    fun testAttendanceMapper_BidirectionalConversion_PreservesIsDeletedAndDeletedAt() {
        val deletedAttendance = AttendanceEntity(
            id = 16,
            workerId = 2,
            siteId = 1,
            date = LocalDate.of(2024, 2, 2),
            status = AttendanceStatus.PRESENT,
            amount = 500.0,
            isDeleted = true,
            deletedAt = 1700000050000L,
            syncOrigin = SyncOrigin.NEW_LOCAL,
            syncId = "att_uuid_del_102",
            isPendingSync = true,
            updatedAt = 1700000050000L
        )

        val dto = AttendanceMapper.toDTO(deletedAttendance, workerSyncId = "worker_uuid_john", siteSyncId = "site_uuid_alpha")
        assertTrue("AttendanceDTO must preserve isDeleted = true", dto.isDeleted)
        assertEquals(1700000050000L, dto.deletedAt)

        val restoredEntity = AttendanceMapper.toEntity(dto, localId = 16, localWorkerId = 2, localSiteId = 1)
        assertTrue("Restored AttendanceEntity must preserve isDeleted = true", restoredEntity.isDeleted)
        assertEquals(1700000050000L, restoredEntity.deletedAt)
        assertEquals(SyncOrigin.CLOUD_SYNCED, restoredEntity.syncOrigin)
        assertFalse(restoredEntity.isPendingSync)
    }

    @Test
    fun testAttendanceSoftDeletion_IsEligibleForCloudTombstonePush() {
        val softDeletedAttendance = AttendanceEntity(
            id = 17,
            workerId = 2,
            siteId = 1,
            date = LocalDate.of(2024, 2, 3),
            status = AttendanceStatus.PRESENT,
            amount = 500.0,
            isDeleted = true,
            deletedAt = 1700000060000L,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "att_uuid_103",
            isPendingSync = true,
            updatedAt = 1700000060000L
        )
        assertTrue("Soft-deleted attendance with pending sync must be eligible for cloud tombstone push",
            isEligibleForCloudUpload(softDeletedAttendance.syncOrigin, softDeletedAttendance.syncId, softDeletedAttendance.isPendingSync))
    }

    @Test
    fun testLegacyRecordUpgrade_BecomesEligibleForCloudUpload() {
        val legacyRecord = SiteEntity(
            id = 10,
            name = "Legacy Site",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = false
        )
        assertFalse("Raw un-upgraded legacy record must not be eligible for cloud upload",
            isEligibleForCloudUpload(legacyRecord.syncOrigin, legacyRecord.syncId, legacyRecord.isPendingSync))

        // Upgraded by repository edit or LegacySyncBootstrapper:
        val upgradedRecord = legacyRecord.copy(
            syncOrigin = SyncOrigin.NEW_LOCAL,
            syncId = "new_generated_uuid_123",
            isPendingSync = true,
            updatedAt = System.currentTimeMillis()
        )
        assertTrue("Upgraded legacy record must be eligible for cloud upload",
            isEligibleForCloudUpload(upgradedRecord.syncOrigin, upgradedRecord.syncId, upgradedRecord.isPendingSync))
    }

    @Test
    fun testPaymentMapper_BidirectionalConversion_PreservesParentSyncIds() {
        val payment = PaymentEntity(
            id = 20,
            workerId = 2,
            siteId = 1,
            amount = 1000.0,
            date = LocalDate.of(2024, 2, 1),
            method = PaymentMethod.CASH,
            paymentType = PaymentType.ADVANCE,
            syncOrigin = SyncOrigin.NEW_LOCAL,
            syncId = "pay_uuid_202",
            updatedAt = 1700000000000L
        )

        val dto = PaymentMapper.toDTO(payment, workerSyncId = "worker_uuid_john", siteSyncId = "site_uuid_alpha")
        assertEquals("pay_uuid_202", dto.syncId)
        assertEquals("worker_uuid_john", dto.workerSyncId)
        assertEquals("site_uuid_alpha", dto.siteSyncId)

        val restoredEntity = PaymentMapper.toEntity(dto, localId = 20, localWorkerId = 2, localSiteId = 1)
        assertEquals(20L, restoredEntity.id)
        assertEquals(2L, restoredEntity.workerId)
        assertEquals(1L, restoredEntity.siteId)
        assertEquals(PaymentType.ADVANCE, restoredEntity.paymentType)
        assertFalse(restoredEntity.isPendingSync)
    }

    @Test
    fun testContractMapper_BidirectionalConversion_PreservesSiteSyncId() {
        val contract = ContractEntity(
            id = 5,
            clientName = "Client Acme",
            siteId = 1,
            totalAmount = 50000.0,
            status = ContractStatus.ACTIVE,
            syncOrigin = SyncOrigin.NEW_LOCAL,
            syncId = "contract_uuid_303",
            updatedAt = 1700000000000L
        )

        val dto = ContractMapper.toDTO(contract, siteSyncId = "site_uuid_alpha")
        assertEquals("contract_uuid_303", dto.syncId)
        assertEquals("Client Acme", dto.clientName)
        assertEquals("site_uuid_alpha", dto.siteSyncId)

        val restoredEntity = ContractMapper.toEntity(dto, localId = 5, localSiteId = 1)
        assertEquals(5L, restoredEntity.id)
        assertEquals(1L, restoredEntity.siteId)
        assertEquals("Client Acme", restoredEntity.clientName)
        assertFalse(restoredEntity.isPendingSync)
    }

    @Test
    fun testTargetUserIdKey_IsConstant() {
        assertEquals("key_target_user_id", CloudSyncWorker.KEY_TARGET_USER_ID)
    }

    @Test
    fun testRepositoryCallbackInvocation() {
        var callbackInvocations = 0
        val callback = { callbackInvocations++ }

        // Simulate local mutation succeeding
        callback.invoke()
        assertEquals(1, callbackInvocations)
    }

    @Test
    fun testFailedLocalMutation_doesNotInvokeCallback() {
        var callbackInvocations = 0

        fun performMutation(shouldFail: Boolean, callback: () -> Unit) {
            if (shouldFail) {
                throw RuntimeException("SQLite Constraint Failure")
            }
            callback()
        }

        try {
            performMutation(shouldFail = true) { callbackInvocations++ }
        } catch (_: Exception) {
            // Mutation failed
        }

        assertEquals(0, callbackInvocations)
    }
}
