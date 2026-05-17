package com.example.androidsmsapp

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                Log.d("SmsReceiver", "Received SMS from: ${message.displayOriginatingAddress}")
                // Store message in the system database
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, message.displayOriginatingAddress)
                    put(Telephony.Sms.BODY, message.messageBody)
                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                    put(Telephony.Sms.READ, 0)
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                }
                context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
            }
        }
    }
}
