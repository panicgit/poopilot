package com.panicdev.poopilot.presentation.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.panicdev.poopilot.R
import com.panicdev.poopilot.data.db.FavoriteRestroom

class FavoriteAdapter(
    private val onItemClick: (FavoriteRestroom) -> Unit
) : ListAdapter<FavoriteRestroom, FavoriteAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_restroom, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvFavName)
        private val tvInfo: TextView = itemView.findViewById(R.id.tvFavInfo)

        fun bind(item: FavoriteRestroom) {
            tvName.text = item.placeName
            val info = if (item.isFavorite) "★ ${item.visitCount}회 방문" else "${item.visitCount}회 방문"
            tvInfo.text = info
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<FavoriteRestroom>() {
        override fun areItemsTheSame(a: FavoriteRestroom, b: FavoriteRestroom) = a.id == b.id
        override fun areContentsTheSame(a: FavoriteRestroom, b: FavoriteRestroom) = a == b
    }
}
