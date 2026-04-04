package com.panicdev.poopilot.presentation.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.panicdev.poopilot.R
import com.panicdev.poopilot.databinding.FragmentNavigationBinding
import com.panicdev.poopilot.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NavigationFragment : Fragment() {

    private var _binding: FragmentNavigationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

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

        binding.btnCancel.setOnClickListener {
            viewModel.resetToStandby()
            findNavController().navigate(R.id.action_navigation_to_main)
        }

        // TODO: Sprint 2에서 NaviHelper 경로 안내 연동
        // TODO: Sprint 3에서 TBT 실시간 업데이트, 도어 언락
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
