# Poopilot - CLAUDE.md

## Project Overview

Poopilot(급똥모드)은 Pleos Connect 차량 인포테인먼트 시스템용 Android 앱입니다.
긴급 상황에서 주변 화장실을 검색하고, 차량 내비게이션으로 안내하며, 도착 시 차문을 자동으로 열어주는 앱입니다.

## Tech Stack

- **Language**: Kotlin (2.0.21)
- **Build**: Gradle Kotlin DSL, compileSdk 36, minSdk 28, targetSdk 35
- **DI**: Hilt (2.51.1) with kapt
- **Architecture**: MVVM (ViewModel + LiveData + Fragment)
- **Navigation**: Jetpack Navigation Component
- **DB**: Room 2.7.0 (kapt, NOT ksp)
- **Network**: Retrofit 2.9.0 + OkHttp 4.12.0
- **Async**: Kotlin Coroutines + Flow

## Pleos Connect SDK

차량 인포테인먼트 전용 SDK. Maven repo: `https://nexus-playground.pleos.ai/repository/maven-releases/`

| SDK | Version | 용도 |
|-----|---------|------|
| Vehicle | 2.0.3 | 도어 잠금/해제 (`Door.setDoorLock`) |
| NaviHelper | 2.0.3 | 경로 안내, TBT, 위치 조회 |
| TextToSpeech | 2.1.5.1 | 음성 안내 (주의: Maven artifact명은 `TextToSpeech`, 문서에 `TextToSppech` 오타 있음) |
| SpeechToText | 2.1.3.2 | 음성 인식, 키워드 감지 |
| LLM | 2.1.3.2 | AI 추천 |

### SDK API 주의사항 (빌드 에러 방지)

실제 SDK 메서드 시그니처는 문서와 다를 수 있음. AAR 디컴파일로 확인한 실제 API:

- **Door.setDoorLock**: `(area: DoorArea, value: Boolean, onSuccess: (DoorArea, Boolean) -> Unit, onFailed: (Exception) -> Unit)` - 파라미터명 `locked`가 아닌 `value`, onSuccess 콜백 필수
- **TTS EventListener**: `onError(message: String)` (파라미터 있음), `onUpdatedRms(rms: Double)` (Float 아님)
- **STT ResultListener**: `onUpdatedRms` 메서드 없음 (오버라이드하면 컴파일 에러)
- **NaviHelperEventListener.onTBTInfo**: `(info: List<TBTInfo>)` - 리스트로 받음, 단일 아이템 아님
- **TBTInfo 필드**: `type`, `distance`, `description`만 있음. `remainDistance`/`remainTime` 없음 -> `DrivingInfo.destination`에서 조회
- **RequestWaypointInfo**: `poiId`, `address`, `poiSubId` 모두 필수 파라미터
- **RouteInfo 생성자 순서**: `(longitude, latitude, poiName, poiId, address, poiSubId, routeOption)`

SDK API 확인 방법:
```bash
# AAR에서 classes.jar 추출 후 javap로 확인
unzip -q ~/.gradle/caches/modules-2/files-2.1/ai.pleos.playground/{SDK}/{VERSION}/*/{SDK}-{VERSION}.aar -d /tmp/sdk
cd /tmp/sdk && mkdir cls && cd cls && jar xf ../classes.jar
javap -p ai/pleos/playground/{package}/{ClassName}.class
```

navi-data 클래스는 별도 AAR (`navi-data-2.0.0.1.aar`)에 있음.

## Build Commands

```bash
./gradlew assembleDebug    # Debug 빌드
./gradlew assembleRelease  # Release 빌드 (ProGuard 적용)
```

## API Keys

`local.properties`에 설정 (절대 커밋하지 말 것):

```properties
KAKAO_API_KEY=카카오_REST_API_키
PUBLIC_DATA_API_KEY=공공데이터_인증키
NAVER_CLIENT_ID=네이버_클라이언트_ID
NAVER_CLIENT_SECRET=네이버_클라이언트_시크릿
```

키가 비어있으면 해당 API는 자동 skip됩니다 (앱 크래시 없음).

## Project Structure

```
app/src/main/java/com/panicdev/poopilot/
├── data/
│   ├── api/           # Retrofit 인터페이스 (Kakao, Naver, Public)
│   ├── db/            # Room DB (FavoriteRestroom)
│   ├── model/         # API 응답 모델
│   ├── receiver/      # GleoAiReceiver (BroadcastReceiver)
│   ├── repository/    # 비즈니스 로직 (Door, Location, Navigation, STT, TTS, LLM, Search, Settings)
│   └── service/       # VoiceActivationService (백그라운드 음성 감지)
├── di/                # Hilt DI 모듈 (Database, Network, Vehicle, NaviHelper, TTS, STT, LLM)
├── presentation/
│   ├── main/          # 메인 화면 (급똥 버튼, 즐겨찾기, 최근 방문)
│   ├── navigation/    # 길 안내 화면 (TBT, 남은 거리/시간)
│   ├── search/        # 검색 결과 화면 (3개 API 병합)
│   ├── settings/      # 설정 화면 (반경, 도어, TTS, 음성명령)
│   └── util/          # AppUpdateChecker, DrivingModeHelper, NetworkUtils
├── MainActivity.kt    # 앱 진입점, 위치 권한 요청, Gleo 명령 처리
└── PoopilotApp.kt     # Hilt Application
```

## Location Strategy

LocationRepository는 3단계 폴백:
1. **NaviHelper** (차량 내비, 5초 타임아웃)
2. **Android GPS/Network** (10초 타임아웃)
3. **Last Known Location** (캐시)

## Search Strategy

SearchViewModel에서 3개 API 결과를 병합:
1. **카카오 로컬 API** - 좌표 기반 "화장실" 키워드 검색
2. **네이버 로컬 API** - 텍스트 기반 검색
3. **공공데이터 API** - 공중화장실 DB

이름 기준 중복 제거 후 거리순 정렬. LLM으로 최적 1곳 추천.

## Known Issues / Gotchas

- Room은 반드시 **2.7.0+** 사용 (2.6.1은 Kotlin 2.0 메타데이터 미지원)
- NaviHelper SDK에는 **POI 검색 API가 없음** (경로 안내 전용)
- 네이버 로컬 API는 KATEC 좌표 반환 → WGS84 변환 필요 (현재 주소만 활용)
- `NaviHelperEventListener`는 `DefaultImpls` 제공 → 필요한 메서드만 오버라이드 가능
- ProGuard 규칙 필수: Pleos SDK, Retrofit, Room, Hilt, Coroutines

## Comments

모든 44개 Kotlin 소스 파일에 한국어 KDoc 주석이 작성되어 있습니다.
