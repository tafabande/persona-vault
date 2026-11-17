package com.pims.vault.presentation.document

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.pims.vault.domain.model.DocumentVersionItem
import com.pims.vault.domain.rules.DocumentRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

data class DocumentPreviewPage(val bitmap: Bitmap, val pageIndex: Int, val pageCount: Int)

/** Bitmaps are either waiting for composition or owned by the visible Image. */
internal class DocumentPreviewLifecycle {
    private var active = true
    private val pending = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Bitmap, Boolean>())

    fun accept(bitmap: Bitmap): Boolean {
        if (!active) {
            if (!bitmap.isRecycled) bitmap.recycle()
            return false
        }
        pending.add(bitmap)
        return true
    }

    fun displayed(bitmap: Bitmap) { pending.remove(bitmap) }
    fun disposed(bitmap: Bitmap) { pending.remove(bitmap); if (!bitmap.isRecycled) bitmap.recycle() }
    fun close() {
        active = false
        pending.forEach { if (!it.isRecycled) it.recycle() }
        pending.clear()
    }
}

/** A seekable anonymous descriptor keeps decrypted source bytes outside persistent storage. */
internal suspend fun renderDocumentPage(
    version: DocumentVersionItem,
    pageIndex: Int,
    decrypt: suspend (OutputStream) -> Unit
): DocumentPreviewPage {
    var rendered: Bitmap? = null
    try {
        return withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) error("Use another app to preview this file on this Android version")
            val descriptor = Os.memfd_create("persona-document-preview", 0)
            try {
                var length = 0L
                val output = object : OutputStream() {
                    override fun write(value: Int) { write(byteArrayOf(value.toByte()), 0, 1) }
                    override fun write(bytes: ByteArray, offset: Int, count: Int) {
                        require(length + count <= DocumentRules.MAX_DOCUMENT_SIZE_BYTES) { "Document exceeds preview limit" }
                        var written = 0
                        while (written < count) {
                            val amount = Os.write(descriptor, bytes, offset + written, count - written)
                            check(amount > 0)
                            written += amount
                        }
                        length += count
                    }
                }
                decrypt(output)
                Os.lseek(descriptor, 0, OsConstants.SEEK_SET)
                if (version.mimeType.startsWith("image/") && version.mimeType != "image/svg+xml") {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFileDescriptor(descriptor, null, options)
                    require(options.outWidth > 0 && options.outHeight > 0) { "Unsupported image" }
                    var sample = 1
                    while (options.outWidth / sample > 1800 || options.outHeight / sample > 2200) sample *= 2
                    options.inJustDecodeBounds = false
                    options.inSampleSize = sample
                    Os.lseek(descriptor, 0, OsConstants.SEEK_SET)
                    val bitmap = checkNotNull(BitmapFactory.decodeFileDescriptor(descriptor, null, options))
                    rendered = bitmap
                    DocumentPreviewPage(bitmap, 0, 1)
                } else {
                    require(version.mimeType in setOf("application/pdf", "application/x-pdf")) { "Preview unavailable for this format" }
                    ParcelFileDescriptor.dup(descriptor).use { duplicate ->
                        PdfRenderer(duplicate).use { renderer ->
                            require(renderer.pageCount > 0)
                            val selected = pageIndex.coerceIn(0, renderer.pageCount - 1)
                            renderer.openPage(selected).use { page ->
                                val scale = minOf(1.5f, 1800f / page.width, 2200f / page.height)
                                val bitmap = Bitmap.createBitmap((page.width * scale).toInt().coerceAtLeast(1),
                                    (page.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                                rendered = bitmap
                                bitmap.eraseColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                DocumentPreviewPage(bitmap, selected, renderer.pageCount)
                            }
                        }
                    }
                }
            } finally { Os.close(descriptor) }
        }
    } catch (failure: Throwable) {
        rendered?.let { if (!it.isRecycled) it.recycle() }
        throw failure
    }
}

@Composable
fun DocumentPreviewDialog(
    version: DocumentVersionItem,
    title: String,
    onDismiss: () -> Unit,
    render: suspend (Int) -> DocumentPreviewPage,
    onOpenExternal: () -> Unit
) {
    val lifecycle = remember(version.id) { DocumentPreviewLifecycle() }
    DisposableEffect(lifecycle) { onDispose { lifecycle.close() } }
    var pageIndex by remember(version.id) { mutableIntStateOf(0) }
    var pageCount by remember(version.id) { mutableIntStateOf(1) }
    var preview by remember(version.id) { mutableStateOf<DocumentPreviewPage?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(version, pageIndex, retry) {
        preview = null
        loading = true
        error = null
        try {
            val rendered = render(pageIndex)
            if (lifecycle.accept(rendered.bitmap)) {
                pageCount = rendered.pageCount
                preview = rendered
            }
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { error = "Preview unavailable. Try another version or open with another app." }
        finally { loading = false }
    }
    preview?.bitmap?.let { bitmap ->
        DisposableEffect(bitmap) {
            lifecycle.displayed(bitmap)
            onDispose { lifecycle.disposed(bitmap) }
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(
        securePolicy = SecureFlagPolicy.SecureOn, usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close preview") }
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    preview?.let { Image(it.bitmap.asImageBitmap(), "Document page ${it.pageIndex + 1}",
                        Modifier.fillMaxSize().padding(12.dp), contentScale = ContentScale.Fit) }
                    if (loading) CircularProgressIndicator()
                    error?.let { message -> Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(message, style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = { retry++ }) { Text("Retry") }
                    } }
                }
                if (pageCount > 1) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { pageIndex-- }, enabled = pageIndex > 0 && !loading) { Text("Previous") }
                    Text("${pageIndex + 1} / $pageCount", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { pageIndex++ }, enabled = pageIndex + 1 < pageCount && !loading) { Text("Next") }
                }
                TextButton(onClick = onOpenExternal, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("Open with another app")
                }
            }
        }
    }
}
