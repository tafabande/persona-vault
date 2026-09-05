package com.pims.vault.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pims.vault.data.local.entity.AddressEntity
import com.pims.vault.data.local.entity.ContactMethodEntity
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.data.local.entity.RelationshipEntity
import com.pims.vault.data.local.relation.PersonRelationshipGraph
import com.pims.vault.data.local.relation.PersonWithFullProfile
import com.pims.vault.data.local.relation.RelationshipWithTargetPerson
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM persons WHERE is_primary_owner = 1 LIMIT 1")
    fun getPrimaryOwnerFlow(): Flow<PersonEntity?>

    @Query("SELECT * FROM persons WHERE is_primary_owner = 1 LIMIT 1")
    suspend fun getPrimaryOwner(): PersonEntity?

    @Query("SELECT * FROM persons WHERE id = :id")
    fun getPersonByIdFlow(id: String): Flow<PersonEntity?>

    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun getPersonById(id: String): PersonEntity?

    @Query("SELECT * FROM persons ORDER BY is_primary_owner DESC, last_name ASC, first_name ASC")
    fun getAllPersonsFlow(): Flow<List<PersonEntity>>

    @Transaction
    @Query("SELECT * FROM persons WHERE id = :id")
    fun getPersonWithFullProfileFlow(id: String): Flow<PersonWithFullProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(person: PersonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(persons: List<PersonEntity>)

    @Update
    suspend fun update(person: PersonEntity)

    @Delete
    suspend fun delete(person: PersonEntity)

    @Query("DELETE FROM persons WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contact_methods WHERE person_id = :personId ORDER BY is_primary DESC, created_at ASC")
    fun getContactsForPersonFlow(personId: String): Flow<List<ContactMethodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(contact: ContactMethodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactMethodEntity>)

    @Delete
    suspend fun delete(contact: ContactMethodEntity)

    @Query("DELETE FROM contact_methods WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface AddressDao {
    @Query("SELECT * FROM addresses WHERE person_id = :personId ORDER BY is_current DESC, created_at DESC")
    fun getAddressesForPersonFlow(personId: String): Flow<List<AddressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(address: AddressEntity)

    @Delete
    suspend fun delete(address: AddressEntity)

    @Query("DELETE FROM addresses WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface RelationshipDao {
    @Query("SELECT * FROM relationships WHERE source_person_id = :personId")
    fun getOutgoingRelationshipsFlow(personId: String): Flow<List<RelationshipEntity>>

    @Transaction
    @Query("SELECT * FROM relationships WHERE source_person_id = :personId")
    fun getRelationshipsWithPersonsFlow(personId: String): Flow<List<RelationshipWithTargetPerson>>

    @Transaction
    @Query("SELECT * FROM persons WHERE id = :personId")
    fun getPersonRelationshipGraphFlow(personId: String): Flow<PersonRelationshipGraph?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(relationship: RelationshipEntity)

    @Delete
    suspend fun delete(relationship: RelationshipEntity)

    @Query("DELETE FROM relationships WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM relationships WHERE (source_person_id = :p1 AND target_person_id = :p2) OR (source_person_id = :p2 AND target_person_id = :p1)")
    suspend fun deleteBetweenPersons(p1: String, p2: String)
}
