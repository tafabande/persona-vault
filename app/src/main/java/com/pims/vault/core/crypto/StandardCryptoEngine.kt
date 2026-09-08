package com.pims.vault.core.crypto

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Standard implementation of [CryptoEngine] using standard JCE primitives (AES-GCM-256, SHA-256, HMAC-SHA256).
 * This allows unit testing and development on any JVM environment before binding to Android Keystore / StrongBox in Option C.
 */
class StandardCryptoEngine : CryptoEngine by HardenedCryptoEngine()
