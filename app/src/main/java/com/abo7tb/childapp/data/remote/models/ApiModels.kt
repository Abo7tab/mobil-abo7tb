package com.abo7tb.childapp.data.remote.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

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
    @Json(name = "device_uuid") val deviceUuid: String,
    @Json(name = "token") val token: String
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
