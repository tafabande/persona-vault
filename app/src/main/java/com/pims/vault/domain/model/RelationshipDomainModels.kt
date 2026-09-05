package com.pims.vault.domain.model

import com.pims.vault.core.model.SecurityClassification

enum class RelationshipCategory {
    FAMILY,
    PERSONAL,
    PROFESSIONAL,
    CARE
}

enum class RelationshipStatus {
    ACTIVE,
    HISTORICAL,
    DISSOLVED,
    UNKNOWN
}

enum class GraphRelationType(
    val category: RelationshipCategory,
    val isSymmetric: Boolean,
    val canonicalInverse: String,
    val displayLabel: String
) {
    // FAMILY
    PARENT(RelationshipCategory.FAMILY, false, "CHILD", "Parent of"),
    CHILD(RelationshipCategory.FAMILY, false, "PARENT", "Child of"),
    SIBLING(RelationshipCategory.FAMILY, true, "SIBLING", "Sibling of"),
    SPOUSE(RelationshipCategory.FAMILY, true, "SPOUSE", "Spouse of"),
    PARTNER(RelationshipCategory.FAMILY, true, "PARTNER", "Partner of"),
    GUARDIAN(RelationshipCategory.FAMILY, false, "DEPENDENT", "Guardian of"),
    DEPENDENT(RelationshipCategory.FAMILY, false, "GUARDIAN", "Dependent of"),

    // PERSONAL
    FRIEND(RelationshipCategory.PERSONAL, true, "FRIEND", "Friend of"),
    EMERGENCY_CONTACT(RelationshipCategory.PERSONAL, false, "PROTECTED_PERSON", "Emergency Contact for"),
    OTHER_PERSONAL(RelationshipCategory.PERSONAL, true, "OTHER_PERSONAL", "Related to"),

    // PROFESSIONAL
    COLLEAGUE(RelationshipCategory.PROFESSIONAL, true, "COLLEAGUE", "Colleague of"),
    EMPLOYER(RelationshipCategory.PROFESSIONAL, false, "EMPLOYEE", "Employer of"),
    EMPLOYEE(RelationshipCategory.PROFESSIONAL, false, "EMPLOYER", "Employee of"),
    MANAGER(RelationshipCategory.PROFESSIONAL, false, "DIRECT_REPORT", "Manager of"),

    // CARE
    DOCTOR(RelationshipCategory.CARE, false, "PATIENT", "Doctor of"),
    CAREGIVER(RelationshipCategory.CARE, false, "CARE_RECIPIENT", "Caregiver for"),
    HOSPITAL_CONTACT(RelationshipCategory.CARE, false, "PATIENT", "Hospital Contact for")
}

data class Relationship(
    val id: String,
    val personAId: String,
    val personBId: String,
    val type: GraphRelationType,
    val customLabel: String? = null,
    val status: RelationshipStatus = RelationshipStatus.ACTIVE,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val notes: String? = null,
    val isVerified: Boolean = false,
    val securityClassification: SecurityClassification = SecurityClassification.ZONE_2_PRIVATE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1
)

/**
 * Related Person Dossier: Represents an edge in the graph pointing to either
 * a lightweight related person or a fully populated PersonProfile.
 */
data class RelatedPersonDossier(
    val relationship: Relationship,
    val targetPerson: PersonProfile,
    val isFullProfile: Boolean = false
) {
    val displayRelationshipLabel: String
        get() = relationship.customLabel?.takeIf { it.isNotBlank() } ?: relationship.type.displayLabel
}

/**
 * Complete Kinship and Interpersonal Graph View for an individual.
 */
data class IdentityGraph(
    val rootPerson: PersonProfile,
    val familyConnections: List<RelatedPersonDossier> = emptyList(),
    val personalConnections: List<RelatedPersonDossier> = emptyList(),
    val professionalConnections: List<RelatedPersonDossier> = emptyList(),
    val careConnections: List<RelatedPersonDossier> = emptyList()
) {
    val totalConnections: Int
        get() = familyConnections.size + personalConnections.size + professionalConnections.size + careConnections.size
}
