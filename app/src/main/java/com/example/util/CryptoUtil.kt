package com.example.util

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility to encrypt and decrypt sensitive fields (such as passwords) at rest.
 * Uses AES with CBC and PKCS5Padding.
 */
object CryptoUtil {
    // 256-bit fixed application-level derivation key for at-rest storage
    private val KEY_BYTES = byteArrayOf(
        0x41.toByte(), 0x6C.toByte(), 0x41.toByte(), 0x6D.toByte(),
        0x69.toByte(), 0x72.toByte(), 0x4E.toByte(), 0x65.toByte(),
        0x74.toByte(), 0x57.toByte(), 0x69.toByte(), 0x73.toByte(),
        0x70.toByte(), 0x4D.toByte(), 0x61.toByte(), 0x70.toByte(),
        0x32.toByte(), 0x30.toByte(), 0x32.toByte(), 0x36.toByte(),
        0x4B.toByte(), 0x65.toByte(), 0x79.toByte(), 0x45.toByte(),
        0x6E.toByte(), 0x67.toByte(), 0x41.toByte(), 0x62.toByte(),
        0x64.toByte(), 0x75.toByte(), 0x6C.toByte(), 0x52.toByte()
    )

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val PREFIX = "ENC:"

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        try {
            val secretKey = SecretKeySpec(KEY_BYTES, ALGORITHM)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = iv + encryptedBytes
            return PREFIX + Base64.getEncoder().encodeToString(combined)
        } catch (e: Exception) {
            return plainText
        }
    }

    fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return ""
        if (!cipherText.startsWith(PREFIX)) return cipherText // Not encrypted or legacy
        try {
            val base64Data = cipherText.removePrefix(PREFIX)
            val combined = Base64.getDecoder().decode(base64Data)
            if (combined.size <= 16) return ""
            val iv = combined.copyOfRange(0, 16)
            val encryptedBytes = combined.copyOfRange(16, combined.size)
            val secretKey = SecretKeySpec(KEY_BYTES, ALGORITHM)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            return cipherText
        }
    }
}
