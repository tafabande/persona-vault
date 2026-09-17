package com.pims.vault.presentation.avatar.hair

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.pims.vault.presentation.avatar.HairStyle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 2D Character Rig & Strand Physics Engine
 *
 * Implements:
 * 1. Modular spring oscillators per strand cluster (mass, damping, stiffness, phase delay).
 * 2. Multi-node strand chains for long hair, ponytails, locs, and braids.
 * 3. Rounded anatomical collision zones (Face, Neck, Shoulders) preventing hair clipping.
 * 4. Hairstyle-specific physical profiles from stiff buzz cuts to heavy dynamic ponytails.
 * 5. HairAppearance with organic multi-tonal color models (root shadow, mass, sheen, accent).
 */

/**
 * Hair layer hierarchy for explicit Z-depth occlusion.
 */
enum class HairLayer {
    BACK_LOCKS,      // Resting behind torso and shoulders
    COLLAR_DRAPE,    // Strands draping over the front of the shirt collar
    SCALP_UNDER,     // Base silhouette adhering to head boundary
    MAIN_VOLUME,     // Mid-tone volumetric body with 4-8% scalp offset
    FACE_FRINGE,     // Bangs and wisps falling across forehead and framing cheeks
    STRAND_DETAILS,  // Curvilinear cubicTo flow highlights
    PERIMETER_FLYAWAY // Micro-wisps breaking outside the silhouette with fast flutter
}

/**
 * Multi-tonal color model to prevent flat plastic appearances.
 */
data class HairAppearance(
    val baseColor: Color,
    val rootColor: Color,
    val shadowColor: Color,
    val highlightColor: Color,
    val accentColor: Color,
    val highlightStrength: Float = 0.22f,
    val shadowStrength: Float = 0.28f,
    val shine: Float = 0.16f
) {
    companion object {
        fun fromBase(base: Color, accent: Color = base): HairAppearance {
            val root = Color(
                red = (base.red * 0.65f).coerceIn(0f, 1f),
                green = (base.green * 0.65f).coerceIn(0f, 1f),
                blue = (base.blue * 0.65f).coerceIn(0f, 1f),
                alpha = base.alpha
            )
            val shadow = Color(
                red = (base.red * 0.50f).coerceIn(0f, 1f),
                green = (base.green * 0.50f).coerceIn(0f, 1f),
                blue = (base.blue * 0.50f).coerceIn(0f, 1f),
                alpha = base.alpha
            )
            val highlight = Color(
                red = (base.red * 1.25f + 0.08f).coerceIn(0f, 1f),
                green = (base.green * 1.25f + 0.08f).coerceIn(0f, 1f),
                blue = (base.blue * 1.25f + 0.08f).coerceIn(0f, 1f),
                alpha = base.alpha
            )
            return HairAppearance(
                baseColor = base,
                rootColor = root,
                shadowColor = shadow,
                highlightColor = highlight,
                accentColor = accent
            )
        }
    }
}

/**
 * Physical motion profile tailored to specific hair textures and lengths.
 */
data class HairPhysicsProfile(
    val stiffness: Float,       // Resistance to bending (higher = stiffer)
    val damping: Float,         // Velocity deceleration (higher = less oscillation)
    val mass: Float,            // Inertial weight (higher = more follow-through lag)
    val gravity: Float = 0.5f,  // Downward pull tendency
    val drag: Float = 0.6f,     // Counter-velocity air resistance
    val followThrough: Float = 0.5f, // Momentum delay when head comes to an abrupt stop
    val collisionRadius: Float = 0.04f
)

/**
 * Hair style specific physics catalog.
 */
object HairPhysicsProfiles {
    val BUZZ_CUT = HairPhysicsProfile(
        stiffness = 380f,
        damping = 26f,
        mass = 0.2f,
        drag = 0.1f,
        followThrough = 0.02f
    )

    val FADE = HairPhysicsProfile(
        stiffness = 320f,
        damping = 24f,
        mass = 0.35f,
        drag = 0.2f,
        followThrough = 0.05f
    )

    val SHORT_CROP = HairPhysicsProfile(
        stiffness = 240f,
        damping = 20f,
        mass = 0.6f,
        drag = 0.35f,
        followThrough = 0.18f
    )

    val AFRO = HairPhysicsProfile(
        stiffness = 190f,
        damping = 17f,
        mass = 0.9f,
        drag = 0.7f,
        followThrough = 0.35f
    )

    val BOB = HairPhysicsProfile(
        stiffness = 160f,
        damping = 14f,
        mass = 1.0f,
        drag = 0.6f,
        followThrough = 0.45f
    )

    val BRAIDS_AND_LOCS = HairPhysicsProfile(
        stiffness = 135f,
        damping = 13f,
        mass = 1.35f,
        drag = 0.75f,
        followThrough = 0.65f
    )

