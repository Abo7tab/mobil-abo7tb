package com.abo7tb.childapp.domain.command

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.provider.Settings
import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.data.remote.ChildApiService
import com.abo7tb.childapp.data.remote.models.CommandStatusRequest
import com.abo7tb.childapp.data.remote.models.RemoteCommand
import com.abo7tb.childapp.service.ScreenLockService
import com.abo7tb.childapp.utils.IntruderCaptureManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ChildApiService,
    private val securePrefsManager: SecurePrefsManager,
    private val intruderCaptureManager: IntruderCaptureManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun pollAndExecuteCommands(): Int {
        val uuid = securePrefsManager.getUuid() ?: return 0
        return try {
            val response = apiService.getPendingCommands(uuid)
            if (!response.isSuccessful) {
                Timber.w("CommandExecutor: pending commands HTTP ${response.code()}")
                return 0
            }
            val commands = response.body()?.data?.commands.orEmpty()
            Timber.d("CommandExecutor: fetched ${commands.size} pending commands")
            var executed = 0
            for (command in commands) {
                if (executeCommand(command)) executed++
            }
            executed
        } catch (e: Exception) {
            Timber.e(e, "CommandExecutor: poll failed")
            0
        }
    }

    suspend fun executeCommand(command: RemoteCommand): Boolean {
        val commandUuid = command.uuid
        return try {
            updateStatus(commandUuid, "executing")
            when (command.commandType.lowercase()) {
                "lock", "lock_device", "lock_screen" -> handleLock(command)
                "unlock", "unlock_device", "unlock_screen" -> handleUnlock()
                "take_photo", "capture_photo", "take_screenshot" -> handleTakePhoto()
                "ring", "ring_device" -> handleRing()
                "show_message" -> handleShowMessage(command)
                "get_location", "update_location", "locate" -> handleGetLocation()
                "sync_data", "sync_now" -> handleSyncData()
                else -> {
                    Timber.w("CommandExecutor: unknown command type ${command.commandType}")
                    updateStatus(commandUuid, "failed", error = "Unknown command type")
                    return false
                }
            }
            updateStatus(commandUuid, "completed")
            true
        } catch (e: Exception) {
            Timber.e(e, "CommandExecutor: failed command $commandUuid")
            updateStatus(commandUuid, "failed", error = e.message ?: "Execution failed")
            false
        }
    }

    private fun handleLock(command: RemoteCommand) {
        if (!Settings.canDrawOverlays(context)) {
            throw IllegalStateException("Overlay permission not granted")
        }
        val data = command.commandData.orEmpty()
        val message = data["message_body"]?.toString()
            ?: data["message"]?.toString()
            ?: "تم قفل الجهاز من قبل ولي الأمر"
        securePrefsManager.setDeviceLocked(true)
        val intent = Intent(context, ScreenLockService::class.java).apply {
            action = ScreenLockService.ACTION_LOCK_SCREEN
            putExtra(ScreenLockService.EXTRA_MESSAGE, message)
        }
        context.startService(intent)
        Timber.d("CommandExecutor: lock screen triggered")
    }

    private fun handleUnlock() {
        securePrefsManager.setDeviceLocked(false)
        val intent = Intent(context, ScreenLockService::class.java).apply {
            action = ScreenLockService.ACTION_UNLOCK_SCREEN
        }
        context.startService(intent)
        Timber.d("CommandExecutor: unlock screen triggered")
    }

    private fun handleTakePhoto() {
        intruderCaptureManager.captureIntruder(3, "remote_command")
        Timber.d("CommandExecutor: photo capture triggered")
    }

    private fun handleRing() {
        scope.launch {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtone.play()
                delay(5000)
                if (ringtone.isPlaying) ringtone.stop()
            } catch (e: Exception) {
                Timber.e(e, "CommandExecutor: ring failed")
            }
        }
        Timber.d("CommandExecutor: ring triggered")
    }

    private fun handleShowMessage(command: RemoteCommand) {
        val message = command.commandData?.get("message")?.toString()
            ?: command.commandData?.get("message_body")?.toString()
            ?: return
        val intent = Intent(context, com.abo7tb.childapp.service.ParentalFcmService::class.java)
        // Notification shown via system - use notification helper inline
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as android.app.NotificationManager
        val notification = androidx.core.app.NotificationCompat.Builder(
            context,
            com.abo7tb.childapp.service.ParentalFcmService.CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("رسالة من ولي الأمر")
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        Timber.d("CommandExecutor: message notification shown")
    }

    private fun handleGetLocation() {
        com.abo7tb.childapp.worker.WorkerHelper.enqueueImmediateLocationSync(context)
        Timber.d("CommandExecutor: immediate location sync triggered")
    }

    private fun handleSyncData() {
        com.abo7tb.childapp.worker.WorkerHelper.enqueueDailySyncWorkers(context) // or maybe a specific immediate sync
        Timber.d("CommandExecutor: data sync triggered")
    }

    private suspend fun updateStatus(
        commandUuid: String,
        status: String,
        result: Map<String, Any?>? = null,
        error: String? = null
    ) {
        try {
            val response = apiService.updateCommandStatus(
                commandUuid,
                CommandStatusRequest(status = status, result = result, error = error)
            )
            Timber.d("CommandExecutor: status $status for $commandUuid -> HTTP ${response.code()}")
        } catch (e: Exception) {
            Timber.e(e, "CommandExecutor: status update failed for $commandUuid")
        }
    }
}
