package com.abo7tb.childapp.presentation.verify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.abo7tb.childapp.presentation.verify.VerifyParentScreen
import com.abo7tb.childapp.utils.DeviceAdminHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VerifyParentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    VerifyParentScreen(
                        onUninstallApp = {
                            startActivity(DeviceAdminHelper.createUninstallIntent(this))
                            finish()
                        }
                    )
                }
            }
        }
    }
}
