package com.abo7tb.childapp.utils

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import timber.log.Timber

object LockScreenManager {

    private var windowManager: WindowManager? = null
    private var lockView: View? = null

    fun showLockScreen(context: Context, message: String) {
        if (lockView != null) return

        if (!Settings.canDrawOverlays(context)) {
            Timber.e("LockScreenManager: overlay permission missing, cannot lock")
            return
        }

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.CENTER
            @Suppress("DEPRECATION")
            systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        }

        lockView = createLockView(context, message)

        try {
            windowManager?.addView(lockView, params)
            Timber.d("LockScreenManager: lock overlay shown")
        } catch (e: Exception) {
            Timber.e(e, "LockScreenManager: failed to add lock view")
        }
    }

    private fun createLockView(context: Context, message: String): View {
        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            setPadding(64, 64, 64, 64)
            isClickable = true
            isFocusable = true
        }

        val iconText = TextView(context).apply {
            text = "🔒"
            textSize = 80f
            gravity = Gravity.CENTER
        }
        linearLayout.addView(iconText)

        val titleText = TextView(context).apply {
            text = "الجهاز مقفول"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 64, 0, 32)
        }
        linearLayout.addView(titleText)

        val messageText = TextView(context).apply {
            text = message
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 64)
        }
        linearLayout.addView(messageText)

        val footerText = TextView(context).apply {
            text = "يرجى التواصل مع ولي الأمر"
            textSize = 14f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        }
        linearLayout.addView(footerText)

        return linearLayout
    }

    fun showLockNotification(context: Context, message: String) {
        val channelId = "parental_lock_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId, "قفل الجهاز", android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setContentTitle("تم قفل الجهاز")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    fun hideLockScreen() {
        lockView?.let {
            try {
                windowManager?.removeView(it)
                Timber.d("LockScreenManager: lock overlay removed")
            } catch (e: Exception) {
                Timber.e(e, "LockScreenManager: failed to remove lock view")
            }
        }
        lockView = null
    }
}
