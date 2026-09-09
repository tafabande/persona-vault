package com.pims.vault.presentation

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.pims.vault.presentation.hub.SharePreviewModal
import com.pims.vault.presentation.hub.VaultSheet
import com.pims.vault.presentation.medical.MedicalDossierView
import com.pims.vault.presentation.medical.MedicalViewModel
import com.pims.vault.presentation.profile.CustomField
import com.pims.vault.presentation.profile.KinRelationshipItem
import com.pims.vault.presentation.profile.ProfileEvent
import com.pims.vault.presentation.profile.ProfileViewModel
import com.pims.vault.presentation.sharing.SharingViewModel
import com.pims.vault.presentation.ui.components.FormFieldLabel
import com.pims.vault.presentation.ui.components.FormTextInput
import com.pims.vault.presentation.ui.theme.StateError
import com.pims.vault.presentation.ui.theme.StateSuccess
import com.pims.vault.presentation.vault.VaultDashboardView
import com.pims.vault.presentation.vault.VaultViewModel
import com.pims.vault.core.sync.SyncState
import com.pims.vault.presentation.hub.AccountDeletionDialog
import com.pims.vault.presentation.hub.NotificationCenterSheet
import com.pims.vault.presentation.hub.PersonaNotificationItem
import com.pims.vault.presentation.hub.SessionManagementSheet
import com.pims.vault.presentation.hub.SyncConflictSheet
import com.pims.vault.presentation.sync.SyncViewModel
import com.pims.vault.presentation.ui.state.AlertLevel
import com.pims.vault.core.security.SecurityTier
import com.pims.vault.presentation.security.BiometricReauthPrompt
import com.pims.vault.presentation.security.AccountSecuritySheet
import com.pims.vault.presentation.security.AccountSecurityViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

