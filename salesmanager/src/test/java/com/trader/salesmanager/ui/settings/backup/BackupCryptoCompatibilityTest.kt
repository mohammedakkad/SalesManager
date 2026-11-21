package com.trader.salesmanager.ui.settings.backup

import com.google.gson.JsonParser
import io.mockk.*
import org.junit.*
import org.junit.Assert.*
import java.security.NoSuchAlgorithmException
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** The absent factory is observed on actual API24. The public envelope must retain version1 interoperability. */
class BackupCryptoCompatibilityTest {
    private fun vectors()=JsonParser.parseString(requireNotNull(javaClass.getResourceAsStream("/backup-v1-pbkdf2-vectors.json")).bufferedReader().use{it.readText()}).asJsonObject["vectors"].asJsonArray
    private fun unavailableFactory(){
        mockkStatic(SecretKeyFactory::class)
        every{SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")} throws NoSuchAlgorithmException("API24 provider fixture: PBKDF2WithHmacSHA256 unavailable")
    }
    @After fun releaseFactory(){unmockkStatic(SecretKeyFactory::class)}
    @Test fun encryptsAndInteroperatesWithTheNativeFactoryWhenTheNamedPlatformFactoryIsAbsent() {
        val referenceFactory=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        unavailableFactory()
        for(raw in vectors()){
            val vector=raw.asJsonObject
            val password=vector["password"].asString.toCharArray();val original=password.copyOf()
            val plaintext=Base64.getDecoder().decode(vector["plaintextBase64"].asString)
            val bytes=BackupCrypto.encryptZip(plaintext,password,42,1700000000000)
            val header=BackupCrypto.parseHeader(bytes)
            assertEquals(42,header.schemaVersion);assertEquals(1700000000000,header.exportedAt)
            val spec=PBEKeySpec(password,header.salt,100000,256)
            val key=try{referenceFactory.generateSecret(spec).encoded}finally{spec.clearPassword()}
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            try{
                cipher.init(Cipher.DECRYPT_MODE,SecretKeySpec(key,"AES"),GCMParameterSpec(128,header.iv))
                assertArrayEquals(plaintext,cipher.doFinal(bytes.copyOfRange(61,bytes.size)))
            }finally{key.fill(0)}
            assertArrayEquals(plaintext,BackupCrypto.decryptToZip(bytes,password))
            assertArrayEquals("Caller-owned password must retain its existing contract",original,password)
        }
    }
    @Test fun decryptsIndependentAsciiArabicAndEmojiVersionOneReferenceArchivesWithoutPlatformFactory() {
        unavailableFactory()
        for(raw in vectors()){
            val vector=raw.asJsonObject
            val bytes=Base64.getDecoder().decode(vector["ciphertextBase64"].asString)
            val digest=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt() and 255)}
            assertEquals(vector["ciphertextSha256"].asString,digest)
            assertArrayEquals(Base64.getDecoder().decode(vector["plaintextBase64"].asString),BackupCrypto.decryptToZip(bytes,vector["password"].asString.toCharArray()))
        }
    }
}
