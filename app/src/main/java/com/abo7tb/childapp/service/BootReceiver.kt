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

        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ChildForegroundService::class.java)
            )
        } catch (e: Exception) {
            Timber.e(e, "BootReceiver: Failed to start ChildForegroundService, missing battery exemption?")
        }
        WorkerHelper.enqueueAllWorkers(context)

        if (securePrefsManager.isDeviceLocked()) {
            try {
                val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                val adminComponent = android.content.ComponentName(context, com.abo7tb.childapp.receivers.DeviceAdminReceiver::class.java)
                
                if (devicePolicyManager.isAdminActive(adminComponent)) {
                    devicePolicyManager.lockNow()
                    com.abo7tb.childapp.utils.LockScreenManager.showLockNotification(context, "تم قفل الجهاز من قبل الوالدين")
                    Timber.d("BootReceiver: device locked via DPM")
                }
            } catch (e: Exception) {
                Timber.e(e, "BootReceiver: failed to lock device")
            }
        }
    }
}
