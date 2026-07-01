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
import com.abo7tb.childapp.utils.DeviceAdminHelper
import com.abo7tb.childapp.utils.ProtectionManager
import com.abo7tb.childapp.utils.SecretCodeRegistrar
import com.abo7tb.childapp.worker.WorkerHelper
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var securePrefsManager: SecurePrefsManager
    @Inject lateinit var protectionManager: ProtectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )

        val fromSecretCode = intent?.getBooleanExtra("from_secret_code", false) == true

        when {
            securePrefsManager.getUuid() == null -> {
                protectionManager.revealForParentAccess()
            }
            fromSecretCode -> {
                protectionManager.revealForParentAccess()
                ensureBackgroundRunning()
            }
            else -> {
                protectionManager.applyFullProtection()
                ensureBackgroundRunning()
                finish()
                return
            }
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val startDestination = when {
                        securePrefsManager.getUuid() == null -> "registration"
                        !DeviceAdminHelper.isAdminActive(this@MainActivity) -> "enable_admin"
                        else -> "verify_parent"
                    }

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("registration") {
                            com.abo7tb.childapp.ui.RegistrationScreen(
                                onRegisterSuccess = {
                                    protectionManager.applyFullProtection()
                                    ensureBackgroundRunning()
                                    WorkerHelper.enqueueAllWorkers(this@MainActivity)
                                    SecretCodeRegistrar.register(this@MainActivity)
                                    Timber.d("MainActivity: registration complete")
                                    protectionManager.hideAfterParentExit()
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
                                    protectionManager.hideAfterParentExit()
                                    finish()
                                },
                                onUninstallApp = {
                                    protectionManager.prepareForUninstallByParent()
                                    startActivity(DeviceAdminHelper.createUninstallIntent(this@MainActivity))
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
        ContextCompat.startForegroundService(this, Intent(this, ChildForegroundService::class.java))
    }
}
