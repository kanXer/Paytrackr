package com.example.paytrackr.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.paytrackr.data.CustomerDue
import com.example.paytrackr.data.TransactionType
import java.util.Locale

fun formatAmount(amount: Double): String = "₹" + String.format(Locale.ENGLISH, "%,.2f", amount)

data class ReminderItemBreakdown(
    val description: String,
    val amount: Double,
)

/**
 * Calculates net unpaid items since the last settlement.
 * Older statement history before the latest settlement is excluded.
 * If older pending due remains, it is grouped cleanly as "Other / Purana Baki" without dumping the entire statement.
 * Jama (payment) values are not displayed in the reminder.
 */
fun calculateReminderBreakdown(customerDue: CustomerDue): List<ReminderItemBreakdown> {
    val totalDue = customerDue.due
    if (totalDue <= 0.0) return emptyList()

    val allTxns = customerDue.transactions.sortedBy { it.timestamp }

    // 1. Find the last point where the balance was zero or settled
    var runningBalance = 0.0
    var lastZeroIndex = -1
    allTxns.forEachIndexed { index, txn ->
        if (txn.type == TransactionType.DUE) {
            runningBalance += txn.amount
        } else {
            runningBalance -= txn.amount
        }
        if (runningBalance <= 0.05) {
            lastZeroIndex = index
        }
    }

    val lastSettleTxnIndex = allTxns.indexOfLast {
        it.type == TransactionType.PAYMENT && it.description.contains("settle", ignoreCase = true)
    }
    val cutoffIndex = maxOf(lastZeroIndex, lastSettleTxnIndex)

    val currentCycleTxns = if (cutoffIndex != -1 && cutoffIndex < allTxns.size - 1) {
        allTxns.subList(cutoffIndex + 1, allTxns.size)
    } else if (cutoffIndex != -1 && cutoffIndex == allTxns.size - 1) {
        emptyList()
    } else {
        allTxns
    }

    val cycleDues = currentCycleTxns.filter { it.type == TransactionType.DUE }

    if (cycleDues.isEmpty()) {
        return listOf(ReminderItemBreakdown("Other / Purana Baki", totalDue))
    }

    // Work backwards from newest dues to account for up to totalDue
    val reversedDues = cycleDues.reversed()
    var remainingToAccount = totalDue
    val recentItems = mutableListOf<ReminderItemBreakdown>()

    for (dueTxn in reversedDues) {
        if (recentItems.size >= 5) break
        if (remainingToAccount <= 0.01) break

        val desc = dueTxn.description.trim().ifBlank { "Item" }
        if (dueTxn.amount <= remainingToAccount) {
            recentItems.add(ReminderItemBreakdown(desc, dueTxn.amount))
            remainingToAccount -= dueTxn.amount
        } else {
            recentItems.add(ReminderItemBreakdown(desc, remainingToAccount))
            remainingToAccount = 0.0
            break
        }
    }

    val items = mutableListOf<ReminderItemBreakdown>()
    // If older balance still remains (older dues before these 5 or prior cycle debt), show single line
    if (remainingToAccount > 0.01) {
        items.add(ReminderItemBreakdown("Other / Purana Baki", remainingToAccount))
    }
    items.addAll(recentItems.reversed())

    return items
}

fun reminderMessage(
    customerDue: CustomerDue,
    shopName: String = "",
    upiId: String = "",
): String {
    val name = customerDue.customer.name
    val due = customerDue.due
    val store = if (shopName.isNotBlank()) shopName.trim() else "PayTrackr"
    val sb = StringBuilder()
    sb.append("Namaste $name ji,")

    if (due <= 0.0) {
        sb.append("\n\nAapka *$store* par hisab nil / settled hai. Dhanyawad! 🙏")
        return sb.toString()
    }

    sb.append("\n\nAapka *$store* par hisab:")

    // Only active items that constitute the current pending due
    val breakdown = calculateReminderBreakdown(customerDue)
    if (breakdown.isNotEmpty()) {
        sb.append("\n\n*Liye gaye saman / Baki entries:*")
        breakdown.forEach { item ->
            sb.append("\n• ${item.description}: ${formatAmount(item.amount)}")
        }
    }

    sb.append("\n\n*Kul Baki Rashi (Total Due): ${formatAmount(due)}*")

    if (upiId.isNotBlank() && due > 0) {
        val cleanUpi = upiId.trim()
        val formattedAmount = String.format(Locale.ENGLISH, "%.2f", due)
        val clickableUpiLink = "https://upi.pe/$cleanUpi/$formattedAmount"

        sb.append("\n\n📲 *Pay via UPI:*")
        sb.append("\n👉 $clickableUpiLink")
        sb.append("\n\nUPI ID: *$cleanUpi*")
        sb.append("\nDhanyawad! 🙏")
    } else {
        sb.append("\n\nKripya shighra bhugtan karein via UPI ya Cash. Dhanyawad! 🙏")
    }
    return sb.toString()
}

fun openWhatsAppReminder(context: Context, phone: String, message: String, isBusiness: Boolean = false) {
    val digits = phone.filter { it.isDigit() }
    val formattedNumber = if (digits.isNotEmpty()) {
        if (digits.length == 10) "91$digits" else digits
    } else {
        ""
    }
    val url = if (formattedNumber.isNotEmpty()) {
        "https://wa.me/$formattedNumber?text=${Uri.encode(message)}"
    } else {
        "https://wa.me/?text=${Uri.encode(message)}"
    }
    val targetPackage = if (isBusiness) "com.whatsapp.w4b" else "com.whatsapp"
    val appName = if (isBusiness) "WhatsApp Business" else "WhatsApp"

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        setPackage(targetPackage)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        if (!isBusiness) {
            // Fallback: try opening without restricting package if regular package wasn't matched
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
                return
            } catch (_: Exception) {}
        }
        Toast.makeText(context, "$appName is not installed on this device", Toast.LENGTH_SHORT).show()
    }
}

fun openWhatsAppBusinessReminder(context: Context, phone: String, message: String) {
    openWhatsAppReminder(context = context, phone = phone, message = message, isBusiness = true)
}

fun openSmsReminder(context: Context, phone: String, message: String) {
    val cleanPhone = phone.filter { it.isDigit() || it == '+' }
    val uri = if (cleanPhone.isNotBlank()) Uri.parse("smsto:$cleanPhone") else Uri.parse("smsto:")
    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
        putExtra("sms_body", message)
        putExtra(Intent.EXTRA_TEXT, message)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                putExtra("sms_body", message)
                putExtra(Intent.EXTRA_TEXT, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(viewIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "No SMS app found on this device", Toast.LENGTH_SHORT).show()
        }
    }
}