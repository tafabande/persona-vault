package com.pims.vault.core.di

import android.content.Context
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.crypto.KeySecurityManager
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.data.local.dao.AddressDao
import com.pims.vault.data.local.dao.AuditDao
import com.pims.vault.data.local.dao.ContactDao
import com.pims.vault.data.local.dao.DocumentDao
import com.pims.vault.data.local.dao.EducationDao
import com.pims.vault.data.local.dao.EmploymentDao
import com.pims.vault.data.local.dao.MedicalDao
import com.pims.vault.data.local.dao.PersonDao
import com.pims.vault.data.local.dao.RelationshipDao
import com.pims.vault.data.local.dao.SocialAccountDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.database.PimsDatabase
import com.pims.vault.data.repository.AuditRepositoryImpl
import com.pims.vault.data.repository.DocumentRepositoryImpl
import com.pims.vault.data.repository.MedicalRepositoryImpl
import com.pims.vault.data.repository.PersonRepositoryImpl
import com.pims.vault.data.repository.RelationshipRepositoryImpl
import com.pims.vault.data.repository.VaultRepositoryImpl
import com.pims.vault.domain.repository.AuditRepository
import com.pims.vault.domain.repository.DocumentRepository
import com.pims.vault.domain.repository.MedicalRepository
import com.pims.vault.domain.repository.PersonRepository
import com.pims.vault.domain.repository.RelationshipRepository
import com.pims.vault.domain.repository.VaultRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePimsDatabase(
        @ApplicationContext context: Context,
        keySecurityManager: KeySecurityManager
    ): PimsDatabase {
        // Derive the SQLCipher key from the app keystore instead of baking secrets into source.
        val dbKey = keySecurityManager.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_DATABASE)
        val sqlCipherFactory = SupportFactory(dbKey.copyBytes())
        dbKey.close()
        return PimsDatabase.buildDatabase(
            context = context,
            openHelperFactory = sqlCipherFactory
        )
    }

    @Provides fun providePersonDao(db: PimsDatabase): PersonDao = db.personDao()
    @Provides fun provideContactDao(db: PimsDatabase): ContactDao = db.contactDao()
    @Provides fun provideAddressDao(db: PimsDatabase): AddressDao = db.addressDao()
    @Provides fun provideRelationshipDao(db: PimsDatabase): RelationshipDao = db.relationshipDao()
    @Provides fun provideDocumentDao(db: PimsDatabase): DocumentDao = db.documentDao()
    @Provides fun provideMedicalDao(db: PimsDatabase): MedicalDao = db.medicalDao()
    @Provides fun provideEducationDao(db: PimsDatabase): EducationDao = db.educationDao()
    @Provides fun provideEmploymentDao(db: PimsDatabase): EmploymentDao = db.employmentDao()
    @Provides fun provideSocialAccountDao(db: PimsDatabase): SocialAccountDao = db.socialAccountDao()
    @Provides fun provideVaultDao(db: PimsDatabase): VaultDao = db.vaultDao()
    @Provides fun provideAuditDao(db: PimsDatabase): AuditDao = db.auditDao()
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePersonRepository(
        personDao: PersonDao,
        contactDao: ContactDao,
        addressDao: AddressDao,
        auditLogger: HardenedAuditLogger
    ): PersonRepository = PersonRepositoryImpl(personDao, contactDao, addressDao, auditLogger)

    @Provides
    @Singleton
    fun provideRelationshipRepository(
        relationshipDao: RelationshipDao,
        auditLogger: HardenedAuditLogger
    ): RelationshipRepository = RelationshipRepositoryImpl(relationshipDao, auditLogger)

    @Provides
    @Singleton
    fun provideDocumentRepository(
        documentDao: DocumentDao,
        fileStorage: FileStorageService,
        auditLogger: HardenedAuditLogger
    ): DocumentRepository = DocumentRepositoryImpl(documentDao, fileStorage, auditLogger)

    @Provides
    @Singleton
    fun provideMedicalRepository(
        medicalDao: MedicalDao,
        auditLogger: HardenedAuditLogger
    ): MedicalRepository = MedicalRepositoryImpl(medicalDao, auditLogger)

    @Provides
    @Singleton
    fun provideVaultRepository(
        vaultDao: VaultDao,
        cryptoEngine: CryptoEngine,
        sessionManager: BiometricSessionManager,
        auditLogger: HardenedAuditLogger
    ): VaultRepository = VaultRepositoryImpl(vaultDao, cryptoEngine, sessionManager, auditLogger)

    @Provides
    @Singleton
    fun provideAuditRepository(
        auditLogger: HardenedAuditLogger
    ): AuditRepository = AuditRepositoryImpl(auditLogger)
}