object DateHelper {
    fun formatBirthdayInfo(dob: String): String {
        if (dob.isBlank()) return ""
        return try {
            val parts = if (dob.contains("-")) dob.split("-") else dob.split("/")
            if (parts.size != 3) return "🎂 DOB: $dob"
            val (year, month, day) = if (parts[0].length == 4) {
                Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else {
                Triple(parts[2].toInt(), parts[0].toInt(), parts[1].toInt())
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

            "🎂 $monthName $day • $age yrs old ($countdown)"
        } catch (e: Exception) {
            "🎂 DOB: $dob"
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
                Triple(parts[2].toInt(), parts[0].toInt(), parts[1].toInt())
            }
            val today = Calendar.getInstance()
            val years = today.get(Calendar.YEAR) - year
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthName = monthNames.getOrElse(month - 1) { "$month" }
            if (years > 0) {
                "💍 $monthName $day ($years yrs married)"
            } else {
                "💍 $monthName $day"
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
    profileViewModel: ProfileViewModel = hiltViewModel(),
    documentViewModel: DocumentViewModel = hiltViewModel(),
    sharingViewModel: SharingViewModel = hiltViewModel(),
    vaultViewModel: VaultViewModel = hiltViewModel(),
    medicalViewModel: MedicalViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
    syncViewModel: SyncViewModel = hiltViewModel(),
    accountSecurityViewModel: AccountSecurityViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val profileState by profileViewModel.uiState.collectAsState()
    val documentState by documentViewModel.uiState.collectAsState()
    val vaultState by vaultViewModel.uiState.collectAsState()
    val medicalState by medicalViewModel.uiState.collectAsState()
    val syncState by syncViewModel.syncState.collectAsState()
    val conflicts by syncViewModel.unresolvedConflicts.collectAsState()
    val activeSessions by syncViewModel.activeSessions.collectAsState()

    val syncStatusText = syncState.label
    val syncIsGood = syncState is SyncState.Idle || syncState is SyncState.Syncing

    // 4 Bottom Navigation Tabs: 0: Home, 1: Me (Centerpiece), 2: People, 3: More
    var selectedTab by remember { mutableIntStateOf(0) }

    // Full screen overlays (e.g. Full Vault or Backup)
    var fullScreenOverlay by remember { mutableStateOf<String?>(null) }

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

    val fullName = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "My Profile" }

    // Date Picker for Profile DOB
    val cal = Calendar.getInstance()
    val profileDatePicker = DatePickerDialog(
        context,
        { _, y, m, d -> dob = String.format("%04d-%02d-%02d", y, m + 1, d) },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    )

    // Bottom Sheets States
    val addActionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddActionSheet by remember { mutableStateOf(false) }

    val shareModalState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showShareModal by remember { mutableStateOf(false) }

    val eduSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showEduSheet by remember { mutableStateOf(false) }

    val healthSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showHealthSheet by remember { mutableStateOf(false) }

    val docsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDocsSheet by remember { mutableStateOf(false) }

    val vaultSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showVaultSheet by remember { mutableStateOf(false) }

    val personDetailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedPersonForDetail by remember { mutableStateOf<KinRelationshipItem?>(null) }

    val conflictSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showConflictSheet by remember { mutableStateOf(false) }

    val sessionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSessionsSheet by remember { mutableStateOf(false) }

    val notificationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showNotificationSheet by remember { mutableStateOf(false) }

    var showAccountSecuritySheet by remember { mutableStateOf(false) }

    val searchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showUniversalSearchSheet by remember { mutableStateOf(false) }

    val storageSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showStorageSheet by remember { mutableStateOf(false) }

    val dataBackupSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDataBackupSheet by remember { mutableStateOf(false) }

    // Tiered Biometric Verification state for sensitive records (Level 2) and Vault (Level 3)
    var pendingSensitiveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingSecurityTier by remember { mutableStateOf<SecurityTier?>(null) }
    var isBiometricPromptVisible by remember { mutableStateOf(false) }
    var biometricFailedAttempts by remember { mutableIntStateOf(0) }
    var isLevel2Unlocked by remember { mutableStateOf(false) }

    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Document Upload & Delete States
    var showUploadDialog by remember { mutableStateOf(false) }
    var pendingUploadUri by remember { mutableStateOf<Uri?>(null) }
    var uploadDocTitle by remember { mutableStateOf("") }
    var uploadDocType by remember { mutableStateOf(DocumentType.NATIONAL_ID) }
    var documentToDelete by remember { mutableStateOf<DocumentWithHistory?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingUploadUri = uri
            uploadDocTitle = uri.lastPathSegment?.substringAfterLast("/") ?: "Document"
            showUploadDialog = true
        }
    }

    // Dialog States for Dynamic Multi-Item Entries
    var showAddPhoneDialog by remember { mutableStateOf(false) }
    var showAddEmailDialog by remember { mutableStateOf(false) }
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var showAddWorkDialog by remember { mutableStateOf(false) }
    var showAddEduDialog by remember { mutableStateOf(false) }
    var showAddCertDialog by remember { mutableStateOf(false) }
    var showAddAllergyDialog by remember { mutableStateOf(false) }
    var showAddConditionDialog by remember { mutableStateOf(false) }
    var showAddMedicationDialog by remember { mutableStateOf(false) }
    var showAddRelationshipDialog by remember { mutableStateOf(false) }
    var relationshipToDelete by remember { mutableStateOf<KinRelationshipItem?>(null) }

    // Primary contact getters
    val primaryPhone = profileState.contacts.firstOrNull { it.contactType == ContactType.PHONE }?.value
    val primaryEmail = profileState.contacts.firstOrNull { it.contactType == ContactType.EMAIL }?.value
    val primaryAddress = profileState.addresses.firstOrNull()?.let { "${it.streetLine1}, ${it.city}" }

    // Activity & Notification Center Data (Strictly meaningful life/security events, zero spam)
    val notifications = remember(syncState, conflicts, documentState.documents) {
        val list = mutableListOf<PersonaNotificationItem>()

        // 1. Sync Conflicts (Urgent: user reconciliation required)
        if (conflicts.isNotEmpty()) {
            list.add(
                PersonaNotificationItem(
                    id = "conflicts_alert",
                    title = "Sync conflict detected",
                    description = "${conflicts.size} items have conflicting versions across devices.",
                    timestampText = "Action needed",
                    level = AlertLevel.WARNING,
                    icon = Icons.Default.Warning,
                    isNeedsAttention = true
                )
            )
        }

        // 2. Sync Failed (Actionable failure)
        if (syncState is SyncState.Failed) {
            list.add(
                PersonaNotificationItem(
                    id = "sync_err_alert",
                    title = "Sync paused",
                    description = (syncState as SyncState.Failed).message,
                    timestampText = "Attention",
                    level = AlertLevel.DANGER,
                    icon = Icons.Default.ErrorOutline,
                    isNeedsAttention = true
                )
            )
        }

        // 3. Expiring Documents (e.g. Passport, National ID expiring within 60 days)
        val now = System.currentTimeMillis()
        documentState.documents.forEach { docWithHist ->
            val doc = docWithHist.document
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
                                    id = "doc_exp_${doc.id}",
                                    title = "${doc.title} expires soon",
                                    description = "Your ${doc.documentType.name.lowercase().replace('_', ' ')} expires in $diffDays days.",
                                    timestampText = "$diffDays days",
                                    level = AlertLevel.WARNING,
                                    icon = Icons.Default.Warning,
                                    isNeedsAttention = true
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // 4. Honest Background Sync Status (Quiet informational)
        when (syncState) {
            is SyncState.Waiting -> {
                list.add(
                    PersonaNotificationItem(
                        id = "sync_waiting_info",
                        title = "Changes saved locally",
                        description = "${(syncState as SyncState.Waiting).count} updates queued on device, will sync when online.",
                        timestampText = "Saved locally",
                        level = AlertLevel.INFO,
                        icon = Icons.Default.Info,
                        isNeedsAttention = false
                    )
                )
            }
            is SyncState.Idle -> {
                list.add(
                    PersonaNotificationItem(
                        id = "sync_ok_info",
                        title = "Documents & profile synced",
                        description = "Local database is in sync with your private cloud vault.",
                        timestampText = "Just now",
                        level = AlertLevel.SUCCESS,
                        icon = Icons.Default.CheckCircle,
                        isNeedsAttention = false
                    )
                )
            }
            is SyncState.Syncing -> {
                list.add(
                    PersonaNotificationItem(
                        id = "sync_syncing_info",
                        title = "Synchronizing data",
                        description = "Uploading encrypted deltas and downloading remote changes.",
                        timestampText = "In progress",
                        level = AlertLevel.INFO,
                        icon = Icons.Default.Info,
                        isNeedsAttention = false
                    )
                )
            }
            else -> {}
        }

        // 5. Hardware Keystore / Security Health
        list.add(
            PersonaNotificationItem(
                id = "security_hw_info",
                title = "Hardware Keystore active",
                description = "Hardware-backed cryptographic keys bound to this device.",
                timestampText = "Protected",
                level = AlertLevel.SUCCESS,
                icon = Icons.Default.Lock,
                isNeedsAttention = false
            )
        )

        list
    }

    val needsAttentionItems = remember(notifications) {
        notifications.filter { it.isNeedsAttention }
    }

    // If a full-screen overlay is requested (e.g. Full Vault or Backup), render it
    if (fullScreenOverlay != null) {
        when (fullScreenOverlay) {
            "VAULT" -> {
                Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    // Top bar to return to Hub
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { fullScreenOverlay = null }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back to Persona")
                            }
                            Text("Vault Fortress", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        OutlinedButton(onClick = onLockClicked) {
                            Text("Lock")
                        }
                    }
                    VaultDashboardView(
                        viewModel = vaultViewModel,
                        onRequireBiometricReauth = onLockClicked,
                        modifier = Modifier.weight(1f)
                    )
                }
                return
            }
            "BACKUP" -> {
                Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
        }
    }

    // MAIN HUB INTERFACE WITH 4-ITEM BOTTOM NAVIGATION
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(22.dp)) },
                    label = { Text("Home", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.tertiary,
                        selectedTextColor = MaterialTheme.colorScheme.tertiary,
                        indicatorColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Me", modifier = Modifier.size(24.dp)) },
                    label = { Text("Me", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.tertiary,
                        selectedTextColor = MaterialTheme.colorScheme.tertiary,
                        indicatorColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.People, contentDescription = "People", modifier = Modifier.size(22.dp)) },
                    label = { Text("People", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.tertiary,
                        selectedTextColor = MaterialTheme.colorScheme.tertiary,
                        indicatorColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "More", modifier = Modifier.size(22.dp)) },
                    label = { Text("More", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.tertiary,
                        selectedTextColor = MaterialTheme.colorScheme.tertiary,
                        indicatorColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = selectedTab, label = "TabSwitch") { tab ->
                when (tab) {
                    // TAB 0: HOME LANDING SCREEN
                    0 -> HomeView(
                        personName = fullName,
                        occupation = occupation,
                        relationships = profileState.relationships,
                        recentDocumentTitle = documentState.documents.firstOrNull()?.document?.title,
                        syncStatusText = syncStatusText,
                        syncIsGood = syncIsGood,
                        needsAttentionItems = needsAttentionItems,
                        onSyncPillClick = { showConflictSheet = true },
                        onNotificationCenterClick = { showNotificationSheet = true },
                        onAlertClick = { alert ->
                            if (alert.id.startsWith("conflicts")) {
                                showConflictSheet = true
                            } else if (alert.id.startsWith("doc_exp")) {
                                showDocsSheet = true
                            } else {
                                showNotificationSheet = true
                            }
                        },
                        onNavigateToMe = { selectedTab = 1 },
                        onOpenVault = {
                            pendingSecurityTier = SecurityTier.LEVEL_3_VAULT
                            pendingSensitiveAction = { showVaultSheet = true }
                            isBiometricPromptVisible = true
                        },
                        onOpenDocuments = {
                            if (isLevel2Unlocked) {
                                showDocsSheet = true
                            } else {
                                pendingSecurityTier = SecurityTier.LEVEL_2_SENSITIVE
                                pendingSensitiveAction = { showDocsSheet = true }
                                isBiometricPromptVisible = true
                            }
                        },
                        onOpenHealth = {
                            if (isLevel2Unlocked) {
                                showHealthSheet = true
                            } else {
                                pendingSecurityTier = SecurityTier.LEVEL_2_SENSITIVE
                                pendingSensitiveAction = { showHealthSheet = true }
                                isBiometricPromptVisible = true
                            }
                        },
                        onSearchClick = { showUniversalSearchSheet = true },
                        onOpenShare = { showShareModal = true },
                        onAddSomething = { showAddActionSheet = true },
                        onPersonClick = { person -> selectedPersonForDetail = person }
                    )

                    // TAB 1: ME CENTERPIECE PROFILE
                    1 -> MeView(
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
                        syncStatusText = syncStatusText,
                        syncIsGood = syncIsGood,
                        onSyncClick = { showConflictSheet = true },
                        onShareProfileClick = { showShareModal = true },
                        onEditProfileClick = { showEditProfileDialog = true },
                        onEducationClick = { showEduSheet = true },
                        onHealthClick = {
                            if (isLevel2Unlocked) {
                                showHealthSheet = true
                            } else {
                                pendingSecurityTier = SecurityTier.LEVEL_2_SENSITIVE
                                pendingSensitiveAction = { showHealthSheet = true }
                                isBiometricPromptVisible = true
                            }
                        },
                        onVaultClick = {
                            pendingSecurityTier = SecurityTier.LEVEL_3_VAULT
                            pendingSensitiveAction = { showVaultSheet = true }
                            isBiometricPromptVisible = true
                        },
                        onDocumentsClick = {
                            if (isLevel2Unlocked) {
                                showDocsSheet = true
                            } else {
                                pendingSecurityTier = SecurityTier.LEVEL_2_SENSITIVE
                                pendingSensitiveAction = { showDocsSheet = true }
                                isBiometricPromptVisible = true
                            }
                        },
                        onPeopleClick = { selectedTab = 2 },
                        onPhoneClick = { showAddPhoneDialog = true },
                        onEmailClick = { showAddEmailDialog = true },
                        onAddressClick = { showAddAddressDialog = true }
                    )

                    // TAB 2: PEOPLE & CONNECTIONS
                    2 -> PeopleView(
                        relationships = profileState.relationships,
                        onAddPersonClick = { showAddRelationshipDialog = true },
                        onSelectPerson = { person -> selectedPersonForDetail = person }
                    )

                    // TAB 3: MORE & SETTINGS
                    3 -> MoreView(
                        onLockClicked = onLockClicked,
                        onSecurityClicked = { showAccountSecuritySheet = true },
                        onDevicesClicked = { showSessionsSheet = true },
                        onStorageClicked = { showStorageSheet = true },
                        onDataBackupClicked = { showDataBackupSheet = true },
                        onEmergencyCardClicked = {
                            if (isLevel2Unlocked) {
                                showHealthSheet = true
                            } else {
                                pendingSecurityTier = SecurityTier.LEVEL_2_SENSITIVE
                                pendingSensitiveAction = { showHealthSheet = true }
                                isBiometricPromptVisible = true
                            }
                        },
                        onDeleteAccountClicked = { showDeleteAccountDialog = true }
                    )
                }
            }
        }
    }

    // =========================================================================
    // MODAL BOTTOM SHEETS
    // =========================================================================

    // 1. ADD SOMETHING BOTTOM SHEET
    if (showAddActionSheet) {
        AddActionBottomSheet(
            sheetState = addActionSheetState,
            onDismissRequest = { showAddActionSheet = false },
            onSelectAction = { actionType ->
                showAddActionSheet = false
                when (actionType) {
                    AddActionType.PHONE -> showAddPhoneDialog = true
                    AddActionType.EMAIL -> showAddEmailDialog = true
                    AddActionType.ADDRESS -> showAddAddressDialog = true
                    AddActionType.EDUCATION -> showAddEduDialog = true
                    AddActionType.CERTIFICATION -> showAddCertDialog = true
                    AddActionType.EXPERIENCE -> showAddWorkDialog = true
                    AddActionType.DOCUMENT -> filePickerLauncher.launch("*/*")
                    AddActionType.HEALTH_RECORD -> showAddAllergyDialog = true
                    AddActionType.PASSWORD -> { fullScreenOverlay = "VAULT" }
                    AddActionType.PERSON -> showAddRelationshipDialog = true
                }
            }
        )
    }

    // 2. DIGITAL PERSON CARD & SELECTIVE SHARING PREVIEW
    if (showShareModal) {
        SharePreviewModal(
            personName = fullName,
            occupation = occupation,
            country = country,
            sheetState = shareModalState,
            onDismissRequest = { showShareModal = false },
            onCopyShareLink = { url ->
                clipboardManager.setText(AnnotatedString(url))
                scope.launch { snackbarHostState.showSnackbar("Share link copied: $url") }
                showShareModal = false
            }
        )
    }

    // 3. EDUCATION & PORTFOLIO FACET SHEET
    if (showEduSheet) {
        EducationPortfolioSheet(
            educationList = profileState.educationRecords,
            certificatesList = profileState.certificates,
            employmentList = profileState.employmentRecords,
            sheetState = eduSheetState,
            onDismissRequest = { showEduSheet = false },
            onAddEducation = { showAddEduDialog = true },
            onAddCert = { showAddCertDialog = true },
            onAddProject = { showAddWorkDialog = true }
        )
    }

    // 4. HEALTH FACET SHEET
    if (showHealthSheet) {
        HealthSheet(
            medicalRecords = profileState.medicalRecords,
            bloodGroup = bloodGroup,
            sheetState = healthSheetState,
            onDismissRequest = { showHealthSheet = false },
            onAddRecord = { showAddAllergyDialog = true }
        )
    }

    // 5. DOCUMENTS WALLET SHEET
    if (showDocsSheet) {
        DocumentsWalletSheet(
            documents = documentState.documents,
            sheetState = docsSheetState,
            onDismissRequest = { showDocsSheet = false },
            onUploadClick = { filePickerLauncher.launch("*/*") },
            onDeleteClick = { doc -> documentToDelete = doc }
        )
    }

    // 6. VAULT FORTRESS PREVIEW SHEET
    if (showVaultSheet) {
        VaultSheet(
            sheetState = vaultSheetState,
            onDismissRequest = { showVaultSheet = false },
            onOpenFullVault = { fullScreenOverlay = "VAULT" }
        )
    }

    // 7. PERSON DETAIL SHEET
    selectedPersonForDetail?.let { person ->
        PersonDetailSheet(
            person = person,
            sheetState = personDetailSheetState,
            onDismissRequest = { selectedPersonForDetail = null },
            onDeletePerson = { p -> relationshipToDelete = p }
        )
    }

    // 8. SYNC CONFLICT RESOLUTION SHEET
    if (showConflictSheet) {
        SyncConflictSheet(
            conflicts = conflicts,
            sheetState = conflictSheetState,
            onDismissRequest = { showConflictSheet = false },
            onResolveConflict = { id, choice, resVal ->
                syncViewModel.resolveConflict(id, choice, resVal)
            }
        )
    }

    // 9. ACTIVE SESSIONS MANAGEMENT SHEET
    if (showSessionsSheet) {
        SessionManagementSheet(
            sessions = activeSessions,
            sheetState = sessionsSheetState,
            onDismissRequest = { showSessionsSheet = false },
            onRevokeSession = { sessionId ->
                syncViewModel.revokeSession(sessionId)
            },
            onRevokeAllOtherSessions = {
                syncViewModel.revokeAllOtherSessions()
            }
        )
    }

    // 10. ACCOUNT DELETION DIALOG
    if (showDeleteAccountDialog) {
        AccountDeletionDialog(
            documentsCount = documentState.documents.size,
            relationshipsCount = profileState.relationships.size,
            medicalCount = profileState.medicalRecords.size,
            vaultAccountsCount = vaultState.vaultItems.size,
            onDismissRequest = { showDeleteAccountDialog = false },
            onConfirmDelete = {
                showDeleteAccountDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Persona account wiped.")
                }
                onLockClicked()
            }
        )
    }

