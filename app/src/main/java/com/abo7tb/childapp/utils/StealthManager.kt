package com.abo7tb.childapp.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.abo7tb.childapp.data.local.SecurePrefsManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StealthManager @Inject constructor(
    private val context: Context,
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
        // Implement save level logically inside SecurePrefsManager
    }
    
    fun getCurrentLevel(): StealthLevel {
        return StealthLevel.VISIBLE // Read from Prefs in actual impl
    }
}
