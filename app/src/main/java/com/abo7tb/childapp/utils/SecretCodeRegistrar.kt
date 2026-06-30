package com.abo7tb.childapp.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.abo7tb.childapp.receivers.SecretCodeReceiver
import timber.log.Timber

object SecretCodeRegistrar {

    private var receiver: SecretCodeReceiver? = null
    private var registeredCode: String? = null

    fun register(context: Context) {
        val appContext = context.applicationContext
        val code = SecretCodeHelper.getStoredCode(appContext)

        if (receiver != null && registeredCode == code) return

        unregister(appContext)

        receiver = SecretCodeReceiver()
        val filter = IntentFilter("android.provider.Telephony.SECRET_CODE").apply {
            addDataScheme("android_secret_code")
            addDataAuthority(code, null)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(
                    receiver,
                    filter,
                    Context.RECEIVER_EXPORTED
                )
            } else {
                @Suppress("DEPRECATION")
                appContext.registerReceiver(receiver, filter)
            }
            registeredCode = code
            Timber.d("SecretCodeRegistrar: dynamically registered code=$code")
        } catch (e: Exception) {
            Timber.e(e, "SecretCodeRegistrar: failed to register")
        }
    }

    fun unregister(context: Context) {
        receiver?.let {
            try {
                context.applicationContext.unregisterReceiver(it)
            } catch (e: Exception) {
                Timber.w(e, "SecretCodeRegistrar: unregister failed")
            }
        }
        receiver = null
        registeredCode = null
    }
}
