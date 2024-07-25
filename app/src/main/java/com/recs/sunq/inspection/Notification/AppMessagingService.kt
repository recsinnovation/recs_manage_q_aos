package com.recs.sunq.inspection.Notification

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.recs.sunq.androidapp.data.network.RetrofitInstance
import com.recs.sunq.inspection.BuildConfig
import com.recs.sunq.inspection.MainActivity
import com.recs.sunq.inspection.R
import com.recs.sunq.inspection.SplashView
import com.recs.sunq.inspection.data.TokenManager
import com.recs.sunq.inspection.data.model.UpdateUserInfo
import com.recs.sunq.inspection.data.model.UserInfo
import com.recs.sunq.inspection.data.model.UserRegistrationInfo
import kotlinx.coroutines.tasks.await
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class AppMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Notification Title: ${remoteMessage.notification?.title}, Body: ${remoteMessage.notification?.body}, Data:${remoteMessage.data}")

        if (remoteMessage.data.isNotEmpty()) {
            showNotification(remoteMessage.data)
        }
    }

    private fun showNotification(data: Map<String, String>) {
        val navigateTo = data["navigateTo"]
        val fromNotification = data["fromNotification"]
        val title = data["title"]
        val content = data["content"]
        val badgeCount = data["badge_count"]?.toIntOrNull() ?: 0
        Log.d("FCM", "Badge count from notification: $badgeCount")

        val notificationIntent = if (isAppRunning()) {
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigateTo", navigateTo)
            }
        } else {
            Intent(this, SplashView::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("fromNotification", fromNotification)
            }
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags)
        val channelId = getString(R.string.default_notification_channel_id)
        val notificationBuilder = NotificationCompat.Builder(this, channelId).apply {
            setPriority(NotificationCompat.PRIORITY_HIGH)
            setSmallIcon(R.drawable.black_icon) // Make sure this icon is available
            setContentTitle(title)
            setContentText(content)
            setContentIntent(pIntent)
            setNumber(badgeCount)
            setAutoCancel(true)
        }

        val notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        ) as NotificationManager

        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun isAppRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks = activityManager.getRunningTasks(Int.MAX_VALUE)
        for (task in runningTasks) {
            if (task.baseActivity?.packageName == packageName) {
                return true
            }
        }
        return false
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token generated: $token")
    }

     suspend fun getFCMToken(): String {
        return suspendCoroutine { continuation ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                    continuation.resumeWithException(task.exception ?: Exception("Unknown error"))
                    return@addOnCompleteListener
                }
                val token = task.result
                continuation.resume(token)
            }
        }
    }

     suspend fun sendRegistrationToServer(context: Context) {
        Log.d("AppMessagingService", "sendRegistrationToServer 호출됨")
        val tokenManager = TokenManager(context)

        val deviceToken = getFCMToken()
        val userSeq = tokenManager.getUserseq() ?: ""
        val isPushEnabled = if (NotificationManagerCompat.from(context).areNotificationsEnabled()) "Y" else "N"
        val os = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "AOS" else "IOS"
        val appVer = BuildConfig.VERSION_NAME
        val appName = "inspection"

        val registrationInfo = UserRegistrationInfo(userSeq, isPushEnabled, os, appVer, deviceToken, appName)

        val apiService = RetrofitInstance.createApi(context)

        try {
            val response = apiService.registerDevice(registrationInfo)
            response.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Log.d("FCM", "서버에 등록 성공")
                    } else {
                        Log.d("FCM", "서버에 등록 실패: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.d("FCM", "서버 통신 실패: ${t.message}")
                }
            })
        } catch (t: Throwable) {
            Log.d("FCM", "서버 통신 실패: ${t.message}")
        }
    }

     suspend fun updateUserInfoToServer(context: Context) {
        Log.d("AppMessagingService", "updateUserInfoToServer 호출됨")
        val tokenManager = TokenManager(context)

        val deviceToken = getFCMToken()
        val userSeq = tokenManager.getUserseq() ?: ""
        val isPushEnabled = if (NotificationManagerCompat.from(context).areNotificationsEnabled()) "Y" else "N"
        val os = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "AOS" else "IOS"
        val appVer = BuildConfig.VERSION_NAME
         val appName = "inspection"

        val updateUserInfo = UpdateUserInfo(userSeq, isPushEnabled, os, appVer, deviceToken, appName)

        val apiService = RetrofitInstance.createApi(context)

        try {
            val response = apiService.updateUserInfo(updateUserInfo)
            response.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Log.d("FCM", "서버에 등록 성공")
                    } else {
                        Log.d("FCM", "서버에 등록 실패: ${response.errorBody()?.string()}")
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.d("FCM", "서버 통신 실패: ${t.message}")
                }
            })
        } catch (t: Throwable) {
            Log.d("FCM", "서버 통신 실패: ${t.message}")
        }
    }

    fun selectUserInfo(context: Context) {
        val tokenManager = TokenManager(context)
        val userSeq = tokenManager.getUserseq() ?: ""
        val apiService = RetrofitInstance.createApi(context)
        val appName = "inspection"

        apiService.selectUserInfo(userSeq, appName).enqueue(object : Callback<UserInfo> {
            override fun onResponse(call: Call<UserInfo>, response: Response<UserInfo>) {
                if (response.isSuccessful) {
                    val userInfo = response.body()

                    if (userInfo == null) {
                        return
                    }

                    // plant_list를 TokenManager에 저장
                    val plantList = userInfo.plant_list
                    if (plantList != null) {
                        tokenManager.savePlantList(plantList)
                    }

                    val isAppVersion = userInfo.is_app_version
                    Log.d("FCM", "is_app_version: $isAppVersion")
                    if (context is MainActivity) {
                        Log.d("FCM", "context is MainActivity")
                        if (!context.isFinishing) {
                            context.runOnUiThread {
                                context.showUpdateDialog(isAppVersion)
                            }
                        }
                    } else {
                        Log.d("FCM", "context is not MainActivity")
                    }
                }
            }

            override fun onFailure(call: Call<UserInfo>, t: Throwable) {
                Log.d("FCM", "Server communication failed: ${t.message}")
            }
        })
    }

    suspend fun updateFCMToken(context: Context) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d("FCM", "FCM registration token: $token")
            updateUserInfoToServer(context)
        } catch (e: Exception) {
            Log.w("FCM", "Fetching FCM registration token failed", e)
        }
    }
}