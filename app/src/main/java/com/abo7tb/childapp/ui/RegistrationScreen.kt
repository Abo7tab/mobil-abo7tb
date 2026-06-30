package com.abo7tb.childapp.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.animation.Crossfade
import androidx.compose.animation.with
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abo7tb.childapp.service.ChildForegroundService
import com.abo7tb.childapp.utils.StealthManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.animation.ExperimentalAnimationApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RegistrationScreen(
    viewModel: DeviceRegistrationViewModel = hiltViewModel(),
    onRegisterSuccess: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    // Trigger success logic when state becomes successful
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            step = 5 // Go to Setup Complete screen
        }
    }

    androidx.compose.animation.AnimatedContent(
        targetState = step,
        transitionSpec = {
            if (targetState > initialState) {
                (androidx.compose.animation.slideInHorizontally(initialOffsetX = { width -> width }) + androidx.compose.animation.fadeIn()).with(
                        androidx.compose.animation.slideOutHorizontally(targetOffsetX = { width -> -width }) + androidx.compose.animation.fadeOut()
                )
            } else {
                (androidx.compose.animation.slideInHorizontally(initialOffsetX = { width -> -width }) + androidx.compose.animation.fadeIn()).with(
                        androidx.compose.animation.slideOutHorizontally(targetOffsetX = { width -> width }) + androidx.compose.animation.fadeOut()
                )
            }
        },
        label = "StepAnimation"
    ) { targetStep ->
        when (targetStep) {
            1 -> WelcomeView(onNext = { step = 2 })
            2 -> PermissionsView(onNext = { step = 3 })
            3 -> ChildConsentView(onAgree = { step = 4 })
            4 -> AuthView(
                state = state,
                onRegister = { email, password, childName, childAge ->
                    val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                    viewModel.registerDevice(email, password, childName, childAge, androidId)
                }
            )
            5 -> SetupCompleteView(onComplete = onRegisterSuccess)
        }
    }
}

@Composable
fun WelcomeView(onNext: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize(), 
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Family Guard", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
        Spacer(modifier = Modifier.height(16.dp))
        Text("تطبيق حماية الأسرة - نسخة الطفل", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("سيتم إعداد هذا الجهاز ليتم متابعته من قبل ولي الأمر لضمان حماية طفلك.", textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("التالي") }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsView(onNext: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    val permissionsState = rememberMultiplePermissionsState(
        permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.CAMERA)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    )

    var hasBackgroundLocation by remember {
        mutableStateOf(
            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var isBatteryExempted by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
                isBatteryExempted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                hasOverlayPermission = Settings.canDrawOverlays(context)
                hasBackgroundLocation = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q ||
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        isBatteryExempted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

    val allGranted = permissionsState.allPermissionsGranted &&
        isBatteryExempted &&
        hasOverlayPermission &&
        hasBackgroundLocation

    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize(), 
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("الصلاحيات المطلوبة", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("يجب منح جميع الصلاحيات التالية لتشغيل البرنامج كمسؤول ولضمان عمله الدائم وعدم توقفه.", textAlign = TextAlign.Center, color = Color.Red)
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { permissionsState.launchMultiplePermissionRequest() }, 
            modifier = Modifier.fillMaxWidth(),
            enabled = !permissionsState.allPermissionsGranted,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (permissionsState.allPermissionsGranted) Color.Green else MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) { 
            Text(if (permissionsState.allPermissionsGranted) "✅ تم منح الصلاحيات الأساسية" else "1. منح الصلاحيات الأساسية") 
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isBatteryExempted,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isBatteryExempted) Color.Green else Color(0xFFEAB308),
                contentColor = Color.White
            )
        ) {
            Text(if (isBatteryExempted) "✅ يعمل في الخلفية دائماً" else "2. السماح بالعمل الدائم في الخلفية")
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !hasOverlayPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasOverlayPermission) Color.Green else Color(0xFFEAB308),
                contentColor = Color.White
            )
        ) {
            Text(if (hasOverlayPermission) "✅ يظهر فوق جميع التطبيقات" else "3. السماح بظهور شاشة القفل")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            Button(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !hasBackgroundLocation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasBackgroundLocation) Color.Green else Color(0xFFEAB308),
                    contentColor = Color.White
                )
            ) {
                Text(
                    if (hasBackgroundLocation) {
                        "✅ صلاحية الموقع في الخلفية"
                    } else {
                        "4. السماح بالموقع في الخلفية (من الإعدادات)"
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        val coroutineScope = rememberCoroutineScope()
        
        Button(
            onClick = { 
                android.widget.Toast.makeText(context, "تم الانتهاء من منح الصلاحيات بنجاح!", android.widget.Toast.LENGTH_SHORT).show()
                coroutineScope.launch {
                    kotlinx.coroutines.delay(1000)
                    onNext()
                }
            }, 
            modifier = Modifier.fillMaxWidth(),
            enabled = allGranted
        ) { 
            Text("متابعة لإنشاء الحساب") 
        }
    }
}

@Composable
fun BatteryOptimizationButton() {
    val context = LocalContext.current
    var isExempted by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        isExempted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
    
    if (!isExempted) {
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)) // Yellow
        ) {
            Text("استثناء التطبيق من توفير البطارية", color = Color.White)
        }
    } else {
        Text("✅ التطبيق مستثنى من توفير البطارية", color = Color.Green)
    }
}

