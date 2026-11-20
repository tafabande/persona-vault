package com.pims.vault.presentation.hub

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal enum class OverviewSource { Profile, Contact, Address, Custom, Education, Employment, Note, Vault }

internal data class OverviewPin(val source: OverviewSource, val id: String, val showValue: Boolean = false) {
    val key: String get() = "${source.name}:$id"
}

private val maskedCardSuffix = Regex("""(?:^|\s)••••\s+([0-9]{4})\s*$""")

internal fun maskedCardPreview(reference: String?): String? =
    reference?.let { maskedCardSuffix.find(it) }
        ?.groupValues?.get(1)?.let { "Ending in $it" }

internal fun List<OverviewPin>.movePin(pin: OverviewPin, offset: Int): List<OverviewPin> {
    val index = indexOf(pin)
    val destination = index + offset
    if (index < 0 || destination !in indices) return this
    return toMutableList().apply { add(destination, removeAt(index)) }
}

internal class MeOverviewPinState(
    val ownerId: String?,
    private val storageScope: String?,
    private val store: MeOverviewPins,
    private val scope: CoroutineScope
) {
    var pins by mutableStateOf<List<OverviewPin>?>(null)
        private set
    var loading by mutableStateOf(storageScope != null)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun load() {
        val account = storageScope ?: return
        if (saving) return
        loading = true
        error = null
        scope.launch {
            try {
                pins = withContext(Dispatchers.IO) { store.read(account) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "Could not load pinned information."
            } finally {
                loading = false
            }
        }
    }

    fun update(transform: (List<OverviewPin>) -> List<OverviewPin>) {
        val account = storageScope ?: return
        val previous = pins ?: return
        if (saving || loading) return
        val updated = transform(previous)
        if (updated == previous) return
        saving = true
        error = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) { store.write(account, updated) }
                pins = updated
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "Could not update pinned information."
            } finally {
                saving = false
            }
        }
    }
}

@Composable
internal fun rememberMeOverviewPins(accountScope: String?, ownerId: String?): MeOverviewPinState {
    val context = LocalContext.current
    val store = remember(context) { MeOverviewPins(context) }
    val storageScope = if (accountScope != null && ownerId != null) "$accountScope/$ownerId" else null
    return key(accountScope, ownerId) {
        val coroutineScope = rememberCoroutineScope()
        val state = remember { MeOverviewPinState(ownerId, storageScope, store, coroutineScope) }
        LaunchedEffect(state) { state.load() }
        state
    }
}

/** Keeps the ordered source references and their display preferences. */
internal class MeOverviewPins(context: Context) {
    private val appContext = context.applicationContext

    @Suppress("DEPRECATION")
    private val preferences by lazy {
        EncryptedSharedPreferences.create(
            appContext,
            "pims_me_overview_v1",
            MasterKey.Builder(appContext, "pims_me_overview_master")
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun read(scope: String): List<OverviewPin> {
        val encoded = JSONArray(preferences.getString(scope, null) ?: return emptyList())
        return (0 until encoded.length()).map { index ->
            val item = encoded.getJSONObject(index)
            OverviewPin(
                OverviewSource.valueOf(item.getString("source")),
                item.getString("id"),
                item.optBoolean("showValue")
            )
        }.distinctBy { it.key }
    }

    fun write(scope: String, pins: List<OverviewPin>) {
        val encoded = JSONArray(pins.distinctBy { it.key }.map { pin ->
            JSONObject().put("source", pin.source.name).put("id", pin.id).put("showValue", pin.showValue)
        })
        check(preferences.edit().putString(scope, encoded.toString()).commit())
    }
}
