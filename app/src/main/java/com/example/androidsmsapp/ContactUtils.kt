package com.example.androidsmsapp

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import java.util.concurrent.ConcurrentHashMap

// Global cache for phone numbers to prevent N+1 query problem
private val contactCache = ConcurrentHashMap<String, String>()

fun getContactName(context: Context, phoneNumber: String): String {
    // Return cached name if available
    if (contactCache.containsKey(phoneNumber)) {
        return contactCache[phoneNumber]!!
    }

    // Check if we actually have permission before querying to prevent crashes
    if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
        return phoneNumber
    }

    val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
    val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
    var contactName = phoneNumber
    
    try {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (nameIndex != -1) {
                    contactName = cursor.getString(nameIndex) ?: phoneNumber
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    contactCache[phoneNumber] = contactName
    return contactName
}

fun getPhoneNumberFromContactUri(context: Context, contactUri: Uri): String? {
    var phoneNumber: String? = null
    try {
        val cursor = context.contentResolver.query(contactUri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val hasPhoneIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                if (hasPhoneIndex != -1 && idIndex != -1) {
                    val hasPhone = it.getString(hasPhoneIndex)
                    if (hasPhone.equals("1", ignoreCase = true)) {
                        val contactId = it.getString(idIndex)
                        val pCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(contactId),
                            null
                        )
                        pCursor?.use { p ->
                            if (p.moveToFirst()) {
                                val numIndex = p.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (numIndex != -1) {
                                    phoneNumber = p.getString(numIndex)
                                }
                            }
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return phoneNumber
}
