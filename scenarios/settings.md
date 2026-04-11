# Test Scenario: Settings (설정 화면 테스트)

## Overview
설정 화면 진입 후 검색 반경 변경, 도어 잠금 해제/음성 명령/TTS 토글을 검증합니다.

## Prerequisites
- Poopilot 앱이 디바이스에 설치되어 있음
- 메인 화면에서 시작

## Test Steps

### Step 1: 설정 화면 진입
- **Action**: 설정 버튼을 탭한다
- **Tap target**: `resource-id: btn_settings`
- **Expected logcat**:
  - `ATP_SCREEN` → `enter: SettingsFragment`
  - `ATP_RENDER` → `renderState: screen=SettingsFragment, searchRadius=`
- **Verify**: 설정 화면이 표시됨

### Step 2: 검색 반경 변경 (500m)
- **Action**: 500m 반경 버튼을 탭한다
- **Tap target**: `resource-id: btn_radius_500`
- **Expected logcat**:
  - `ATP_RENDER` → `renderState: screen=SettingsFragment, searchRadius=500`
- **Verify**: 500m 버튼이 선택 상태(강조)로 변경됨

### Step 3: 검색 반경 변경 (2km)
- **Action**: 2km 반경 버튼을 탭한다
- **Tap target**: `resource-id: btn_radius_2km`
- **Expected logcat**:
  - `ATP_RENDER` → `renderState: screen=SettingsFragment, searchRadius=2000`
- **Verify**: 2km 버튼이 선택 상태(강조)로 변경됨

### Step 4: 도어 잠금 해제 토글
- **Action**: 도어 잠금 해제 스위치를 탭한다
- **Tap target**: `resource-id: switch_door_unlock`
- **Verify**: 스위치 상태가 토글됨

### Step 5: TTS 토글
- **Action**: TTS 스위치를 탭한다
- **Tap target**: `resource-id: switch_tts`
- **Verify**: 스위치 상태가 토글됨

### Step 6: 뒤로가기
- **Action**: 뒤로가기 버튼을 탭한다
- **Tap target**: `resource-id: btn_back`
- **Expected logcat**:
  - `ATP_SCREEN` → `enter: MainFragment`
- **Verify**: 메인 화면으로 복귀함

## Expected Result
설정 화면의 모든 컨트롤(반경 버튼 3개, 스위치 3개)이 정상 동작하며,
변경된 설정이 저장되고 메인 화면으로 정상 복귀한다.

## Troubleshooting
- Step 1 실패: btn_settings resource-id 확인
- Step 2-3 실패: 반경 버튼 resource-id 확인
- Step 4-5 실패: 스위치 resource-id 확인
- Step 6 실패: btn_back resource-id 확인
