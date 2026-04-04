package com.panicdev.poopilot.presentation.search

import android.os.Bundle
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

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var adapter: RestroomAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

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

    private fun setupRecyclerView() {
        adapter = RestroomAdapter { place ->
            searchViewModel.selectPlace(place)
        }
        binding.rvResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResults.adapter = adapter
    }

    private fun observeViewModel() {
        searchViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.tvSearchTitle.text = if (loading) "화장실 검색 중..." else "검색 결과"
            binding.tvSearchSub.visibility = if (loading) View.VISIBLE else View.GONE
        }

        searchViewModel.searchResults.observe(viewLifecycleOwner) { results ->
            adapter.submitList(results)
        }

        searchViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
