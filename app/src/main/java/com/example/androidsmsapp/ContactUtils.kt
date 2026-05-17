package com.example.androidsmsapp

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

fun getContactName(context: Context, phoneNumber: String): String {
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
    
    return contactName
}
