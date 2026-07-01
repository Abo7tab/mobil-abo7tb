package com.abo7tb.childapp.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.abo7tb.childapp.data.local.SecurePrefsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StealthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePrefsManager: SecurePrefsManager
) {

    enum class StealthLevel(val value: Int) {
        VISIBLE(1),
        HIDDEN_NAME(2),
        FULLY_HIDDEN(3)
    }

    fun setStealthLevel(level: StealthLevel) {
        // No longer toggling aliases because we use the fake game disguise.
        saveCurrentLevel(level)
        Timber.d("StealthManager: applied level=$level (Disguise Mode)")
        notifyLauncherRefresh()
    }

    /** إخفاء كامل: لا أيقونة ولا اسم في الـ launcher + إزالة الاختصارات */
    fun hideCompletely() {
        removeLauncherShortcuts()
        setStealthLevel(StealthLevel.FULLY_HIDDEN)

        Handler(Looper.getMainLooper()).postDelayed({
            setStealthLevel(StealthLevel.FULLY_HIDDEN)
            removeLauncherShortcuts()
            notifyLauncherRefresh()
            requestSamsungLauncherRefresh()
            Timber.d("StealthManager: hideCompletely second pass done")
        }, 700)
    }

    /** أثناء التثبيت الأول فقط — إظهار الأيقونة */
    fun showForSetup() {
        setStealthLevel(StealthLevel.VISIBLE)
    }

    private fun enableComponent(pm: PackageManager, component: ComponentName) {
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun disableComponent(pm: PackageManager, component: ComponentName) {
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun removeLauncherShortcuts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
            val manifestIds = shortcutManager.manifestShortcuts.map { it.id }
            val dynamicIds = shortcutManager.dynamicShortcuts.map { it.id }
            val pinnedIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                shortcutManager.pinnedShortcuts.map { it.id }
            } else {
                emptyList()
            }
            val allIds = (manifestIds + dynamicIds + pinnedIds).distinct()
            if (allIds.isNotEmpty()) {
                shortcutManager.disableShortcuts(allIds)
            }
            shortcutManager.removeAllDynamicShortcuts()
            Timber.d("StealthManager: disabled ${allIds.size} shortcuts")
        } catch (e: Exception) {
            Timber.w(e, "StealthManager: shortcut removal failed")
        }
    }

    private fun saveCurrentLevel(level: StealthLevel) {
        securePrefsManager.putInt("stealth_level", level.value)
    }

    fun getCurrentLevel(): StealthLevel {
        val levelValue = securePrefsManager.getInt("stealth_level", StealthLevel.VISIBLE.value)
        return StealthLevel.entries.find { it.value == levelValue } ?: StealthLevel.VISIBLE
    }

    fun applyStoredLevel() {
        if (securePrefsManager.getUuid() != null) {
            hideCompletely()
        } else {
            setStealthLevel(getCurrentLevel())
        }
    }

    fun ensureHiddenForRegisteredDevice() {
        if (securePrefsManager.getUuid() == null) return
        hideCompletely()
    }

    private fun notifyLauncherRefresh() {
        try {
            context.sendBroadcast(
                Intent(Intent.ACTION_PACKAGE_CHANGED).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    putExtra(Intent.EXTRA_UID, android.os.Process.myUid())
                }
            )
            context.sendBroadcast(
                Intent(Intent.ACTION_PACKAGE_REPLACED).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    putExtra(Intent.EXTRA_UID, android.os.Process.myUid())
                }
            )
        } catch (e: Exception) {
            Timber.w(e, "StealthManager: launcher refresh broadcast failed")
        }
    }

    private fun requestSamsungLauncherRefresh() {
        try {
            context.sendBroadcast(Intent("com.sec.android.app.launcher.ALLAPPS_GRID_VIEW_UPDATE"))
        } catch (_: Exception) {
            // Samsung only
        }
    }

    fun goHomeAndHide(delayMs: Long = 300L) {
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)
            } catch (e: Exception) {
                Timber.w(e, "StealthManager: goHome failed")
            }
        }, delayMs)
    }
}
