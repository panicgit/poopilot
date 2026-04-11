package com.panicdev.poopilot.presentation.main

import android.os.Bundle
import android.util.Log
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

/**
 * 앱의 메인 홈 화면을 담당하는 Fragment입니다.
 *
 * 사용자가 앱을 켰을 때 가장 먼저 보이는 화면으로,
 * 급똥 모드 버튼, 즐겨찾기 목록, 최근 방문 목록을 제공합니다.
 * 음성 명령으로도 급똥 모드를 활성화할 수 있습니다.
 */
@AndroidEntryPoint
class MainFragment : Fragment() {

    /** View Binding 객체. onDestroyView에서 메모리 누수 방지를 위해 null로 초기화됩니다. */
    private var _binding: FragmentMainBinding? = null
    /** null 안전하게 binding에 접근하기 위한 프로퍼티 */
    private val binding get() = _binding!!
    /** Activity 범위의 ViewModel. 다른 Fragment와 상태를 공유합니다. */
    private val viewModel: MainViewModel by activityViewModels()
    /** 즐겨찾기 목록을 표시하는 RecyclerView 어댑터 */
    private var favoritesAdapter: FavoriteAdapter? = null
    /** 최근 방문 목록을 표시하는 RecyclerView 어댑터 */
    private var recentAdapter: FavoriteAdapter? = null

    /**
     * Fragment의 레이아웃을 inflate하고 루트 뷰를 반환합니다.
     * View Binding을 초기화하는 단계입니다.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * 뷰가 완전히 생성된 후 호출됩니다.
     * 버튼 클릭 리스너 설정, 목록 초기화, ViewModel 관찰을 시작합니다.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ATP_SCREEN", "enter: ${this::class.simpleName}")

        // 급똥 모드 버튼: 누르면 주변 화장실 즉시 검색을 시작합니다
        binding.btnEmergency.setOnClickListener {
            viewModel.activateEmergencyMode()
        }

        // 설정 버튼: 설정 화면으로 이동합니다
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_settings)
        }

        setupFavoriteLists()
        observeViewModel()
    }

    /**
     * 즐겨찾기 목록과 최근 방문 목록의 RecyclerView를 초기화합니다.
     * 각 항목을 클릭하면 해당 화장실로 바로 길 안내를 시작합니다.
     */
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

    /**
     * ViewModel의 LiveData를 관찰하여 UI를 최신 상태로 유지합니다.
     * 검색 화면 이동, 오류 메시지, 음성 활성화, 앱 상태 변경, 목록 업데이트 등을 처리합니다.
     */
    private fun observeViewModel() {
        // 검색 화면으로 이동해야 할 때 Navigation을 실행합니다
        viewModel.navigateToSearch.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                viewModel.onNavigateToSearchConsumed()
                findNavController().navigate(R.id.action_main_to_search)
            }
        }

        // 오류가 발생하면 토스트 메시지로 사용자에게 알립니다
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        // 음성 명령이 감지되면 알림 메시지를 표시합니다
        viewModel.voiceActivated.observe(viewLifecycleOwner) { activated ->
            if (activated) {
                viewModel.onVoiceActivatedConsumed()
                Toast.makeText(requireContext(), "음성 명령 감지! 급똥모드 활성화", Toast.LENGTH_SHORT).show()
            }
        }

        // 앱 상태(검색 중 / 대기 중)에 따라 상태 표시 UI를 업데이트합니다
        viewModel.appState.observe(viewLifecycleOwner) { state ->
            Log.d("ATP_RENDER", "renderState: screen=MainFragment, appState=$state")
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

        // 즐겨찾기 목록이 변경되면 어댑터에 전달하고 빈 상태 뷰를 토글합니다
        viewModel.favorites.observe(viewLifecycleOwner) { list ->
            Log.d("ATP_RENDER", "renderState: screen=MainFragment, favoritesCount=${list.size}, favoritesVisible=${list.isNotEmpty()}")
            favoritesAdapter?.submitList(list)
            binding.rvFavorites?.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvFavEmpty?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        // 최근 방문 목록이 변경되면 어댑터에 전달하고 빈 상태 뷰를 토글합니다
        viewModel.recentVisits.observe(viewLifecycleOwner) { list ->
            Log.d("ATP_RENDER", "renderState: screen=MainFragment, recentCount=${list.size}, recentVisible=${list.isNotEmpty()}")
            recentAdapter?.submitList(list)
            binding.rvRecent?.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvRecentEmpty?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        // 즐겨찾기 항목을 선택하면 해당 화장실로 길 안내 화면으로 이동합니다
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

    /**
     * Fragment의 뷰가 소멸될 때 호출됩니다.
     * 메모리 누수를 방지하기 위해 어댑터와 바인딩 참조를 null로 초기화합니다.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        favoritesAdapter = null
        recentAdapter = null
        _binding = null
    }
}
