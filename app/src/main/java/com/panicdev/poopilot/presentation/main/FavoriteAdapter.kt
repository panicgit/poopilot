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

/**
 * 즐겨찾기 및 최근 방문 화장실 목록을 RecyclerView에 표시하기 위한 어댑터입니다.
 *
 * [FavoriteRestroom] 데이터를 받아 각 항목을 카드 형태로 보여주며,
 * 항목을 탭하면 [onItemClick] 콜백이 호출됩니다.
 *
 * @param onItemClick 목록의 항목을 클릭했을 때 실행할 람다 함수입니다.
 */
class FavoriteAdapter(
    private val onItemClick: (FavoriteRestroom) -> Unit
) : ListAdapter<FavoriteRestroom, FavoriteAdapter.ViewHolder>(DiffCallback) {

    /** 새 뷰홀더를 생성합니다. 각 목록 항목의 레이아웃을 inflate해서 반환합니다. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_restroom, parent, false)
        return ViewHolder(view)
    }

    /** 특정 위치(position)의 데이터를 뷰홀더에 연결합니다. */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * 개별 화장실 항목을 화면에 표시하는 뷰홀더입니다.
     * 화장실 이름, 방문 횟수, 즐겨찾기 여부를 표시하고
     * 클릭 시 [onItemClick]을 호출합니다.
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        /** 화장실 이름을 표시하는 텍스트뷰 */
        private val tvName: TextView = itemView.findViewById(R.id.tvFavName)
        /** 방문 횟수 및 즐겨찾기 여부를 표시하는 텍스트뷰 */
        private val tvInfo: TextView = itemView.findViewById(R.id.tvFavInfo)

        /** 전달받은 [FavoriteRestroom] 데이터로 뷰의 내용을 채우고 클릭 리스너를 설정합니다. */
        fun bind(item: FavoriteRestroom) {
            tvName.text = item.placeName
            val info = if (item.isFavorite) "★ ${item.visitCount}회 방문" else "${item.visitCount}회 방문"
            tvInfo.text = info
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    /**
     * RecyclerView가 목록 변경을 효율적으로 처리할 수 있도록 돕는 DiffCallback입니다.
     * 동일한 항목인지(areItemsTheSame), 내용이 같은지(areContentsTheSame)를 비교합니다.
     */
    companion object DiffCallback : DiffUtil.ItemCallback<FavoriteRestroom>() {
        /** 두 항목의 고유 ID가 같으면 동일한 항목으로 판단합니다. */
        override fun areItemsTheSame(a: FavoriteRestroom, b: FavoriteRestroom) = a.id == b.id
        /** 두 항목의 모든 내용이 같으면 변경이 없는 것으로 판단합니다. */
        override fun areContentsTheSame(a: FavoriteRestroom, b: FavoriteRestroom) = a == b
    }
}
