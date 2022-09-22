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

package com.raywenderlich.android.snitcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.raywenderlich.android.snitcher.model.User
import com.raywenderlich.android.snitcher.model.Users
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.text.DateFormat
import java.util.Date
import kotlin.collections.ArrayList
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import org.simpleframework.xml.core.Persister

/**
 * Main Screen
 */
class MainActivity : AppCompatActivity() {

  private var isSignedUp = false
  private var workingFile: File? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    workingFile = File(filesDir.absolutePath + File.separator +
        FileConstants.DATA_SOURCE_FILE_NAME)

    updateLoggedInState()
  }

  fun loginPressed(view: View ) {

    var success = false
    val password = login_password.text.toString()

    //Check if already signed up
    if (isSignedUp) {

      val fileInputStream = FileInputStream(workingFile)
      val objectInputStream = ObjectInputStream(fileInputStream)
      val list = objectInputStream.readObject() as ArrayList<User>
      val firstUser = list.first()
      if (firstUser.password == password) {
        success = true
      }

      if (success) {
        toast("Last login: ${lastLoggedIn()}")
      } else {
        toast("Please check your password and try again.")
      }

    } else {
      when {
        password.isEmpty() -> toast("Please enter a password!")
        password == login_confirm_password.text.toString() -> workingFile?.let {
          createDataSource("users.xml", it)
          success = true
        }
        else -> toast("Passwords do not match!")
      }
    }

    if (success) {

      saveLastLoggedInTime()

      //Start next activity
      val context = view.context
      val reportListIntent = Intent(context, ReportListActivity::class.java)
      reportListIntent.putExtra(PWD_KEY, password.toCharArray())
      context.startActivity(reportListIntent)
    }
  }

  private fun updateLoggedInState() {
    val fileExists = workingFile?.exists() ?: false
    if (fileExists) {
      isSignedUp = true
      button.text = getString(R.string.login)
      login_confirm_password.visibility = View.INVISIBLE
    } else {
      button.text = getString(R.string.signup)
    }
  }

  private fun lastLoggedIn(): String? {
    val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    return preferences.getString("lastLogin", "")
  }

  private fun saveLastLoggedInTime() {
    val currentDateTimeString = DateFormat.getDateTimeInstance().format(Date())
    val editor = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).edit()
    editor.putString("lastLogin", currentDateTimeString)
    editor.apply()
  }

  private fun createDataSource(filename: String, outFile: File) {
    val inputStream = applicationContext.assets.open(filename)
    val serializer = Persister()
    val users = try { serializer.read(Users::class.java, inputStream) } catch (e: Exception) {null}

    users?.list?.let {

      val userList = ArrayList(it)
      val firstUser = userList.first()
      firstUser.password = login_password.text.toString()

      val fileOutputStream = FileOutputStream(outFile)
      val objectOutputStream = ObjectOutputStream(fileOutputStream)
      objectOutputStream.writeObject(userList)
      objectOutputStream.close()
      fileOutputStream.close()
    }
    inputStream.close()

  }

  companion object {
    private const val PWD_KEY = "PWD"
  }

  object FileConstants {
    const val DATA_SOURCE_FILE_NAME = "users.dat"
  }
}
