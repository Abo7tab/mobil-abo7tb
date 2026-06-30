package com.abo7tb.childapp.data.remote

import com.abo7tb.childapp.data.local.SecurePrefsManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val securePrefsManager: SecurePrefsManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        val token = securePrefsManager.getToken()
        
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        
        requestBuilder.addHeader("Accept", "application/json")
        // Removed Content-Type to avoid breaking Multipart requests; OkHttp handles body content types.
        
        return chain.proceed(requestBuilder.build())
    }
}
