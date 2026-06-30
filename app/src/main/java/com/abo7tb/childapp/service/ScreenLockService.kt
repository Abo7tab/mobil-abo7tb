package com.abo7tb.childapp.service

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class ScreenLockService : Service() {

    private var windowManager: WindowManager? = null
    private var lockView: View? = null
    private var lockMessage: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_LOCK_SCREEN -> {
                if (!Settings.canDrawOverlays(this)) {
                    Timber.e("ScreenLockService: overlay permission missing, cannot lock")
                    return START_NOT_STICKY
                }
                lockMessage = intent.getStringExtra(EXTRA_MESSAGE) ?: "تم قفل الجهاز من قبل ولي الأمر"
                showLockScreen()
            }
            ACTION_UNLOCK_SCREEN -> {
                hideLockScreen()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun showLockScreen() {
        if (lockView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

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

        lockView = createLockView(lockMessage)

        try {
            windowManager?.addView(lockView, params)
            Timber.d("ScreenLockService: lock overlay shown")
        } catch (e: Exception) {
            Timber.e(e, "ScreenLockService: failed to add lock view")
        }
    }

    private fun createLockView(message: String): View {
        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            setPadding(64, 64, 64, 64)
            isClickable = true
            isFocusable = true
        }

        val iconText = TextView(this).apply {
            text = "🔒"
            textSize = 80f
            gravity = Gravity.CENTER
        }
        linearLayout.addView(iconText)

        val titleText = TextView(this).apply {
            text = "الجهاز مقفول"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 64, 0, 32)
        }
        linearLayout.addView(titleText)

        val messageText = TextView(this).apply {
            text = message
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 64)
        }
        linearLayout.addView(messageText)

        val footerText = TextView(this).apply {
            text = "يرجى التواصل مع ولي الأمر"
            textSize = 14f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        }
        linearLayout.addView(footerText)

        return linearLayout
    }

    private fun hideLockScreen() {
        lockView?.let {
            try {
                windowManager?.removeView(it)
                Timber.d("ScreenLockService: lock overlay removed")
            } catch (e: Exception) {
                Timber.e(e, "ScreenLockService: failed to remove lock view")
            }
        }
        lockView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        hideLockScreen()
    }

    companion object {
        const val ACTION_LOCK_SCREEN = "LOCK_SCREEN"
        const val ACTION_UNLOCK_SCREEN = "UNLOCK_SCREEN"
        const val EXTRA_MESSAGE = "message"
    }
}
