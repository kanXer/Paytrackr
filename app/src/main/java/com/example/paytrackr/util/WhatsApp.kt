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
    sb.append("\n\nAapka *$store* par hisab:")

    // Items taken on credit: kya le gya tha (e.g. grossery ₹10, anda ₹50)
    val dueItems = customerDue.transactions.filter { it.type == TransactionType.DUE }
    if (dueItems.isNotEmpty()) {
        sb.append("\n\n*Liye gaye saman / entries:*")
        dueItems.forEach { t ->
            val desc = t.description.trim().ifBlank { "Item" }
            sb.append("\n• $desc: ${formatAmount(t.amount)}")
        }
    }

    val payments = customerDue.transactions.filter { it.type == TransactionType.PAYMENT }
    if (payments.isNotEmpty()) {
        sb.append("\n\n*Jama (Paid):*")
        payments.forEach { p ->
            val desc = p.description.trim().ifBlank { "Payment" }
            sb.append("\n• $desc: -${formatAmount(p.amount)}")
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