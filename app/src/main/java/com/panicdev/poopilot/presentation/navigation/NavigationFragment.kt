package com.panicdev.poopilot.presentation.navigation

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
import com.panicdev.poopilot.R
import com.panicdev.poopilot.databinding.FragmentNavigationBinding
import com.panicdev.poopilot.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 화장실로 가는 길 안내 화면을 담당하는 Fragment입니다.
 *
 * 목적지까지의 남은 거리, 남은 시간, 회전 안내(TBT) 등을 표시하며
 * 도착 시 자동으로 메인 화면으로 돌아갑니다.
 * 설정에 따라 도착 시 차량 문 잠금 해제도 수행합니다.
 */
@AndroidEntryPoint
class NavigationFragment : Fragment() {

    /** View Binding 객체. onDestroyView에서 메모리 누수 방지를 위해 null로 초기화됩니다. */
    private var _binding: FragmentNavigationBinding? = null
    /** null 안전하게 binding에 접근하기 위한 프로퍼티 */
    private val binding get() = _binding!!
    /** Activity 범위의 ViewModel. 앱 상태 리셋 등을 위해 MainViewModel을 공유합니다. */
    private val mainViewModel: MainViewModel by activityViewModels()
    /** 이 화면 전용 NavigationViewModel. 길 안내 로직을 담당합니다. */
    private val navViewModel: NavigationViewModel by viewModels()

    /**
     * Fragment의 레이아웃을 inflate하고 루트 뷰를 반환합니다.
     * View Binding을 초기화하는 단계입니다.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNavigationBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * 뷰가 완전히 생성된 후 호출됩니다.
     * 이전 화면에서 전달받은 목적지 정보로 길 안내를 시작하고
     * 버튼 클릭 리스너와 ViewModel 관찰을 설정합니다.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ATP_SCREEN", "enter: ${this::class.simpleName}")

        // Bundle에서 목적지 정보 수신
        val destName = arguments?.getString("destName") ?: ""
        val destAddr = arguments?.getString("destAddr") ?: ""
        val destLat = arguments?.getDouble("destLat") ?: 0.0
        val destLng = arguments?.getDouble("destLng") ?: 0.0

        // 목적지 정보가 유효한 경우 길 안내를 시작하고, 아니면 이전 화면으로 돌아갑니다
        if (destName.isNotBlank() && destLat != 0.0 && destLng != 0.0) {
            navViewModel.startNavigation(destName, destAddr, destLat, destLng)
        } else {
            Toast.makeText(requireContext(), "목적지 정보가 올바르지 않습니다", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        // 취소 버튼: 길 안내를 중단하고 메인 화면으로 돌아갑니다
        binding.btnCancel.setOnClickListener {
            navViewModel.cancelNavigation()
            mainViewModel.resetToStandby()
            findNavController().navigate(R.id.action_navigation_to_main)
        }

        // 경로 변경 버튼: 현재 길 안내를 취소하고 검색 화면으로 돌아가 다시 선택합니다
        binding.btnChangeRoute.setOnClickListener {
            navViewModel.cancelNavigation()
            findNavController().navigateUp()
        }

        observeViewModel()
    }

    /**
     * ViewModel의 LiveData를 관찰하여 UI를 최신 상태로 유지합니다.
     * 목적지 이름/주소, 남은 시간/거리, 회전 안내, 도착 여부, TTS 메시지를 처리합니다.
     */
    private fun observeViewModel() {
        // 목적지 이름을 화면에 표시합니다
        navViewModel.destinationName.observe(viewLifecycleOwner) { name ->
            binding.tvDestName.text = name
        }

        // 목적지 주소를 화면에 표시합니다
        navViewModel.destinationAddress.observe(viewLifecycleOwner) { addr ->
            binding.tvDestAddr.text = addr
        }

        // 남은 도착 예상 시간을 화면에 표시합니다
        navViewModel.remainingTime.observe(viewLifecycleOwner) { time ->
            binding.tvTimeValue.text = time
        }

        // 남은 거리를 화면에 표시합니다
        navViewModel.remainingDistance.observe(viewLifecycleOwner) { dist ->
            binding.tvDistValue.text = "$dist 남음"
        }

        // 다음 회전 안내(TBT)가 있으면 표시하고, 없으면 숨깁니다
        navViewModel.tbtDescription.observe(viewLifecycleOwner) { desc ->
            Log.d("ATP_RENDER", "renderState: screen=NavigationFragment, tbtVisible=${!desc.isNullOrBlank()}, tbtDesc=$desc")
            binding.tvTbtDesc?.let { tv ->
                tv.text = desc
                tv.visibility = if (desc.isNullOrBlank()) View.GONE else View.VISIBLE
            }
        }

        // 목적지에 도착하면 메시지를 표시하고 메인 화면으로 이동합니다
        navViewModel.hasArrived.observe(viewLifecycleOwner) { arrived ->
            if (arrived) {
                navViewModel.onArrivalConsumed()
                Toast.makeText(requireContext(), "도착했습니다!", Toast.LENGTH_LONG).show()
                mainViewModel.resetToStandby()
                findNavController().navigate(R.id.action_navigation_to_main)
            }
        }

        // TTS(음성 안내) 메시지가 있으면 토스트로 표시합니다
        navViewModel.ttsMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                navViewModel.onTtsMessageConsumed()
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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
