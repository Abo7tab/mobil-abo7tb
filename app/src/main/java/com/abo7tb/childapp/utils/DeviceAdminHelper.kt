package com.abo7tb.childapp.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.abo7tb.childapp.receivers.DeviceAdminReceiver

object DeviceAdminHelper {

    fun adminComponent(context: Context): ComponentName =
        ComponentName(context, DeviceAdminReceiver::class.java)

    fun isAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(adminComponent(context))
    }

    fun createActivateAdminIntent(context: Context): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent(context))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "مطلوب لمنع الطفل من حذف التطبيق بدون موافقة ولي الأمر."
            )
        }
    }

    fun deactivateAdmin(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = adminComponent(context)
            if (dpm.isAdminActive(component)) {
                dpm.removeActiveAdmin(component)
                true
            } else {
                true
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "DeviceAdminHelper: deactivate failed")
            false
        }
    }

    fun createUninstallIntent(context: Context): Intent {
        return Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
