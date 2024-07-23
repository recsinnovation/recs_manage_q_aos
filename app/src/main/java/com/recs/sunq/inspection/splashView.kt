package com.recs.sunq.inspection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.recs.sunq.androidapp.data.network.RetrofitInstance
import com.recs.sunq.inspection.Notification.AppMessagingService
import com.recs.sunq.inspection.data.TokenManager
import com.recs.sunq.inspection.data.model.AutoLoginData
import com.recs.sunq.inspection.data.model.LoginResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SplashView : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_splash_view)
        Log.d("SplashView", "onCreate called")
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("SplashView", "onNewIntent called")
        intent?.let {
            handleIntent(it)
        }
    }

    private fun handleIntent(intent: Intent?) {
        Log.d("SplashView", "Intent extras: ${intent?.extras}")
        val fromNotification = intent?.getStringExtra("fromNotification")
        Log.d("SplashView", "From Notification in handleIntent: $fromNotification")

        Handler(Looper.getMainLooper()).postDelayed({
            checkAutoLoginAndNavigate(fromNotification, this)
        }, 2000)
    }

    private fun checkAutoLoginAndNavigate(fromNotification: String?, context: Context) {
        // Use encryptedPreferences to check autoLogin status
        val encryptedPreferences = EncryptedSharedPreferences.create(
            "encryptedLoginPrefs",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val autoLogin = encryptedPreferences.getBoolean("autoLogin", false)
        val username = encryptedPreferences.getString("username", "")
        val tokenManager = TokenManager(context)
        val token = tokenManager.getToken()

        if (autoLogin && !username.isNullOrEmpty()) {
            Log.d("SplashView", "Auto login is enabled, validating token")
            validateTokenAndNavigate(fromNotification, context)
        } else {
            Log.d("SplashView", "Auto login is not enabled or username is empty, navigating to login")
            navigateToLogin()
        }
    }

    private fun validateTokenAndNavigate(fromNotification: String?, context: Context) {
        val tokenManager = TokenManager(context)
        val token = tokenManager.getToken()

        if (token.isNullOrBlank()) {
            Log.d("SplashView", "Token is blank, navigating to login")
            navigateToLogin()
        } else {
            val service = RetrofitInstance.createApi(context)
            val autoLoginData = AutoLoginData(token)
            val call = service.autoLoginUser(autoLoginData)

            call.enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val loginResponse = response.body()!!
                        val token = loginResponse.data?.token
                        val userSeq = loginResponse.data?.user_seq
                        val userID = loginResponse.data?.user_id
                        val plantSeq = loginResponse.data?.plant_info?.plant_seq
                        val plantName = loginResponse.data?.plant_info?.plant_name

                        if (token != null && userSeq != null && plantSeq != null && plantName != null && userID != null) {
                                tokenManager.saveToken(token, userSeq, plantSeq, plantName, userID)

                            val appMessagingService = AppMessagingService()
                            CoroutineScope(Dispatchers.Main).launch {
                                appMessagingService.updateFCMToken(context)
                                appMessagingService.selectUserInfo(context)
                                navigateToMain(fromNotification)
                            }
                        } else {
                            Log.d("SplashView", "Token or userSeq or plantSeq is null, navigating to login")
                            navigateToLogin()
                        }
                    } else {
                        Log.d("SplashView", "Response unsuccessful or body is null, navigating to login")
                        navigateToLogin()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e("SplashView", "API call failed: ${t.message}, navigating to login")
                    navigateToLogin()
                }
            })
        }
    }

    private fun navigateToMain(fromNotification: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            if (fromNotification == "true") {
                putExtra("navigateTo", "AlarmFragment")
                Log.d("SplashView", "Navigating to MainActivity with AlarmFragment")
            }
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        Log.d("SplashView", "Navigating to LoginActivity")
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}