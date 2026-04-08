package com.example.comforterproject.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.comforterproject.R
import com.example.comforterproject.model.PromiseItem

class PromiseAdapter(
    private val items: List<PromiseItem>
) : RecyclerView.Adapter<PromiseAdapter.PromiseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromiseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_promise_card, parent, false)
        return PromiseViewHolder(view)
    }

    override fun onBindViewHolder(holder: PromiseViewHolder, position: Int) {
        val item = items[position]
        val imageUrl = item.imageNameLarge ?: item.imageNameThumb

        Glide.with(holder.imageView.context)
            .load(imageUrl)
            .placeholder(R.drawable.home_banner_placeholder)
            .error(R.drawable.home_banner_placeholder)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = items.size

    class PromiseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.promiseImage)
    }
}
