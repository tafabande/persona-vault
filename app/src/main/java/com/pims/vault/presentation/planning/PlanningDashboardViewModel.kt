package com.pims.vault.presentation.planning

import androidx.lifecycle.ViewModel
import com.pims.vault.core.model.PlanningPhaseState
import com.pims.vault.domain.usecase.planning.GetPlanningPhasesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PlanningDashboardViewModel @Inject constructor(
    getPlanningPhases: GetPlanningPhasesUseCase
) : ViewModel() {
    private val _phases = MutableStateFlow<List<PlanningPhaseState>>(emptyList())
    val phases: StateFlow<List<PlanningPhaseState>> = _phases.asStateFlow()

    init {
        _phases.value = com.pims.vault.core.model.PlanningRegistry.allStates
    }
}
