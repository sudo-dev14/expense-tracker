package com.expensetracker.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.expensetracker.app.ExpenseApp
import com.expensetracker.core.model.SmsMessage
import kotlinx.coroutines.launch

/** Picks up new payment messages the moment they arrive, entirely on the device. */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val container = (context.applicationContext as ExpenseApp).container
        if (!container.prefs.autoTracking.value) return

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        // Long messages arrive in several parts; stitch them back together per sender.
        val messages = parts.filterNotNull()
            .groupBy { it.displayOriginatingAddress ?: "" }
            .map { (sender, list) ->
                SmsMessage(
                    id = null,
                    sender = sender,
                    body = list.joinToString("") { it.displayMessageBody ?: "" },
                    timestamp = list.first().timestampMillis,
                )
            }

        val pending = goAsync()
        container.appScope.launch {
            try {
                messages.forEach { container.repository.ingest(it) }
            } finally {
                pending.finish()
            }
        }
    }
}
