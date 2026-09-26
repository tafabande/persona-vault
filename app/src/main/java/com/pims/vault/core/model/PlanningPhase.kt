package com.pims.vault.core.model

/**
 * App mirror of docs/planning/ROADMAP.md — single source of truth for phase gating.
 *
 * Keep in sync with ROADMAP.md and docs/planning/PHASES.md.
 * CI asserts count + names match (see scripts/security-check.sh).
 *
 * Feature flags: isEnabled = true means the phase's UI may be shown.
 * Zone 4 still requires BiometricSessionManager.unlockZone4Vault() regardless of flag.
 */
enum class PlanningPhase(
    val phaseNumber: Int,
    val milestone: String,
    val displayName: String,
    val isEnabled: Boolean
) {
    PLANNING_SYSTEM(0, "Planning System", "Planning System", true),
    M1_CORE_FOUNDATION(1, "M1", "Core Foundation & Security", true),
    M2_DOMAIN_STORAGE(2, "M2", "Domain & Storage", true),
    M3_DESIGN_SYSTEM(3, "M3", "Design System & Core Features", true),
    M4_GRAPH_MEDICAL(4, "M4", "Relationship Graph & Medical Dossier", false),
    M5_SECURITY_VAULT(5, "M5", "Security Vault (Zone 4)", false),
    M6_SHARING_PAIRING(6, "M6", "Selective Sharing & Pairing", false),
    M7_HARDENING(7, "M7", "Hardening & Release", false);

    companion object {
        fun fromNumber(number: Int): PlanningPhase? = entries.find { it.phaseNumber == number }
        fun fromMilestone(milestone: String): PlanningPhase? = entries.find { it.milestone == milestone }
        val enabledPhases: List<PlanningPhase> get() = entries.filter { it.isEnabled }
        val upcomingPhases: List<PlanningPhase> get() = entries.filter { !it.isEnabled }
    }
}

enum class PlanningStatus {
    PLANNED,
    IN_PROGRESS,
    IN_REVIEW,
    DONE
}

data class PlanningPhaseState(
    val phase: PlanningPhase,
    val status: PlanningStatus
)

object PlanningRegistry {
    val allStates: List<PlanningPhaseState> = listOf(
        PlanningPhaseState(PlanningPhase.PLANNING_SYSTEM, PlanningStatus.IN_PROGRESS),
        PlanningPhaseState(PlanningPhase.M1_CORE_FOUNDATION, PlanningStatus.DONE),
        PlanningPhaseState(PlanningPhase.M2_DOMAIN_STORAGE, PlanningStatus.DONE),
        PlanningPhaseState(PlanningPhase.M3_DESIGN_SYSTEM, PlanningStatus.IN_PROGRESS),
        PlanningPhaseState(PlanningPhase.M4_GRAPH_MEDICAL, PlanningStatus.PLANNED),
        PlanningPhaseState(PlanningPhase.M5_SECURITY_VAULT, PlanningStatus.PLANNED),
        PlanningPhaseState(PlanningPhase.M6_SHARING_PAIRING, PlanningStatus.PLANNED),
        PlanningPhaseState(PlanningPhase.M7_HARDENING, PlanningStatus.PLANNED)
    )

    fun isFeatureEnabled(phase: PlanningPhase): Boolean = phase.isEnabled
    fun statusOf(phase: PlanningPhase): PlanningStatus? = allStates.find { it.phase == phase }?.status
}
