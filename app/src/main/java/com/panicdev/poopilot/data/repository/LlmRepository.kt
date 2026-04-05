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

@Singleton
class LlmRepository @Inject constructor(
    private val llm: LLM
) {
    @Volatile
    private var isInitialized = false

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

    suspend fun generateContent(prompt: String): Result<String> {
        return withContext(Dispatchers.IO) {
            if (!isInitialized) {
                initialize()
                if (!isInitialized) {
                    return@withContext Result.failure(IllegalStateException("LLM initialization failed"))
                }
            }
            try {
                withTimeout(30_000L) {
                    suspendCancellableCoroutine { continuation ->
                        val responseBuilder = StringBuffer()
                        llm.generateContent(prompt, object : ResultListener {
                            override fun onResponse(response: String, completed: Boolean) {
                                responseBuilder.append(response)
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
