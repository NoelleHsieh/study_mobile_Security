/*
 * Copyright (c) 2020 Razeware LLC
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

package com.raywenderlich.android.petmed

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.recyclerview_item_row.view.*


class RecyclerAdapter(private val pets: ArrayList<Pet>) :
  RecyclerView.Adapter<RecyclerAdapter.PhotoHolder>() {

  override fun getItemCount() = pets.size

  override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
    val pet = pets[position]
    holder.bindPet(pet)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
    val inflatedView = parent.inflate(R.layout.recyclerview_item_row, false)
    return PhotoHolder(inflatedView)
  }

  class PhotoHolder(theView: View) : RecyclerView.ViewHolder(theView), View.OnClickListener {
    private val view = theView
    private var pet: Pet? = null

    init {
      theView.setOnClickListener(this)
    }

    override fun onClick(view: View) {
      val context = itemView.context
      val showDetailsIntent = Intent(context, PetDetailActivity::class.java)
      showDetailsIntent.putExtra(PET_KEY, pet)
      context.startActivity(showDetailsIntent)
    }

    fun bindPet(pet: Pet) {
      this.pet = pet
      Picasso.get().load(pet.url).into(view.itemImage)
      view.itemDate.text = pet.date
      view.itemName.text = pet.name
    }

    companion object {
      private const val PET_KEY = "PET"
    }
  }
}