package com.panicdev.poopilot.presentation.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.panicdev.poopilot.data.model.KakaoPlace
import com.panicdev.poopilot.databinding.ItemRestroomResultBinding

class RestroomAdapter(
    private val onNavigateClick: (KakaoPlace) -> Unit
) : ListAdapter<KakaoPlace, RestroomAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRestroomResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemRestroomResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(place: KakaoPlace) {
            binding.tvPlaceName.text = place.placeName
            binding.tvPlaceInfo.text = buildString {
                append(place.distance)
                append("m")
                if (place.categoryName.isNotBlank()) {
                    append(" · ")
                    append(place.categoryName.substringAfterLast(">").trim())
                }
            }
            binding.btnNavigate.setOnClickListener {
                onNavigateClick(place)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<KakaoPlace>() {
        override fun areItemsTheSame(oldItem: KakaoPlace, newItem: KakaoPlace): Boolean {
            return oldItem.x == newItem.x && oldItem.y == newItem.y
        }

        override fun areContentsTheSame(oldItem: KakaoPlace, newItem: KakaoPlace): Boolean {
            return oldItem == newItem
        }
    }
}
