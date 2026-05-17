package com.example.androidsmsapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.READ_CONTACTS,
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.SEND_SMS
    )

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testFabOpensNewChatDialog() {
        composeTestRule.waitForIdle()
        // Find the Floating Action Button using its text
        composeTestRule.onNodeWithText("+").performClick()

        // Verify the dialog opens by checking for the title and input field
        composeTestRule.onNodeWithText("New Chat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Phone number").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pick from Contacts").assertIsDisplayed()
    }

    @Test
    fun testDialogCloseOnCancel() {
        composeTestRule.waitForIdle()
        // Open the dialog
        composeTestRule.onNodeWithText("+").performClick()
        
        // Assert it opened
        composeTestRule.onNodeWithText("New Chat").assertIsDisplayed()

        // Click the Cancel button
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Assert the dialog is gone
        composeTestRule.onNodeWithText("New Chat").assertDoesNotExist()
    }
}
