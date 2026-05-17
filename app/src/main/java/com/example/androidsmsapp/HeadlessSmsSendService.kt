package com.example.androidsmsapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("HeadlessSmsSendService", "Action: ${intent?.action}")
        // Handle sending messages when responding via quick reply
        return START_NOT_STICKY
    }
}
