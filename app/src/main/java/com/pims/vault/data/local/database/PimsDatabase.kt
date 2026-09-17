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
import com.pims.vault.data.local.dao.RelationshipNoteDao
import com.pims.vault.data.local.dao.SocialAccountDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.dao.SyncConflictDao
import com.pims.vault.data.local.dao.SyncQueueDao
import com.pims.vault.data.local.entity.AddressEntity
import com.pims.vault.data.local.entity.AuditEventEntity
import com.pims.vault.data.local.entity.ContactMethodEntity
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import com.pims.vault.data.local.entity.EducationRecordEntity
import com.pims.vault.data.local.entity.EmploymentRecordEntity
import com.pims.vault.data.local.entity.MedicalProfileEntity
import com.pims.vault.data.local.entity.MedicalRecordEntity
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.data.local.entity.RelationshipEntity
import com.pims.vault.data.local.entity.RelationshipNoteEntity
import com.pims.vault.data.local.entity.SocialAccountEntity
import com.pims.vault.data.local.entity.SyncConflictEntity
import com.pims.vault.data.local.entity.SyncQueueEntity
import com.pims.vault.data.local.entity.SharingProfileEntity
import com.pims.vault.data.local.entity.SharingProfileDao
import com.pims.vault.data.local.entity.VaultItemEntity

