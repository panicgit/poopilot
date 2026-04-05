package com.panicdev.poopilot.presentation.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.panicdev.poopilot.R
import com.panicdev.poopilot.databinding.FragmentMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private var favoritesAdapter: FavoriteAdapter? = null
    private var recentAdapter: FavoriteAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnEmergency.setOnClickListener {
            viewModel.activateEmergencyMode()
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_settings)
        }

        setupFavoriteLists()
        observeViewModel()
    }

    private fun setupFavoriteLists() {
        binding.rvFavorites?.let { rv ->
            favoritesAdapter = FavoriteAdapter { restroom ->
                viewModel.navigateToFavorite(restroom)
            }
            rv.layoutManager = LinearLayoutManager(requireContext())
            rv.adapter = favoritesAdapter
        }
        binding.rvRecent?.let { rv ->
            recentAdapter = FavoriteAdapter { restroom ->
                viewModel.navigateToFavorite(restroom)
            }
            rv.layoutManager = LinearLayoutManager(requireContext())
            rv.adapter = recentAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.navigateToSearch.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                viewModel.onNavigateToSearchConsumed()
                findNavController().navigate(R.id.action_main_to_search)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.voiceActivated.observe(viewLifecycleOwner) { activated ->
            if (activated) {
                viewModel.onVoiceActivatedConsumed()
                Toast.makeText(requireContext(), "음성 명령 감지! 급똥모드 활성화", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.appState.observe(viewLifecycleOwner) { state ->
            when (state) {
                AppState.SEARCHING -> {
                    binding.tvStatus.text = "검색 중..."
                    binding.statusDot.setBackgroundResource(R.drawable.status_dot_purple)
                }
                AppState.STANDBY -> {
                    binding.tvStatus.text = "대기 중"
                    binding.statusDot.setBackgroundResource(R.drawable.status_dot_green)
                }
                else -> {}
            }
        }

        viewModel.favorites.observe(viewLifecycleOwner) { list ->
            favoritesAdapter?.submitList(list)
            binding.rvFavorites?.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvFavEmpty?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.recentVisits.observe(viewLifecycleOwner) { list ->
            recentAdapter?.submitList(list)
            binding.rvRecent?.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvRecentEmpty?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.navigateToFavorite.observe(viewLifecycleOwner) { restroom ->
            if (restroom != null) {
                viewModel.onNavigateToFavoriteConsumed()
                viewModel.setNavigating()
                val bundle = Bundle().apply {
                    putString("destName", restroom.placeName)
                    putString("destAddr", restroom.roadAddressName.ifBlank { restroom.addressName })
                    putDouble("destLat", restroom.latitude)
                    putDouble("destLng", restroom.longitude)
                }
                findNavController().navigate(R.id.action_main_to_search, bundle)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        favoritesAdapter = null
        recentAdapter = null
        _binding = null
    }
}
