package com.panicdev.poopilot.presentation.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.panicdev.poopilot.R
import com.panicdev.poopilot.databinding.FragmentNavigationBinding
import com.panicdev.poopilot.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NavigationFragment : Fragment() {

    private var _binding: FragmentNavigationBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private val navViewModel: NavigationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNavigationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bundle에서 목적지 정보 수신
        val destName = arguments?.getString("destName") ?: ""
        val destAddr = arguments?.getString("destAddr") ?: ""
        val destLat = arguments?.getDouble("destLat") ?: 0.0
        val destLng = arguments?.getDouble("destLng") ?: 0.0

        if (destName.isNotBlank() && destLat != 0.0 && destLng != 0.0) {
            navViewModel.startNavigation(destName, destAddr, destLat, destLng)
        } else {
            Toast.makeText(requireContext(), "목적지 정보가 올바르지 않습니다", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        // 취소 버튼
        binding.btnCancel.setOnClickListener {
            navViewModel.cancelNavigation()
            mainViewModel.resetToStandby()
            findNavController().navigate(R.id.action_navigation_to_main)
        }

        // 경로 변경 버튼
        binding.btnChangeRoute.setOnClickListener {
            navViewModel.cancelNavigation()
            findNavController().navigateUp()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        navViewModel.destinationName.observe(viewLifecycleOwner) { name ->
            binding.tvDestName.text = name
        }

        navViewModel.destinationAddress.observe(viewLifecycleOwner) { addr ->
            binding.tvDestAddr.text = addr
        }

        navViewModel.remainingTime.observe(viewLifecycleOwner) { time ->
            binding.tvTimeValue.text = time
        }

        navViewModel.remainingDistance.observe(viewLifecycleOwner) { dist ->
            binding.tvDistValue.text = "$dist 남음"
        }

        navViewModel.tbtDescription.observe(viewLifecycleOwner) { desc ->
            binding.tvTbtDesc?.let { tv ->
                tv.text = desc
                tv.visibility = if (desc.isNullOrBlank()) View.GONE else View.VISIBLE
            }
        }

        navViewModel.hasArrived.observe(viewLifecycleOwner) { arrived ->
            if (arrived) {
                navViewModel.onArrivalConsumed()
                Toast.makeText(requireContext(), "도착했습니다!", Toast.LENGTH_LONG).show()
                mainViewModel.resetToStandby()
                findNavController().navigate(R.id.action_navigation_to_main)
            }
        }

        navViewModel.ttsMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                navViewModel.onTtsMessageConsumed()
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
