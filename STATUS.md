# Poopilot — 개발 진행 현황

> 최종 업데이트: 2026-04-05
> GitHub: https://github.com/panicgit/poopilot
> 총 커밋: 27개

---

## 완료된 Sprint

### Sprint 0: 프로젝트 인프라 ✅
- [x] minSdk 34 → 28 (Pleos 요구사항)
- [x] Pleos Maven 저장소 추가
- [x] Pleos SDK 5개 의존성 (Vehicle, NaviHelper, STT, TTS, LLM)
- [x] Hilt DI 설정
- [x] AndroidManifest 권한 13개
- [x] Retrofit + OkHttp + Coroutines + Lifecycle 의존성
- [x] 패키지 구조 (data/domain/presentation/di)

### Sprint 1: 메인 화면 + 위치 획득 ✅
- [x] 메인 화면 UI (급똥모드 버튼)
- [x] Vehicle SDK 초기화 모듈 (VehicleModule)
- [x] NaviHelper SDK 초기화 모듈 (NaviHelperModule + LocationRepository)
- [x] 현재 위치 획득 (getCurrentLocation with timeout)
- [x] 급똥모드 버튼 이벤트 (MainFragment → MainViewModel)
- [x] 화면 전환 (Navigation Component, 4개 Fragment)

### Sprint 2: 화장실 검색 + 경로 안내 ✅
- [x] 검색 화면 UI (RecyclerView + RestroomAdapter)
- [x] SearchViewModel (검색, 로딩, 에러, 장소 선택)
- [x] NaviHelper 경로 설정 (requestRoute + addWaypoint 분기)
- [x] NavigationRepository (경로 관리 + 이벤트 SharedFlow)
- [x] NavigationViewModel (카운트다운, 도착 감지)
- [x] NavigationFragment 연동 (Bundle 전달, 실시간 업데이트)

### 디자인 매칭 ✅
- [x] 색상 토큰 20개 (colors.xml)
- [x] 치수 토큰 (dimens.xml — padding, radius, font, touch target)
- [x] 다크 테마 (themes.xml — always dark automotive)
- [x] Portrait(1/3) 레이아웃 — @color/@dimen 참조 통일
- [x] Landscape(2/3) 레이아웃 4개 — 좌우 분할 패널
- [x] Drawable 7개 — @color/@dimen 참조

### 코드 리뷰 (2회 완료) ✅
- [x] 1차: CRITICAL 2 + HIGH 4 수정 (타임아웃, 에러 핸들링, lifecycle, 로깅)
- [x] 2차: HIGH 3 + MEDIUM 3 수정 (터치 타겟, 좌표 검증, dimen 토큰)

---

### Sprint 3: 도어 언락 + TTS 안내 ✅
- [x] Door 제어 모듈 (DoorRepository — unlockDriverDoor/unlockAllDoors)
- [x] 도착 시 자동 언락 (DestinationArrived → DoorRepository.unlockDriverDoor)
- [x] TTS 초기화 모듈 (TtsRepository + TtsModule — HYBRID mode)
- [x] TTS 음성 안내 (경로 시작, 소요시간 안내, 도착+도어 언락 안내)
- [x] TBT 기반 실시간 카운트다운 (3초 폴링, TBT description 표시)
- [x] 설정 화면 기능 (SettingsViewModel + SharedPreferences — 반경/도어/음성/TTS)

---

### Sprint 4: STT 음성 활성화 ✅
- [x] STT 초기화 모듈 (SttRepository + SttModule — HYBRID mode, ResultListener)
- [x] "급똥모드!" 키워드 인식 (VoiceActivationService — 연속 감지 + 자동 재시작)
- [x] 음성 → 전체 플로우 자동 실행 (MainViewModel — 키워드 → activateEmergencyMode)
- [x] Gleo AI Schema (assets/schema.json — ACTIVATE/CANCEL 함수 정의)
- [x] Gleo AI Callback (GleoAiReceiver + GleoCommandBus — Intent 수신 → 급똥모드)

---

## 다음 Sprint

### Sprint 5: 스마트 기능 (3일)
- [ ] LLM 검색 결과 필터링
- [ ] 단골 화장실 DB (Room)
- [ ] 자동 재탐색
- [ ] 공공데이터포털 API

### Sprint 6: 고도화 + 품질 (3일)
- [ ] DO 선언, 오디오 포커스, 반응형 완성
- [ ] 에러 핸들링, 앱 아이콘, 보안, 업데이트 시퀀스
- [ ] 남은 코드 리뷰 이슈 (MEDIUM/LOW)

