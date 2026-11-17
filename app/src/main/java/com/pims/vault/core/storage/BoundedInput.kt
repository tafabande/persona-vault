package com.pims.vault.core.storage

import java.io.InputStream

/** Reads at most one byte beyond the limit so callers can distinguish an oversized image. */
fun InputStream.readBoundedBytes(limit: Int): ByteArray {
    require(limit in 1 until Int.MAX_VALUE)
    val buffer = ByteArray(limit + 1)
    try {
        var count = 0
        while (count < buffer.size) {
            val read = read(buffer, count, minOf(64 * 1024, buffer.size - count))
            if (read < 0) break
            count += read
        }
        return buffer.copyOf(count)
    } finally { buffer.fill(0) }
}
