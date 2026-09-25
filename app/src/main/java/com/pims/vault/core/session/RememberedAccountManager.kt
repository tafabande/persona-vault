package com.pims.vault.core.session

import android.content.Context
import android.content.SharedPreferences
import com.pims.vault.core.auth.AuthUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class RememberedAccount(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String,
    val providerKind: String,
    val rememberedAt: Long = System.currentTimeMillis()
)

@Singleton
class RememberedAccountManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val PREFS_NAME = "pims_remembered_account_prefs"
        const val KEY_REMEMBER_ME_ENABLED = "remember_me_enabled"
        const val KEY_ACCOUNT_UID = "account_uid"
        const val KEY_ACCOUNT_EMAIL = "account_email"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_PHOTO_URL = "photo_url"
        const val KEY_PROVIDER_KIND = "provider_kind"
        const val KEY_REMEMBERED_AT = "remembered_at"
        const val KEY_BOUND_VAULT_UID = "bound_vault_uid"

        fun openPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private val prefs: SharedPreferences = openPrefs(context)

    private val _isRememberMeEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_REMEMBER_ME_ENABLED, true)
    )
    val isRememberMeEnabled: StateFlow<Boolean> = _isRememberMeEnabled.asStateFlow()

    private val _rememberedAccount = MutableStateFlow<RememberedAccount?>(loadRememberedAccount())
    val rememberedAccount: StateFlow<RememberedAccount?> = _rememberedAccount.asStateFlow()

    private val _boundVaultUid = MutableStateFlow<String?>(
        prefs.getString(KEY_BOUND_VAULT_UID, null)
    )
    val boundVaultUid: StateFlow<String?> = _boundVaultUid.asStateFlow()

    fun setRememberMeEnabled(enabled: Boolean) {
        _isRememberMeEnabled.value = enabled
        prefs.edit().putBoolean(KEY_REMEMBER_ME_ENABLED, enabled).apply()
        if (!enabled) {
            forgetAccount()
        }
    }

    fun rememberAccount(user: AuthUser, providerKind: String = "EMAIL_PASSWORD") {
        if (!_isRememberMeEnabled.value) return
        val now = System.currentTimeMillis()
        val account = RememberedAccount(
            uid = user.uid,
            email = user.email ?: "",
            displayName = user.displayName ?: "",
            photoUrl = user.photoUrl ?: "",
            providerKind = providerKind,
            rememberedAt = now
        )
        prefs.edit()
            .putString(KEY_ACCOUNT_UID, account.uid)
            .putString(KEY_ACCOUNT_EMAIL, account.email)
            .putString(KEY_DISPLAY_NAME, account.displayName)
            .putString(KEY_PHOTO_URL, account.photoUrl)
            .putString(KEY_PROVIDER_KIND, account.providerKind)
            .putLong(KEY_REMEMBERED_AT, account.rememberedAt)
            .apply()
        _rememberedAccount.value = account
    }

    fun forgetAccount() {
        prefs.edit()
            .remove(KEY_ACCOUNT_UID)
            .remove(KEY_ACCOUNT_EMAIL)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_PHOTO_URL)
            .remove(KEY_PROVIDER_KIND)
            .remove(KEY_REMEMBERED_AT)
            .apply()
        _rememberedAccount.value = null
    }

    fun bindVaultToAccount(accountUid: String) {
        prefs.edit().putString(KEY_BOUND_VAULT_UID, accountUid).apply()
        _boundVaultUid.value = accountUid
    }

    fun isVaultBoundToDifferentAccount(accountUid: String): Boolean {
        val bound = _boundVaultUid.value
        return bound != null && bound != accountUid
    }

    private fun loadRememberedAccount(): RememberedAccount? {
        val uid = prefs.getString(KEY_ACCOUNT_UID, null) ?: return null
        return RememberedAccount(
            uid = uid,
            email = prefs.getString(KEY_ACCOUNT_EMAIL, "") ?: "",
            displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: "",
            photoUrl = prefs.getString(KEY_PHOTO_URL, "") ?: "",
            providerKind = prefs.getString(KEY_PROVIDER_KIND, "EMAIL_PASSWORD") ?: "EMAIL_PASSWORD",
            rememberedAt = prefs.getLong(KEY_REMEMBERED_AT, System.currentTimeMillis())
        )
    }
}
