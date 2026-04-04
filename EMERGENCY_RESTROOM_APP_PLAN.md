# Poopilot - 급똥모드 앱 기획서

> 프로젝트명: poopilot
> 작성일: 2026-04-03
> 기준: Pleos Connect SDK + 외부 API 조합

---

## 1. 개요

### 컨셉
운전 중 급한 상황에서 **음성 한마디로 가장 가까운 화장실까지 최단시간 경로를 안내**하는 앱.
기존 목적지가 있으면 경유지로 추가하여 원래 경로를 유지한다.

### 핵심 가치
- **원터치(원보이스)**: "급똥모드!" 한마디로 모든 것이 자동 실행
- **최단시간 도착**: 가장 가까운 화장실 + 가장 빠른 경로
- **경로 유지**: 기존 네비 목적지를 방해하지 않음 (경유지 모드)
- **도착 즉시 행동**: 도어 자동 언락으로 바로 뛰어나갈 수 있음

### 타겟 사용자
- 장거리 운전자 (고속도로, 국도)
- 배달/물류 드라이버
- 낯선 지역 운전자
- 모든 급한 사람

---

## 2. 사용자 시나리오

### 시나리오 A: 목적지 없이 주행 중
```
사용자: "급똥모드!"
   ↓
앱: 현재 위치 파악 → 가장 가까운 화장실 검색
   ↓
앱: "500m 앞 GS25 편의점 화장실로 안내합니다. 약 1분 소요."
   ↓
네비: SHORT_TIME 최단시간 경로 안내 시작
   ↓
앱: "300m 남았습니다" → "좌회전 후 도착" → "도착! 문 열렸습니다!"
   ↓
차량: 도어 언락
```

### 시나리오 B: 이미 목적지로 가는 중
```
사용자: "급똥모드!"
   ↓
앱: 기존 목적지 확인 → 경유지 모드로 전환
   ↓
앱: "경로에서 가장 가까운 화장실을 경유지로 추가합니다."
   ↓
네비: 화장실 경유 → 원래 목적지 계속 안내
   ↓
(화장실 도착 후 출발하면 원래 목적지로 안내 재개)
```

### 시나리오 C: 단골 화장실 근처
```
사용자: "급똥모드!"
   ↓
앱: 최근/즐겨찾기 화장실이 근처에 있음 감지
   ↓
앱: "이전에 가셨던 스타벅스 화장실이 300m 앞에 있습니다. 그곳으로 갈까요?"
   ↓
사용자: "응" (STT)
   ↓
네비: 경로 안내 시작
```

---

## 3. 핵심 기능 정의

### 3.1 음성 활성화
| 항목 | 내용 |
|------|------|
| 트리거 | "급똥모드", "화장실", "급해" 등 키워드 |
| API | Gleo AI STT (Streaming 모드) |
| 이유 | 운전 중 손을 쓸 수 없으므로 음성이 필수 |

### 3.2 화장실 검색
| 항목 | 내용 |
|------|------|
| 위치 기반 | NaviHelper `getCurrentLocationInfo()` |
| 검색 소스 | 카카오 로컬 API (키워드: "화장실", "편의점") |
| 보조 소스 | 공공데이터포털 공중화장실 API |
| 필터링 | Gleo AI LLM — 24시간 여부, 주차 가능 여부, 유형 우선순위 결정 |

### 3.3 경로 안내
| 항목 | 내용 |
|------|------|
| 목적지 없을 때 | `requestRoute(RouteInfo, RouteOption.SHORT_TIME)` |
| 목적지 있을 때 | `addWaypoint(RequestWaypointInfo, WaypointIndex.FIRST)` |
| 경로 옵션 | `SHORT_TIME` (최단시간) |
| 막힐 때 | `requestReRoute()` 자동 재탐색 |
| 더 가까운 곳 발견 시 | `cancelRoute()` → 새 `requestRoute()` |

### 3.4 실시간 안내
| 항목 | 내용 |
|------|------|
| TBT 정보 | `getTBTInfo()` — 남은 거리/시간 실시간 추적 |
| 음성 안내 | Gleo AI TTS — 남은 시간, 도착 알림 |
| 안내 주기 | 500m, 300m, 100m, 도착 시 |

