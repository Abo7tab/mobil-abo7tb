package com.abo7tb.childapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.delay

object NetworkController {
    
    // فحص هل النت شغال
    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    // فحص حالة WiFi
    fun isWifiEnabled(context: Context): Boolean {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifi.isWifiEnabled
    }
    
    // فتح WiFi
    fun enableWifi(context: Context) {
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                wifi.isWifiEnabled = true
            } else {
                // Android 10+ محتاج Settings intent
                val intent = android.content.Intent(android.provider.Settings.Panel.ACTION_WIFI)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
            Log.d("NETWORK", "📶 WiFi enable requested")
        } catch (e: Exception) {
            Log.e("NETWORK", "Failed to enable WiFi: ${e.message}")
        }
    }
    
    // قفل WiFi
    fun disableWifi(context: Context) {
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                wifi.isWifiEnabled = false
            }
            Log.d("NETWORK", "📵 WiFi disabled")
        } catch (e: Exception) {
            Log.e("NETWORK", "Failed to disable WiFi: ${e.message}")
        }
    }
    
    // فتح Mobile Data (يحتاج Device Owner أو Root)
    fun enableMobileData(context: Context) {
        try {
            // Android بيمنع التحكم المباشر في Mobile Data للتطبيقات العادية
            // الحل: نستخدم Settings Panel لطلب من المستخدم
            val intent = android.content.Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            
            Log.d("NETWORK", "📱 Mobile Data settings opened")
        } catch (e: Exception) {
            Log.e("NETWORK", "Failed to open mobile data: ${e.message}")
        }
    }
    
    // انتظر لحد ما النت يشتغل
    suspend fun waitForInternet(context: Context, timeoutMs: Long = 15000): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (isInternetAvailable(context)) {
                return true
            }
            delay(500)
        }
        return false
    }
    
    // احفظ الحالة الحالية للشبكة
    data class NetworkState(
        val wifiEnabled: Boolean,
        val dataEnabled: Boolean
    )
    
    fun saveCurrentState(context: Context): NetworkState {
        return NetworkState(
            wifiEnabled = isWifiEnabled(context),
            dataEnabled = isInternetAvailable(context)
        )
    }
    
    // ارجع للحالة الأصلية
    fun restoreState(context: Context, state: NetworkState) {
        if (!state.wifiEnabled && isWifiEnabled(context)) {
            disableWifi(context)
        }
    }
}
