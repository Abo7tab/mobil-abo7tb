package com.abo7tb.childapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.utils.ProtectionManager
import com.abo7tb.childapp.utils.SecretCodeRegistrar
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ChildApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var securePrefsManager: SecurePrefsManager
    @Inject lateinit var protectionManager: ProtectionManager

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        SecretCodeRegistrar.register(this)

        if (securePrefsManager.getUuid() != null) {
            protectionManager.applyFullProtection()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
