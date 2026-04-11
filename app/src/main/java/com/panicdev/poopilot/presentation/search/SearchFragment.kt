package com.panicdev.poopilot.presentation.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.panicdev.poopilot.R
import com.panicdev.poopilot.databinding.FragmentSearchBinding
import com.panicdev.poopilot.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 주변 화장실 검색 결과를 보여주는 Fragment입니다.
 *
 * 메인 화면에서 급똥 모드가 활성화되면 이 화면으로 이동합니다.
 * 카카오, 네이버, 공공 데이터 API를 동시에 호출하여 가장 가까운 화장실 목록을 표시하고,
 * 목록에서 화장실을 선택하면 길 안내 화면으로 이동합니다.
 */
@AndroidEntryPoint
class SearchFragment : Fragment() {

    /** View Binding 객체. onDestroyView에서 메모리 누수 방지를 위해 null로 초기화됩니다. */
    private var _binding: FragmentSearchBinding? = null
    /** null 안전하게 binding에 접근하기 위한 프로퍼티 */
    private val binding get() = _binding!!
    /** Activity 범위의 ViewModel. 현재 위치 좌표와 앱 상태를 공유합니다. */
    private val mainViewModel: MainViewModel by activityViewModels()
    /** 이 화면 전용 SearchViewModel. 화장실 검색 로직을 담당합니다. */
    private val searchViewModel: SearchViewModel by viewModels()
    /** 검색 결과를 표시하는 RecyclerView 어댑터 */
    private lateinit var adapter: RestroomAdapter

    /**
     * Fragment의 레이아웃을 inflate하고 루트 뷰를 반환합니다.
     * View Binding을 초기화하는 단계입니다.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * 뷰가 완전히 생성된 후 호출됩니다.
     * RecyclerView 초기화, ViewModel 관찰 설정, 닫기 버튼 처리,
     * 그리고 현재 위치를 이용한 화장실 검색을 시작합니다.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ATP_SCREEN", "enter: ${this::class.simpleName}")

        setupRecyclerView()
        observeViewModel()

        // 닫기 버튼: 검색을 취소하고 앱을 대기 상태로 돌려 메인 화면으로 이동합니다
        binding.btnClose.setOnClickListener {
            mainViewModel.resetToStandby()
            findNavController().navigate(R.id.action_search_to_main)
        }

        // 현재 위치로 검색 시작
        val lat = mainViewModel.currentLatitude.value ?: 0.0
        val lng = mainViewModel.currentLongitude.value ?: 0.0
        if (lat != 0.0 && lng != 0.0) {
            searchViewModel.searchRestrooms(lat, lng)
        }
    }

    /**
     * 검색 결과를 표시할 RecyclerView와 어댑터를 초기화합니다.
     * 항목을 선택하면 해당 장소 정보를 SearchViewModel에 전달합니다.
     */
    private fun setupRecyclerView() {
        adapter = RestroomAdapter { place ->
            searchViewModel.selectPlace(place)
        }
        binding.rvResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResults.adapter = adapter
    }

    /**
     * ViewModel의 LiveData를 관찰하여 UI를 최신 상태로 유지합니다.
     * 로딩 상태, 검색 결과 목록, 오류 메시지, 선택된 장소로의 이동을 처리합니다.
     */
    private fun observeViewModel() {
        // 검색 중에는 로딩 인디케이터와 "검색 중..." 텍스트를 표시합니다
        searchViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            Log.d("ATP_RENDER", "renderState: screen=SearchFragment, isLoading=$loading")
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.tvSearchTitle.text = if (loading) "화장실 검색 중..." else "검색 결과"
            binding.tvSearchSub.visibility = if (loading) View.VISIBLE else View.GONE
        }

        // 검색 결과 목록이 업데이트되면 어댑터에 전달하여 화면에 표시합니다
        searchViewModel.searchResults.observe(viewLifecycleOwner) { results ->
            Log.d("ATP_RENDER", "renderState: screen=SearchFragment, searchResultsCount=${results.size}")
            adapter.submitList(results)
        }

        // 오류가 발생하면 토스트 메시지로 사용자에게 알립니다
        searchViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        // 사용자가 화장실을 선택하면 길 안내 화면으로 이동합니다
        searchViewModel.selectedPlace.observe(viewLifecycleOwner) { place ->
            if (place != null) {
                searchViewModel.onPlaceSelectionConsumed()
                mainViewModel.setNavigating()
                // 선택된 장소 정보를 Bundle로 전달
                val bundle = Bundle().apply {
                    putString("destName", place.placeName)
                    putString("destAddr", place.roadAddressName.ifBlank { place.addressName })
                    putDouble("destLat", place.y.toDoubleOrNull() ?: 0.0)
                    putDouble("destLng", place.x.toDoubleOrNull() ?: 0.0)
                }
                findNavController().navigate(R.id.action_search_to_navigation, bundle)
            }
        }
    }

    /**
     * Fragment의 뷰가 소멸될 때 호출됩니다.
     * 메모리 누수를 방지하기 위해 바인딩 참조를 null로 초기화합니다.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
