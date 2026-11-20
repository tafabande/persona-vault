package com.pims.vault.presentation.vault.components

import com.pims.vault.presentation.ui.theme.PersonaIcons

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.storage.VaultEvidenceStore
import com.pims.vault.core.sync.VaultProofSyncService
import com.pims.vault.data.local.entity.VaultProofEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultProofState(
    val images: List<VaultProofEntity> = emptyList(),
    val busy: Boolean = false,
    val error: String? = null,
    val preview: Bitmap? = null
)

@HiltViewModel
class VaultProofViewModel @Inject constructor(
    private val store: VaultEvidenceStore,
    private val sync: VaultProofSyncService,
    private val session: BiometricSessionManager
) : ViewModel() {
    private val _state = MutableStateFlow(VaultProofState())
    val state = _state.asStateFlow()
    private var observation: Job? = null
    private var previewJob: Job? = null
    private var itemId = ""
    private var personId = ""
    private var bindingGeneration = 0L
    private var bindingActive = false
    private var previewGeneration = 0L
    private var displayedPreview: Bitmap? = null
    private var previewProofId: String? = null

    init {
        viewModelScope.launch {
            session.sessionState.collect {
                if (!session.isZone4Unlocked()) closePreview()
            }
        }
    }

    fun bind(itemId: String, personId: String): Long {
        observation?.cancel()
        closePreview()
        bindingGeneration++
        bindingActive = true
        this.itemId = itemId
        this.personId = personId
        _state.value = VaultProofState()
        val generation = bindingGeneration
        observation = viewModelScope.launch {
            store.dao.observeForItem(itemId).collect { images ->
                if (isCurrentBinding(generation)) {
                    _state.update { it.copy(images = images) }
                    val openProofId = previewProofId
                    if (openProofId != null && images.none { it.id == openProofId }) closePreview()
                }
            }
        }
        return generation
    }

    fun unbind(generation: Long) {
        if (!isCurrentBinding(generation)) return
        bindingActive = false
        bindingGeneration++
        observation?.cancel()
        observation = null
        closePreview()
        _state.value = VaultProofState()
    }

    fun add(uri: Uri) = change("Could not save this image. Please try again.") { id, owner, generation ->
        store.add(id, owner, uri)
        scheduleSync(id, generation)
    }

    fun remove(proof: VaultProofEntity) = change("Could not remove this image. Please try again.") { id, owner, generation ->
        check(proof.itemId == id)
        store.remove(proof, owner)
        scheduleSync(id, generation)
    }

    fun open(proof: VaultProofEntity) {
        if (!bindingActive || _state.value.busy) return
        closePreview()
        val request = ++previewGeneration
        previewProofId = proof.id
        previewJob = change("Could not open this image. Unlock the vault and try again.") { id, owner, generation ->
            check(proof.itemId == id)
            val bitmap = store.preview(proof, owner)
            if (isCurrentBinding(generation) && request == previewGeneration && session.isZone4Unlocked()) {
                _state.update { it.copy(preview = bitmap) }
            } else {
                bitmap.recycle()
            }
        }
    }

    fun closePreview() {
        previewProofId = null
        previewGeneration++
        previewJob?.cancel()
        previewJob = null
        val bitmap = _state.value.preview
        _state.update { it.copy(preview = null) }
        // A rendered bitmap remains owned by Compose until its image leaves composition.
        if (bitmap != null && bitmap !== displayedPreview && !bitmap.isRecycled) bitmap.recycle()
    }

    fun previewDisplayed(bitmap: Bitmap) {
        displayedPreview = bitmap
    }

    fun previewDisposed(bitmap: Bitmap) {
        if (_state.value.preview === bitmap) {
            previewProofId = null
            _state.update { it.copy(preview = null) }
        }
        if (displayedPreview === bitmap) displayedPreview = null
    }

    private fun scheduleSync(id: String, generation: Long) {
        viewModelScope.launch {
            try {
                sync.syncCurrentItem(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (isCurrentBinding(generation)) _state.update { it.copy(error = "Cloud sync will retry.") }
            }
        }
    }

    private fun isCurrentBinding(generation: Long): Boolean =
        bindingActive && bindingGeneration == generation

    private fun change(message: String, action: suspend (String, String, Long) -> Unit): Job? {
        if (!bindingActive || _state.value.busy) return null
        val generation = bindingGeneration
        val id = itemId
        val owner = personId
        _state.update { it.copy(busy = true, error = null) }
        return viewModelScope.launch {
            try {
                action(id, owner, generation)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (isCurrentBinding(generation)) {
                    _state.update { it.copy(error = if (failure is IllegalArgumentException) failure.message ?: message else message) }
                }
            } finally {
                if (isCurrentBinding(generation)) _state.update { it.copy(busy = false) }
            }
        }
    }

    override fun onCleared() {
        bindingActive = false
        bindingGeneration++
        closePreview()
        _state.value = VaultProofState()
        super.onCleared()
    }
}

@Composable
fun VaultProofSection(itemId: String, personId: String, modifier: Modifier = Modifier) {
    val viewModel: VaultProofViewModel = hiltViewModel(key = "proof-$itemId")
    DisposableEffect(viewModel, itemId, personId) {
        val generation = viewModel.bind(itemId, personId)
        onDispose { viewModel.unbind(generation) }
    }
    val state by viewModel.state.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::add)
    }
    var pendingRemoval by remember { mutableStateOf<VaultProofEntity?>(null) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Proof images", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { picker.launch(arrayOf("image/jpeg", "image/png")) }, enabled = !state.busy) {
                Icon(PersonaIcons.Photo, null, Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add image")
            }
        }
        state.images.forEachIndexed { index, proof ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).clickable(enabled = !state.busy) { viewModel.open(proof) }
                    .padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(PersonaIcons.Image, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Image ${index + 1}", style = MaterialTheme.typography.bodyLarge)
                }
                IconButton(onClick = { pendingRemoval = proof }, enabled = !state.busy) {
                    Icon(PersonaIcons.DeleteOutline, "Remove image ${index + 1}")
                }
            }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    }
    pendingRemoval?.let { proof ->
        AlertDialog(onDismissRequest = { pendingRemoval = null }, title = { Text("Remove image?") },
            confirmButton = { TextButton(onClick = { pendingRemoval = null; viewModel.remove(proof) }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Keep") } })
    }
    state.preview?.let { bitmap ->
        DisposableEffect(bitmap) {
            viewModel.previewDisplayed(bitmap)
            onDispose {
                viewModel.previewDisposed(bitmap)
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
        Dialog(onDismissRequest = viewModel::closePreview, properties = DialogProperties(
            securePolicy = SecureFlagPolicy.SecureOn, usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    Image(bitmap.asImageBitmap(), "Proof image", Modifier.fillMaxSize().padding(16.dp), contentScale = ContentScale.Fit)
                    IconButton(onClick = viewModel::closePreview, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(PersonaIcons.Close, "Close image")
                    }
                }
            }
        }
    }
}