    val LONG_FLOWING = HairPhysicsProfile(
        stiffness = 110f,
        damping = 11f,
        mass = 1.45f,
        drag = 0.82f,
        followThrough = 0.80f
    )

    val PONYTAIL = HairPhysicsProfile(
        stiffness = 95f,
        damping = 9.5f,
        mass = 1.6f,
        drag = 0.88f,
        followThrough = 0.92f
    )

    fun forStyle(style: HairStyle): HairPhysicsProfile = when (style) {
        HairStyle.FADE -> FADE
        HairStyle.LONG -> LONG_FLOWING
    }
}

/**
 * Single spring oscillator representing a strand cluster or fringe lock.
 */
class HairStrandSpring(
    val id: String,
    val profile: HairPhysicsProfile,
    val lengthFactor: Float = 1.0f,
    val phaseOffset: Float = 0f
) {
    var currentOffset: Float = 0f
    var velocity: Float = 0f

    fun update(targetTilt: Float, deltaSeconds: Float) {
        val dt = deltaSeconds.coerceIn(0.001f, 0.05f)
        // Spring equation: F = -k * (x - target) - c * v
        val force = -profile.stiffness * (currentOffset - targetTilt) - profile.damping * velocity
        val acceleration = force / profile.mass
        velocity += acceleration * dt
        currentOffset += velocity * dt
    }

    fun reset() {
        currentOffset = 0f
        velocity = 0f
    }
}

/**
 * Multi-node chain for long strands, locs, braids, or ponytail plumes.
 * Propagates motion down a hierarchy of control nodes.
 */
class HairStrandChain(
    val id: String,
    val nodeCount: Int = 3,
    val profile: HairPhysicsProfile
) {
    data class ChainNode(
        var offsetAngle: Float = 0f,
        var velocity: Float = 0f
    )

    val nodes = List(nodeCount) { ChainNode() }

    fun update(rootTilt: Float, deltaSeconds: Float) {
        val dt = deltaSeconds.coerceIn(0.001f, 0.05f)
        var previousAngle = rootTilt

        nodes.forEachIndexed { index, node ->
            // Successive nodes experience cumulative mass delay and lower stiffness
            val nodeMass = profile.mass * (1.0f + index * 0.28f)
            val nodeStiffness = profile.stiffness * (1.0f - index * 0.15f).coerceAtLeast(0.4f)
            val nodeDamping = profile.damping * (1.0f - index * 0.08f).coerceAtLeast(0.5f)

            val force = -nodeStiffness * (node.offsetAngle - previousAngle) - nodeDamping * node.velocity
            val acceleration = force / nodeMass
            node.velocity += acceleration * dt
            node.offsetAngle += node.velocity * dt

            previousAngle = node.offsetAngle
        }
    }

    val tipOffset: Float
        get() = nodes.lastOrNull()?.offsetAngle ?: 0f

    val midOffset: Float
        get() = nodes.getOrNull(nodeCount / 2)?.offsetAngle ?: 0f

    fun reset() {
        nodes.forEach {
            it.offsetAngle = 0f
            it.velocity = 0f
        }
    }
}

/**
 * Rounded 2D anatomical colliders for collision and deflection.
 */
object AvatarColliders {

    /**
     * Resolves deflection against a rounded facial boundary (oval capsule).
     * If a hair strand position penetrates the cheek or chin boundary, pushes it along the surface normal.
     */
    fun resolveFaceBoundary(
        point: Offset,
        faceCenter: Offset,
        radiusX: Float,
        radiusY: Float,
        strandMargin: Float = 2f
    ): Offset {
        val dx = point.x - faceCenter.x
        val dy = point.y - faceCenter.y
        // Normalized ellipse equation: (dx/rx)^2 + (dy/ry)^2
        val nx = dx / radiusX
        val ny = dy / radiusY
        val distSq = nx * nx + ny * ny

        if (distSq < 1.0f && distSq > 0.0001f) {
            val dist = sqrt(distSq)
            val scale = (1.0f + (strandMargin / radiusX)) / dist
            return Offset(
                x = faceCenter.x + dx * scale,
                y = faceCenter.y + dy * scale
            )
        }
        return point
    }

    /**
     * Resolves deflection against rounded shoulder slope.
     */
    fun resolveShoulderBoundary(
        point: Offset,
        shoulderAnchor: Offset,
        shoulderRadius: Float,
        strandMargin: Float = 2f
    ): Offset {
        val dx = point.x - shoulderAnchor.x
        val dy = point.y - shoulderAnchor.y
        val dist = sqrt(dx * dx + dy * dy)
        val targetRadius = shoulderRadius + strandMargin

        if (dist < targetRadius && dist > 0.001f) {
            val factor = targetRadius / dist
            return Offset(
                x = shoulderAnchor.x + dx * factor,
                y = shoulderAnchor.y + dy * factor
            )
        }
        return point
    }
}
