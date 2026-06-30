package com.abo7tb.childapp.utils

import android.content.Context
import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.data.repository.DeviceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DialerCodeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePrefsManager: SecurePrefsManager,
    private val deviceRepository: DeviceRepository
) {
    companion object {
        private const val KEY_DIALER_CODE = "dialer_secret_code"
        private const val DEFAULT_CODE = "7269"
    }
    
    fun getCurrentCode(): String {
        return securePrefsManager.getString(KEY_DIALER_CODE, DEFAULT_CODE) ?: DEFAULT_CODE
    }
    
    suspend fun syncCodeFromBackend() {
        try {
            val settings = deviceRepository.getDeviceSettings()
            val newCode = settings["stealth_dialer_code"]?.toString() ?: DEFAULT_CODE
            
            val currentCode = getCurrentCode()
            if (newCode != currentCode) {
                updateLocalCode(newCode)
                Timber.d("DialerCode: Updated from $currentCode to $newCode")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync dialer code")
        }
    }
    
    private fun updateLocalCode(newCode: String) {
        securePrefsManager.putString(KEY_DIALER_CODE, newCode)
        SecretCodeRegistrar.register(context)
    }
    
    fun validateCode(enteredCode: String): Boolean {
        return enteredCode == getCurrentCode()
    }
}
