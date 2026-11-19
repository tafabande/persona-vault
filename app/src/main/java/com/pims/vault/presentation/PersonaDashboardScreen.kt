package com.pims.vault.presentation

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import kotlin.math.roundToInt
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import com.pims.vault.presentation.ui.theme.LocalReducedMotion
import com.pims.vault.presentation.ui.theme.PimsAnimation
import com.pims.vault.presentation.ui.util.screenEnterRise
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.material3.CircularProgressIndicator
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
import com.pims.vault.presentation.hub.PersonDetailScreen
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
import com.pims.vault.presentation.ui.theme.PimsColors.error as StateError
import com.pims.vault.presentation.ui.theme.PimsColors.success as StateSuccess
import com.pims.vault.presentation.hub.NotificationCategory
import com.pims.vault.presentation.ui.theme.ThemeManager
import com.pims.vault.presentation.ui.theme.ThemeMode
import com.pims.vault.presentation.ui.util.rememberPimsSoundManager
import com.pims.vault.presentation.wallpaper.LocalWallpaperManager
import com.pims.vault.presentation.wallpaper.WallpaperOptionsSheet
import com.pims.vault.presentation.wallpaper.WallpaperSurface
import com.pims.vault.presentation.wallpaper.PersonaMediaReferenceResolver
import androidx.activity.result.PickVisualMediaRequest
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


// Keep remembered workflow state grouped so this dashboard's generated DEX frame stays bounded.
private class DashboardNavigationState(initialOpenNoteId: String?) {
    var selectedTab by mutableIntStateOf(0)
    var fullScreenOverlay by mutableStateOf<String?>(null)
    var activeNoteId by mutableStateOf<String?>(initialOpenNoteId)
    var selectedPersonForDetail by mutableStateOf<KinRelationshipItem?>(null)
    var personToEditForDialog by mutableStateOf<KinRelationshipItem?>(null)
    var showPublicResumeModal by mutableStateOf(false)
    var relationshipToDelete by mutableStateOf<KinRelationshipItem?>(null)
}

private class DashboardDocumentState {
    var pendingUploadUri by mutableStateOf<Uri?>(null)
    var uploadDocTitle by mutableStateOf("")
    var uploadDocType by mutableStateOf(DocumentType.NATIONAL_ID)
    var uploadDocNumber by mutableStateOf("")
    var uploadDocAuthority by mutableStateOf("")
    var uploadDocExpiryDate by mutableStateOf("")
    var uploadDocClassification by mutableStateOf(SecurityClassification.ZONE_2_PRIVATE)
    var uploadDocFileName by mutableStateOf("")
    var uploadDocSizeBytes by androidx.compose.runtime.mutableLongStateOf(0L)
    var uploadDocMimeType by mutableStateOf("application/pdf")
    var isEncryptingAndSavingDoc by mutableStateOf(false)
    var uploadDocImportId by mutableStateOf(java.util.UUID.randomUUID().toString())
    var uploadDocError by mutableStateOf<String?>(null)
    var versionUploadTarget by mutableStateOf<DocumentWithHistory?>(null)
    var documentToDelete by mutableStateOf<DocumentWithHistory?>(null)
    var pendingExportDoc by mutableStateOf<DocumentWithHistory?>(null)
}

private class DashboardAccessState {
    var pendingSensitiveAction by mutableStateOf<(() -> Unit)?>(null)
    var pendingSecurityTier by mutableStateOf<SecurityTier?>(null)
    var isBiometricPromptVisible by mutableStateOf(false)
    var isPinChallengeVisible by mutableStateOf(false)
    var biometricFailedAttempts by mutableIntStateOf(0)
    var isLevel2Unlocked by mutableStateOf(false)
    var zone4CryptoObject by mutableStateOf<androidx.biometric.BiometricPrompt.CryptoObject?>(null)
}

private class DashboardCustomFieldState {
    var customFieldInitialLabel by mutableStateOf("")
    var customFieldEditingId by mutableStateOf<String?>(null)
    var customFieldInitialValue by mutableStateOf("")
    var customFieldCategory by mutableStateOf("Custom Information")
}

