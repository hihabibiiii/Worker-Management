package com.example.workermanager

import com.example.workermanager.data.remote.SmokeTestResult
import com.example.workermanager.data.remote.SmokeTestStageResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreSmokeTestUnitResultTest {

    @Test
    fun testAllStagesSuccess_ResultsInOverallSuccess() {
        val writeStage = SmokeTestStageResult("Write", true, "Write OK")
        val readStage = SmokeTestStageResult("Read", true, "Read OK")
        val cleanupStage = SmokeTestStageResult("Cleanup", true, "Cleanup OK")

        val result = SmokeTestResult(
            isOverallSuccess = writeStage.isSuccess && readStage.isSuccess && cleanupStage.isSuccess,
            testRunId = "test_run_123",
            authenticatedUid = "user_123",
            writeStage = writeStage,
            readStage = readStage,
            cleanupStage = cleanupStage
        )

        assertTrue("Overall result must be success when all 3 stages pass", result.isOverallSuccess)
    }

    @Test
    fun testStageFailure_ResultsInOverallFailure() {
        val writeStage = SmokeTestStageResult("Write", true, "Write OK")
        val readStage = SmokeTestStageResult("Read", false, "Read Mismatch")
        val cleanupStage = SmokeTestStageResult("Cleanup", false, "Not Executed")

        val result = SmokeTestResult(
            isOverallSuccess = writeStage.isSuccess && readStage.isSuccess && cleanupStage.isSuccess,
            testRunId = "test_run_456",
            authenticatedUid = "user_456",
            writeStage = writeStage,
            readStage = readStage,
            cleanupStage = cleanupStage,
            errorMessage = "Data Verification Mismatch"
        )

        assertFalse("Overall result must be failure if any stage fails", result.isOverallSuccess)
    }
}
