package com.abo7tb.childapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.service.ChildForegroundService
import com.abo7tb.childapp.utils.StealthManager
import com.abo7tb.childapp.worker.WorkerHelper
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var securePrefsManager: SecurePrefsManager

    @Inject
    lateinit var stealthManager: StealthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )

        if (securePrefsManager.getUuid() != null) {
            stealthManager.applyStoredLevel()
            ensureBackgroundRunning()
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startDestination = if (securePrefsManager.getUuid() != null) {
                        "verify_parent"
                    } else {
                        "registration"
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable("registration") {
                            com.abo7tb.childapp.ui.RegistrationScreen(
                                onRegisterSuccess = {
                                    stealthManager.setStealthLevel(StealthManager.StealthLevel.FULLY_HIDDEN)
                                    ensureBackgroundRunning()
                                    WorkerHelper.enqueueAllWorkers(this@MainActivity)
                                    Timber.d("MainActivity: registration complete, hiding app")
                                    finish()
                                }
                            )
                        }
                        composable("verify_parent") {
                            com.abo7tb.childapp.presentation.verify.VerifyParentScreen(
                                onSuccess = {
                                    if (intent?.getBooleanExtra("from_secret_code", false) == true) {
                                        finish()
                                    } else {
                                        finish()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun ensureBackgroundRunning() {
        val serviceIntent = Intent(this, ChildForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        Timber.d("MainActivity: foreground service start requested")
    }
}
