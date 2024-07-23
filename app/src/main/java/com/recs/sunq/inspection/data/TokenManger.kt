package com.recs.sunq.inspection.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.json.JSONObject

class TokenManager(context: Context) {
    private var sharedPreferences: SharedPreferences
    private var editor: SharedPreferences.Editor

    init {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        sharedPreferences = EncryptedSharedPreferences.create(
            "preferences",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        editor = sharedPreferences.edit()
    }

    companion object {
        private const val TOKEN_KEY = "sun_q_a_t"
        private const val USER_SEQ = "user_seq"
        private const val PLANT_SEQ = "plant_seq"
        private const val PLANT_NAME = "plant_name"
        private const val USER_ID = "user_id"
    }

    fun saveToken(token: String, userSeq: String, plantSeq: String, plantName: String, userId: String) {

        editor.putString(TOKEN_KEY, token)
        editor.putString(USER_SEQ, userSeq)
        editor.putString(PLANT_SEQ, plantSeq)
        editor.putString(PLANT_NAME, plantName)
        editor.putString(USER_ID, userId)
        editor.apply()
    }

    fun getUserInfo(): String? {
        val userInfo = JSONObject()
        userInfo.put("user_id", sharedPreferences.getString(USER_ID, ""))
        userInfo.put("user_seq", sharedPreferences.getString(USER_SEQ, ""))
        userInfo.put("plant_seq", sharedPreferences.getString(PLANT_SEQ, ""))
        userInfo.put("plant_name", sharedPreferences.getString(PLANT_NAME, ""))
        userInfo.put("device", "mobile")
        userInfo.put("manager", "true")
        return userInfo.toString()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(TOKEN_KEY, null)
    }

    fun getUserseq(): String? {
        Log.d("TokenManager", "Decrypted userSeq: $USER_SEQ")
        return sharedPreferences.getString(USER_SEQ, null)
    }

    fun clearToken() {
        editor.remove(TOKEN_KEY).apply()
        // SessionManager.userInfo = null
    }
}