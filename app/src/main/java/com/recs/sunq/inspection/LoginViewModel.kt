package com.recs.sunq.androidapp.ui.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recs.sunq.androidapp.data.LoginRepository
import com.recs.sunq.androidapp.data.Result
import com.recs.sunq.inspection.data.TokenManager
import com.recs.sunq.inspection.data.model.LoginData
import com.recs.sunq.inspection.data.model.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel(private val loginRepository: LoginRepository, private val context: Context) : ViewModel() {

    private val tokenManager = TokenManager(context)
    private val _loginResult = MutableLiveData<Result<LoginResponse>>()
    val loginResult: LiveData<Result<LoginResponse>> = _loginResult

    private val _navigateToMain = MutableLiveData<Boolean>()
    val navigateToMain: LiveData<Boolean> = _navigateToMain

    fun login(user_id: String, user_pw: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loginResult.postValue(Result.Loading)
            try {
                val loginData = LoginData(user_id, user_pw)
                val result = loginRepository.login(loginData)
                when (result) {
                    is Result.Success -> {
                        val user = result.data // LoginResponse의 data
                        Log.d("LoginViewModel", "로그인 성공: ${user}")
                        val token = user.data.token
                        val userSeq = user.data.user_seq
                        val userId = user.data.user_id
                        val plantSeq = user.data.plant_info.plant_seq
                        val plantName = user.data.plant_info.plant_name
                        if (token != null && userSeq != null && plantSeq != null && userId != null) {
                            try {
                                tokenManager.saveToken(token, userSeq, plantSeq, plantName, userId)
                                Log.d(
                                    "LoginViewModel",
                                    "토큰 및 사용자 정보 저장 완료: $token, $userSeq, $plantSeq"
                                )
                                Log.d("LoginViewModel", "서버에 등록 정보 전송 및 사용자 정보 선택 완료")

                                _navigateToMain.postValue(true)
                            } catch (e: Exception) {
                                Log.e("LoginViewModel", "토큰 및 사용자 정보 저장 실패", e)
                            }
                        }
                    }

                    is Result.Error -> _loginResult.postValue(result)
                    is Result.Loading -> {}
                }
            } catch (e: Exception) {
                _loginResult.postValue(Result.Error(e))
            }
        }
    }
}