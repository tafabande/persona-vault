package com.pims.vault.core.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object PayloadIntegrity {

    fun signPayload(
        uid: String,
        operationId: String,
        action: String,
        json: String,
        keyBytes: ByteArray
    ): String {
        val raw = "$uid:$operationId:$action:$json"
        return hmacSha256(keyBytes, raw.toByteArray(Charsets.UTF_8))
    }

    fun verifyPayload(
        uid: String,
        operationId: String,
        action: String,
        json: String,
        expectedHmac: String,
        keyBytes: ByteArray
    ): Boolean {
        if (expectedHmac.isBlank()) return false
        val computed = signPayload(uid, operationId, action, json, keyBytes)
        return constantTimeEquals(computed, expectedHmac)
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data).joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
