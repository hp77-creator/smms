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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

data class Conversation(val threadId: Long, val address: String, val snippet: String, val contactName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(context: Context, onChatSelected: (String) -> Unit) {
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var showNewMessageDialog by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshTrigger) {
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
            items(conversations, key = { it.threadId }) { convo ->
                ConversationItem(
                    conversation = convo,
                    onClick = { onChatSelected(convo.address) },
                    onLongClick = { conversationToDelete = convo }
                )
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

        if (conversationToDelete != null) {
            AlertDialog(
                onDismissRequest = { conversationToDelete = null },
                title = { Text("Delete Conversation") },
                text = { Text("Are you sure you want to delete this entire conversation?") },
                confirmButton = {
                    Button(onClick = {
                        val threadId = conversationToDelete!!.threadId
                        conversationToDelete = null
                        scope.launch {
                            deleteConversation(context, threadId)
                            refreshTrigger++
                        }
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { conversationToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ConversationItem(conversation: Conversation, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(conversation.threadId) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
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

suspend fun deleteConversation(context: Context, threadId: Long) = withContext(Dispatchers.IO) {
    context.contentResolver.delete(
        Telephony.Sms.CONTENT_URI,
        "${Telephony.Sms.THREAD_ID} = ?",
        arrayOf(threadId.toString())
    )
}

suspend fun loadConversations(context: Context): List<Conversation> = withContext(Dispatchers.IO) {
    val list = mutableListOf<Conversation>()
    val cursor: Cursor? = context.contentResolver.query(
        Telephony.Sms.CONTENT_URI,
        arrayOf(Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
        null,
        null,
        Telephony.Sms.DEFAULT_SORT_ORDER
    )

    cursor?.use {
        val threadIdIndex = it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
        val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
        val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
        
        // Group by thread ID instead of address
        val seenThreads = mutableSetOf<Long>()

        while (it.moveToNext()) {
            val threadId = it.getLong(threadIdIndex)
            val address = it.getString(addressIndex) ?: "Unknown"
            val body = it.getString(bodyIndex) ?: ""
            if (!seenThreads.contains(threadId)) {
                seenThreads.add(threadId)
                val contactName = getContactName(context, address)
                list.add(Conversation(threadId, address, body, contactName))
            }
        }
    }
    list
}
