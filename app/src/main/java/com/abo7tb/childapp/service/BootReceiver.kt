package com.abo7tb.childapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.abo7tb.childapp.di.AppEntryPoint
import com.abo7tb.childapp.utils.SecretCodeRegistrar
import com.abo7tb.childapp.worker.WorkerHelper
import dagger.hilt.android.EntryPointAccessors
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val appContext = context.applicationContext
        val entry = EntryPointAccessors.fromApplication(appContext, AppEntryPoint::class.java)
        val securePrefsManager = entry.securePrefsManager()
        val protectionManager = entry.protectionManager()

        val uuid = securePrefsManager.getUuid()
        if (uuid == null) {
            Timber.d("BootReceiver: device not registered, skipping")
            return
        }

        Timber.d("BootReceiver: restoring protection for $uuid")
        protectionManager.applyFullProtection()
        SecretCodeRegistrar.register(context)

        ContextCompat.startForegroundService(
            context,
            Intent(context, ChildForegroundService::class.java)
        )
        WorkerHelper.enqueueAllWorkers(context)

        if (securePrefsManager.isDeviceLocked()) {
            Timber.d("BootReceiver: device locked, restarting ScreenLockService")
            context.startService(
                Intent(context, ScreenLockService::class.java).apply {
                    action = ScreenLockService.ACTION_LOCK_SCREEN
                    putExtra(ScreenLockService.EXTRA_MESSAGE, "تم قفل الجهاز من قبل الوالدين")
                }
            )
        }
    }
}
