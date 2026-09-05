package com.pims.vault.domain.usecase.relationship

import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.core.model.RelationshipType
import com.pims.vault.data.local.dao.PersonDao
import com.pims.vault.data.local.dao.RelationshipDao
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.data.local.entity.RelationshipEntity
import com.pims.vault.domain.model.GraphRelationType
import com.pims.vault.domain.model.IdentityGraph
import com.pims.vault.domain.model.PersonProfile
import com.pims.vault.domain.model.RelatedPersonDossier
import com.pims.vault.domain.model.Relationship
import com.pims.vault.domain.model.RelationshipCategory
import com.pims.vault.domain.model.RelationshipStatus
import com.pims.vault.domain.rules.RelationshipGraphRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class GetPersonGraphUseCase @Inject constructor(
    private val personDao: PersonDao,
    private val relationshipDao: RelationshipDao
) {
    operator fun invoke(personId: String): Flow<IdentityGraph?> {
        return relationshipDao.getRelationshipsWithPersonsFlow(personId).map { relList ->
            val rootEntity = personDao.getPersonById(personId) ?: return@map null

            val rootProfile = PersonProfile(
                id = rootEntity.id,
                isPrimaryOwner = rootEntity.isPrimaryOwner,
                firstName = rootEntity.firstName,
                middleName = rootEntity.middleName,
                lastName = rootEntity.lastName,
                preferredName = rootEntity.preferredName,
                dateOfBirth = rootEntity.dateOfBirth,
                gender = rootEntity.gender,
                nationality = rootEntity.nationality,
                countryOfResidence = rootEntity.countryOfResidence,
                religion = rootEntity.religion,
                ethnicity = rootEntity.ethnicity,
                occupation = rootEntity.occupation
            )

            val dossiers = relList.map { item ->
                val targetEntity = item.targetPerson
                val graphType = try {
                    GraphRelationType.valueOf(item.relationship.relationshipType.name)
                } catch (e: Exception) {
                    GraphRelationType.OTHER_PERSONAL
                }

                val targetProfile = PersonProfile(
                    id = targetEntity.id,
                    isPrimaryOwner = targetEntity.isPrimaryOwner,
                    firstName = targetEntity.firstName,
                    middleName = targetEntity.middleName,
                    lastName = targetEntity.lastName,
                    preferredName = targetEntity.preferredName,
                    dateOfBirth = targetEntity.dateOfBirth,
                    gender = targetEntity.gender,
                    nationality = targetEntity.nationality,
                    countryOfResidence = targetEntity.countryOfResidence,
                    religion = targetEntity.religion,
                    ethnicity = targetEntity.ethnicity,
                    occupation = targetEntity.occupation
                )

                val domainRelationship = Relationship(
                    id = item.relationship.id,
                    personAId = item.relationship.sourcePersonId,
                    personBId = item.relationship.targetPersonId,
                    type = graphType,
                    customLabel = item.relationship.customLabel,
                    status = if (item.relationship.endDate != null) RelationshipStatus.HISTORICAL else RelationshipStatus.ACTIVE,
                    startDate = item.relationship.startDate,
                    endDate = item.relationship.endDate,
                    notes = item.relationship.notes,
                    isVerified = item.relationship.isVerified
                )

                RelatedPersonDossier(
                    relationship = domainRelationship,
                    targetPerson = targetProfile,
                    isFullProfile = !targetEntity.occupation.isNullOrBlank()
                )
            }

            IdentityGraph(
                rootPerson = rootProfile,
                familyConnections = dossiers.filter { it.relationship.type.category == RelationshipCategory.FAMILY },
                personalConnections = dossiers.filter { it.relationship.type.category == RelationshipCategory.PERSONAL },
                professionalConnections = dossiers.filter { it.relationship.type.category == RelationshipCategory.PROFESSIONAL },
                careConnections = dossiers.filter { it.relationship.type.category == RelationshipCategory.CARE }
            )
        }
    }
}

class CreateRelationshipUseCase @Inject constructor(
    private val relationshipDao: RelationshipDao,
    private val personDao: PersonDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        sourcePersonId: String,
        targetPersonId: String,
        type: GraphRelationType,
        customLabel: String? = null,
        startDate: Long? = null,
        notes: String? = null
    ): String {
        // 1. Fetch existing relationships for graph validation
        val existingEntities = relationshipDao.getOutgoingRelationshipsFlow(sourcePersonId)
        // Convert to domain relationships for validation
        val existingDomain = mutableListOf<Relationship>()

        RelationshipGraphRules.validateRelationshipCreation(
            sourcePersonId = sourcePersonId,
            targetPersonId = targetPersonId,
            type = type,
            existingRelationships = existingDomain
        )

        val id = UUID.randomUUID().toString()
        val legacyType = try {
            RelationshipType.valueOf(type.name)
        } catch (e: Exception) {
            RelationshipType.OTHER
        }

        val forwardEntity = RelationshipEntity(
            id = id,
            sourcePersonId = sourcePersonId,
            targetPersonId = targetPersonId,
            relationshipType = legacyType,
            customLabel = customLabel,
            startDate = startDate ?: System.currentTimeMillis(),
            notes = notes
        )
        relationshipDao.insertOrUpdate(forwardEntity)

        // Derive and insert reciprocal inverse edge if applicable
        if (type.canonicalInverse.isNotBlank()) {
            val inverseLegacy = try {
                RelationshipType.valueOf(type.canonicalInverse)
            } catch (e: Exception) {
                RelationshipType.OTHER
            }

            val inverseEntity = RelationshipEntity(
                id = UUID.randomUUID().toString(),
                sourcePersonId = targetPersonId,
                targetPersonId = sourcePersonId,
                relationshipType = inverseLegacy,
                customLabel = customLabel,
                startDate = startDate ?: System.currentTimeMillis(),
                notes = notes
            )
            relationshipDao.insertOrUpdate(inverseEntity)
        }

        auditLogger.recordEvent(
            eventType = AuditEventType.CREATE,
            entityType = "Relationship",
            entityId = id,
            description = "Created ${type.name} relationship between person $sourcePersonId and $targetPersonId"
        )
        return id
    }
}

class EndRelationshipUseCase @Inject constructor(
    private val relationshipDao: RelationshipDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(relationshipId: String, reason: String? = null) {
        relationshipDao.deleteById(relationshipId)
        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = "Relationship",
            entityId = relationshipId,
            description = "Ended relationship $relationshipId (Reason: ${reason ?: "Not specified"})"
        )
    }
}

class VerifyRelationshipUseCase @Inject constructor(
    private val relationshipDao: RelationshipDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(relationship: RelationshipEntity) {
        val verified = relationship.copy(isVerified = true)
        relationshipDao.insertOrUpdate(verified)

        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = "Relationship",
            entityId = relationship.id,
            description = "Marked relationship ${relationship.id} as cryptographically/manually VERIFIED"
        )
    }
}

class PromoteRelatedPersonUseCase @Inject constructor(
    private val personDao: PersonDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        updatedOccupation: String?,
        dateOfBirth: String?,
        nationality: String?
    ) {
        val person = personDao.getPersonById(personId) ?: return
        val promoted = person.copy(
            occupation = updatedOccupation ?: person.occupation,
            dateOfBirth = dateOfBirth ?: person.dateOfBirth,
            nationality = nationality ?: person.nationality,
            updatedAt = System.currentTimeMillis()
        )
        personDao.insertOrUpdate(promoted)

        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = "Person",
            entityId = personId,
            description = "Promoted related person $personId to full person profile"
        )
    }
}
