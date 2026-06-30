package com.abo7tb.childapp.presentation.verify

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abo7tb.childapp.utils.LockStatus
import kotlinx.coroutines.delay

@Composable
fun VerifyParentScreen(
    viewModel: VerifyParentViewModel = hiltViewModel(),
    onSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lock,
            null,
            tint = Color(0xFF6366F1),
            modifier = Modifier.size(80.dp).align(Alignment.CenterHorizontally)
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            "تأكيد هوية ولي الأمر",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(32.dp))
        
        when (val lockStatus = state.lockStatus) {
            is LockStatus.Locked -> {
                LockedView(
                    remainingSeconds = lockStatus.remainingSeconds,
                    attemptsCount = state.failedAttempts
                )
            }
            is LockStatus.Unlocked -> {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                )
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                )
                
                Spacer(Modifier.height(8.dp))
                
                if (state.failedAttempts > 0) {
                    val remainingAttempts = 5 - state.failedAttempts
                    Text(
                        text = if (remainingAttempts > 0) {
                            "⚠️ متبقي $remainingAttempts محاولات"
                        } else {
                            "⚠️ المحاولة التالية ستقفل الشاشة"
                        },
                        color = Color(0xFFD97706),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        viewModel.verifyParent(email, password)
                    },
                    enabled = !state.isLoading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("تأكيد")
                    }
                }
                
                state.errorMessage?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        LaunchedEffect(state.failedAttempts) {
            if (state.failedAttempts >= 3) {
                viewModel.triggerIntruderCapture(email)
            }
        }
        
        LaunchedEffect(state.isVerified) {
            if (state.isVerified) {
                onSuccess()
            }
        }
    }
}

@Composable
fun LockedView(remainingSeconds: Long, attemptsCount: Int) {
    var seconds by remember { mutableStateOf(remainingSeconds) }
    
    LaunchedEffect(remainingSeconds) {
        while (seconds > 0) {
            delay(1000)
            seconds--
        }
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFEE2E2)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Lock,
                null,
                tint = Color.Red,
                modifier = Modifier.size(60.dp)
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                "🔒 الشاشة مقفولة",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "بعد $attemptsCount محاولات فاشلة",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF991B1B)
            )
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = formatTime(seconds),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
            
            Text(
                "ثانية متبقية",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF991B1B)
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                "تم إرسال إشعار لولي الأمر بمحاولات الدخول الفاشلة.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF991B1B),
                textAlign = TextAlign.Center
            )
        }
    }
}

fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
