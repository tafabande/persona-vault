package com.pims.vault.presentation.avatar

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.R
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import java.io.File

/**
 * WhatsApp-Style Persona Profile Avatar:
 * Clean, minimal, and premium.
 * - Custom uploaded photo if set (with zoom scale).
 * - Otherwise: Crisp pure white silhouette (male or female with long hair) on neutral slate circle.
 */
@Composable
fun PersonaAvatar(
    name: String = "",
    modifier: Modifier = Modifier,
    config: PersonaAvatarConfig? = null,
    expression: AvatarExpression? = null,
    size: Dp = 84.dp,
    avatarTextSize: TextUnit = 28.sp,
    behaviorMode: AvatarBehaviorMode = AvatarBehaviorMode.STATIC,
    showBackground: Boolean = true,
    customMood: com.pims.vault.presentation.ui.theme.PersonaMood? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val haptics = rememberPimsHaptics()

    val customBitmap = remember(config?.customAvatarPath) {
        config?.customAvatarPath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) BitmapFactory.decodeFile(path) else null
            } catch (_: Exception) {
                null
            }
        }
    }

    val isFemale = config?.gender == AvatarGender.FEMALE ||
            (config?.gender == null && name.isNotBlank() && name.hashCode() % 2 != 0)

    val clickModifier = if (onClick != null) {
        Modifier.tactilePress {
            haptics.selection()
            onClick()
        }
    } else Modifier

    val zoom = config?.customAvatarAlignmentX?.let { 1f } ?: 1f

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap.asImageBitmap(),
                contentDescription = "$name profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            val drawableRes = if (isFemale) R.drawable.ic_default_avatar_female else R.drawable.ic_default_avatar_male
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = "$name default profile",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
