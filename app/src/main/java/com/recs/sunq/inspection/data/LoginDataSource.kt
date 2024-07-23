package com.recs.sunq.androidapp.data

import com.recs.sunq.inspection.data.TokenManager
import com.recs.sunq.inspection.data.model.LoginData
import com.recs.sunq.inspection.data.model.LoginResponse
import com.recs.sunq.inspection.data.network.LoginService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class LoginDataSource(private val loginService: LoginService, private val tokenManager: TokenManager) {

    fun login(loginData: LoginData, callback: (Result<LoginResponse>) -> Unit) {
        loginService.loginUser(loginData).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { loginResponse ->
                        loginResponse.data?.let {
                            if (!it.token.isNullOrEmpty()) {
                                val token = it.token
                                val userSeq = it.user_seq
                                val userId = it.user_id
                                val plantSeq = it.plant_info.plant_seq
                                val plantName = it.plant_info.plant_name

                                // saveToken 호출 시 userSeq와 plantSeq도 함께 저장합니다.
                                tokenManager.saveToken(token, userSeq, plantSeq, plantName, userId)

                                // 성공한 경우 사용자 정의 Result 타입으로 콜백 호출
                                callback(Result.Success(loginResponse))
                            } else {
                                // token이 없는 경우
                                callback(Result.Error(RuntimeException("Token is null or empty")))
                            }
                        } ?: callback(Result.Error(RuntimeException("아이디 또는 비밀번호가 잘못되었습니다.")))
                    } ?: callback(Result.Error(RuntimeException("응답 본문이 비어있습니다")))
                } else {
                    // 서버 응답 실패한 경우 (예: 400 Bad Request, 401 Unauthorized)
                    callback(Result.Error(RuntimeException("관리자에게문의하세요.")))
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                // 네트워크 호출 실패 경우
                callback(Result.Error(Exception(t)))
            }
        })
    }
}