    // 11. ACCOUNT SECURITY & TWO-TIER PASSWORD MANAGEMENT SHEET
    if (showAccountSecuritySheet) {
        AccountSecuritySheet(
            securityManager = accountSecurityViewModel.securityManager,
            sessionManager = accountSecurityViewModel.sessionManager,
            onDismissRequest = { showAccountSecuritySheet = false }
        )
    }

    // 11. NOTIFICATIONS & ACTIVITY SHEET
    if (showNotificationSheet) {
        NotificationCenterSheet(
            notifications = notifications,
            sheetState = notificationSheetState,
            onDismissRequest = { showNotificationSheet = false },
            onNotificationClick = { item ->
                showNotificationSheet = false
                if (item.id.startsWith("conflicts")) {
                    showConflictSheet = true
                } else if (item.id.startsWith("doc_exp")) {
                    showDocsSheet = true
                }
            }
        )
    }

    // 12. TIERED BIOMETRIC / CREDENTIAL REAUTHENTICATION PROMPT
    if (isBiometricPromptVisible && pendingSecurityTier != null) {
        BiometricReauthPrompt(
            tier = pendingSecurityTier!!,
            failedAttempts = biometricFailedAttempts,
            onAuthenticateBiometric = {
                biometricFailedAttempts = 0
                isBiometricPromptVisible = false
                if (pendingSecurityTier == SecurityTier.LEVEL_2_SENSITIVE) {
                    isLevel2Unlocked = true
                }
                pendingSensitiveAction?.invoke()
                pendingSensitiveAction = null
            },
            onUseDevicePin = {
                biometricFailedAttempts = 0
                isBiometricPromptVisible = false
                if (pendingSecurityTier == SecurityTier.LEVEL_2_SENSITIVE) {
                    isLevel2Unlocked = true
                }
                pendingSensitiveAction?.invoke()
                pendingSensitiveAction = null
            },
            onUseMasterPassword = {
                biometricFailedAttempts = 0
                isBiometricPromptVisible = false
                if (pendingSecurityTier == SecurityTier.LEVEL_2_SENSITIVE) {
                    isLevel2Unlocked = true
                }
                pendingSensitiveAction?.invoke()
                pendingSensitiveAction = null
            },
            onDismissRequest = {
                isBiometricPromptVisible = false
                pendingSensitiveAction = null
            }
        )
    }