@Database(
    entities = [
        PersonEntity::class,
        ContactMethodEntity::class,
        AddressEntity::class,
        RelationshipEntity::class,
        RelationshipNoteEntity::class,
        DocumentEntity::class,
        DocumentVersionEntity::class,
        MedicalProfileEntity::class,
        MedicalRecordEntity::class,
        EducationRecordEntity::class,
        EmploymentRecordEntity::class,
        SocialAccountEntity::class,
        VaultItemEntity::class,
        AuditEventEntity::class,
        SyncQueueEntity::class,
        SyncConflictEntity::class,
        SharingProfileEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class PimsDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao
    abstract fun contactDao(): ContactDao
    abstract fun addressDao(): AddressDao
    abstract fun relationshipDao(): RelationshipDao
    abstract fun relationshipNoteDao(): RelationshipNoteDao
    abstract fun documentDao(): DocumentDao
    abstract fun medicalDao(): MedicalDao
    abstract fun educationDao(): EducationDao
    abstract fun employmentDao(): EmploymentDao
    abstract fun socialAccountDao(): SocialAccountDao
    abstract fun vaultDao(): VaultDao
    abstract fun auditDao(): AuditDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun syncConflictDao(): SyncConflictDao
    abstract fun sharingProfileDao(): SharingProfileDao

    companion object {
        const val DATABASE_NAME = "pims_identity_vault.db"

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_queue` (
                        `id` TEXT NOT NULL, 
                        `operation_id` TEXT NOT NULL, 
                        `entity_type` TEXT NOT NULL, 
                        `entity_id` TEXT NOT NULL, 
                        `action` TEXT NOT NULL, 
                        `payload_json` TEXT NOT NULL, 
                        `local_version` INTEGER NOT NULL, 
                        `attempts` INTEGER NOT NULL, 
                        `status` TEXT NOT NULL, 
                        `error_message` TEXT, 
                        `created_at` INTEGER NOT NULL, 
                        `updated_at` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_conflicts` (
                        `id` TEXT NOT NULL, 
                        `entity_type` TEXT NOT NULL, 
                        `entity_id` TEXT NOT NULL, 
                        `field_name` TEXT NOT NULL, 
                        `local_value` TEXT NOT NULL, 
                        `remote_value` TEXT NOT NULL, 
                        `local_version` INTEGER NOT NULL, 
                        `server_version` INTEGER NOT NULL, 
                        `is_resolved` INTEGER NOT NULL, 
                        `resolution_choice` TEXT, 
                        `created_at` INTEGER NOT NULL, 
                        `resolved_at` INTEGER, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_status` ON `sync_queue` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_entity_type_entity_id` ON `sync_queue` (`entity_type`, `entity_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_queue_operation_id` ON `sync_queue` (`operation_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_conflicts_entity_type_entity_id` ON `sync_conflicts` (`entity_type`, `entity_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_conflicts_is_resolved` ON `sync_conflicts` (`is_resolved`)")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sharing_profiles` (
                        `id` TEXT NOT NULL, 
                        `owner_person_id` TEXT NOT NULL, 
                        `token_hash` TEXT NOT NULL, 
                        `scope` TEXT NOT NULL, 
                        `share_type` TEXT NOT NULL, 
                        `label` TEXT NOT NULL, 
                        `allowed_field_mask` INTEGER NOT NULL, 
                        `allowed_resource_ids_json` TEXT NOT NULL, 
                        `recipient_person_id` TEXT, 
                        `max_access_count` INTEGER, 
                        `access_count` INTEGER NOT NULL, 
                        `expires_at` INTEGER, 
                        `revoked_at` INTEGER, 
                        `last_accessed_at` INTEGER, 
                        `created_at` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sharing_profiles_token_hash` ON `sharing_profiles` (`token_hash`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sharing_profiles_owner_person_id` ON `sharing_profiles` (`owner_person_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sharing_profiles_revoked_at` ON `sharing_profiles` (`revoked_at`)")

                db.execSQL("ALTER TABLE `persons` ADD COLUMN `account_uid` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `persons` ADD COLUMN `national_id_number` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `persons` ADD COLUMN `previous_names` TEXT DEFAULT NULL")

                db.execSQL("ALTER TABLE `addresses` ADD COLUMN `valid_from` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `addresses` ADD COLUMN `valid_to` INTEGER DEFAULT NULL")

                db.execSQL("ALTER TABLE `relationships` ADD COLUMN `label_for_a` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `relationships` ADD COLUMN `label_for_b` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `relationships` ADD COLUMN `asymmetric_label` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `relationships` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'ACTIVE'")
                db.execSQL("ALTER TABLE `relationships` ADD COLUMN `is_private` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `relationship_notes` (
                        `id` TEXT NOT NULL, 
                        `relationship_id` TEXT NOT NULL, 
                        `topic` TEXT, 
                        `content_plaintext` TEXT, 
                        `encrypted_payload` BLOB, 
                        `encryption_iv` TEXT, 
                        `format` TEXT NOT NULL, 
                        `is_private` INTEGER NOT NULL, 
                        `created_at` INTEGER NOT NULL, 
                        `updated_at` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`relationship_id`) REFERENCES `relationships`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_relationship_notes_relationship_id` ON `relationship_notes` (`relationship_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_relationship_notes_relationship_id_is_private` ON `relationship_notes` (`relationship_id`, `is_private`)")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `medical_profiles` (
                        `person_id` TEXT NOT NULL, 
                        `blood_type` TEXT, 
                        `height_cm` TEXT, 
                        `weight_kg` TEXT, 
                        `emergency_contact_name` TEXT, 
                        `emergency_contact_phone` TEXT, 
                        `emergency_contact_relationship` TEXT, 
                        `medical_aid_provider` TEXT, 
                        `medical_aid_number` TEXT, 
                        `membership_number` TEXT, 
                        `policy_number` TEXT, 
                        `updated_at` INTEGER NOT NULL, 
                        PRIMARY KEY(`person_id`), 
                        FOREIGN KEY(`person_id`) REFERENCES `persons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_medical_profiles_person_id` ON `medical_profiles` (`person_id`)")

                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `route` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `start_date` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `end_date` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `contact_email` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `address` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `specialty` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `patient_number` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `policy_number` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `membership_number` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `plan_name` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `valid_until` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `visit_date` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `treatment` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `follow_up_date` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `instructions` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `photo_uri` TEXT DEFAULT NULL")
            }
        }

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
            ).addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5
            )

            if (openHelperFactory != null) {
                builder.openHelperFactory(openHelperFactory)
            }

            return builder.build()
        }
    }
}
