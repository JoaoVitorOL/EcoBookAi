package com.ecobook.di

import com.ecobook.utils.SecureStorage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SecureStorageEntryPoint {
    fun secureStorage(): SecureStorage
}
