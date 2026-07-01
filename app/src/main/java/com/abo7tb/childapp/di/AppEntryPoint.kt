package com.abo7tb.childapp.di

import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.utils.ProtectionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun protectionManager(): ProtectionManager
    fun securePrefsManager(): SecurePrefsManager
}
