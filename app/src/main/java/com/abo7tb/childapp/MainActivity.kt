package com.abo7tb.childapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var securePrefsManager: com.abo7tb.childapp.data.local.SecurePrefsManager
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
                    val navController = androidx.navigation.compose.rememberNavController()
                    val startDestination = if (securePrefsManager.getUuid() != null) {
                        "verify_parent"
                    } else {
                        "registration"
                    }

                    androidx.navigation.compose.NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        androidx.navigation.compose.composable("registration") {
                            com.abo7tb.childapp.ui.RegistrationScreen(
                                onRegisterSuccess = {
                                    navController.navigate("verify_parent") {
                                        popUpTo("registration") { inclusive = true }
                                    }
                                }
                            )
                        }
                        androidx.navigation.compose.composable("verify_parent") {
                            com.abo7tb.childapp.presentation.verify.VerifyParentScreen(
                                onSuccess = {
                                    finish() // Close or hide the app after verification for stealth
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
