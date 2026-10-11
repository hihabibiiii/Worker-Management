package com.example.workermanager.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log

object ContactUtils {

    fun processContactUri(context: Context, contactUri: Uri): Pair<String?, String?> {
        var fetchedName: String? = null
        var fetchedNumber: String? = null

        try {
            // Attempt 1: Query as CommonDataKinds.Phone URI
            val phoneCursor = context.contentResolver.query(
                contactUri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )

            phoneCursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    if (nameIdx >= 0) fetchedName = cursor.getString(nameIdx)
                    if (numIdx >= 0) fetchedNumber = cursor.getString(numIdx)
                }
            }

            // Attempt 2: If phone number was not retrieved, query Contacts table & Phone table
            if (fetchedNumber.isNullOrBlank()) {
                val contactCursor = context.contentResolver.query(
                    contactUri,
                    arrayOf(
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.Contacts.HAS_PHONE_NUMBER
                    ),
                    null,
                    null,
                    null
                )

                contactCursor?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val hasPhoneIdx = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                        if (fetchedName.isNullOrBlank() && nameIdx >= 0) {
                            fetchedName = cursor.getString(nameIdx)
                        }

                        val contactId = if (idIdx >= 0) cursor.getString(idIdx) else null
                        val hasPhone = if (hasPhoneIdx >= 0) cursor.getInt(hasPhoneIdx) else 0

                        if (hasPhone > 0 && contactId != null) {
                            val pCursor = context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )
                            pCursor?.use { pc ->
                                if (pc.moveToFirst()) {
                                    val numIdx = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (numIdx >= 0) {
                                        fetchedNumber = pc.getString(numIdx)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WorkCraftContact", "Error reading contact details for URI: $contactUri", e)
        }

        return Pair(fetchedName, fetchedNumber)
    }

    fun normalizePhoneNumber(rawNumber: String): String {
        val trimmed = rawNumber.trim()
        if (trimmed.isEmpty()) return ""

        val hasPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }
        
        return if (hasPlus) "+$digitsOnly" else digitsOnly
    }
}
