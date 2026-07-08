package com.trader.salesmanager.ui.settings.backup

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal object BackupCrypto {
    private val MAGIC = byteArrayOf(0x53, 0x4D, 0x42, 0x4B)
    private const val ENCRYPTION_FORMAT_VERSION: Byte = 1
    private const val SALT_LENGTH = 32
    private const val IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_LENGTH_BYTES = 32
    private const val HEADER_SIZE = 61

    data class FileHeader(
        val schemaVersion: Int,
        val exportedAt: Long,
        val salt: ByteArray,
        val iv: ByteArray
    )

    fun isEncryptedBackup(bytes: ByteArray): Boolean =
        bytes.size >= HEADER_SIZE && bytes.copyOfRange(0, 4).contentEquals(MAGIC)

    fun encryptZip(
        zipBytes: ByteArray,
        password: CharArray,
        schemaVersion: Int,
        exportedAt: Long
    ): ByteArray {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(zipBytes)
        return buildHeader(schemaVersion, exportedAt, salt, iv) + ciphertext
    }

    fun decryptToZip(bytes: ByteArray, password: CharArray): ByteArray {
        if (!isEncryptedBackup(bytes)) throw BackupException.InvalidFile
        val header = parseHeader(bytes)
        val ciphertext = bytes.copyOfRange(HEADER_SIZE, bytes.size)
        val key = deriveKey(password, header.salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, header.iv))
            cipher.doFinal(ciphertext)
        } catch (_: AEADBadTagException) {
            throw BackupException.WrongPasswordOrCorrupted
        }
    }

    fun parseHeader(bytes: ByteArray): FileHeader {
        if (!isEncryptedBackup(bytes)) throw BackupException.InvalidFile
        val buffer = ByteBuffer.wrap(bytes, 0, HEADER_SIZE).order(ByteOrder.BIG_ENDIAN)
        buffer.position(4)
        val formatVersion = buffer.get()
        if (formatVersion != ENCRYPTION_FORMAT_VERSION) throw BackupException.InvalidFile
        val schemaVersion = buffer.int
        val exportedAt = buffer.long
        val salt = ByteArray(SALT_LENGTH).also { buffer.get(it) }
        val iv = ByteArray(IV_LENGTH).also { buffer.get(it) }
        return FileHeader(schemaVersion, exportedAt, salt, iv)
    }

    private fun buildHeader(
        schemaVersion: Int,
        exportedAt: Long,
        salt: ByteArray,
        iv: ByteArray
    ): ByteArray {
        val buffer = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.BIG_ENDIAN)
        buffer.put(MAGIC)
        buffer.put(ENCRYPTION_FORMAT_VERSION)
        buffer.putInt(schemaVersion)
        buffer.putLong(exportedAt)
        buffer.put(salt)
        buffer.put(iv)
        return buffer.array()
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BYTES * 8)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val encoded = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(encoded, "AES")
    }
}