    // 13. UNIVERSAL OMNIBAR SEARCH SHEET
    if (showUniversalSearchSheet) {
        UniversalSearchSheet(
            sheetState = searchSheetState,
            profileState = profileState,
            documents = documentState.documents,
            medicalState = medicalState,
            vaultState = vaultState,
            onSelectPerson = { person ->
                showUniversalSearchSheet = false
                selectedPersonForDetail = person
            },
            onSelectDocument = { doc ->
                showUniversalSearchSheet = false
                showDocsSheet = true
            },
            onOpenHealth = {
                showUniversalSearchSheet = false
                if (isLevel2Unlocked) {
                    showHealthSheet = true
                } else {
                    pendingSecurityTier = SecurityTier.LEVEL_2_SENSITIVE
                    pendingSensitiveAction = { showHealthSheet = true }
                    isBiometricPromptVisible = true
                }
            },
            onOpenVault = {
                showUniversalSearchSheet = false
                pendingSecurityTier = SecurityTier.LEVEL_3_VAULT
                pendingSensitiveAction = { showVaultSheet = true }
                isBiometricPromptVisible = true
            },
            onDismissRequest = { showUniversalSearchSheet = false }
        )
    }

    // 14. STORAGE MANAGEMENT SHEET (3-Layer Progressive Disclosure)
    if (showStorageSheet) {
        StorageManagementSheet(
            sheetState = storageSheetState,
            totalDocumentsCount = documentState.documents.size,
            onDismissRequest = { showStorageSheet = false }
        )
    }

