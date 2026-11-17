package com.pims.vault.storage

import com.pims.vault.core.storage.readBoundedBytes
import org.junit.Assert.*
import org.junit.Test

class BoundedInputTest {
    @Test fun preservesAnImageWithinTheLimit() {
        val image = byteArrayOf(1, 2, 3)
        assertArrayEquals(image, image.inputStream().readBoundedBytes(10))
    }

    @Test fun readsOnlyOneExtraByteToDetectOversizedInput() {
        val input = ByteArray(100) { 9 }.inputStream()
        assertEquals(11, input.readBoundedBytes(10).size)
        assertEquals(89, input.available())
    }
}
