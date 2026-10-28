package com.example.workermanager.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

data class SmokeTestStageResult(
    val stageName: String,
    val isSuccess: Boolean,
    val details: String
)

data class SmokeTestResult(
    val isOverallSuccess: Boolean,
    val testRunId: String,
    val authenticatedUid: String,
    val writeStage: SmokeTestStageResult,
    val readStage: SmokeTestStageResult,
    val cleanupStage: SmokeTestStageResult,
    val errorMessage: String? = null
)

object FirestoreSmokeTest {

    private const val TAG = "FirestoreSmokeTest"

    suspend fun runConnectivityTest(): SmokeTestResult = withContext(Dispatchers.IO) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid?.ifBlank { null }

        if (uid.isNullOrBlank()) {
            val emptyStage = SmokeTestStageResult("Authentication", false, "No authenticated user found.")
            return@withContext SmokeTestResult(
                isOverallSuccess = false,
                testRunId = "N/A",
                authenticatedUid = "UNAUTHENTICATED",
                writeStage = emptyStage,
                readStage = emptyStage,
                cleanupStage = emptyStage,
                errorMessage = "Authentication Check Failed: No user signed in."
            )
        }

        val testRunId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val diagCollectionRef = FirestoreProvider.getUserDocumentRef()
            .collection("_diagnostics")
        val docRef = diagCollectionRef.document(testRunId)

        var writeResult = SmokeTestStageResult("Write", false, "Not executed")
        var readResult = SmokeTestStageResult("Read", false, "Not executed")
        var cleanupResult = SmokeTestStageResult("Cleanup", false, "Not executed")

        try {
            // STAGE 1: WRITE
            val payload = hashMapOf<String, Any>(
                "testRunId" to testRunId,
                "createdAt" to timestamp,
                "status" to "PENDING_VERIFICATION"
            )

            Log.d(TAG, "[Stage 1: Write] Writing diagnostic document: $testRunId under authenticated user namespace")
            docRef.set(payload).await()
            writeResult = SmokeTestStageResult(
                stageName = "Write",
                isSuccess = true,
                details = "Diagnostic document written to path: users/UID/_diagnostics/$testRunId"
            )

            // STAGE 2: READ
            Log.d(TAG, "[Stage 2: Read] Reading diagnostic document back: $testRunId")
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val fetchedTestRunId = snapshot.getString("testRunId")
                val fetchedStatus = snapshot.getString("status")
                val fetchedTimestamp = snapshot.getLong("createdAt")

                if (fetchedTestRunId == testRunId && fetchedStatus == "PENDING_VERIFICATION" && fetchedTimestamp == timestamp) {
                    readResult = SmokeTestStageResult(
                        stageName = "Read",
                        isSuccess = true,
                        details = "Successfully read diagnostic document back. Verified testRunId, createdAt, and status fields match."
                    )
                } else {
                    readResult = SmokeTestStageResult(
                        stageName = "Read",
                        isSuccess = false,
                        details = "Field content mismatch: expected testRunId=$testRunId, got=$fetchedTestRunId"
                    )
                    return@withContext SmokeTestResult(
                        isOverallSuccess = false,
                        testRunId = testRunId,
                        authenticatedUid = uid,
                        writeStage = writeResult,
                        readStage = readResult,
                        cleanupStage = cleanupResult,
                        errorMessage = "Data Verification Mismatch during Read Stage."
                    )
                }
            } else {
                readResult = SmokeTestStageResult(
                    stageName = "Read",
                    isSuccess = false,
                    details = "Document does not exist after write operation."
                )
                return@withContext SmokeTestResult(
                    isOverallSuccess = false,
                    testRunId = testRunId,
                    authenticatedUid = uid,
                    writeStage = writeResult,
                    readStage = readResult,
                    cleanupStage = cleanupResult,
                    errorMessage = "Document missing during Read Stage."
                )
            }

            // STAGE 3: CLEANUP (Delete exact diagnostic document)
            Log.d(TAG, "[Stage 3: Cleanup] Deleting diagnostic document: $testRunId")
            docRef.delete().await()

            // Verify deletion
            val postDeleteSnapshot = docRef.get().await()
            if (!postDeleteSnapshot.exists()) {
                cleanupResult = SmokeTestStageResult(
                    stageName = "Cleanup",
                    isSuccess = true,
                    details = "Diagnostic test document deleted successfully and confirmed non-existent."
                )
            } else {
                cleanupResult = SmokeTestStageResult(
                    stageName = "Cleanup",
                    isSuccess = false,
                    details = "Diagnostic document still exists after delete call."
                )
            }

            val overallSuccess = writeResult.isSuccess && readResult.isSuccess && cleanupResult.isSuccess

            SmokeTestResult(
                isOverallSuccess = overallSuccess,
                testRunId = testRunId,
                authenticatedUid = uid,
                writeStage = writeResult,
                readStage = readResult,
                cleanupStage = cleanupResult,
                errorMessage = if (overallSuccess) null else "Cleanup verification failed."
            )

        } catch (e: Exception) {
            Log.e(TAG, "Smoke test failed with exception", e)
            val errorDetails = e.localizedMessage ?: e.message ?: "Unknown Firestore error"

            if (!writeResult.isSuccess) {
                writeResult = SmokeTestStageResult("Write", false, "Write failed: $errorDetails")
            } else if (!readResult.isSuccess) {
                readResult = SmokeTestStageResult("Read", false, "Read failed: $errorDetails")
            } else if (!cleanupResult.isSuccess) {
                cleanupResult = SmokeTestStageResult("Cleanup", false, "Cleanup failed: $errorDetails")
            }

            SmokeTestResult(
                isOverallSuccess = false,
                testRunId = testRunId,
                authenticatedUid = uid,
                writeStage = writeResult,
                readStage = readResult,
                cleanupStage = cleanupResult,
                errorMessage = "Firestore Error: $errorDetails"
            )
        }
    }
}
