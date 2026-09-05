package com.pims.vault.relationship

import com.pims.vault.domain.model.GraphRelationType
import com.pims.vault.domain.model.Relationship
import com.pims.vault.domain.model.RelationshipStatus
import com.pims.vault.domain.rules.GraphValidationException
import com.pims.vault.domain.rules.RelationshipGraphRules
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RelationshipGraphRulesTest {

    @Test(expected = GraphValidationException::class)
    fun testSelfRelationshipIsRejected() {
        RelationshipGraphRules.validateRelationshipCreation(
            sourcePersonId = "person_A",
            targetPersonId = "person_A", // Self
            type = GraphRelationType.FRIEND,
            existingRelationships = emptyList()
        )
    }

    @Test(expected = GraphValidationException::class)
    fun testDuplicateActiveRelationshipIsRejected() {
        val existing = listOf(
            Relationship(
                id = "rel_1",
                personAId = "person_A",
                personBId = "person_B",
                type = GraphRelationType.SIBLING,
                status = RelationshipStatus.ACTIVE
            )
        )

        RelationshipGraphRules.validateRelationshipCreation(
            sourcePersonId = "person_A",
            targetPersonId = "person_B",
            type = GraphRelationType.SIBLING,
            existingRelationships = existing
        )
    }

    @Test(expected = GraphValidationException::class)
    fun testMultipleActiveSpousePolicyViolation() {
        val existing = listOf(
            Relationship(
                id = "rel_1",
                personAId = "person_A",
                personBId = "person_B",
                type = GraphRelationType.SPOUSE,
                status = RelationshipStatus.ACTIVE
            )
        )

        // Attempting to link person_A to person_C as second active spouse
        RelationshipGraphRules.validateRelationshipCreation(
            sourcePersonId = "person_A",
            targetPersonId = "person_C",
            type = GraphRelationType.SPOUSE,
            existingRelationships = existing
        )
    }

    @Test(expected = GraphValidationException::class)
    fun testCircularAncestryCycleIsDetectedAndRejected() {
        // Existing tree:
        // Person A is parent of Person B
        // Person B is parent of Person C
        val existing = listOf(
            Relationship(
                id = "rel_1",
                personAId = "person_A",
                personBId = "person_B",
                type = GraphRelationType.PARENT,
                status = RelationshipStatus.ACTIVE
            ),
            Relationship(
                id = "rel_2",
                personAId = "person_B",
                personBId = "person_C",
                type = GraphRelationType.PARENT,
                status = RelationshipStatus.ACTIVE
            )
        )

        // Now attempt to declare Person C as parent of Person A (Loop: A -> B -> C -> A)
        RelationshipGraphRules.validateRelationshipCreation(
            sourcePersonId = "person_C",
            targetPersonId = "person_A",
            type = GraphRelationType.PARENT,
            existingRelationships = existing
        )
    }

    @Test
    fun testValidMultiGenerationalTreePassesValidation() {
        // Grandparent -> Parent -> Child
        val existing = listOf(
            Relationship(
                id = "rel_1",
                personAId = "grandparent_1",
                personBId = "parent_1",
                type = GraphRelationType.PARENT,
                status = RelationshipStatus.ACTIVE
            )
        )

        try {
            RelationshipGraphRules.validateRelationshipCreation(
                sourcePersonId = "parent_1",
                targetPersonId = "child_1",
                type = GraphRelationType.PARENT,
                existingRelationships = existing
            )
            // Success
        } catch (e: Exception) {
            fail("Valid parent-child hierarchy should not throw: ${e.message}")
        }
    }
}