    // 15. DATA & BACKUP HUB SHEET
    if (showDataBackupSheet) {
        DataBackupHubSheet(
            sheetState = dataBackupSheetState,
            syncIsGood = syncIsGood,
            syncStatusText = syncStatusText,
            onExportBackup = {
                showDataBackupSheet = false
                fullScreenOverlay = "BACKUP"
            },
            onRestoreBackup = {
                showDataBackupSheet = false
                fullScreenOverlay = "BACKUP"
            },
            onOpenSyncConflicts = {
                showDataBackupSheet = false
                showConflictSheet = true
            },
            onDismissRequest = { showDataBackupSheet = false }
        )
    }

    // =========================================================================
    // CONVERSATIONAL PROFILE EDIT DIALOG
    // =========================================================================
    if (showEditProfileDialog) {
        var editFirst by remember { mutableStateOf(firstName) }
        var editLast by remember { mutableStateOf(lastName) }
        var editDob by remember { mutableStateOf(dob) }
        var editCountry by remember { mutableStateOf(country) }
        var editGender by remember { mutableStateOf(gender) }
        var editOcc by remember { mutableStateOf(occupation) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = "Edit Personal Identity",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        FormFieldLabel("What should we call you?", isRequired = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FormTextInput(value = editFirst, onValueChange = { editFirst = it }, subCaption = "First name", modifier = Modifier.weight(1f))
                            FormTextInput(value = editLast, onValueChange = { editLast = it }, subCaption = "Last name", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        FormFieldLabel("When were you born?", isRequired = false)
                        FormTextInput(
                            value = editDob,
                            onValueChange = { editDob = it },
                            placeholder = "YYYY-MM-DD",
                            subCaption = "Date of Birth",
                            trailingIcon = {
                                IconButton(onClick = { profileDatePicker.show() }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                    item {
                        FormFieldLabel("Occupation & Profession", isRequired = false)
                        FormTextInput(value = editOcc, onValueChange = { editOcc = it }, subCaption = "e.g. Telecommunications Engineering")
                    }
                    item {
                        FormFieldLabel("Country of Residence", isRequired = false)
                        FormTextInput(value = editCountry, onValueChange = { editCountry = it }, subCaption = "e.g. Country of residence")
                    }
                    item {
                        FormFieldLabel("Gender Representation", isRequired = false)
                        FormTextInput(value = editGender, onValueChange = { editGender = it }, subCaption = "Male, Female, or Other")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        firstName = editFirst
                        lastName = editLast
                        dob = editDob
                        occupation = editOcc
                        country = editCountry
                        gender = editGender

                        profileViewModel.onEvent(
                            ProfileEvent.SavePersonalDetails(
                                firstName = editFirst,
                                lastName = editLast,
                                dob = editDob,
                                nationality = nationality,
                                country = editCountry,
                                gender = editGender,
                                sexuality = sexuality,
                                bloodGroup = bloodGroup
                            )
                        )
                        showEditProfileDialog = false
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Identity")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // =========================================================================
    // CRUD DIALOGS FOR PERSONAL ITEMS
    // =========================================================================

    // 1. Add Phone Dialog
    if (showAddPhoneDialog) {
        var newPhone by remember { mutableStateOf("") }
        var phoneLabel by remember { mutableStateOf("Personal") }
        var isPrimary by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddPhoneDialog = false },
            title = { Text("Add Phone Number") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormTextInput(value = newPhone, onValueChange = { newPhone = it }, subCaption = "Phone Number (e.g. +263 77 123 4567)", keyboardType = KeyboardType.Phone)
                    FormTextInput(value = phoneLabel, onValueChange = { phoneLabel = it }, subCaption = "Label (e.g. Personal, Work)")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPrimary, onCheckedChange = { isPrimary = it })
                        Text("Set as primary phone", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPhone.isNotBlank()) {
                            profileViewModel.onEvent(ProfileEvent.AddPhone(newPhone, phoneLabel, isPrimary))
                        }
                        showAddPhoneDialog = false
                    }
                ) { Text("Save Phone") }
            },
            dismissButton = { TextButton(onClick = { showAddPhoneDialog = false }) { Text("Cancel") } }
        )
    }

    // 2. Add Email Dialog
    if (showAddEmailDialog) {
        var newEmail by remember { mutableStateOf("") }
        var emailLabel by remember { mutableStateOf("Personal") }
        var isPrimary by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddEmailDialog = false },
            title = { Text("Add Email Address") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormTextInput(value = newEmail, onValueChange = { newEmail = it }, subCaption = "Email Address", keyboardType = KeyboardType.Email)
                    FormTextInput(value = emailLabel, onValueChange = { emailLabel = it }, subCaption = "Label (e.g. Personal, Work)")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPrimary, onCheckedChange = { isPrimary = it })
                        Text("Set as primary email", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newEmail.isNotBlank()) {
                            profileViewModel.onEvent(ProfileEvent.AddEmail(newEmail, emailLabel, isPrimary))
                        }
                        showAddEmailDialog = false
                    }
                ) { Text("Save Email") }
            },
            dismissButton = { TextButton(onClick = { showAddEmailDialog = false }) { Text("Cancel") } }
        )
    }

    // 3. Add Address Dialog
    if (showAddAddressDialog) {
        var st1 by remember { mutableStateOf("") }
        var st2 by remember { mutableStateOf("") }
        var cityVal by remember { mutableStateOf("") }
        var stateVal by remember { mutableStateOf("") }
        var zipVal by remember { mutableStateOf("") }
        var countryVal by remember { mutableStateOf(country) }

        AlertDialog(
            onDismissRequest = { showAddAddressDialog = false },
            title = { Text("Add Physical Address") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormTextInput(value = st1, onValueChange = { st1 = it }, subCaption = "Street Address / Suburb")
                    FormTextInput(value = st2, onValueChange = { st2 = it }, subCaption = "Directions / Line 2 (Optional)")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormTextInput(value = cityVal, onValueChange = { cityVal = it }, subCaption = "City / Town", modifier = Modifier.weight(1f))
                        FormTextInput(value = stateVal, onValueChange = { stateVal = it }, subCaption = "Province", modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormTextInput(value = zipVal, onValueChange = { zipVal = it }, subCaption = "Postal Code", modifier = Modifier.weight(1f))
                        FormTextInput(value = countryVal, onValueChange = { countryVal = it }, subCaption = "Country", modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (st1.isNotBlank()) {
                            profileViewModel.onEvent(ProfileEvent.AddAddress(st1, st2, cityVal, stateVal, zipVal, countryVal))
                        }
                        showAddAddressDialog = false
                    }
                ) { Text("Save Address") }
            },
            dismissButton = { TextButton(onClick = { showAddAddressDialog = false }) { Text("Cancel") } }
        )
    }

    // 4. Add Work Experience Dialog
    if (showAddWorkDialog) {
        var company by remember { mutableStateOf("") }
        var position by remember { mutableStateOf("") }
        var startYear by remember { mutableStateOf("") }
        var endYear by remember { mutableStateOf("") }
        var isCurrent by remember { mutableStateOf(false) }
        var responsibilities by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddWorkDialog = false },
            title = { Text("Add Experience / Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormTextInput(value = company, onValueChange = { company = it }, subCaption = "Company or Organisation")
                    FormTextInput(value = position, onValueChange = { position = it }, subCaption = "Role / Project Title")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormTextInput(value = startYear, onValueChange = { startYear = it }, subCaption = "Start (e.g. 2022)", modifier = Modifier.weight(1f))
                        FormTextInput(value = endYear, onValueChange = { endYear = it }, subCaption = "End (e.g. 2026)", modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isCurrent, onCheckedChange = { isCurrent = it })
                        Text("Current role / Active project", fontSize = 13.sp)
                    }
                    FormTextInput(value = responsibilities, onValueChange = { responsibilities = it }, subCaption = "Description / Technologies used")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (company.isNotBlank() && position.isNotBlank()) {
                            profileViewModel.onEvent(ProfileEvent.AddWorkHistory(company, position, null, null, startYear, endYear, isCurrent, responsibilities))
                        }
                        showAddWorkDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddWorkDialog = false }) { Text("Cancel") } }
        )
    }

    // 5. Add Qualification Dialog
    if (showAddEduDialog) {
        var institution by remember { mutableStateOf("") }
        var qualification by remember { mutableStateOf("") }
        var field by remember { mutableStateOf("") }
        var year by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddEduDialog = false },
            title = { Text("Add Qualification") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormTextInput(value = institution, onValueChange = { institution = it }, subCaption = "Institution / University")
                    FormTextInput(value = qualification, onValueChange = { qualification = it }, subCaption = "Degree / Qualification (e.g. BSc)")
                    FormTextInput(value = field, onValueChange = { field = it }, subCaption = "Field of Study")
                    FormTextInput(value = year, onValueChange = { year = it }, subCaption = "Year Completed / Expected")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (institution.isNotBlank() && qualification.isNotBlank()) {
                            profileViewModel.onEvent(ProfileEvent.AddQualification(institution, qualification, field, null, year, null, null))
                        }
                        showAddEduDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddEduDialog = false }) { Text("Cancel") } }
        )
    }

    // 6. Add Certificate Dialog
    if (showAddCertDialog) {
        var certTitle by remember { mutableStateOf("") }
        var issuer by remember { mutableStateOf("") }
        var dateVal by remember { mutableStateOf("") }
        var credId by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCertDialog = false },
            title = { Text("Add Certificate") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormTextInput(value = certTitle, onValueChange = { certTitle = it }, subCaption = "Certificate Name (e.g. CCNA)")
                    FormTextInput(value = issuer, onValueChange = { issuer = it }, subCaption = "Issuing Body (e.g. Cisco)")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormTextInput(value = dateVal, onValueChange = { dateVal = it }, subCaption = "Date", modifier = Modifier.weight(1f))
                        FormTextInput(value = credId, onValueChange = { credId = it }, subCaption = "Credential ID", modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (certTitle.isNotBlank() && issuer.isNotBlank()) {
                            profileViewModel.onEvent(ProfileEvent.AddCertificate(certTitle, issuer, dateVal, null, credId, null))
                        }
                        showAddCertDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddCertDialog = false }) { Text("Cancel") } }
        )
    }

    // 7. Add Allergy Dialog
    if (showAddAllergyDialog) {
        var allergen by remember { mutableStateOf("") }
        var reaction by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddAllergyDialog = false },
            title = { Text("Add Allergy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormTextInput(value = allergen, onValueChange = { allergen = it }, subCaption = "Allergen (e.g. Penicillin)")
                    FormTextInput(value = reaction, onValueChange = { reaction = it }, subCaption = "Reaction / Severity")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (allergen.isNotBlank()) {
                            profileViewModel.onEvent(ProfileEvent.AddAllergy(allergen, AllergySeverity.MODERATE, reaction))
                        }
                        showAddAllergyDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddAllergyDialog = false }) { Text("Cancel") } }
        )
    }

    // 8. Add Relationship / Relative Dialog
    if (showAddRelationshipDialog) {
        var relRole by remember { mutableStateOf("Mother") }
        var relName by remember { mutableStateOf("") }
        var relPhone by remember { mutableStateOf("") }
        var relEmail by remember { mutableStateOf("") }
        var relAddress by remember { mutableStateOf("") }
        var relDob by remember { mutableStateOf("") }
        var relAnniversary by remember { mutableStateOf("") }
        var relNotes by remember { mutableStateOf("") }
        var isNok by remember { mutableStateOf(false) }

        val relRoles = listOf("Mother", "Father", "Sister", "Brother", "Uncle", "Aunt", "Wife", "Husband", "Spouse", "Child", "Partner", "Friend", "Next of Kin", "Other")

        AlertDialog(
            onDismissRequest = { showAddRelationshipDialog = false },
            title = { Text("Connect Person") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        var roleExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                            OutlinedTextField(
                                value = relRole,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("How are you connected?") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                                relRoles.forEach { r ->
                                    DropdownMenuItem(text = { Text(r) }, onClick = { relRole = r; roleExpanded = false })
                                }
                            }
                        }
                    }
                    item { FormTextInput(value = relName, onValueChange = { relName = it }, subCaption = "Full Name *") }
                    item { FormTextInput(value = relPhone, onValueChange = { relPhone = it }, subCaption = "Phone Number", keyboardType = KeyboardType.Phone) }
                    item { FormTextInput(value = relEmail, onValueChange = { relEmail = it }, subCaption = "Email Address", keyboardType = KeyboardType.Email) }
                    item { FormTextInput(value = relAddress, onValueChange = { relAddress = it }, subCaption = "Address") }
                    item { FormTextInput(value = relDob, onValueChange = { relDob = it }, placeholder = "MM-DD-YYYY", subCaption = "Birthday") }
                    item { FormTextInput(value = relAnniversary, onValueChange = { relAnniversary = it }, placeholder = "MM-DD-YYYY", subCaption = "Anniversary (if applicable)") }
                    item { FormTextInput(value = relNotes, onValueChange = { relNotes = it }, subCaption = "Private Notes (e.g. Likes gardening)", singleLine = false) }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isNok, onCheckedChange = { isNok = it })
                            Text("Mark as Next of Kin / ICE", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (relName.isNotBlank()) {
                            profileViewModel.onEvent(
                                ProfileEvent.AddRelationship(
                                    role = relRole,
                                    fullName = relName.trim(),
                                    phone = relPhone.trim(),
                                    email = relEmail.trim(),
                                    address = relAddress.trim(),
                                    dob = relDob.trim(),
                                    anniversary = relAnniversary.trim(),
                                    notes = relNotes.trim(),
                                    isNextOfKin = isNok
                                )
                            )
                        }
                        showAddRelationshipDialog = false
                    }
                ) { Text("Save Person") }
            },
            dismissButton = { TextButton(onClick = { showAddRelationshipDialog = false }) { Text("Cancel") } }
        )
    }

    // 9. Delete Relationship Dialog
    relationshipToDelete?.let { rel ->
        AlertDialog(
            onDismissRequest = { relationshipToDelete = null },
            title = { Text("Disconnect Person") },
            text = { Text("Are you sure you want to remove ${rel.relationRole} (${rel.fullName})? Notes will be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        profileViewModel.onEvent(ProfileEvent.DeleteRelationship(rel.id, rel.targetPersonId))
                        relationshipToDelete = null
                        selectedPersonForDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StateError)
                ) { Text("Disconnect", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { relationshipToDelete = null }) { Text("Cancel") } }
        )
    }

    // 10. Document Delete Confirmation Dialog
    documentToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            title = { Text("Delete Document") },
            text = { Text("Permanently remove \"${doc.document.title}\" from encrypted storage?") },
            confirmButton = {
                Button(
                    onClick = {
                        documentViewModel.onEvent(DocumentEvent.DeleteDocument(doc.document.id))
                        documentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StateError)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { documentToDelete = null }) { Text("Cancel") } }
        )
    }

    // 11. Document Upload Dialog
    if (showUploadDialog && pendingUploadUri != null) {
        AlertDialog(
            onDismissRequest = { showUploadDialog = false; pendingUploadUri = null },
            title = { Text("Save Document to Wallet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormTextInput(value = uploadDocTitle, onValueChange = { uploadDocTitle = it }, subCaption = "Document Title (e.g. Degree Certificate, Passport)")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingUploadUri
                        if (uri != null && uploadDocTitle.isNotBlank()) {
                            try {
                                val stream = context.contentResolver.openInputStream(uri)
                                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                                if (stream != null) {
                                    documentViewModel.onEvent(
                                        DocumentEvent.IngestDocument(
                                            type = uploadDocType,
                                            title = uploadDocTitle,
                                            docNumber = null,
                                            authority = null,
                                            country = country,
                                            issueDate = null,
                                            expiryDate = null,
                                            classification = SecurityClassification.ZONE_2_PRIVATE,
                                            fileStream = stream,
                                            mimeType = mime,
                                            filename = uri.lastPathSegment
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                scope.launch { snackbarHostState.showSnackbar("Upload failed: ${e.message}") }
                            }
                        }
                        showUploadDialog = false
                        pendingUploadUri = null
                    }
                ) { Text("Encrypt & Save") }
            },
            dismissButton = { TextButton(onClick = { showUploadDialog = false; pendingUploadUri = null }) { Text("Cancel") } }
        )
    }
}
