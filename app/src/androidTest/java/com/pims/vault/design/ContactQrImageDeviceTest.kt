package com.pims.vault.design

import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.pims.vault.presentation.hub.generateCardBitmap
import com.pims.vault.presentation.ui.components.PersonaVCardHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Uses synthetic contact data and reads the same PNG URI a receiving app would receive. */
@RunWith(AndroidJUnit4::class)
class ContactQrImageDeviceTest {
    @Test
    fun exportedContactImageOpensThroughProviderAndDecodesToTheSelectedContact() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "Alex Rivera ".repeat(18)
        val contact = PersonaVCardHelper.formatVCard(
            fullName = name,
            phone = "+263 770 000 000",
            email = "alex@example.test",
            occupation = "Designer",
            country = "Zimbabwe"
        )
        val file = generateCardBitmap(context, name, contact)
        try {
            assertEquals("shared_cards", file.parentFile?.name)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            assertEquals("content", uri.scheme)
            val bitmap = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
            checkNotNull(bitmap)
            try {
                assertEquals(1200, bitmap.width)
                assertEquals(756, bitmap.height)
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
                val result = MultiFormatReader().decode(
                    BinaryBitmap(HybridBinarizer(source)),
                    mapOf(DecodeHintType.TRY_HARDER to true, DecodeHintType.CHARACTER_SET to "UTF-8")
                )
                assertEquals(contact, result.text)
                assertTrue(result.text.contains("TEL:+263 770 000 000"))
            } finally {
                bitmap.recycle()
            }
        } finally {
            file.delete()
        }
    }
}
