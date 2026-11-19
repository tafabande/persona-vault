package com.pims.vault.presentation.ui.theme

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Preview of the downloaded Gojo theme artwork with a single selection target. */
@Composable
fun JujutsuKaisenThemePreview(selected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val artwork by produceState<ImageBitmap?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) {
            context.assets.open("wallpapers/wp_jjk_gojo.jpg").use { stream ->
                BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = 2 })
                    ?.asImageBitmap()
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(136.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PersonaMood.JUJUTSU_KAISEN.darkSurfaceColor)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        artwork?.let { image ->
            Image(image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.12f), Color.Black.copy(alpha = 0.8f)))
            )
        )
        Text(
            text = PersonaMood.JUJUTSU_KAISEN.title,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                .background(Color.Black.copy(alpha = 0.55f), CircleShape),
            colors = RadioButtonDefaults.colors(
                selectedColor = PersonaMood.JUJUTSU_KAISEN.darkAccentColor,
                unselectedColor = Color.White
            )
        )
    }
}
