package com.abo7tb.childapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.data.repository.DeviceRepository
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
        com.abo7tb.childapp.worker.WorkerHelper.enqueueImmediateCommandPoll(applicationContext)
    }
    
    @Inject
    lateinit var intruderCaptureManager: com.abo7tb.childapp.utils.IntruderCaptureManager

    private fun handleParentAction(data: Map<String, String>) {
        val actionType = data["action"] ?: return
        
        when (actionType) {
            "lock_device" -> {
                securePrefsManager.setDeviceLocked(true)
                val intent = android.content.Intent(this, ScreenLockService::class.java).apply {
                    this.action = ScreenLockService.ACTION_LOCK_SCREEN
                    putExtra(ScreenLockService.EXTRA_MESSAGE, data["message"] ?: "تم قفل الجهاز")
                }
                startService(intent)
                Timber.d("FCM action: Device locked via ScreenLockService")
            }
            "unlock_device" -> {
                securePrefsManager.setDeviceLocked(false)
                val intent = android.content.Intent(this, ScreenLockService::class.java).apply {
                    this.action = ScreenLockService.ACTION_UNLOCK_SCREEN
                }
                startService(intent)
                Timber.d("FCM action: Device unlocked via ScreenLockService")
            }
            "show_message" -> {
                val message = data["message"] ?: return
                showNotification("رسالة من ولي الأمر", message)
                Timber.d("FCM action: Message shown")
            }
            "take_photo" -> {
                // Trigger camera capture
                triggerIntruderCapture()
                Timber.d("FCM action: Photo capture triggered")
            }
            "ring_device" -> {
                playRingTone()
                Timber.d("FCM action: Ringing device")
            }
            else -> {
                Timber.w("Unknown parent action: $actionType")
            }
        }
    }
    
    private fun handleAlert(data: Map<String, String>) {
        val title = data["title"] ?: "تنبيه"
        val message = data["message"] ?: return
        
        showNotification(title, message)
    }

    private fun showNotification(title: String, message: String) {
        val notification = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .build()
        
        androidx.core.app.NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun triggerIntruderCapture() {
        // IntruderCaptureManager is designed for failed attempts, but we can reuse it
        // Or call captureIntruder directly with attempt = 3 to force it
        intruderCaptureManager.captureIntruder(3, "Remote Request")
    }

    private fun playRingTone() {
        try {
            val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(applicationContext, uri)
            ringtone.play()
            
            // Stop after 5 seconds for safety
            serviceScope.launch {
                kotlinx.coroutines.delay(5000)
                if (ringtone.isPlaying) {
                    ringtone.stop()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to play ringtone")
        }
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
