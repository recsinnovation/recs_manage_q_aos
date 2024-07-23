package com.recs.sunq.androidapp.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.recs.sunq.androidapp.data.LoginDataSource
import com.recs.sunq.androidapp.data.LoginRepository
import com.recs.sunq.androidapp.data.network.RetrofitInstance
import com.recs.sunq.inspection.data.TokenManager

class LoginViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            val tokenManager = TokenManager(context) // TokenManager 인스턴스 생성
            return LoginViewModel(
                loginRepository = LoginRepository(
                    dataSource = LoginDataSource(
                        loginService = RetrofitInstance.createApi(context),
                        tokenManager = tokenManager // TokenManager를 전달
                    )
                ),
                context = context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
