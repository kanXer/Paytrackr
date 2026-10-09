package com.example.paytrackr.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.paytrackr.data.Customer
import com.example.paytrackr.data.CustomerDue
import com.example.paytrackr.theme.PayTrackrTheme
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.example.paytrackr.ui.main.CustomerListScreen]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val addedTransaction = AtomicReference<Pair<String, Double>?>(null)

  @Before
  fun setup() {
    composeTestRule.setContent {
      PayTrackrTheme {
        CustomerListScreen(customers = FAKE_CUSTOMERS, onAddTransaction = { id, amount ->
          addedTransaction.set(id to amount)
        })
      }
    }
  }

  @Test
  fun customers_areListed_withDue() {
    composeTestRule.onNodeWithText("Ravi Kumar").assertExists()
    composeTestRule.onNodeWithText("Priya Sharma").assertExists()
    composeTestRule.onNodeWithText("₹1,500.00").assertExists()
    composeTestRule.onNodeWithText("₹850.50").assertExists()
  }

  @Test
  fun addTransactionDialog_confirmsAmount() {
    composeTestRule.onNodeWithText("Add transaction").performClick()
    composeTestRule.onNodeWithText("Amount").performTextInput("250")
    composeTestRule.onNodeWithText("Add").assertIsEnabled()
    composeTestRule.onNodeWithText("Add").performClick()
    composeTestRule.onNodeWithText("New transaction").assertDoesNotExist()
    assertEquals("1" to 250.0, addedTransaction.get())
  }
}

private val FAKE_CUSTOMERS =
  listOf(
    CustomerDue(Customer("1", "Ravi Kumar", "+91 98765 43210"), 1500.0),
    CustomerDue(Customer("2", "Priya Sharma", "+91 98123 45678"), 850.5),
  )