### 3.5 도착 액션
| 항목 | 내용 |
|------|------|
| 도어 언락 | Vehicle SDK Door `controlDoorLock(UNLOCK)` |
| 도착 감지 | NaviHelper `onDestinationArrived` 콜백 |
| 음성 알림 | TTS "도착했습니다! 문 열렸습니다!" |

### 3.6 단골 화장실 관리
| 항목 | 내용 |
|------|------|
| 최근 방문 | `getRecentDestinationInfo()` 에서 화장실 태그된 목록 |
| 즐겨찾기 | `getBookmarkInfo()` 에서 화장실 카테고리 |
| 앱 자체 DB | 방문 이력, 평점, 청결도 사용자 메모 저장 |

---

## 4. API 아키텍처

### 4.1 사용 API 목록

#### Pleos Connect SDK
| SDK | 메서드 | 용도 |
|-----|--------|------|
| **Gleo AI** | `startSTT()` | 음성 명령 인식 |
| **Gleo AI** | `startTTS()` | 음성 안내 출력 |
| **Gleo AI** | `requestLLM()` | 검색 결과 필터링/추천 |
| **NaviHelper** | `getCurrentLocationInfo()` | 현재 위치 획득 |
| **NaviHelper** | `getRouteStateInfo()` | 기존 경로 존재 여부 확인 |
| **NaviHelper** | `getDestinationInfo()` | 기존 목적지 정보 확인 |
| **NaviHelper** | `requestRoute()` | 화장실로 경로 설정 |
| **NaviHelper** | `addWaypoint()` | 경유지로 화장실 추가 |
| **NaviHelper** | `cancelRoute()` | 경로 취소 (더 가까운 곳 전환 시) |
| **NaviHelper** | `requestReRoute()` | 교통 상황 변경 시 재탐색 |
| **NaviHelper** | `changeRouteOption()` | 경로 옵션 SHORT_TIME 설정 |
| **NaviHelper** | `getTBTInfo()` | 남은 거리/시간 실시간 |
| **NaviHelper** | `getBookmarkInfo()` | 즐겨찾기 화장실 |
| **NaviHelper** | `getRecentDestinationInfo()` | 최근 방문 화장실 |
| **Vehicle SDK** | `Door.controlDoorLock()` | 도착 시 도어 언락 |

#### 외부 API
| API | 용도 |
|-----|------|
| **카카오 로컬 API** | 키워드 기반 주변 화장실/편의점 검색 |
| **공공데이터포털** | 전국 공중화장실 정보 (위치, 운영시간) |

### 4.2 권한 (Pleos Permission)

| 권한 | 필요 이유 |
|------|----------|
| `DOOR` | 도어 상태 조회 |
| `CONTROL_DOOR` | 도어 언락 제어 |
| `NAVIGATION` | NaviHelper 사용 |
| `MICROPHONE` | STT 음성 인식 |
| `SPEAKER` | TTS 음성 출력 |

---

## 5. 핵심 로직 플로우

```
[음성 입력: "급똥모드!"]
        │
        ▼
[STT 인식 → 키워드 매칭]
        │
        ▼
[getCurrentLocationInfo() → 현재 위도/경도]
        │
        ├──────────────────────────────┐
        ▼                              ▼
[getRouteStateInfo()]          [외부 API: 주변 화장실 검색]
  기존 경로 있는지 확인              │
        │                              ▼
        │                     [getBookmarkInfo() / 
        │                      getRecentDestinationInfo()
        │                      → 단골 화장실 근처에 있는지 확인]
        │                              │
        │                              ▼
        │                     [LLM 필터링: 최적 화장실 1개 선택]
        │                              │
        ├──────────────────────────────┘
        │
        ▼
   ┌─────────┐
   │경로 있음?│
   └────┬────┘
    YES │    NO
        ▼        ▼
[addWaypoint()]  [requestRoute(SHORT_TIME)]
        │              │
        └──────┬───────┘
               ▼
    [TTS: "○○까지 N분, 안내 시작합니다"]
               │
               ▼
    [주행 중: getTBTInfo() 실시간 추적]
        │              │
        │     [막힘 감지 → requestReRoute()]
        │              │
        ▼              ▼
    [onDestinationArrived 콜백]
               │
               ▼
    [Door.controlDoorLock(UNLOCK)]
               │
               ▼
    [TTS: "도착! 문 열렸습니다!"]
               │
               ▼
    [경유지 모드였다면 → 원래 목적지로 안내 재개]
```

