package com.pims.vault.presentation

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.pims.vault.presentation.ui.util.PimsHaptics
import com.pims.vault.presentation.ui.util.PimsSoundManager
import com.pims.vault.presentation.profile.ProfileUiState
import com.pims.vault.presentation.hub.EditableAddress
import com.pims.vault.presentation.hub.EditableAllergy
import com.pims.vault.presentation.hub.EditableCondition
import com.pims.vault.presentation.hub.EditableMedication
import com.pims.vault.presentation.hub.EditableEducation
import com.pims.vault.presentation.hub.EditableWork
import com.pims.vault.presentation.hub.EditableSocial
import com.pims.vault.presentation.hub.EditableCustomField
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import com.pims.vault.presentation.ui.theme.LocalReducedMotion
import com.pims.vault.presentation.ui.util.screenEnterRise
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import com.pims.vault.presentation.ui.theme.tactilePress
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.pims.vault.presentation.hub.VaultSheet
import com.pims.vault.core.session.AccountMode
import com.pims.vault.core.session.AccountModeManager
import com.pims.vault.presentation.ui.components.ContextualExplanation
import com.pims.vault.presentation.ui.components.ContextualExplanationSheet
import com.pims.vault.presentation.ui.components.PersonaDialog
import com.pims.vault.presentation.ui.components.PersonaDropdownSelector
import com.pims.vault.presentation.ui.components.PersonaSearchableCombobox
import com.pims.vault.presentation.ui.components.PersonaFormSection
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.components.CountrySuggestionField
import com.pims.vault.presentation.ui.components.PersonaToggleRow
import com.pims.vault.presentation.ui.components.ux.PersonaToastController
import com.pims.vault.presentation.ui.components.ux.PersonaToastHost
import com.pims.vault.presentation.ui.components.ux.PersonaToastMessage
import com.pims.vault.presentation.ui.components.ux.ToastType
import com.pims.vault.presentation.ui.components.ux.rememberPersonaToastController
import com.pims.vault.presentation.ui.util.rememberPimsFeedback
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import com.pims.vault.domain.model.PublicResumeData
import com.pims.vault.domain.rules.DocumentRules
import com.pims.vault.core.util.ResumePdfGenerator
import com.pims.vault.presentation.ui.components.InternationalPhoneInput
import com.pims.vault.presentation.hub.PublicResumeModal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pims.vault.core.crypto.KeySecurityLevel
import com.pims.vault.core.model.AllergySeverity
import com.pims.vault.core.model.ConditionStatus
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.domain.model.DocumentWithHistory
import com.pims.vault.presentation.backup.BackupDashboardScreen
import com.pims.vault.presentation.backup.BackupViewModel
import com.pims.vault.presentation.document.DocumentEvent
import com.pims.vault.presentation.document.DocumentViewModel
import com.pims.vault.presentation.document.DocumentUploadSheet
import com.pims.vault.core.model.ContactType
import com.pims.vault.presentation.hub.AddActionBottomSheet
import com.pims.vault.presentation.hub.AddActionType
import com.pims.vault.presentation.hub.EditPersonalIdentitySheet
import com.pims.vault.presentation.hub.EditManagedPersonSheet
import com.pims.vault.presentation.hub.PersonaDashboardDialogs
import com.pims.vault.presentation.hub.DocumentsWalletSheet
import com.pims.vault.presentation.hub.EducationPortfolioSheet
import com.pims.vault.presentation.hub.HealthSheet
import com.pims.vault.presentation.hub.HomeView
import com.pims.vault.presentation.hub.MeView
import com.pims.vault.presentation.hub.MoreView
import com.pims.vault.presentation.hub.UniversalSearchSheet
import com.pims.vault.presentation.hub.StorageManagementSheet
import com.pims.vault.presentation.hub.DataBackupHubSheet
import com.pims.vault.presentation.hub.PeopleView
import com.pims.vault.presentation.hub.PersonDetailSheet
import com.pims.vault.presentation.hub.ConnectPersonScreen
import com.pims.vault.presentation.notes.PlainNotesScreen
import com.pims.vault.presentation.notes.PlainNoteEditorScreen
import com.pims.vault.presentation.avatar.AvatarEditorSheet
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import com.pims.vault.presentation.medical.MedicalDossierView
import com.pims.vault.presentation.medical.MedicalViewModel
import com.pims.vault.presentation.profile.ProfileViewModel
import com.pims.vault.presentation.profile.CustomField
import com.pims.vault.presentation.profile.KinRelationshipItem
import com.pims.vault.presentation.profile.ProfileEvent
import com.pims.vault.presentation.relationship.RelationshipViewModel
import com.pims.vault.presentation.relationship.RelationshipEvent
import com.pims.vault.presentation.sharing.SharingViewModel
import com.pims.vault.core.model.InformationCategory
import com.pims.vault.core.model.InformationSensitivity
import com.pims.vault.core.activity.RecentActivityManager
import com.pims.vault.core.security.PinSecurityManager
import com.pims.vault.presentation.security.SecuritySettingsScreen
import com.pims.vault.presentation.security.PinChallengeDialog
import com.pims.vault.presentation.library.InformationLibraryScreen
import com.pims.vault.presentation.library.LibraryItem
import com.pims.vault.presentation.ui.components.FormFieldLabel
import com.pims.vault.presentation.ui.components.FormTextInput
import com.pims.vault.presentation.ui.theme.StateError
import com.pims.vault.presentation.ui.theme.StateSuccess
import com.pims.vault.presentation.hub.NotificationCategory
import com.pims.vault.presentation.ui.theme.ThemeManager
import com.pims.vault.presentation.ui.theme.ThemeMode
import com.pims.vault.presentation.ui.util.rememberPimsSoundManager
import com.pims.vault.presentation.wallpaper.LocalWallpaperManager
import com.pims.vault.presentation.wallpaper.WallpaperOptionsSheet
import com.pims.vault.presentation.vault.VaultDashboardView
import com.pims.vault.presentation.vault.VaultViewModel
import com.pims.vault.core.sync.SyncState
import com.pims.vault.presentation.hub.AccountDeletionDialog
import com.pims.vault.presentation.hub.NotificationCenterSheet
import com.pims.vault.presentation.hub.PersonaNotificationItem
import com.pims.vault.presentation.hub.SessionManagementSheet
import com.pims.vault.presentation.hub.SyncConflictSheet
import com.pims.vault.presentation.hub.SharePreviewModal
import com.pims.vault.presentation.sync.SyncViewModel
import com.pims.vault.presentation.ui.state.AlertLevel
import com.pims.vault.core.security.SecurityTier
import com.pims.vault.presentation.security.BiometricReauthPrompt
import kotlinx.coroutines.launch
import java.util.Calendar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaDashboardScreen(
    securityLevel: KeySecurityLevel,
    onLockClicked: () -> Unit,
    onSignOutClicked: () -> Unit = onLockClicked,
    onRequestBiometricAuth: ((title: String, subtitle: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit)? = null
) {
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val documentViewModel: DocumentViewModel = hiltViewModel()
    val sharingViewModel: SharingViewModel = hiltViewModel()
    val vaultViewModel: VaultViewModel = hiltViewModel()
    val medicalViewModel: MedicalViewModel = hiltViewModel()
    val backupViewModel: BackupViewModel = hiltViewModel()
    val syncViewModel: SyncViewModel = hiltViewModel()
    val relationshipViewModel: RelationshipViewModel = hiltViewModel()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val toastController = rememberPersonaToastController()
    val feedback = rememberPimsFeedback()

    // Automatically route all notifications/snackbars into Apple-grade floating dynamic status HUD
    LaunchedEffect(snackbarHostState.currentSnackbarData) {
        snackbarHostState.currentSnackbarData?.let { data ->
            val msg = data.visuals.message
            val type = when {
                msg.startsWith("✓") || msg.contains("success", ignoreCase = true) || msg.contains("copied", ignoreCase = true) -> ToastType.SUCCESS
                msg.contains("fail", ignoreCase = true) || msg.contains("error", ignoreCase = true) || msg.contains("locked", ignoreCase = true) -> ToastType.ERROR
                msg.contains("warning", ignoreCase = true) || msg.contains("declined", ignoreCase = true) || msg.contains("wiped", ignoreCase = true) -> ToastType.WARNING
                else -> ToastType.INFO
            }
            toastController.show(
                PersonaToastMessage(
                    message = msg,
                    type = type,
                    actionLabel = data.visuals.actionLabel,
                    onAction = { data.performAction() }
                )
            )
            data.dismiss()
        }
    }

    val profileState by profileViewModel.uiState.collectAsState()
    val documentState by documentViewModel.uiState.collectAsState()
    val vaultState by vaultViewModel.uiState.collectAsState()
    val medicalState by medicalViewModel.uiState.collectAsState()
    val relationshipState by relationshipViewModel.uiState.collectAsState()
    val syncState by syncViewModel.syncState.collectAsState()
    val conflicts by syncViewModel.unresolvedConflicts.collectAsState()
    val activeSessions by syncViewModel.activeSessions.collectAsState()

    val haptics = rememberPimsHaptics()
    val accountModeManager = remember { AccountModeManager(context) }
    val accountMode by accountModeManager.accountMode.collectAsState()
    val authUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
    val isLocalOnly = accountMode == AccountMode.LOCAL_ONLY && authUser == null
    val effectiveSyncStatusText = when {
        syncState is SyncState.Syncing -> "Syncing..."
        isLocalOnly -> "Please sync"
        syncState is SyncState.Idle -> "Synced"
        else -> "Please sync"
    }
    val effectiveSyncIsGood = !isLocalOnly && (syncState is SyncState.Idle || syncState is SyncState.Syncing)

    var contextualExplanation by remember { mutableStateOf<ContextualExplanation?>(null) }

    val syncStatusText = effectiveSyncStatusText
    val syncIsGood = effectiveSyncIsGood

    val themeManager = remember { ThemeManager(context) }
    val themeMode by themeManager.themeMode.collectAsState()
    val soundManager = rememberPimsSoundManager()
    val isSoundEnabled by soundManager.isSoundEnabled.collectAsState()
    val wallpaperManager = remember { LocalWallpaperManager(context) }
    val wallpapers by wallpaperManager.wallpapers.collectAsState()
    val activeWallpaperIndex by wallpaperManager.activeWallpaperIndex.collectAsState()
    val moodManager = remember { com.pims.vault.presentation.ui.theme.PersonaMoodManager(context) }
    val currentMood by moodManager.currentMood.collectAsState()
    val motionManager = remember { com.pims.vault.presentation.ui.theme.PersonaMotionManager(context) }
    val isReducedMotion by motionManager.isReducedMotionPreferred.collectAsState()
    val recentActivityManager = remember { RecentActivityManager(context) }
    val pinSecurityManager = remember { PinSecurityManager(context) }
    val avatarManager = remember { com.pims.vault.presentation.avatar.PersonaAvatarManager.getInstance(context) }
    val customAvatarPath by avatarManager.customAvatarPath.collectAsState()
    val avatarConfig by avatarManager.avatarConfig.collectAsState()
    var isHapticsEnabled by remember { mutableStateOf(haptics.isUserHapticsEnabled()) }
    var dismissedNotificationIds by remember { mutableStateOf(setOf<String>()) }

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val success = wallpaperManager.importFromUri(uri)
                if (success) {
                    recentActivityManager.recordActivity("Added new profile wallpaper")
                    soundManager.success()
                    snackbarHostState.showSnackbar("✓ Wallpaper updated locally")
                }
            }
        }
    }

    // 5 Bottom Navigation Tabs: 0: Home, 1: Notes, 2: Me, 3: People, 4: Settings
    var selectedTab by remember { mutableIntStateOf(0) }

    // Full screen overlays (e.g. Full Vault, Backup, Notes Editor, Connect Person)
    var fullScreenOverlay by remember { mutableStateOf<String?>(null) }
    var activeNoteId by remember { mutableStateOf<String?>(null) }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var sexuality by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (!isLocalOnly) {
            syncViewModel.processSyncNow()
        }
    }

    LaunchedEffect(profileState.person) {
        profileState.person?.let { p ->
            firstName = p.firstName
            lastName = p.lastName
            dob = p.dateOfBirth ?: ""
            nationality = p.nationality ?: ""
            country = p.countryOfResidence ?: ""
            gender = p.gender ?: ""
            p.occupation?.let { if (it.isNotBlank()) occupation = it }
        }
        sexuality = profileState.sexuality
        bloodGroup = profileState.bloodGroup
    }

    // Feedback messages
    LaunchedEffect(profileState.userFeedbackMessage) {
        profileState.userFeedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            profileViewModel.onEvent(ProfileEvent.ClearFeedback)
        }
    }
    LaunchedEffect(documentState.feedbackMessage) {
        documentState.feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            documentViewModel.onEvent(DocumentEvent.ClearFeedback)
        }
    }
    LaunchedEffect(documentState.errorMessage) {
        documentState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar("⚠️ $msg")
            documentViewModel.onEvent(DocumentEvent.ClearFeedback)
        }
    }

    val fullName = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "My Profile" }


    // Bottom Sheets States - Consolidated state management
    val sheetStates = remember {
        mutableStateOf(
            mapOf(
                "addAction" to false,
                "share" to false,
                "education" to false,
                "health" to false,
                "documents" to false,
                "vault" to false,
                "conflict" to false,
                "sessions" to false,
                "notification" to false,
                "accountSecurity" to false,
                "search" to false,
                "storage" to false,
                "dataBackup" to false,
                "wallpaperOptions" to false,
                "uploadDoc" to false,
                "centralizedEditor" to false
            )
        )
    }
    
    fun showSheet(sheetName: String) {
        sheetStates.value = sheetStates.value.toMutableMap().apply { this[sheetName] = true }
    }
    
    fun hideSheet(sheetName: String) {
        sheetStates.value = sheetStates.value.toMutableMap().apply { this[sheetName] = false }
    }

    // Dynamic screenshot protection: enforce FLAG_SECURE exclusively while inside the Vault
    val isVaultActive = fullScreenOverlay == "VAULT" || sheetStates.value["vault"] == true
    DisposableEffect(isVaultActive) {
        val activity = context as? Activity
        if (isVaultActive) {
            activity?.window?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val addActionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shareModalState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val eduSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val healthSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val docsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uploadDocSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val vaultSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val personDetailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val conflictSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sessionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val notificationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val searchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val storageSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dataBackupSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val wallpaperOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val centralizedEditorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val avatarEditorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var selectedPersonForDetail by remember { mutableStateOf<KinRelationshipItem?>(null) }
    var personToEditForDialog by remember { mutableStateOf<KinRelationshipItem?>(null) }
    var showPublicResumeModal by remember { mutableStateOf(false) }

    // Tiered Biometric Verification state for sensitive records (Level 2) and Vault (Level 3)
    var pendingSensitiveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingSecurityTier by remember { mutableStateOf<SecurityTier?>(null) }
    var isBiometricPromptVisible by remember { mutableStateOf(false) }
    var isPinChallengeVisible by remember { mutableStateOf(false) }
    var biometricFailedAttempts by remember { mutableIntStateOf(0) }
    var isLevel2Unlocked by remember { mutableStateOf(false) }

    fun requestGatedAccess(
        tier: SecurityTier,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit
    ) {
        if (tier == SecurityTier.LEVEL_2_SENSITIVE && isLevel2Unlocked) {
            onSuccess()
            return
        }
        pendingSecurityTier = tier
        pendingSensitiveAction = onSuccess

        val biometricAvailable = onRequestBiometricAuth != null && pinSecurityManager.isBiometricEnabled.value
        if (biometricAvailable) {
            onRequestBiometricAuth(
                title,
                subtitle,
                {
                    if (tier == SecurityTier.LEVEL_2_SENSITIVE) {
                        isLevel2Unlocked = true
                    }
                    val action = pendingSensitiveAction
                    pendingSensitiveAction = null
                    pendingSecurityTier = null
                    action?.invoke()
                },
                { error ->
                    // Biometric cancelled or failed -> fallback to PIN challenge if configured
                    if (pinSecurityManager.hasPin()) {
                        isPinChallengeVisible = true
                    } else if (error.contains("BIOMETRICS_UNAVAILABLE", ignoreCase = true) ||
                        error.contains("No fingerprints", ignoreCase = true) ||
                        error.contains("No biometric", ignoreCase = true)) {
                        // Hardware credential unavailable and no app PIN configured: allow graceful access
                        if (tier == SecurityTier.LEVEL_2_SENSITIVE) {
                            isLevel2Unlocked = true
                        }
                        val action = pendingSensitiveAction
                        pendingSensitiveAction = null
                        pendingSecurityTier = null
                        action?.invoke()
                    } else {
                        pendingSensitiveAction = null
                        pendingSecurityTier = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Authentication cancelled: Access locked")
                        }
                    }
                }
            )
        } else if (pinSecurityManager.hasPin()) {
            isPinChallengeVisible = true
        } else {
            // Initial setup or unconfigured security
            if (tier == SecurityTier.LEVEL_2_SENSITIVE) {
                isLevel2Unlocked = true
            }
            pendingSensitiveAction = null
            pendingSecurityTier = null
            onSuccess()
        }
    }

    val openVaultFortress: () -> Unit = {
        if (vaultState.isVaultUnlocked) {
            fullScreenOverlay = "VAULT"
        } else {
            requestGatedAccess(
                SecurityTier.LEVEL_3_VAULT,
                "Unlock Security Vault",
                "Biometric or device credential required for hardware vault unlock"
            ) {
                vaultViewModel.elevateAndUnlockVault(
                    onSuccess = {
                        fullScreenOverlay = "VAULT"
                        recentActivityManager.recordActivity("Unlocked Security Vault", "Zone 4 hardware encryption accessed")
                    },
                    onError = { err ->
                        scope.launch { snackbarHostState.showSnackbar("Vault unlock failed: $err") }
                    }
                )
            }
        }
    }

    // Document Upload & Delete States
    var pendingUploadUri by remember { mutableStateOf<Uri?>(null) }
    var uploadDocTitle by remember { mutableStateOf("") }
    var uploadDocType by remember { mutableStateOf(DocumentType.NATIONAL_ID) }
    var uploadDocNumber by remember { mutableStateOf("") }
    var uploadDocAuthority by remember { mutableStateOf("") }
    var uploadDocExpiryDate by remember { mutableStateOf("") }
    var uploadDocClassification by remember { mutableStateOf(SecurityClassification.ZONE_2_PRIVATE) }
    var uploadDocFileName by remember { mutableStateOf("") }
    var uploadDocSizeBytes by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var uploadDocMimeType by remember { mutableStateOf("application/pdf") }
    var isEncryptingAndSavingDoc by remember { mutableStateOf(false) }
    var pendingUploadBytes by remember { mutableStateOf<ByteArray?>(null) }
    var documentToDelete by remember { mutableStateOf<DocumentWithHistory?>(null) }

    fun queryUploadMetadata(ctx: android.content.Context, uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment?.substringAfterLast("/") ?: "Document"
        var size = 0L
        try {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}
        return Pair(name, size)
    }

    fun guessUploadDocumentTypeAndTitle(fileName: String): Pair<DocumentType, String> {
        val nameWithoutExt = if (fileName.contains(".")) fileName.substringBeforeLast(".") else fileName
        val cleanTitle = nameWithoutExt
            .replace(Regex("[_\\-\\.]+"), " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
            .ifBlank { "Document" }

        val lower = fileName.lowercase()
        val type = when {
            lower.contains("passport") -> DocumentType.PASSPORT
            lower.contains("national") || lower.contains("id_") || lower.contains("id card") || lower.contains("national_id") || lower.contains("citizen") -> DocumentType.NATIONAL_ID
            lower.contains("license") || lower.contains("licence") || lower.contains("driving") || lower.contains("driver") -> DocumentType.DRIVING_LICENCE
            lower.contains("birth") -> DocumentType.BIRTH_CERTIFICATE
            lower.contains("degree") || lower.contains("cert") || lower.contains("diploma") -> DocumentType.ACADEMIC_CERTIFICATE
            lower.contains("transcript") || lower.contains("grades") -> DocumentType.TRANSCRIPT
            lower.contains("resume") || lower.contains("cv") -> DocumentType.CURRICULUM_VITAE
            lower.contains("contract") || lower.contains("offer") || lower.contains("employment") || lower.contains("appointment") -> DocumentType.EMPLOYMENT_CONTRACT
            lower.contains("medical") || lower.contains("health") || lower.contains("doctor") || lower.contains("prescription") || lower.contains("hospital") || lower.contains("vaccine") -> DocumentType.MEDICAL_RECORD
            lower.contains("insurance") || lower.contains("policy") -> DocumentType.INSURANCE_POLICY
            lower.contains("legal") || lower.contains("deed") || lower.contains("agreement") -> DocumentType.LEGAL_CONTRACT
            else -> DocumentType.OTHER
        }
        return Pair(type, cleanTitle)
    }

    fun resolveUploadMimeType(ctx: android.content.Context, uri: Uri, fileName: String): String {
        val resolverType = ctx.contentResolver.getType(uri)
        if (!resolverType.isNullOrBlank() && resolverType != "application/octet-stream") {
            return resolverType
        }
        val ext = fileName.substringAfterLast(".", "").lowercase()
        if (ext.isNotBlank()) {
            val fromMap = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            if (!fromMap.isNullOrBlank()) return fromMap
            return when (ext) {
                "pdf" -> "application/pdf"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "doc" -> "application/msword"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "xls" -> "application/vnd.ms-excel"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "txt" -> "text/plain"
                "csv" -> "text/csv"
                else -> "application/octet-stream"
            }
        }
        return "application/octet-stream"
    }

    fun formatUploadFileSize(bytes: Long): String {
        if (bytes <= 0) return "Ready for encryption"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> "%.1f MB".format(mb)
            kb >= 1.0 -> "%.1f KB".format(kb)
            else -> "$bytes B"
        }
    }

    // Dialog States for Dynamic Multi-Item Entries - Consolidated
    val dialogStates = remember {
        mutableStateOf(
            mapOf(
                "addPhone" to false,
                "addEmail" to false,
                "addAddress" to false,
                "addWork" to false,
                "addEdu" to false,
                "addCert" to false,
                "addAllergy" to false,
                "addCondition" to false,
                "addMedication" to false,
                "addCustomField" to false,
                "addRelationship" to false,
                "editProfile" to false,
                "upload" to false,
                "deleteAccount" to false
            )
        )
    }

    var customFieldInitialLabel by remember { mutableStateOf("") }
    var customFieldCategory by remember { mutableStateOf("Custom Information") }
    
    fun showDialog(dialogName: String) {
        dialogStates.value = dialogStates.value.toMutableMap().apply { this[dialogName] = true }
    }
    
    fun hideDialog(dialogName: String) {
        dialogStates.value = dialogStates.value.toMutableMap().apply { this[dialogName] = false }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val meta = queryUploadMetadata(context, uri)
                val fileName = meta.first
                var size = meta.second

                // Safeguard against unusually large files (> 50 MB threshold)
                if (size > DocumentRules.MAX_DOCUMENT_SIZE_BYTES) {
                    scope.launch {
                        snackbarHostState.showSnackbar("File exceeds the 50 MB maximum allowed threshold.")
                    }
                    return@rememberLauncherForActivityResult
                }

                // Immediately read file into byte buffer and close stream safely
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Selected file is empty or inaccessible.")
                    }
                    return@rememberLauncherForActivityResult
                }

                size = bytes.size.toLong()
                if (size > DocumentRules.MAX_DOCUMENT_SIZE_BYTES) {
                    scope.launch {
                        snackbarHostState.showSnackbar("File exceeds the 50 MB maximum allowed threshold.")
                    }
                    return@rememberLauncherForActivityResult
                }

                val guessed = guessUploadDocumentTypeAndTitle(fileName)
                val mime = resolveUploadMimeType(context, uri, fileName)

                pendingUploadUri = uri
                pendingUploadBytes = bytes
                uploadDocFileName = fileName
                uploadDocSizeBytes = size
                uploadDocMimeType = mime
                uploadDocType = guessed.first
                uploadDocTitle = guessed.second
                uploadDocNumber = ""
                uploadDocAuthority = ""
                uploadDocExpiryDate = ""
                uploadDocClassification = DocumentRules.resolveDefaultClassification(guessed.first)

                showSheet("uploadDoc")
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Could not read selected file: ${e.localizedMessage}")
                }
            }
        }
    }

    var pendingExportDoc by remember { mutableStateOf<com.pims.vault.domain.model.DocumentWithHistory?>(null) }
    val exportDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destinationUri: Uri? ->
        val doc = pendingExportDoc
        val version = doc?.currentVersion
        if (destinationUri != null && version != null) {
            documentViewModel.exportDocument(
                contentResolver = context.contentResolver,
                destinationUri = destinationUri,
                version = version,
                onComplete = {
                    soundManager.success()
                    scope.launch {
                        snackbarHostState.showSnackbar("✓ Exported \"${doc.document.title}\" successfully")
                    }
                },
                onError = { err ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Export failed: $err")
                    }
                }
            )
        }
        pendingExportDoc = null
    }

    val performDocumentDecryptionAndView: (com.pims.vault.domain.model.DocumentWithHistory) -> Unit = { doc ->
        val version = doc.currentVersion
        if (version == null) {
            scope.launch { snackbarHostState.showSnackbar("No version available for this document") }
        } else {
            documentViewModel.decryptForViewing(
                context = context,
                version = version,
                title = doc.document.title,
                onReady = { uri, mimeType ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(intent, "Open Document"))
                    } catch (e: Exception) {
                        scope.launch {
                            snackbarHostState.showSnackbar("No app found to open ${doc.document.title}")
                        }
                    }
                },
                onError = { err ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Decryption error: $err")
                    }
                }
            )
        }
    }

    val onOpenDocumentForViewing: (com.pims.vault.domain.model.DocumentWithHistory) -> Unit = { doc ->
        val isSensitive = (doc.document.securityClassification == SecurityClassification.ZONE_3_SENSITIVE ||
                doc.document.securityClassification == SecurityClassification.ZONE_4_CRITICAL) &&
                pinSecurityManager.requireAuthForSensitive.value &&
                !isLevel2Unlocked

        if (isSensitive && pinSecurityManager.hasPin()) {
            pendingSensitiveAction = { performDocumentDecryptionAndView(doc) }
            pendingSecurityTier = SecurityTier.LEVEL_2_SENSITIVE
            isPinChallengeVisible = true
        } else {
            performDocumentDecryptionAndView(doc)
        }
    }

    val onInitiateDocumentExport: (com.pims.vault.domain.model.DocumentWithHistory) -> Unit = { doc ->
        val isSensitive = (doc.document.securityClassification == SecurityClassification.ZONE_3_SENSITIVE ||
                doc.document.securityClassification == SecurityClassification.ZONE_4_CRITICAL) &&
                pinSecurityManager.requireAuthForSensitive.value &&
                !isLevel2Unlocked

        val launchExport = {
            pendingExportDoc = doc
            val version = doc.currentVersion
            val extension = when {
                version?.mimeType?.contains("pdf", ignoreCase = true) == true -> ".pdf"
                version?.mimeType?.contains("png", ignoreCase = true) == true -> ".png"
                version?.mimeType?.contains("jpeg", ignoreCase = true) == true || version?.mimeType?.contains("jpg", ignoreCase = true) == true -> ".jpg"
                version?.mimeType?.contains("text", ignoreCase = true) == true -> ".txt"
                else -> ""
            }
            val defaultName = "${doc.document.title.replace(Regex("[^a-zA-Z0-9._-]"), "_")}$extension"
            exportDocLauncher.launch(defaultName)
        }

        if (isSensitive && pinSecurityManager.hasPin()) {
            pendingSensitiveAction = launchExport
            pendingSecurityTier = SecurityTier.LEVEL_2_SENSITIVE
            isPinChallengeVisible = true
        } else {
            launchExport()
        }
    }
    
    var relationshipToDelete by remember { mutableStateOf<KinRelationshipItem?>(null) }

    // Primary contact getters
    val primaryPhone = profileState.contacts.firstOrNull { it.contactType == ContactType.PHONE }?.value
    val primaryEmail = profileState.contacts.firstOrNull { it.contactType == ContactType.EMAIL }?.value
    val primaryAddress = profileState.addresses.firstOrNull()?.let { "${it.streetLine1}, ${it.city}" }

    fun handleExportPdf() {
        val resumeData = PublicResumeData.fromProfile(
            person = profileState.person,
            primaryPhone = primaryPhone,
            primaryEmail = primaryEmail,
            primaryAddress = primaryAddress,
            employments = profileState.employmentRecords,
            educations = profileState.educationRecords,
            certificates = profileState.certificates,
            socialAccounts = profileState.socialAccounts.map { entity ->
                com.pims.vault.presentation.ui.components.SocialProfileItem(
                    id = entity.id,
                    platform = com.pims.vault.presentation.ui.components.SocialPlatform.fromName(entity.platform),
                    handleOrUrl = entity.username ?: entity.url,
                    customPlatformName = if (com.pims.vault.presentation.ui.components.SocialPlatform.fromName(entity.platform) == com.pims.vault.presentation.ui.components.SocialPlatform.OTHER) entity.platform else null
                )
            },
            customFields = profileState.customFields
        )
        val pdfFile = ResumePdfGenerator.generateResumePdf(context, resumeData)
        if (pdfFile != null) {
            ResumePdfGenerator.shareResumePdf(context, pdfFile)
            haptics.success()
            soundManager.success()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Failed to generate PDF resume")
            }
        }
    }

    // Activity & Notification Center Data (Strictly relationship, sharing, and urgent profile alerts)
    val notifications = remember(profileState.relationships, documentState.documents, conflicts, dismissedNotificationIds) {
        val list = mutableListOf<PersonaNotificationItem>()

        // 1. Pending Connection Requests & Relationship Updates from actual data
        profileState.relationships.forEach { rel ->
            val notifId = "rel_${rel.id}"
            if (!dismissedNotificationIds.contains(notifId)) {
                val isRequest = rel.relationRole.contains("Request", ignoreCase = true) || rel.notes.contains("Request", ignoreCase = true)
                if (isRequest) {
                    list.add(
                        PersonaNotificationItem(
                            id = notifId,
                            title = "Connection request",
                            description = "${rel.fullName} wants to connect with you as ${rel.relationRole}.",
                            timestampText = "Today",
                            category = NotificationCategory.CONNECTION_REQUEST,
                            senderName = rel.fullName,
                            targetPersonId = rel.targetPersonId,
                            isNeedsAttention = true,
                            isToday = true,
                            isRead = false,
                            level = AlertLevel.INFO,
                            icon = Icons.Default.People
                        )
                    )
                } else if (rel.notes.isNotBlank()) {
                    list.add(
                        PersonaNotificationItem(
                            id = notifId,
                            title = "Connection updated",
                            description = "${rel.fullName} updated details: ${rel.notes}",
                            timestampText = "Recent",
                            category = NotificationCategory.PROFILE_UPDATE,
                            senderName = rel.fullName,
                            targetPersonId = rel.targetPersonId,
                            isNeedsAttention = false,
                            isToday = true,
                            isRead = false,
                            level = AlertLevel.INFO,
                            icon = Icons.Default.Person
                        )
                    )
                }
            }
        }

        // 2. Urgent Sync Conflict (if any cross-device divergence occurs)
        if (conflicts.isNotEmpty() && !dismissedNotificationIds.contains("conflict_alert")) {
            list.add(
                PersonaNotificationItem(
                    id = "conflict_alert",
                    title = "Connection sync conflict",
                    description = "${conflicts.size} shared record versions need conflict resolution.",
                    timestampText = "Action needed",
                    category = NotificationCategory.PROFILE_UPDATE,
                    isNeedsAttention = true,
                    isToday = true,
                    isRead = false,
                    level = AlertLevel.WARNING,
                    icon = Icons.Default.Warning
                )
            )
        }

        // 3. Expiring Documents (Urgent life event)
        val now = System.currentTimeMillis()
        documentState.documents.forEach { docWithHist ->
            val doc = docWithHist.document
            val notifId = "doc_exp_${doc.id}"
            if (!dismissedNotificationIds.contains(notifId)) {
                doc.expirationDate?.let { expStr ->
                    try {
                        val parts = if (expStr.contains("-")) expStr.split("-") else expStr.split("/")
                        if (parts.size == 3) {
                            val expCal = Calendar.getInstance()
                            val (y, m, d) = if (parts[0].length == 4) Triple(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                                            else Triple(parts[2].toInt(), parts[0].toInt() - 1, parts[1].toInt())
                            expCal.set(y, m, d)
                            val diffDays = ((expCal.timeInMillis - now) / (24 * 60 * 60 * 1000)).toInt()
                            if (diffDays in 0..60) {
                                list.add(
                                    PersonaNotificationItem(
                                        id = notifId,
                                        title = "${doc.title} expires soon",
                                        description = "Your ${doc.documentType.name.lowercase().replace('_', ' ')} expires in $diffDays days.",
                                        timestampText = "$diffDays days",
                                        category = NotificationCategory.SHARING_ACTIVITY,
                                        isNeedsAttention = true,
                                        isToday = diffDays <= 7,
                                        isRead = false,
                                        level = AlertLevel.WARNING,
                                        icon = Icons.Default.Warning
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        list
    }

    val needsAttentionItems = remember(notifications) {
        notifications.filter { it.isNeedsAttention }
    }

    // If a full-screen overlay is requested (e.g. Full Vault or Backup), render it
    if (fullScreenOverlay != null) {
        when (fullScreenOverlay) {
            "NOTE_EDITOR" -> {
                PlainNoteEditorScreen(
                    noteId = activeNoteId,
                    onBack = {
                        activeNoteId = null
                        fullScreenOverlay = null
                    }
                )
                return
            }
            "CONNECT_PERSON" -> {
                ConnectPersonScreen(
                    onBack = { fullScreenOverlay = null },
                    onSavePerson = { role, relName, gender, phone, email, address, dob, anniversary, notes, photoPath ->
                        val targetId = java.util.UUID.randomUUID().toString()
                        if (!photoPath.isNullOrBlank()) {
                            avatarManager.setPersonPhotoPath(targetId, photoPath)
                        }
                        profileViewModel.onEvent(
                            ProfileEvent.AddRelationship(
                                role = role,
                                fullName = relName,
                                phone = phone,
                                email = email,
                                address = address,
                                dob = dob,
                                anniversary = anniversary,
                                notes = notes,
                                isNextOfKin = false,
                                customPersonId = targetId,
                                photoPath = photoPath
                            )
                        )
                        recentActivityManager.recordActivity("Connected $relName", "Relationship: $role")
                        toastController.showSuccess("Connected $relName to People")
                        soundManager.success()
                        fullScreenOverlay = null
                    }
                )
                return
            }
            "VAULT" -> {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).screenEnterRise()) {
                    VaultDashboardView(
                        viewModel = vaultViewModel,
                        onRequireBiometricReauth = {
                            requestGatedAccess(
                                SecurityTier.LEVEL_3_VAULT,
                                "Unlock Vault Fortress",
                                "Authentication required for vault access"
                            ) {
                                vaultViewModel.elevateAndUnlockVault(
                                    onError = { err ->
                                        scope.launch { snackbarHostState.showSnackbar("Vault re-authentication failed: $err") }
                                    }
                                )
                            }
                        },
                        onBack = { fullScreenOverlay = null },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                return
            }
            "BACKUP" -> {
                BackHandler(enabled = true) {
                    fullScreenOverlay = null
                }
                Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().screenEnterRise()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { fullScreenOverlay = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to Persona")
                        }
                        Text("Encrypted Backup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    BackupDashboardScreen(
                        viewModel = backupViewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
                return
            }
            "SECURITY_SETTINGS" -> {
                Box(modifier = Modifier.fillMaxSize().statusBarsPadding().screenEnterRise()) {
                    SecuritySettingsScreen(
                        pinSecurityManager = pinSecurityManager,
                        onBack = { fullScreenOverlay = null },
                        onRequestBiometricAuth = onRequestBiometricAuth
                    )
                }
                return
            }
            "HEALTH" -> {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).screenEnterRise()) {
                    MedicalDossierView(
                        uiState = medicalState,
                        onEvent = medicalViewModel::onEvent,
                        onDismiss = { fullScreenOverlay = null },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                return
            }
            "INFORMATION_LIBRARY" -> {
                val libraryItems = remember(profileState, documentState, vaultState, medicalState) {
                    com.pims.vault.presentation.library.LibraryItemBuilder.build(
                        profileState,
                        documentState,
                        vaultState,
                        medicalState
                    )
                }

                Box(modifier = Modifier.fillMaxSize().statusBarsPadding().screenEnterRise()) {
                    InformationLibraryScreen(
                        items = libraryItems,
                        onBack = { fullScreenOverlay = null },
                        onRequestAuthToReveal = { onSuccess ->
                            requestGatedAccess(
                                SecurityTier.LEVEL_2_SENSITIVE,
                                "Protected Information",
                                "Verify identity to reveal this record",
                                onSuccess
                            )
                        }
                    )
                }
                return
            }
        }
    }

    val hasOpenDialog = remember(dialogStates.value) { dialogStates.value.any { it.value } }
    val hasOpenSheet = remember(sheetStates.value) { sheetStates.value.any { it.value } }

    BackHandler(
        enabled = isPinChallengeVisible ||
                personToEditForDialog != null ||
                showPublicResumeModal ||
                selectedPersonForDetail != null ||
                hasOpenDialog ||
                hasOpenSheet ||
                selectedTab != 0
    ) {
        when {
            isPinChallengeVisible -> {
                isPinChallengeVisible = false
                pendingSensitiveAction = null
                pendingSecurityTier = null
            }
            personToEditForDialog != null -> {
                personToEditForDialog = null
            }
            showPublicResumeModal -> {
                showPublicResumeModal = false
            }
            selectedPersonForDetail != null -> {
                selectedPersonForDetail = null
            }
            hasOpenDialog -> {
                dialogStates.value = dialogStates.value.mapValues { false }
            }
            hasOpenSheet -> {
                sheetStates.value = sheetStates.value.mapValues { false }
            }
            selectedTab != 0 -> {
                selectedTab = 0
            }
        }
    }

    // MAIN HUB INTERFACE WITH 4-ITEM BOTTOM NAVIGATION
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { PersonaToastHost(toastController) },
        floatingActionButton = {
            when (selectedTab) {
                1 -> {
                    FloatingActionButton(
                        onClick = {
                            haptics.medium()
                            soundManager.navigation()
                            activeNoteId = null
                            fullScreenOverlay = "NOTE_EDITOR"
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .padding(bottom = 68.dp)
                            .tactilePress()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "New note", modifier = Modifier.size(24.dp))
                    }
                }
                3 -> {
                    FloatingActionButton(
                        onClick = {
                            haptics.medium()
                            soundManager.navigation()
                            fullScreenOverlay = "CONNECT_PERSON"
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .padding(bottom = 68.dp)
                            .tactilePress()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Connect Person", modifier = Modifier.size(24.dp))
                    }
                }
            }
        },
        bottomBar = {
            PersonaDashboardBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        // Top inset is applied per-tab (Home bleeds wallpaper under the status
        // bar; the other tabs pad themselves below it). Bottom inset is handled
        // by PersonaDashboardBottomBar's own navigationBarsPadding() — consuming
        // it here too would double-pad and leave a gap under the nav dock.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                )
        ) {
            val reducedMotion = LocalReducedMotion.current
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(tween(durationMillis = 0)) togetherWith fadeOut(tween(durationMillis = 0))
                    } else {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it / 10 * direction } +
                            fadeIn(spring(stiffness = Spring.StiffnessMediumLow))) togetherWith
                            (slideOutHorizontally(spring(stiffness = Spring.StiffnessMedium)) { -it / 10 * direction } +
                                fadeOut(spring(stiffness = Spring.StiffnessMedium)))
                    }
                },
                label = "TabSwitch"
            ) { tab ->
                when (tab) {
                    // TAB 0: HOME LANDING SCREEN (Personal Identity Dashboard)
                    0 -> HomeView(
                        personName = fullName,
                        occupation = occupation,
                        country = country,
                        primaryPhone = primaryPhone,
                        primaryEmail = primaryEmail,
                        primaryAddress = primaryAddress,
                        nationalIdNumber = profileState.person?.nationalIdNumber,
                        profilePhotoPath = customAvatarPath ?: profileState.idPhotoPath,
                        idPhotoPath = profileState.idPhotoPath,
                        onSaveIdentityDetails = { name, email, phone, idNum, idPhotoUri ->
                            saveIdentityDetailsHelper(context, profileViewModel, haptics, soundManager, name, email, phone, idNum, idPhotoUri)
                        },
                        documents = documentState.documents,
                        syncStatusText = syncStatusText,
                        syncIsGood = syncIsGood,
                        onSyncClick = {
                            haptics.selection()
                            soundManager.navigation()
                            if (conflicts.isNotEmpty()) {
                                showSheet("conflict")
                            } else {
                                syncViewModel.processSyncNow()
                                scope.launch {
                                    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                    if (currentUid != null) {
                                        snackbarHostState.showSnackbar("Syncing Persona Vault...")
                                    } else {
                                        snackbarHostState.showSnackbar("Vault sync initiated")
                                    }
                                }
                            }
                        },
                        isLocalOnly = isLocalOnly,
                        unreadNotificationCount = notifications.count { !it.isRead },
                        wallpapers = wallpapers,
                        activeWallpaperIndex = activeWallpaperIndex,
                        onActiveWallpaperChanged = { wallpaperManager.setActiveIndex(it) },
                        onOpenWallpaperOptions = { showSheet("wallpaperOptions") },
                        onOpenCentralizedEditor = { showSheet("centralizedEditor") },
                        onNotificationClick = {
                            soundManager.navigation()
                            showSheet("notification")
                        },
                        onOpenProfile = {
                            soundManager.navigation()
                            selectedTab = 2
                        },
                        onOpenPeople = {
                            soundManager.navigation()
                            selectedTab = 3
                        },
                        avatarConfig = avatarConfig,
                        onOpenAvatarEditor = { showSheet("avatarEditor") },
                        onOpenDocuments = {
                            soundManager.navigation()
                            showSheet("documents")
                        },
                        onOpenShare = {
                            soundManager.navigation()
                            showSheet("share")
                        },
                        onOpenEmergency = {
                            soundManager.navigation()
                            requestGatedAccess(
                                SecurityTier.LEVEL_2_SENSITIVE,
                                "Emergency Medical",
                                "Confirm identity to access health information"
                            ) {
                                fullScreenOverlay = "HEALTH"
                            }
                        },
                        onOpenCredentials = {
                            soundManager.navigation()
                            openVaultFortress()
                        },
                        onOpenBackup = {
                            soundManager.navigation()
                            showSheet("dataBackup")
                        }
                    )

                    // TAB 1: NOTES HUB
                    1 -> PlainNotesScreen(
                        onOpenEditor = { noteId ->
                            activeNoteId = noteId
                            fullScreenOverlay = "NOTE_EDITOR"
                        }
                    )

                    // TAB 2: ME CENTERPIECE PROFILE
                    2 -> MeView(
                        personName = fullName,
                        occupation = occupation,
                        country = country,
                        primaryPhone = primaryPhone,
                        primaryEmail = primaryEmail,
                        primaryAddress = primaryAddress,
                        educationCount = profileState.educationRecords.size,
                        certCount = profileState.certificates.size,
                        medicalCount = profileState.medicalRecords.size,
                        vaultAccountsCount = vaultState.vaultItems.size,
                        documentsCount = documentState.documents.size,
                        relationshipsCount = profileState.relationships.size,
                        customFields = profileState.customFields,
                        socialAccounts = remember(profileState.socialAccounts) {
                            profileState.socialAccounts.map { entity ->
                                com.pims.vault.presentation.ui.components.SocialProfileItem(
                                    id = entity.id,
                                    platform = com.pims.vault.presentation.ui.components.SocialPlatform.fromName(entity.platform),
                                    handleOrUrl = entity.username ?: entity.url,
                                    customPlatformName = if (com.pims.vault.presentation.ui.components.SocialPlatform.fromName(entity.platform) == com.pims.vault.presentation.ui.components.SocialPlatform.OTHER) entity.platform else null
                                )
                            }
                        },
                        syncStatusText = syncStatusText,
                        syncIsGood = syncIsGood,
                        onSyncClick = {
                            haptics.selection()
                            soundManager.navigation()
                            if (conflicts.isNotEmpty()) {
                                showSheet("conflict")
                            } else {
                                syncViewModel.processSyncNow()
                                scope.launch {
                                    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                    if (currentUid != null) {
                                        snackbarHostState.showSnackbar("Syncing Persona Vault...")
                                    } else {
                                        snackbarHostState.showSnackbar("Vault sync initiated")
                                    }
                                }
                            }
                        },
                        onShareProfileClick = {
                            soundManager.navigation()
                            showSheet("share")
                        },
                        onEditProfileClick = { showDialog("editProfile") },
                        onOpenAvatarEditor = { showSheet("avatarEditor") },
                        onViewPublicDossier = { showPublicResumeModal = true },
                        onExportPdf = { handleExportPdf() },
                        onDeleteCustomField = { label -> profileViewModel.onEvent(ProfileEvent.DeleteCustomField(label)) },
                        onEducationClick = { showSheet("education") },
                        onHealthClick = {
                            requestGatedAccess(
                                SecurityTier.LEVEL_2_SENSITIVE,
                                "Health & Medical",
                                "Confirm identity to access medical records"
                            ) {
                                fullScreenOverlay = "HEALTH"
                            }
                        },
                        onVaultClick = {
                            soundManager.navigation()
                            openVaultFortress()
                        },
                        onDocumentsClick = {
                            soundManager.navigation()
                            showSheet("documents")
                        },
                        onPeopleClick = { selectedTab = 3 },
                        onPhoneClick = { showDialog("addPhone") },
                        onEmailClick = { showDialog("addEmail") },
                        onAddressClick = { showDialog("addAddress") },
                        onAddSocialAccount = { item ->
                            profileViewModel.onEvent(
                                ProfileEvent.AddSocialAccount(
                                    platform = item.platform.displayName,
                                    username = item.handleOrUrl,
                                    url = item.getFullUrl(primaryPhone),
                                    displayName = item.customPlatformName
                                )
                            )
                            recentActivityManager.recordActivity("Added ${item.platform.displayName} profile")
                            soundManager.success()
                        },
                        onRemoveSocialAccount = { id ->
                            profileViewModel.onEvent(ProfileEvent.DeleteSocialAccount(id))
                            recentActivityManager.recordActivity("Removed social profile")
                            soundManager.delete()
                        },
                        onExplain = { contextualExplanation = it }
                    )

                    // TAB 3: PEOPLE & CONNECTIONS
                    3 -> PeopleView(
                        relationships = profileState.relationships,
                        onAddPersonClick = { fullScreenOverlay = "CONNECT_PERSON" },
                        onSelectPerson = { person ->
                            relationshipViewModel.onEvent(RelationshipEvent.LoadNotes(person.id))
                            selectedPersonForDetail = person
                        }
                    )

                    // TAB 4: MORE & SETTINGS
                    4 -> MoreView(
                        userName = fullName,
                        userEmail = primaryEmail ?: "",
                        userPhotoPath = customAvatarPath,
                        isLocalOnly = isLocalOnly,
                        syncStatusText = syncStatusText,
                        currentThemeMode = themeMode,
                        isSoundEnabled = isSoundEnabled,
                        currentMood = currentMood,
                        onMoodSelected = {
                            moodManager.setMood(it)
                            recentActivityManager.recordActivity("Changed profile theme", it.title)
                        },
                        isReducedMotion = isReducedMotion,
                        onToggleReducedMotion = { motionManager.setReducedMotion(it) },
                        isHapticsEnabled = isHapticsEnabled,
                        onToggleHaptics = {
                            haptics.setUserHapticsEnabled(it)
                            isHapticsEnabled = it
                        },
                        onOpenWallpaperOptions = { showSheet("wallpaperOptions") },
                        onThemeModeSelected = { mode ->
                            themeManager.setThemeMode(mode)
                            soundManager.navigation()
                            haptics.selection()
                        },
                        onToggleSound = { enabled ->
                            soundManager.setSoundEnabled(enabled)
                            haptics.selection()
                        },
                        onLockClicked = onLockClicked,
                        onChangePasswordClicked = { showDialog("changePassword") },
                        onSignOutClicked = onSignOutClicked,
                        onSecurityClicked = { fullScreenOverlay = "SECURITY_SETTINGS" },
                        onAppPinSecurityClicked = { fullScreenOverlay = "SECURITY_SETTINGS" },
                        onOpenInformationLibrary = { fullScreenOverlay = "INFORMATION_LIBRARY" },
                        onDevicesClicked = { showSheet("sessions") },
                        onStorageClicked = { showSheet("storage") },
                        onDataBackupClicked = { showSheet("dataBackup") },
                        onEmergencyCardClicked = {
                            requestGatedAccess(
                                SecurityTier.LEVEL_2_SENSITIVE,
                                "Emergency Medical",
                                "Confirm identity to access emergency card"
                            ) {
                                fullScreenOverlay = "HEALTH"
                            }
                        },
                        onDeleteAccountClicked = { showDialog("deleteAccount") },
                        onUpgradeAccount = { email ->
                            accountModeManager.upgradeToCloudAccount(email)
                            soundManager.success()
                            haptics.success()
                            scope.launch { snackbarHostState.showSnackbar("✓ Your data is now synced") }
                        },
                        onRevisitWalkthrough = { accountModeManager.revisitWalkthrough() },
                        onExplain = { contextualExplanation = it }
                    )
                }
            }
        }
    }

    // =========================================================================
    // MODAL BOTTOM SHEETS
    // =========================================================================

    // 1. ADD SOMETHING BOTTOM SHEET
    if (sheetStates.value["addAction"] == true) {
        AddActionBottomSheet(
            sheetState = addActionSheetState,
            onDismissRequest = { hideSheet("addAction") },
            onSelectAction = { actionType ->
                hideSheet("addAction")
                when (actionType) {
                    // Identity
                    AddActionType.PERSONAL_DETAILS -> showDialog("editProfile")
                    AddActionType.IDENTIFICATION_DOC -> filePickerLauncher.launch("*/*")
                    AddActionType.NAMES_HISTORY -> {
                        customFieldInitialLabel = "Previous Name / Alias"
                        customFieldCategory = "Names & History"
                        showDialog("addCustomField")
                    }
                    AddActionType.EMERGENCY_IDENTITY -> {
                        customFieldInitialLabel = "Blood Type / Emergency Note"
                        customFieldCategory = "Emergency Identity"
                        showDialog("addCustomField")
                    }

                    // Contact
                    AddActionType.PHONE -> showDialog("addPhone")
                    AddActionType.EMAIL -> showDialog("addEmail")
                    AddActionType.ADDRESS -> showDialog("addAddress")
                    AddActionType.ONLINE_PRESENCE -> {
                        customFieldInitialLabel = "Website / Social Profile"
                        customFieldCategory = "Online Presence"
                        showDialog("addCustomField")
                    }
                    AddActionType.COMMUNICATION_PREFS -> {
                        customFieldInitialLabel = "Preferred Contact Method"
                        customFieldCategory = "Communication"
                        showDialog("addCustomField")
                    }

                    // Education
                    AddActionType.EDUCATION,
                    AddActionType.QUALIFICATION -> showDialog("addEdu")
                    AddActionType.CERTIFICATION,
                    AddActionType.ACADEMIC_ACHIEVEMENT,
                    AddActionType.COURSES_TRAINING -> showDialog("addCert")
                    AddActionType.SKILLS_LANGUAGES -> {
                        customFieldInitialLabel = "Skill / Spoken Language"
                        customFieldCategory = "Skills & Languages"
                        showDialog("addCustomField")
                    }

                    // Career
                    AddActionType.EXPERIENCE,
                    AddActionType.PROJECTS,
                    AddActionType.VOLUNTEERING -> showDialog("addWork")
                    AddActionType.PORTFOLIO -> {
                        customFieldInitialLabel = "Portfolio / Project Link"
                        customFieldCategory = "Portfolio"
                        showDialog("addCustomField")
                    }
                    AddActionType.AWARDS,
                    AddActionType.PROFESSIONAL_MEMBERSHIPS -> showDialog("addCert")
                    AddActionType.REFERENCES -> showDialog("addRelationship")

                    // Health
                    AddActionType.MEDICAL_HISTORY -> showDialog("addCondition")
                    AddActionType.ALLERGIES -> showDialog("addAllergy")
                    AddActionType.MEDICATIONS -> showDialog("addMedication")
                    AddActionType.EMERGENCY_INFO -> showDialog("addRelationship")
                    AddActionType.DOCTORS_PROVIDERS -> showDialog("addRelationship")
                    AddActionType.HEALTH_DOCUMENTS -> filePickerLauncher.launch("*/*")

                    // Personal
                    AddActionType.INTERESTS_HOBBIES -> {
                        customFieldInitialLabel = "Hobby / Interest"
                        customFieldCategory = "Interests & Hobbies"
                        showDialog("addCustomField")
                    }
                    AddActionType.PREFERENCES -> {
                        customFieldInitialLabel = "Personal Preference"
                        customFieldCategory = "Preferences"
                        showDialog("addCustomField")
                    }
                    AddActionType.PERSONAL_NOTES -> {
                        activeNoteId = null
                        fullScreenOverlay = "NOTE_EDITOR"
                    }
                    AddActionType.GOALS -> {
                        customFieldInitialLabel = "Personal Goal"
                        customFieldCategory = "Goals"
                        showDialog("addCustomField")
                    }
                    AddActionType.IMPORTANT_DATES -> {
                        customFieldInitialLabel = "Important Date"
                        customFieldCategory = "Important Dates"
                        showDialog("addCustomField")
                    }
                    AddActionType.FAVOURITE_THINGS -> {
                        customFieldInitialLabel = "Favourite Thing"
                        customFieldCategory = "Favourite Things"
                        showDialog("addCustomField")
                    }
                    AddActionType.MEMBERSHIPS_AFFILIATIONS -> {
                        customFieldInitialLabel = "Membership / Club"
                        customFieldCategory = "Memberships & Affiliations"
                        showDialog("addCustomField")
                    }

                    // Top-Level / Direct
                    AddActionType.DOCUMENT -> filePickerLauncher.launch("*/*")
                    AddActionType.CUSTOM_FIELD -> {
                        customFieldInitialLabel = ""
                        customFieldCategory = "Custom Information"
                        showDialog("addCustomField")
                    }
                    AddActionType.PERSON -> {
                        fullScreenOverlay = "CONNECT_PERSON"
                    }
                    AddActionType.PASSWORD -> {
                        soundManager.navigation()
                        requestGatedAccess(
                            SecurityTier.LEVEL_3_VAULT,
                            "Unlock Vault",
                            "Authenticate to create password"
                        ) {
                            vaultViewModel.elevateAndUnlockVault(
                                onSuccess = {
                                    vaultViewModel.openEditor(com.pims.vault.core.model.VaultCategory.PASSWORD)
                                    fullScreenOverlay = "VAULT"
                                },
                                onError = { err ->
                                    scope.launch { snackbarHostState.showSnackbar("Vault unlock failed: $err") }
                                }
                            )
                        }
                    }
                    AddActionType.PAYMENT_CARD -> {
                        soundManager.navigation()
                        requestGatedAccess(
                            SecurityTier.LEVEL_3_VAULT,
                            "Unlock Vault",
                            "Authenticate to add payment card"
                        ) {
                            vaultViewModel.elevateAndUnlockVault(
                                onSuccess = {
                                    vaultViewModel.openEditor(com.pims.vault.core.model.VaultCategory.PAYMENT_REFERENCE)
                                    fullScreenOverlay = "VAULT"
                                },
                                onError = { err ->
                                    scope.launch { snackbarHostState.showSnackbar("Vault unlock failed: $err") }
                                }
                            )
                        }
                    }
                    AddActionType.BANK_ACCOUNT -> {
                        soundManager.navigation()
                        requestGatedAccess(
                            SecurityTier.LEVEL_3_VAULT,
                            "Unlock Vault",
                            "Authenticate to add bank account"
                        ) {
                            vaultViewModel.elevateAndUnlockVault(
                                onSuccess = {
                                    vaultViewModel.openEditor(com.pims.vault.core.model.VaultCategory.BANK_ACCOUNT)
                                    fullScreenOverlay = "VAULT"
                                },
                                onError = { err ->
                                    scope.launch { snackbarHostState.showSnackbar("Vault unlock failed: $err") }
                                }
                            )
                        }
                    }
                    AddActionType.SOCIAL_PROFILE -> {
                        customFieldInitialLabel = ""
                        customFieldCategory = "Social Profiles"
                        showDialog("addCustomField")
                    }
                }
            }
        )
    }

    // AVATAR EDITOR SHEET (WhatsApp style with zoom resizability)
    if (sheetStates.value["avatarEditor"] == true) {
        AvatarEditorSheet(
            initialConfig = avatarConfig,
            sheetState = avatarEditorSheetState,
            avatarManager = avatarManager,
            onDismissRequest = { hideSheet("avatarEditor") },
            onSaveAvatar = { newConfig ->
                avatarManager.saveConfig(newConfig)
                hideSheet("avatarEditor")
                toastController.showSuccess("Avatar updated")
            }
        )
    }

    // 2. SHARE IDENTITY & VCARD QR SHEET
    if (sheetStates.value["share"] == true) {
        SharePreviewModal(
            personName = fullName,
            occupation = occupation,
            country = country,
            sheetState = shareModalState,
            phone = primaryPhone ?: "",
            email = primaryEmail ?: "",
            bloodGroup = bloodGroup,
            linkedIn = profileState.socialAccounts.firstOrNull { it.platform.equals("LinkedIn", ignoreCase = true) }?.let { it.username ?: it.url } ?: "",
            profilePhotoPath = customAvatarPath,
            onDismissRequest = { hideSheet("share") },
            onCopyShareLink = { link ->
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(link))
                haptics.selection()
                scope.launch { snackbarHostState.showSnackbar("Copied: $link") }
            }
        )
    }

    // 3. EDUCATION & PORTFOLIO FACET SHEET
    if (sheetStates.value["education"] == true) {
        EducationPortfolioSheet(
            educationList = profileState.educationRecords,
            certificatesList = profileState.certificates,
            employmentList = profileState.employmentRecords,
            sheetState = eduSheetState,
            onDismissRequest = { hideSheet("education") },
            onAddEducation = { showDialog("addEdu") },
            onAddCert = { showDialog("addCert") },
            onAddProject = { showDialog("addWork") }
        )
    }

    // 4. HEALTH FULL SCREEN (Full Medical Dossier)
    if (sheetStates.value["health"] == true) {
        hideSheet("health")
        fullScreenOverlay = "HEALTH"
    }

    // 5. DOCUMENTS WALLET SHEET
    if (sheetStates.value["documents"] == true) {
        DocumentsWalletSheet(
            documents = documentState.documents,
            sheetState = docsSheetState,
            onDismissRequest = { hideSheet("documents") },
            onUploadClick = { filePickerLauncher.launch("*/*") },
            onDeleteClick = { doc -> documentToDelete = doc },
            onViewClick = onOpenDocumentForViewing,
            onExportClick = onInitiateDocumentExport
        )
    }

    // 6. VAULT FORTRESS PREVIEW SHEET
    if (sheetStates.value["vault"] == true) {
        VaultSheet(
            sheetState = vaultSheetState,
            onDismissRequest = { hideSheet("vault") },
            onOpenFullVault = { fullScreenOverlay = "VAULT" }
        )
    }

    // 7. PERSON DETAIL SHEET
    selectedPersonForDetail?.let { person ->
        PersonDetailSheet(
            person = person,
            sheetState = personDetailSheetState,
            onDismissRequest = { selectedPersonForDetail = null },
            onDeletePerson = { p -> relationshipToDelete = p },
            onEditPerson = { p ->
                selectedPersonForDetail = null
                personToEditForDialog = p
            },
            relationshipNotes = relationshipState.relationshipNotes,
            isVaultUnlocked = relationshipState.isVaultUnlocked,
            onAddNote = { topic, content, format, isPrivate ->
                relationshipViewModel.onEvent(
                    RelationshipEvent.SaveNote(
                        relationshipId = person.id,
                        topic = topic,
                        content = content,
                        format = format,
                        isPrivate = isPrivate
                    )
                )
                soundManager.success()
                scope.launch {
                    snackbarHostState.showSnackbar("Note saved to memory dossier")
                }
            },
            onEditNote = { note ->
                relationshipViewModel.onEvent(
                    RelationshipEvent.SaveNote(
                        relationshipId = person.id,
                        topic = note.topic,
                        content = note.content,
                        format = note.format,
                        isPrivate = note.isPrivate,
                        noteId = note.id
                    )
                )
                soundManager.success()
                scope.launch {
                    snackbarHostState.showSnackbar("Note updated")
                }
            },
            onDeleteNote = { note ->
                relationshipViewModel.onEvent(RelationshipEvent.DeleteNote(note.id))
                soundManager.delete()
                scope.launch {
                    snackbarHostState.showSnackbar("Note deleted")
                }
            },
            onToggleNotePrivacy = { noteId, makePrivate ->
                relationshipViewModel.onEvent(RelationshipEvent.ToggleNotePrivacy(noteId, makePrivate))
            },
            onUnlockVaultSession = {
                requestGatedAccess(
                    SecurityTier.LEVEL_3_VAULT,
                    "Unlock Private Memory Notes",
                    "Authenticate to view confidential relationship notes"
                ) {
                    relationshipViewModel.onEvent(RelationshipEvent.UnlockVaultSession)
                }
            }
        )
    }

    // 8. SYNC CONFLICT RESOLUTION SHEET
    if (sheetStates.value["conflict"] == true) {
        SyncConflictSheet(
            conflicts = conflicts,
            sheetState = conflictSheetState,
            onDismissRequest = { hideSheet("conflict") },
            onResolveConflict = { id, choice, resVal ->
                syncViewModel.resolveConflict(id, choice, resVal)
            }
        )
    }

    // 9. ACTIVE SESSIONS MANAGEMENT SHEET
    if (sheetStates.value["sessions"] == true) {
        SessionManagementSheet(
            sessions = activeSessions,
            sheetState = sessionsSheetState,
            onDismissRequest = { hideSheet("sessions") },
            onRevokeSession = { sessionId ->
                syncViewModel.revokeSession(sessionId)
            },
            onRevokeAllOtherSessions = {
                syncViewModel.revokeAllOtherSessions()
            }
        )
    }

    // 10. ACCOUNT DELETION DIALOG
    if (dialogStates.value["deleteAccount"] == true) {
        AccountDeletionDialog(
            documentsCount = documentState.documents.size,
            relationshipsCount = profileState.relationships.size,
            medicalCount = profileState.medicalRecords.size,
            vaultAccountsCount = vaultState.vaultItems.size,
            onDismissRequest = { hideDialog("deleteAccount") },
            onConfirmDelete = {
                hideDialog("deleteAccount")
                scope.launch {
                    snackbarHostState.showSnackbar("Persona account wiped.")
                }
                onLockClicked()
            }
        )
    }



    // 12. NOTIFICATIONS & ACTIVITY SHEET
    if (sheetStates.value["notification"] == true) {
        NotificationCenterSheet(
            notifications = notifications,
            sheetState = notificationSheetState,
            onDismissRequest = { hideSheet("notification") },
            onNotificationClick = { item ->
                hideSheet("notification")
                soundManager.navigation()
                when {
                    item.id.startsWith("rel") -> {
                        selectedTab = 2
                    }
                    item.id.startsWith("conflict") -> showSheet("conflict")
                    item.id.startsWith("doc_exp") -> showSheet("documents")
                }
            },
            onAcceptRequest = { item ->
                dismissedNotificationIds = dismissedNotificationIds + item.id
                soundManager.success()
                haptics.success()
                scope.launch {
                    snackbarHostState.showSnackbar("✓ Connection accepted")
                }
            },
            onDeclineRequest = { item ->
                dismissedNotificationIds = dismissedNotificationIds + item.id
                soundManager.delete()
                haptics.selection()
                scope.launch {
                    snackbarHostState.showSnackbar("Connection request declined")
                }
            },
            onMarkAllRead = {
                dismissedNotificationIds = dismissedNotificationIds + notifications.map { it.id }.toSet()
                soundManager.navigation()
                haptics.selection()
            }
        )
    }

    // WALLPAPER OPTIONS BOTTOM SHEET
    if (sheetStates.value["wallpaperOptions"] == true) {
        WallpaperOptionsSheet(
            sheetState = wallpaperOptionsSheetState,
            wallpapers = wallpapers,
            availablePresets = wallpaperManager.defaultPresets,
            currentIndex = activeWallpaperIndex,
            onSelectWallpaper = { index ->
                wallpaperManager.setActiveIndex(index)
                soundManager.navigation()
                haptics.selection()
            },
            onSelectPreset = { preset ->
                wallpaperManager.selectOrAddPreset(preset)
                soundManager.navigation()
                haptics.selection()
            },
            onPickFromDevice = {
                soundManager.navigation()
                haptics.selection()
                wallpaperPickerLauncher.launch("image/*")
            },
            onRemoveWallpaper = { item ->
                val removed = wallpaperManager.removeWallpaperById(item.id)
                if (removed) {
                    recentActivityManager.recordActivity("Removed profile wallpaper")
                    soundManager.delete()
                    haptics.warning()
                    scope.launch {
                        snackbarHostState.showSnackbar("Wallpaper removed")
                    }
                }
            },
            onRestorePresets = {
                wallpaperManager.restoreDefaultPresets()
                recentActivityManager.recordActivity("Restored default artwork presets")
                soundManager.success()
                haptics.selection()
                scope.launch {
                    snackbarHostState.showSnackbar("Default presets restored")
                }
            },
            onUpdateAlignment = { id, alignY ->
                wallpaperManager.updateWallpaperAlignment(id, alignY)
            },
            onDismiss = { hideSheet("wallpaperOptions") }
        )
    }

    // 15. CENTRALIZED INFORMATION EDITOR BOTTOM SHEET
    if (sheetStates.value["centralizedEditor"] == true) {
        val currentPhones = remember(profileState.contacts) {
            profileState.contacts
                .filter { it.contactType == ContactType.PHONE && it.value.isNotBlank() }
                .distinctBy { it.value.filter { c -> c.isDigit() }.ifBlank { it.value.trim() } }
                .map {
                    com.pims.vault.presentation.hub.EditablePhone(
                        id = it.id,
                        number = it.value,
                        label = it.label,
                        isPrimary = it.isPrimary
                    )
                }
        }
        val currentEmails = remember(profileState.contacts) {
            profileState.contacts
                .filter { it.contactType == ContactType.EMAIL && it.value.isNotBlank() }
                .distinctBy { it.value.trim().lowercase() }
                .map {
                    com.pims.vault.presentation.hub.EditableEmail(
                        id = it.id,
                        address = it.value,
                        label = it.label,
                        isPrimary = it.isPrimary
                    )
                }
        }
        val currentAddresses = remember(profileState.addresses) {
            profileState.addresses
                .distinctBy { "${it.streetLine1}:${it.city}:${it.country}".lowercase() }
                .map {
                    com.pims.vault.presentation.hub.EditableAddress(
                        id = it.id,
                        street1 = it.streetLine1,
                        street2 = it.streetLine2 ?: "",
                        city = it.city,
                        state = it.stateProvince ?: "",
                        postalCode = it.postalCode ?: "",
                        country = it.country,
                        label = it.label.name
                    )
                }
        }

        com.pims.vault.presentation.hub.CentralizedInformationEditorSheet(
            sheetState = centralizedEditorSheetState,
            initialFullName = fullName.takeIf { it != "My Profile" } ?: "",
            initialOccupation = occupation,
            initialCountry = country,
            initialNationalId = profileState.person?.nationalIdNumber,
            initialIdPhotoPath = profileState.idPhotoPath,
            initialPhones = currentPhones,
            initialEmails = currentEmails,
            initialAddresses = currentAddresses,
            onDismissRequest = { hideSheet("centralizedEditor") },
            onSaveAll = { name, occ, ctry, natId, photoUri, phonesList, emailsList, addrsList, allergiesList, conditionsList, medsList, edusList, worksList, socialsList, customList ->
                handleCentralizedInfoSave(
                    context = context,
                    profileViewModel = profileViewModel,
                    profileState = profileState,
                    name = name,
                    occ = occ,
                    ctry = ctry,
                    natId = natId,
                    photoUri = photoUri,
                    phonesList = phonesList,
                    emailsList = emailsList,
                    addrsList = addrsList,
                    allergiesList = allergiesList,
                    conditionsList = conditionsList,
                    medsList = medsList,
                    edusList = edusList,
                    worksList = worksList,
                    socialsList = socialsList,
                    customList = customList,
                    currentDob = dob,
                    currentNationality = nationality,
                    currentGender = gender,
                    currentSexuality = sexuality,
                    currentBloodGroup = bloodGroup,
                    onNameUpdated = { f, l ->
                        firstName = f
                        lastName = l
                    },
                    onOccUpdated = { occupation = it },
                    onCtryUpdated = { country = it },
                    onComplete = {
                        recentActivityManager.recordActivity("Profile Updated", "Centralized profile & records updated")
                        soundManager.success()
                        haptics.success()
                        scope.launch {
                            snackbarHostState.showSnackbar("✓ Personal information updated successfully")
                        }
                        hideSheet("centralizedEditor")
                    }
                )
            }
        )
    }

    // 13. CONTEXTUAL EXPLANATION BOTTOM SHEET (Progressive Disclosure)
    ContextualExplanationSheet(
        explanation = contextualExplanation,
        onDismiss = { contextualExplanation = null }
    )

    // 12. SECURE PIN CHALLENGE DIALOG
    if (isPinChallengeVisible && pendingSecurityTier != null) {
        PinChallengeDialog(
            pinSecurityManager = pinSecurityManager,
            title = when (pendingSecurityTier) {
                SecurityTier.LEVEL_3_VAULT -> "Unlock Vault"
                SecurityTier.LEVEL_2_SENSITIVE -> "Verify Identity"
                else -> "Enter Security PIN"
            },
            subtitle = "Confirm your PIN to access protected records",
            onSuccess = {
                isPinChallengeVisible = false
                if (pendingSecurityTier == SecurityTier.LEVEL_2_SENSITIVE) {
                    isLevel2Unlocked = true
                }
                val action = pendingSensitiveAction
                pendingSensitiveAction = null
                pendingSecurityTier = null
                action?.invoke()
            },
            onDismiss = {
                isPinChallengeVisible = false
                pendingSensitiveAction = null
                pendingSecurityTier = null
                scope.launch {
                    snackbarHostState.showSnackbar("Authentication cancelled: Access locked")
                }
            }
        )
    }

    if (isBiometricPromptVisible && pendingSecurityTier != null) {
        BiometricReauthPrompt(
            tier = pendingSecurityTier!!,
            failedAttempts = biometricFailedAttempts,
            onAuthenticateBiometric = {
                isBiometricPromptVisible = false
                val tier = pendingSecurityTier ?: SecurityTier.LEVEL_2_SENSITIVE
                val action = pendingSensitiveAction ?: {}
                requestGatedAccess(tier, "Verify Identity", "Authentication required", action)
            },
            onUseDevicePin = {
                isBiometricPromptVisible = false
                if (pinSecurityManager.hasPin()) {
                    isPinChallengeVisible = true
                } else {
                    pendingSensitiveAction = null
                    pendingSecurityTier = null
                    scope.launch {
                        snackbarHostState.showSnackbar("No PIN configured. Setup PIN in Security Settings.")
                    }
                }
            },
            onDismissRequest = {
                isBiometricPromptVisible = false
                pendingSensitiveAction = null
                pendingSecurityTier = null
                scope.launch {
                    snackbarHostState.showSnackbar("Authentication cancelled: Access locked")
                }
            }
        )
    }

    // 13. UNIVERSAL OMNIBAR SEARCH SHEET
    if (sheetStates.value["search"] == true) {
        UniversalSearchSheet(
            sheetState = searchSheetState,
            profileState = profileState,
            documents = documentState.documents,
            medicalState = medicalState,
            vaultState = vaultState,
            onSelectPerson = { person ->
                hideSheet("search")
                selectedPersonForDetail = person
            },
            onSelectDocument = { doc ->
                hideSheet("search")
                showSheet("documents")
            },
            onOpenHealth = {
                hideSheet("search")
                requestGatedAccess(
                    SecurityTier.LEVEL_2_SENSITIVE,
                    "Health & Medical",
                    "Confirm identity to access health details"
                ) {
                    fullScreenOverlay = "HEALTH"
                }
            },
            onOpenVault = {
                hideSheet("search")
                openVaultFortress()
            },
            onDismissRequest = { hideSheet("search") }
        )
    }

    // 14. STORAGE MANAGEMENT SHEET (3-Layer Progressive Disclosure)
    if (sheetStates.value["storage"] == true) {
        StorageManagementSheet(
            sheetState = storageSheetState,
            totalDocumentsCount = documentState.documents.size,
            onDismissRequest = { hideSheet("storage") }
        )
    }

    // 15. DATA & BACKUP HUB SHEET
    if (sheetStates.value["dataBackup"] == true) {
        DataBackupHubSheet(
            sheetState = dataBackupSheetState,
            syncIsGood = syncIsGood,
            syncStatusText = syncStatusText,
            onExportBackup = {
                hideSheet("dataBackup")
                fullScreenOverlay = "BACKUP"
            },
            onRestoreBackup = {
                hideSheet("dataBackup")
                fullScreenOverlay = "BACKUP"
            },
            onOpenSyncConflicts = {
                hideSheet("dataBackup")
                showSheet("conflict")
            },
            onDismissRequest = { hideSheet("dataBackup") }
        )
    }

    // =========================================================================
    // EDIT PERSONAL IDENTITY BOTTOM SHEET (extracted to EditPersonalIdentitySheet)
    // =========================================================================
    EditPersonalIdentitySheet(
        isVisible = dialogStates.value["editProfile"] == true,
        firstName = firstName,
        lastName = lastName,
        primaryPhone = primaryPhone,
        primaryEmail = primaryEmail,
        dob = dob,
        nationality = nationality,
        country = country,
        gender = gender,
        occupation = occupation,
        sexuality = sexuality,
        bloodGroup = bloodGroup,
        profileViewModel = profileViewModel,
        recentActivityManager = recentActivityManager,
        haptics = haptics,
        soundManager = soundManager,
        toastController = toastController,
        onSaved = { first, last, dobVal, occ, countryVal, genderVal, nat ->
            firstName = first
            lastName = last
            dob = dobVal
            occupation = occ
            country = countryVal
            gender = genderVal
            nationality = nat
            profileViewModel.onEvent(
                ProfileEvent.SavePersonalDetails(
                    firstName = first, lastName = last, dob = dobVal,
                    nationality = nat, country = countryVal, gender = genderVal,
                    sexuality = sexuality, bloodGroup = bloodGroup
                )
            )
            profileViewModel.savePrimaryPhone(primaryPhone ?: "")
            profileViewModel.savePrimaryEmail(primaryEmail ?: "")
            recentActivityManager.recordActivity("Updated personal information", "Profile name & details updated")
            haptics.success()
            soundManager.success()
            toastController.showSuccess("Personal identity saved")
            hideDialog("editProfile")
        },
        onDismissRequest = { hideDialog("editProfile") }
    )

    // Edit Managed Person Dialog (extracted to EditManagedPersonSheet)
    EditManagedPersonSheet(
        person = personToEditForDialog,
        relationshipState = relationshipState,
        profileViewModel = profileViewModel,
        recentActivityManager = recentActivityManager,
        soundManager = soundManager,
        haptics = haptics,
        toastController = toastController,
        onDismissRequest = { personToEditForDialog = null }
    )

    // =========================================================================
    // CRUD DIALOGS FOR PERSONAL ITEMS (extracted to PersonaDashboardDialogs)
    // =========================================================================
    PersonaDashboardDialogs(
        dialogStates = dialogStates.value,
        onHideDialog = { hideDialog(it) },
        country = country,
        customFieldInitialLabel = customFieldInitialLabel,
        customFieldCategory = customFieldCategory,
        profileState = profileState,
        profileViewModel = profileViewModel,
        documentViewModel = documentViewModel,
        relationshipToDelete = relationshipToDelete,
        onDismissRelationshipToDelete = { relationshipToDelete = null },
        onConfirmDeleteRelationship = { rel ->
            profileViewModel.onEvent(ProfileEvent.DeleteRelationship(rel.id, rel.targetPersonId))
            recentActivityManager.recordActivity("Removed person from People", "Disconnected ${rel.fullName}")
            relationshipToDelete = null
            selectedPersonForDetail = null
        },
        documentToDelete = documentToDelete,
        onDismissDocumentToDelete = { documentToDelete = null },
        onConfirmDeleteDocument = { doc ->
            documentViewModel.onEvent(DocumentEvent.DeleteDocument(doc.document.id))
            recentActivityManager.recordActivity("Removed document from wallet", doc.document.title)
            documentToDelete = null
        },
        recentActivityManager = recentActivityManager,
        soundManager = soundManager,
        haptics = haptics
    )


    // 11. Document Upload Bottom Sheet (Section 11)
    if (sheetStates.value["uploadDoc"] == true && pendingUploadUri != null && pendingUploadBytes != null) {
        DocumentUploadSheet(
            sheetState = uploadDocSheetState,
            fileName = uploadDocFileName,
            fileSizeBytes = uploadDocSizeBytes,
            mimeType = uploadDocMimeType,
            title = uploadDocTitle,
            onTitleChange = { uploadDocTitle = it },
            documentType = uploadDocType,
            onDocumentTypeChange = { uploadDocType = it },
            docNumber = uploadDocNumber,
            onDocNumberChange = { uploadDocNumber = it },
            authority = uploadDocAuthority,
            onAuthorityChange = { uploadDocAuthority = it },
            expiryDate = uploadDocExpiryDate,
            onExpiryDateChange = { uploadDocExpiryDate = it },
            classification = uploadDocClassification,
            onClassificationChange = { uploadDocClassification = it },
            isProcessing = isEncryptingAndSavingDoc,
            onChangeFileClick = { filePickerLauncher.launch("*/*") },
            onDismissRequest = {
                if (!isEncryptingAndSavingDoc) {
                    hideSheet("uploadDoc")
                    pendingUploadBytes = null
                    pendingUploadUri = null
                }
            },
            onConfirmSave = {
                val bytes = pendingUploadBytes
                if (bytes != null && uploadDocTitle.isNotBlank()) {
                    isEncryptingAndSavingDoc = true
                    documentViewModel.onEvent(
                        DocumentEvent.IngestDocument(
                            type = uploadDocType,
                            title = uploadDocTitle.trim(),
                            docNumber = uploadDocNumber.trim().takeIf { it.isNotBlank() },
                            authority = uploadDocAuthority.trim().takeIf { it.isNotBlank() },
                            country = country.ifBlank { "ZW" },
                            issueDate = null,
                            expiryDate = uploadDocExpiryDate.trim().takeIf { it.isNotBlank() },
                            classification = uploadDocClassification,
                            fileBytes = bytes,
                            mimeType = uploadDocMimeType,
                            filename = uploadDocFileName,
                            onSuccess = {
                                recentActivityManager.recordActivity("Added document to wallet", uploadDocTitle.trim())
                                soundManager.success()
                                haptics.success()
                                isEncryptingAndSavingDoc = false
                                pendingUploadBytes = null
                                pendingUploadUri = null
                                hideSheet("uploadDoc")
                            },
                            onError = { _ ->
                                isEncryptingAndSavingDoc = false
                            }
                        )
                    )
                }
            }
        )
    }

    // 12. Add Custom Information Dialog
    if (dialogStates.value["addCustomField"] == true) {
        var customLabel by remember(customFieldInitialLabel) { mutableStateOf(customFieldInitialLabel) }
        var customVal by remember { mutableStateOf("") }
        var selectedCategory by remember {
            mutableStateOf(InformationCategory.fromName(customFieldCategory))
        }
        var categoryDropdownExpanded by remember { mutableStateOf(false) }

        val recognizedField = remember(customLabel) {
            com.pims.vault.core.domain.SemanticFieldRecognizer.resolve(customLabel)
        }
        LaunchedEffect(recognizedField) {
            if (recognizedField != null) {
                selectedCategory = recognizedField.category
            }
        }

        PersonaDialog(
            onDismissRequest = { hideDialog("addCustomField") },
            title = "Add Custom Information",
            confirmText = "Save Field",
            confirmEnabled = customLabel.isNotBlank() && customVal.isNotBlank(),
            onConfirm = {
                if (customLabel.isNotBlank() && customVal.isNotBlank()) {
                    profileViewModel.onEvent(
                        ProfileEvent.AddCustomField(
                            label = customLabel.trim(),
                            value = customVal.trim(),
                            category = selectedCategory
                        )
                    )
                    recentActivityManager.recordActivity("Added custom information item", "${selectedCategory.displayName}: ${customLabel.trim()}")
                    soundManager.success()
                    haptics.success()
                }
                hideDialog("addCustomField")
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Category Picker
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Category / Type",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory.displayName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false },
                            modifier = Modifier
                                .exposedDropdownSize(matchTextFieldWidth = true)
                                .heightIn(max = 240.dp)
                        ) {
                            InformationCategory.values().forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.displayName) },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryDropdownExpanded = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                )
                            }
                        }
                    }
                }

                PersonaTextInput(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = "Field name / identifier",
                    placeholder = "e.g. Blood Type, Discord, Student ID, Secondary Email"
                )

                if (recognizedField != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "💡 Recognized: ${recognizedField.canonicalName} (${recognizedField.category.displayName})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                PersonaTextInput(
                    value = customVal,
                    onValueChange = { customVal = it },
                    label = "Value / detail",
                    placeholder = "Enter the information here"
                )
            }
        }
    }

    // 13. Add Medical Condition Dialog
    if (dialogStates.value["addCondition"] == true) {
        var conditionName by remember { mutableStateOf("") }
        var conditionNotes by remember { mutableStateOf("") }
        var conditionStatus by remember { mutableStateOf(ConditionStatus.ACTIVE) }

        val statusOptions = listOf("Active", "Managed", "Resolved")

        PersonaDialog(
            onDismissRequest = { hideDialog("addCondition") },
            title = "Add Medical Condition",
            confirmText = "Save Condition",
            confirmEnabled = conditionName.isNotBlank(),
            onConfirm = {
                if (conditionName.isNotBlank()) {
                    profileViewModel.onEvent(
                        ProfileEvent.AddMedicalCondition(
                            condition = conditionName.trim(),
                            status = conditionStatus,
                            notes = conditionNotes.trim().ifBlank { null }
                        )
                    )
                    soundManager.success()
                    haptics.success()
                }
                hideDialog("addCondition")
            }
        ) {
            PersonaTextInput(
                value = conditionName,
                onValueChange = { conditionName = it },
                label = "Condition / diagnosis",
                placeholder = "e.g. Asthma, Hypertension"
            )

            PersonaDropdownSelector(
                selectedOption = when (conditionStatus) {
                    ConditionStatus.ACTIVE -> "Active"
                    ConditionStatus.CHRONIC -> "Managed"
                    ConditionStatus.RESOLVED -> "Resolved"
                    else -> "Active"
                },
                options = statusOptions,
                onOptionSelected = { selected ->
                    conditionStatus = when (selected) {
                        "Managed" -> ConditionStatus.CHRONIC
                        "Resolved" -> ConditionStatus.RESOLVED
                        else -> ConditionStatus.ACTIVE
                    }
                },
                label = "Status"
            )

            PersonaTextInput(
                value = conditionNotes,
                onValueChange = { conditionNotes = it },
                label = "Notes / treatment (Optional)",
                placeholder = "Diagnosed year, specialist, care plan",
                singleLine = false
            )
        }
    }

    // 14. Add Medication Dialog
    if (dialogStates.value["addMedication"] == true) {
        var medName by remember { mutableStateOf("") }
        var dosage by remember { mutableStateOf("") }
        var frequency by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        PersonaDialog(
            onDismissRequest = { hideDialog("addMedication") },
            title = "Add Medication",
            confirmText = "Save Medication",
            confirmEnabled = medName.isNotBlank(),
            onConfirm = {
                if (medName.isNotBlank()) {
                    profileViewModel.onEvent(
                        ProfileEvent.AddMedication(
                            name = medName.trim(),
                            dosage = dosage.trim(),
                            frequency = frequency.trim(),
                            notes = notes.trim().ifBlank { null }
                        )
                    )
                    soundManager.success()
                    haptics.success()
                }
                hideDialog("addMedication")
            }
        ) {
            PersonaTextInput(
                value = medName,
                onValueChange = { medName = it },
                label = "Medication name",
                placeholder = "e.g. Ventolin, Metformin"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PersonaTextInput(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = "Dosage",
                    placeholder = "e.g. 100mcg",
                    modifier = Modifier.weight(1f)
                )
                PersonaTextInput(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = "Frequency",
                    placeholder = "e.g. Twice daily",
                    modifier = Modifier.weight(1f)
                )
            }

            PersonaTextInput(
                value = notes,
                onValueChange = { notes = it },
                label = "Prescription details / Notes (Optional)",
                placeholder = "Doctor, refill date, special instructions",
                singleLine = false
            )
        }
    }

    // 14. Unified Public Dossier & Executive Resume Modal
    if (showPublicResumeModal) {
        val resumeData = remember(profileState, primaryPhone, primaryEmail, primaryAddress) {
            PublicResumeData.fromProfile(
                person = profileState.person,
                primaryPhone = primaryPhone,
                primaryEmail = primaryEmail,
                primaryAddress = primaryAddress,
                employments = profileState.employmentRecords,
                educations = profileState.educationRecords,
                certificates = profileState.certificates,
                socialAccounts = profileState.socialAccounts.map { entity ->
                    com.pims.vault.presentation.ui.components.SocialProfileItem(
                        id = entity.id,
                        platform = com.pims.vault.presentation.ui.components.SocialPlatform.fromName(entity.platform),
                        handleOrUrl = entity.username ?: entity.url,
                        customPlatformName = if (com.pims.vault.presentation.ui.components.SocialPlatform.fromName(entity.platform) == com.pims.vault.presentation.ui.components.SocialPlatform.OTHER) entity.platform else null
                    )
                },
                customFields = profileState.customFields
            )
        }
        PublicResumeModal(
            data = resumeData,
            onDismissRequest = { showPublicResumeModal = false },
            onExportPdf = { handleExportPdf() },
            onEditProfile = {
                showSheet("centralizedEditor")
            }
        )
    }
}
