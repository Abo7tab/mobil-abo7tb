package com.abo7tb.childapp.presentation.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                    errorMessage = "الشاشة مقفولة. حاول لاحقاً."
                )
            }
            return
        }
        
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
            try {
                val result = deviceRepository.verifyParent(email, password)
                
                if (result.isSuccess) {
                    passwordAttemptManager.recordSuccessfulAttempt()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isVerified = true,
                            verificationToken = result.getOrNull()?.token
                        )
                    }
                } else {
                    handleFailedAttempt(email)
                }
            } catch (e: Exception) {
                handleFailedAttempt(email)
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
