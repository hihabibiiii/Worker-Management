package com.example.workermanager.data.preference

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class UserProfile(
    val userId: String,
    val email: String,
    val displayName: String,
    val photoUrl: String
)

class AuthPreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("workcraft_auth_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(checkIsLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _userProfile = MutableStateFlow(getSavedUserProfile())
    val userProfile: StateFlow<UserProfile?> = _userProfile

    init {
        FirebaseAuth.getInstance().addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                val profile = UserProfile(
                    userId = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: (user.email?.substringBefore("@") ?: "User"),
                    photoUrl = user.photoUrl?.toString() ?: ""
                )
                saveSession(profile)
            } else {
                _isLoggedIn.value = false
                _userProfile.value = null
            }
        }
    }

    private fun checkIsLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null || getSavedUserProfile() != null
    }

    private fun getSavedUserProfile(): UserProfile? {
        val fbUser = FirebaseAuth.getInstance().currentUser
        if (fbUser != null) {
            return UserProfile(
                userId = fbUser.uid,
                email = fbUser.email ?: "",
                displayName = fbUser.displayName ?: (fbUser.email?.substringBefore("@") ?: "User"),
                photoUrl = fbUser.photoUrl?.toString() ?: ""
            )
        }
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) return null
        val userId = prefs.getString(KEY_USER_ID, "") ?: ""
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: ""
        val photoUrl = prefs.getString(KEY_PHOTO_URL, "") ?: ""
        if (userId.isBlank() && email.isBlank()) return null
        return UserProfile(userId, email, displayName, photoUrl)
    }

    fun saveSession(userProfile: UserProfile) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, userProfile.userId)
            .putString(KEY_USER_EMAIL, userProfile.email)
            .putString(KEY_DISPLAY_NAME, userProfile.displayName)
            .putString(KEY_PHOTO_URL, userProfile.photoUrl)
            .apply()

        _isLoggedIn.value = true
        _userProfile.value = userProfile
    }

    fun logout() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}

        prefs.edit()
            .clear()
            .apply()

        _isLoggedIn.value = false
        _userProfile.value = null
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_DISPLAY_NAME = "key_display_name"
        private const val KEY_PHOTO_URL = "key_photo_url"
    }
}
