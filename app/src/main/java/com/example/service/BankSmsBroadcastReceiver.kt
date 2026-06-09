package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.ProductivityApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankSmsBroadcastReceiver : BroadcastReceiver() {

    private val bankSenderIds = setOf(
        "HDFCBK", "ICICIBK", "SBICRD", "AXISBK", "KOTAKB",
        "INDBNK", "CANBNK", "PNBSMS", "BOIIND", "YESBNK",
        "IDBIBK", "CENTBK", "FEDBK"
    )
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages.forEach { sms ->
            val sender = sms.originatingAddress?.uppercase() ?: return@forEach
            // Checking if the sender ID contains any of the known bank IDs 
            // SMS sender IDs often look like "VM-HDFCBK"
            if (bankSenderIds.any { sender.contains(it) }) {
                processBankSms(sms.messageBody, context)
            }
        }
    }

    private fun processBankSms(body: String, context: Context) {
        val debitRegex = Regex(
            """(?:INR|Rs\.?|₹)\s*([\d,]+\.?\d*)\s+(?:debited|deducted|spent)""",
            RegexOption.IGNORE_CASE
        )
        val amount = debitRegex.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: return

        val upiRef = Regex("""UPI[:\s/]+([0-9]{12,})""").find(body)?.groupValues?.get(1)

        val merchant = Regex("""(?:to|at)\s+([A-Za-z0-9\s@.]+?)(?:\s+on|\s+via|\.|$)""",
            RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)?.trim()

        val app = context.applicationContext as ProductivityApplication
        
        scope.launch {
            app.expenseProcessor.processUpiDebit(
                amountPaise = (amount * 100).toLong(),
                merchantName = merchant,
                source = "SMS",
                referenceId = upiRef
            )
        }
    }
}
