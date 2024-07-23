package com.recs.sunq.inspection

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.ViewModelProvider
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.recs.sunq.androidapp.data.Result
import com.recs.sunq.androidapp.ui.login.LoginViewModel
import com.recs.sunq.androidapp.ui.login.LoginViewModelFactory
import com.recs.sunq.inspection.data.TokenManager
import com.recs.sunq.inspection.data.model.LoginResponse
import com.recs.sunq.inspection.data.model.User
import com.recs.sunq.inspection.databinding.ActivityLoginBinding
import jp.wasabeef.blurry.Blurry

class LoginActivity : AppCompatActivity() {

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding
    private lateinit var encryptedPreferences: SharedPreferences
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize EncryptedSharedPreferences
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        encryptedPreferences = EncryptedSharedPreferences.create(
            "encryptedLoginPrefs",
            masterKeyAlias,
            applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        tokenManager = TokenManager(applicationContext)

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionsResult(permissions)
        }

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory(applicationContext))
            .get(LoginViewModel::class.java)

        setupLoginForm()
        observeLoginFormState()
        observeNavigationToMainActivity()
        loadLoginPreferences()

        checkPermissions()
    }

    private fun updatePermissionPreference(permission: String, isGranted: Boolean) {
        val sharedPreferences = getSharedPreferences("appPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(permission, isGranted).apply()
    }

    private fun setupLoginForm() {
        val username = binding.username
        val password = binding.password
        val login = binding.login
        val loading = binding.loading
        val rememberId = binding.rememberId
        val rememberPw = binding.rememberPw

        // 기본적으로 로그인 버튼을 활성화 상태로 설정
        login.isEnabled = true

        login.setOnClickListener {
            loading.visibility = View.VISIBLE
            if (rememberId?.isChecked == true) {
                saveLoginPreferences(username.text.toString(), rememberPw?.isChecked == true)
            } else {
                clearLoginPreferences()
            }
            loginViewModel.login(username.text.toString(), password.text.toString())
        }
    }

    private fun saveLoginPreferences(username: String, autoLogin: Boolean) {
        with(encryptedPreferences.edit()) {
            putBoolean("rememberId", true)
            putString("username", username)
            putBoolean("autoLogin", autoLogin) // Ensure autoLogin is saved correctly
            apply()
        }
    }

    private fun loadLoginPreferences() {
        val rememberIdPreference = encryptedPreferences.getBoolean("rememberId", false)
        val username = encryptedPreferences.getString("username", "")
        val autoLogin = encryptedPreferences.getBoolean("autoLogin", false)

        binding.rememberId?.isChecked = rememberIdPreference // Safe call with null check
        binding.username.setText(username)
        binding.rememberPw?.isChecked = autoLogin // Safe call with null check
    }

    private fun clearLoginPreferences() {
        with(encryptedPreferences.edit()) {
            clear()
            apply()
        }
    }

    private fun checkPermissions() {
        val sharedPreferences = getSharedPreferences("appPreferences", Context.MODE_PRIVATE)
        val isNotificationPermissionGranted = sharedPreferences.getBoolean("notificationPermission", false)
        val isCameraPermissionGranted = sharedPreferences.getBoolean("cameraPermission", false)
        val isStoragePermissionGranted = sharedPreferences.getBoolean("storagePermission", false)

        val permissionsToRequest = mutableListOf<String>()

        if (!isNotificationPermissionGranted) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                sharedPreferences.edit().putBoolean("notificationPermission", true).apply()
            }
        }

        if (!isCameraPermissionGranted) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.CAMERA)
            } else {
                sharedPreferences.edit().putBoolean("cameraPermission", true).apply()
            }
        }

        if (!isStoragePermissionGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    sharedPreferences.edit().putBoolean("storagePermission", true).apply()
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    sharedPreferences.edit().putBoolean("storagePermission", true).apply()
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d("LoginActivity", "모든 권한이 승인됨")
        }
    }

    private fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        var showDialog = false
        var message = ""

        permissions.forEach { (permission, isGranted) ->
            if (!isGranted) {
                showDialog = true
                when (permission) {
                    Manifest.permission.CAMERA -> message = "카메라 권한이 필요합니다."
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_EXTERNAL_STORAGE -> message = "갤러리 권한이 필요합니다."
                }
            }
        }

        if (showDialog) {
            showPermissionsDeniedDialog(message)
        }
    }

    private fun showPermissionsDeniedDialog(message: String) {
        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            .setTitle("권한 필요")
            .setMessage("$message 앱 설정에서 권한을 활성화해 주세요.")
            .setPositiveButton("설정") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${packageName}")
                }
                startActivity(intent)
            }
            .setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.show()
    }

    private fun observeLoginFormState() {
        loginViewModel.loginResult.observe(this) { result ->
            val loading = binding.loading
            loading.visibility = View.GONE
            when (result) {
                is Result.Loading -> {
                    loading.visibility = View.VISIBLE
                }
                is Result.Success<*> -> {
                    val loginResponse = result.data as? LoginResponse
                    val user = loginResponse?.data as? User
                    user?.let {
                        checkPermissions()
                        tokenManager.saveToken(it.token, it.user_seq, it.plant_info.plant_seq, it.plant_info.plant_name, it.user_id)
                    }
                }
                is Result.Error -> {
                    showLoginFailedDialog()
                }
            }
        }
    }

    private fun showLoginFailedDialog() {
        // Capture the current screen to blur
        val rootView = window.decorView.rootView
        rootView.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(rootView.drawingCache)
        rootView.isDrawingCacheEnabled = false

        // Apply blur
        val blurredBackground = findViewById<ImageView>(R.id.blurred_background)
        Blurry.with(this).from(bitmap).into(blurredBackground)
        blurredBackground.visibility = View.VISIBLE

        // Create and show the dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(R.layout.loginfail)

        // Set up the dialog views
        val confirmButton = dialog.findViewById<Button>(R.id.confirm_button)
        confirmButton.setOnClickListener {
            dialog.dismiss()
            blurredBackground.visibility = View.GONE
        }

        dialog.setOnDismissListener {
            blurredBackground.visibility = View.GONE
        }

        dialog.show()
    }

    private fun observeNavigationToMainActivity() {
        loginViewModel.navigateToMain.observe(this) { navigate ->
            if (navigate) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}