package com.pims.vault.core.crypto

import java.io.ByteArrayOutputStream

/** Byte-array staging for plaintext that can be explicitly wiped after copying. */
class ZeroizingByteArrayOutputStream(initialCapacity: Int = 32) : ByteArrayOutputStream(initialCapacity) {
    fun takeBytesAndWipe(): ByteArray = toByteArray().also { wipe() }

    fun wipe() {
        buf.fill(0)
        reset()
    }

    override fun close() {
        wipe()
    }
}
