package com.abo7tb.childapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RegistrationScreen(
    onRegisterSuccess: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    
    when (step) {
        1 -> OnboardingView(onNext = { step = 2 })
        2 -> ChildConsentView(onAgree = { step = 3 })
        3 -> PermissionsView(onAllGranted = { step = 4 })
        4 -> AuthView(onSuccess = onRegisterSuccess)
    }
}

@Composable
fun OnboardingView(onNext: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Welcome to Family Guard", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNext) { Text("Next") }
    }
}

@Composable
fun ChildConsentView(onAgree: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Parental Monitoring Consent", style = MaterialTheme.typography.headlineMedium)
        Text("This device will be monitored by your parents for your safety.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAgree) { Text("I Agree") }
    }
}

@Composable
fun PermissionsView(onAllGranted: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Permissions Required", style = MaterialTheme.typography.headlineMedium)
        // TODO: Use Accompanist Permissions to request Location, Contacts, etc.
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAllGranted) { Text("Grant Permissions") }
    }
}

@Composable
fun AuthView(onSuccess: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Parent Authentication", style = MaterialTheme.typography.headlineMedium)
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSuccess) { Text("Register Device") }
    }
}
