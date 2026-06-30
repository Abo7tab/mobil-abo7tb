package com.abo7tb.childapp.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.abo7tb.childapp.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePrefsManager @Inject constructor(@ApplicationContext context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        Constants.PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        sharedPreferences.edit().putString(Constants.TOKEN_KEY, token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(Constants.TOKEN_KEY, null)
    }

    fun saveUuid(uuid: String) {
        sharedPreferences.edit().putString(Constants.UUID_KEY, uuid).apply()
    }

    fun getUuid(): String? {
        return sharedPreferences.getString(Constants.UUID_KEY, null)
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    fun saveFcmToken(token: String) {
        sharedPreferences.edit().putString(Constants.FCM_TOKEN_KEY, token).apply()
    }

    fun getFcmToken(): String? {
        return sharedPreferences.getString(Constants.FCM_TOKEN_KEY, null)
    }

    fun getInt(key: String, defValue: Int): Int = sharedPreferences.getInt(key, defValue)
    fun putInt(key: String, value: Int) = sharedPreferences.edit().putInt(key, value).apply()
    fun getLong(key: String, defValue: Long): Long = sharedPreferences.getLong(key, defValue)
    fun putLong(key: String, value: Long) = sharedPreferences.edit().putLong(key, value).apply()
    fun getString(key: String, defValue: String): String? = sharedPreferences.getString(key, defValue)
    fun putString(key: String, value: String) = sharedPreferences.edit().putString(key, value).apply()
    fun remove(key: String) = sharedPreferences.edit().remove(key).apply()
}
