package com.panicdev.poopilot.presentation.settings

import android.os.Bundle
import android.util.Log
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

/**
 * 앱 설정 화면을 담당하는 Fragment입니다.
 *
 * 사용자가 검색 반경, 도어 잠금 해제, 음성 명령, TTS(음성 안내) 등의
 * 설정을 조정할 수 있는 화면입니다.
 *
 * 스위치 상태 변경 시 ViewModel을 통해 설정을 즉시 저장합니다.
 * 스위치에 리스너를 직접 붙이되, ViewModel 관찰로 인한 무한 루프를 방지하기 위해
 * 리스너를 일시적으로 해제하고 값을 설정하는 방식([setSwitchCheckedSilently])을 사용합니다.
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    /** View Binding 객체. onDestroyView에서 메모리 누수 방지를 위해 null로 초기화됩니다. */
    private var _binding: FragmentSettingsBinding? = null
    /** null 안전하게 binding에 접근하기 위한 프로퍼티 */
    private val binding get() = _binding!!
    /** 설정 화면 전용 ViewModel. 설정값의 읽기/저장을 담당합니다. */
    private val viewModel: SettingsViewModel by viewModels()

    /** 도어 잠금 해제 스위치의 변경 이벤트를 처리하는 리스너 */
    private val doorUnlockListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        viewModel.setDoorUnlockEnabled(isChecked)
    }
    /** 음성 명령 스위치의 변경 이벤트를 처리하는 리스너 */
    private val voiceListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        viewModel.setVoiceCommandEnabled(isChecked)
    }
    /** TTS(음성 안내) 스위치의 변경 이벤트를 처리하는 리스너 */
    private val ttsListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        viewModel.setTtsEnabled(isChecked)
    }

    /**
     * Fragment의 레이아웃을 inflate하고 루트 뷰를 반환합니다.
     * View Binding을 초기화하는 단계입니다.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * 뷰가 완전히 생성된 후 호출됩니다.
     * 뒤로가기 버튼, 검색 반경 버튼, 스위치를 초기화하고 ViewModel 관찰을 시작합니다.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ATP_SCREEN", "enter: ${this::class.simpleName}")

        // 뒤로가기 버튼: 이전 화면(메인)으로 돌아갑니다
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupRadiusButtons()
        setupSwitches()
        observeViewModel()
    }

    /**
     * 검색 반경 선택 버튼(500m, 1km, 2km)의 클릭 리스너를 설정합니다.
     * 버튼을 누르면 해당 반경 값이 ViewModel을 통해 즉시 저장됩니다.
     */
    private fun setupRadiusButtons() {
        binding.btnRadius500.setOnClickListener { viewModel.setSearchRadius(500) }
        binding.btnRadius1km.setOnClickListener { viewModel.setSearchRadius(1000) }
        binding.btnRadius2km.setOnClickListener { viewModel.setSearchRadius(2000) }
    }

    /**
     * 각 스위치에 변경 이벤트 리스너를 연결합니다.
     * 스위치 상태가 바뀌면 바로 ViewModel에 반영되어 설정이 저장됩니다.
     */
    private fun setupSwitches() {
        binding.switchDoorUnlock.setOnCheckedChangeListener(doorUnlockListener)
        binding.switchVoice.setOnCheckedChangeListener(voiceListener)
        binding.switchTts?.setOnCheckedChangeListener(ttsListener)
    }

    /**
     * ViewModel의 LiveData를 관찰하여 현재 설정값으로 UI를 초기화하고 동기화합니다.
     * 설정값이 변경되면 해당 UI 요소를 조용히(리스너 해제 후) 업데이트합니다.
     */
    private fun observeViewModel() {
        // 검색 반경이 변경되면 선택된 버튼을 강조 표시합니다
        viewModel.searchRadius.observe(viewLifecycleOwner) { radius ->
            Log.d("ATP_RENDER", "renderState: screen=SettingsFragment, searchRadius=$radius")
            updateRadiusUI(radius)
        }
        // 도어 잠금 해제 설정이 변경되면 스위치 상태를 조용히 업데이트합니다
        viewModel.doorUnlockEnabled.observe(viewLifecycleOwner) { enabled ->
            setSwitchCheckedSilently(binding.switchDoorUnlock, enabled, doorUnlockListener)
        }
        // 음성 명령 설정이 변경되면 스위치 상태를 조용히 업데이트합니다
        viewModel.voiceCommandEnabled.observe(viewLifecycleOwner) { enabled ->
            setSwitchCheckedSilently(binding.switchVoice, enabled, voiceListener)
        }
        // TTS 설정이 변경되면 스위치 상태를 조용히 업데이트합니다
        viewModel.ttsEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchTts?.let { setSwitchCheckedSilently(it, enabled, ttsListener) }
        }
    }

    /**
     * 스위치의 체크 상태를 리스너 없이 조용히 변경합니다.
     *
     * ViewModel에서 LiveData 변경이 오면 스위치를 업데이트해야 하는데,
     * 이때 리스너가 붙어 있으면 ViewModel이 다시 호출되는 무한 루프가 발생합니다.
     * 이를 방지하기 위해 리스너를 잠시 제거했다가 값 설정 후 다시 붙입니다.
     *
     * @param switch 업데이트할 스위치 뷰
     * @param checked 설정할 체크 상태
     * @param listener 다시 연결할 체크 변경 리스너
     */
    private fun setSwitchCheckedSilently(
        switch: CompoundButton,
        checked: Boolean,
        listener: CompoundButton.OnCheckedChangeListener
    ) {
        switch.setOnCheckedChangeListener(null)
        switch.isChecked = checked
        switch.setOnCheckedChangeListener(listener)
    }

    /**
     * 현재 선택된 검색 반경에 맞는 버튼을 강조 표시하고 나머지는 기본 스타일로 되돌립니다.
     *
     * @param radius 현재 설정된 검색 반경(미터 단위)
     */
    private fun updateRadiusUI(radius: Int) {
        val buttons = listOf(
            binding.btnRadius500 to 500,
            binding.btnRadius1km to 1000,
            binding.btnRadius2km to 2000
        )
        buttons.forEach { (btn, value) ->
            if (value == radius) {
                // 선택된 버튼: 강조 배경과 굵은 글씨로 표시합니다
                btn.setBackgroundResource(R.drawable.button_selected_bg)
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                btn.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                // 선택되지 않은 버튼: 기본 배경과 보통 글씨로 표시합니다
                btn.setBackgroundResource(R.drawable.card_bg)
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                btn.setTypeface(null, android.graphics.Typeface.NORMAL)
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
