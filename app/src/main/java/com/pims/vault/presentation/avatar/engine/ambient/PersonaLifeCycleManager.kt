package com.pims.vault.presentation.avatar.engine.ambient

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random

/**
 * Singleton LifeCycle Engine driving autonomous ambient activities across all screens.
 * Decouples character behavioral states from view logic so the avatar lives independently.
 */
class PersonaLifeCycleManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    companion object {
        val instance: PersonaLifeCycleManager by lazy {
            PersonaLifeCycleManager().apply {
                startAmbientLife()
            }
        }
    }

    private val _currentActivity = MutableStateFlow(AmbientActivity.IDLE_LOOKAROUND)
    val currentActivity: StateFlow<AmbientActivity> = _currentActivity.asStateFlow()

    private val _isAutonomousRunning = MutableStateFlow(false)
    val isAutonomousRunning: StateFlow<Boolean> = _isAutonomousRunning.asStateFlow()

    private var routineJob: Job? = null
    private var isManualOverride = false

    fun startAmbientLife() {
        if (_isAutonomousRunning.value) return
        _isAutonomousRunning.value = true
        isManualOverride = false

        routineJob?.cancel()
        routineJob = scope.launch {
            while (isActive) {
                if (!isManualOverride) {
                    val nextActivity = determineNextActivity()
                    _currentActivity.value = nextActivity

                    val durationSec = Random.nextInt(
                        nextActivity.minDurationSec,
                        nextActivity.maxDurationSec + 1
                    )
                    delay(durationSec * 1000L)
                } else {
                    delay(2000L)
                }
            }
        }
    }

    fun stopAmbientLife() {
        routineJob?.cancel()
        routineJob = null
        _isAutonomousRunning.value = false
        _currentActivity.value = AmbientActivity.IDLE_LOOKAROUND
    }

    fun forceActivity(activity: AmbientActivity) {
        isManualOverride = true
        _currentActivity.value = activity
    }

    fun resumeAutonomousSchedule() {
        isManualOverride = false
    }

    /**
     * Weighted probabilistic selection reflecting a natural daily routine.
     */
    private fun determineNextActivity(): AmbientActivity {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // Night hours (22:00 to 05:59): Dynamic avatar activities are disabled at night; character sleeps peacefully
        if (currentHour >= 22 || currentHour < 6) {
            return AmbientActivity.SLEEPING_SNORE
        }

        // Weighted daytime distribution (Idle 38%, Reading 14%, Music 12%, Phone 10%, Bored 8%, Knitting 7%, Stretch 6%, Catnap 5%)
        val roll = Random.nextInt(100)
        return when {
            roll < 38 -> AmbientActivity.IDLE_LOOKAROUND
            roll < 52 -> AmbientActivity.READING_BOOK
            roll < 64 -> AmbientActivity.LISTENING_MUSIC
            roll < 74 -> AmbientActivity.CHECKING_PHONE
            roll < 82 -> AmbientActivity.LOOKING_BORED
            roll < 89 -> AmbientActivity.KNITTING
            roll < 95 -> AmbientActivity.STRETCHING
            else -> AmbientActivity.SLEEPING_SNORE
        }
    }
}
