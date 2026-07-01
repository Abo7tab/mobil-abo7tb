package com.abo7tb.childapp.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.abo7tb.childapp.receivers.DeviceAdminReceiver
import timber.log.Timber

object DeviceOwnerHelper {

    private fun admin(context: Context): ComponentName =
        ComponentName(context, DeviceAdminReceiver::class.java)

    private fun dpm(context: Context): DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun isDeviceOwner(context: Context): Boolean {
        return try {
            dpm(context).isDeviceOwnerApp(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    fun isProfileOwner(context: Context): Boolean {
        return try {
            dpm(context).isProfileOwnerApp(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    fun hasStrongProtection(context: Context): Boolean =
        isDeviceOwner(context) || isProfileOwner(context)

    fun applyDeviceOwnerProtection(context: Context): Boolean {
        if (!hasStrongProtection(context)) return false
        return try {
            val manager = dpm(context)
            val component = admin(context)
            val pkg = context.packageName
            manager.setUninstallBlocked(component, pkg, true)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                manager.setApplicationHidden(component, pkg, true)
            }
            Timber.d("DeviceOwnerHelper: uninstall blocked + app hidden")
            true
        } catch (e: Exception) {
            Timber.e(e, "DeviceOwnerHelper: apply failed")
            false
        }
    }

    fun revealForParent(context: Context): Boolean {
        if (!hasStrongProtection(context)) return false
        return try {
            val manager = dpm(context)
            val component = admin(context)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                manager.setApplicationHidden(component, context.packageName, false)
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "DeviceOwnerHelper: reveal failed")
            false
        }
    }

    fun prepareUninstall(context: Context): Boolean {
        if (!hasStrongProtection(context)) return false
        return try {
            val manager = dpm(context)
            val component = admin(context)
            val pkg = context.packageName
            manager.setUninstallBlocked(component, pkg, false)
            manager.setApplicationHidden(component, pkg, false)
            true
        } catch (e: Exception) {
            Timber.e(e, "DeviceOwnerHelper: prepareUninstall failed")
            false
        }
    }

    fun getAdbSetupCommand(context: Context): String {
        val component = admin(context).flattenToString()
        return "adb shell dpm set-device-owner $component"
    }
}