@Composable
fun OverlayPermissionButton() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        hasPermission = Settings.canDrawOverlays(context)
    }
    
    if (!hasPermission) {
        Button(
            onClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)) // Yellow
        ) {
            Text("السماح بعرض شاشة القفل", color = Color.White)
        }
    } else {
        Text("✅ صلاحية شاشة القفل مفعلة", color = Color.Green)
    }
}

@Composable
fun ChildConsentView(onAgree: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize(), 
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("موافقة الرقابة الأبوية", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("سيتم مراقبة هذا الجهاز من قبل والديك من أجل سلامتك. لا تقم بالاستمرار إذا لم تكن موافقاً.", textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onAgree, modifier = Modifier.fillMaxWidth()) { Text("أنا أوافق") }
    }
}

@Composable
fun AuthView(state: RegistrationState, onRegister: (String, String, String, Int) -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize(), 
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("تسجيل جهاز الطفل", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var childName by remember { mutableStateOf("") }
        var childAge by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("بريد ولي الأمر") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password, 
            onValueChange = { password = it }, 
            label = { Text("كلمة مرور ولي الأمر") }, 
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "إخفاء" else "إظهار")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = childName, onValueChange = { childName = it }, label = { Text("اسم الطفل") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = childAge, onValueChange = { childAge = it }, label = { Text("عمر الطفل") }, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { 
                val parsedAge = childAge.toIntOrNull() ?: 10
                val validAge = if (parsedAge < 1 || parsedAge > 17) 10 else parsedAge
                onRegister(email, password, childName, validAge) 
            }, 
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && email.isNotBlank() && password.isNotBlank() && childName.isNotBlank()
        ) { 
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("تسجيل الجهاز") 
            }
        }

        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = state.errorMessage, color = Color.Red, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SetupCompleteView(onComplete: () -> Unit) {
    var seconds by remember { mutableStateOf(5) }
    
    LaunchedEffect(Unit) {
        while (seconds > 0) {
            delay(1000)
            seconds--
        }
        
        onComplete()
    }

    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize(), 
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✅ تم التثبيت بنجاح!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
        Spacer(modifier = Modifier.height(16.dp))
        Text("التطبيق سيختفي خلال $seconds ثواني...", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Text("لفتح التطبيق مجدداً، افتح تطبيق الاتصال واطلب الكود:\n*#*#7269#*#*", textAlign = TextAlign.Center, color = Color.Gray)
    }
}
