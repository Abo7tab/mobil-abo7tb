package com.abo7tb.childapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.abo7tb.childapp.service.ChildForegroundService
import com.abo7tb.childapp.utils.StealthManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var securePrefsManager: com.abo7tb.childapp.data.local.SecurePrefsManager

    @javax.inject.Inject
    lateinit var stealthManager: StealthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide from recent apps if stealth mode is enabled
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        
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
                                    // 1. Activate stealth mode
                                    stealthManager.setStealthLevel(StealthManager.StealthLevel.FULLY_HIDDEN)
                                    
                                    // 2. Start Foreground Service
                                    val intent = Intent(this@MainActivity, ChildForegroundService::class.java)
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        startForegroundService(intent)
                                    } else {
                                        startService(intent)
                                    }
                                    
                                    // 3. Start Background Workers
                                    com.abo7tb.childapp.worker.WorkerHelper.enqueueDailySyncWorkers(this@MainActivity)
                                    com.abo7tb.childapp.worker.WorkerHelper.enqueueLocationWorker(this@MainActivity)
                                    
                                    // 4. Close the app
                                    finish()
                                }
                            )
                        }
                        composable("verify_parent") {
                            com.abo7tb.childapp.presentation.verify.VerifyParentScreen(
                                onSuccess = {
                                    finish() // Close or hide the app after verification
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
