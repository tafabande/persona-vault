package com.pims.vault.presentation.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val NationalIdVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "NationalIdVector",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // ID card outline
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 5f)
            lineTo(21f, 5f)
            lineTo(21f, 19f)
            lineTo(3f, 19f)
            close()
            // Head
            moveTo(9f, 9.5f)
            lineTo(9f, 9.6f)
            // Body curve
            moveTo(6.5f, 15f)
            lineTo(11.5f, 15f)
            // Details lines
            moveTo(14f, 9f)
            lineTo(18.5f, 9f)
            moveTo(14f, 12f)
            lineTo(18.5f, 12f)
            moveTo(14f, 15f)
            lineTo(17f, 15f)
        }
    }.build()
}

internal val SchoolIdVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "SchoolIdVector",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Mortarboard cap top
            moveTo(12f, 3f)
            lineTo(22f, 8f)
            lineTo(12f, 13f)
            lineTo(2f, 8f)
            close()
            // Cap lower body
            moveTo(6f, 10f)
            lineTo(6f, 16f)
            lineTo(12f, 19f)
            lineTo(18f, 16f)
            lineTo(18f, 10f)
            // Tassel
            moveTo(22f, 8f)
            lineTo(22f, 15f)
        }
    }.build()
}

internal val LibraryIdVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "LibraryIdVector",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Open book spine & pages
            moveTo(12f, 6f)
            lineTo(12f, 20f)
            moveTo(12f, 6f)
            lineTo(4f, 3f)
            lineTo(4f, 17f)
            lineTo(12f, 20f)
            moveTo(12f, 6f)
            lineTo(20f, 3f)
            lineTo(20f, 17f)
            lineTo(12f, 20f)
        }
    }.build()
}

internal val MedicalAidVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "MedicalAidVector",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Shield outline
            moveTo(12f, 2f)
            lineTo(4f, 5f)
            lineTo(4f, 12f)
            lineTo(12f, 21f)
            lineTo(20f, 12f)
            lineTo(20f, 5f)
            close()
            // First aid cross inside
            moveTo(12f, 7f)
            lineTo(12f, 15f)
            moveTo(8f, 11f)
            lineTo(16f, 11f)
        }
    }.build()
}

internal val CustomMembershipVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "CustomMembershipVector",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Ribbon star / medal
            moveTo(12f, 3f)
            lineTo(15f, 9f)
            lineTo(21f, 10f)
            lineTo(16.5f, 14.5f)
            lineTo(18f, 21f)
            lineTo(12f, 17.5f)
            lineTo(6f, 21f)
            lineTo(7.5f, 14.5f)
            lineTo(3f, 10f)
            lineTo(9f, 9f)
            close()
        }
    }.build()
}

internal val BankVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "Bank",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF9E9E9E)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 21f)
            lineTo(21f, 21f)
            moveTo(3f, 10f)
            lineTo(21f, 10f)
            moveTo(5f, 10f)
            lineTo(5f, 21f)
            moveTo(19f, 10f)
            lineTo(19f, 21f)
            moveTo(9f, 10f)
            lineTo(9f, 21f)
            moveTo(15f, 10f)
            lineTo(15f, 21f)
            moveTo(12f, 3f)
            lineTo(21f, 10f)
            lineTo(3f, 10f)
            close()
        }
    }.build()
}
