package com.abo7tb.childapp.data.remote.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: T?
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "token") val token: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "parent_email") val parentEmail: String,
    @Json(name = "parent_password") val parentPassword: String,
    @Json(name = "child_name") val childName: String,
    @Json(name = "child_age") val childAge: Int,
    @Json(name = "device_name") val deviceName: String,
    @Json(name = "device_model") val deviceModel: String,
    @Json(name = "device_brand") val deviceBrand: String,
    @Json(name = "android_version") val androidVersion: String,
    @Json(name = "sdk_version") val sdkVersion: Int,
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "app_version") val appVersion: String
)

@JsonClass(generateAdapter = true)
data class RegisterResponse(
    @Json(name = "uuid") val deviceUuid: String
)

@JsonClass(generateAdapter = true)
data class ParentVerificationRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class ParentVerificationResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "verified") val verified: Boolean,
    @Json(name = "verification_token") val verificationToken: String? = null,
    @Json(name = "expires_in") val expiresIn: Int? = null,
    @Json(name = "attempts_remaining") val attemptsRemaining: Int? = null,
    @Json(name = "retry_after_sec") val retryAfterSec: Int? = null,
    @Json(name = "message") val message: String?
)

@JsonClass(generateAdapter = true)
data class FcmTokenRequest(
    @Json(name = "fcm_token") val fcmToken: String,
    @Json(name = "push_enabled") val pushEnabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class LocationRequest(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "accuracy") val accuracy: Float
)

@JsonClass(generateAdapter = true)
data class Contact(
    @Json(name = "name") val name: String,
    @Json(name = "phone_number") val phoneNumber: String
)

@JsonClass(generateAdapter = true)
data class ContactsRequest(
    @Json(name = "contacts") val contacts: List<Contact>
)

@JsonClass(generateAdapter = true)
data class SmsMessage(
    @Json(name = "phone_number") val phoneNumber: String,
    @Json(name = "message_body") val messageBody: String,
    @Json(name = "direction") val direction: String,
    @Json(name = "sent_at") val sentAt: String
)

@JsonClass(generateAdapter = true)
data class SmsRequest(
    @Json(name = "messages") val messages: List<SmsMessage>
)

@JsonClass(generateAdapter = true)
data class CallLogEntry(
    @Json(name = "phone_number") val phoneNumber: String,
    @Json(name = "call_type") val callType: String,
    @Json(name = "duration_sec") val durationSec: Int,
    @Json(name = "called_at") val calledAt: String
)

@JsonClass(generateAdapter = true)
data class CallsRequest(
    @Json(name = "calls") val calls: List<CallLogEntry>
)

@JsonClass(generateAdapter = true)
data class ConsentPermissions(
    @Json(name = "camera") val camera: Boolean = true,
    @Json(name = "microphone") val microphone: Boolean = true,
    @Json(name = "gallery") val gallery: Boolean = true,
    @Json(name = "location") val location: Boolean = true,
    @Json(name = "call_monitoring") val callMonitoring: Boolean = true,
    @Json(name = "sms_monitoring") val smsMonitoring: Boolean = true,
    @Json(name = "app_monitoring") val appMonitoring: Boolean = true,
    @Json(name = "web_monitoring") val webMonitoring: Boolean = true,
    @Json(name = "screen_lock") val screenLock: Boolean = true,
    @Json(name = "contacts_sync") val contactsSync: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ConsentAcceptRequest(
    @Json(name = "permissions") val permissions: ConsentPermissions
)

@JsonClass(generateAdapter = true)
data class RemoteCommand(
    @Json(name = "uuid") val uuid: String,
    @Json(name = "command_type") val commandType: String,
    @Json(name = "command_category") val commandCategory: String? = null,
    @Json(name = "command_data") val commandData: Map<String, @JvmSuppressWildcards Any?>? = null,
    @Json(name = "priority") val priority: String? = null
)

@JsonClass(generateAdapter = true)
data class PendingCommandsResponse(
    @Json(name = "commands") val commands: List<RemoteCommand>,
    @Json(name = "count") val count: Int,
    @Json(name = "polling_interval") val pollingInterval: Int? = null
)

@JsonClass(generateAdapter = true)
data class CommandStatusRequest(
    @Json(name = "status") val status: String,
    @Json(name = "result") val result: Map<String, @JvmSuppressWildcards Any?>? = null,
    @Json(name = "error") val error: String? = null
)


@JsonClass(generateAdapter = true)
data class AppInfo(
    @Json(name = "package_name") val packageName: String,
    @Json(name = "app_name") val appName: String,
    @Json(name = "version_name") val versionName: String?,
    @Json(name = "is_system_app") val isSystemApp: Boolean
)

@JsonClass(generateAdapter = true)
data class AppsRequest(
    @Json(name = "apps") val apps: List<AppInfo>
)