---

## 프로젝트 구조

```
com.panicdev.poopilot/
├── PoopilotApp.kt              @HiltAndroidApp
├── MainActivity.kt             @AndroidEntryPoint
├── di/
│   ├── VehicleModule.kt        Vehicle SDK singleton
│   ├── NaviHelperModule.kt     NaviHelper SDK singleton
│   ├── NetworkModule.kt        Retrofit + KakaoLocalApi
│   ├── TtsModule.kt            TextToSpeech SDK singleton (HYBRID)
│   └── SttModule.kt            SpeechToText SDK singleton (HYBRID)
├── data/
│   ├── api/KakaoLocalApi.kt    카카오 로컬 검색 API
│   ├── model/KakaoSearchResponse.kt  응답 모델
│   └── repository/
│       ├── LocationRepository.kt     위치 획득 (coroutine + timeout)
│       ├── RestroomRepository.kt     화장실 검색 (Result 반환)
│       ├── NavigationRepository.kt   경로 관리 (SharedFlow 이벤트)
│       ├── DoorRepository.kt         도어 언락 (Vehicle Door API)
│       ├── TtsRepository.kt          음성 합성 (TextToSpeech SDK)
│       ├── SttRepository.kt          음성 인식 (SpeechToText SDK)
│       └── SettingsRepository.kt     설정 저장/로드 (SharedPreferences)
├── service/
│   └── VoiceActivationService.kt  연속 키워드 감지 서비스
├── receiver/
│   └── GleoAiReceiver.kt         Gleo AI Intent 수신
├── presentation/
│   ├── main/
│   │   ├── MainFragment.kt          급똥모드 버튼 + 상태
│   │   └── MainViewModel.kt         AppState, 위치 획득
│   ├── search/
│   │   ├── SearchFragment.kt        검색 결과 표시
│   │   ├── SearchViewModel.kt       검색 로직
│   │   └── RestroomAdapter.kt       RecyclerView 어댑터
│   ├── navigation/
│   │   ├── NavigationFragment.kt    안내 화면
│   │   └── NavigationViewModel.kt   카운트다운, 도착 감지
│   └── settings/
│       ├── SettingsFragment.kt      설정 UI (반경/도어/음성/TTS)
│       └── SettingsViewModel.kt     SharedPreferences 저장/로드
└── domain/usecase/                  (Sprint 5+)
```

## 리소스 구조

```
res/
├── layout/                    Portrait (1/3) 레이아웃
│   ├── activity_main.xml
│   ├── fragment_main.xml
│   ├── fragment_search.xml
│   ├── fragment_navigation.xml
│   ├── fragment_settings.xml
│   └── item_restroom_result.xml
├── layout-land/               Landscape (2/3) 레이아웃
│   ├── fragment_main.xml      좌: 버튼 / 우: 단골+최근
│   ├── fragment_search.xml    좌: 스피너 / 우: 결과 리스트
│   ├── fragment_navigation.xml 좌: 카운트다운 / 우: 컨트롤
│   └── fragment_settings.xml  좌: 반경+순위 / 우: 토글
├── navigation/nav_graph.xml
├── drawable/ (7개)
└── values/
    ├── colors.xml (20개 토큰)
    ├── dimens.xml (치수 토큰)
    ├── themes.xml (다크 테마)
    └── strings.xml
```

## 외부 준비물

| 항목 | 필요 시점 | 상태 |
|------|----------|------|
| 카카오 로컬 API 키 | Sprint 2 (실제 테스트) | ❌ 미발급 |
| 공공데이터포털 API 키 | Sprint 5 | ❌ 미발급 |
| Pleos Playground CRN | 에뮬레이터 테스트 | ❌ 미확인 |

## 알려진 이슈

| 이슈 | 심각도 | 상태 |
|------|--------|------|
| Pleos SDK 빌드 실패 (TextToSppech 미해결) | 환경 | Pleos Emulator 환경에서만 빌드 가능 |
| ProGuard 미활성화 | 릴리즈 전 | Sprint 6 |
| Switch → SwitchMaterial 교체 | LOW | Sprint 6 |
| Safe Args 미적용 (Bundle 수동) | MEDIUM | Sprint 6 |
| Landscape 사이드 패널 폭 불일치 (360/400/420dp) | LOW | Sprint 6 |
