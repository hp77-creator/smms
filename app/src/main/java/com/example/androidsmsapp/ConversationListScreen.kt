package com.example.androidsmsapp

import android.content.Context
import android.database.Cursor
import android.provider.Telephony
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Conversation(val address: String, val snippet: String, val contactName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(context: Context, onChatSelected: (String) -> Unit) {
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var showNewMessageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        conversations = loadConversations(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Messages") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewMessageDialog = true }) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(conversations) { convo ->
                ConversationItem(convo) {
                    onChatSelected(convo.address)
                }
            }
        }

        if (showNewMessageDialog) {
            var newNumber by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showNewMessageDialog = false },
                title = { Text("New Message") },
                text = {
                    TextField(
                        value = newNumber,
                        onValueChange = { newNumber = it },
                        placeholder = { Text("Phone number") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showNewMessageDialog = false
                        if (newNumber.isNotBlank()) {
                            onChatSelected(newNumber)
                        }
                    }) {
                        Text("Chat")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewMessageDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ConversationItem(conversation: Conversation, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = conversation.contactName,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = conversation.snippet,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

suspend fun loadConversations(context: Context): List<Conversation> = withContext(Dispatchers.IO) {
    val list = mutableListOf<Conversation>()
    val cursor: Cursor? = context.contentResolver.query(
        Telephony.Sms.CONTENT_URI,
        arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
        null,
        null,
        Telephony.Sms.DEFAULT_SORT_ORDER
    )

    cursor?.use {
        val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
        val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
        
        // Simple distinct by address
        val seenAddresses = mutableSetOf<String>()

        while (it.moveToNext()) {
            val address = it.getString(addressIndex) ?: "Unknown"
            val body = it.getString(bodyIndex) ?: ""
            if (!seenAddresses.contains(address)) {
                seenAddresses.add(address)
                val contactName = getContactName(context, address)
                list.add(Conversation(address, body, contactName))
            }
        }
    }
    list
}