@OptIn(ExperimentalMaterial3Api::class)
private class DashboardModalSheets(
    val addActionSheetState: androidx.compose.material3.SheetState,
    val shareModalState: androidx.compose.material3.SheetState,
    val eduSheetState: androidx.compose.material3.SheetState,
    val healthSheetState: androidx.compose.material3.SheetState,
    val docsSheetState: androidx.compose.material3.SheetState,
    val uploadDocSheetState: androidx.compose.material3.SheetState,
    val vaultSheetState: androidx.compose.material3.SheetState,
    val conflictSheetState: androidx.compose.material3.SheetState,
    val sessionsSheetState: androidx.compose.material3.SheetState,
    val notificationSheetState: androidx.compose.material3.SheetState,
    val searchSheetState: androidx.compose.material3.SheetState,
    val storageSheetState: androidx.compose.material3.SheetState,
    val dataBackupSheetState: androidx.compose.material3.SheetState,
    val wallpaperOptionsSheetState: androidx.compose.material3.SheetState,
    val centralizedEditorSheetState: androidx.compose.material3.SheetState,
    val avatarEditorSheetState: androidx.compose.material3.SheetState
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberDashboardModalSheets(): DashboardModalSheets {
    val addActionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shareModalState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val eduSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val healthSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val docsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uploadDocSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val vaultSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val conflictSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sessionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val notificationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val searchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val storageSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dataBackupSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val wallpaperOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val centralizedEditorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val avatarEditorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    return remember(
        addActionSheetState,
        shareModalState,
        eduSheetState,
        healthSheetState,
        docsSheetState,
        uploadDocSheetState,
        vaultSheetState,
        conflictSheetState,
        sessionsSheetState,
        notificationSheetState,
        searchSheetState,
        storageSheetState,
        dataBackupSheetState,
        wallpaperOptionsSheetState,
        centralizedEditorSheetState,
        avatarEditorSheetState
    ) {
        DashboardModalSheets(
            addActionSheetState,
            shareModalState,
            eduSheetState,
            healthSheetState,
            docsSheetState,
            uploadDocSheetState,
            vaultSheetState,
            conflictSheetState,
            sessionsSheetState,
            notificationSheetState,
            searchSheetState,
            storageSheetState,
            dataBackupSheetState,
            wallpaperOptionsSheetState,
            centralizedEditorSheetState,
            avatarEditorSheetState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaDashboardScreen(
    securityLevel: KeySecurityLevel,
    initialOpenNoteId: String? = null,
    initialQuickAction: String? = null,
    onQuickActionHandled: (() -> Unit)? = null,
    onLockClicked: () -> Unit,
    onSignOutClicked: () -> Unit = onLockClicked,
    onSignInClicked: () -> Unit = {},
    onRequestBiometricAuth: ((title: String, subtitle: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onRequestVaultBiometricAuth: ((title: String, subtitle: String, onSuccess: (androidx.biometric.BiometricPrompt.CryptoObject) -> Unit, onError: (String) -> Unit) -> Unit)? = null
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
    LaunchedEffect(Unit) {
        val status = context.getSharedPreferences("pims_restore_status", android.content.Context.MODE_PRIVATE)
        if (status.getBoolean("restore_failed", false)) {
            snackbarHostState.showSnackbar("Restore failed. Your previous vault was preserved. Choose the backup again to retry.")
            status.edit().remove("restore_failed").apply()
        } else if (status.getBoolean("restore_applied", false)) {
            snackbarHostState.showSnackbar("Backup restored. This vault is in local-only mode.")
            status.edit().remove("restore_applied").apply()
        }
    }


    // Automatically route all notifications/snackbars into Apple-grade floating dynamic status HUD
    LaunchedEffect(snackbarHostState.currentSnackbarData) {
        snackbarHostState.currentSnackbarData?.let { data ->
            val msg = data.visuals.message
            val type = com.pims.vault.presentation.ui.components.ux.personaFeedbackType(msg)
            toastController.show(
                PersonaToastMessage(
                    message = msg,
                    type = type,
                    actionLabel = data.visuals.actionLabel,
                    onAction = { data.performAction() },
                    onDismiss = { data.dismiss() }
                )
            )
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
    val hasSyncAttentionNeeded = syncState is SyncState.Conflict || 
                                 syncState is SyncState.Failed || 
                                 conflicts.isNotEmpty()
    val effectiveSyncStatusText = when {
        syncState is SyncState.Syncing -> "Syncing..."
        syncState is SyncState.Conflict -> "Needs attention"
        syncState is SyncState.Failed -> "Sync paused"
        isLocalOnly -> "Local Vault"
        syncState is SyncState.Idle -> "Synced"
        else -> "Ready"
    }
    val effectiveSyncIsGood = !hasSyncAttentionNeeded

    var contextualExplanation by remember { mutableStateOf<ContextualExplanation?>(null) }

    val syncStatusText = effectiveSyncStatusText
    val syncIsGood = effectiveSyncIsGood

    val themeManager = remember { ThemeManager(context) }
    val themeMode by themeManager.themeMode.collectAsState()
    val soundManager = rememberPimsSoundManager()
    val isSoundEnabled by soundManager.isSoundEnabled.collectAsState()
    val wallpaperManager = remember { LocalWallpaperManager.getInstance(context) }
    val wallpapers by wallpaperManager.wallpapers.collectAsState()
    val activeWallpaperIndex by wallpaperManager.activeWallpaperIndex.collectAsState()
    val isWallpaperAutoplayEnabled by wallpaperManager.isAutoplayEnabled.collectAsState()
    val wallpaperImportProgress by wallpaperManager.importProgress.collectAsState()
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
    val notifPrefs = remember { context.getSharedPreferences("pims_notifications_prefs", Context.MODE_PRIVATE) }
    var dismissedNotificationIds by remember {
        mutableStateOf(notifPrefs.getStringSet("dismissed_ids", emptySet())?.toSet() ?: emptySet())
    }
    val dismissNotification: (String) -> Unit = remember {
        { id ->
            val updated = dismissedNotificationIds + id
            dismissedNotificationIds = updated
            notifPrefs.edit().putStringSet("dismissed_ids", updated).apply()
        }
    }
    val dismissAllNotifications: (Collection<String>) -> Unit = remember {
        { ids ->
            val updated = dismissedNotificationIds + ids
            dismissedNotificationIds = updated
            notifPrefs.edit().putStringSet("dismissed_ids", updated).apply()
        }
    }

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { selectedUris: List<Uri> ->
        if (selectedUris.isNotEmpty()) {
            scope.launch {
                val result = wallpaperManager.importFromUris(selectedUris)
                if (result.importedItems.isNotEmpty()) {
                    recentActivityManager.recordActivity("Added ${result.importedItems.size} profile wallpapers")
                    soundManager.success()
                }
                val message = when {
                    result.failedCount == 0 -> "Added ${result.importedItems.size} wallpapers"
                    result.importedItems.isEmpty() -> "No photos could be added. Choose images under 16 MB."
                    else -> "Added ${result.importedItems.size} wallpapers. ${result.failedCount} could not be added."
                }
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    // 5 Bottom Navigation Tabs: 0: Home, 1: Notes, 2: Me, 3: People, 4: Settings
    val navigationUi = remember { DashboardNavigationState(initialOpenNoteId) }

    // Full screen overlays (e.g. Full Vault, Backup, Notes Editor, Connect Person)

    LaunchedEffect(initialOpenNoteId) {
        if (!initialOpenNoteId.isNullOrBlank()) {
            navigationUi.activeNoteId = initialOpenNoteId
            navigationUi.fullScreenOverlay = "NOTE_EDITOR"
            navigationUi.selectedTab = 1
        }
    }

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
                "centralizedEditor" to false,
                "avatarEditor" to false
            )
        )
    }
    
    fun showSheet(sheetName: String) {
        sheetStates.value = sheetStates.value.toMutableMap().apply { this[sheetName] = true }
    }
    
    fun hideSheet(sheetName: String) {
        sheetStates.value = sheetStates.value.toMutableMap().apply { this[sheetName] = false }
    }

    LaunchedEffect(initialQuickAction) {
        when (initialQuickAction) {
            MainActivity.ACTION_DOCUMENTS -> showSheet("documents")
            MainActivity.ACTION_PEOPLE -> navigationUi.selectedTab = 3
            MainActivity.ACTION_VAULT -> navigationUi.fullScreenOverlay = "VAULT"
            MainActivity.ACTION_NOTES -> navigationUi.selectedTab = 1
        }
        if (initialQuickAction != null) {
            onQuickActionHandled?.invoke()
        }
    }

    LaunchedEffect(relationshipState.feedbackMessage, relationshipState.errorMessage) {
        val message = relationshipState.errorMessage ?: relationshipState.feedbackMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            relationshipViewModel.onEvent(RelationshipEvent.ClearFeedback)
        }
    }

    var attachmentToPreview by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
    var attachmentPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAttachmentPreviewLoading by remember { mutableStateOf(false) }

    // Dashboard destinations contain Zone 2+ data, including relationship notes.
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        val wasSecure = window?.attributes?.flags?.and(android.view.WindowManager.LayoutParams.FLAG_SECURE) != 0
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (!wasSecure) window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    LaunchedEffect(relationshipState.isVaultUnlocked) {
        if (!relationshipState.isVaultUnlocked) attachmentToPreview = null
    }

    LaunchedEffect(attachmentToPreview) {
        attachmentPreviewBitmap?.recycle()
        attachmentPreviewBitmap = null
        val attachment = attachmentToPreview ?: run { isAttachmentPreviewLoading = false; return@LaunchedEffect }
        isAttachmentPreviewLoading = true
        try {
            attachmentPreviewBitmap = withContext(Dispatchers.IO) {
            try {
                if (attachment.second.startsWith("image/")) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(attachment.first, 0, attachment.first.size, options)
                    require(options.outWidth > 0 && options.outHeight > 0) { "Invalid attachment image" }
                    var sample = 1
                    while (options.outWidth / sample > 1600 || options.outHeight / sample > 1600) sample *= 2
                    options.inJustDecodeBounds = false
                    options.inSampleSize = sample
                    BitmapFactory.decodeByteArray(attachment.first, 0, attachment.first.size, options)
                } else if (attachment.second == "application/pdf") {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) error("In-memory PDF preview requires Android 11 or newer")
                    val memoryFd = Os.memfd_create("pims-note-pdf-preview", 0)
                    try {
                        var written = 0
                        while (written < attachment.first.size) {
                            written += Os.write(memoryFd, attachment.first, written, attachment.first.size - written)
                        }
                        Os.lseek(memoryFd, 0, OsConstants.SEEK_SET)
                        ParcelFileDescriptor.dup(memoryFd).use { descriptor ->
                            PdfRenderer(descriptor).use { renderer ->
                                if (renderer.pageCount == 0) null else renderer.openPage(0).use { page ->
                                    val scale = minOf(1f, 1400f / page.width, 1800f / page.height)
                                    val bitmap = Bitmap.createBitmap(
                                        (page.width * scale).toInt().coerceAtLeast(1),
                                        (page.height * scale).toInt().coerceAtLeast(1),
                                        Bitmap.Config.ARGB_8888
                                    )
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    bitmap
                                }
                            }
                        }
                    } finally {
                        Os.close(memoryFd)
                    }
                } else null
            } catch (_: Exception) { null }
            }
        } finally {
            isAttachmentPreviewLoading = false
        }
    }

    DisposableEffect(attachmentToPreview) {
        val previewBytes = attachmentToPreview?.first
        onDispose {
            previewBytes?.fill(0)
            attachmentPreviewBitmap?.recycle()
            attachmentPreviewBitmap = null
        }
    }

    val modalSheets = rememberDashboardModalSheets()
    

    // Tiered Biometric Verification state for sensitive records (Level 2) and Vault (Level 3)
    val accessUi = remember { DashboardAccessState() }

    fun requestGatedAccess(
        tier: SecurityTier,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit
    ) {
        if (tier == SecurityTier.LEVEL_2_SENSITIVE && accessUi.isLevel2Unlocked) {
            onSuccess()
            return
        }
        accessUi.pendingSecurityTier = tier
        accessUi.pendingSensitiveAction = onSuccess

        if (tier == SecurityTier.LEVEL_3_VAULT) {
            val requestVaultAuth = onRequestVaultBiometricAuth
            if (requestVaultAuth == null) {
                accessUi.pendingSensitiveAction = null
                accessUi.pendingSecurityTier = null
                scope.launch { snackbarHostState.showSnackbar("Vault hardware authentication is unavailable") }
                return
            }
            requestVaultAuth(
                title,
                subtitle,
                { cryptoObject ->
                    accessUi.zone4CryptoObject = cryptoObject
                    val action = accessUi.pendingSensitiveAction
                    accessUi.pendingSensitiveAction = null
                    accessUi.pendingSecurityTier = null
                    action?.invoke()
                },
                { error ->
                    accessUi.zone4CryptoObject = null
                    accessUi.pendingSensitiveAction = null
                    accessUi.pendingSecurityTier = null
                    scope.launch { snackbarHostState.showSnackbar("Vault authentication cancelled: $error") }
                }
            )
            return
        }

        val biometricAvailable = onRequestBiometricAuth != null && pinSecurityManager.isBiometricEnabled.value
        if (biometricAvailable) {
            onRequestBiometricAuth?.invoke(
                title,
                subtitle,
                {
                    if (tier == SecurityTier.LEVEL_2_SENSITIVE) {
                        accessUi.isLevel2Unlocked = true
                    }
                    val action = accessUi.pendingSensitiveAction
                    accessUi.pendingSensitiveAction = null
                    accessUi.pendingSecurityTier = null
                    action?.invoke()
                },
                { error ->
                    // Biometric cancelled or failed -> fallback to PIN challenge if configured
                    if (pinSecurityManager.hasPin()) {
                        accessUi.isPinChallengeVisible = true
                    } else if (error.contains("BIOMETRICS_UNAVAILABLE", ignoreCase = true) ||
                        error.contains("No fingerprints", ignoreCase = true) ||
                        error.contains("No biometric", ignoreCase = true)) {
                        // Hardware credential unavailable and no app PIN configured: allow graceful access
                        if (tier == SecurityTier.LEVEL_2_SENSITIVE) {
                            accessUi.isLevel2Unlocked = true
                        }
                        val action = accessUi.pendingSensitiveAction
                        accessUi.pendingSensitiveAction = null
                        accessUi.pendingSecurityTier = null
                        action?.invoke()
                    } else {
                        accessUi.pendingSensitiveAction = null
                        accessUi.pendingSecurityTier = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Authentication cancelled: Access locked")
                        }
                    }
                }
            )
        } else if (pinSecurityManager.hasPin()) {
            accessUi.isPinChallengeVisible = true
        } else {
            // Initial setup or unconfigured security
            if (tier == SecurityTier.LEVEL_2_SENSITIVE) {
                accessUi.isLevel2Unlocked = true
            }
            accessUi.pendingSensitiveAction = null
            accessUi.pendingSecurityTier = null
            onSuccess()
        }
    }

    fun withPrivateNotesUnlocked(action: () -> Unit) {
        if (relationshipState.isVaultUnlocked) {
            action()
            return
        }
        requestGatedAccess(SecurityTier.LEVEL_3_VAULT, "Unlock security vault", "Confirm with a strong biometric") {
            vaultViewModel.elevateAndUnlockVault(
                cryptoObject = requireNotNull(accessUi.zone4CryptoObject).also { accessUi.zone4CryptoObject = null },
                onSuccess = action,
                onError = { error -> scope.launch { snackbarHostState.showSnackbar(error) } }
            )
        }
    }

    val openVaultFortress: () -> Unit = {
        if (vaultState.isVaultUnlocked) {
            navigationUi.fullScreenOverlay = "VAULT"
        } else {
            requestGatedAccess(
                SecurityTier.LEVEL_3_VAULT,
                "Unlock Security Vault",
                "Confirm with a strong biometric to unlock this hardware-protected vault"
            ) {
                vaultViewModel.elevateAndUnlockVault(
                    cryptoObject = requireNotNull(accessUi.zone4CryptoObject).also { accessUi.zone4CryptoObject = null },
                    onSuccess = {
                        navigationUi.fullScreenOverlay = "VAULT"
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
    val documentUi = remember { DashboardDocumentState() }

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

    val customFieldUi = remember { DashboardCustomFieldState() }
    
    fun showDialog(dialogName: String) {
        dialogStates.value = dialogStates.value.toMutableMap().apply { this[dialogName] = true }
    }
    
    fun hideDialog(dialogName: String) {
        dialogStates.value = dialogStates.value.toMutableMap().apply { this[dialogName] = false }
    }

    fun clearDocumentDraft() {
        documentUi.pendingUploadUri?.let { uri ->
            runCatching { context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        documentUi.pendingUploadUri = null
        documentUi.versionUploadTarget = null
        documentUi.uploadDocError = null
    }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val metadata = withContext(Dispatchers.IO) {
                        val value = queryUploadMetadata(context, uri)
                        require(value.second <= DocumentRules.MAX_DOCUMENT_SIZE_BYTES) { "File exceeds the 50 MB limit" }
                        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                        Triple(value.first, value.second, resolveUploadMimeType(context, uri, value.first))
                    }
                    val replacing = documentUi.pendingUploadUri != null
                    val previous = documentUi.pendingUploadUri
                    documentUi.pendingUploadUri = uri
                    if (previous != null && previous != uri) runCatching {
                        context.contentResolver.releasePersistableUriPermission(previous, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    documentUi.uploadDocImportId = java.util.UUID.randomUUID().toString()
                    documentUi.uploadDocFileName = metadata.first
                    documentUi.uploadDocSizeBytes = metadata.second
                    documentUi.uploadDocMimeType = metadata.third
                    documentUi.uploadDocError = null
                    if (!replacing) {
                        val target = documentUi.versionUploadTarget
                        val guessed = guessUploadDocumentTypeAndTitle(metadata.first)
                        documentUi.uploadDocType = target?.document?.documentType ?: guessed.first
                        documentUi.uploadDocTitle = target?.document?.title ?: guessed.second
                        documentUi.uploadDocNumber = target?.document?.documentNumber.orEmpty()
                        documentUi.uploadDocAuthority = target?.document?.issuingAuthority.orEmpty()
                        documentUi.uploadDocExpiryDate = target?.document?.expirationDate.orEmpty()
                        documentUi.uploadDocClassification = target?.document?.securityClassification ?: DocumentRules.resolveDefaultClassification(guessed.first)
                    }
                    showSheet("uploadDoc")
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
                } catch (_: Exception) { snackbarHostState.showSnackbar("Could not select this file. Choose an accessible file up to 50 MB.") }
            }
        } else if (documentUi.pendingUploadUri == null) documentUi.versionUploadTarget = null
    }

    val exportDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destinationUri: Uri? ->
        val doc = documentUi.pendingExportDoc
        val version = doc?.currentVersion
        if (destinationUri != null && version != null) {
            documentViewModel.exportDocument(
                contentResolver = context.contentResolver,
                destinationUri = destinationUri,
                version = version,
                onComplete = {
                    soundManager.success()
                    scope.launch {
                        snackbarHostState.showSnackbar("Document copy saved")
                    }
                },
                onError = { err ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Export failed: $err")
                    }
                }
            )
        }
        documentUi.pendingExportDoc = null
    }

    fun authorizeDocumentAction(doc: DocumentWithHistory, action: () -> Unit) {
        when {
            doc.document.securityClassification == SecurityClassification.ZONE_4_CRITICAL -> withPrivateNotesUnlocked(action)
            (doc.document.isLocked || (doc.document.securityClassification == SecurityClassification.ZONE_3_SENSITIVE &&
                pinSecurityManager.requireAuthForSensitive.value)) && !accessUi.isLevel2Unlocked ->
                requestGatedAccess(SecurityTier.LEVEL_2_SENSITIVE, "Unlock document", "Confirm your identity", action)
            else -> action()
        }
    }

    val openDocumentExternally: (DocumentWithHistory) -> Unit = { doc ->
        authorizeDocumentAction(doc) {
            doc.currentVersion?.let { version ->
                documentViewModel.decryptForViewing(context, version, doc.document.title, onReady = { uri, mime ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mime)
                        clipData = android.content.ClipData.newUri(context.contentResolver, "Document", uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open with another app"))
                }, onError = { message -> scope.launch { snackbarHostState.showSnackbar(message) } })
            }
        }
    }

    val onOpenDocumentForViewing: (DocumentWithHistory) -> Unit = { doc ->
        authorizeDocumentAction(doc) {
            val version = doc.currentVersion
            if (version == null) scope.launch { snackbarHostState.showSnackbar("No file version is available. Check the version history.") }
            else {
                documentViewModel.onEvent(DocumentEvent.SelectDocument(doc))
                documentViewModel.onEvent(DocumentEvent.PreviewVersion(version))
            }
        }
    }

    val onInitiateDocumentExport: (DocumentWithHistory) -> Unit = { doc ->
        authorizeDocumentAction(doc) {
            documentUi.pendingExportDoc = doc
            val original = doc.currentVersion?.originalFilename
            val suffix = when (doc.currentVersion?.mimeType) {
                "application/pdf", "application/x-pdf" -> ".pdf"
                "image/jpeg", "image/jpg" -> ".jpg"
                "image/png" -> ".png"
                "text/plain" -> ".txt"
                else -> ""
            }
            val filename = original?.takeIf { it.isNotBlank() } ?: "${doc.document.title}$suffix"
            exportDocLauncher.launch(filename.replace(Regex("[^a-zA-Z0-9._ -]"), "_").take(120))
        }
    }

    

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

    // Activity & Notification Center Data (Strictly genuine pending requests, sync alerts, and expiring documents)
    val notifications = remember(profileState.relationships, documentState.documents, conflicts, dismissedNotificationIds) {
        val list = mutableListOf<PersonaNotificationItem>()

        // 1. Pending Connection Requests (ONLY genuine pending requests, not stale contacts with static notes)
        profileState.relationships.forEach { rel ->
            val notifId = "rel_${rel.id}"
            if (!dismissedNotificationIds.contains(notifId)) {
                val isPendingRequest = rel.relationRole.contains("Request", ignoreCase = true) ||
                                       rel.relationRole.equals("PENDING", ignoreCase = true) ||
                                       rel.notes.contains("Request", ignoreCase = true)
                if (isPendingRequest) {
                    list.add(
                        PersonaNotificationItem(
                            id = notifId,
                            title = "Connection request",
                            description = "${rel.fullName} wants to connect with you.",
                            timestampText = "Request",
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

        // 3. Expiring Documents (Urgent life event: only documents expiring within 30 days or expired within the last 7 days)
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
                            if (diffDays in -7..30) {
                                val (titleText, descText, timeText) = when {
                                    diffDays < 0 -> Triple(
                                        "${doc.title} expired",
                                        "Your ${doc.documentType.name.lowercase().replace('_', ' ')} expired ${-diffDays} days ago.",
                                        "Expired ${-diffDays}d ago"
                                    )
                                    diffDays == 0 -> Triple(
                                        "${doc.title} expires today",
                                        "Your ${doc.documentType.name.lowercase().replace('_', ' ')} expires today.",
                                        "Today"
                                    )
                                    else -> Triple(
                                        "${doc.title} expires soon",
                                        "Your ${doc.documentType.name.lowercase().replace('_', ' ')} expires in $diffDays days.",
                                        "$diffDays d"
                                    )
                                }
                                list.add(
                                    PersonaNotificationItem(
                                        id = notifId,
                                        title = titleText,
                                        description = descText,
                                        timestampText = timeText,
                                        category = NotificationCategory.SHARING_ACTIVITY,
                                        isNeedsAttention = diffDays <= 7,
                                        isToday = diffDays <= 1,
                                        isRead = false,
                                        level = if (diffDays <= 7) AlertLevel.WARNING else AlertLevel.INFO,
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
    if (navigationUi.fullScreenOverlay != null) {
        when (navigationUi.fullScreenOverlay) {
            "NOTE_EDITOR" -> {
                PlainNoteEditorScreen(
                    noteId = navigationUi.activeNoteId,
                    onBack = {
                        navigationUi.activeNoteId = null
                        navigationUi.fullScreenOverlay = null
                    },
                    onRequestAuth = { title, subtitle, onSuccess ->
                        requestGatedAccess(
                            SecurityTier.LEVEL_2_SENSITIVE,
                            title,
                            subtitle,
                            onSuccess
                        )
                    }
                )
                return
            }
            "CONNECT_PERSON" -> {
                ConnectPersonScreen(
                    onBack = { navigationUi.fullScreenOverlay = null },
                    onSavePerson = { role, relName, gender, phone, email, address, dob, anniversary, notes, photoPath, isNextOfKin, contacts ->
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
                                isNextOfKin = isNextOfKin,
                                customPersonId = targetId,
                                photoPath = photoPath,
                                contactMethods = contacts.map { input ->
                                    com.pims.vault.data.local.entity.ContactMethodEntity(
                                        id = java.util.UUID.randomUUID().toString(),
                                        personId = targetId,
                                        contactType = input.type,
                                        label = input.label,
                                        value = input.value.trim(),
                                        isPrimary = input.isPrimary,
                                        sortOrder = contacts.filter { it.type == input.type }.indexOfFirst { it.key == input.key }
                                    )
                                }
                            )
                        )
                        recentActivityManager.recordActivity("Added $relName", "Relationship: $role")
                        toastController.showSuccess("Added $relName to People")
                        soundManager.success()
                        navigationUi.fullScreenOverlay = null
                    }
                )
                return
            }
            "PERSON_DETAIL" -> {
                val currentPerson = profileState.relationships.find { it.id == navigationUi.selectedPersonForDetail?.id } ?: navigationUi.selectedPersonForDetail
                if (currentPerson != null) {
                    BackHandler(enabled = true) {
                        if (navigationUi.personToEditForDialog != null) {
                            navigationUi.personToEditForDialog = null
                        } else {
                            navigationUi.fullScreenOverlay = null
                            navigationUi.selectedPersonForDetail = null
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        PersonDetailScreen(
                            person = currentPerson,
                            relationshipNotes = relationshipState.relationshipNotes,
                            isVaultUnlocked = relationshipState.isVaultUnlocked,
                            onUnlockVault = { withPrivateNotesUnlocked {} },
                            onBack = {
                                navigationUi.fullScreenOverlay = null
                                navigationUi.selectedPersonForDetail = null
                            },
                            onEditPerson = { p ->
                                navigationUi.personToEditForDialog = p
                            },
                            onDeletePerson = { p ->
                                profileViewModel.onEvent(ProfileEvent.DeleteRelationship(p.id, p.targetPersonId))
                                recentActivityManager.recordActivity("Removed person from People", "Disconnected ${p.fullName}")
                                navigationUi.fullScreenOverlay = null
                                navigationUi.selectedPersonForDetail = null
                            },
                            onOpenAttachment = { noteId, attachmentId ->
                                scope.launch {
                                    val previous = attachmentToPreview?.first
                                    previous?.fill(0)
                                    attachmentToPreview = try {
                                        withContext(Dispatchers.IO) { relationshipViewModel.readAttachment(noteId, attachmentId) }
                                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) { null }
                                    if (attachmentToPreview == null) snackbarHostState.showSnackbar("Attachment is unavailable or this note is locked")
                                }
                            },
                            onAddNote = { topic, content, format, isPrivate, attachments, removals ->
                                val saved = relationshipViewModel.saveRelationshipNote(
                                    relationshipId = currentPerson.id,
                                    topic = topic,
                                    content = content,
                                    format = format,
                                    isPrivate = isPrivate,
                                    attachmentsToAdd = attachments,
                                    attachmentIdsToRemove = removals
                                )
                                if (saved) {
                                    soundManager.success()
                                    snackbarHostState.showSnackbar("Note saved to memory dossier")
                                }
                                saved
                            },
                            onUpdateNote = { noteId, topic, content, format, isPrivate, attachments, removals ->
                                val saved = relationshipViewModel.saveRelationshipNote(
                                    relationshipId = currentPerson.id,
                                    topic = topic,
                                    content = content,
                                    format = format,
                                    isPrivate = isPrivate,
                                    noteId = noteId,
                                    attachmentsToAdd = attachments,
                                    attachmentIdsToRemove = removals
                                )
                                if (saved) {
                                    soundManager.success()
                                    snackbarHostState.showSnackbar("Note updated")
                                }
                                saved
                            },
                            onDeleteNote = { noteId ->
                                val delete = { relationshipViewModel.onEvent(RelationshipEvent.DeleteNote(noteId)) }
                                if (relationshipState.relationshipNotes.any { it.id == noteId && it.isPrivate }) {
                                    withPrivateNotesUnlocked(delete)
                                } else delete()
                            },
                            onToggleTrusted = { _ ->
                            },
                            onToggleNotePrivacy = { noteId, makePrivate ->
                                withPrivateNotesUnlocked {
                                    relationshipViewModel.onEvent(RelationshipEvent.ToggleNotePrivacy(noteId, makePrivate))
                                }
                            }
                        )

                        if (navigationUi.personToEditForDialog != null) {
                            EditManagedPersonSheet(
                                person = navigationUi.personToEditForDialog,
                                relationshipState = relationshipState,
                                profileViewModel = profileViewModel,
                                recentActivityManager = recentActivityManager,
                                soundManager = soundManager,
                                haptics = haptics,
                                toastController = toastController,
                                onDismissRequest = { navigationUi.personToEditForDialog = null }
                            )
                        }
                    }
                    return
                } else {
                    navigationUi.fullScreenOverlay = null
                }
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
                                    cryptoObject = requireNotNull(accessUi.zone4CryptoObject).also { accessUi.zone4CryptoObject = null },
                                    onError = { err ->
                                        scope.launch { snackbarHostState.showSnackbar("Vault re-authentication failed: $err") }
                                    }
                                )
                            }
                        },
                        onBack = { navigationUi.fullScreenOverlay = null },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                return
            }
            "BACKUP" -> {
                BackHandler(enabled = true) {
                    navigationUi.fullScreenOverlay = null
                }
                Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().screenEnterRise()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navigationUi.fullScreenOverlay = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to Persona")
                        }
                        Text("Backup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    BackupDashboardScreen(
                        viewModel = backupViewModel,
                        isVaultUnlocked = relationshipState.isVaultUnlocked,
                        onUnlockVault = { withPrivateNotesUnlocked {} },
                        modifier = Modifier.weight(1f)
                    )
                }
                return
            }
            "SECURITY_SETTINGS" -> {
                Box(modifier = Modifier.fillMaxSize().statusBarsPadding().screenEnterRise()) {
                    SecuritySettingsScreen(
                        pinSecurityManager = pinSecurityManager,
                        onBack = { navigationUi.fullScreenOverlay = null },
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
                        onDismiss = { navigationUi.fullScreenOverlay = null },
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
                        onBack = { navigationUi.fullScreenOverlay = null },
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
        enabled = accessUi.isPinChallengeVisible ||
                navigationUi.personToEditForDialog != null ||
                navigationUi.showPublicResumeModal ||
                navigationUi.selectedPersonForDetail != null ||
                hasOpenDialog ||
                hasOpenSheet ||
                navigationUi.selectedTab != 0
    ) {
        when {
            accessUi.isPinChallengeVisible -> {
                accessUi.isPinChallengeVisible = false
                accessUi.pendingSensitiveAction = null
                accessUi.pendingSecurityTier = null
            }
            navigationUi.personToEditForDialog != null -> {
                navigationUi.personToEditForDialog = null
            }
            navigationUi.showPublicResumeModal -> {
                navigationUi.showPublicResumeModal = false
            }
            navigationUi.selectedPersonForDetail != null -> {
                navigationUi.selectedPersonForDetail = null
            }
            hasOpenDialog -> {
                dialogStates.value = dialogStates.value.mapValues { false }
            }
            hasOpenSheet -> {
                sheetStates.value = sheetStates.value.mapValues { false }
            }
            navigationUi.selectedTab != 0 -> {
                navigationUi.selectedTab = 0
            }
        }
    }

    val useNavigationRail = LocalConfiguration.current.screenWidthDp >= 600

    // Primary navigation adapts to the available window width.
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { PersonaToastHost(toastController) },
        floatingActionButton = {
            when (navigationUi.selectedTab) {
                3 -> {
                    FloatingActionButton(
                        onClick = {
                            haptics.medium()
                            soundManager.navigation()
                            navigationUi.fullScreenOverlay = "CONNECT_PERSON"
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .tactilePress()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add a person", modifier = Modifier.size(24.dp))
                    }
                }
            }
        },
        bottomBar = {
            if (!useNavigationRail) {
                PersonaDashboardBottomBar(
                    selectedTab = navigationUi.selectedTab,
                    onTabSelected = { navigationUi.selectedTab = it }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                PersonaDashboardNavigationRail(
                    selectedTab = navigationUi.selectedTab,
                    onTabSelected = { navigationUi.selectedTab = it },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (useNavigationRail) 80.dp else innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                        end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = innerPadding.calculateBottomPadding()
                    )
            ) {
            val reducedMotion = LocalReducedMotion.current
            AnimatedContent(
                targetState = navigationUi.selectedTab,
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(tween(durationMillis = 0)) togetherWith fadeOut(tween(durationMillis = 0))
                    } else {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(tween(260, easing = PimsAnimation.EasingEnter)) { (it / 24) * direction } +
                            fadeIn(tween(210, easing = PimsAnimation.EasingEnter))) togetherWith
                            (slideOutHorizontally(tween(170, easing = PimsAnimation.EasingExit)) { (-it / 32) * direction } +
                                fadeOut(tween(150, easing = PimsAnimation.EasingExit)))
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
                        isWallpaperAutoplayEnabled = isWallpaperAutoplayEnabled,
                        onActiveWallpaperChanged = { wallpaperManager.setActiveIndex(it) },
                        onOpenWallpaperOptions = { showSheet("wallpaperOptions") },
                        onOpenCentralizedEditor = { showSheet("centralizedEditor") },
                        onNotificationClick = {
                            soundManager.navigation()
                            showSheet("notification")
                        },
                        onOpenProfile = {
                            soundManager.navigation()
                            navigationUi.selectedTab = 2
                        },
                        onOpenPeople = {
                            soundManager.navigation()
                            navigationUi.selectedTab = 3
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
                                navigationUi.fullScreenOverlay = "HEALTH"
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
                            navigationUi.activeNoteId = noteId
                            navigationUi.fullScreenOverlay = "NOTE_EDITOR"
                        },
                        onRequestAuth = { title, subtitle, onSuccess ->
                            requestGatedAccess(
                                SecurityTier.LEVEL_2_SENSITIVE,
                                title,
                                subtitle,
                                onSuccess
                            )
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
                        onViewPublicDossier = { navigationUi.showPublicResumeModal = true },
                        onExportPdf = { handleExportPdf() },
                        onDeleteCustomField = { id -> profileViewModel.onEvent(ProfileEvent.DeleteCustomField(id)) },
                        onAddCustomField = {
                            customFieldUi.customFieldEditingId = null
                            customFieldUi.customFieldInitialLabel = ""
                            customFieldUi.customFieldInitialValue = ""
                            customFieldUi.customFieldCategory = "PERSONAL"
                            showDialog("addCustomField")
                        },
                        onEditCustomField = { field ->
                            customFieldUi.customFieldEditingId = field.id
                            customFieldUi.customFieldInitialLabel = field.label
                            customFieldUi.customFieldInitialValue = field.value
                            customFieldUi.customFieldCategory = field.category.name
                            showDialog("addCustomField")
                        },
                        onEducationClick = { showSheet("education") },
                        onHealthClick = {
                            requestGatedAccess(
                                SecurityTier.LEVEL_2_SENSITIVE,
                                "Health & Medical",
                                "Confirm identity to access medical records"
                            ) {
                                navigationUi.fullScreenOverlay = "HEALTH"
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
                        onPeopleClick = { navigationUi.selectedTab = 3 },
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
                        onSyncCloud = {
                            haptics.selection()
                            soundManager.navigation()
                            syncViewModel.processSyncNow()
                            scope.launch {
                                snackbarHostState.showSnackbar("Backing up socials & vault to cloud...")
                            }
                        },
                        onRestoreCloud = {
                            haptics.selection()
                            soundManager.navigation()
                            syncViewModel.processSyncNow()
                            scope.launch {
                                snackbarHostState.showSnackbar("Restoring profiles from cloud...")
                            }
                        },
                        onSavePasswordToVault = { platform, username, password ->
                            vaultViewModel.savePassword(
                                title = "Social - $platform",
                                username = username,
                                passwordPlain = password,
                                websiteUrl = null,
                                notes = "Linked from Persona Socials ($platform)"
                            )
                            recentActivityManager.recordActivity("Saved $platform password to Vault")
                            soundManager.success()
                        },
                        onExplain = { contextualExplanation = it }
                    )

                    // TAB 3: PEOPLE & CONNECTIONS
                    3 -> PeopleView(
                        relationships = profileState.relationships,
                        onAddPersonClick = { navigationUi.fullScreenOverlay = "CONNECT_PERSON" },
                        onSelectPerson = { person ->
                            relationshipViewModel.onEvent(RelationshipEvent.LoadNotes(person.id))
                            navigationUi.selectedPersonForDetail = person
                            navigationUi.fullScreenOverlay = "PERSON_DETAIL"
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
                        onOpenAvatarEditor = { showSheet("avatarEditor") },
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
                        onSignInClicked = onSignInClicked,
                        onSecurityClicked = { navigationUi.fullScreenOverlay = "SECURITY_SETTINGS" },
                        onAppPinSecurityClicked = { navigationUi.fullScreenOverlay = "SECURITY_SETTINGS" },
                        onOpenInformationLibrary = { navigationUi.fullScreenOverlay = "INFORMATION_LIBRARY" },
                        onDevicesClicked = { showSheet("sessions") },
                        onStorageClicked = { showSheet("storage") },
                        onDataBackupClicked = { showSheet("dataBackup") },
                        onEmergencyCardClicked = {
                            requestGatedAccess(
                                SecurityTier.LEVEL_2_SENSITIVE,
                                "Emergency Medical",
                                "Confirm identity to access emergency card"
                            ) {
                                navigationUi.fullScreenOverlay = "HEALTH"
                            }
                        },
                        onDeleteAccountClicked = { showDialog("deleteAccount") },
                        onUpgradeAccount = { email ->
                            accountModeManager.upgradeToCloudAccount(email)
                            soundManager.success()
                            haptics.success()
                            syncViewModel.processSyncNow()
                            scope.launch { snackbarHostState.showSnackbar("Cloud account enabled. Backup is starting.") }
                        },
                        onRevisitWalkthrough = { accountModeManager.revisitWalkthrough() },
                        onExplain = { contextualExplanation = it }
                    )
                }
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
            sheetState = modalSheets.addActionSheetState,
            onDismissRequest = { hideSheet("addAction") },
            onSelectAction = { actionType ->
                hideSheet("addAction")
                when (actionType) {
                    // Identity
                    AddActionType.PERSONAL_DETAILS -> showDialog("editProfile")
                    AddActionType.IDENTIFICATION_DOC -> filePickerLauncher.launch(arrayOf("*/*"))
                    AddActionType.NAMES_HISTORY -> {
                        customFieldUi.customFieldInitialLabel = "Previous Name / Alias"
                        customFieldUi.customFieldCategory = "Names & History"
                        showDialog("addCustomField")
                    }
                    AddActionType.EMERGENCY_IDENTITY -> {
                        customFieldUi.customFieldInitialLabel = "Blood Type / Emergency Note"
                        customFieldUi.customFieldCategory = "Emergency Identity"
                        showDialog("addCustomField")
                    }

                    // Contact
                    AddActionType.PHONE -> showDialog("addPhone")
                    AddActionType.EMAIL -> showDialog("addEmail")
                    AddActionType.ADDRESS -> showDialog("addAddress")
                    AddActionType.ONLINE_PRESENCE -> {
                        customFieldUi.customFieldInitialLabel = "Website / Social Profile"
                        customFieldUi.customFieldCategory = "Online Presence"
                        showDialog("addCustomField")
                    }
                    AddActionType.COMMUNICATION_PREFS -> {
                        customFieldUi.customFieldInitialLabel = "Preferred Contact Method"
                        customFieldUi.customFieldCategory = "Communication"
                        showDialog("addCustomField")
                    }

                    // Education
                    AddActionType.EDUCATION,
                    AddActionType.QUALIFICATION -> showDialog("addEdu")
                    AddActionType.CERTIFICATION,
                    AddActionType.ACADEMIC_ACHIEVEMENT,
                    AddActionType.COURSES_TRAINING -> showDialog("addCert")
                    AddActionType.SKILLS_LANGUAGES -> {
                        customFieldUi.customFieldInitialLabel = "Skill / Spoken Language"
                        customFieldUi.customFieldCategory = "Skills & Languages"
                        showDialog("addCustomField")
                    }

                    // Career
                    AddActionType.EXPERIENCE,
                    AddActionType.PROJECTS,
                    AddActionType.VOLUNTEERING -> showDialog("addWork")
                    AddActionType.PORTFOLIO -> {
                        customFieldUi.customFieldInitialLabel = "Portfolio / Project Link"
                        customFieldUi.customFieldCategory = "Portfolio"
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
                    AddActionType.HEALTH_DOCUMENTS -> filePickerLauncher.launch(arrayOf("*/*"))

                    // Personal
                    AddActionType.INTERESTS_HOBBIES -> {
                        customFieldUi.customFieldInitialLabel = "Hobby / Interest"
                        customFieldUi.customFieldCategory = "Interests & Hobbies"
                        showDialog("addCustomField")
                    }
                    AddActionType.PREFERENCES -> {
                        customFieldUi.customFieldInitialLabel = "Personal Preference"
                        customFieldUi.customFieldCategory = "Preferences"
                        showDialog("addCustomField")
                    }
                    AddActionType.PERSONAL_NOTES -> {
                        navigationUi.activeNoteId = null
                        navigationUi.fullScreenOverlay = "NOTE_EDITOR"
                    }
                    AddActionType.GOALS -> {
                        customFieldUi.customFieldInitialLabel = "Personal Goal"
                        customFieldUi.customFieldCategory = "Goals"
                        showDialog("addCustomField")
                    }
                    AddActionType.IMPORTANT_DATES -> {
                        customFieldUi.customFieldInitialLabel = "Important Date"
                        customFieldUi.customFieldCategory = "Important Dates"
                        showDialog("addCustomField")
                    }
                    AddActionType.FAVOURITE_THINGS -> {
                        customFieldUi.customFieldInitialLabel = "Favourite Thing"
                        customFieldUi.customFieldCategory = "Favourite Things"
                        showDialog("addCustomField")
                    }
                    AddActionType.MEMBERSHIPS_AFFILIATIONS -> {
                        customFieldUi.customFieldInitialLabel = "Membership / Club"
                        customFieldUi.customFieldCategory = "Memberships & Affiliations"
                        showDialog("addCustomField")
                    }

                    // Top-Level / Direct
                    AddActionType.DOCUMENT -> filePickerLauncher.launch(arrayOf("*/*"))
                    AddActionType.CUSTOM_FIELD -> {
                        customFieldUi.customFieldInitialLabel = ""
                        customFieldUi.customFieldCategory = "Custom Information"
                        showDialog("addCustomField")
                    }
                    AddActionType.PERSON -> {
                        navigationUi.fullScreenOverlay = "CONNECT_PERSON"
                    }
                    AddActionType.PASSWORD -> {
                        soundManager.navigation()
                        requestGatedAccess(
                            SecurityTier.LEVEL_3_VAULT,
                            "Unlock Vault",
                            "Authenticate to create password"
                        ) {
                            vaultViewModel.elevateAndUnlockVault(
                                cryptoObject = requireNotNull(accessUi.zone4CryptoObject).also { accessUi.zone4CryptoObject = null },
                                onSuccess = {
                                    vaultViewModel.openEditor(com.pims.vault.core.model.VaultCategory.PASSWORD)
                                    navigationUi.fullScreenOverlay = "VAULT"
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
                                cryptoObject = requireNotNull(accessUi.zone4CryptoObject).also { accessUi.zone4CryptoObject = null },
                                onSuccess = {
                                    vaultViewModel.openEditor(com.pims.vault.core.model.VaultCategory.PAYMENT_REFERENCE)
                                    navigationUi.fullScreenOverlay = "VAULT"
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
                                cryptoObject = requireNotNull(accessUi.zone4CryptoObject).also { accessUi.zone4CryptoObject = null },
                                onSuccess = {
                                    vaultViewModel.openEditor(com.pims.vault.core.model.VaultCategory.BANK_ACCOUNT)
                                    navigationUi.fullScreenOverlay = "VAULT"
                                },
                                onError = { err ->
                                    scope.launch { snackbarHostState.showSnackbar("Vault unlock failed: $err") }
                                }
                            )
                        }
                    }
                    AddActionType.SOCIAL_PROFILE -> {
                        customFieldUi.customFieldInitialLabel = ""
                        customFieldUi.customFieldCategory = "Social Profiles"
                        showDialog("addCustomField")
                    }
                }
            }
        )
    }

    // 2. SHARE IDENTITY & VCARD QR SHEET
    if (sheetStates.value["share"] == true) {
        SharePreviewModal(
            personName = fullName,
            occupation = occupation,
            country = country,
            sheetState = modalSheets.shareModalState,
            phone = primaryPhone ?: "",
            email = primaryEmail ?: "",
            bloodGroup = bloodGroup,
            linkedIn = profileState.socialAccounts.firstOrNull { it.platform.equals("LinkedIn", ignoreCase = true) }?.let { it.username ?: it.url } ?: "",
            profilePhotoPath = customAvatarPath,
            onDismissRequest = { hideSheet("share") },
            onCopyShareLink = { link ->
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(link))
                haptics.selection()
                scope.launch { snackbarHostState.showSnackbar("Contact copied") }
            }
        )
    }

    // 3. EDUCATION & PORTFOLIO FACET SHEET
    if (sheetStates.value["education"] == true) {
        EducationPortfolioSheet(
            educationList = profileState.educationRecords,
            certificatesList = profileState.certificates,
            employmentList = profileState.employmentRecords,
            sheetState = modalSheets.eduSheetState,
            onDismissRequest = { hideSheet("education") },
            onAddEducation = { showDialog("addEdu") },
            onAddCert = { showDialog("addCert") },
            onAddProject = { showDialog("addWork") }
        )
    }

    // 4. HEALTH FULL SCREEN (Full Medical Dossier)
    if (sheetStates.value["health"] == true) {
        hideSheet("health")
        navigationUi.fullScreenOverlay = "HEALTH"
    }

    // 5. DOCUMENTS WALLET SHEET
    if (sheetStates.value["documents"] == true) {
        DocumentsWalletSheet(
            documents = documentState.documents,
            sheetState = modalSheets.docsSheetState,
            onDismissRequest = { hideSheet("documents") },
            onUploadClick = { filePickerLauncher.launch(arrayOf("*/*")) },
            onDeleteClick = { doc -> documentUi.documentToDelete = doc },
            onViewClick = onOpenDocumentForViewing,
            onExportClick = onInitiateDocumentExport,
            onOpenExternal = openDocumentExternally,
            onAddVersion = { doc -> authorizeDocumentAction(doc) {
                documentUi.versionUploadTarget = doc
                filePickerLauncher.launch(arrayOf("*/*"))
            } },
            onRestoreVersion = { id, version ->
                documentState.documents.find { it.document.id == id }?.let { doc ->
                    authorizeDocumentAction(doc) { documentViewModel.onEvent(DocumentEvent.RestoreVersion(id, version)) }
                }
            },
            onVerifyVersion = { version ->
                documentState.documents.find { it.document.id == version.documentId }?.let { doc ->
                    authorizeDocumentAction(doc) { documentViewModel.onEvent(DocumentEvent.VerifyIntegrity(version)) }
                }
            },
            integrityResults = documentState.integrityResults,
            isWorking = documentState.isSaving,
            isLoading = documentState.isLoading,
            loadError = documentState.loadError,
            onRetryLoad = { documentViewModel.onEvent(DocumentEvent.RetryLoad) },
            onRetryDownload = { doc -> authorizeDocumentAction(doc) {
                documentViewModel.onEvent(DocumentEvent.RetryDownload(doc.document.id))
            } }
        )
    }

    // 6. VAULT FORTRESS PREVIEW SHEET
    if (sheetStates.value["vault"] == true) {
        VaultSheet(
            sheetState = modalSheets.vaultSheetState,
            onDismissRequest = { hideSheet("vault") },
            onOpenFullVault = { navigationUi.fullScreenOverlay = "VAULT" }
        )
    }

    // 8. SYNC CONFLICT RESOLUTION SHEET
    if (sheetStates.value["conflict"] == true) {
        SyncConflictSheet(
            conflicts = conflicts,
            sheetState = modalSheets.conflictSheetState,
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
            sheetState = modalSheets.sessionsSheetState,
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
            sheetState = modalSheets.notificationSheetState,
            onDismissRequest = { hideSheet("notification") },
            onNotificationClick = { item ->
                hideSheet("notification")
                soundManager.navigation()
                when {
                    item.id.startsWith("rel") -> {
                        navigationUi.selectedTab = 3 // People tab
                    }
                    item.id.startsWith("conflict") -> showSheet("conflict")
                    item.id.startsWith("doc_exp") -> showSheet("documents")
                }
            },
            onDismissNotification = { item ->
                dismissNotification(item.id)
                soundManager.delete()
                haptics.selection()
            },
            onClearAll = {
                dismissAllNotifications(notifications.map { it.id })
                soundManager.delete()
                haptics.selection()
            },
            onAcceptRequest = { item ->
                dismissNotification(item.id)
                soundManager.success()
                haptics.success()
                scope.launch {
                    snackbarHostState.showSnackbar("✓ Connection accepted")
                }
            },
            onDeclineRequest = { item ->
                dismissNotification(item.id)
                soundManager.delete()
                haptics.selection()
                scope.launch {
                    snackbarHostState.showSnackbar("Connection request declined")
                }
            },
            onMarkAllRead = {
                dismissAllNotifications(notifications.map { it.id })
                soundManager.navigation()
                haptics.selection()
            }
        )
    }

    // WALLPAPER OPTIONS BOTTOM SHEET
    if (sheetStates.value["wallpaperOptions"] == true) {
        val mediaRefResolver = remember {
            PersonaMediaReferenceResolver(context) { wallpaperManager }
        }
        WallpaperOptionsSheet(
            sheetState = modalSheets.wallpaperOptionsSheetState,
            wallpapers = wallpapers,
            availablePresets = wallpaperManager.defaultPresets,
            currentIndex = activeWallpaperIndex,
            isImporting = wallpaperImportProgress != null,
            importCompleted = wallpaperImportProgress?.completed ?: 0,
            importTotal = wallpaperImportProgress?.total ?: 0,
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
                wallpaperPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemoveWallpaper = { item ->
                val result = wallpaperManager.removeFromPersona(listOf(item.id))
                if (result.succeededIds.isNotEmpty()) {
                    recentActivityManager.recordActivity("Removed profile wallpaper")
                    soundManager.delete()
                    haptics.warning()
                    scope.launch {
                        val snackResult = snackbarHostState.showSnackbar(
                            message = "Wallpaper removed from Persona",
                            actionLabel = "Undo",
                            duration = androidx.compose.material3.SnackbarDuration.Short
                        )
                        if (snackResult == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                            val restored = wallpaperManager.undoLastRemoval()
                            if (restored) {
                                soundManager.success()
                                haptics.selection()
                            }
                        }
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
            onDismiss = { hideSheet("wallpaperOptions") },
            onApplySurface = { surface, item, alignX, alignY, zoom ->
                val result = wallpaperManager.applyWallpaperToSurface(surface, item, alignX, alignY, zoom)
                if (result.isSuccess) {
                    recentActivityManager.recordActivity("Applied wallpaper to ${surface.title}")
                    soundManager.success()
                    haptics.selection()
                    scope.launch {
                        snackbarHostState.showSnackbar("✓ Applied to ${surface.title}")
                    }
                    true
                } else {
                    soundManager.delete()
                    haptics.warning()
                    scope.launch {
                        snackbarHostState.showSnackbar("Failed to apply to ${surface.title}: ${result.exceptionOrNull()?.message ?: "Unknown error"}")
                    }
                    false
                }
            },
            onRemoveWallpapersBulk = { ids ->
                val result = wallpaperManager.removeFromPersona(ids)
                if (result.succeededIds.isNotEmpty()) {
                    recentActivityManager.recordActivity("Removed ${result.succeededIds.size} wallpaper(s)")
                    soundManager.delete()
                    haptics.warning()
                    scope.launch {
                        val snackResult = snackbarHostState.showSnackbar(
                            message = "Removed ${result.succeededIds.size} wallpaper(s) from Persona",
                            actionLabel = "Undo",
                            duration = androidx.compose.material3.SnackbarDuration.Short
                        )
                        if (snackResult == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                            val restored = wallpaperManager.undoLastRemoval()
                            if (restored) {
                                soundManager.success()
                                haptics.selection()
                            }
                        }
                    }
                }
            },
            onDeleteWallpapersBulk = { ids ->
                val result = wallpaperManager.deleteFromStorage(ids, mediaRefResolver)
                if (result.succeededIds.isNotEmpty()) {
                    recentActivityManager.recordActivity("Deleted ${result.succeededIds.size} wallpaper(s)")
                    soundManager.delete()
                    haptics.warning()
                    scope.launch {
                        val msg = buildString {
                            append("Deleted ${result.succeededIds.size} item(s)")
                            if (result.preservedDueToReferences.isNotEmpty()) {
                                append(" (${result.preservedDueToReferences.size} preserved due to profile references)")
                            }
                            if (result.failedIds.isNotEmpty()) {
                                append(" (${result.failedIds.size} failed)")
                            }
                        }
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            },
            mediaReferenceResolver = mediaRefResolver,
            activeSurfaceWallpaperId = { surface -> wallpaperManager.getActiveSurfaceWallpaperId(surface) }
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
            sheetState = modalSheets.centralizedEditorSheetState,
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

    // 16. AVATAR EDITOR BOTTOM SHEET
    if (sheetStates.value["avatarEditor"] == true) {
        com.pims.vault.presentation.avatar.AvatarEditorSheet(
            initialConfig = avatarConfig ?: com.pims.vault.presentation.avatar.PersonaAvatarConfig.default(),
            sheetState = modalSheets.avatarEditorSheetState,
            avatarManager = avatarManager,
            onDismissRequest = { hideSheet("avatarEditor") },
            onSaveAvatar = { newCfg: com.pims.vault.presentation.avatar.PersonaAvatarConfig ->
                avatarManager.saveConfig(newCfg)
                recentActivityManager.recordActivity("Profile Avatar", "Avatar profile configuration updated")
                soundManager.success()
                haptics.success()
                hideSheet("avatarEditor")
                scope.launch {
                    snackbarHostState.showSnackbar("✓ Avatar profile updated")
                }
            }
        )
    }

    // 13. CONTEXTUAL EXPLANATION BOTTOM SHEET (Progressive Disclosure)
    ContextualExplanationSheet(
        explanation = contextualExplanation,
        onDismiss = { contextualExplanation = null }
    )

    // 12. SECURE PIN CHALLENGE DIALOG
    if (accessUi.isPinChallengeVisible && accessUi.pendingSecurityTier != null) {
        PinChallengeDialog(
            pinSecurityManager = pinSecurityManager,
            title = when (accessUi.pendingSecurityTier) {
                SecurityTier.LEVEL_3_VAULT -> "Unlock Vault"
                SecurityTier.LEVEL_2_SENSITIVE -> "Verify Identity"
                else -> "Enter Security PIN"
            },
            subtitle = "Confirm your PIN to access protected records",
            onSuccess = {
                accessUi.isPinChallengeVisible = false
                if (accessUi.pendingSecurityTier == SecurityTier.LEVEL_2_SENSITIVE) {
                    accessUi.isLevel2Unlocked = true
                }
                val action = accessUi.pendingSensitiveAction
                accessUi.pendingSensitiveAction = null
                accessUi.pendingSecurityTier = null
                action?.invoke()
            },
            onDismiss = {
                accessUi.isPinChallengeVisible = false
                accessUi.pendingSensitiveAction = null
                accessUi.pendingSecurityTier = null
                scope.launch {
                    snackbarHostState.showSnackbar("Authentication cancelled: Access locked")
                }
            }
        )
    }

    if (accessUi.isBiometricPromptVisible && accessUi.pendingSecurityTier != null) {
        BiometricReauthPrompt(
            tier = accessUi.pendingSecurityTier!!,
            failedAttempts = accessUi.biometricFailedAttempts,
            onAuthenticateBiometric = {
                accessUi.isBiometricPromptVisible = false
                val tier = accessUi.pendingSecurityTier ?: SecurityTier.LEVEL_2_SENSITIVE
                val action = accessUi.pendingSensitiveAction ?: {}
                requestGatedAccess(tier, "Verify Identity", "Authentication required", action)
            },
            onUseDevicePin = {
                accessUi.isBiometricPromptVisible = false
                if (pinSecurityManager.hasPin()) {
                    accessUi.isPinChallengeVisible = true
                } else {
                    accessUi.pendingSensitiveAction = null
                    accessUi.pendingSecurityTier = null
                    scope.launch {
                        snackbarHostState.showSnackbar("No PIN configured. Setup PIN in Security Settings.")
                    }
                }
            },
            onDismissRequest = {
                accessUi.isBiometricPromptVisible = false
                accessUi.pendingSensitiveAction = null
                accessUi.pendingSecurityTier = null
                scope.launch {
                    snackbarHostState.showSnackbar("Authentication cancelled: Access locked")
                }
            }
        )
    }

    // 13. UNIVERSAL OMNIBAR SEARCH SHEET
    if (sheetStates.value["search"] == true) {
        UniversalSearchSheet(
            sheetState = modalSheets.searchSheetState,
            profileState = profileState,
            documents = documentState.documents,
            medicalState = medicalState,
            vaultState = vaultState,
            onSelectPerson = { person ->
                hideSheet("search")
                relationshipViewModel.onEvent(RelationshipEvent.LoadNotes(person.id))
                navigationUi.selectedPersonForDetail = person
                navigationUi.fullScreenOverlay = "PERSON_DETAIL"
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
                    navigationUi.fullScreenOverlay = "HEALTH"
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
            sheetState = modalSheets.storageSheetState,
            totalDocumentsCount = documentState.documents.size,
            onDismissRequest = { hideSheet("storage") }
        )
    }

    // 15. DATA & BACKUP HUB SHEET
    if (sheetStates.value["dataBackup"] == true) {
        DataBackupHubSheet(
            sheetState = modalSheets.dataBackupSheetState,
            syncIsGood = syncIsGood,
            syncStatusText = syncStatusText,
            onExportBackup = {
                hideSheet("dataBackup")
                navigationUi.fullScreenOverlay = "BACKUP"
            },
            onRestoreBackup = {
                hideSheet("dataBackup")
                navigationUi.fullScreenOverlay = "BACKUP"
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
        onSaved = { first, last, dobVal, occ, countryVal, genderVal, nat, phoneVal, emailVal ->
            firstName = first
            lastName = last
            dob = dobVal
            occupation = occ
            country = countryVal
            gender = genderVal
            nationality = nat
            hideDialog("editProfile")
        },
        onDismissRequest = { hideDialog("editProfile") }
    )

    // Edit Managed Person Dialog (extracted to EditManagedPersonSheet)
    EditManagedPersonSheet(
        person = navigationUi.personToEditForDialog,
        relationshipState = relationshipState,
        profileViewModel = profileViewModel,
        recentActivityManager = recentActivityManager,
        soundManager = soundManager,
        haptics = haptics,
        toastController = toastController,
        onDismissRequest = { navigationUi.personToEditForDialog = null }
    )

    // =========================================================================
    // CRUD DIALOGS FOR PERSONAL ITEMS (extracted to PersonaDashboardDialogs)
    // =========================================================================
    PersonaDashboardDialogs(
        dialogStates = dialogStates.value,
        onHideDialog = { hideDialog(it) },
        country = country,
        customFieldInitialLabel = customFieldUi.customFieldInitialLabel,
        customFieldCategory = customFieldUi.customFieldCategory,
        profileState = profileState,
        profileViewModel = profileViewModel,
        documentViewModel = documentViewModel,
        relationshipToDelete = navigationUi.relationshipToDelete,
        onDismissRelationshipToDelete = { navigationUi.relationshipToDelete = null },
        onConfirmDeleteRelationship = { rel ->
            profileViewModel.onEvent(ProfileEvent.DeleteRelationship(rel.id, rel.targetPersonId))
            recentActivityManager.recordActivity("Removed person from People", "Disconnected ${rel.fullName}")
            navigationUi.relationshipToDelete = null
            navigationUi.selectedPersonForDetail = null
        },
        documentToDelete = documentUi.documentToDelete,
        onDismissDocumentToDelete = { documentUi.documentToDelete = null },
        onConfirmDeleteDocument = { doc -> authorizeDocumentAction(doc) {
            documentViewModel.onEvent(DocumentEvent.DeleteDocument(doc.document.id, onSuccess = {
                recentActivityManager.recordActivity("Removed document from wallet", "Document removed")
                documentUi.documentToDelete = null
            }))
        } },
        recentActivityManager = recentActivityManager,
        soundManager = soundManager,
        haptics = haptics
    )


    // Document import retains the selected source and details until local publication succeeds.
    if (sheetStates.value["uploadDoc"] == true && documentUi.pendingUploadUri != null) {
        DocumentUploadSheet(sheetState = modalSheets.uploadDocSheetState, fileName = documentUi.uploadDocFileName,
            fileSizeBytes = documentUi.uploadDocSizeBytes, mimeType = documentUi.uploadDocMimeType, title = documentUi.uploadDocTitle,
            onTitleChange = { documentUi.uploadDocTitle = it; documentUi.uploadDocError = null }, documentType = documentUi.uploadDocType,
            onDocumentTypeChange = { documentUi.uploadDocType = it }, docNumber = documentUi.uploadDocNumber,
            onDocNumberChange = { documentUi.uploadDocNumber = it }, authority = documentUi.uploadDocAuthority,
            onAuthorityChange = { documentUi.uploadDocAuthority = it }, expiryDate = documentUi.uploadDocExpiryDate,
            onExpiryDateChange = { documentUi.uploadDocExpiryDate = it }, classification = documentUi.uploadDocClassification,
            onClassificationChange = { documentUi.uploadDocClassification = it }, isProcessing = documentUi.isEncryptingAndSavingDoc,
            isAddingVersion = documentUi.versionUploadTarget != null, errorMessage = documentUi.uploadDocError,
            onChangeFileClick = { filePickerLauncher.launch(arrayOf("*/*")) },
            onDismissRequest = { if (!documentUi.isEncryptingAndSavingDoc) { hideSheet("uploadDoc"); clearDocumentDraft() } },
            onConfirmSave = {
                val uri = documentUi.pendingUploadUri
                if (uri != null && documentUi.uploadDocTitle.isNotBlank() && !documentUi.isEncryptingAndSavingDoc) {
                    documentUi.isEncryptingAndSavingDoc = true
                    documentUi.uploadDocError = null
                    val openSource = { context.contentResolver.openInputStream(uri) ?: throw java.io.FileNotFoundException() }
                    val success = {
                        recentActivityManager.recordActivity("Saved document", "Document saved to this device")
                        documentUi.isEncryptingAndSavingDoc = false
                        clearDocumentDraft()
                        hideSheet("uploadDoc")
                    }
                    val failed: (String) -> Unit = { message -> documentUi.isEncryptingAndSavingDoc = false; documentUi.uploadDocError = message }
                    val target = documentUi.versionUploadTarget
                    val save = {
                    if (target != null) documentViewModel.onEvent(DocumentEvent.AddVersion(
                        target.document.id, mimeType = documentUi.uploadDocMimeType, notes = null,
                        originalFilename = documentUi.uploadDocFileName, openStream = openSource, onSuccess = success, onError = failed, versionId = documentUi.uploadDocImportId))
                    else documentViewModel.onEvent(DocumentEvent.IngestDocument(type = documentUi.uploadDocType,
                        title = documentUi.uploadDocTitle.trim(), docNumber = documentUi.uploadDocNumber.trim().takeIf(String::isNotBlank),
                        authority = documentUi.uploadDocAuthority.trim().takeIf(String::isNotBlank), country = country.ifBlank { "ZW" },
                        expiryDate = documentUi.uploadDocExpiryDate.trim().takeIf(String::isNotBlank), classification = documentUi.uploadDocClassification,
                        mimeType = documentUi.uploadDocMimeType, filename = documentUi.uploadDocFileName, openStream = openSource,
                        importId = documentUi.uploadDocImportId, onSuccess = success, onError = failed))
                    }
                    if (documentUi.uploadDocClassification == SecurityClassification.ZONE_4_CRITICAL) {
                        // The system picker can outlive a Zone 4 authorization window.
                        documentUi.isEncryptingAndSavingDoc = false
                        withPrivateNotesUnlocked { documentUi.isEncryptingAndSavingDoc = true; save() }
                    } else save()
                }
            })
    }

    documentState.selectedVersionForPreview?.let { version ->
        com.pims.vault.presentation.document.DocumentPreviewDialog(version = version,
            title = documentState.selectedDocument?.document?.title ?: "Document",
            onDismiss = { documentViewModel.onEvent(DocumentEvent.PreviewVersion(null)) },
            render = { page -> documentViewModel.renderPreview(version, page) },
            onOpenExternal = {
                val selected = documentState.selectedDocument
                documentViewModel.onEvent(DocumentEvent.PreviewVersion(null))
                selected?.let { openDocumentExternally(it.copy(versions = listOf(version))) }
            })
    }

    // 12. Add Custom Information Dialog
    if (dialogStates.value["addCustomField"] == true) {
        DashboardCustomFieldDialog(
            customFieldUi = customFieldUi,
            profileViewModel = profileViewModel,
            onDismiss = { hideDialog("addCustomField") },
            onSaved = {
                recentActivityManager.recordActivity("Saved personal information", "Personal profile")
                haptics.success()
            }
        )
    }

    // 13. Add Medical Condition Dialog
    if (dialogStates.value["addCondition"] == true) {
        DashboardConditionDialog(
            profileViewModel = profileViewModel,
            onDismiss = { hideDialog("addCondition") },
            onSaved = {
                soundManager.success()
                haptics.success()
            }
        )
    }

    // 14. Add Medication Dialog
    if (dialogStates.value["addMedication"] == true) {
        DashboardMedicationDialog(
            profileViewModel = profileViewModel,
            onDismiss = { hideDialog("addMedication") },
            onSaved = {
                soundManager.success()
                haptics.success()
            }
        )
    }

    // 14. Unified Public Dossier & Executive Resume Modal
    if (navigationUi.showPublicResumeModal) {
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
            onDismissRequest = { navigationUi.showPublicResumeModal = false },
            onExportPdf = { handleExportPdf() },
            onEditProfile = {
                showSheet("centralizedEditor")
            }
        )
    }

    attachmentToPreview?.let { attachment ->
        AlertDialog(
            onDismissRequest = { attachmentToPreview = null },
            title = { Text(if (attachment.second == "application/pdf") "PDF preview" else "Image attachment") },
            text = {
                Box(Modifier.fillMaxWidth().heightIn(max = 560.dp), contentAlignment = Alignment.Center) {
                    val bitmap = attachmentPreviewBitmap
                    if (isAttachmentPreviewLoading) CircularProgressIndicator()
                    else if (bitmap == null) Text("This file could not be previewed. It remains encrypted in your vault.")
                    else Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Secure attachment preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
                    )
                }
            },
            confirmButton = { TextButton(onClick = { attachmentToPreview = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun DashboardCustomFieldDialog(
    customFieldUi: DashboardCustomFieldState,
    profileViewModel: ProfileViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var customLabel by remember(customFieldUi.customFieldInitialLabel) { mutableStateOf(customFieldUi.customFieldInitialLabel) }
    var customVal by remember { mutableStateOf(customFieldUi.customFieldInitialValue) }
    val customFieldSaveId = remember { customFieldUi.customFieldEditingId ?: java.util.UUID.randomUUID().toString() }
    var customSaving by remember { mutableStateOf(false) }
    var customSaveError by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember {
        mutableStateOf(InformationCategory.fromName(customFieldUi.customFieldCategory))
    }
    val recognizedField = remember(customLabel) {
        com.pims.vault.core.domain.SemanticFieldRecognizer.resolve(customLabel)
    }
    LaunchedEffect(recognizedField) {
        if (recognizedField != null && (customFieldUi.customFieldEditingId == null || customLabel != customFieldUi.customFieldInitialLabel)) {
            selectedCategory = recognizedField.category
        }
    }

    PersonaDialog(
        onDismissRequest = { if (!customSaving) { onDismiss(); customFieldUi.customFieldEditingId = null; customFieldUi.customFieldInitialValue = "" } },
        title = if (customFieldUi.customFieldEditingId == null) "Add information" else "Edit information",
        confirmText = if (customSaving) "Saving…" else "Save",
        confirmEnabled = !customSaving && customLabel.isNotBlank() && customVal.isNotBlank(),
        onConfirm = {
            if (!customSaving && customLabel.isNotBlank() && customVal.isNotBlank()) {
                customSaving = true
                customSaveError = null
                profileViewModel.onEvent(
                    ProfileEvent.AddCustomField(
                        label = customLabel.trim(), value = customVal.trim(), category = selectedCategory,
                        id = customFieldSaveId,
                        onComplete = { success ->
                            customSaving = false
                            if (success) {
                                onDismiss()
                                customFieldUi.customFieldEditingId = null
                                customFieldUi.customFieldInitialValue = ""
                                onSaved()
                            } else customSaveError = "Could not save this field. Please retry."
                        }
                    )
                )
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            customSaveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PersonaTextInput(
                value = customLabel,
                onValueChange = { customLabel = it },
                label = "Field name",
                placeholder = "e.g. Student ID",
                enabled = !customSaving
            )
            PersonaTextInput(
                value = customVal,
                onValueChange = { customVal = it },
                label = "Value",
                enabled = !customSaving
            )
            PersonaDropdownSelector(
                selectedOption = selectedCategory.displayName,
                options = InformationCategory.entries.map { it.displayName },
                onOptionSelected = { label ->
                    if (!customSaving) selectedCategory = InformationCategory.entries.first { it.displayName == label }
                },
                label = "Category",
                enabled = !customSaving
            )
        }
    }
}

@Composable
private fun DashboardConditionDialog(
    profileViewModel: ProfileViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var conditionName by remember { mutableStateOf("") }
    var conditionNotes by remember { mutableStateOf("") }
    var conditionStatus by remember { mutableStateOf(ConditionStatus.ACTIVE) }

    val statusOptions = listOf("Active", "Managed", "Resolved")

    PersonaDialog(
        onDismissRequest = { onDismiss() },
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
                onSaved()
            }
            onDismiss()
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

@Composable
private fun DashboardMedicationDialog(
    profileViewModel: ProfileViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var medName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    PersonaDialog(
        onDismissRequest = { onDismiss() },
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
                onSaved()
            }
            onDismiss()
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
