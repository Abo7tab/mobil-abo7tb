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
    @Json(name = "verified") val verified: Boolean,
    @Json(name = "verification_token") val verificationToken: String?
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
    @Json(name = "accuracy") val accuracy: Float,
    @Json(name = "recorded_at") val recordedAt: Long
)

@JsonClass(generateAdapter = true)
data class Contact(
    @Json(name = "name") val name: String,
    @Json(name = "number") val number: String
)

@JsonClass(generateAdapter = true)
data class ContactsRequest(
    @Json(name = "contacts") val contacts: List<Contact>
)

@JsonClass(generateAdapter = true)
data class SmsMessage(
    @Json(name = "address") val address: String,
    @Json(name = "body") val body: String,
    @Json(name = "date") val date: Long,
    @Json(name = "type") val type: Int
)

@JsonClass(generateAdapter = true)
data class SmsRequest(
    @Json(name = "sms_list") val smsList: List<SmsMessage>
)

@JsonClass(generateAdapter = true)
data class CallLog(
    @Json(name = "number") val number: String,
    @Json(name = "date") val date: Long,
    @Json(name = "duration") val duration: Int,
    @Json(name = "type") val type: Int
)

@JsonClass(generateAdapter = true)
data class CallsRequest(
    @Json(name = "call_logs") val callLogs: List<CallLog>
)
