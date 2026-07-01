package com.abo7tb.childapp.receivers

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import com.abo7tb.childapp.utils.DeviceOwnerHelper
import timber.log.Timber

class DeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Timber.d("DeviceAdminReceiver: enabled")
        if (DeviceOwnerHelper.hasStrongProtection(context)) {
            DeviceOwnerHelper.applyDeviceOwnerProtection(context)
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Timber.w("DeviceAdminReceiver: disabled — parent should be notified")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "لا يمكن إلغاء الحماية إلا بإيميل وكلمة مرور ولي الأمر.\nافتح الهاتف → *#*#7269#*#* → زر الاتصال الأخضر"
    }

    override fun onPasswordFailed(context: Context, intent: Intent, user: android.os.UserHandle) {
        super.onPasswordFailed(context, intent, user)
        Timber.w("DeviceAdminReceiver: password failed")
    }
}
