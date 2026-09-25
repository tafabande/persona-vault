package com.pims.vault.core.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.KeySecurityManager
import com.pims.vault.core.storage.B2StorageUploadService
import com.pims.vault.core.storage.FirebaseStorageUploadService
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
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideB2StorageUploadService(
        cryptoEngine: CryptoEngine,
        keySecurityManager: KeySecurityManager
    ): B2StorageUploadService = B2StorageUploadService(cryptoEngine, keySecurityManager)

    @Provides
    @Singleton
    fun provideFirebaseStorageUploadService(
        cryptoEngine: CryptoEngine,
        keySecurityManager: KeySecurityManager,
        storage: FirebaseStorage,
        auth: FirebaseAuth
    ): FirebaseStorageUploadService = FirebaseStorageUploadService(cryptoEngine, keySecurityManager, storage, auth)

    @Provides
    @Singleton
    fun provideStorageUploadService(
        b2Service: B2StorageUploadService
    ): StorageUploadService = b2Service
}
