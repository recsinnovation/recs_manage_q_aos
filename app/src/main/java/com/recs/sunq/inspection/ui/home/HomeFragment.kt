package com.recs.sunq.inspection.ui.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.recs.sunq.inspection.R
import com.recs.sunq.inspection.UrlHandler
import com.recs.sunq.inspection.WebViewInterface
import com.recs.sunq.inspection.data.TokenManager
import com.recs.sunq.inspection.databinding.FragmentHomeBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(), UrlHandler {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var tokenManager: TokenManager
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val REQUEST_FILE_CHOOSER = 1001
    private var cameraImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext().applicationContext)

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            binding.webView.reload()
        }

        val url = arguments?.getString("url") ?: "https://192.168.0.28:5173/device-management/inspection/history"
        setupWebView(url)
    }

    private fun setupWebView(url: String) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webView, true)

        val toUrl: String? = "device-management/inspection/history"
        binding.webView.addJavascriptInterface(WebViewInterface(this, tokenManager, toUrl), "AndroidInterface")

        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true

            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    if (hasRequiredPermissions()) {
                        this@HomeFragment.filePathCallback = filePathCallback
                        openImageChooser()
                        return true
                    } else {
                        // 권한이 없을 경우 파일 선택기 비활성화
                        showPermissionRationaleDialog()
//                        Toast.makeText(requireContext(), "카메라 및 갤러리 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                        return false
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    println("Page started: $url")

                    // 토큰을 쿠키로 설정
                    val token = tokenManager.getToken()
                    if (!token.isNullOrEmpty()) {
                        CookieManager.getInstance().setCookie(url, "token=$token;")
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    println("Page finished: $url")
                    binding.webView.evaluateJavascript("injectSessionData();") {
                        println("Injected session data.")
                    }
                    swipeRefreshLayout.isRefreshing = false
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    handler?.proceed() // SSL 인증 오류 무시하고 계속 진행
                }
            }
            loadUrl(url)
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        val readMediaImagesPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        Log.d("Permissions", "Camera: $cameraPermission, Read: $readMediaImagesPermission")

        return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                readMediaImagesPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("권한 요청")
            .setMessage("파일찾기 기능을 사용하기 위해 카메라 및 저장소 권한이 필요합니다. 설정에서 권한을 허용해 주세요.")
            .setPositiveButton("설정으로 가기") { _, _ ->
                // 설정 화면으로 이동
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(requireContext(), "파일찾기 권한이 거부되었습니다 앱 설정에서 권한을 허용해 주세요.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    fun openImageChooser() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }
            photoFile?.also {
                cameraImageUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
            }
        }

        val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickPhotoIntent.type = "image/*"
        pickPhotoIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        val chooserIntent = Intent.createChooser(pickPhotoIntent, "이미지 선택")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePictureIntent))

        startActivityForResult(chooserIntent, REQUEST_FILE_CHOOSER)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_FILE_CHOOSER) {
            if (resultCode == Activity.RESULT_OK) {
                val result: Array<Uri>? = data?.clipData?.let { clipData ->
                    val uriList = mutableListOf<Uri>()
                    for (i in 0 until clipData.itemCount) {
                        uriList.add(clipData.getItemAt(i).uri)
                    }
                    if (uriList.size > 10) {
                        Toast.makeText(requireContext(), "최대 10장까지 선택할 수 있습니다.", Toast.LENGTH_SHORT).show()
                        null
                    } else {
                        uriList.toTypedArray()
                    }
                } ?: data?.data?.let { arrayOf(it) } ?: cameraImageUri?.let { arrayOf(it) }

                // URI 데이터를 로그로 출력
                result?.forEach { uri ->
                    Log.d("Selected URI", uri.toString())
                }
                filePathCallback?.onReceiveValue(result)
            } else {
                filePathCallback?.onReceiveValue(null)
            }
            filePathCallback = null
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun updateUrl(newUrl: String) {
        binding.webView.loadUrl(newUrl)
    }

    fun canGoBack(): Boolean {
        return binding.webView.canGoBack()
    }

    fun goBack() {
        binding.webView.goBack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}