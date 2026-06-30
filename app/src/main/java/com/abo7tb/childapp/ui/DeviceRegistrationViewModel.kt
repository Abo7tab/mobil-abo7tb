package com.abo7tb.childapp.ui

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abo7tb.childapp.data.remote.models.RegisterRequest
import com.abo7tb.childapp.data.repository.DeviceRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

data class RegistrationState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DeviceRegistrationViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegistrationState())
    val state: StateFlow<RegistrationState> = _state.asStateFlow()

    fun registerDevice(parentEmail: String, parentPassword: String, childName: String, childAge: Int, deviceId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                val request = RegisterRequest(
                    parentEmail = parentEmail,
                    parentPassword = parentPassword,
                    childName = childName,
                    childAge = childAge,
                    deviceName = Build.MODEL,
                    deviceModel = Build.MODEL,
                    deviceBrand = Build.MANUFACTURER,
                    androidVersion = Build.VERSION.RELEASE,
                    sdkVersion = Build.VERSION.SDK_INT,
                    deviceId = deviceId,
                    appVersion = "1.0.0"
                )
                
                val result = deviceRepository.registerDevice(request)
                
                if (result.isSuccess) {
                    // Fetch FCM Token
                    FirebaseMessaging.getInstance().token
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val token = task.result
                                Timber.d("FCM Token after registration: $token")
                                viewModelScope.launch {
                                    deviceRepository.updateFcmToken(token)
                                }
                            } else {
                                Timber.w(task.exception, "Failed to get FCM token")
                            }
                        }
                    _state.value = _state.value.copy(isLoading = false, isSuccess = true)
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false, 
                        errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Device registration failed")
                _state.value = _state.value.copy(
                    isLoading = false, 
                    errorMessage = e.message
                )
            }
        }
    }
}
