package com.abo7tb.childapp.presentation.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.data.repository.DeviceRepository
import com.abo7tb.childapp.utils.AttemptResult
import com.abo7tb.childapp.utils.IntruderCaptureManager
import com.abo7tb.childapp.utils.LockStatus
import com.abo7tb.childapp.utils.PasswordAttemptManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerifyParentViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val securePrefsManager: SecurePrefsManager,
    private val passwordAttemptManager: PasswordAttemptManager,
    private val intruderCaptureManager: IntruderCaptureManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(VerifyParentState())
    val state: StateFlow<VerifyParentState> = _state.asStateFlow()
    
    init {
        checkLockStatus()
    }
    
    private fun checkLockStatus() {
        val lockStatus = passwordAttemptManager.isLocked()
        val attempts = passwordAttemptManager.getFailedAttemptsCount()
        
        _state.update {
            it.copy(
                lockStatus = lockStatus,
                failedAttempts = attempts
            )
        }
    }
    
    fun verifyParent(email: String, password: String) {
        val lockStatus = passwordAttemptManager.isLocked()
        if (lockStatus is LockStatus.Locked) {
            _state.update {
                it.copy(
                    lockStatus = lockStatus,
                    errorMessage = "الشاشة مقفلة. حاول لاحقاً."
                )
            }
            return
        }
        
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
            try {
                android.util.Log.d("VERIFY", "🔐 Sending verification request...")
                val result = deviceRepository.verifyParent(email, password)
                android.util.Log.d("VERIFY", "Result: ${result.isSuccess}")
                val body = result.getOrNull()

                if (body != null && body.verified) {
                    body.verificationToken?.let { token ->
                        securePrefsManager.saveVerificationToken(token)
                        android.util.Log.d("VERIFY", "✅ Saved verification token")
                    }
                    passwordAttemptManager.recordSuccessfulAttempt()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isVerified = true,
                            errorMessage = null
                        )
                    }
                    android.util.Log.d("VERIFY", "✅ Password correct, executing action")
                } else {
                    android.util.Log.w("VERIFY", "❌ Wrong password or verification error")
                    val message = body?.message
                    val attemptsRemaining = body?.attemptsRemaining
                    val retryAfter = body?.retryAfterSec

                    if (!message.isNullOrBlank() || attemptsRemaining != null || retryAfter != null) {
                        val details = buildString {
                            if (!message.isNullOrBlank()) append(message)
                            if (attemptsRemaining != null) {
                                if (isNotEmpty()) append(" ")
                                append("متبقي $attemptsRemaining محاولات.")
                            }
                            if (retryAfter != null) {
                                if (isNotEmpty()) append(" ")
                                append("حاول مرة أخرى بعد $retryAfter ثانية.")
                            }
                        }
                        _state.update { it.copy(isLoading = false, errorMessage = details) }
                    } else {
                        _state.update { it.copy(isLoading = false, errorMessage = "خطأ في التحقق. حاول مرة أخرى.") }
                    }
                    handleFailedAttempt(email)
                }
            } catch (e: Exception) {
                android.util.Log.e("VERIFY", "❌ Error: ${e.message}", e)
                _state.update { it.copy(isLoading = false, errorMessage = "خطأ: ${e.message ?: "حدث خطأ"}") }
            }
        }
    }
    
    private fun handleFailedAttempt(email: String) {
        val attemptResult = passwordAttemptManager.recordFailedAttempt()
        
        when (attemptResult) {
            is AttemptResult.AttemptsRemaining -> {
                _state.update {
                    it.copy(
                        isLoading = false,
                        failedAttempts = attemptResult.attemptsCount,
                        errorMessage = "خطأ! متبقي ${attemptResult.remaining} محاولات"
                    )
                }
            }
            is AttemptResult.Locked -> {
                _state.update {
                    it.copy(
                        isLoading = false,
                        failedAttempts = attemptResult.attemptsCount,
                        lockStatus = LockStatus.Locked(attemptResult.lockDurationSeconds),
                        errorMessage = null
                    )
                }
            }
        }
        
        if (passwordAttemptManager.getFailedAttemptsCount() >= 3) {
            triggerIntruderCapture(email)
        }
    }
    
    fun triggerIntruderCapture(email: String? = null) {
        intruderCaptureManager.captureIntruder(
            attemptNumber = passwordAttemptManager.getFailedAttemptsCount(),
            parentEmail = email
        )
    }
}

data class VerifyParentState(
    val isLoading: Boolean = false,
    val isVerified: Boolean = false,
    val verificationToken: String? = null,
    val failedAttempts: Int = 0,
    val lockStatus: LockStatus = LockStatus.Unlocked,
    val errorMessage: String? = null
)
