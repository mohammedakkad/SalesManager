package com.trader.salesmanager.ui.settings.backup
import org.junit.Assert.*
import org.junit.Test
class BackupCryptoTest {
    private val password="isolated-fixture-password".toCharArray()
    private fun encrypted()=BackupCrypto.encryptZip("مرحبا merchant fixture".toByteArray(),password,20,123L)
    @Test fun encryptedRoundTripPreservesArabicBytesAndHeader() {
        val bytes=encrypted()
        assertTrue(BackupCrypto.isEncryptedBackup(bytes))
        assertEquals(20,BackupCrypto.parseHeader(bytes).schemaVersion)
        assertEquals(123L,BackupCrypto.parseHeader(bytes).exportedAt)
        assertArrayEquals("مرحبا merchant fixture".toByteArray(),BackupCrypto.decryptToZip(bytes,password))
    }
    @Test fun randomSaltAndIvPreventIdenticalCiphertexts() { assertFalse(encrypted().contentEquals(encrypted())) }
    @Test fun wrongPasswordRejected() { assertThrows(BackupException.WrongPasswordOrCorrupted::class.java) { BackupCrypto.decryptToZip(encrypted(),"wrong".toCharArray()) } }
    @Test fun ciphertextTamperingRejected() {
        val bytes=encrypted(); bytes[bytes.lastIndex]=(bytes.last().toInt() xor 1).toByte()
        assertThrows(BackupException.WrongPasswordOrCorrupted::class.java) { BackupCrypto.decryptToZip(bytes,password) }
    }
    @Test fun truncatedHeaderRejected() { assertThrows(BackupException.InvalidFile::class.java) { BackupCrypto.decryptToZip(byteArrayOf(0x53,0x4d),password) } }
    @Test fun unknownEncryptionVersionRejected() {
        val bytes=encrypted();bytes[4]=99
        assertThrows(BackupException.InvalidFile::class.java) { BackupCrypto.decryptToZip(bytes,password) }
    }
    @Test fun emptyArchiveRoundTrips() {
        val bytes=BackupCrypto.encryptZip(byteArrayOf(),password,20,0L)
        assertArrayEquals(byteArrayOf(),BackupCrypto.decryptToZip(bytes,password))
    }
}
