package com.pims.vault.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pims.vault.data.local.entity.SyncConflictEntity
import com.pims.vault.data.local.entity.SyncOperationStatus
import com.pims.vault.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: SyncQueueEntity): Long

    @Update
    suspend fun update(operation: SyncQueueEntity)

    @Delete
    suspend fun delete(operation: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY created_at ASC")
    fun getPendingOperationsFlow(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY created_at ASC")
    suspend fun getPendingOperations(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE entity_type = :entityType AND entity_id = :entityId AND status = 'PENDING' LIMIT 1")
    suspend fun findPendingByEntity(entityType: String, entityId: String): SyncQueueEntity?

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'CONFLICT'")
    fun getConflictCountFlow(): Flow<Int>

    @Query("SELECT * FROM sync_queue ORDER BY created_at DESC")
    fun getAllOperationsFlow(): Flow<List<SyncQueueEntity>>

    @Query("DELETE FROM sync_queue WHERE status = 'PENDING'")
    suspend fun clearPending()
}

@Dao
interface SyncConflictDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conflict: SyncConflictEntity): Long

    @Query("SELECT * FROM sync_conflicts WHERE is_resolved = 0 ORDER BY created_at DESC")
    fun getUnresolvedConflictsFlow(): Flow<List<SyncConflictEntity>>

    @Query("SELECT * FROM sync_conflicts WHERE is_resolved = 0 ORDER BY created_at DESC")
    suspend fun getUnresolvedConflicts(): List<SyncConflictEntity>

    @Query("SELECT * FROM sync_conflicts WHERE id = :id LIMIT 1")
    suspend fun getConflictById(id: String): SyncConflictEntity?

    @Query("UPDATE sync_conflicts SET is_resolved = 1, resolution_choice = :choice, resolved_at = :resolvedAt WHERE id = :id")
    suspend fun resolveConflict(id: String, choice: String, resolvedAt: Long)

    @Query("SELECT COUNT(*) FROM sync_conflicts WHERE is_resolved = 0")
    fun getUnresolvedCountFlow(): Flow<Int>
}
