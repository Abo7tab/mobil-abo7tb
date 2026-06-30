package com.abo7tb.childapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.abo7tb.childapp.MainActivity
import com.abo7tb.childapp.utils.DialerCodeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SecretCodeReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var dialerCodeManager: DialerCodeManager
    
    override fun onReceive(context: Context, intent: Intent) {
        val data = intent.data?.toString() ?: return
        val enteredCode = data.substringAfter("android_secret_code://")
        
        if (dialerCodeManager.validateCode(enteredCode)) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(launchIntent)
        }
    }
}
