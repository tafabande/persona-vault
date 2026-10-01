package com.pims.vault.core.di

import com.google.firebase.auth.FirebaseAuth
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.storage.B2StorageUploadService
import com.pims.vault.core.storage.StorageUploadService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideB2StorageUploadService(
        cryptoEngine: CryptoEngine,
        portableFileKeyManager: com.pims.vault.core.crypto.PortableFileKeyManager
    ): B2StorageUploadService = B2StorageUploadService(cryptoEngine, portableFileKeyManager)

    @Provides
    @Singleton
    fun provideStorageUploadService(
        b2Service: B2StorageUploadService
    ): StorageUploadService = b2Service
}
