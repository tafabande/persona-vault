package com.pims.vault.core.di

import com.google.firebase.auth.FirebaseAuth
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.KeySecurityManager
import com.pims.vault.core.storage.B2StorageUploadService
import com.pims.vault.core.storage.StorageUploadService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Storage architecture: Firestore = structured data + metadata.
 * Backblaze B2 = authoritative object store for user-uploaded binaries.
 * Firebase Storage is intentionally NOT wired — no binary may flow through it.
 */
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
        keySecurityManager: KeySecurityManager
    ): B2StorageUploadService = B2StorageUploadService(cryptoEngine, keySecurityManager)

    @Provides
    @Singleton
    fun provideStorageUploadService(
        b2Service: B2StorageUploadService
    ): StorageUploadService = b2Service
}
