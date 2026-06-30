package com.abo7tb.childapp.utils

import com.abo7tb.childapp.data.local.SecurePrefsManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordAttemptManager @Inject constructor(
    private val securePrefs: SecurePrefsManager
) {
    companion object {
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts_count"
        private const val KEY_LOCKED_UNTIL = "locked_until_timestamp"
        private const val KEY_LAST_ATTEMPT = "last_attempt_timestamp"
    }
    
    fun isLocked(): LockStatus {
        val lockedUntil = securePrefs.getLong(KEY_LOCKED_UNTIL, 0)
        val now = System.currentTimeMillis()
        
        return if (lockedUntil > now) {
            val remainingMs = lockedUntil - now
            val remainingSeconds = remainingMs / 1000
            LockStatus.Locked(remainingSeconds)
        } else {
            LockStatus.Unlocked
        }
    }
    
    fun recordFailedAttempt(): AttemptResult {
        val attempts = securePrefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        securePrefs.putInt(KEY_FAILED_ATTEMPTS, attempts)
        securePrefs.putLong(KEY_LAST_ATTEMPT, System.currentTimeMillis())
        
        val lockDurationSeconds = when {
            attempts <= 5 -> 0L
            attempts == 6 -> 60L          // 1 minute
            attempts == 11 -> 5 * 60L     // 5 minutes
            attempts == 16 -> 15 * 60L    // 15 minutes
            attempts == 17 -> 30 * 60L    // 30 minutes
            attempts >= 18 -> 60 * 60L    // 60 minutes
            else -> 0L
        }
        
        return if (lockDurationSeconds > 0) {
            val lockUntil = System.currentTimeMillis() + (lockDurationSeconds * 1000)
            securePrefs.putLong(KEY_LOCKED_UNTIL, lockUntil)
            
            AttemptResult.Locked(
                attemptsCount = attempts,
                lockDurationSeconds = lockDurationSeconds
            )
        } else {
            val remainingAttempts = 5 - attempts
            AttemptResult.AttemptsRemaining(
                attemptsCount = attempts,
                remaining = remainingAttempts
            )
        }
    }
    
    fun recordSuccessfulAttempt() {
        securePrefs.remove(KEY_FAILED_ATTEMPTS)
        securePrefs.remove(KEY_LOCKED_UNTIL)
        securePrefs.remove(KEY_LAST_ATTEMPT)
    }
    
    fun getFailedAttemptsCount(): Int {
        return securePrefs.getInt(KEY_FAILED_ATTEMPTS, 0)
    }
}

sealed class LockStatus {
    object Unlocked : LockStatus()
    data class Locked(val remainingSeconds: Long) : LockStatus()
}

sealed class AttemptResult {
    data class AttemptsRemaining(
        val attemptsCount: Int,
        val remaining: Int
    ) : AttemptResult()
    
    data class Locked(
        val attemptsCount: Int,
        val lockDurationSeconds: Long
    ) : AttemptResult()
}
