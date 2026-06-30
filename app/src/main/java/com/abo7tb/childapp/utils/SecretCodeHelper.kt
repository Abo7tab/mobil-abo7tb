package com.abo7tb.childapp.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecretCodeHelper {
    private const val KEY_DIALER_CODE = "dialer_secret_code"
    private const val DEFAULT_CODE = "7269"

    fun getStoredCode(context: Context): String {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                context,
                Constants.PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.getString(KEY_DIALER_CODE, DEFAULT_CODE) ?: DEFAULT_CODE
        } catch (e: Exception) {
            DEFAULT_CODE
        }
    }
}
