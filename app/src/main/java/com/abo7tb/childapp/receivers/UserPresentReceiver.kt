package com.abo7tb.childapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.abo7tb.childapp.data.local.SecurePrefsManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UserPresentReceiver : BroadcastReceiver() {

    @Inject
    lateinit var securePrefsManager: SecurePrefsManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            securePrefsManager.setDeviceLocked(false)
        }
    }
}
