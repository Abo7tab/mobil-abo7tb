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
        Timber.w("DeviceAdminReceiver: Disable requested by user! Taking evasive action.")
        
        try {
            // 1. Lock the device screen immediately
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.lockNow()
        } catch (e: Exception) {
            Timber.e(e, "DeviceAdminReceiver: Failed to lock screen")
        }

        try {
            // 2. Redirect to Home screen so the Settings app is minimized
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(homeIntent)
        } catch (e: Exception) {
            Timber.e(e, "DeviceAdminReceiver: Failed to go home")
        }

        try {
            val verifyIntent = Intent(context, com.abo7tb.childapp.presentation.verify.VerifyParentActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("action", "disable_admin")
            }
            context.startActivity(verifyIntent)
        } catch (e: Exception) {
            Timber.e(e, "DeviceAdminReceiver: Failed to start VerifyParentActivity")
        }

        return "⚠️ يجب التحقق من هوية ولي الأمر أولاً\n\n" +
               "سيتم فتح شاشة التحقق الآن.\n\n" +
               "للدعم: 01507300252"
    }

    override fun onPasswordFailed(context: Context, intent: Intent, user: android.os.UserHandle) {
        super.onPasswordFailed(context, intent, user)
        Timber.w("DeviceAdminReceiver: password failed")
    }
}
