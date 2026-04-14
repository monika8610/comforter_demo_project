package com.example.comforterproject.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.comforterproject.R
import com.example.comforterproject.model.MusicAlbumItem

class MusicAlbumAdapter(
    private val items: List<MusicAlbumItem>,
    private val imageBaseUrl: String,
    private val onItemClick: (albumId: String, albumName: String, imageUrl: String?) -> Unit
) : RecyclerView.Adapter<MusicAlbumAdapter.MusicAlbumViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicAlbumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_music_album, parent, false)
        return MusicAlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicAlbumViewHolder, position: Int) {
        val item = items[position]
        holder.titleView.text = item.albumName

        val imageUrl = buildImageUrl(item.imageName)
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.home_banner_placeholder)
            .error(R.drawable.home_banner_placeholder)
            .into(holder.imageView)

        holder.itemView.setOnClickListener {
            onItemClick(item.id, item.albumName, imageUrl)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun buildImageUrl(imageName: String?): String? {
        if (imageName.isNullOrBlank()) return null
        val normalizedBase = imageBaseUrl.trimEnd('/')
        val normalizedName = imageName.trimStart('/')
        return "$normalizedBase/$normalizedName"
    }

    class MusicAlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.albumImage)
        val titleView: TextView = itemView.findViewById(R.id.albumTitle)
    }
}
