package com.recs.sunq.inspection

import android.app.Activity
import android.content.Context
import android.webkit.JavascriptInterface
import com.recs.sunq.inspection.data.TokenManager
import com.recs.sunq.inspection.ui.home.HomeFragment

class WebViewInterface(private val fragment: HomeFragment, private val tokenManager: TokenManager, private val url: String?) {

    @JavascriptInterface
    fun receiveUserInfo(): String? {
        return tokenManager.getUserInfo()
    }

    @JavascriptInterface
    fun toUrl(): String? {
        return url
    }

    @JavascriptInterface
    fun getToken(): String? {
        return tokenManager.getToken()
    }

    @JavascriptInterface
    fun openImageChooser() {
        fragment.requireActivity().runOnUiThread {
            fragment.openImageChooser()
        }
    }
}