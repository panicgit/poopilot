package com.panicdev.poopilot.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.panicdev.poopilot.R
import com.panicdev.poopilot.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    private val doorUnlockListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        viewModel.setDoorUnlockEnabled(isChecked)
    }
    private val voiceListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        viewModel.setVoiceCommandEnabled(isChecked)
    }
    private val ttsListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        viewModel.setTtsEnabled(isChecked)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupRadiusButtons()
        setupSwitches()
        observeViewModel()
    }

    private fun setupRadiusButtons() {
        binding.btnRadius500.setOnClickListener { viewModel.setSearchRadius(500) }
        binding.btnRadius1km.setOnClickListener { viewModel.setSearchRadius(1000) }
        binding.btnRadius2km.setOnClickListener { viewModel.setSearchRadius(2000) }
    }

    private fun setupSwitches() {
        binding.switchDoorUnlock.setOnCheckedChangeListener(doorUnlockListener)
        binding.switchVoice.setOnCheckedChangeListener(voiceListener)
        binding.switchTts?.setOnCheckedChangeListener(ttsListener)
    }

    private fun observeViewModel() {
        viewModel.searchRadius.observe(viewLifecycleOwner) { radius ->
            updateRadiusUI(radius)
        }
        viewModel.doorUnlockEnabled.observe(viewLifecycleOwner) { enabled ->
            setSwitchCheckedSilently(binding.switchDoorUnlock, enabled, doorUnlockListener)
        }
        viewModel.voiceCommandEnabled.observe(viewLifecycleOwner) { enabled ->
            setSwitchCheckedSilently(binding.switchVoice, enabled, voiceListener)
        }
        viewModel.ttsEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchTts?.let { setSwitchCheckedSilently(it, enabled, ttsListener) }
        }
    }

    private fun setSwitchCheckedSilently(
        switch: CompoundButton,
        checked: Boolean,
        listener: CompoundButton.OnCheckedChangeListener
    ) {
        switch.setOnCheckedChangeListener(null)
        switch.isChecked = checked
        switch.setOnCheckedChangeListener(listener)
    }

    private fun updateRadiusUI(radius: Int) {
        val buttons = listOf(
            binding.btnRadius500 to 500,
            binding.btnRadius1km to 1000,
            binding.btnRadius2km to 2000
        )
        buttons.forEach { (btn, value) ->
            if (value == radius) {
                btn.setBackgroundResource(R.drawable.button_selected_bg)
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                btn.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                btn.setBackgroundResource(R.drawable.card_bg)
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                btn.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
