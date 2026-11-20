package com.pims.vault.core.sync

import com.pims.vault.core.auth.AuthenticationService
import com.pims.vault.core.session.AccountMode
import com.pims.vault.core.session.AccountModeManager
import com.pims.vault.core.session.RememberedAccountManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/** Background backup may use only the account explicitly bound to this local vault. */
@Singleton
class CloudSyncAccess @Inject constructor(
    private val authentication: AuthenticationService,
    private val accountMode: AccountModeManager,
    private val rememberedAccount: RememberedAccountManager
) {
    val authorizedAccount = combine(
        authentication.authState,
        accountMode.accountMode,
        rememberedAccount.boundVaultUid
    ) { account, mode, binding ->
        account?.uid?.takeIf { mode == AccountMode.CLOUD_SYNCED && it == binding }
    }.distinctUntilChanged()

    fun currentAccount(): String? = authentication.currentUser?.uid?.takeIf { uid ->
        accountMode.isCloudSynced() && uid == rememberedAccount.boundVaultUid.value
    }

    fun boundAccount(): String? = rememberedAccount.boundVaultUid.value
}
