package com.abo7tb.childapp

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
                                    navController.navigate("verify_parent") {
                                        popUpTo("registration") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("verify_parent") {
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
