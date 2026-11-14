package com.cbsedudesk.mobile

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** A local PIN verifier. The verifier is itself encrypted by SecureVault. */
object PinSecurity {
    private const val ITERATIONS = 180_000
    private const val KEY_BITS = 256

    fun createVerifier(pin: String): String {
        require(pin.matches(Regex("[0-9]{4}"))) { "PIN must contain exactly four digits." }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derive(pin, salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun verify(pin: String, verifier: String): Boolean {
        if (!pin.matches(Regex("[0-9]{4}"))) return false
        return try {
            val parts = verifier.split(":")
            if (parts.size != 2) return false
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val expected = Base64.decode(parts[1], Base64.NO_WRAP)
            MessageDigest.isEqual(expected, derive(pin, salt))
        } catch (_: Exception) {
            false
        }
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
