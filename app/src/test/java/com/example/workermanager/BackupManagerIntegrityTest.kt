package com.example.workermanager

import com.example.workermanager.data.entity.AttendanceEntity
import com.example.workermanager.data.entity.AttendanceStatus
import com.example.workermanager.data.entity.ClientPaymentEntity
import com.example.workermanager.data.entity.ClientPaymentMethod
import com.example.workermanager.data.entity.ContractEntity
import com.example.workermanager.data.entity.ContractStatus
import com.example.workermanager.data.entity.ContractorMaterialCategory
import com.example.workermanager.data.entity.ContractorMaterialEntity
import com.example.workermanager.data.entity.ContractorMaterialTransactionEntity
import com.example.workermanager.data.entity.ContractorMaterialTransactionType
import com.example.workermanager.data.entity.ContractorMaterialUnit
import com.example.workermanager.data.entity.ExpenseCategory
import com.example.workermanager.data.entity.ExpensePaymentMethod
import com.example.workermanager.data.entity.MaterialCategory
import com.example.workermanager.data.entity.MaterialEntity
import com.example.workermanager.data.entity.MaterialTransactionEntity
import com.example.workermanager.data.entity.MaterialTransactionType
import com.example.workermanager.data.entity.MaterialUnit
import com.example.workermanager.data.entity.PaymentEntity
import com.example.workermanager.data.entity.PaymentMethod
import com.example.workermanager.data.entity.PaymentType
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.SiteExpenseEntity
import com.example.workermanager.data.entity.SiteExpensePaymentEntity
import com.example.workermanager.data.entity.WorkerEntity
import com.example.workermanager.data.entity.WorkerSiteHistoryEntity
import com.example.workermanager.data.entity.WorkerType
import com.example.workermanager.util.BackupManager
import com.example.workermanager.util.BackupPayload
import com.example.workermanager.util.IntegrityValidationResult
import com.example.workermanager.util.OwnershipValidationResult
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupManagerIntegrityTest {

    private fun createValidSamplePayload(): BackupPayload {
        val worker = WorkerEntity(
            id = 1,
            name = "John",
            phone = "1234567890",
            workerType = WorkerType.MISTRI,
            dailyRate = 500.0,
            currentSiteId = 10
        )
        val site = SiteEntity(
            id = 10,
            name = "Site Alpha"
        )
        val attendance = AttendanceEntity(
            id = 100,
            workerId = 1,
            siteId = 10,
            date = LocalDate.of(2024, 1, 1),
            status = AttendanceStatus.PRESENT,
            amount = 500.0
        )
        val payment = PaymentEntity(
            id = 200,
            workerId = 1,
            siteId = 10,
            amount = 1000.0,
            date = LocalDate.of(2024, 1, 2),
            method = PaymentMethod.CASH,
            paymentType = PaymentType.ADVANCE
        )
        val contract = ContractEntity(
            id = 300,
            clientName = "Client Acme",
            siteId = 10,
            totalAmount = 25000.0,
            status = ContractStatus.ACTIVE
        )
        val clientPayment = ClientPaymentEntity(
            id = 400,
            contractId = 300,
            amount = 5000.0,
            paymentDate = LocalDate.of(2024, 1, 3),
            method = ClientPaymentMethod.CASH
        )
        val history = WorkerSiteHistoryEntity(
            id = 500,
            workerId = 1,
            siteId = 10,
            startDate = LocalDate.of(2024, 1, 1)
        )
        val expense = SiteExpenseEntity(
            id = 600,
            siteId = 10,
            description = "Cement",
            amount = 3000.0,
            expenseDate = LocalDate.of(2024, 1, 4),
            category = ExpenseCategory.CONSTRUCTION_MATERIAL,
            paymentMethod = ExpensePaymentMethod.CASH
        )
        val expensePayment = SiteExpensePaymentEntity(
            id = 700,
            expenseId = 600,
            amount = 1500.0,
            paymentDate = LocalDate.of(2024, 1, 5),
            paymentMethod = ExpensePaymentMethod.CASH
        )
        val material = MaterialEntity(
            id = 800,
            name = "Sand",
            category = MaterialCategory.CIVIL,
            unit = MaterialUnit.BAGS
        )
        val materialTx = MaterialTransactionEntity(
            id = 900,
            materialId = 800,
            siteId = 10,
            transactionType = MaterialTransactionType.RECEIVED,
            transactionDate = LocalDate.of(2024, 1, 6),
            quantity = 50.0,
            unit = "Bags"
        )
        val contractorMat = ContractorMaterialEntity(
            id = 1000,
            name = "Steel Shuttering Plate",
            category = ContractorMaterialCategory.SHUTTERING_ROOF_CASTING,
            unit = ContractorMaterialUnit.PCS
        )
        val contractorMatTx = ContractorMaterialTransactionEntity(
            id = 1100,
            materialId = 1000,
            siteId = 10,
            transactionType = ContractorMaterialTransactionType.ADDED,
            transactionDate = LocalDate.of(2024, 1, 7),
            quantity = 5.0,
            unit = "Pcs"
        )

        return BackupPayload(
            version = 2,
            appName = "WorkCraft",
            timestamp = System.currentTimeMillis(),
            ownerUid = "user_alpha",
            workers = listOf(worker),
            sites = listOf(site),
            attendance = listOf(attendance),
            payments = listOf(payment),
            contracts = listOf(contract),
            clientPayments = listOf(clientPayment),
            workerSiteHistory = listOf(history),
            siteAttendance = emptyList(),
            siteExpenses = listOf(expense),
            siteExpensePayments = listOf(expensePayment),
            materials = listOf(material),
            materialTransactions = listOf(materialTx),
            contractorMaterials = listOf(contractorMat),
            contractorMaterialTransactions = listOf(contractorMatTx)
        )
    }

    @Test
    fun testValidPayload_passesIntegrityCheck() {
        val payload = createValidSamplePayload()
        val result = BackupManager.validateBackupIntegrity(payload)
        assertTrue("Valid payload must pass integrity check", result is IntegrityValidationResult.Valid)
    }

    @Test
    fun testInvalidAppName_failsIntegrityCheck() {
        val payload = createValidSamplePayload().copy(appName = "MaliciousCraft")
        val result = BackupManager.validateBackupIntegrity(payload)
        assertTrue("Payload with incorrect appName must fail integrity check", result is IntegrityValidationResult.Invalid)
    }

    @Test
    fun testUnsupportedVersion_failsIntegrityCheck() {
        val payloadZero = createValidSamplePayload().copy(version = 0)
        assertTrue(BackupManager.validateBackupIntegrity(payloadZero) is IntegrityValidationResult.Invalid)

        val payloadFuture = createValidSamplePayload().copy(version = 99)
        assertTrue(BackupManager.validateBackupIntegrity(payloadFuture) is IntegrityValidationResult.Invalid)
    }

    @Test
    fun testAttendance_missingWorker_failsIntegrityCheck() {
        val payload = createValidSamplePayload().copy(
            attendance = listOf(
                AttendanceEntity(id = 999, workerId = 99999, siteId = 10, date = LocalDate.now(), status = AttendanceStatus.PRESENT, amount = 0.0)
            )
        )
        val result = BackupManager.validateBackupIntegrity(payload)
        assertTrue("Attendance referencing nonexistent workerId must fail integrity check", result is IntegrityValidationResult.Invalid)
    }

    @Test
    fun testAttendance_missingSite_failsIntegrityCheck() {
        val payload = createValidSamplePayload().copy(
            attendance = listOf(
                AttendanceEntity(id = 999, workerId = 1, siteId = 99999, date = LocalDate.now(), status = AttendanceStatus.PRESENT, amount = 0.0)
            )
        )
        val result = BackupManager.validateBackupIntegrity(payload)
        assertTrue("Attendance referencing nonexistent siteId must fail integrity check", result is IntegrityValidationResult.Invalid)
    }

    @Test
    fun testPayment_missingWorker_failsIntegrityCheck() {
        val payload = createValidSamplePayload().copy(
            payments = listOf(
                PaymentEntity(id = 999, workerId = 99999, siteId = 10, amount = 100.0, date = LocalDate.now(), method = PaymentMethod.CASH, paymentType = PaymentType.ADVANCE)
            )
        )
        val result = BackupManager.validateBackupIntegrity(payload)
        assertTrue("Payment referencing nonexistent workerId must fail integrity check", result is IntegrityValidationResult.Invalid)
    }

    @Test
    fun testContract_missingSite_failsIntegrityCheck() {
        val payload = createValidSamplePayload().copy(
            contracts = listOf(
                ContractEntity(id = 999, clientName = "Bad Site Contract", siteId = 99999, totalAmount = 5000.0, status = ContractStatus.ACTIVE)
            )
        )
        val result = BackupManager.validateBackupIntegrity(payload)
        assertTrue("Contract referencing nonexistent siteId must fail integrity check", result is IntegrityValidationResult.Invalid)
    }

    @Test
    fun testClientPayment_missingContract_failsIntegrityCheck() {
        val payload = createValidSamplePayload().copy(
            clientPayments = listOf(
                ClientPaymentEntity(id = 999, contractId = 99999, amount = 500.0, paymentDate = LocalDate.now(), method = ClientPaymentMethod.CASH)
            )
        )
        val result = BackupManager.validateBackupIntegrity(payload)
        assertTrue("Client payment referencing nonexistent contractId must fail integrity check", result is IntegrityValidationResult.Invalid)
    }

    @Test
    fun testSiteExpensePayment_missingExpense_failsIntegrityCheck() {
        val payload = createValidSamplePayload().copy(
            siteExpensePayments = listOf(
                SiteExpensePaymentEntity(id = 999, expenseId = 99999, amount = 500.0, paymentDate = LocalDate.now(), paymentMethod = ExpensePaymentMethod.CASH)
            )
        )
        val result = BackupManager.validateBackupIntegrity(payload)
        assertTrue("Expense payment referencing nonexistent expenseId must fail integrity check", result is IntegrityValidationResult.Invalid)
    }

    @Test
    fun testMaterialTransaction_missingMaterial_failsIntegrityCheck() {
        val payload = createValidSamplePayload().copy(
            materialTransactions = listOf(
                MaterialTransactionEntity(id = 999, materialId = 99999, siteId = 10, transactionType = MaterialTransactionType.RECEIVED, transactionDate = LocalDate.now(), quantity = 10.0, unit = "Bags")
            )
        )
        val result = BackupManager.validateBackupIntegrity(payload)
        assertTrue("Material transaction referencing nonexistent materialId must fail integrity check", result is IntegrityValidationResult.Invalid)
    }

    @Test
    fun testContractorMaterialTransaction_missingContractorMaterial_failsIntegrityCheck() {
        val payload = createValidSamplePayload().copy(
            contractorMaterialTransactions = listOf(
                ContractorMaterialTransactionEntity(id = 999, materialId = 99999, siteId = 10, transactionType = ContractorMaterialTransactionType.ADDED, transactionDate = LocalDate.now(), quantity = 10.0, unit = "Pcs")
            )
        )
        val result = BackupManager.validateBackupIntegrity(payload)
        assertTrue("Contractor material transaction referencing nonexistent materialId must fail integrity check", result is IntegrityValidationResult.Invalid)
    }

    @Test
    fun testOwnershipValidation_matchingUid_isValid() {
        val result = BackupManager.validateBackupOwnership(payloadOwnerUid = "user_123", currentFirebaseUid = "user_123")
        assertTrue("Matching UID must be valid", result is OwnershipValidationResult.Valid)
    }

    @Test
    fun testOwnershipValidation_nullPayloadUid_isInvalid() {
        val result = BackupManager.validateBackupOwnership(payloadOwnerUid = null, currentFirebaseUid = "user_123")
        assertTrue("Missing owner UID in payload must be rejected for security", result is OwnershipValidationResult.Invalid)
    }

    @Test
    fun testOwnershipValidation_differentUid_isInvalid() {
        val result = BackupManager.validateBackupOwnership(payloadOwnerUid = "user_alice", currentFirebaseUid = "user_bob")
        assertTrue("Mismatched UID must be rejected", result is OwnershipValidationResult.Invalid)
    }
}
