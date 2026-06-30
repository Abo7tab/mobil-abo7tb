package com.abo7tb.childapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.utils.StealthManager
import com.abo7tb.childapp.worker.WorkerHelper
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var securePrefsManager: SecurePrefsManager

    @Inject
    lateinit var stealthManager: StealthManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val uuid = securePrefsManager.getUuid()
        if (uuid == null) {
            Timber.d("BootReceiver: device not registered, skipping")
            return
        }

        Timber.d("BootReceiver: restoring stealth and starting services for $uuid")
        stealthManager.applyStoredLevel()

        val serviceIntent = Intent(context, ChildForegroundService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)

        WorkerHelper.enqueueAllWorkers(context)

        if (securePrefsManager.isDeviceLocked()) {
            Timber.d("BootReceiver: device locked, restarting ScreenLockService")
            val lockIntent = Intent(context, ScreenLockService::class.java).apply {
                action = ScreenLockService.ACTION_LOCK_SCREEN
                putExtra(ScreenLockService.EXTRA_MESSAGE, "تم قفل الجهاز من قبل الوالدين")
            }
            context.startService(lockIntent)
        }
    }
}
