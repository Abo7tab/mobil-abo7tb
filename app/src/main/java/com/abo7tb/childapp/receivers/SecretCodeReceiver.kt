package com.abo7tb.childapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.abo7tb.childapp.MainActivity
import com.abo7tb.childapp.utils.SecretCodeHelper
import timber.log.Timber

class SecretCodeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SECRET_CODE") {
            Timber.d("SecretCodeReceiver: ignored action=${intent.action}")
            return
        }

        val uri = intent.data
        val host = uri?.host
            ?: uri?.toString()?.substringAfter("android_secret_code://")?.substringBefore("/")
            ?: return

        val normalizedHost = host.filter { it.isDigit() }
        val storedCode = SecretCodeHelper.getStoredCode(context).filter { it.isDigit() }

        Timber.d("SecretCodeReceiver: uri=$uri host=$normalizedHost stored=$storedCode")

        if (normalizedHost == storedCode) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra("from_secret_code", true)
            }
            context.startActivity(launchIntent)
            Timber.d("SecretCodeReceiver: MainActivity launched")
        }
    }
}
