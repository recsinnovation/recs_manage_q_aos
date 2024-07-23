package com.recs.sunq.inspection.optionsMenu

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.recs.sunq.androidapp.data.network.RetrofitInstance
import com.recs.sunq.inspection.LoginActivity
import com.recs.sunq.inspection.R
import com.recs.sunq.inspection.data.TokenManager
import com.recs.sunq.inspection.data.model.LogoutResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Apply the custom theme
        context?.theme?.applyStyle(R.style.PreferenceScreenTheme, true)
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val pushNotificationPreference: SwitchPreferenceCompat? =
            findPreference("pref_key_push_notifications")
        checkNotificationPermission(pushNotificationPreference)

        pushNotificationPreference?.setOnPreferenceChangeListener { _, _ ->
            val intent = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                    Intent("android.settings.APP_NOTIFICATION_SETTINGS").apply {
                        putExtra("app_package", requireContext().packageName)
                        putExtra("app_uid", requireContext().applicationInfo.uid)
                    }
                }
                else -> {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        data = Uri.parse("package:" + requireContext().packageName)
                    }
                }
            }
            startActivity(intent)
            Toast.makeText(activity, "알림 설정은 시스템 설정에서 변경해주세요.", Toast.LENGTH_LONG).show()
            false // 변경을 취소합니다.
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.title_bar))
    }

    override fun onResume() {
        super.onResume()
        val pushNotificationPreference: SwitchPreferenceCompat? =
            findPreference("pref_key_push_notifications")
        checkNotificationPermission(pushNotificationPreference)

        // 로그아웃 설정 클릭 이벤트 처리를 위한 Preference 찾기
        val logoutPreference: androidx.preference.Preference? = findPreference("pref_key_logout")
        logoutPreference?.setOnPreferenceClickListener {
            // RetrofitInstance에서 LoginService 인스턴스를 생성합니다.
            val loginService = RetrofitInstance.createApi(requireContext())

            // 로그아웃 API 요청을 보냅니다.
            loginService.logout().enqueue(object : Callback<LogoutResponse> {
                override fun onResponse(
                    call: Call<LogoutResponse>,
                    response: Response<LogoutResponse>
                ) {
                    if (response.isSuccessful) {
                        // 로그아웃 요청 성공 시, 토큰을 삭제합니다.
                        val tokenManager = TokenManager(requireContext())
                        tokenManager.clearToken()

                        // LoginActivity로 이동합니다.
                        val intent = Intent(activity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)

                        Log.d("SettingsFragment", "로그아웃 처리됨, 토큰 삭제됨")
                    } else {
                        // 응답 실패 시 처리
                        Log.e("SettingsFragment", "로그아웃 요청 실패: ${response.errorBody()?.string()}")
                        Toast.makeText(activity, "로그아웃 요청에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LogoutResponse>, t: Throwable) {
                    // 네트워크 요청 실패 시 처리
                    Log.e("SettingsFragment", "네트워크 요청 실패: ${t.message}")
                    Toast.makeText(activity, "네트워크 요청에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
            true
        }
    }

    private fun checkNotificationPermission(preference: SwitchPreferenceCompat?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val notificationManager = ContextCompat.getSystemService(
                requireContext(),
                NotificationManager::class.java
            ) as NotificationManager
            val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
            preference?.isChecked = areNotificationsEnabled

            if (areNotificationsEnabled) {
                // 알림이 활성화되어 있으면, FCM 토큰을 갱신합니다.
            } else {
                // 알림이 비활성화된 경우 추가 처리가 필요하다면 이곳에 작성
            }
        } else {
            preference?.isChecked = true
            // Android N 미만 버전에서는 기본적으로 알림을 활성화 상태로 간주합니다.
        }
    }
}