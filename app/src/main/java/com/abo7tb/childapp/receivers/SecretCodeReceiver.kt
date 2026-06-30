package com.abo7tb.childapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.abo7tb.childapp.MainActivity
import com.abo7tb.childapp.utils.SecretCodeHelper
import timber.log.Timber

class SecretCodeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SECRET_CODE") return

        val host = intent.data?.host ?: return
        val storedCode = SecretCodeHelper.getStoredCode(context)

        Timber.d("SecretCodeReceiver: entered=$host stored=$storedCode")

        if (host == storedCode) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("from_secret_code", true)
            }
            context.startActivity(launchIntent)
            Timber.d("SecretCodeReceiver: launching MainActivity")
        }
    }
}
