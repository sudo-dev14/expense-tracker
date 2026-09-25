package com.expensetracker.app.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.expensetracker.core.model.SmsMessage

/** Reads the SMS inbox through the system content provider. Nothing is copied except transactions. */
class SmsReader(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    fun countSince(sinceMillis: Long): Int {
        if (!hasPermission()) return 0
        return context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(sinceMillis.toString()),
            null,
        )?.use { it.count } ?: 0
    }

    /** Streams inbox messages newer than [sinceMillis], oldest first. */
    suspend fun readSince(sinceMillis: Long, onMessage: suspend (SmsMessage) -> Unit) {
        if (!hasPermission()) return
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(sinceMillis.toString()),
            "${Telephony.Sms.DATE} ASC",
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressCol = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyCol = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateCol = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (c.moveToNext()) {
                val body = c.getString(bodyCol) ?: continue
                onMessage(
                    SmsMessage(
                        id = c.getLong(idCol),
                        sender = c.getString(addressCol) ?: "",
                        body = body,
                        timestamp = c.getLong(dateCol),
                    )
                )
            }
        }
    }
}
