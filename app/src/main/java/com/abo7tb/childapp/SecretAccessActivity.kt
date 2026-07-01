package com.abo7tb.childapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.abo7tb.childapp.utils.ProtectionManager
import com.abo7tb.childapp.utils.SecretCodeHelper
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * نقطة دخول بديلة للكود السري — Samsung أحياناً يفتح Activity بدل Broadcast.
 */
@AndroidEntryPoint
class SecretAccessActivity : ComponentActivity() {

    @Inject lateinit var protectionManager: ProtectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val host = intent?.data?.host
            ?: intent?.data?.toString()?.substringAfter("android_secret_code://")?.substringBefore("/")
        val stored = SecretCodeHelper.getStoredCode(this).filter { it.isDigit() }
        val entered = host?.filter { it.isDigit() } ?: stored

        Timber.d("SecretAccessActivity: uri=${intent?.data} entered=$entered stored=$stored")

        if (entered != stored && host != null) {
            Timber.w("SecretAccessActivity: code mismatch")
            finish()
            return
        }

        protectionManager.revealForParentAccess()

        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("from_secret_code", true)
            }
        )
        finish()
    }
}
