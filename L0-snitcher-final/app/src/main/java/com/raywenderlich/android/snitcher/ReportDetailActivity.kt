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

import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.raywenderlich.android.snitcher.model.Report
import java.util.UUID
import kotlinx.android.synthetic.main.activity_report_detail.*
import org.jetbrains.anko.toast

class ReportDetailActivity : AppCompatActivity() {

  companion object {
    private const val REPORT_KEY = "REPORT"
    private const val REPORT_CLIENT_ID = "10000684"
    private const val REPORT_TOKEN = "EX05IF5R70LOKI98UI87BKDLFHJ5RHF6"
  }

  private var currentReportCategory: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_report_detail)

    //setup ui
    details_textview.imeOptions = EditorInfo.IME_ACTION_DONE
    details_textview.setRawInputType(InputType.TYPE_CLASS_TEXT)

    //setup current report category
    currentReportCategory = intent.getSerializableExtra(REPORT_KEY) as String
    category_textview?.text = currentReportCategory
  }

  fun sendReportPressed(view: android.view.View ) {
    currentReportCategory?.let {
      AsyncTask.execute {

        //1. Save report
        val appDatabase = Snitcher.database!!
        val listCategoryDao = appDatabase.listCategoryDao()
        val listCategory = Report(it, details_textview.text.toString(),
            UUID.randomUUID().toString())
        listCategoryDao.insertAll(listCategory)

        //2. Send report
        val urlString =
            "https://example.com/?send_report&client_id=$REPORT_CLIENT_ID&token=$REPORT_TOKEN"
        if (urlString.isNotEmpty()) {
          //send report here
        }

        //3. Notify user
        runOnUiThread {
          finish()
          toast("Thank you for your report.")
        }
      }
    }
  }
}