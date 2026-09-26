package com.pims.vault.presentation.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeGenerator {
    /**
     * Generates a standard, fully scannable QR code bitmap using ZXing QRCodeWriter.
     * Compliant with ISO/IEC 18004.
     */
    fun generateQrBitmap(
        content: String,
        size: Int = 384,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        val bitMatrix = QRCodeWriter().encode(
            content.ifBlank { "https://persona.vault" },
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) darkColor else lightColor)
            }
        }
        return bitmap
    }
}

object PersonaVCardHelper {
    /**
     * Encodes complete contact details into standard vCard 3.0 format.
     * Recognized by Android Camera, iOS Camera, and Google Lens out-of-the-box.
     */
    fun formatVCard(
        fullName: String,
        phone: String = "",
        email: String = "",
        occupation: String = "",
        linkedIn: String = "",
        country: String = ""
    ): String {
        return buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            appendLine("FN:${fullName.ifBlank { "Personal Contact" }}")
            if (occupation.isNotBlank()) appendLine("TITLE:$occupation")
            if (phone.isNotBlank()) appendLine("TEL:$phone")
            if (email.isNotBlank()) appendLine("EMAIL:$email")
            if (linkedIn.isNotBlank()) {
                val url = if (linkedIn.startsWith("http://") || linkedIn.startsWith("https://")) {
                    linkedIn
                } else {
                    "https://linkedin.com/in/$linkedIn"
                }
                appendLine("URL:$url")
            }
            if (country.isNotBlank()) appendLine("ADR:;;;$country;;;")
            appendLine("NOTE:Shared securely from Persona Vault")
            append("END:VCARD")
        }
    }
}
