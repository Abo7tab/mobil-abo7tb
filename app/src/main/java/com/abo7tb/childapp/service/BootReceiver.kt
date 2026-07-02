package com.abo7tb.childapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.presentation.lock.LockActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var securePrefsManager: SecurePrefsManager
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BOOT", "🔄 Boot detected: ${intent.action}")
        
        // لو الجهاز كان مقفول قبل الـ Restart، افتح شاشة القفل فوراً
        if (securePrefsManager.isDeviceLocked()) {
            val lockIntent = Intent(context, LockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            context.startActivity(lockIntent)
            Log.d("BOOT", "🔒 Lock screen opened immediately")
        }
    }
}
