package com.example.workermanager.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreProvider {

    val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    val currentUid: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid?.ifBlank { null }

    fun requireAuthenticatedUid(): String {
        val uid = currentUid
        require(!uid.isNullOrBlank()) {
            "Operation rejected: No authenticated user found. Please sign in to perform Firestore operations."
        }
        return uid
    }

    fun getUserDocumentRef(): DocumentReference {
        val authUid = requireAuthenticatedUid()
        return firestore.collection("users").document(authUid)
    }
}
