package com.example.androidsmsapp

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Role granted
            isDefaultSmsApp = true
        }
    }

    private var isDefaultSmsApp by mutableStateOf(false)

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkDefaultSmsApp()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val contactsPermissionState = rememberPermissionState(android.Manifest.permission.READ_CONTACTS)
                    LaunchedEffect(Unit) {
                        if (!contactsPermissionState.status.isGranted) {
                            contactsPermissionState.launchPermissionRequest()
                        }
                    }

                    if (isDefaultSmsApp) {
                        var currentChatAddress by remember { mutableStateOf<String?>(null) }
                        if (currentChatAddress == null) {
                            ConversationListScreen(context = this@MainActivity, onChatSelected = { address -> 
                                currentChatAddress = address 
                            })
                        } else {
                            ChatScreen(context = this@MainActivity, address = currentChatAddress!!, onBack = { 
                                currentChatAddress = null 
                            })
                        }
                    } else {
                        DefaultAppPrompt()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkDefaultSmsApp()
    }

    private fun checkDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            isDefaultSmsApp = roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
        } else {
            isDefaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this) == packageName
        }
    }

    private fun requestDefaultSmsRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_SMS) == true) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                roleRequestLauncher.launch(intent)
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            startActivity(intent)
        }
    }

    @Composable
    fun DefaultAppPrompt() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to your ad-free SMS app!",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "To send and receive messages, please set this app as your default SMS handler.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { requestDefaultSmsRole() }) {
                Text("Set as Default")
            }
        }
    }
}
