package com.abo7tb.childapp.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
        val packageManager = context.packageManager
        val packageName = context.packageName

        val visibleAlias = ComponentName(packageName, "$packageName.LauncherAliasVisible")
        val hiddenAlias = ComponentName(packageName, "$packageName.LauncherAliasHidden")

        when (level) {
            StealthLevel.VISIBLE -> {
                enableComponent(packageManager, visibleAlias)
                disableComponent(packageManager, hiddenAlias)
            }
            StealthLevel.HIDDEN_NAME -> {
                disableComponent(packageManager, visibleAlias)
                enableComponent(packageManager, hiddenAlias)
            }
            StealthLevel.FULLY_HIDDEN -> {
                disableComponent(packageManager, visibleAlias)
                disableComponent(packageManager, hiddenAlias)
            }
        }

        saveCurrentLevel(level)
        notifyLauncherRefresh()
        Timber.d("StealthManager: applied level=$level")
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

    private fun saveCurrentLevel(level: StealthLevel) {
        securePrefsManager.putInt("stealth_level", level.value)
    }

    fun getCurrentLevel(): StealthLevel {
        val levelValue = securePrefsManager.getInt("stealth_level", StealthLevel.VISIBLE.value)
        return StealthLevel.entries.find { it.value == levelValue } ?: StealthLevel.VISIBLE
    }

    fun applyStoredLevel() {
        setStealthLevel(getCurrentLevel())
    }

    /** Registered devices should always be hidden — fixes devices that registered before stealth was saved. */
    fun ensureHiddenForRegisteredDevice() {
        if (securePrefsManager.getUuid() == null) return
        val level = getCurrentLevel()
        val target = if (level == StealthLevel.VISIBLE) StealthLevel.FULLY_HIDDEN else level
        setStealthLevel(target)
    }

    private fun notifyLauncherRefresh() {
        try {
            context.sendBroadcast(
                Intent(Intent.ACTION_PACKAGE_CHANGED).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    putExtra(Intent.EXTRA_UID, android.os.Process.myUid())
                }
            )
        } catch (e: Exception) {
            Timber.w(e, "StealthManager: launcher refresh broadcast failed")
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
