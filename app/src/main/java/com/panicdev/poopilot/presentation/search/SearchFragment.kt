package com.panicdev.poopilot.presentation.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.panicdev.poopilot.R
import com.panicdev.poopilot.databinding.FragmentSearchBinding
import com.panicdev.poopilot.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

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

        binding.btnClose.setOnClickListener {
            viewModel.resetToStandby()
            findNavController().navigate(R.id.action_search_to_main)
        }

        // TODO: Sprint 2에서 검색 결과 RecyclerView 연동
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
