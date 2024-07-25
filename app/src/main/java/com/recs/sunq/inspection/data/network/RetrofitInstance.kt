package com.recs.sunq.androidapp.data.network

import android.content.Context
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.recs.sunq.inspection.data.network.LoginService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    fun createApi(context: Context): LoginService {
        val client = createSecureOkHttpClient(context)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.sunq.co.kr/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()

        return retrofit.create(LoginService::class.java)
    }
}