package com.pims.vault.vault

import com.pims.vault.core.crypto.TotpGenerator
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Validates TotpGenerator against RFC 6238 test vectors (Appendix B).
 *
 * RFC 6238 test vectors use:
 * Seed for HMAC-SHA1: "12345678901234567890" (20 bytes ASCII)
 * Seed for HMAC-SHA256: "12345678901234567890123456789012" (32 bytes ASCII)
 * Seed for HMAC-SHA512: "1234567890123456789012345678901234567890123456789012345678901234" (64 bytes ASCII)
 */
class TotpRfc6238Tests {

    private val seedSha1 = "12345678901234567890".toByteArray(Charsets.US_ASCII)
    private val seedSha256 = "12345678901234567890123456789012".toByteArray(Charsets.US_ASCII)
    private val seedSha512 = "1234567890123456789012345678901234567890123456789012345678901234".toByteArray(Charsets.US_ASCII)

    @Test
    fun testRfc6238Sha1Vectors8Digits() {
        // Time = 59s (T = 1)
        assertEquals("94287082", TotpGenerator.generateTotp(seedSha1, timestampMs = 59_000L, algorithm = "SHA1", digits = 8, periodSeconds = 30))
        // Time = 1111111109s (T = 37037036)
        assertEquals("07081804", TotpGenerator.generateTotp(seedSha1, timestampMs = 1111111109_000L, algorithm = "SHA1", digits = 8, periodSeconds = 30))
        // Time = 1111111111s (T = 37037037)
        assertEquals("14050471", TotpGenerator.generateTotp(seedSha1, timestampMs = 1111111111_000L, algorithm = "SHA1", digits = 8, periodSeconds = 30))
        // Time = 1234567890s (T = 41152263)
        assertEquals("89005924", TotpGenerator.generateTotp(seedSha1, timestampMs = 1234567890_000L, algorithm = "SHA1", digits = 8, periodSeconds = 30))
        // Time = 2000000000s (T = 66666666)
        assertEquals("69279037", TotpGenerator.generateTotp(seedSha1, timestampMs = 2000000000_000L, algorithm = "SHA1", digits = 8, periodSeconds = 30))
    }

    @Test
    fun testRfc6238Sha256Vectors8Digits() {
        // Time = 59s
        assertEquals("46114540", TotpGenerator.generateTotp(seedSha256, timestampMs = 59_000L, algorithm = "SHA256", digits = 8, periodSeconds = 30))
        // Time = 1111111109s
        assertEquals("68084774", TotpGenerator.generateTotp(seedSha256, timestampMs = 1111111109_000L, algorithm = "SHA256", digits = 8, periodSeconds = 30))
        // Time = 1111111111s
        assertEquals("67062674", TotpGenerator.generateTotp(seedSha256, timestampMs = 1111111111_000L, algorithm = "SHA256", digits = 8, periodSeconds = 30))
        // Time = 1234567890s
        assertEquals("91819424", TotpGenerator.generateTotp(seedSha256, timestampMs = 1234567890_000L, algorithm = "SHA256", digits = 8, periodSeconds = 30))
        // Time = 2000000000s
        assertEquals("90698825", TotpGenerator.generateTotp(seedSha256, timestampMs = 2000000000_000L, algorithm = "SHA256", digits = 8, periodSeconds = 30))
    }

    @Test
    fun testRfc6238Sha512Vectors8Digits() {
        // Time = 59s
        assertEquals("90693936", TotpGenerator.generateTotp(seedSha512, timestampMs = 59_000L, algorithm = "SHA512", digits = 8, periodSeconds = 30))
        // Time = 1111111109s
        assertEquals("25091201", TotpGenerator.generateTotp(seedSha512, timestampMs = 1111111109_000L, algorithm = "SHA512", digits = 8, periodSeconds = 30))
        // Time = 1111111111s
        assertEquals("99943326", TotpGenerator.generateTotp(seedSha512, timestampMs = 1111111111_000L, algorithm = "SHA512", digits = 8, periodSeconds = 30))
        // Time = 1234567890s
        assertEquals("93441116", TotpGenerator.generateTotp(seedSha512, timestampMs = 1234567890_000L, algorithm = "SHA512", digits = 8, periodSeconds = 30))
        // Time = 2000000000s
        assertEquals("38618901", TotpGenerator.generateTotp(seedSha512, timestampMs = 2000000000_000L, algorithm = "SHA512", digits = 8, periodSeconds = 30))
    }

    @Test
    fun testBase32DecodingAndStandard6Digits() {
        // "JBSWY3DPEHPK3PXP" is standard Base32 for "Hello!\xde\xad\xbe\xef"
        val base32Secret = "JBSWY3DPEHPK3PXP"
        val code = TotpGenerator.generateTotp(base32Secret, timestampMs = 1600000000_000L, digits = 6, periodSeconds = 30)
        assertEquals(6, code.length)

        val liveToken = TotpGenerator.getLiveToken(base32Secret, timestampMs = 1600000010_000L, periodSeconds = 30)
        assertEquals(6, liveToken.token.length)
        assertEquals(20, liveToken.remainingSeconds) // 30 - (10 % 30) = 20
    }

    @Test
    fun testNonStandardPeriods() {
        val base32Secret = "JBSWY3DPEHPK3PXP"
        val liveToken60 = TotpGenerator.getLiveToken(base32Secret, timestampMs = 1600000045_000L, periodSeconds = 60)
        assertEquals(15, liveToken60.remainingSeconds) // 60 - 45 = 15
    }
}
