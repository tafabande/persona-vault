package com.pims.vault.core.session

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AccountMode {
    UNSET,         // First launch: show Welcome choice
    LOCAL_ONLY,    // Offline-first: local encrypted vault, quiet UI
    CLOUD_SYNCED   // Authenticated: cloud backup & sync enabled
}

@Singleton
class AccountModeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("pims_account_mode_prefs", Context.MODE_PRIVATE)

    private val _accountMode = MutableStateFlow(loadInitialMode())
    val accountMode: StateFlow<AccountMode> = _accountMode.asStateFlow()

    private val _accountEmail = MutableStateFlow(prefs.getString("account_email", null))
    val accountEmail: StateFlow<String?> = _accountEmail.asStateFlow()

    private val _hasCompletedWalkthrough = MutableStateFlow(prefs.getBoolean(KEY_WALKTHROUGH_COMPLETED, false))
    val hasCompletedWalkthrough: StateFlow<Boolean> = _hasCompletedWalkthrough.asStateFlow()

    private val _hasCompletedInitialProfile = MutableStateFlow(prefs.getBoolean(KEY_PROFILE_SETUP_COMPLETED, false))
    val hasCompletedInitialProfile: StateFlow<Boolean> = _hasCompletedInitialProfile.asStateFlow()

    private val _isRevisitingWalkthrough = MutableStateFlow(false)
    val isRevisitingWalkthrough: StateFlow<Boolean> = _isRevisitingWalkthrough.asStateFlow()

    private fun loadInitialMode(): AccountMode {
        val saved = prefs.getString(KEY_MODE, null) ?: return AccountMode.UNSET
        return try {
            AccountMode.valueOf(saved)
        } catch (_: Exception) {
            AccountMode.UNSET
        }
    }

    fun completeWalkthrough() {
        prefs.edit()
            .putBoolean(KEY_WALKTHROUGH_COMPLETED, true)
            .apply()
        _hasCompletedWalkthrough.value = true
        _isRevisitingWalkthrough.value = false
    }

    fun revisitWalkthrough() {
        _isRevisitingWalkthrough.value = true
    }

    fun closeRevisitWalkthrough() {
        _isRevisitingWalkthrough.value = false
    }

    fun setLocalOnlyMode() {
        prefs.edit()
            .putString(KEY_MODE, AccountMode.LOCAL_ONLY.name)
            .apply()
        _accountMode.value = AccountMode.LOCAL_ONLY
    }

    fun upgradeToCloudAccount(email: String) {
        prefs.edit()
            .putString(KEY_MODE, AccountMode.CLOUD_SYNCED.name)
            .putString("account_email", email)
            .apply()
        _accountEmail.value = email
        _accountMode.value = AccountMode.CLOUD_SYNCED
    }

    fun completeInitialProfile() {
        prefs.edit()
            .putBoolean(KEY_PROFILE_SETUP_COMPLETED, true)
            .apply()
        _hasCompletedInitialProfile.value = true
    }

    fun isLocalOnly(): Boolean = _accountMode.value == AccountMode.LOCAL_ONLY

    fun isCloudSynced(): Boolean = _accountMode.value == AccountMode.CLOUD_SYNCED

    companion object {
        private const val KEY_MODE = "account_mode"
        private const val KEY_WALKTHROUGH_COMPLETED = "walkthrough_completed"
        private const val KEY_PROFILE_SETUP_COMPLETED = "initial_profile_setup_completed"
    }
}
