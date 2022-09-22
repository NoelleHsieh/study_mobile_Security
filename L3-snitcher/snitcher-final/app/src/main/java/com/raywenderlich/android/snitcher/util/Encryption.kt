/*
 * Copyright (c) 2019 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.snitcher.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


class Encryption {
  companion object {

    private const val KEYSTORE_ALIAS = "SnitcherLoginKey"
    private const val PROVIDER = "AndroidKeyStore"

    private fun getSecretKey(): SecretKey {
      val keyStore = KeyStore.getInstance(PROVIDER)

      // Before the keystore can be accessed, it must be loaded.
      keyStore.load(null)
      return keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
    }

    private fun getCipher(): Cipher {
      return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
          + KeyProperties.BLOCK_MODE_GCM + "/"
          + KeyProperties.ENCRYPTION_PADDING_NONE)
    }

    fun generateSecretKey() {
      val keyGenParameterSpec = KeyGenParameterSpec.Builder(
          KEYSTORE_ALIAS,
          KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
          .setBlockModes(KeyProperties.BLOCK_MODE_GCM) // 1
          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
          .setUserAuthenticationRequired(true) // 2
          .setUserAuthenticationValidityDurationSeconds(120) // 3
          .build()
      val keyGenerator = KeyGenerator.getInstance(
          KeyProperties.KEY_ALGORITHM_AES, PROVIDER) // 4
      keyGenerator.init(keyGenParameterSpec)
      keyGenerator.generateKey()
    }

    fun createLoginPassword(context: Context): ByteArray {
      val cipher = getCipher()
      val secretKey = getSecretKey()
      val random = SecureRandom()
      val passwordBytes = ByteArray(256)
      random.nextBytes(passwordBytes) // 1
      cipher.init(Cipher.ENCRYPT_MODE, secretKey)
      val ivParameters = cipher.parameters.getParameterSpec(GCMParameterSpec::class.java)
      val iv = ivParameters.iv
      PreferencesHelper.saveIV(context, iv) // 2
      return cipher.doFinal(passwordBytes) // 3
    }

    fun decryptPassword(context: Context, password: ByteArray): ByteArray {
      val cipher = getCipher()
      val secretKey = getSecretKey()
      val iv = PreferencesHelper.iv(context)
      val ivParameters = GCMParameterSpec(128, iv)
      cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameters)
      return cipher.doFinal(password)
    }

    fun encryptFile(context: Context, file: File): EncryptedFile {
      val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
      val masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
      return EncryptedFile.Builder(
          file,
          context,
          masterKeyAlias,
          EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
      ).build()
    }
  }
}