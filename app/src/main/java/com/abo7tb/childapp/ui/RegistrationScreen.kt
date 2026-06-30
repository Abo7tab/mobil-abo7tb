package com.abo7tb.childapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.animation.with
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abo7tb.childapp.utils.DeviceAdminHelper
import androidx.hilt.navigation.compose.hiltViewModel
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
    val lifecycleOwner = LocalLifecycleOwner.current

    val corePermissions = remember {
        buildList {
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
    }

    val permissionsState = rememberMultiplePermissionsState(permissions = corePermissions)

    var refreshKey by remember { mutableIntStateOf(0) }

    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun refreshPermissionStates() {
        refreshKey++
    }

    val coreGranted = remember(refreshKey, permissionsState.allPermissionsGranted) {
        corePermissions.all { hasPermission(it) }
    }

    val hasBackgroundLocation = remember(refreshKey) {
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q ||
            hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    val hasFineLocation = remember(refreshKey) {
        hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    var isBatteryExempted by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(false) }
    var hasDeviceAdmin by remember { mutableStateOf(false) }

    fun updateSystemStates() {
        val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        isBatteryExempted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        hasOverlayPermission = Settings.canDrawOverlays(context)
        hasDeviceAdmin = DeviceAdminHelper.isAdminActive(context)
        refreshPermissionStates()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                updateSystemStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { updateSystemStates() }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { updateSystemStates() }

    val deviceAdminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { updateSystemStates() }

    // الموقع في الخلفية: إما الصلاحية الكاملة أو على الأقل الموقع الدقيق (لعدم حظر المستخدم على Samsung)
    val locationReady = hasBackgroundLocation || hasFineLocation

    val allGranted = coreGranted &&
        isBatteryExempted &&
        hasOverlayPermission &&
        locationReady &&
        hasDeviceAdmin

    val pendingItems = buildList {
        if (!coreGranted) add("الصلاحيات الأساسية (كاميرا، جهات اتصال، SMS، إشعارات...)")
        if (!hasFineLocation) add("صلاحية الموقع")
        if (hasFineLocation && !hasBackgroundLocation && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            add("الموقع طوال الوقت (مُفضّل — اختر «السماح طوال الوقت»)")
        }
        if (!isBatteryExempted) add("العمل الدائم في الخلفية (توفير البطارية)")
        if (!hasOverlayPermission) add("الظهور فوق التطبيقات (شاشة القفل)")
        if (!hasDeviceAdmin) add("حماية من الحذف (مدير الجهاز)")
    }

    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("الصلاحيات المطلوبة", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "يجب منح الصلاحيات التالية. إذا عدت من الإعدادات وزر المتابعة لا يزال معطّلاً، تأكد من النقاط بالأسفل.",
            textAlign = TextAlign.Center,
            color = Color.Red
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { permissionsState.launchMultiplePermissionRequest() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !coreGranted,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (coreGranted) Color.Green else MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Text(if (coreGranted) "✅ تم منح الصلاحيات الأساسية" else "1. منح الصلاحيات الأساسية")
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
                    if (!hasFineLocation) {
                        android.widget.Toast.makeText(
                            context,
                            "منح صلاحية الموقع أولاً من الزر 1",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        permissionsState.launchMultiplePermissionRequest()
                        return@Button
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        android.widget.Toast.makeText(
                            context,
                            "اختر: الأذونات ← الموقع ← السماح طوال الوقت",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    } else {
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !hasBackgroundLocation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        hasBackgroundLocation -> Color.Green
                        hasFineLocation -> Color(0xFF22C55E)
                        else -> Color(0xFFEAB308)
                    },
                    contentColor = Color.White
                )
            ) {
                Text(
                    when {
                        hasBackgroundLocation -> "✅ الموقع طوال الوقت"
                        hasFineLocation -> "4. تفعيل الموقع طوال الوقت (مُفضّل)"
                        else -> "4. الموقع في الخلفية (بعد منح الموقع أولاً)"
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                deviceAdminLauncher.launch(DeviceAdminHelper.createActivateAdminIntent(context))
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !hasDeviceAdmin,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasDeviceAdmin) Color.Green else Color(0xFFEAB308),
                contentColor = Color.White
            )
        ) {
            Text(
                if (hasDeviceAdmin) {
                    "✅ محمي من الحذف بدون ولي الأمر"
                } else {
                    "5. تفعيل الحماية من الحذف (مدير الجهاز)"
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (pendingItems.isNotEmpty()) {
            Text("متبقي:", style = MaterialTheme.typography.labelLarge, color = Color.Red)
            pendingItems.forEach { item ->
                Text("• $item", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB91C1C))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        val coroutineScope = rememberCoroutineScope()

        Button(
            onClick = {
                android.widget.Toast.makeText(context, "تم الانتهاء من منح الصلاحيات بنجاح!", android.widget.Toast.LENGTH_SHORT).show()
                coroutineScope.launch {
                    delay(1000)
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
fun EnableAdminView(onDone: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isActive by remember { mutableStateOf(DeviceAdminHelper.isAdminActive(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isActive = DeviceAdminHelper.isAdminActive(context)
        if (isActive) onDone()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isActive = DeviceAdminHelper.isAdminActive(context)
                if (isActive) onDone()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🛡️ حماية من الحذف", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "يجب تفعيل «مدير الجهاز» لمنع الطفل من حذف التطبيق بدون إيميل وكلمة مرور ولي الأمر.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { launcher.launch(DeviceAdminHelper.createActivateAdminIntent(context)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isActive
        ) {
            Text(if (isActive) "✅ تم تفعيل الحماية" else "تفعيل الحماية من الحذف")
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
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "🛡️ التطبيق محمي من الحذف — لا يمكن للطفل مسحه إلا بإيميل وكلمة مرور ولي الأمر.",
            textAlign = TextAlign.Center,
            color = Color(0xFF6366F1),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("لفتح التطبيق مجدداً:\n1. افتح تطبيق الهاتف\n2. اكتب: *#*#7269#*#*\n3. على Samsung: اضغط زر الاتصال الأخضر 🟢", textAlign = TextAlign.Center, color = Color.Gray)
    }
}
