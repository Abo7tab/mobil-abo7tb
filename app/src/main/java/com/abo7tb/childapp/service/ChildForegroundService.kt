package com.abo7tb.childapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.data.remote.ChildApiService
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class ChildForegroundService : Service() {

    @Inject lateinit var apiService: ChildApiService
    @Inject lateinit var securePrefsManager: SecurePrefsManager
    
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "child_app_channel")
            .setContentTitle("Family Guard Active")
            .setContentText("Keeping your device safe.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
            
        startForeground(1, notification)
        
        startHeartbeatLoop()
        
        return START_STICKY
    }
    
    private fun startHeartbeatLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val uuid = securePrefsManager.getUuid()
                    if (uuid != null) {
                        val batteryStatus: Intent? = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                        val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                        val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 100
                        val status = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
                        val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL

                        val data = mapOf(
                            "battery_level" to batteryPct,
                            "is_charging" to isCharging,
                            "is_online" to true
                        )
                        apiService.sendHeartbeat(uuid, data)
                        // Also poll commands here since 1 minute interval is required
                        apiService.getPendingCommands(uuid)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Heartbeat/Polling failed")
                }
                delay(60_000) // 1 minute
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "child_app_channel",
                "Family Guard Service",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
