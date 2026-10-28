package com.example.workermanager.viewmodel

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workermanager.data.preference.AuthPreferenceManager
import com.example.workermanager.data.preference.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class AuthViewModel(
    private val authPreferenceManager: AuthPreferenceManager
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authPreferenceManager.isLoggedIn
    val userProfile: StateFlow<UserProfile?> = authPreferenceManager.userProfile

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun generateNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun signInWithIdToken(
        idToken: String,
        email: String,
        displayName: String,
        photoUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                Log.d("AuthViewModel", "[Step 5-Intent] Exchanging Google ID token from Intent with Firebase Auth...")
                val firebaseAuth = FirebaseAuth.getInstance()
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val firebaseUser = authResult.user

                if (firebaseUser == null || firebaseUser.uid.isBlank()) {
                    val err = "Firebase Authentication returned a null/blank user."
                    Log.e("AuthViewModel", err)
                    _errorMessage.value = err
                    _isLoading.value = false
                    onError(err)
                    return@launch
                }

                val uid = firebaseUser.uid
                Log.d("AuthViewModel", "[Step 6-Intent] Firebase Authentication successful. Verified UID: $uid")

                val profile = UserProfile(
                    userId = uid,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl
                )

                Log.d("AuthViewModel", "[Step 7-Intent] Saving user session and invoking onSuccess...")
                authPreferenceManager.saveSession(profile)
                _isLoading.value = false
                onSuccess()
            } catch (e: FirebaseAuthException) {
                val errorMsg = "Firebase Authentication error (${e.errorCode}): ${e.localizedMessage}"
                Log.e("AuthViewModel", "[FirebaseAuthException] ${e.errorCode}: ${e.localizedMessage}", e)
                _errorMessage.value = errorMsg
                _isLoading.value = false
                onError(errorMsg)
            } catch (e: Exception) {
                val errorMsg = if (e is java.io.IOException || e is java.net.UnknownHostException) {
                    "Network error. Please check your internet connection."
                } else {
                    "Sign-in failed (${e.javaClass.simpleName}): ${e.localizedMessage ?: "Unknown error"}"
                }
                Log.e("AuthViewModel", "[Exception] ${e.javaClass.name}: ${e.localizedMessage}", e)
                _errorMessage.value = errorMsg
                _isLoading.value = false
                onError(errorMsg)
            }
        }
    }

    fun signInWithGoogle(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFallbackToIntent: (() -> Unit)? = null
    ) {
        if (_isLoading.value) return // Prevent duplicate login requests

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val hostActivity = context.findActivity() ?: context
                val nonce = generateNonce()
                Log.d("AuthViewModel", "[Step 1] Initiating Google Sign-In with serverClientId: $DEFAULT_WEB_CLIENT_ID on $hostActivity")
                val credentialManager = CredentialManager.create(hostActivity)

                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(DEFAULT_WEB_CLIENT_ID)
                    .setNonce(nonce)
                    .build()

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(DEFAULT_WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .setNonce(nonce)
                    .build()

                val primaryRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .addCredentialOption(googleIdOption)
                    .build()

                Log.d("AuthViewModel", "[Step 2] Sending GetCredentialRequest with GetSignInWithGoogleOption & GetGoogleIdOption to CredentialManager...")
                val result = credentialManager.getCredential(
                    request = primaryRequest,
                    context = hostActivity
                )

                val credential = result.credential
                Log.d("AuthViewModel", "[Step 3] Credential response received: class=${credential.javaClass.name}, type=${credential.type}")

                if (credential is CustomCredential) {
                    val idToken: String
                    val email: String
                    val displayName: String
                    val photoUrl: String

                    val googleIdTokenCredential = try {
                        GoogleIdTokenCredential.createFrom(credential.data)
                    } catch (parseException: Exception) {
                        Log.w("AuthViewModel", "[Step 3a] GoogleIdTokenCredential.createFrom parsing exception for type='${credential.type}': ${parseException.message}", parseException)
                        null
                    }

                    if (googleIdTokenCredential != null) {
                        idToken = googleIdTokenCredential.idToken
                        email = googleIdTokenCredential.id
                        displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                        photoUrl = googleIdTokenCredential.profilePictureUri?.toString() ?: ""
                        Log.d("AuthViewModel", "[Step 3b] Successfully parsed via GoogleIdTokenCredential.createFrom. User: $email")
                    } else {
                        // Fallback bundle extraction
                        idToken = credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
                            ?: credential.data.getString("idToken")
                            ?: ""
                        email = credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID")
                            ?: credential.data.getString("id")
                            ?: ""
                        displayName = credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_DISPLAY_NAME")
                            ?: email.substringBefore("@")
                        photoUrl = credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_PROFILE_PICTURE_URI")
                            ?: ""
                        Log.d("AuthViewModel", "[Step 3c] Extracted from bundle directly. Email: $email, ID Token present: ${idToken.isNotBlank()}")
                    }

                    Log.d("AuthViewModel", "[Step 4] Token extraction complete. Email: $email, Token Length: ${idToken.length}")

                    if (idToken.isBlank()) {
                        val err = "Received empty Google ID token from Credential Manager (Type: ${credential.type})."
                        Log.e("AuthViewModel", err)
                        _errorMessage.value = err
                        _isLoading.value = false
                        onError(err)
                        return@launch
                    }

                    Log.d("AuthViewModel", "[Step 5] Exchanging Google ID token with Firebase Auth...")
                    val firebaseAuth = FirebaseAuth.getInstance()
                    val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                    val firebaseUser = authResult.user

                    if (firebaseUser == null || firebaseUser.uid.isBlank()) {
                        val err = "Firebase Authentication returned a null/blank user."
                        Log.e("AuthViewModel", err)
                        _errorMessage.value = err
                        _isLoading.value = false
                        onError(err)
                        return@launch
                    }

                    val uid = firebaseUser.uid
                    Log.d("AuthViewModel", "[Step 6] Firebase Authentication successful. Verified UID: $uid")

                    val profile = UserProfile(
                        userId = uid,
                        email = email,
                        displayName = displayName,
                        photoUrl = photoUrl
                    )

                    Log.d("AuthViewModel", "[Step 7] Saving user session and invoking onSuccess...")
                    authPreferenceManager.saveSession(profile)
                    _isLoading.value = false
                    onSuccess()
                } else {
                    val errorMsg = "Unrecognized credential response type: ${credential.javaClass.name} / ${credential.type}"
                    Log.e("AuthViewModel", errorMsg)
                    _errorMessage.value = errorMsg
                    _isLoading.value = false
                    onError(errorMsg)
                }
            } catch (e: NoCredentialException) {
                Log.w("AuthViewModel", "[Step 2a] CredentialManager returned NoCredentialException: ${e.message}. Triggering Play Services Google Sign-In intent fallback...", e)
                if (onFallbackToIntent != null) {
                    onFallbackToIntent()
                } else {
                    _isLoading.value = false
                    val errorMsg = "No Google credentials available: ${e.localizedMessage ?: "No accounts on device"}"
                    _errorMessage.value = errorMsg
                    onError(errorMsg)
                }
            } catch (e: GetCredentialCancellationException) {
                val rawMsg = e.message ?: e.localizedMessage ?: ""
                Log.e("AuthViewModel", "[GetCredentialCancellationException] ${e.javaClass.name}: $rawMsg", e)

                if (rawMsg.contains("[16]") || rawMsg.contains("reauth", ignoreCase = true)) {
                    Log.w("AuthViewModel", "[Step 2a] Received status [16] Account reauth failed. Triggering Play Services Google Sign-In intent fallback...", e)
                    if (onFallbackToIntent != null) {
                        onFallbackToIntent()
                    } else {
                        _isLoading.value = false
                        val err = "Google Sign-In re-authentication failed [Code 16]."
                        _errorMessage.value = err
                        onError(err)
                    }
                } else {
                    val isUserCancel = rawMsg.contains("user", ignoreCase = true) ||
                            rawMsg.contains("canceled", ignoreCase = true) ||
                            rawMsg.contains("cancelled", ignoreCase = true)

                    if (isUserCancel) {
                        _isLoading.value = false
                        onError("Sign-in cancelled.")
                    } else if (onFallbackToIntent != null) {
                        Log.w("AuthViewModel", "[Step 2a] CredentialManager prompt dismissed/failed: $rawMsg. Triggering Play Services Google Sign-In intent fallback...", e)
                        onFallbackToIntent()
                    } else {
                        _isLoading.value = false
                        val err = "Google Sign-In dismissed: ${rawMsg.ifBlank { "Credential error" }}"
                        _errorMessage.value = err
                        onError(err)
                    }
                }
            } catch (e: GetCredentialException) {
                Log.w("AuthViewModel", "[GetCredentialException] ${e.javaClass.name}: ${e.localizedMessage}. Triggering Play Services Google Sign-In intent fallback...", e)
                if (onFallbackToIntent != null) {
                    onFallbackToIntent()
                } else {
                    _isLoading.value = false
                    val errorMsg = "Google Sign-In failed (${e.javaClass.simpleName}): ${e.localizedMessage ?: "Credential error"}"
                    _errorMessage.value = errorMsg
                    onError(errorMsg)
                }
            } catch (e: FirebaseAuthException) {
                val errorMsg = "Firebase Authentication error (${e.errorCode}): ${e.localizedMessage}"
                Log.e("AuthViewModel", "[FirebaseAuthException] ${e.errorCode}: ${e.localizedMessage}", e)
                _errorMessage.value = errorMsg
                _isLoading.value = false
                onError(errorMsg)
            } catch (e: Exception) {
                val errorMsg = if (e is java.io.IOException || e is java.net.UnknownHostException) {
                    "Network error. Please check your internet connection."
                } else {
                    "Sign-in failed (${e.javaClass.simpleName}): ${e.localizedMessage ?: "Unknown error"}"
                }
                Log.e("AuthViewModel", "[Exception] ${e.javaClass.name}: ${e.localizedMessage}", e)
                _errorMessage.value = errorMsg
                _isLoading.value = false
                onError(errorMsg)
            }
        }
    }

    fun updateProfile(
        newDisplayName: String,
        newPhotoUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val current = userProfile.value
                if (current == null) {
                    onError("User profile not found")
                    return@launch
                }
                val updatedProfile = current.copy(
                    displayName = newDisplayName,
                    photoUrl = newPhotoUrl
                )
                authPreferenceManager.saveSession(updatedProfile)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to update profile")
            }
        }
    }

    fun logout() {
        authPreferenceManager.logout()
    }

    companion object {
        /**
         * Web OAuth 2.0 Client ID from google-services.json for project workcraft-ad559.
         */
        const val DEFAULT_WEB_CLIENT_ID = "352309331151-ir0ornr6sj71327s05c4iof6smp8im6h.apps.googleusercontent.com"
    }
}

class AuthViewModelFactory(
    private val authPreferenceManager: AuthPreferenceManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authPreferenceManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
