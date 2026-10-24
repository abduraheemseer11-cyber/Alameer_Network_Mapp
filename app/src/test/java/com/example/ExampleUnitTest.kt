package com.example

import com.example.map.MercatorProjection
import com.example.util.CryptoUtil
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testPasswordEncryptionAndDecryption() {
        val originalPassword = "MySecretPass@2026_Mikrotik"
        val encrypted = CryptoUtil.encrypt(originalPassword)

        assertNotEquals(originalPassword, encrypted)
        assertTrue(encrypted.startsWith("ENC:"))

        val decrypted = CryptoUtil.decrypt(encrypted)
        assertEquals(originalPassword, decrypted)
    }

    @Test
    fun testEmptyPasswordEncryption() {
        assertEquals("", CryptoUtil.encrypt(""))
        assertEquals("", CryptoUtil.decrypt(""))
    }

    @Test
    fun testDistanceCalculation() {
        // Sana'a coordinates
        val lat1 = 15.3694
        val lon1 = 44.1910
        val lat2 = 15.3704
        val lon2 = 44.1910

        val distance = MercatorProjection.distanceInMeters(lat1, lon1, lat2, lon2)
        assertTrue(distance > 100.0 && distance < 120.0) // ~111 meters

        val formattedArabic = MercatorProjection.formatDistanceArabic(distance)
        assertTrue(formattedArabic.contains("متر"))
    }

    @Test
    fun testKilometersFormatting() {
        val distanceKm = 2500.0
        val formatted = MercatorProjection.formatDistanceArabic(distanceKm)
        assertTrue(formatted.contains("كم"))
        assertTrue(formatted.contains("2.50"))
    }
}
