package com.pims.vault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "sharing_profiles",
    indices = [
        Index(value = ["token_hash"], unique = true),
        Index(value = ["owner_person_id"]),
        Index(value = ["revoked_at"])
    ]
)
data class SharingProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "owner_person_id")
    val ownerPersonId: String,

    @ColumnInfo(name = "token_hash")
    val tokenHash: String, // SHA-256 hash of raw capability token

    @ColumnInfo(name = "scope")
    val scope: String = "SHARED_PROFILE", // SELF, CONNECTED_PERSON, SHARED_PROFILE, EMERGENCY, VAULT

    @ColumnInfo(name = "share_type")
    val shareType: String, // QR_CODE, SECURE_LINK, DIRECT_P2P, EMERGENCY_ICE

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "allowed_field_mask")
    val allowedFieldMask: Long,

    @ColumnInfo(name = "allowed_resource_ids_json")
    val allowedResourceIdsJson: String = "[]",

    @ColumnInfo(name = "recipient_person_id")
    val recipientPersonId: String? = null,

    @ColumnInfo(name = "max_access_count")
    val maxAccessCount: Int? = null,

    @ColumnInfo(name = "access_count")
    val accessCount: Int = 0,

    @ColumnInfo(name = "expires_at")
    val expiresAt: Long? = null,

    @ColumnInfo(name = "revoked_at")
    val revokedAt: Long? = null,

    @ColumnInfo(name = "last_accessed_at")
    val lastAccessedAt: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface SharingProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SharingProfileEntity): Long

    @Update
    suspend fun update(entity: SharingProfileEntity)

    @Query("SELECT * FROM sharing_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SharingProfileEntity?

    @Query("SELECT * FROM sharing_profiles WHERE token_hash = :tokenHash LIMIT 1")
    suspend fun getByTokenHash(tokenHash: String): SharingProfileEntity?

    @Query("SELECT * FROM sharing_profiles WHERE owner_person_id = :personId AND revoked_at IS NULL ORDER BY created_at DESC")
    fun getActiveSharesFlow(personId: String): Flow<List<SharingProfileEntity>>

    @Query("UPDATE sharing_profiles SET revoked_at = :timestamp WHERE id = :id")
    suspend fun revokeShare(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM sharing_profiles WHERE id = :id")
    suspend fun deleteShare(id: String)

    @Query("SELECT COUNT(*) FROM sharing_profiles WHERE revoked_at IS NULL")
    suspend fun countActiveShares(): Int
}
