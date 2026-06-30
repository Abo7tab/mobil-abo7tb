package com.abo7tb.childapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import timber.log.Timber

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.abo7tb.childapp.data.local.SecurePrefsManager

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var securePrefsManager: SecurePrefsManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            val uuid = securePrefsManager.getUuid()
            if (uuid != null) {
                Timber.d("Boot received, starting ChildForegroundService")
                val serviceIntent = Intent(context, ChildForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } else {
                Timber.d("Boot received but device not registered, skipping service start")
            }
        }
    }
}
