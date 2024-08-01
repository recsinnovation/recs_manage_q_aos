package com.recs.sunq.inspection.data.network

import com.recs.sunq.inspection.data.model.Alarm
import com.recs.sunq.inspection.data.model.AutoLoginData
import com.recs.sunq.inspection.data.model.LoginData
import com.recs.sunq.inspection.data.model.LoginResponse
import com.recs.sunq.inspection.data.model.LogoutResponse
import com.recs.sunq.inspection.data.model.ReadMessage
import com.recs.sunq.inspection.data.model.UpdateUserInfo
import com.recs.sunq.inspection.data.model.UserInfo
import com.recs.sunq.inspection.data.model.UserRegistrationInfo
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LoginService {

    @POST("login/auth")
    fun loginUser(@Body loginData: LoginData): Call<LoginResponse>

    @POST("login/authAuto")
    fun autoLoginUser(@Body AutoLoginData: AutoLoginData): Call<LoginResponse>

    @POST("login/logout")
    fun logout(): Call<LogoutResponse>

    @POST("userApp/insertAppUser")
    fun registerDevice(@Body registrationInfo: UserRegistrationInfo): Call<Void>

    @POST("userApp/updateAppUser")
    fun updateUserInfo(@Body UpdateUserInfo: UpdateUserInfo): Call<Void>

    @GET("userApp/selectAppUser")
    fun selectUserInfo(@Query("user_seq") userSeq: String, @Query("app_type") appName: String): Call<UserInfo>

    @GET("userAppAlarm/userAllAlarm")
    fun selectAlertList(@Query("user_seq") userSeq: String): Call<List<Alarm>>

    @POST("userAppAlarm/userRead")
    fun ReadAlert(@Body readMessage: ReadMessage): Call<Void>

}
