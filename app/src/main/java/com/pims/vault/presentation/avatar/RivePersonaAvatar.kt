package com.pims.vault.presentation.avatar

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * RivePersonaAvatar renders a vector Rive animation file (.riv) for the persona.
 *
 * It initializes the Rive C++ runtime safely and provides automatic lifecycle
 * management with pause/resume support and seamless fallback to the kinetic vector avatar
 * if no Rive asset is available or if runtime initialization fails.
 */
@Composable
fun RivePersonaAvatar(
    modifier: Modifier = Modifier,
    size: Dp,
    riveAssetPath: String? = null,
    riveResource: Int? = null,
    animationName: String? = null,
    stateMachineName: String? = null,
    fallback: @Composable () -> Unit
) {
    val context = LocalContext.current
    var hasError by remember(riveAssetPath, riveResource) { mutableStateOf(false) }

    // If no asset or resource provided, immediately use fallback
    if (hasError || (riveAssetPath.isNullOrBlank() && riveResource == null)) {
        fallback()
        return
    }

    // Safely ensure Rive runtime initialization
    val isInitialized = remember {
        try {
            app.rive.runtime.kotlin.core.Rive.init(context.applicationContext)
            true
        } catch (_: Throwable) {
            false
        }
    }

    if (!isInitialized) {
        fallback()
        return
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        var riveViewRef by remember { mutableStateOf<app.rive.runtime.kotlin.RiveAnimationView?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                try {
                    riveViewRef?.pause()
                    riveViewRef?.stop()
                } catch (_: Throwable) {}
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                try {
                    app.rive.runtime.kotlin.RiveAnimationView(ctx).apply {
                        riveViewRef = this
                        when {
                            riveResource != null -> {
                                setRiveResource(
                                    resId = riveResource,
                                    animationName = animationName,
                                    stateMachineName = stateMachineName
                                )
                            }
                            !riveAssetPath.isNullOrBlank() -> {
                                if (riveAssetPath.startsWith("/") || riveAssetPath.startsWith("file://")) {
                                    val cleanPath = riveAssetPath.removePrefix("file://")
                                    val bytes = java.io.File(cleanPath).readBytes()
                                    setRiveBytes(
                                        bytes = bytes,
                                        animationName = animationName,
                                        stateMachineName = stateMachineName
                                    )
                                } else {
                                    // Relative path: bundled asset under app/src/main/assets
                                    val bytes = ctx.assets.open(riveAssetPath).readBytes()
                                    setRiveBytes(
                                        bytes = bytes,
                                        animationName = animationName,
                                        stateMachineName = stateMachineName
                                    )
                                }
                            }
                        }
                    }
                } catch (_: Throwable) {
                    hasError = true
                    android.view.View(ctx)
                }
            },
            update = { view ->
                if (view is app.rive.runtime.kotlin.RiveAnimationView) {
                    try {
                        if (!view.isPlaying) {
                            view.play()
                        }
                    } catch (_: Throwable) {}
                }
            }
        )
    }
}
