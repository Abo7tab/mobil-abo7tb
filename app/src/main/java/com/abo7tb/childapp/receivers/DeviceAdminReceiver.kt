package com.abo7tb.childapp.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class DeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Timber.d("Device Admin Enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Timber.d("Device Admin Disabled")
    }
}
