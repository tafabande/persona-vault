package com.pims.vault.core.di

import android.content.Context
import com.pims.vault.core.crypto.BackupCryptoEngine
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.crypto.KeySecurityManager
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.data.local.dao.AuditDao
import com.pims.vault.data.local.storage.EncryptedFileStorageImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideKeySecurityManager(@ApplicationContext context: Context): KeySecurityManager {
        return KeySecurityManager(context)
    }

    @Provides
    @Singleton
    fun provideCryptoEngine(): CryptoEngine {
        // Enforce HardenedCryptoEngine for all application builds
        return HardenedCryptoEngine()
    }

    @Provides
    @Singleton
    fun provideBiometricSessionManager(keySecurityManager: KeySecurityManager): BiometricSessionManager {
        return BiometricSessionManager(keySecurityManager)
    }

    @Provides
    @Singleton
    fun provideHardenedAuditLogger(
        auditDao: AuditDao,
        cryptoEngine: CryptoEngine,
        sessionManager: BiometricSessionManager
    ): HardenedAuditLogger {
        return HardenedAuditLogger(auditDao, cryptoEngine, sessionManager)
    }

    @Provides
    @Singleton
    fun providePortableFileKeyManager(
        @ApplicationContext context: Context,
        keySecurityManager: KeySecurityManager,
        cryptoEngine: CryptoEngine
    ): com.pims.vault.core.crypto.PortableFileKeyManager {
        return com.pims.vault.core.crypto.PortableFileKeyManager(context, keySecurityManager, cryptoEngine)
    }

    @Provides
    @Singleton
    fun provideFileStorageService(
        @ApplicationContext context: Context,
        cryptoEngine: CryptoEngine,
        portableFileKeyManager: com.pims.vault.core.crypto.PortableFileKeyManager,
        sessionManager: BiometricSessionManager
    ): FileStorageService {
        return EncryptedFileStorageImpl(
            context = context,
            cryptoEngine = cryptoEngine,
            // Portable file key (cross-device) with Keystore-wrapped local
            // protection. Falls back to the legacy device-bound key only when
            // no portable key exists yet (pre-migration installs).
            keyProvider = {
                try {
                    portableFileKeyManager.copyKeyBytes()
                } catch (_: Exception) {
                    sessionManager.getFileStorageKey()
                }
            }
        )
    }

    @Provides
    @Singleton
    fun provideBackupCryptoEngine(): BackupCryptoEngine {
        return BackupCryptoEngine()
    }
}
