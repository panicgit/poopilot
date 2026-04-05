package com.panicdev.poopilot.data.repository

import ai.pleos.playground.llm.LLM
import ai.pleos.playground.llm.listener.ResultListener
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 차량 내장 LLM(대형 언어 모델)을 활용한 AI 응답 생성을 담당하는 Repository입니다.
 *
 * 사용자의 질문이나 상황 설명(프롬프트)을 받아 AI가 생성한 텍스트 응답을 반환합니다.
 * LLM은 앱 시작 시 초기화되고, 필요 없을 때 메모리에서 해제할 수 있습니다.
 */
@Singleton
class LlmRepository @Inject constructor(
    /** 차량 내장 LLM SDK 인스턴스 */
    private val llm: LLM
) {
    /**
     * LLM 초기화 완료 여부를 나타내는 플래그.
     * 여러 스레드에서 동시에 접근할 수 있어 @Volatile로 선언되었습니다.
     */
    @Volatile
    private var isInitialized = false

    /**
     * LLM을 초기화합니다. 이미 초기화된 경우 아무 동작도 하지 않습니다.
     * 동시에 여러 곳에서 호출되더라도 안전하게 한 번만 초기화됩니다 (@Synchronized).
     */
    @Synchronized
    fun initialize() {
        if (isInitialized) return
        try {
            llm.initialize()
            isInitialized = true
            Log.d(TAG, "LLM initialized")
        } catch (e: Exception) {
            Log.e(TAG, "LLM initialization failed", e)
        }
    }

    /**
     * 주어진 프롬프트(질문/지시문)를 LLM에 전달하여 AI 응답을 생성합니다.
     *
     * - 아직 초기화되지 않았다면 자동으로 초기화를 시도합니다.
     * - 응답은 스트리밍 방식으로 수신되며, 완료 시 전체 텍스트를 한꺼번에 반환합니다.
     * - 30초 이내에 응답이 완료되지 않으면 타임아웃 처리됩니다.
     *
     * @param prompt LLM에 전달할 질문 또는 지시 내용
     * @return 성공 시 AI가 생성한 텍스트, 실패 시 예외를 담은 [Result]
     */
    suspend fun generateContent(prompt: String): Result<String> {
        return withContext(Dispatchers.IO) {
            // LLM이 초기화되지 않은 경우 초기화 시도
            if (!isInitialized) {
                initialize()
                if (!isInitialized) {
                    return@withContext Result.failure(IllegalStateException("LLM initialization failed"))
                }
            }
            try {
                // 최대 30초 안에 응답이 와야 함, 초과 시 타임아웃 예외 발생
                withTimeout(30_000L) {
                    suspendCancellableCoroutine { continuation ->
                        // 스트리밍으로 오는 응답 조각들을 하나씩 이어 붙임
                        val responseBuilder = StringBuffer()
                        llm.generateContent(prompt, object : ResultListener {
                            override fun onResponse(response: String, completed: Boolean) {
                                responseBuilder.append(response)
                                // completed == true 이면 마지막 조각이므로 전체 응답 반환
                                if (completed && continuation.isActive) {
                                    continuation.resume(Result.success(responseBuilder.toString()))
                                }
                            }

                            override fun onError(reason: String) {
                                Log.e(TAG, "LLM error: $reason")
                                if (continuation.isActive) {
                                    continuation.resume(Result.failure(Exception(reason)))
                                }
                            }
                        })
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "LLM generateContent failed", e)
                Result.failure(e)
            }
        }
    }

    /**
     * LLM을 메모리에서 해제합니다.
     * 더 이상 AI 기능이 필요 없을 때(예: 앱 종료 시) 호출하여 리소스를 반환합니다.
     */
    fun release() {
        try {
            llm.release()
            isInitialized = false
            Log.d(TAG, "LLM released")
        } catch (e: Exception) {
            Log.e(TAG, "LLM release failed", e)
        }
    }

    companion object {
        private const val TAG = "LlmRepository"
    }
}
