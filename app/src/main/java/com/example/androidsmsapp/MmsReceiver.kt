package com.example.androidsmsapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MmsReceiver", "Received MMS intent")
        // Handling MMS requires significantly more logic (downloading PDU, etc.)
        // For now, this is a stub required by Android to become the default SMS app.
    }
}
