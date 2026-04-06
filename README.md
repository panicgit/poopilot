# Poopilot (급똥모드)

> 운전 중 급한 상황에서 **음성 한마디로 가장 가까운 화장실까지 최단시간 경로를 안내**하는 Pleos Connect 차량 인포테인먼트 앱

## 핵심 기능

- **원터치/원보이스 활성화** — 버튼 클릭 또는 "급똥모드!" 음성 명령으로 즉시 시작
- **주변 화장실 검색** — 카카오 + 네이버 + 공공데이터 3개 API 동시 검색, AI 최적 추천
- **차량 내비 경로 안내** — Pleos NaviHelper로 최단시간 경로 설정, 실시간 TBT 안내
- **경유지 모드** — 기존 목적지가 있으면 화장실을 경유지로 추가하여 원래 경로 유지
- **도착 시 자동 도어 언락** — 도착 즉시 운전석 문 자동 해제
- **TTS 음성 안내** — "○○까지 N분 소요됩니다", "도착했습니다! 문이 열렸습니다!"
- **Gleo AI 연동** — 차량 AI 어시스턴트 음성 명령 지원

## 스크린샷

UI 디자인: `design.pen` (Pencil MCP로 열기)

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Kotlin 2.0 |
| Architecture | MVVM (ViewModel + LiveData + Fragment) |
| DI | Hilt |
| DB | Room |
| Network | Retrofit + OkHttp |
| Async | Coroutines + Flow |
| Vehicle | Pleos Vehicle SDK (Door) |
| Navigation | Pleos NaviHelper SDK |
| Voice | Pleos STT / TTS SDK |
| AI | Pleos LLM SDK |

## 프로젝트 구조

```
app/src/main/java/com/panicdev/poopilot/
├── data/
│   ├── api/           # Retrofit API 인터페이스 (Kakao, Naver, Public)
│   ├── db/            # Room DB (즐겨찾기/최근 방문)
│   ├── model/         # API 응답 모델
│   ├── receiver/      # Gleo AI BroadcastReceiver
│   ├── repository/    # Repository 계층 (Door, Location, Navigation, Search, STT, TTS, LLM, Settings)
│   └── service/       # 백그라운드 음성 감지 서비스
├── di/                # Hilt DI 모듈
├── presentation/
│   ├── main/          # 메인 화면 (급똥 버튼, 즐겨찾기, 최근 방문)
│   ├── search/        # 검색 결과 화면
│   ├── navigation/    # 길 안내 화면 (TBT, 남은 거리/시간)
│   ├── settings/      # 설정 화면
│   └── util/          # 유틸리티 (DO 모드, 네트워크, 업데이트)
├── MainActivity.kt    # 앱 진입점
└── PoopilotApp.kt     # Hilt Application
```

## 설정

### 1. API 키 설정

`local.properties`에 추가 (절대 커밋하지 말 것):

```properties
KAKAO_API_KEY=카카오_REST_API_키
PUBLIC_DATA_API_KEY=공공데이터_인증키
NAVER_CLIENT_ID=네이버_클라이언트_ID
NAVER_CLIENT_SECRET=네이버_클라이언트_시크릿
```

> 키가 비어있으면 해당 검색 소스는 자동으로 건너뜁니다.

### 2. 빌드

```bash
./gradlew assembleDebug    # Debug APK
./gradlew assembleRelease  # Release APK (ProGuard)
```

### 3. Pleos 에뮬레이터

에뮬레이터 설정은 `../developer_guide/environment-setup/` 참고

## 앱 사용 흐름

```
1. 앱 실행 → 메인 화면 (대기 중)
2. 급똥 버튼 클릭 or "급똥모드!" 음성 명령
3. 현재 위치 기반 주변 화장실 검색 (3개 API + AI 추천)
4. 검색 결과에서 화장실 선택
5. 차량 내비게이션 경로 안내 시작
6. 실시간 TBT + 남은 거리/시간 표시
7. 도착 → 자동 도어 언락 + 음성 안내
```

## 설정 항목

| 설정 | 기본값 | 설명 |
|------|--------|------|
| 검색 반경 | 1km | 500m / 1km / 2km 선택 |
| 도어 자동 언락 | ON | 도착 시 운전석 문 자동 해제 |
| 음성 안내 | ON | TTS 경로 안내 |
| 음성 명령 | ON | STT 키워드 감지 |

## 문서

| 문서 | 설명 |
|------|------|
| `EMERGENCY_RESTROOM_APP_PLAN.md` | 앱 기획서 |
| `STATUS.md` | 스프린트별 개발 진행 현황 |
| `CLAUDE.md` | 개발 컨텍스트 (SDK 주의사항, 아키텍처, 빌드 이슈) |
| `design.pen` | UI 디자인 8화면 |

## 라이선스

Private - PanicDev
