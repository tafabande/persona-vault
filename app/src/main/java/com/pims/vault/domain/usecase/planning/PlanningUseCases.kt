package com.pims.vault.domain.usecase.planning

import com.pims.vault.core.model.PlanningPhase
import com.pims.vault.core.model.PlanningRegistry
import com.pims.vault.core.model.PlanningStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetPlanningPhasesUseCase @Inject constructor() {
    operator fun invoke(): Flow<List<com.pims.vault.core.model.PlanningPhaseState>> =
        flowOf(PlanningRegistry.allStates)
}

@Singleton
class IsFeatureEnabledUseCase @Inject constructor() {
    operator fun invoke(phase: PlanningPhase): Boolean =
        PlanningRegistry.isFeatureEnabled(phase)

    fun requireEnabled(phase: PlanningPhase) {
        if (!invoke(phase)) {
            throw IllegalStateException("Feature ${phase.displayName} (${phase.milestone}) is not yet enabled. See docs/planning/ROADMAP.md.")
        }
    }
}

@Singleton
class GetPlanningStatusUseCase @Inject constructor() {
    operator fun invoke(phase: PlanningPhase): PlanningStatus? =
        PlanningRegistry.statusOf(phase)
}
