# Test Scenario: Emergency Flow (급똥 모드 전체 플로우)

## Overview
급똥 버튼 클릭 → 위치 획득 → 화장실 검색 → 결과 표시 → 장소 선택 → 길 안내 화면 전환까지의 메인 플로우를 검증합니다.

## Prerequisites
- Poopilot 앱이 디바이스에 설치되어 있음
- 네트워크 연결 상태 (카카오/네이버/공공 API 호출 필요)
- 위치 권한 허용 상태

## Test Steps

### Step 1: 앱 실행 및 메인 화면 진입
- **Action**: 앱을 실행한다
- **Expected logcat**:
  - `ATP_SCREEN` → `enter: MainActivity`
  - `ATP_SCREEN` → `enter: MainFragment`
  - `ATP_RENDER` → `renderState: screen=MainFragment, appState=STANDBY`
- **Verify**: 메인 화면이 로드되고 상태가 "대기 중"으로 표시됨

### Step 2: 급똥 모드 버튼 클릭
- **Action**: 급똥 모드 버튼을 탭한다
- **Tap target**: `resource-id: btn_emergency`
- **Expected logcat**:
  - `ATP_RENDER` → `renderState: screen=MainFragment, appState=SEARCHING`
- **Verify**: 상태가 "검색 중..."으로 변경됨

### Step 3: 검색 화면 전환 및 API 호출
- **Action**: 위치 획득 후 검색 화면으로 자동 전환을 기다린다
- **Expected logcat**:
  - `ATP_SCREEN` → `enter: SearchFragment`
  - `ATP_RENDER` → `renderState: screen=SearchFragment, isLoading=true`
  - `ATP_API` → `apiResponse: endpoint=KakaoLocal/searchByKeyword, status=SUCCESS`
  - `ATP_API` → `apiResponse: endpoint=PublicRestroom/searchPublicRestrooms, status=SUCCESS`
  - `ATP_API` → `apiResponse: endpoint=NaverSearch/searchLocal, status=SUCCESS`
- **Verify**:
  - SearchFragment가 표시됨
  - 로딩 인디케이터가 나타남
  - 최소 1개 API가 SUCCESS 응답

### Step 4: 검색 결과 표시
- **Action**: API 응답 후 결과 목록이 나타나는 것을 확인한다
- **Expected logcat**:
  - `ATP_RENDER` → `renderState: screen=SearchFragment, isLoading=false`
  - `ATP_RENDER` → `renderState: screen=SearchFragment, searchResultsCount=`
- **Verify**:
  - 로딩이 종료됨 (isLoading=false)
  - 검색 결과가 1개 이상 표시됨 (searchResultsCount > 0)

### Step 5: 첫 번째 검색 결과 선택
- **Action**: 검색 결과 목록의 첫 번째 항목을 탭한다
- **Tap target**: RecyclerView `rv_results` 의 첫 번째 아이템
- **Expected logcat**:
  - `ATP_SCREEN` → `enter: NavigationFragment`
- **Verify**: 길 안내 화면(NavigationFragment)으로 전환됨

### Step 6: 길 안내 화면 확인
- **Action**: 길 안내 화면의 UI 요소를 확인한다
- **Expected logcat**:
  - `ATP_RENDER` → `renderState: screen=NavigationFragment, tbtVisible=`
- **Verify**:
  - 목적지 이름(tvDestName)이 표시됨
  - 목적지 주소(tvDestAddr)가 표시됨
  - 남은 거리(tvDistValue)가 표시됨
  - 남은 시간(tvTimeValue)이 표시됨

### Step 7: 길 안내 취소
- **Action**: 취소 버튼을 탭하여 메인 화면으로 돌아간다
- **Tap target**: `resource-id: btn_cancel`
- **Expected logcat**:
  - `ATP_SCREEN` → `enter: MainFragment`
  - `ATP_RENDER` → `renderState: screen=MainFragment, appState=STANDBY`
- **Verify**: 메인 화면으로 돌아가고 상태가 다시 "대기 중"이 됨

## Expected Result
급똥 버튼 → 검색 → 결과 표시 → 장소 선택 → 길 안내 → 취소의 전체 플로우가 정상 동작하며,
모든 화면 전환에서 ATP_SCREEN 로그가, 모든 상태 변화에서 ATP_RENDER 로그가,
모든 API 호출에서 ATP_API 로그가 정상적으로 출력된다.

## Troubleshooting
- Step 2 실패: btn_emergency resource-id 확인, 위치 권한 허용 여부 확인
- Step 3 실패: 네트워크 연결 확인, API 키 설정 확인 (local.properties)
- Step 4 실패: 검색 반경 내 화장실 데이터 존재 여부 확인
- Step 5 실패: rv_results RecyclerView가 비어있지 않은지 확인
- Step 6 실패: NaviHelper SDK 초기화 상태 확인
- Step 7 실패: btn_cancel resource-id 확인
