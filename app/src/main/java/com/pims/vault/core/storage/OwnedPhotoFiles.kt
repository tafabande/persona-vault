package com.pims.vault.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

internal const val MAX_ORIGINAL_PHOTO_BYTES = 16L * 1024 * 1024
private const val MAX_PHOTO_PIXELS = 100_000_000L

internal fun photoExtension(mimeType: String): String? = when (mimeType) {
    "image/jpeg" -> "jpg"
    "image/png" -> "png"
    "image/webp" -> "webp"
    "image/heic" -> "heic"
    "image/heif" -> "heif"
    "image/avif" -> "avif"
    "image/gif" -> "gif"
    "image/bmp" -> "bmp"
    else -> null
}

internal fun inspectPhoto(file: File): String? {
    if (!file.isFile || file.length() !in 1..MAX_ORIGINAL_PHOTO_BYTES) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    return supportedMimeType(bounds)
}

internal fun inspectPhoto(bytes: ByteArray): String? {
    if (bytes.size.toLong() !in 1..MAX_ORIGINAL_PHOTO_BYTES) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    return supportedMimeType(bounds)
}

private fun supportedMimeType(bounds: BitmapFactory.Options): String? {
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || bounds.outWidth.toLong() * bounds.outHeight > MAX_PHOTO_PIXELS) return null
    return bounds.outMimeType?.takeIf { photoExtension(it) != null }
}

internal fun decodePhotoPreview(file: File, maxDimension: Int = 512): Bitmap? =
    decodePhotoPreview(ImageDecoder.createSource(file), maxDimension)

internal fun decodePhotoPreview(bytes: ByteArray, maxDimension: Int = 512): Bitmap? =
    decodePhotoPreview(ImageDecoder.createSource(ByteBuffer.wrap(bytes)), maxDimension)

internal fun decodePhotoPreview(context: Context, uri: Uri, maxDimension: Int = 512): Bitmap? =
    decodePhotoPreview(ImageDecoder.createSource(context.contentResolver, uri), maxDimension)

private fun decodePhotoPreview(source: ImageDecoder.Source, maxDimension: Int): Bitmap? = try {
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val width = info.size.width
        val height = info.size.height
        require(width > 0 && height > 0 && width.toLong() * height <= MAX_PHOTO_PIXELS)
        val longest = maxOf(width, height)
        if (longest > maxDimension) {
            decoder.setTargetSize(
                (width.toLong() * maxDimension / longest).toInt().coerceAtLeast(1),
                (height.toLong() * maxDimension / longest).toInt().coerceAtLeast(1)
            )
        }
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
} catch (_: Exception) {
    null
}

internal suspend fun importOwnedPhoto(context: Context, uri: Uri, directory: String, prefix: String): File =
    withContext(Dispatchers.IO) {
        val root = context.filesDir.canonicalFile
        val base = File(root, directory).canonicalFile
        require(base != root && base.toPath().startsWith(root.toPath()))
        check(base.isDirectory || base.mkdirs()) { "Photo storage is unavailable" }
        val staged = File.createTempFile(".photo-", ".pending", base)
        try {
            val source = context.contentResolver.openInputStream(uri) ?: error("This photo could not be opened")
            source.use { input ->
                FileOutputStream(staged).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    try {
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= MAX_ORIGINAL_PHOTO_BYTES) { "Choose a photo up to 16 MB" }
                            output.write(buffer, 0, count)
                        }
                        output.fd.sync()
                    } finally {
                        buffer.fill(0)
                    }
                }
            }
            val mimeType = checkNotNull(inspectPhoto(staged)) { "Choose a supported photo" }
            checkNotNull(decodePhotoPreview(staged, 256)) { "This photo could not be decoded" }.recycle()
            currentCoroutineContext().ensureActive()
            val target = File(base, "${prefix}_${UUID.randomUUID()}.${photoExtension(mimeType)}").canonicalFile
            check(target.parentFile == base)
            Files.move(staged.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
            target
        } finally {
            staged.delete()
        }
    }
