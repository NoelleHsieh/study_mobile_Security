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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.raywenderlich.android.snitcher.R
import kotlinx.android.synthetic.main.recyclerview_item_row.view.*

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
  return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

class RecyclerAdapter(private val reports: ArrayList<String>) :
    RecyclerView.Adapter<RecyclerAdapter.ReportHolder>() {

  override fun getItemCount() = reports.size

  override fun onBindViewHolder(holder: ReportHolder, position: Int) {
    val report = reports[position]
    holder.bindReport(report)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportHolder {
    val inflatedView = parent.inflate(R.layout.recyclerview_item_row, false)
    return ReportHolder(inflatedView)
  }

  class ReportHolder(mainView: View) : RecyclerView.ViewHolder(mainView), View.OnClickListener {
    private val view = mainView
    private var report: String? = null

    init {
      mainView.setOnClickListener(this)
    }

    override fun onClick(view: View) {
      val context = itemView.context
      val showDetailsIntent = Intent(context, ReportDetailActivity::class.java)
      showDetailsIntent.putExtra(REPORT_KEY, report)
      context.startActivity(showDetailsIntent)
    }

    fun bindReport(report: String) {
      this.report = report
      view.itemName.text = report
    }

    companion object {
      private const val REPORT_KEY = "REPORT"
    }
  }
}