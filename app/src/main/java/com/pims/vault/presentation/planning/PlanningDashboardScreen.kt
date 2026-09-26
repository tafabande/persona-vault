package com.pims.vault.presentation.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pims.vault.BuildConfig
import com.pims.vault.core.model.PlanningPhaseState
import com.pims.vault.core.model.PlanningStatus

@Composable
fun PlanningDashboardScreen(
    viewModel: PlanningDashboardViewModel = hiltViewModel()
) {
    if (!BuildConfig.DEBUG) {
        Text("Planning dashboard is available in debug builds only.", modifier = Modifier.padding(16.dp))
        return
    }
    val states by viewModel.phases.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Planning Phases", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Mirror of docs/planning/ROADMAP.md — debug only",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(states) { state ->
            PlanningPhaseCard(state)
        }
    }
}

@Composable
private fun PlanningPhaseCard(state: PlanningPhaseState) {
    val containerColor = when (state.status) {
        PlanningStatus.DONE -> MaterialTheme.colorScheme.primaryContainer
        PlanningStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondaryContainer
        PlanningStatus.IN_REVIEW -> MaterialTheme.colorScheme.tertiaryContainer
        PlanningStatus.PLANNED -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(state.phase.displayName, style = MaterialTheme.typography.titleMedium)
                Text(state.status.name, style = MaterialTheme.typography.labelMedium)
            }
            Text(
                "${state.phase.milestone} · Phase ${state.phase.phaseNumber} · ${if (state.phase.isEnabled) "Enabled" else "Gated"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
