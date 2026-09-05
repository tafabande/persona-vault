package com.pims.vault.core.crypto

import java.util.Arrays

/**
 * A secure container for sensitive in-memory bytes (passwords, TOTP seeds, derivation keys)
 * that ensures zeroization (wiping with zeros) upon close to mitigate memory-dump attacks.
 */
class SecretBytes(bytes: ByteArray) : AutoCloseable {
    private var internalBytes: ByteArray? = bytes.clone()
    private var isDestroyed = false

    val bytes: ByteArray
        get() {
            check(!isDestroyed) { "SecretBytes has already been zeroized and destroyed" }
            return internalBytes ?: throw IllegalStateException("SecretBytes is null")
        }

    val size: Int
        get() = internalBytes?.size ?: 0

    fun copyBytes(): ByteArray {
        check(!isDestroyed) { "SecretBytes has already been zeroized and destroyed" }
        return internalBytes!!.clone()
    }

    override fun close() {
        if (!isDestroyed) {
            internalBytes?.let { Arrays.fill(it, 0.toByte()) }
            internalBytes = null
            isDestroyed = true
        }
    }

    fun isClosed(): Boolean = isDestroyed

    companion object {
        inline fun <R> useSecret(bytes: ByteArray, block: (SecretBytes) -> R): R {
            val secret = SecretBytes(bytes)
            return try {
                block(secret)
            } finally {
                secret.close()
            }
        }
    }
}
