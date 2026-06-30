package com.abo7tb.childapp.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScreenLockService : Service() {

    private var windowManager: WindowManager? = null
    private var lockView: View? = null
    private var lockMessage: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "LOCK_SCREEN" -> {
                lockMessage = intent.getStringExtra("message") ?: "تم قفل الجهاز من قبل ولي الأمر"
                showLockScreen()
            }
            "UNLOCK_SCREEN" -> {
                hideLockScreen()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun showLockScreen() {
        if (lockView != null) return // Already showing

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.CENTER
            systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }

        // Create ComposeView programmatically
        lockView = ComposeView(this).apply {
            setViewTreeLifecycleOwner()
            setViewTreeSavedStateRegistryOwner()
            setViewTreeViewModelStoreOwner()
            setContent {
                LockScreenContent(message = lockMessage)
            }
        }

        try {
            windowManager?.addView(lockView, params)
        } catch (e: Exception) {
            android.util.Log.e("ScreenLockService", "Failed to add lock view", e)
        }
    }

    private fun ComposeView.setViewTreeLifecycleOwner() {
        val lifecycleOwner = object : androidx.lifecycle.LifecycleOwner {
            private val lifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this)
            init {
                lifecycleRegistry.currentState = androidx.lifecycle.Lifecycle.State.RESUMED
            }
            override val lifecycle: androidx.lifecycle.Lifecycle get() = lifecycleRegistry
        }
        androidx.lifecycle.setViewTreeLifecycleOwner(this, lifecycleOwner)
    }

    private fun ComposeView.setViewTreeSavedStateRegistryOwner() {
        val savedStateRegistryOwner = object : androidx.savedstate.SavedStateRegistryOwner {
            private val savedStateRegistryController = androidx.savedstate.SavedStateRegistryController.create(this)
            init {
                savedStateRegistryController.performRestore(null)
            }
            override val savedStateRegistry: androidx.savedstate.SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
            override val lifecycle: androidx.lifecycle.Lifecycle get() = object : androidx.lifecycle.LifecycleOwner {
                private val lifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this).apply {
                    currentState = androidx.lifecycle.Lifecycle.State.RESUMED
                }
                override val lifecycle: androidx.lifecycle.Lifecycle get() = lifecycleRegistry
            }.lifecycle
        }
        androidx.savedstate.setViewTreeSavedStateRegistryOwner(this, savedStateRegistryOwner)
    }

    private fun ComposeView.setViewTreeViewModelStoreOwner() {
        val viewModelStoreOwner = object : androidx.lifecycle.ViewModelStoreOwner {
            private val store = androidx.lifecycle.ViewModelStore()
            override val viewModelStore: androidx.lifecycle.ViewModelStore get() = store
        }
        androidx.lifecycle.setViewTreeViewModelStoreOwner(this, viewModelStoreOwner)
    }

    private fun hideLockScreen() {
        lockView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                android.util.Log.e("ScreenLockService", "Failed to remove lock view", e)
            }
        }
        lockView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        hideLockScreen()
    }
}

@Composable
fun LockScreenContent(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "🔒",
                fontSize = 80.sp,
                color = ComposeColor.White
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "الجهاز مقفول",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ComposeColor.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = message,
                fontSize = 18.sp,
                color = ComposeColor.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "يرجى التواصل مع ولي الأمر",
                fontSize = 14.sp,
                color = ComposeColor.Gray
            )
        }
    }
}
