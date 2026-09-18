package com.pims.vault.presentation.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ShaderBrush
import com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme
import java.util.Random

/**
 * NoiseTextureGenerator
 *
 * Procedurally generates a lightweight 96x96 grayscale noise bitmap once,
 * cached for the entire app lifecycle, and provides an ImageShader with TileMode.Repeated.
 * GPU-accelerated hardware tiling uses near-zero CPU and memory.
 */
object NoiseTextureGenerator {
    private var cachedShader: Shader? = null

    @Synchronized
    fun getOrCreateShader(): Shader {
        val existing = cachedShader
        if (existing != null) return existing

        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        val random = Random(1337L) // Deterministic seed for a natural, uniform grain pattern

        for (i in pixels.indices) {
            val lum = random.nextInt(256)
            // Store monochromatic value in RGB and subtle alpha
            val alpha = (random.nextInt(180) + 75)
            pixels[i] = (alpha shl 24) or (lum shl 16) or (lum shl 8) or lum
        }

        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        cachedShader = shader
        return shader
    }
}

/**
 * FilmGrainOverlay
 *
 * Renders a subtle film grain overlay on top of views.
 * Solves color banding in dark gradients and delivers a tactile, premium physical texture.
 * Completely transparent to user gestures and touches.
 */
@Composable
fun FilmGrainOverlay(
    modifier: Modifier = Modifier,
    alpha: Float? = null
) {
    val isDark = LocalPimsDarkTheme.current
    val effectiveAlpha = alpha ?: if (isDark) 0.045f else 0.022f

    val shader = remember { NoiseTextureGenerator.getOrCreateShader() }
    val brush = remember(shader) { ShaderBrush(shader) }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        drawRect(
            brush = brush,
            alpha = effectiveAlpha,
            blendMode = if (isDark) BlendMode.Overlay else BlendMode.SrcOver
        )
    }
}