---

## 6. 화면 구성 (최소 UI)

급한 상황이므로 UI는 최소화. 대부분 음성으로 처리.

### 메인 화면
- **급똥모드 버튼** (크고 빨간 버튼 1개) — 음성 외 수동 발동용
- 현재 상태 표시: "대기 중" / "검색 중..." / "안내 중 (2분 남음)"

### 안내 중 화면
- 목적지 이름 + 남은 시간
- "더 가까운 곳으로 변경" 버튼
- "취소" 버튼

### 설정 화면
- 검색 반경 설정 (500m / 1km / 2km)
- 우선순위 설정 (공용화장실 > 편의점 > 주유소 등)
- 도어 자동 언락 ON/OFF
- 단골 화장실 관리

---

## 7. 구현 단계

### Phase 1: MVP (2주)
핵심 동작만 구현

- [ ] 급똥모드 버튼 (수동 발동)
- [ ] `getCurrentLocationInfo()` → 현재 위치 획득
- [ ] 카카오 로컬 API 연동 → 주변 화장실 검색
- [ ] `requestRoute(SHORT_TIME)` → 최단시간 경로 안내
- [ ] `onDestinationArrived` → 도착 알림

### Phase 2: 음성 + 스마트 기능 (+2주)
편의성 강화

- [ ] Gleo AI STT → 음성 활성화 "급똥모드!"
- [ ] Gleo AI TTS → 음성 안내 (남은 시간, 도착 알림)
- [ ] `getRouteStateInfo()` → 기존 경로 확인 후 경유지 모드 분기
- [ ] `addWaypoint()` → 경유지 모드 구현
- [ ] `Door.controlDoorLock(UNLOCK)` → 도착 시 도어 언락

### Phase 3: 고도화 (+2주)
사용자 경험 완성

- [ ] Gleo AI LLM → 검색 결과 스마트 필터링
- [ ] 단골 화장실 기능 (즐겨찾기/최근 방문 활용)
- [ ] `requestReRoute()` → 교통 상황 기반 자동 재탐색
- [ ] 공공데이터포털 API 추가 연동 (공중화장실 운영시간 등)
- [ ] 사용자 메모/평점 기능 (앱 자체 DB)

### Phase 4: 고급 기능 (+2주)
차별화 요소

- [ ] 화장실 크라우드소싱 (사용자 제보 기능)
- [ ] 고속도로 모드 (다음 휴게소/졸음쉼터 우선 안내)
- [ ] 위젯/바로가기 지원
- [ ] 사용 통계 (급똥모드 발동 횟수, 평균 도착 시간 등)

---

## 8. 기술적 제약 및 대응

| 제약 | 영향 | 대응 방안 |
|------|------|----------|
| NaviHelper에 POI 검색 API 없음 | 화장실 검색 불가 | 카카오 로컬 API + 공공데이터 API 사용 |
| DrivingMode 제어 불가 | 스포츠 모드 전환 불가 | 경로 옵션(SHORT_TIME)으로 대체 |
| 비상등 API 없음 | 비상등 자동 점멸 불가 | 해당 기능 제외 |
| TurnSignal 제어 불가 | 방향지시등 자동 불가 | 해당 기능 제외 |
| EvBattery 구독 미지원 | 전기차 배터리 실시간 불가 | 급똥모드에는 영향 없음 |

---

## 9. 차별화 포인트

1. **음성 원터치** — 다른 화장실 찾기 앱은 손으로 조작해야 함. 운전 중 음성만으로 동작.
2. **경유지 모드** — 기존 목적지를 방해하지 않고 화장실 경유 후 원래 경로 복귀.
3. **차량 연동** — 도착 시 도어 자동 언락. 일반 스마트폰 앱에서는 불가능한 기능.
4. **단골 관리** — 검증된 화장실을 기억하고 우선 추천.
5. **LLM 추천** — 24시간 운영, 주차 가능 등 조건을 AI가 판단.

