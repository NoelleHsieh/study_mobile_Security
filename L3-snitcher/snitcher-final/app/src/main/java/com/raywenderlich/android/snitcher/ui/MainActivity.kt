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

package com.raywenderlich.android.snitcher.ui

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.raywenderlich.android.snitcher.R
import com.raywenderlich.android.snitcher.model.User
import com.raywenderlich.android.snitcher.model.Users
import com.raywenderlich.android.snitcher.util.Encryption.Companion.createLoginPassword
import com.raywenderlich.android.snitcher.util.Encryption.Companion.decryptPassword
import com.raywenderlich.android.snitcher.util.Encryption.Companion.generateSecretKey
import com.raywenderlich.android.snitcher.util.FileConstants
import com.raywenderlich.android.snitcher.util.PreferencesHelper.Companion.lastLoggedIn
import com.raywenderlich.android.snitcher.util.PreferencesHelper.Companion.saveLastLoggedInTime
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import org.simpleframework.xml.core.Persister
import java.io.*
import java.util.concurrent.Executors
import java.util.regex.Pattern


/**
 * Main Screen
 */
class MainActivity : AppCompatActivity() {

  private var isSignedUp = false
  private var workingFile: File? = null

  private lateinit var biometricPrompt: BiometricPrompt
  private lateinit var promptInfo: BiometricPrompt.PromptInfo

  private fun isValidEmailString(emailString: String): Boolean {
    return emailString.isNotEmpty() && Pattern.compile(EMAIL_REGEX).matcher(emailString).matches()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    workingFile = File(filesDir.absolutePath + File.separator +
        FileConstants.DATA_SOURCE_FILE_NAME)

    updateLoggedInState()
  }

  fun loginPressed(view: View) {
    val email = login_email.text.toString()
    if (!isSignedUp && !isValidEmailString(email)) {
      toast("Please enter a valid email.")
    } else {
      val biometricManager = BiometricManager.from(this)
      when (biometricManager.canAuthenticate()) {
        BiometricManager.BIOMETRIC_SUCCESS ->
          displayLogin(view, false)
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
          displayLogin(view, true)
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
          toast("Biometric features are currently unavailable.")
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
          toast("Please associate a biometric credential with your account.")
        else ->
          toast("An unknown error occurred. Please check your Biometric settings")
      }
    }
  }

  private fun updateLoggedInState() {
    val fileExists = workingFile?.exists() ?: false
    if (fileExists) {
      isSignedUp = true
      login_button.text = getString(R.string.login)
      login_email.visibility = View.INVISIBLE
    } else {
      login_button.text = getString(R.string.signup)
    }
  }

  private fun displayLogin(view: View, fallback: Boolean) {
    val executor = Executors.newSingleThreadExecutor()
    biometricPrompt = BiometricPrompt(this, executor, // 1
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationError(errorCode: Int,
                                             errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            runOnUiThread {
              toast("Authentication error: $errString")
            }
          }

          override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            runOnUiThread {
              toast("Authentication failed")
            }
          }

          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {// 2
            super.onAuthenticationSucceeded(result)

            runOnUiThread {
              toast("Authentication succeeded!")
              if (!isSignedUp) {
                generateSecretKey() // 3
              }
              performLoginOperation(view)
            }
          }
        })

    if (fallback) {
      promptInfo = BiometricPrompt.PromptInfo.Builder()
          .setTitle("Biometric login for my app")
          .setSubtitle("Log in using your biometric credential")
          // Cannot call setNegativeButtonText() and
          // setDeviceCredentialAllowed() at the same time.
          // .setNegativeButtonText("Use account password")
          .setDeviceCredentialAllowed(true) // 4
          .build()
    } else {
      promptInfo = BiometricPrompt.PromptInfo.Builder()
          .setTitle("Biometric login for my app")
          .setSubtitle("Log in using your biometric credential")
          .setNegativeButtonText("Use account password")
          .build()
    }
    biometricPrompt.authenticate(promptInfo)
  }

  private fun performLoginOperation(view: View) {
    var success = false

    workingFile?.let {
      //Check if already signed up
      if (isSignedUp) {
        val fileInputStream = FileInputStream(it)
        val objectInputStream = ObjectInputStream(fileInputStream)
        val list = objectInputStream.readObject() as ArrayList<User>
        val firstUser = list.first() as? User
        if (firstUser is User) { //2
          val password = decryptPassword(this,
              Base64.decode(firstUser.password, Base64.NO_WRAP))
          if (password.isNotEmpty()) {
            //Send password to authenticate with server etc
            success = true
          }
        }

        if (success) {
          toast("Last login: ${lastLoggedIn(this)}")
        } else {
          toast("Please check your credentials and try again.")
        }

        objectInputStream.close()
        fileInputStream.close()
      } else {
        val encryptedInfo = createLoginPassword(this)
        createDataSource(it, encryptedInfo)
        success = true
      }
    }

    if (success) {
      saveLastLoggedInTime(this)

      //Start next activity
      val context = view.context
      val reportListIntent = Intent(context, ReportListActivity::class.java)
      context.startActivity(reportListIntent)
    }
  }

  private fun createDataSource(outFile: File, password: ByteArray) {
    val inputStream = applicationContext.assets.open("users.xml")
    val serializer = Persister()
    val users = try {
      serializer.read(Users::class.java, inputStream)
    } catch (e: Exception) {
      null
    }
    users?.list?.let {
      //1
      val userList = ArrayList(it) as? ArrayList
      if (userList is ArrayList<User>) { //2
        val firstUser = userList.first() as? User
        if (firstUser is User) { //3
          firstUser.password = Base64.encodeToString(password, Base64.NO_WRAP)
          val fileOutputStream = FileOutputStream(outFile)
          val objectOutputStream = ObjectOutputStream(fileOutputStream)
          objectOutputStream.writeObject(userList)

          objectOutputStream.close()
          fileOutputStream.close()
        }
      }
    }

    inputStream.close()
  }

  companion object {
    private const val EMAIL_REGEX = "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,4}$"
  }
}
