package com.recs.sunq.inspection.data.model

import com.google.gson.annotations.SerializedName

data class LoginData(
    val user_id: String,
    val user_pw: String
)

data class AutoLoginData(
    val sun_q_a_t: String
)

data class LoginResponse(
    val message: String,
    val data: User
)

data class User(
    val user_id: String,
    val user_seq: String,
    val plant_info: PlantInfo,
    val token: String
)

data class PlantInfo(
    val plant_seq: String,
    val plant_name: String
)

data class Plant_list(
    val plant_name: String,
    val plant_seq: String
)

data class LogoutResponse(
    val result: Int,
    val message: String?,
    val data: Int
)

data class UserRegistrationInfo(
    @SerializedName("user_seq") val userSeq: String,
    @SerializedName("is_push") val isPushEnabled: String,
    @SerializedName("os") val os: String,
    @SerializedName("app_ver") val appVer: String,
    @SerializedName("device_token") val devicetoken: String,
    @SerializedName("app_name")val appName: String
)

data class UpdateUserInfo(
    @SerializedName("user_seq") val userSeq: String,
    @SerializedName("is_push") val isPushEnabled: String,
    @SerializedName("os") val os: String,
    @SerializedName("app_ver") val appVer: String,
    @SerializedName("device_token") val devicetoken: String,
    @SerializedName("app_name")val appName: String
)

data class UserInfo(
    var user_seq: String,
    var device_token: String,
    var user_app_ver: String,
    var os : String,
    var is_push : String,
    var latest_app_ver : String,
    var is_app_version : Boolean,
    val plant_list: List<Plant_list>?,
    val plant_seq: String
)

data class Alarm(
    val user_app_alarm_seq: String,
    val user_seq: String,
    val app_alarm_seq : String,
    val title: String,
    val url: String,
    val content: String,
    var is_read: String,
    val sent_datetime: String,
    val read_datetime: String,
    val reg_datetime: String
)

data class ReadMessage(
    @SerializedName("user_seq") val userSeq: String,
    @SerializedName("app_alarm_seq") val alarmSeq: String
)
