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

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.raywenderlich.android.snitcher.R
import com.raywenderlich.android.snitcher.util.Encryption.Companion.encryptFile
import kotlinx.android.synthetic.main.activity_report_detail.*
import org.jetbrains.anko.toast
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger


class ReportDetailActivity : AppCompatActivity() {

  companion object {
    private const val API_URL = "https://example.com/?send_report"
    private const val PIC_FROM_GALLERY = 2
    private const val REPORT_KEY = "REPORT"
    private const val REPORT_APP_ID = 46341L
    private const val REPORT_PROVIDER_ID = 46341L
  }

  object ReportTracker {
    var reportNumber = AtomicInteger()
  }

  private var currentReportCategory: String? = null

  @Volatile
  private var isSendingReport = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    setContentView(R.layout.activity_report_detail)

    //setup ui
    details_edtxtview.imeOptions = EditorInfo.IME_ACTION_DONE
    details_edtxtview.setRawInputType(InputType.TYPE_CLASS_TEXT)

    //setup current report category
    currentReportCategory = intent.getSerializableExtra(REPORT_KEY) as String
    category_textview?.text = currentReportCategory
  }

  override fun onPause() {

    cacheDir.deleteRecursively()
    externalCacheDir?.deleteRecursively()

    super.onPause()
  }

  fun sendReportPressed(view: View) {
    currentReportCategory?.let {
      if (!isSendingReport) {
        isSendingReport = true
        Executors.newSingleThreadExecutor().execute {

          //1. Save report
          var reportString = details_edtxtview.text.toString()
          reportString = reportString.replace("\\", "")
              .replace(";", "").replace("%", "")
              .replace("\"", "").replace("\'", "")
          val reportID = UUID.randomUUID().toString()

          val file = File(filesDir.absolutePath, "$reportID.txt")
          val encryptedFile = encryptFile(baseContext, file)
          encryptedFile.openFileOutput().bufferedWriter().use {
            it.write(reportString)
          }

          synchronized(this) {
            ReportTracker.reportNumber.incrementAndGet()
          }

          //2. Send report
          val postParameters = mapOf("application_id" to REPORT_APP_ID * REPORT_PROVIDER_ID,
              "report_id" to reportID,
              "report" to reportString)
          if (postParameters.isNotEmpty()) {
            //send report
            val connection = URL(API_URL).openConnection() as HttpURLConnection
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.defaultUseCaches = false
            connection.useCaches = false
          }

          //3. Notify user
          runOnUiThread {
            isSendingReport = false
            var report: String
            synchronized(this) {
              //Locked.
              report = "Report: ${ReportTracker.reportNumber.get()}"
            }
            finish()
            toast("Thank you for your report.$report")
          }
        }
      }
    }
  }

  fun uploadPhotoPressed(view: View) {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this,
          arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
              Manifest.permission.READ_EXTERNAL_STORAGE), PIC_FROM_GALLERY)
    } else {
      val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
      startActivityForResult(galleryIntent, PIC_FROM_GALLERY)
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int,
                                          permissions: Array<String>, grantResults: IntArray) {
    when (requestCode) {
      PIC_FROM_GALLERY -> {
        // If request is cancelled, the result arrays are empty.
        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
          // Permission was granted
          val galleryIntent = Intent(Intent.ACTION_PICK,
              MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
          startActivityForResult(galleryIntent, PIC_FROM_GALLERY)
        }
        return
      }
      else -> {
        // Ignore all other requests.
      }
    }
  }

  public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    when (requestCode) {

      PIC_FROM_GALLERY ->

        if (resultCode == Activity.RESULT_OK) {

          //image from gallery
          val selectedImage = data?.data
          selectedImage?.let {
            getFilename(selectedImage)
          }
        }
      else -> println("Didn't select picture option")
    }
  }

  private fun getFilename(selectedImage: Uri) {
    // Validate image
    val isValid = isValidJPEGAtPath(selectedImage)
    if (isValid) {
      //get filename
      val fileNameColumn = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
      val nameCursor = contentResolver.query(selectedImage, fileNameColumn, null,
          null, null)
      nameCursor?.moveToFirst()
      val nameIndex = nameCursor?.getColumnIndex(fileNameColumn[0])
      var filename = ""
      nameIndex?.let {
        filename = nameCursor.getString(it)
      }
      nameCursor?.close()

      //update UI with filename
      upload_status_textview?.text = filename
    } else {
      toast("Please choose a JPEG image")
    }
  }

  private fun isValidJPEGAtPath(selectedImage: Uri): Boolean {
    var success = false
    val file = File(baseContext.cacheDir, "temp.jpg")
    val inputStream = contentResolver.openInputStream(selectedImage)
    val outputStream = contentResolver.openOutputStream(Uri.fromFile(file))
    outputStream?.let {
      inputStream?.copyTo(it)

      val randomAccessFile = RandomAccessFile(file, "r")
      val length = randomAccessFile.length()
      val lengthError = (length < 10L)
      val start = ByteArray(2)
      randomAccessFile.readFully(start)
      randomAccessFile.seek(length - 2)
      val end = ByteArray(2)
      randomAccessFile.readFully(end)
      success = !lengthError && start[0].toInt() == -1 && start[1].toInt() == -40 &&
          end[0].toInt() == -1 && end[1].toInt() == -39

      randomAccessFile.close()
      outputStream.close()
    }
    inputStream?.close()
    file.delete()

    return success
  }

  fun shareFile(file: File) {
    val contentUri = FileProvider.getUriForFile(applicationContext,
        "com.raywenderlich.android.snitcher.fileprovider",
        file)
    val sharingIntent = Intent(Intent.ACTION_SEND)
    sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    sharingIntent.type = "text/*"
    sharingIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
    startActivity(Intent.createChooser(sharingIntent, "Report"))
  }
}