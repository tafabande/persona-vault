package com.pims.vault.domain.rules

import com.pims.vault.domain.model.GraphRelationType
import com.pims.vault.domain.model.Relationship
import com.pims.vault.domain.model.RelationshipStatus

class GraphValidationException(message: String) : IllegalArgumentException(message)

/**
 * Graph Validation Engine: Enforces semantic consistency, symmetry, and cycle-free ancestry trees.
 */
object RelationshipGraphRules {

    /**
     * Validates that a relationship candidate does not violate core graph invariants.
     */
    fun validateRelationshipCreation(
        sourcePersonId: String,
        targetPersonId: String,
        type: GraphRelationType,
        existingRelationships: List<Relationship>
    ) {
        // Rule 1: No self-relationships
        if (sourcePersonId == targetPersonId) {
            throw GraphValidationException("Invalid relationship: A person cannot be related to themselves.")
        }

        // Rule 2: Prevent duplicate active relationships between the exact same individuals with the same type
        val hasDuplicate = existingRelationships.any {
            it.status == RelationshipStatus.ACTIVE &&
            ((it.personAId == sourcePersonId && it.personBId == targetPersonId && it.type == type) ||
             (it.type.isSymmetric && it.personAId == targetPersonId && it.personBId == sourcePersonId && it.type == type))
        }
        if (hasDuplicate) {
            throw GraphValidationException("Duplicate relationship: An active ${type.name} connection already exists between these persons.")
        }

        // Rule 3: Single active spouse policy
        if (type == GraphRelationType.SPOUSE) {
            val hasExistingSpouse = existingRelationships.any {
                it.status == RelationshipStatus.ACTIVE &&
                it.type == GraphRelationType.SPOUSE &&
                (it.personAId == sourcePersonId || it.personBId == sourcePersonId)
            }
            if (hasExistingSpouse) {
                throw GraphValidationException("Monogamy policy violation: An active spouse connection already exists for this person. End the existing connection before creating a new one.")
            }
        }

        // Rule 4: Cycle detection for Parent/Child ancestry
        if (type == GraphRelationType.PARENT || type == GraphRelationType.CHILD) {
            validateAncestryCycles(sourcePersonId, targetPersonId, type, existingRelationships)
        }
    }

    /**
     * Uses Depth-First Search (DFS) to detect circular ancestry loops (e.g. A -> parent -> B -> parent -> C -> parent -> A).
     */
    private fun validateAncestryCycles(
        sourcePersonId: String,
        targetPersonId: String,
        type: GraphRelationType,
        existingRelationships: List<Relationship>
    ) {
        // Build parent adjacency map: Child -> Set<Parent>
        val parentMap = mutableMapOf<String, MutableSet<String>>()

        existingRelationships
            .filter { it.status == RelationshipStatus.ACTIVE }
            .forEach { rel ->
                when (rel.type) {
                    GraphRelationType.PARENT -> {
                        // rel.personAId is parent of rel.personBId (personBId has parent personAId)
                        parentMap.getOrPut(rel.personBId) { mutableSetOf() }.add(rel.personAId)
                    }
                    GraphRelationType.CHILD -> {
                        // rel.personAId is child of rel.personBId (personAId has parent personBId)
                        parentMap.getOrPut(rel.personAId) { mutableSetOf() }.add(rel.personBId)
                    }
                    else -> {}
                }
            }

        // Insert candidate edge
        if (type == GraphRelationType.PARENT) {
            // targetPersonId gets sourcePersonId as parent
            parentMap.getOrPut(targetPersonId) { mutableSetOf() }.add(sourcePersonId)
        } else {
            // sourcePersonId gets targetPersonId as parent
            parentMap.getOrPut(sourcePersonId) { mutableSetOf() }.add(targetPersonId)
        }

        // Run cycle detection across the graph
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()

        fun hasCycle(current: String): Boolean {
            visited.add(current)
            recursionStack.add(current)

            val parents = parentMap[current] ?: emptySet()
            for (parent in parents) {
                if (!visited.contains(parent)) {
                    if (hasCycle(parent)) return true
                } else if (recursionStack.contains(parent)) {
                    return true // Circular ancestry loop detected!
                }
            }

            recursionStack.remove(current)
            return false
        }

        val allNodes = parentMap.keys + parentMap.values.flatten()
        for (node in allNodes) {
            if (!visited.contains(node)) {
                if (hasCycle(node)) {
                    throw GraphValidationException("Circular ancestry loop detected: A person cannot be their own ancestor (Cycle involving person ID $node).")
                }
            }
        }
    }
}
