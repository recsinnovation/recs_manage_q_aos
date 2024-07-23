package com.recs.sunq.androidapp.data

import com.recs.sunq.inspection.data.model.LoginData
import com.recs.sunq.inspection.data.model.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LoginRepository(private val dataSource: LoginDataSource) {

    // 콜백 기반 함수 (기존 코드와의 호환성 유지를 위해)
    fun login(loginData: LoginData, callback: (Result<LoginResponse>) -> Unit) {
        dataSource.login(loginData) { result ->
            callback(result)
        }
    }

    // 코루틴을 사용한 비동기 로그인 함수
    suspend fun login(loginData: LoginData): com.recs.sunq.androidapp.data.Result<LoginResponse> =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                dataSource.login(loginData) { result ->
                    when (result) {
                        is com.recs.sunq.androidapp.data.Result.Success ->
                            continuation.resume(
                                com.recs.sunq.androidapp.data.Result.Success(
                                    result.data
                                )
                            )

                        is com.recs.sunq.androidapp.data.Result.Error ->
                            continuation.resumeWithException(result.exception)

                        else -> continuation.resumeWithException(RuntimeException("Unknown error"))
                    }
                }
            }
        }
}
