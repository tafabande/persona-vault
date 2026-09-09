package com.pims.vault.presentation.security

import androidx.lifecycle.ViewModel
import com.pims.vault.core.security.AccountSecurityManager
import com.pims.vault.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AccountSecurityViewModel @Inject constructor(
    val securityManager: AccountSecurityManager,
    val sessionManager: SessionManager
) : ViewModel()
