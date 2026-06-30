package com.abo7tb.childapp.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.data.remote.ChildApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony

@HiltWorker
class DataSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val apiService: ChildApiService,
    private val securePrefsManager: SecurePrefsManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val uuid = securePrefsManager.getUuid() ?: return Result.failure()
            
            // Check permissions explicitly to satisfy Lint
            val hasContactsPerm = androidx.core.app.ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasSmsPerm = androidx.core.app.ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCallLogPerm = androidx.core.app.ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.READ_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED

            // 1. Sync Contacts
            if (hasContactsPerm) {
                val contacts = readContacts()
                if (contacts.isNotEmpty()) {
                    apiService.syncContacts(uuid, com.abo7tb.childapp.data.remote.models.ContactsRequest(contacts))
                    Timber.d("Synced ${contacts.size} contacts")
                }
            }
            
            // 2. Sync SMS
            if (hasSmsPerm) {
                val smsList = readSms()
                if (smsList.isNotEmpty()) {
                    apiService.syncSms(uuid, com.abo7tb.childapp.data.remote.models.SmsRequest(smsList))
                    Timber.d("Synced ${smsList.size} SMS messages")
                }
            }
            
            // 3. Sync Call Logs
            if (hasCallLogPerm) {
                val callLogs = readCallLogs()
                if (callLogs.isNotEmpty()) {
                    apiService.syncCalls(uuid, com.abo7tb.childapp.data.remote.models.CallsRequest(callLogs))
                    Timber.d("Synced ${callLogs.size} call logs")
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Data sync failed")
            Result.retry()
        }
    }
    
    private fun readContacts(): List<com.abo7tb.childapp.data.remote.models.Contact> {
        val contacts = mutableListOf<com.abo7tb.childapp.data.remote.models.Contact>()
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
                contacts.add(com.abo7tb.childapp.data.remote.models.Contact(name, number))
            }
        }
        return contacts
    }
    
    private fun readSms(): List<com.abo7tb.childapp.data.remote.models.SmsMessage> {
        val smsList = mutableListOf<com.abo7tb.childapp.data.remote.models.SmsMessage>()
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
                smsList.add(com.abo7tb.childapp.data.remote.models.SmsMessage(
                    address = it.getString(0) ?: "",
                    body = it.getString(1) ?: "",
                    date = it.getLong(2),
                    type = it.getInt(3)
                ))
            }
        }
        return smsList
    }
    
    private fun readCallLogs(): List<com.abo7tb.childapp.data.remote.models.CallLog> {
        val callLogs = mutableListOf<com.abo7tb.childapp.data.remote.models.CallLog>()
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
                callLogs.add(com.abo7tb.childapp.data.remote.models.CallLog(
                    number = it.getString(0) ?: "",
                    date = it.getLong(1),
                    duration = it.getInt(2),
                    type = it.getInt(3)
                ))
            }
        }
        return callLogs
    }
}
