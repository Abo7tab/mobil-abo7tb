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
import com.abo7tb.childapp.utils.SecretCodeRegistrar
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
            stealthManager.ensureHiddenForRegisteredDevice()
            ensureBackgroundRunning()
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startDestination = when {
                        securePrefsManager.getUuid() == null -> "registration"
                        !com.abo7tb.childapp.utils.DeviceAdminHelper.isAdminActive(this@MainActivity) -> "enable_admin"
                        else -> "verify_parent"
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
                                    SecretCodeRegistrar.register(this@MainActivity)
                                    Timber.d("MainActivity: registration complete, hiding app")
                                    stealthManager.goHomeAndHide(600)
                                    finish()
                                }
                            )
                        }
                        composable("enable_admin") {
                            com.abo7tb.childapp.ui.EnableAdminView(
                                onDone = {
                                    navController.navigate("verify_parent") {
                                        popUpTo("enable_admin") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("verify_parent") {
                            com.abo7tb.childapp.presentation.verify.VerifyParentScreen(
                                onHideApp = {
                                    stealthManager.ensureHiddenForRegisteredDevice()
                                    stealthManager.goHomeAndHide(300)
                                    finish()
                                },
                                onUninstallApp = {
                                    com.abo7tb.childapp.utils.DeviceAdminHelper.deactivateAdmin(this@MainActivity)
                                    startActivity(
                                        com.abo7tb.childapp.utils.DeviceAdminHelper.createUninstallIntent(this@MainActivity)
                                    )
                                    finish()
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
