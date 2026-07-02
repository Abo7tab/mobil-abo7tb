package com.abo7tb.childapp.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.abo7tb.childapp.presentation.verify.VerifyParentActivity

class ProtectionAccessibilityService : AccessibilityService() {
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("PROTECT", "🛡️ Protection Service connected")
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        
        val packageName = event.packageName?.toString() ?: return
        
        // ركز على تطبيق Settings فقط
        if (!packageName.contains("settings", ignoreCase = true)) return
        
        val rootNode = rootInActiveWindow ?: return
        
        try {
            // كلمات تدل على محاولة حذف التطبيق
            val dangerKeywords = listOf(
                "Uninstall", "إلغاء التثبيت", "إزالة",
                "Force stop", "فرض الإيقاف",
                "Disable", "تعطيل",
                "Clear data", "مسح البيانات",
                "Device admin", "المسؤولون عن الجهاز",
                "Deactivate", "إلغاء التنشيط"
            )
            
            // كلمات تدل على إن تطبيقنا ظاهر
            val ourAppKeywords = listOf(
                "القرآن الكريم",
                "com.abo7tb.childapp"
            )
            
            var dangerFound = false
            var ourAppFound = false
            
            // ابحث عن كلمات خطر
            for (keyword in dangerKeywords) {
                val nodes = rootNode.findAccessibilityNodeInfosByText(keyword)
                if (nodes.isNotEmpty()) {
                    dangerFound = true
                    break
                }
            }
            
            // ابحث عن تطبيقنا
            for (keyword in ourAppKeywords) {
                val nodes = rootNode.findAccessibilityNodeInfosByText(keyword)
                if (nodes.isNotEmpty()) {
                    ourAppFound = true
                    break
                }
            }
            
            // لو الطفل حاول يشيل تطبيقنا
            if (dangerFound && ourAppFound) {
                Log.w("PROTECT", "⚠️ Uninstall attempt detected!")
                
                // ارجع للـ Home فوراً
                performGlobalAction(GLOBAL_ACTION_HOME)
                
                // اعرض Toast
                Toast.makeText(
                    this,
                    "⚠️ يجب التحقق من هوية ولي الأمر أولاً",
                    Toast.LENGTH_LONG
                ).show()
                
                // افتح شاشة Password Verification
                val intent = Intent(this, VerifyParentActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("action", "uninstall_attempt")
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("PROTECT", "Error: ${e.message}")
        }
    }
    
    override fun onInterrupt() {
        Log.d("PROTECT", "Service interrupted")
    }
}
