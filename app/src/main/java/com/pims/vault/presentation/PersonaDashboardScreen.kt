package com.pims.vault.presentation

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
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

fun isRomanticOrMaritalRelationship(role: String): Boolean {
    val clean = role.trim().lowercase()
    return clean in setOf(
        "wife", "husband", "spouse", "partner",
        "fiancé", "fiancée", "fiance", "fiancee",
        "lover", "married partner", "romantic partner"
    )
}

object DateHelper {
    fun formatBirthdayInfo(dob: String): String {
        if (dob.isBlank()) return ""
        return try {
            val parts = if (dob.contains("-")) dob.split("-") else dob.split("/")
            if (parts.size != 3) return "🎂 DOB: $dob"
            val (year, month, day) = if (parts[0].length == 4) {
                Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else {
                Triple(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }

            val today = Calendar.getInstance()
            val birthCal = Calendar.getInstance().apply {
                set(year, month - 1, day)
            }
            var age = today.get(Calendar.YEAR) - year
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthName = monthNames.getOrElse(month - 1) { "$month" }

            val nextBirthday = Calendar.getInstance().apply {
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                if (before(today)) {
                    add(Calendar.YEAR, 1)
                }
            }
            val diffMillis = nextBirthday.timeInMillis - today.timeInMillis
            val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

            val countdown = when {
                diffDays == 0 -> "Today! 🎉"
                diffDays == 1 -> "Tomorrow"
                diffDays < 30 -> "in $diffDays days"
                else -> "in ${diffDays / 30} months"
            }

            val dayStr = day.toString().padStart(2, '0')
            val monthStr = month.toString().padStart(2, '0')
            "🎂 $dayStr/$monthStr/$year ($monthName $day) • $age yrs old ($countdown)"
        } catch (e: Exception) {
            "🎂 DOB: $dob"
        }
    }

    fun formatDobOnly(dob: String): String {
        if (dob.isBlank()) return ""
        return try {
            val parts = if (dob.contains("-")) dob.split("-") else dob.split("/")
            if (parts.size != 3) return dob
            val (year, month, day) = if (parts[0].length == 4) {
                Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else {
                Triple(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }
            val today = Calendar.getInstance()
            val birthCal = Calendar.getInstance().apply {
                set(year, month - 1, day)
            }
            var age = today.get(Calendar.YEAR) - year
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            val dayStr = day.toString().padStart(2, '0')
            val monthStr = month.toString().padStart(2, '0')
            "$dayStr/$monthStr/$year ($age yrs old)"
        } catch (e: Exception) {
            dob
        }
    }

    fun formatBirthdayCountdown(dob: String): String {
        if (dob.isBlank()) return ""
        return try {
            val parts = if (dob.contains("-")) dob.split("-") else dob.split("/")
            if (parts.size != 3) return ""
            val (year, month, day) = if (parts[0].length == 4) {
                Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else {
                Triple(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }
            val today = Calendar.getInstance()
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthName = monthNames.getOrElse(month - 1) { "$month" }

            val nextBirthday = Calendar.getInstance().apply {
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                if (before(today)) {
                    add(Calendar.YEAR, 1)
                }
            }
            val diffMillis = nextBirthday.timeInMillis - today.timeInMillis
            val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

            val countdown = when {
                diffDays == 0 -> "Today! 🎉"
                diffDays == 1 -> "Tomorrow"
                diffDays < 30 -> "in $diffDays days"
                else -> "in ${diffDays / 30} months"
            }
            "$monthName $day ($countdown)"
        } catch (e: Exception) {
            ""
        }
    }

    fun formatAnniversaryInfo(anniversary: String): String {
        if (anniversary.isBlank()) return ""
        return try {
            val parts = if (anniversary.contains("-")) anniversary.split("-") else anniversary.split("/")
            if (parts.size != 3) return "💍 Anniversary: $anniversary"
            val (year, month, day) = if (parts[0].length == 4) {
                Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else {
                Triple(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }
            val today = Calendar.getInstance()
            val years = today.get(Calendar.YEAR) - year
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthName = monthNames.getOrElse(month - 1) { "$month" }
            val dayStr = day.toString().padStart(2, '0')
            val monthStr = month.toString().padStart(2, '0')
            if (years > 0) {
                "💍 $dayStr/$monthStr/$year ($years yrs married)"
            } else {
                "💍 $dayStr/$monthStr/$year"
            }
        } catch (e: Exception) {
            "💍 Anniversary: $anniversary"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaDashboardScreen(
    securityLevel: KeySecurityLevel,
    onLockClicked: () -> Unit,
    onSignOutClicked: () -> Unit = onLockClicked,
    onRequestBiometricAuth: ((title: String, subtitle: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    documentViewModel: DocumentViewModel = hiltViewModel(),
    sharingViewModel: SharingViewModel = hiltViewModel(),
    vaultViewModel: VaultViewModel = hiltViewModel(),
    medicalViewModel: MedicalViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
    syncViewModel: SyncViewModel = hiltViewModel(),
    relationshipViewModel: RelationshipViewModel = hiltViewModel()
) {
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
    val isLocalOnly = accountMode == AccountMode.LOCAL_ONLY
    val effectiveSyncStatusText = if (isLocalOnly) "Local only" else if (syncState is SyncState.Idle) "✓ Synced" else syncState.label
    val effectiveSyncIsGood = if (isLocalOnly) true else (syncState is SyncState.Idle || syncState is SyncState.Syncing)

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
    val avatarManager = remember { com.pims.vault.presentation.avatar.PersonaAvatarManager(context) }
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

    val defaultCountry = remember { java.util.Locale.getDefault().displayCountry.ifBlank { "Country" } }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("") }
    var country by remember { mutableStateOf(defaultCountry) }
    var gender by remember { mutableStateOf("") }
    var sexuality by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }

    LaunchedEffect(profileState.person) {
        profileState.person?.let { p ->
            firstName = p.firstName
            lastName = p.lastName
            dob = p.dateOfBirth ?: ""
            nationality = p.nationality ?: ""
            country = p.countryOfResidence ?: defaultCountry
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
                    val list = mutableListOf<LibraryItem>()

                    // 1. Personal Identity & Profile attributes
                    profileState.person?.let { p ->
                        val pFullName = listOf(p.firstName, p.lastName).filter { it.isNotBlank() }.joinToString(" ")
                        if (pFullName.isNotBlank()) {
                            list.add(
                                LibraryItem(
                                    id = "personal_name_${p.id}",
                                    title = "Legal Name",
                                    subtitle = pFullName,
                                    category = InformationCategory.PERSONAL,
                                    sensitivity = InformationSensitivity.NORMAL,
                                    personName = "Me",
                                    updatedAt = p.updatedAt
                                )
                            )
                        }
                        p.dateOfBirth?.takeIf { it.isNotBlank() }?.let { bday ->
                            list.add(
                                LibraryItem(
                                    id = "personal_dob_${p.id}",
                                    title = "Date of Birth",
                                    subtitle = bday,
                                    category = InformationCategory.PERSONAL,
                                    sensitivity = InformationSensitivity.PRIVATE,
                                    personName = "Me",
                                    updatedAt = p.updatedAt
                                )
                            )
                        }
                        p.nationality?.takeIf { it.isNotBlank() }?.let { nat ->
                            list.add(
                                LibraryItem(
                                    id = "personal_nat_${p.id}",
                                    title = "Nationality",
                                    subtitle = nat,
                                    category = InformationCategory.PERSONAL,
                                    sensitivity = InformationSensitivity.NORMAL,
                                    personName = "Me",
                                    updatedAt = p.updatedAt
                                )
                            )
                        }
                        p.countryOfResidence?.takeIf { it.isNotBlank() }?.let { res ->
                            list.add(
                                LibraryItem(
                                    id = "personal_res_${p.id}",
                                    title = "Country of Residence",
                                    subtitle = res,
                                    category = InformationCategory.PERSONAL,
                                    sensitivity = InformationSensitivity.NORMAL,
                                    personName = "Me",
                                    updatedAt = p.updatedAt
                                )
                            )
                        }
                        p.gender?.takeIf { it.isNotBlank() }?.let { gen ->
                            list.add(
                                LibraryItem(
                                    id = "personal_gen_${p.id}",
                                    title = "Gender",
                                    subtitle = gen,
                                    category = InformationCategory.PERSONAL,
                                    sensitivity = InformationSensitivity.NORMAL,
                                    personName = "Me",
                                    updatedAt = p.updatedAt
                                )
                            )
                        }
                        p.occupation?.takeIf { it.isNotBlank() }?.let { occ ->
                            list.add(
                                LibraryItem(
                                    id = "personal_occ_${p.id}",
                                    title = "Occupation",
                                    subtitle = occ,
                                    category = InformationCategory.EMPLOYMENT,
                                    sensitivity = InformationSensitivity.NORMAL,
                                    personName = "Me",
                                    updatedAt = p.updatedAt
                                )
                            )
                        }
                    }

                    // 2. Contact details
                    profileState.contacts.forEach { c ->
                        val isPhone = c.contactType == ContactType.PHONE
                        list.add(
                            LibraryItem(
                                id = c.id,
                                title = if (isPhone) "Phone (${c.label})" else "Email (${c.label})",
                                subtitle = c.value,
                                category = InformationCategory.CONTACT,
                                sensitivity = InformationSensitivity.PRIVATE,
                                personName = "Me"
                            )
                        )
                    }

                    // 3. Addresses
                    profileState.addresses.forEach { a ->
                        val fullAddr = listOf(a.streetLine1, a.streetLine2, a.city, a.stateProvince, a.postalCode, a.country).filter { !it.isNullOrBlank() }.joinToString(", ")
                        list.add(
                            LibraryItem(
                                id = a.id,
                                title = "Address (${a.label.name.lowercase().replaceFirstChar { it.uppercase() }})",
                                subtitle = fullAddr,
                                category = InformationCategory.ADDRESS,
                                sensitivity = InformationSensitivity.PRIVATE,
                                personName = "Me"
                            )
                        )
                    }

                    // 4. Custom fields
                    profileState.customFields.forEach { cf ->
                        list.add(
                            LibraryItem(
                                id = cf.id,
                                title = cf.label,
                                subtitle = cf.value,
                                category = cf.category,
                                sensitivity = cf.sensitivity,
                                personName = "Me"
                            )
                        )
                    }

                    // 5. Documents
                    documentState.documents.forEach { d ->
                        list.add(
                            LibraryItem(
                                id = d.document.id,
                                title = d.document.title,
                                subtitle = d.document.documentType.name.replace('_', ' '),
                                category = InformationCategory.DOCUMENT,
                                sensitivity = InformationSensitivity.PROTECTED,
                                personName = "Me",
                                updatedAt = d.document.updatedAt
                            )
                        )
                    }

                    // 6. Passwords & Cards from Vault
                    vaultState.vaultItems.forEach { v ->
                        val isCard = v.category.name.contains("CARD", ignoreCase = true) || v.category.name.contains("PAYMENT", ignoreCase = true) || v.category.name.contains("BANK", ignoreCase = true)
                        list.add(
                            LibraryItem(
                                id = v.id,
                                title = v.title,
                                subtitle = v.accountIdentifier ?: if (isCard) "Payment Card" else "Vault Record",
                                category = if (isCard) InformationCategory.FINANCIAL else InformationCategory.PASSWORD,
                                sensitivity = InformationSensitivity.HIGHLY_PROTECTED,
                                personName = "Me",
                                secretValue = null,
                                updatedAt = v.updatedAt
                            )
                        )
                    }

                    // 7. Education & Certificates
                    profileState.educationRecords.forEach { edu ->
                        list.add(
                            LibraryItem(
                                id = edu.id,
                                title = edu.institution,
                                subtitle = "${edu.qualification} • ${edu.fieldOfStudy ?: "Graduated"}",
                                category = InformationCategory.EDUCATION,
                                sensitivity = InformationSensitivity.NORMAL,
                                personName = "Me"
                            )
                        )
                    }
                    profileState.certificates.forEach { cert ->
                        list.add(
                            LibraryItem(
                                id = cert.id,
                                title = cert.qualification,
                                subtitle = "${cert.institution} • ${cert.fieldOfStudy ?: "Certified"}",
                                category = InformationCategory.EDUCATION,
                                sensitivity = InformationSensitivity.NORMAL,
                                personName = "Me"
                            )
                        )
                    }

                    // 8. Employment records
                    profileState.employmentRecords.forEach { emp ->
                        list.add(
                            LibraryItem(
                                id = emp.id,
                                title = emp.company,
                                subtitle = "${emp.position} • ${emp.startDate} - ${emp.endDate ?: "Present"}",
                                category = InformationCategory.EMPLOYMENT,
                                sensitivity = InformationSensitivity.NORMAL,
                                personName = "Me"
                            )
                        )
                    }

                    // 9. Social Accounts
                    profileState.socialAccounts.forEach { s ->
                        list.add(
                            LibraryItem(
                                id = s.id,
                                title = s.platform,
                                subtitle = s.username ?: s.url,
                                category = InformationCategory.SOCIAL,
                                sensitivity = InformationSensitivity.NORMAL,
                                personName = "Me"
                            )
                        )
                    }

                    // 10. Medical Records
                    medicalState.dossier?.conditions?.forEach { cond ->
                        list.add(
                            LibraryItem(
                                id = cond.id,
                                title = "Condition: ${cond.name}",
                                subtitle = "Diagnosed: ${cond.diagnosedDate ?: "Recorded"} • Severity: ${cond.severity.displayLabel}",
                                category = InformationCategory.MEDICAL,
                                sensitivity = InformationSensitivity.PROTECTED,
                                personName = "Me"
                            )
                        )
                    }
                    medicalState.dossier?.allergies?.forEach { a ->
                        list.add(
                            LibraryItem(
                                id = a.id,
                                title = "Allergy: ${a.allergen}",
                                subtitle = "Severity: ${a.severity.displayLabel} • Reaction: ${a.reaction ?: "Recorded"}",
                                category = InformationCategory.MEDICAL,
                                sensitivity = InformationSensitivity.PROTECTED,
                                personName = "Me"
                            )
                        )
                    }
                    medicalState.dossier?.medications?.forEach { m ->
                        list.add(
                            LibraryItem(
                                id = m.id,
                                title = "Medication: ${m.name}",
                                subtitle = "Dosage: ${m.dosage} • Frequency: ${m.frequency}",
                                category = InformationCategory.MEDICAL,
                                sensitivity = InformationSensitivity.PROTECTED,
                                personName = "Me"
                            )
                        )
                    }

                    // 11. Relationships
                    profileState.relationships.forEach { rel ->
                        list.add(
                            LibraryItem(
                                id = rel.id,
                                title = rel.fullName,
                                subtitle = rel.relationRole,
                                category = InformationCategory.RELATIONSHIP,
                                sensitivity = InformationSensitivity.PRIVATE,
                                personName = rel.fullName
                            )
                        )
                    }
                    list
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
                0 -> {
                    FloatingActionButton(
                        onClick = {
                            haptics.medium()
                            soundManager.navigation()
                            showSheet("addAction")
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .padding(bottom = 68.dp)
                            .tactilePress()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Action", modifier = Modifier.size(26.dp))
                    }
                }
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                    shadowElevation = 6.dp,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FloatingDockItem(
                            selected = selectedTab == 0,
                            onClick = {
                                if (selectedTab != 0) {
                                    haptics.light()
                                    selectedTab = 0
                                }
                            },
                            icon = Icons.Default.Home,
                            label = "Home"
                        )
                        FloatingDockItem(
                            selected = selectedTab == 1,
                            onClick = {
                                if (selectedTab != 1) {
                                    haptics.light()
                                    selectedTab = 1
                                }
                            },
                            icon = Icons.Default.Description,
                            label = "Notes"
                        )
                        FloatingDockItem(
                            selected = selectedTab == 2,
                            onClick = {
                                if (selectedTab != 2) {
                                    haptics.light()
                                    selectedTab = 2
                                }
                            },
                            icon = Icons.Default.Person,
                            label = "Me"
                        )
                        FloatingDockItem(
                            selected = selectedTab == 3,
                            onClick = {
                                if (selectedTab != 3) {
                                    haptics.light()
                                    selectedTab = 3
                                }
                            },
                            icon = Icons.Default.People,
                            label = "People"
                        )
                        FloatingDockItem(
                            selected = selectedTab == 4,
                            onClick = {
                                if (selectedTab != 4) {
                                    haptics.light()
                                    selectedTab = 4
                                }
                            },
                            icon = Icons.Default.Settings,
                            label = "Settings"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Top inset is applied per-tab (Home bleeds wallpaper under the status
        // bar; the other tabs pad themselves below it).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = innerPadding.calculateBottomPadding()
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
                            val localPhotoPath = idPhotoUri?.let { uri ->
                                try {
                                    val destFile = java.io.File(context.filesDir, "identity_id_photo_${System.currentTimeMillis()}.jpg")
                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                        destFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    destFile.absolutePath
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            profileViewModel.onEvent(
                                ProfileEvent.SaveIdentityDetails(
                                    fullName = name,
                                    email = email,
                                    phone = phone,
                                    nationalId = idNum,
                                    idPhotoPath = localPhotoPath
                                )
                            )
                            haptics.success()
                            soundManager.success()
                        },
                        documents = documentState.documents,
                        syncStatusText = syncStatusText,
                        syncIsGood = syncIsGood,
                        onSyncClick = {
                            soundManager.navigation()
                            if (conflicts.isNotEmpty()) {
                                showSheet("conflict")
                            } else {
                                syncViewModel.processSyncNow()
                                scope.launch {
                                    snackbarHostState.showSnackbar(if (isLocalOnly) "Operating in local storage mode" else "Vault sync initiated")
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
                            soundManager.navigation()
                            if (conflicts.isNotEmpty()) {
                                showSheet("conflict")
                            } else {
                                syncViewModel.processSyncNow()
                                scope.launch {
                                    snackbarHostState.showSnackbar(if (isLocalOnly) "Operating in local storage mode" else "Vault sync initiated")
                                }
                            }
                        },
                        onShareProfileClick = {
                            soundManager.navigation()
                            showSheet("share")
                        },
                        onEditProfileClick = { showDialog("editProfile") },
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
                .filter { it.contactType == ContactType.PHONE }
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
                .filter { it.contactType == ContactType.EMAIL }
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
            profileState.addresses.map {
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
                val localPhotoPath = photoUri?.let { uri ->
                    try {
                        val destFile = java.io.File(context.filesDir, "identity_id_photo_${System.currentTimeMillis()}.jpg")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        destFile.absolutePath
                    } catch (_: Exception) {
                        null
                    }
                } ?: profileState.idPhotoPath

                val primaryPhoneVal = phonesList.firstOrNull { it.isPrimary }?.number
                    ?: phonesList.firstOrNull()?.number ?: ""
                val primaryEmailVal = emailsList.firstOrNull { it.isPrimary }?.address
                    ?: emailsList.firstOrNull()?.address ?: ""

                profileViewModel.onEvent(
                    ProfileEvent.SaveIdentityDetails(
                        fullName = name,
                        email = primaryEmailVal,
                        phone = primaryPhoneVal,
                        nationalId = natId,
                        idPhotoPath = localPhotoPath
                    )
                )

                if (name.isNotBlank()) {
                    firstName = name.substringBefore(" ")
                    lastName = name.substringAfter(" ", "")
                }
                if (occ.isNotBlank()) occupation = occ
                if (ctry.isNotBlank()) country = ctry

                profileViewModel.onEvent(
                    ProfileEvent.SavePersonalDetails(
                        firstName = firstName,
                        lastName = lastName,
                        dob = dob,
                        nationality = nationality,
                        country = country,
                        gender = gender,
                        sexuality = sexuality,
                        bloodGroup = bloodGroup
                    )
                )

                phonesList.drop(1).forEach { p ->
                    if (p.number.isNotBlank()) {
                        profileViewModel.onEvent(ProfileEvent.AddPhone(p.number.trim(), p.label, p.isPrimary))
                    }
                }

                emailsList.drop(1).forEach { e ->
                    if (e.address.isNotBlank()) {
                        profileViewModel.onEvent(ProfileEvent.AddEmail(e.address.trim(), e.label, e.isPrimary))
                    }
                }

                addrsList.forEach { a ->
                    if (a.street1.isNotBlank()) {
                        profileViewModel.onEvent(
                            ProfileEvent.AddAddress(
                                street1 = a.street1.trim(),
                                street2 = a.street2.trim().ifBlank { null },
                                city = a.city.trim(),
                                stateProvince = a.state.trim().ifBlank { null },
                                postalCode = a.postalCode.trim().ifBlank { null },
                                country = a.country.trim().ifBlank { country }
                            )
                        )
                    }
                }

                allergiesList.forEach { al ->
                    if (al.allergen.isNotBlank()) {
                        val sev = try {
                            AllergySeverity.valueOf(al.severity.uppercase())
                        } catch (_: Exception) {
                            AllergySeverity.MODERATE
                        }
                        profileViewModel.onEvent(
                            ProfileEvent.AddAllergy(
                                allergen = al.allergen.trim(),
                                severity = sev,
                                reaction = al.reaction.trim().ifBlank { null }
                            )
                        )
                    }
                }

                conditionsList.forEach { cd ->
                    if (cd.name.isNotBlank()) {
                        profileViewModel.onEvent(
                            ProfileEvent.AddMedicalCondition(
                                condition = cd.name.trim(),
                                status = try { ConditionStatus.valueOf(cd.status.uppercase()) } catch (_: Exception) { ConditionStatus.ACTIVE },
                                notes = cd.diagnosedDate.trim().ifBlank { null }
                            )
                        )
                    }
                }

                medsList.forEach { md ->
                    if (md.name.isNotBlank()) {
                        profileViewModel.onEvent(
                            ProfileEvent.AddMedication(
                                name = md.name.trim(),
                                dosage = md.dosage.trim(),
                                frequency = md.frequency.trim(),
                                notes = null
                            )
                        )
                    }
                }

                edusList.forEach { ed ->
                    if (ed.institution.isNotBlank() || ed.qualification.isNotBlank()) {
                        profileViewModel.onEvent(
                            ProfileEvent.AddQualification(
                                institution = ed.institution.trim(),
                                qualification = ed.qualification.trim(),
                                fieldOfStudy = ed.fieldOfStudy.trim().ifBlank { null },
                                startDate = ed.year.trim().ifBlank { null },
                                endDate = null,
                                grade = null,
                                description = null
                            )
                        )
                    }
                }

                worksList.forEach { wk ->
                    if (wk.company.isNotBlank() || wk.position.isNotBlank()) {
                        profileViewModel.onEvent(
                            ProfileEvent.AddWorkHistory(
                                company = wk.company.trim(),
                                position = wk.position.trim(),
                                department = null,
                                location = null,
                                startDate = wk.startDate.trim().ifBlank { null },
                                endDate = wk.endDate.trim().ifBlank { null },
                                isCurrent = wk.isCurrent,
                                responsibilities = null
                            )
                        )
                    }
                }

                socialsList.forEach { sc ->
                    if (sc.usernameOrUrl.isNotBlank()) {
                        profileViewModel.onEvent(
                            ProfileEvent.AddSocialAccount(
                                platform = sc.platform.trim(),
                                username = sc.usernameOrUrl.trim(),
                                url = sc.usernameOrUrl.trim()
                            )
                        )
                    }
                }

                customList.forEach { cf ->
                    if (cf.label.isNotBlank() && cf.value.isNotBlank()) {
                        profileViewModel.onEvent(
                            ProfileEvent.AddCustomField(
                                label = cf.label.trim(),
                                value = cf.value.trim()
                            )
                        )
                    }
                }

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
    // EDIT PERSONAL IDENTITY BOTTOM SHEET
    // =========================================================================
    if (dialogStates.value["editProfile"] == true) {
        var editFirst by remember { mutableStateOf(firstName) }
        var editLast by remember { mutableStateOf(lastName) }
        var editPhone by remember(primaryPhone) { mutableStateOf(primaryPhone ?: "") }
        var editEmail by remember(primaryEmail) { mutableStateOf(primaryEmail ?: "") }
        var editDob by remember { mutableStateOf(dob) }
        var editNationality by remember { mutableStateOf(nationality) }
        var editCountry by remember { mutableStateOf(country) }
        var editGender by remember { mutableStateOf(gender) }
        var editOcc by remember { mutableStateOf(occupation) }
        val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { hideDialog("editProfile") },
            sheetState = editSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit personal identity",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { hideDialog("editProfile") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Preferred / Legal Names
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PersonaTextInput(
                        value = editFirst,
                        onValueChange = { editFirst = it },
                        label = "Legal first name",
                        placeholder = "First name",
                        modifier = Modifier.weight(1f)
                    )

                    PersonaTextInput(
                        value = editLast,
                        onValueChange = { editLast = it },
                        label = "Last name",
                        placeholder = "Last name",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Phone Number
                InternationalPhoneInput(
                    value = editPhone,
                    onValueChange = { editPhone = it },
                    label = "Phone number"
                )

                // Email Address
                PersonaTextInput(
                    value = editEmail,
                    onValueChange = { editEmail = it },
                    label = "Email address",
                    placeholder = "name@example.com",
                    keyboardType = KeyboardType.Email
                )

                // Date of Birth
                com.pims.vault.presentation.ui.components.StandardDateInput(
                    isoDate = editDob,
                    onDateChange = { editDob = it },
                    label = "Date of birth"
                )

                // Nationality
                PersonaTextInput(
                    value = editNationality,
                    onValueChange = { editNationality = it },
                    label = "Nationality",
                    placeholder = "e.g. Nationality"
                )

                // Country of residence
                PersonaTextInput(
                    value = editCountry,
                    onValueChange = { editCountry = it },
                    label = "Country of residence",
                    placeholder = "e.g. Country"
                )

                // Occupation
                PersonaTextInput(
                    value = editOcc,
                    onValueChange = { editOcc = it },
                    label = "Occupation",
                    placeholder = "e.g. Occupation / Profession"
                )

                // Gender
                PersonaTextInput(
                    value = editGender,
                    onValueChange = { editGender = it },
                    label = "Gender",
                    placeholder = "e.g. Female, Male, Non-binary"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Actions: Save changes & Cancel
                Button(
                    onClick = {
                        firstName = editFirst
                        lastName = editLast
                        dob = editDob
                        occupation = editOcc
                        country = editCountry
                        gender = editGender
                        nationality = editNationality

                        profileViewModel.onEvent(
                            ProfileEvent.SavePersonalDetails(
                                firstName = editFirst,
                                lastName = editLast,
                                dob = editDob,
                                nationality = editNationality,
                                country = editCountry,
                                gender = editGender,
                                sexuality = sexuality,
                                bloodGroup = bloodGroup
                            )
                        )
                        profileViewModel.savePrimaryPhone(editPhone)
                        profileViewModel.savePrimaryEmail(editEmail)
                        recentActivityManager.recordActivity("Updated personal information", "Profile name & details updated")
                        haptics.success()
                        soundManager.success()
                        toastController.showSuccess("Personal identity saved")
                        hideDialog("editProfile")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .tactilePress(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Save changes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                TextButton(
                    onClick = { hideDialog("editProfile") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Edit Managed Person Dialog
    personToEditForDialog?.let { person ->
        var editFirstName by remember(person) { mutableStateOf(person.fullName.substringBefore(" ")) }
        var editLastName by remember(person) { mutableStateOf(person.fullName.substringAfter(" ", "")) }
        var editRole by remember(person) { mutableStateOf(person.relationRole) }
        var editPhones by remember(person) {
            mutableStateOf(
                person.phone.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .ifEmpty { listOf("") }
            )
        }
        var editEmails by remember(person) {
            mutableStateOf(
                person.email.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .ifEmpty { listOf("") }
            )
        }
        var editAddress by remember(person) { mutableStateOf(person.address) }
        var editDob by remember(person) { mutableStateOf(person.dateOfBirth) }
        var editAnniversary by remember(person) { mutableStateOf(person.anniversary) }
        var editNotesList by remember(person) {
            val existingNotes = person.notes.split("\n\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val resolvedNotes = if (existingNotes.isNotEmpty()) {
                existingNotes
            } else {
                relationshipState.relationshipNotes
                    .map { it.content.trim() }
                    .filter { it.isNotEmpty() }
            }
            mutableStateOf(resolvedNotes.ifEmpty { listOf("") })
        }
        val editManagedPersonSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { personToEditForDialog = null },
            sheetState = editManagedPersonSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Breadcrumb
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PEOPLE → EDIT MANAGED PROFILE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Edit ${person.fullName}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { personToEditForDialog = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PersonaTextInput(
                        value = editFirstName,
                        onValueChange = { editFirstName = it },
                        label = "First name",
                        modifier = Modifier.weight(1f)
                    )
                    PersonaTextInput(
                        value = editLastName,
                        onValueChange = { editLastName = it },
                        label = "Last name",
                        modifier = Modifier.weight(1f)
                    )
                }

                PersonaSearchableCombobox(
                    value = editRole,
                    onValueChange = { editRole = it },
                    options = listOf("Mother", "Father", "Sister", "Brother", "Spouse", "Wife", "Husband", "Partner", "Daughter", "Son", "Child", "Uncle", "Aunt", "Cousin", "Grandmother", "Grandfather", "Grandchild", "In-law", "Friend", "Next of Kin", "Mentor", "Colleague"),
                    label = "Relationship (e.g. Sibling, Mother, Friend)",
                    placeholder = "Type or select relationship...",
                    allowCustom = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Phone Numbers
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Phone Numbers", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        TextButton(onClick = { editPhones = editPhones + "" }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Phone", fontSize = 12.sp)
                        }
                    }
                    editPhones.forEachIndexed { idx, phone ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                InternationalPhoneInput(
                                    value = phone,
                                    onValueChange = { newVal ->
                                        editPhones = editPhones.toMutableList().also { it[idx] = newVal }
                                    },
                                    label = if (idx == 0) "Primary Phone" else "Phone ${idx + 1}"
                                )
                            }
                            if (editPhones.size > 1) {
                                IconButton(
                                    onClick = {
                                        editPhones = editPhones.toMutableList().also { it.removeAt(idx) }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                // Email Addresses
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Email Addresses", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        TextButton(onClick = { editEmails = editEmails + "" }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Email", fontSize = 12.sp)
                        }
                    }
                    editEmails.forEachIndexed { idx, email ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                PersonaTextInput(
                                    value = email,
                                    onValueChange = { newVal ->
                                        editEmails = editEmails.toMutableList().also { it[idx] = newVal }
                                    },
                                    label = if (idx == 0) "Primary Email" else "Email ${idx + 1}",
                                    placeholder = "name@example.com",
                                    keyboardType = KeyboardType.Email
                                )
                            }
                            if (editEmails.size > 1) {
                                IconButton(
                                    onClick = {
                                        editEmails = editEmails.toMutableList().also { it.removeAt(idx) }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                PersonaTextInput(
                    value = editAddress,
                    onValueChange = { editAddress = it },
                    label = "Location / Address",
                    modifier = Modifier.fillMaxWidth()
                )

                com.pims.vault.presentation.ui.components.StandardDateInput(
                    isoDate = editDob,
                    onDateChange = { editDob = it },
                    label = "Date of Birth"
                )

                AnimatedVisibility(visible = isRomanticOrMaritalRelationship(editRole)) {
                    com.pims.vault.presentation.ui.components.StandardDateInput(
                        isoDate = editAnniversary,
                        onDateChange = { editAnniversary = it },
                        label = "Anniversary"
                    )
                }

                // Private Memory Notes
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Private Memory Notes", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        TextButton(onClick = { editNotesList = editNotesList + "" }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Note", fontSize = 12.sp)
                        }
                    }
                    editNotesList.forEachIndexed { idx, noteItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                PersonaTextInput(
                                    value = noteItem,
                                    onValueChange = { newVal ->
                                        editNotesList = editNotesList.toMutableList().also { it[idx] = newVal }
                                    },
                                    label = if (idx == 0) "Note" else "Note ${idx + 1}",
                                    singleLine = false
                                )
                            }
                            if (editNotesList.size > 1) {
                                IconButton(
                                    onClick = {
                                        editNotesList = editNotesList.toMutableList().also { it.removeAt(idx) }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        val savedPhone = editPhones.map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
                        val savedEmail = editEmails.map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
                        val enteredNotes = editNotesList.map { it.trim() }.filter { it.isNotBlank() }.joinToString("\n\n")
                        val savedNotes = enteredNotes.ifBlank { person.notes.trim() }

                        profileViewModel.onEvent(
                            ProfileEvent.UpdatePersonDetails(
                                personId = person.targetPersonId,
                                firstName = editFirstName.ifBlank { person.fullName },
                                lastName = editLastName,
                                relationRole = editRole.ifBlank { person.relationRole },
                                phone = savedPhone,
                                email = savedEmail,
                                address = editAddress,
                                dob = editDob,
                                anniversary = editAnniversary,
                                notes = savedNotes
                            )
                        )
                        recentActivityManager.recordActivity("Updated ${person.fullName}'s profile")
                        soundManager.success()
                        haptics.selection()
                        scope.launch {
                            snackbarHostState.showSnackbar("✓ Updated ${person.fullName}'s details")
                        }
                        personToEditForDialog = null
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { personToEditForDialog = null },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }

    // =========================================================================
    // CRUD DIALOGS FOR PERSONAL ITEMS
    // =========================================================================

    // 1. Add Phone Dialog
    if (dialogStates.value["addPhone"] == true) {
        var newPhone by remember { mutableStateOf("") }
        var phoneLabel by remember { mutableStateOf("Personal") }
        var isPrimary by remember { mutableStateOf(true) }

        val phoneTypes = listOf("Personal", "Work", "Home", "School", "Other")

        PersonaDialog(
            onDismissRequest = { hideDialog("addPhone") },
            title = "Add Phone Number",
            confirmText = "Save Phone",
            confirmEnabled = newPhone.isNotBlank(),
            onConfirm = {
                if (newPhone.isNotBlank()) {
                    profileViewModel.onEvent(ProfileEvent.AddPhone(newPhone.trim(), phoneLabel, isPrimary))
                }
                hideDialog("addPhone")
            }
        ) {
            InternationalPhoneInput(
                value = newPhone,
                onValueChange = { newPhone = it },
                label = "Phone number"
            )

            PersonaDropdownSelector(
                selectedOption = phoneLabel,
                options = phoneTypes,
                onOptionSelected = { phoneLabel = it },
                label = "Type"
            )

            PersonaToggleRow(
                title = "Set as primary phone",
                checked = isPrimary,
                onCheckedChange = { isPrimary = it }
            )
        }
    }

    // 2. Add Email Dialog
    if (dialogStates.value["addEmail"] == true) {
        var newEmail by remember { mutableStateOf("") }
        var emailLabel by remember { mutableStateOf("Personal") }
        var isPrimary by remember { mutableStateOf(false) }

        val emailTypes = listOf("Personal", "Work", "School", "Other")

        PersonaDialog(
            onDismissRequest = { hideDialog("addEmail") },
            title = "Add Email Address",
            confirmText = "Save Email",
            confirmEnabled = newEmail.isNotBlank(),
            onConfirm = {
                if (newEmail.isNotBlank()) {
                    profileViewModel.onEvent(ProfileEvent.AddEmail(newEmail.trim(), emailLabel, isPrimary))
                }
                hideDialog("addEmail")
            }
        ) {
            PersonaTextInput(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = "Email address",
                placeholder = "name@example.com",
                keyboardType = KeyboardType.Email
            )

            PersonaDropdownSelector(
                selectedOption = emailLabel,
                options = emailTypes,
                onOptionSelected = { emailLabel = it },
                label = "Type"
            )

            PersonaToggleRow(
                title = "Set as primary email",
                checked = isPrimary,
                onCheckedChange = { isPrimary = it }
            )
        }
    }

    // 3. Add Address Dialog
    if (dialogStates.value["addAddress"] == true) {
        var st1 by remember { mutableStateOf("") }
        var st2 by remember { mutableStateOf("") }
        var cityVal by remember { mutableStateOf("") }
        var stateVal by remember { mutableStateOf("") }
        var zipVal by remember { mutableStateOf("") }
        var countryVal by remember { mutableStateOf(country) }

        PersonaDialog(
            onDismissRequest = { hideDialog("addAddress") },
            title = "Add Physical Address",
            confirmText = "Save Address",
            confirmEnabled = st1.isNotBlank(),
            onConfirm = {
                if (st1.isNotBlank()) {
                    profileViewModel.onEvent(ProfileEvent.AddAddress(st1.trim(), st2.trim(), cityVal.trim(), stateVal.trim(), zipVal.trim(), countryVal.trim()))
                }
                hideDialog("addAddress")
            }
        ) {
            PersonaTextInput(value = st1, onValueChange = { st1 = it }, label = "Street address", placeholder = "Street address / Suburb")
            PersonaTextInput(value = st2, onValueChange = { st2 = it }, label = "Directions / Line 2 (Optional)", placeholder = "Apt, Suite, Unit, etc.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PersonaTextInput(value = cityVal, onValueChange = { cityVal = it }, label = "City / Town", modifier = Modifier.weight(1f))
                PersonaTextInput(value = stateVal, onValueChange = { stateVal = it }, label = "Province / State", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PersonaTextInput(value = zipVal, onValueChange = { zipVal = it }, label = "Postal code", modifier = Modifier.weight(1f))
                PersonaTextInput(value = countryVal, onValueChange = { countryVal = it }, label = "Country", modifier = Modifier.weight(1f))
            }
        }
    }

    // 4. Add Work Experience Dialog
    if (dialogStates.value["addWork"] == true) {
        var company by remember { mutableStateOf("") }
        var position by remember { mutableStateOf("") }
        var startYear by remember { mutableStateOf("") }
        var endYear by remember { mutableStateOf("") }
        var isCurrent by remember { mutableStateOf(false) }
        var responsibilities by remember { mutableStateOf("") }

        PersonaDialog(
            onDismissRequest = { hideDialog("addWork") },
            title = "Add Experience / Project",
            confirmText = "Save",
            confirmEnabled = company.isNotBlank() && position.isNotBlank(),
            onConfirm = {
                if (company.isNotBlank() && position.isNotBlank()) {
                    profileViewModel.onEvent(ProfileEvent.AddWorkHistory(company.trim(), position.trim(), null, null, startYear.trim(), endYear.trim(), isCurrent, responsibilities.trim()))
                }
                hideDialog("addWork")
            }
        ) {
            PersonaTextInput(value = company, onValueChange = { company = it }, label = "Company or organisation", placeholder = "e.g. Acme Corp")
            PersonaTextInput(value = position, onValueChange = { position = it }, label = "Role / project title", placeholder = "e.g. Lead Designer")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PersonaTextInput(value = startYear, onValueChange = { startYear = it }, label = "Start year", placeholder = "e.g. 2022", modifier = Modifier.weight(1f))
                PersonaTextInput(value = endYear, onValueChange = { endYear = it }, label = "End year", placeholder = if (isCurrent) "Present" else "e.g. 2026", modifier = Modifier.weight(1f), readOnly = isCurrent)
            }
            PersonaToggleRow(
                title = "Current role / Active project",
                checked = isCurrent,
                onCheckedChange = { isCurrent = it }
            )
            PersonaTextInput(value = responsibilities, onValueChange = { responsibilities = it }, label = "Description", placeholder = "Technologies used, key responsibilities", singleLine = false)
        }
    }

    // 5. Add Qualification Dialog
    if (dialogStates.value["addEdu"] == true) {
        var institution by remember { mutableStateOf("") }
        var qualification by remember { mutableStateOf("") }
        var field by remember { mutableStateOf("") }
        var year by remember { mutableStateOf("") }

        PersonaDialog(
            onDismissRequest = { hideDialog("addEdu") },
            title = "Add Qualification",
            confirmText = "Save",
            confirmEnabled = institution.isNotBlank() && qualification.isNotBlank(),
            onConfirm = {
                if (institution.isNotBlank() && qualification.isNotBlank()) {
                    profileViewModel.onEvent(ProfileEvent.AddQualification(institution.trim(), qualification.trim(), field.trim(), null, year.trim(), null, null))
                }
                hideDialog("addEdu")
            }
        ) {
            PersonaTextInput(value = institution, onValueChange = { institution = it }, label = "Institution / University", placeholder = "e.g. Oxford University")
            PersonaTextInput(value = qualification, onValueChange = { qualification = it }, label = "Degree / Qualification", placeholder = "e.g. BSc")
            PersonaTextInput(value = field, onValueChange = { field = it }, label = "Field of study", placeholder = "e.g. Computer Science")
            PersonaTextInput(value = year, onValueChange = { year = it }, label = "Year completed / expected", placeholder = "e.g. 2024")
        }
    }

    // 6. Add Certificate Dialog
    if (dialogStates.value["addCert"] == true) {
        var certTitle by remember { mutableStateOf("") }
        var issuer by remember { mutableStateOf("") }
        var dateVal by remember { mutableStateOf("") }
        var credId by remember { mutableStateOf("") }

        PersonaDialog(
            onDismissRequest = { hideDialog("addCert") },
            title = "Add Certificate",
            confirmText = "Save",
            confirmEnabled = certTitle.isNotBlank() && issuer.isNotBlank(),
            onConfirm = {
                if (certTitle.isNotBlank() && issuer.isNotBlank()) {
                    profileViewModel.onEvent(ProfileEvent.AddCertificate(certTitle.trim(), issuer.trim(), dateVal.trim(), null, credId.trim(), null))
                }
                hideDialog("addCert")
            }
        ) {
            PersonaTextInput(value = certTitle, onValueChange = { certTitle = it }, label = "Certificate name", placeholder = "e.g. CCNA")
            PersonaTextInput(value = issuer, onValueChange = { issuer = it }, label = "Issuing body", placeholder = "e.g. Cisco")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PersonaTextInput(value = dateVal, onValueChange = { dateVal = it }, label = "Date", placeholder = "YYYY-MM", modifier = Modifier.weight(1f))
                PersonaTextInput(value = credId, onValueChange = { credId = it }, label = "Credential ID", placeholder = "ID (optional)", modifier = Modifier.weight(1f))
            }
        }
    }

    // 7. Add Allergy Dialog
    if (dialogStates.value["addAllergy"] == true) {
        var allergen by remember { mutableStateOf("") }
        var reaction by remember { mutableStateOf("") }

        PersonaDialog(
            onDismissRequest = { hideDialog("addAllergy") },
            title = "Add Allergy",
            confirmText = "Save",
            confirmEnabled = allergen.isNotBlank(),
            onConfirm = {
                if (allergen.isNotBlank()) {
                    profileViewModel.onEvent(ProfileEvent.AddAllergy(allergen.trim(), AllergySeverity.MODERATE, reaction.trim()))
                }
                hideDialog("addAllergy")
            }
        ) {
            PersonaTextInput(value = allergen, onValueChange = { allergen = it }, label = "Allergen", placeholder = "e.g. Penicillin")
            PersonaTextInput(value = reaction, onValueChange = { reaction = it }, label = "Reaction / notes", placeholder = "e.g. Mild rash")
        }
    }

    // 8. Add Relationship / Relative Dialog
    if (dialogStates.value["addRelationship"] == true) {
        val userGenderStr = profileState.person?.gender?.uppercase() ?: "MALE"
        val oppositeIsFemale = !(userGenderStr.startsWith("F") || userGenderStr.contains("FEMALE"))

        var relRole by remember { mutableStateOf("Mother") }
        var personIsFemale by remember { mutableStateOf(true) }
        var relName by remember { mutableStateOf("") }
        var relPhones by remember { mutableStateOf(listOf("")) }
        var relEmails by remember { mutableStateOf(listOf("")) }
        var relAddress by remember { mutableStateOf("") }
        var relDob by remember { mutableStateOf("") }
        var relAnniversary by remember { mutableStateOf("") }
        var relNotesList by remember { mutableStateOf(listOf("")) }
        var isNok by remember { mutableStateOf(false) }

        val relRoles = listOf(
            "Mother", "Father", "Sister", "Brother", "Spouse", "Wife", "Husband", "Partner",
            "Daughter", "Son", "Child", "Uncle", "Aunt", "Cousin", "Grandmother", "Grandfather",
            "Grandchild", "In-law", "Friend", "Next of Kin", "Mentor", "Colleague", "Neighbor",
            "Guardian", "Doctor", "Lawyer"
        )

        // Function to update role and pre-set inferenced gender
        fun onRoleSelected(role: String) {
            relRole = role
            when (role.trim().lowercase()) {
                "mother", "sister", "aunt", "wife", "daughter", "grandmother", "niece" -> {
                    personIsFemale = true
                }
                "father", "brother", "uncle", "husband", "son", "grandfather", "nephew" -> {
                    personIsFemale = false
                }
                "spouse", "partner" -> {
                    personIsFemale = oppositeIsFemale
                }
                else -> {}
            }
        }

        val effectiveRole = relRole.trim().ifBlank { "Connected Person" }

        PersonaDialog(
            onDismissRequest = { hideDialog("addRelationship") },
            title = "Connect Person",
            confirmText = "Save Person",
            confirmEnabled = relName.isNotBlank() && relRole.isNotBlank(),
            onConfirm = {
                if (relName.isNotBlank()) {
                    val savedPhone = relPhones.map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
                    val savedEmail = relEmails.map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
                    val savedNotes = relNotesList.map { it.trim() }.filter { it.isNotBlank() }.joinToString("\n\n")

                    profileViewModel.onEvent(
                        ProfileEvent.AddRelationship(
                            role = effectiveRole,
                            fullName = relName.trim(),
                            phone = savedPhone,
                            email = savedEmail,
                            address = relAddress.trim(),
                            dob = relDob.trim(),
                            anniversary = relAnniversary.trim(),
                            notes = savedNotes,
                            isNextOfKin = isNok
                        )
                    )
                    recentActivityManager.recordActivity("Added ${relName.trim()} to People", "Connected as $effectiveRole")
                }
                hideDialog("addRelationship")
            }
        ) {
            // Section 1: Relationship & Identity
            PersonaFormSection(
                title = "Relationship & Identity",
                icon = Icons.Default.Person
            ) {
                PersonaSearchableCombobox(
                    label = "How are you connected?",
                    value = relRole,
                    options = relRoles,
                    onValueChange = { onRoleSelected(it) },
                    placeholder = "Search or type relation (e.g. Mother, Godmother)...",
                    allowCustom = true
                )

                PersonaDropdownSelector(
                    label = "Person's Gender",
                    selectedOption = if (personIsFemale) "Female" else "Male",
                    options = listOf("Female", "Male"),
                    onOptionSelected = { label ->
                        personIsFemale = label == "Female"
                    }
                )

                PersonaTextInput(
                    value = relName,
                    onValueChange = { relName = it },
                    label = "Full name *",
                    placeholder = "Full name"
                )
            }

            // Section 2: Contact Details
            PersonaFormSection(
                title = "Contact Information",
                icon = Icons.Default.Phone
            ) {
                // Phone Numbers
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Phone Numbers", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        TextButton(onClick = { relPhones = relPhones + "" }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Phone", fontSize = 12.sp)
                        }
                    }
                    relPhones.forEachIndexed { idx, phone ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                InternationalPhoneInput(
                                    value = phone,
                                    onValueChange = { newVal ->
                                        relPhones = relPhones.toMutableList().also { it[idx] = newVal }
                                    },
                                    label = if (idx == 0) "Primary Phone" else "Phone ${idx + 1}"
                                )
                            }
                            if (relPhones.size > 1) {
                                IconButton(
                                    onClick = {
                                        relPhones = relPhones.toMutableList().also { it.removeAt(idx) }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                // Email Addresses
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Email Addresses", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        TextButton(onClick = { relEmails = relEmails + "" }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Email", fontSize = 12.sp)
                        }
                    }
                    relEmails.forEachIndexed { idx, email ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                PersonaTextInput(
                                    value = email,
                                    onValueChange = { newVal ->
                                        relEmails = relEmails.toMutableList().also { it[idx] = newVal }
                                    },
                                    label = if (idx == 0) "Primary Email" else "Email ${idx + 1}",
                                    placeholder = "name@example.com",
                                    keyboardType = KeyboardType.Email
                                )
                            }
                            if (relEmails.size > 1) {
                                IconButton(
                                    onClick = {
                                        relEmails = relEmails.toMutableList().also { it.removeAt(idx) }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                PersonaTextInput(
                    value = relAddress,
                    onValueChange = { relAddress = it },
                    label = "Address",
                    placeholder = "Physical address"
                )
            }

            // Section 3: Important Dates & Notes
            PersonaFormSection(
                title = "Dates & Notes",
                icon = Icons.Default.CalendarToday
            ) {
                com.pims.vault.presentation.ui.components.StandardDateInput(
                    isoDate = relDob,
                    onDateChange = { relDob = it },
                    label = "Birthday"
                )
                AnimatedVisibility(visible = isRomanticOrMaritalRelationship(relRole) || isRomanticOrMaritalRelationship(effectiveRole)) {
                    com.pims.vault.presentation.ui.components.StandardDateInput(
                        isoDate = relAnniversary,
                        onDateChange = { relAnniversary = it },
                        label = "Anniversary"
                    )
                }
                // Private notes
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Private Notes", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        TextButton(onClick = { relNotesList = relNotesList + "" }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Note", fontSize = 12.sp)
                        }
                    }
                    relNotesList.forEachIndexed { idx, noteItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                PersonaTextInput(
                                    value = noteItem,
                                    onValueChange = { newVal ->
                                        relNotesList = relNotesList.toMutableList().also { it[idx] = newVal }
                                    },
                                    label = if (idx == 0) "Private notes" else "Note ${idx + 1}",
                                    placeholder = "e.g. Likes gardening, allergies, memories",
                                    singleLine = false
                                )
                            }
                            if (relNotesList.size > 1) {
                                IconButton(
                                    onClick = {
                                        relNotesList = relNotesList.toMutableList().also { it.removeAt(idx) }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                PersonaToggleRow(
                    title = "Mark as Next of Kin / ICE",
                    checked = isNok,
                    onCheckedChange = { isNok = it }
                )
            }
        }
    }

    // 9. Delete Relationship Dialog
    relationshipToDelete?.let { rel ->
        PersonaDialog(
            onDismissRequest = { relationshipToDelete = null },
            title = "Disconnect Person",
            confirmText = "Disconnect",
            isDestructive = true,
            onConfirm = {
                profileViewModel.onEvent(ProfileEvent.DeleteRelationship(rel.id, rel.targetPersonId))
                recentActivityManager.recordActivity("Removed person from People", "Disconnected ${rel.fullName}")
                relationshipToDelete = null
                selectedPersonForDetail = null
            }
        ) {
            Text("Are you sure you want to remove ${rel.relationRole} (${rel.fullName})? Notes will be deleted.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    // 10. Document Delete Confirmation Dialog
    documentToDelete?.let { doc ->
        PersonaDialog(
            onDismissRequest = { documentToDelete = null },
            title = "Delete Document",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                documentViewModel.onEvent(DocumentEvent.DeleteDocument(doc.document.id))
                recentActivityManager.recordActivity("Removed document from wallet", doc.document.title)
                documentToDelete = null
            }
        ) {
            Text("Permanently remove \"${doc.document.title}\" from encrypted storage?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

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
                showPublicResumeModal = false
                showSheet("centralizedEditor")
            }
        )
    }
}

/**
 * Floating dock bottom navigation item:
 * Capsule indicator with terracotta accent for selected state and muted secondary for unselected.
 */
@Composable
private fun FloatingDockItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    val feedback = rememberPimsFeedback()
    val isReducedMotion = com.pims.vault.presentation.ui.theme.LocalReducedMotion.current
    val activeBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    val animatedBg by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) activeBg else Color.Transparent,
        animationSpec = com.pims.vault.presentation.ui.theme.PersonaMotion.smoothSpring(isReducedMotion),
        label = "dockItemBg"
    )
    val iconTint by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        animationSpec = com.pims.vault.presentation.ui.theme.PersonaMotion.smoothSpring(isReducedMotion),
        label = "dockIconTint"
    )
    val iconScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1.06f else 1.0f,
        animationSpec = com.pims.vault.presentation.ui.theme.PersonaMotion.snappySpring(isReducedMotion),
        label = "dockIconScale"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = animatedBg,
        modifier = Modifier
            .tactilePress(targetScale = 0.94f) {
                feedback.tap()
                onClick()
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier
                    .size(20.dp)
                    .scale(iconScale)
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = androidx.compose.animation.fadeIn() +
                        androidx.compose.animation.expandHorizontally(androidx.compose.animation.core.spring()),
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(100)) +
                        androidx.compose.animation.shrinkHorizontally(androidx.compose.animation.core.tween(100))
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = activeColor
                )
            }
        }
    }
}

