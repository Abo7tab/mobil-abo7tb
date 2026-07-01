package com.abo7tb.childapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.abo7tb.childapp.MainActivity
import com.abo7tb.childapp.di.AppEntryPoint
import com.abo7tb.childapp.utils.SecretCodeHelper
import dagger.hilt.android.EntryPointAccessors
import timber.log.Timber

class SecretCodeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SECRET_CODE") return

        val host = intent.data?.host
            ?: intent.data?.toString()?.substringAfter("android_secret_code://")?.substringBefore("/")
            ?: return

        val storedCode = SecretCodeHelper.getStoredCode(context).filter { it.isDigit() }
        val entered = host.filter { it.isDigit() }

        Timber.d("SecretCodeReceiver: entered=$entered stored=$storedCode uri=${intent.data}")

        if (entered != storedCode) return

        try {
            val protection = EntryPointAccessors.fromApplication(
                context.applicationContext,
                AppEntryPoint::class.java
            ).protectionManager()
            protection.revealForParentAccess()
        } catch (e: Exception) {
            Timber.e(e, "SecretCodeReceiver: reveal failed")
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("from_secret_code", true)
        }
        context.startActivity(launchIntent)
        Timber.d("SecretCodeReceiver: MainActivity launched")
    }
}
