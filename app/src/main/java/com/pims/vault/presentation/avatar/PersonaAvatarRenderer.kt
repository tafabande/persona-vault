package com.pims.vault.presentation.avatar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pims.vault.presentation.avatar.engine.core.AvatarCoreConfig
import com.pims.vault.presentation.avatar.engine.core.BrowShape
import com.pims.vault.presentation.avatar.engine.core.CoreExpression
import com.pims.vault.presentation.avatar.engine.core.EyeShape
import com.pims.vault.presentation.avatar.engine.core.FacialDetail
import com.pims.vault.presentation.avatar.engine.core.HeadSilhouette
import com.pims.vault.presentation.avatar.engine.core.PersonaCoreRenderer
import com.pims.vault.presentation.avatar.engine.core.RusticSkinTone
import com.pims.vault.presentation.ui.theme.LocalPersonaMood
import com.pims.vault.presentation.ui.theme.PersonaMood

/**
 * Clean Procedural Vector Avatar Renderer powered by PersonaCoreRenderer.
 * Curated 8 hairstyles: Buzz, Fade, Short, Afro, Bob, Braids, Long, Ponytail.
 */
@Composable
fun PersonaAvatarCanvas(
    config: PersonaAvatarConfig,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    showBackground: Boolean = true,
    customMood: PersonaMood? = null,
    blinkProgress: Float = 0f,
    isSleepy: Boolean = false,
    headTiltAngle: Float = 0f,
    hairSwayAngle: Float = 0f,
    idleBreathY: Float = 0f,
    squashScaleX: Float = 1f,
    squashScaleY: Float = 1f,
    yawnProgress: Float = 0f,
    sparkleProgress: Float = 0f,
    blushBoost: Float = 0f,
    breathShoulderY: Float = idleBreathY,
    breathHeadY: Float = idleBreathY,
    gazeOffset: Offset = Offset.Zero,
    hairInertiaAngle: Float = 0f
) {
    val activeMood = customMood ?: LocalPersonaMood.current

    val coreConfig = remember(config, activeMood, isSleepy, yawnProgress) {
        val coreGender = when (config.gender) {
            AvatarGender.FEMALE -> com.pims.vault.presentation.avatar.engine.core.AvatarGender.FEMALE
            AvatarGender.MALE -> com.pims.vault.presentation.avatar.engine.core.AvatarGender.MALE
            else -> com.pims.vault.presentation.avatar.engine.core.AvatarGender.NON_BINARY
        }

        val coreSilhouette = when (config.headShape) {
            HeadShape.CHISELED_ANGULAR, HeadShape.SQUARE_BROAD -> HeadSilhouette.CHISELED_SQUARE
            HeadShape.ROUND_YOUTHFUL -> HeadSilhouette.ROUND_SOFT
            else -> HeadSilhouette.RUSTIC_OVAL
        }

        val coreSkin = when (config.skinTone) {
            SkinTone.DEEP_COCOA, SkinTone.DEEP_ESPRESSO -> RusticSkinTone.WARM_EBONY
            SkinTone.CHESTNUT, SkinTone.TOFFEE -> RusticSkinTone.RICH_SIENNA
            SkinTone.AMBER_BRONZE, SkinTone.GOLDEN_HONEY -> RusticSkinTone.TERRACOTTA_CLAY
            else -> RusticSkinTone.DUSTY_ALMOND
        }

        val coreHairStyle = when (config.hairStyle) {
            HairStyle.FADE -> com.pims.vault.presentation.avatar.engine.core.HairStyle.FADE
            HairStyle.LONG -> com.pims.vault.presentation.avatar.engine.core.HairStyle.LONG
        }

        val coreExpression = when {
            isSleepy || yawnProgress > 0.3f || config.expression == AvatarExpression.SLEEPY || config.expression == AvatarExpression.YAWN -> CoreExpression.DROWSY
            config.expression == AvatarExpression.HAPPY_SQUISH -> CoreExpression.SMIRK
            else -> CoreExpression.IDLE_CALM
        }

        val coreFacialDetail = when (config.facialFeature) {
            FacialFeature.CUTE_BLUSH -> FacialDetail.CHEEK_BLUSH
            FacialFeature.LIGHT_FRECKLES -> FacialDetail.SOFT_FRECKLES
            FacialFeature.SOFT_STUBBLE -> FacialDetail.EARTH_STUBBLE
            FacialFeature.GOATEE -> FacialDetail.WARM_GOATEE
            FacialFeature.NONE -> FacialDetail.NONE
        }

        val coreEyeShape = when (config.eyeType) {
            EyeType.ALMOND -> EyeShape.ALMOND_WARM
            EyeType.GENTLE_DOT, EyeType.SMILING_CURVE, EyeType.KAWAII_SPARKLE -> EyeShape.GENTLE_ROUND
            EyeType.CALM_LIDS -> EyeShape.CALM_SLEEPY
        }

        val coreBrowShape = when (config.eyebrowType) {
            EyebrowType.NEUTRAL_ARCH, EyebrowType.PLAYFUL_CURVED -> BrowShape.SOFT_ARCH
            EyebrowType.CONFIDENT_SASSY -> BrowShape.EXPRESSIVE_TILT
            EyebrowType.INTENSE_ANGLED -> BrowShape.FLAT_INTENSE
        }

        AvatarCoreConfig(
            gender = coreGender,
            silhouette = coreSilhouette,
            skinTone = coreSkin,
            hairStyle = coreHairStyle,
            hairColor = config.hairColor.color,
            eyeShape = coreEyeShape,
            browShape = coreBrowShape,
            facialDetail = coreFacialDetail,
            expression = coreExpression,
            backdropColor = activeMood.avatarBackdropColor
        )
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            withTransform({
                scale(scaleX = squashScaleX, scaleY = squashScaleY, pivot = Offset(w * 0.5f, h * 0.5f))
            }) {
                PersonaCoreRenderer.drawBaseAvatar(
                    scope = this,
                    config = coreConfig,
                    w = w,
                    h = h,
                    headTiltDeg = headTiltAngle,
                    swayAngle = hairSwayAngle,
                    breathOffsetY = idleBreathY,
                    blushIntensity = blushBoost + (if (config.facialFeature == FacialFeature.CUTE_BLUSH) 0.25f else 0f),
                    eyeGazeOffset = gazeOffset,
                    blinkProgress = blinkProgress,
                    drawHair = true,
                    breathShoulderY = breathShoulderY,
                    breathHeadY = breathHeadY,
                    hairInertiaAngle = hairInertiaAngle,
                    sparkleProgress = sparkleProgress
                )
            }
        }
    }
}
