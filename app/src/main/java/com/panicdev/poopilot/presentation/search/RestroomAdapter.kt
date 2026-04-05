package com.panicdev.poopilot.presentation.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.panicdev.poopilot.data.model.KakaoPlace
import com.panicdev.poopilot.databinding.ItemRestroomResultBinding

/**
 * 화장실 검색 결과 목록을 RecyclerView에 표시하기 위한 어댑터입니다.
 *
 * 카카오, 네이버, 공공 데이터 등 여러 출처에서 합쳐진 [KakaoPlace] 목록을
 * 화면에 표시하며, 각 항목의 "길 안내" 버튼을 누르면 [onNavigateClick]이 호출됩니다.
 *
 * @param onNavigateClick 목록의 항목에서 길 안내 버튼을 눌렀을 때 실행할 람다 함수입니다.
 */
class RestroomAdapter(
    private val onNavigateClick: (KakaoPlace) -> Unit
) : ListAdapter<KakaoPlace, RestroomAdapter.ViewHolder>(DiffCallback) {

    /** 새 뷰홀더를 생성합니다. View Binding을 사용하여 아이템 레이아웃을 inflate합니다. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRestroomResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    /** 특정 위치(position)의 데이터를 뷰홀더에 연결합니다. */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * 개별 화장실 검색 결과 항목을 화면에 표시하는 뷰홀더입니다.
     * 화장실 이름, 거리, 카테고리를 표시하고 길 안내 버튼 클릭을 처리합니다.
     */
    inner class ViewHolder(
        private val binding: ItemRestroomResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * 전달받은 [KakaoPlace] 데이터로 뷰의 내용을 채웁니다.
         * 카테고리명이 있으면 "거리 · 카테고리" 형식으로 부가 정보를 표시합니다.
         */
        fun bind(place: KakaoPlace) {
            binding.tvPlaceName.text = place.placeName
            binding.tvPlaceInfo.text = buildString {
                append(place.distance)
                append("m")
                if (place.categoryName.isNotBlank()) {
                    append(" · ")
                    // 카테고리의 마지막 단계만 표시합니다. 예: "공공시설 > 화장실" → "화장실"
                    append(place.categoryName.substringAfterLast(">").trim())
                }
            }
            // 길 안내 버튼 클릭 시 선택된 장소를 콜백으로 전달합니다
            binding.btnNavigate.setOnClickListener {
                onNavigateClick(place)
            }
        }
    }

    /**
     * RecyclerView가 목록 변경을 효율적으로 처리할 수 있도록 돕는 DiffCallback입니다.
     * 좌표(x, y)가 같으면 동일한 장소로, 모든 필드가 같으면 내용이 같은 것으로 판단합니다.
     */
    companion object DiffCallback : DiffUtil.ItemCallback<KakaoPlace>() {
        /** 두 장소의 좌표(경도, 위도)가 동일하면 같은 장소로 판단합니다. */
        override fun areItemsTheSame(oldItem: KakaoPlace, newItem: KakaoPlace): Boolean {
            return oldItem.x == newItem.x && oldItem.y == newItem.y
        }

        /** 두 장소의 모든 데이터가 동일하면 변경이 없는 것으로 판단합니다. */
        override fun areContentsTheSame(oldItem: KakaoPlace, newItem: KakaoPlace): Boolean {
            return oldItem == newItem
        }
    }
}
