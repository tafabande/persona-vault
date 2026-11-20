package com.pims.vault.presentation.media

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.pims.vault.core.storage.decodePhotoPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun rememberPhotoPreview(path: String?, maxDimension: Int = 512): Bitmap? = key(path, maxDimension) {
    val bitmap by produceState<Bitmap?>(null) {
        value = withContext(Dispatchers.IO) {
            path?.let { decodePhotoPreview(File(it), maxDimension) }
        }
    }
    bitmap
}

@Composable
internal fun rememberSelectedPhotoPreview(uri: Uri?, maxDimension: Int): Bitmap? {
    val context = LocalContext.current
    return key(uri, maxDimension) {
        val bitmap by produceState<Bitmap?>(null) {
            value = withContext(Dispatchers.IO) {
                uri?.let { decodePhotoPreview(context, it, maxDimension) }
            }
        }
        bitmap
    }
}
