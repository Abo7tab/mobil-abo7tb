package com.abo7tb.childapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.data.repository.DeviceRepository
import com.abo7tb.childapp.worker.CommandPollerWorker
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ParentalFcmService : FirebaseMessagingService() {
    
    @Inject
    lateinit var deviceRepository: DeviceRepository
    
    @Inject
    lateinit var securePrefsManager: SecurePrefsManager
    
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM: New token received: $token")
        
        // حفظ الـ token محلياً
        securePrefsManager.saveFcmToken(token)
        
        // إرسال الـ token للباك اند
        sendTokenToServer(token)
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Timber.d("FCM: Message received - Data: ${remoteMessage.data}")
        
        val type = remoteMessage.data["type"]
        val commandUuid = remoteMessage.data["command_uuid"]
        val category = remoteMessage.data["category"]
        val priority = remoteMessage.data["priority"]
        
        when (type) {
            "new_command" -> {
                Timber.d("FCM: New command received - UUID: $commandUuid")
                triggerCommandPolling()
            }
            "parent_action" -> {
                Timber.d("FCM: Parent action received")
                handleParentAction(remoteMessage.data)
            }
            "alert" -> {
                Timber.d("FCM: Alert received")
                handleAlert(remoteMessage.data)
            }
            else -> {
                Timber.w("FCM: Unknown message type: $type")
            }
        }
    }
    
    private fun sendTokenToServer(token: String) {
        val deviceUuid = securePrefsManager.getUuid()
        if (deviceUuid.isNullOrEmpty()) {
            Timber.w("FCM: Device not registered yet, token will be sent later")
            return
        }
        
        serviceScope.launch {
            try {
                val result = deviceRepository.updateFcmToken(token)
                if (result.isSuccess) {
                    Timber.d("FCM: Token sent to server successfully")
                } else {
                    Timber.e("FCM: Failed to send token to server")
                }
            } catch (e: Exception) {
                Timber.e(e, "FCM: Error sending token to server")
            }
        }
    }
    
    private fun triggerCommandPolling() {
        val workRequest = OneTimeWorkRequestBuilder<CommandPollerWorker>().build()
        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }
    
    private fun handleParentAction(data: Map<String, String>) {
        // التعامل مع الـ actions المباشرة من الأب
    }
    
    private fun handleAlert(data: Map<String, String>) {
        // عرض تنبيه للطفل
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Family Guard",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Family Guard notifications"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    companion object {
        const val CHANNEL_ID = "family_guard_channel"
    }
}
