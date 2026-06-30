package com.abo7tb.childapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abo7tb.childapp.data.repository.DeviceRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DeviceRegistrationViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    fun registerDevice(parentEmail: String, parentPassword: String) {
        viewModelScope.launch {
            try {
                // 1. Placeholder for Device Registration logic
                // val result = deviceRepository.registerDevice(...)
                val isSuccess = true // Simulate success
                
                if (isSuccess) {
                    // 2. Fetch FCM Token
                    FirebaseMessaging.getInstance().token
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val token = task.result
                                Timber.d("FCM Token after registration: $token")
                                
                                // 3. Save and send token
                                viewModelScope.launch {
                                    deviceRepository.updateFcmToken(token)
                                }
                            } else {
                                Timber.w(task.exception, "Failed to get FCM token")
                            }
                        }
                }
            } catch (e: Exception) {
                Timber.e(e, "Device registration failed")
            }
        }
    }
}
