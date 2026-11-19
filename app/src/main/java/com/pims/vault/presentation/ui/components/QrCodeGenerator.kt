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
        require(content.isNotBlank()) { "QR content is empty" }
        require(size > 0) { "QR size must be positive" }
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 4
        )
        val bitMatrix = QRCodeWriter().encode(
            content,
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
        fun text(value: String): String = value.replace("\\", "\\\\")
            .replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\\n")
            .replace(";", "\\;").replace(",", "\\,")
        fun line(value: String): String = value.replace("\r", "").replace("\n", "")
        val lines = mutableListOf("BEGIN:VCARD", "VERSION:3.0", "FN:${text(fullName.ifBlank { "Personal Contact" })}")
        // Preserve the supplied name without guessing surname boundaries.
        lines.add("N:;${text(fullName.ifBlank { "Personal Contact" })};;;")
        if (occupation.isNotBlank()) lines.add("TITLE:${text(occupation)}")
        if (phone.isNotBlank()) lines.add("TEL:${line(phone)}")
        if (email.isNotBlank()) lines.add("EMAIL:${text(email)}")
        if (linkedIn.isNotBlank()) {
            val supplied = line(linkedIn).trim()
            val url = if (supplied.startsWith("https://", true) || supplied.startsWith("http://", true)) supplied
                else "https://linkedin.com/in/${supplied.removePrefix("@")}"
            lines.add("URL:$url")
        }
        if (country.isNotBlank()) lines.add("ADR:;;;;;;${text(country)}")
        lines.add("END:VCARD")
        return lines.joinToString("\r\n")
    }
}
