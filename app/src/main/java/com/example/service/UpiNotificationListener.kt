package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.ProductivityApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpiNotificationListener : NotificationListenerService() {
    private val watchedPackages = setOf(
        "indwin.c3.shareapp",          // Slice
        "com.google.android.apps.nbu.paisa.user",  // GPay
        "net.one97.paytm",             // Paytm
        "com.phonepe.app",             // PhonePe
        "in.org.npci.upiapp"           // BHIM
    )

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in watchedPackages) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val fullText = "$title $text"

        val amount = extractAmountFromText(fullText) ?: return
        val merchant = extractMerchantFromText(fullText)
        val upiRef = extractUpiRef(fullText)

        val app = application as ProductivityApplication
        
        serviceScope.launch {
            app.expenseProcessor.processUpiDebit(
                amountPaise = (amount * 100).toLong(),
                merchantName = merchant,
                source = sbn.packageName,
                referenceId = upiRef
            )
        }
    }

    private fun extractAmountFromText(text: String): Double? {
        val regex = Regex("""[₹Rs\.INR]+\s*([0-9,]+\.?[0-9]*)""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
    }

    private fun extractMerchantFromText(text: String): String? {
        val toRegex = Regex("""(?:paid to|to)\s+([A-Za-z0-9\s@.]+)""", RegexOption.IGNORE_CASE)
        return toRegex.find(text)?.groupValues?.get(1)?.trim()?.take(50)
    }

    private fun extractUpiRef(text: String): String? {
        val refRegex = Regex("""(?:UPI Ref|Ref No\.?|Transaction ID)[:\s]+([0-9]+)""", RegexOption.IGNORE_CASE)
        return refRegex.find(text)?.groupValues?.get(1)
    }
}
