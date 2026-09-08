package com.pims.vault.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pims.vault.data.local.converter.RoomConverters
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
import com.pims.vault.data.local.entity.AddressEntity
import com.pims.vault.data.local.entity.AuditEventEntity
import com.pims.vault.data.local.entity.ContactMethodEntity
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import com.pims.vault.data.local.entity.EducationRecordEntity
import com.pims.vault.data.local.entity.EmploymentRecordEntity
import com.pims.vault.data.local.entity.MedicalRecordEntity
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.data.local.entity.RelationshipEntity
import com.pims.vault.data.local.entity.SocialAccountEntity
import com.pims.vault.data.local.entity.VaultItemEntity

@Database(
    entities = [
        PersonEntity::class,
        ContactMethodEntity::class,
        AddressEntity::class,
        RelationshipEntity::class,
        DocumentEntity::class,
        DocumentVersionEntity::class,
        MedicalRecordEntity::class,
        EducationRecordEntity::class,
        EmploymentRecordEntity::class,
        SocialAccountEntity::class,
        VaultItemEntity::class,
        AuditEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class PimsDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao
    abstract fun contactDao(): ContactDao
    abstract fun addressDao(): AddressDao
    abstract fun relationshipDao(): RelationshipDao
    abstract fun documentDao(): DocumentDao
    abstract fun medicalDao(): MedicalDao
    abstract fun educationDao(): EducationDao
    abstract fun employmentDao(): EmploymentDao
    abstract fun socialAccountDao(): SocialAccountDao
    abstract fun vaultDao(): VaultDao
    abstract fun auditDao(): AuditDao

    companion object {
        const val DATABASE_NAME = "pims_identity_vault.db"

        /**
         * Builder helper for instantiating the database.
         * The open helper factory can be plugged in later for SQLCipher / encryption
         * without altering the database entity graph or DAO contracts.
         */
        fun buildDatabase(
            context: Context,
            databaseName: String = DATABASE_NAME,
            openHelperFactory: androidx.sqlite.db.SupportSQLiteOpenHelper.Factory? = null
        ): PimsDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                PimsDatabase::class.java,
                databaseName
            )

            if (openHelperFactory != null) {
                builder.openHelperFactory(openHelperFactory)
            }

            return builder.build()
        }
    }
}
