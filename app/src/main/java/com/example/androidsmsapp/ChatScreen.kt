package com.example.androidsmsapp

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ChatMessage(val body: String, val isMe: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(context: Context, address: String, onBack: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var contactName by remember { mutableStateOf(address) }
    val smsManager = context.getSystemService(SmsManager::class.java)

    BackHandler { onBack() }

    LaunchedEffect(address, messageText) {
        messages = loadMessages(context, address)
        withContext(Dispatchers.IO) {
            contactName = getContactName(context, address)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contactName) },
                navigationIcon = {
                    Button(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                items(messages) { msg ->
                    Text(
                        text = (if (msg.isMe) "Me: " else "$address: ") + msg.body,
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = if (msg.isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            // 1. Send out over network
                            smsManager?.sendTextMessage(address, null, messageText, null, null)
                            
                            // 2. Default SMS apps must save their outgoing messages manually!
                            val values = ContentValues().apply {
                                put(Telephony.Sms.ADDRESS, address)
                                put(Telephony.Sms.BODY, messageText)
                                put(Telephony.Sms.DATE, System.currentTimeMillis())
                                put(Telephony.Sms.READ, 1)
                                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                            }
                            context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
                            
                            messageText = "" // This triggers LaunchedEffect to reload messages
                        }
                    }
                ) {
                    Text("Send")
                }
            }
        }
    }
}

suspend fun loadMessages(context: Context, targetAddress: String): List<ChatMessage> = withContext(Dispatchers.IO) {
    val list = mutableListOf<ChatMessage>()
    val cursor: Cursor? = context.contentResolver.query(
        Telephony.Sms.CONTENT_URI,
        arrayOf(Telephony.Sms.BODY, Telephony.Sms.TYPE),
        "${Telephony.Sms.ADDRESS} = ?",
        arrayOf(targetAddress),
        "${Telephony.Sms.DATE} DESC"
    )

    cursor?.use {
        val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
        val typeIndex = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)
        
        while (it.moveToNext()) {
            val body = it.getString(bodyIndex) ?: ""
            val type = it.getInt(typeIndex)
            val isMe = type == Telephony.Sms.MESSAGE_TYPE_SENT
            list.add(ChatMessage(body, isMe))
        }
    }
    list
}