---

## 10. 필요 외부 리소스

| 리소스 | 용도 | 비용 |
|--------|------|------|
| 카카오 로컬 API 키 | 주변 장소 검색 | 무료 (일 30만 건) |
| 공공데이터포털 API 키 | 공중화장실 데이터 | 무료 |
| 앱 자체 서버 (선택) | 사용자 데이터, 크라우드소싱 | Phase 4부터 필요 |

---

## 11. 개발 환경 및 기술 스펙

### 플랫폼
- **Android** (Pleos Connect 인포테인먼트 전용)
- **언어**: Kotlin (권장) / Java
- **IDE**: Android Studio + Pleos Connect Emulator (AVD)

### SDK 의존성 (build.gradle)

```kotlin
// settings.gradle.kts — 공통 Maven 저장소
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
```

```kotlin
// app/build.gradle.kts
dependencies {
    // Pleos Connect SDK
    implementation("ai.pleos.playground:Vehicle:2.0.3")          // 도어 언락
    implementation("ai.pleos.playground:NaviHelper:2.0.3")       // 네비게이션 연동
    implementation("ai.pleos.playground:SpeechToText:2.1.3.2")   // 음성 인식
    implementation("ai.pleos.playground:TextToSppech:2.1.5.1")   // 음성 안내 (원문 오타 그대로)
    implementation("ai.pleos.playground:LLM:2.1.3.2")            // AI 필터링

    // 외부 API 통신
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}
```

### AndroidManifest.xml 권한

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Vehicle SDK: 도어 제어 -->
    <uses-permission android:name="pleos.car.permission.CAR_INFO" />
    <uses-permission android:name="pleos.car.permission.CONTROL_DOOR" />
    <uses-permission android:name="pleos.car.permission.DOOR" />

    <!-- NaviHelper SDK: 네비게이션 -->
    <uses-permission android:name="pleos.car.permission.NAVI_ROUTE" />
    <uses-permission android:name="pleos.car.permission.NAVI_ROUTE_SEARCH" />
    <uses-permission android:name="pleos.car.permission.NAVI_CUSTOM_ROUTE" />
    <uses-permission android:name="pleos.car.permission.NAVI_CUSTOM_ETC" />

    <!-- Gleo AI SDK: STT / TTS / LLM -->
    <uses-permission android:name="pleos.car.permission.STT_SERVICE" />
    <uses-permission android:name="pleos.car.permission.TTS_SERVICE" />
    <uses-permission android:name="pleos.car.permission.LLM_SERVICE" />

    <!-- 네트워크 (외부 API 호출) -->
    <uses-permission android:name="android.permission.INTERNET" />

</manifest>
```

### SDK 초기화 패턴

```kotlin
// Vehicle SDK
val vehicle = Vehicle(context)
vehicle.initialize()
val door = vehicle.getDoor()

// NaviHelper SDK
val naviHelper = NaviHelper(context)
naviHelper.initialize()
naviHelper.addListener(naviEventListener)

// Gleo AI STT
val stt = SpeechToText(context)
stt.initialize()
stt.registerApp()

// Gleo AI TTS
val tts = TextToSpeech(context)
tts.initialize()
tts.registerApp()

// Gleo AI LLM
val llm = LLM(context)
llm.initialize()
llm.registerApp()
```

### SDK 해제 (생명주기 관리)

```kotlin
// onDestroy 또는 앱 종료 시 반드시 해제
naviHelper.removeListener(naviEventListener)
naviHelper.release()
vehicle.release()
stt.release()
tts.release()
llm.release()
```

### 주요 참고사항

| 항목 | 내용 |
|------|------|
| 에뮬레이터 | Pleos Connect Emulator (Android Studio AVD) 필수 |
| 차량 호환성 | `checkXXXCapability()` API로 차량별 기능 지원 여부 사전 확인 필요 |
| TTS 아티팩트명 | `TextToSppech` (오타지만 공식 아티팩트명이므로 그대로 사용) |
| Maven 저장소 | `https://nexus-playground.pleos.ai/repository/maven-releases/` |
| 최소 SDK | Pleos Connect Emulator AVD 기준 (문서에 minSdk 미명시) |
