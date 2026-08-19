package com.ntvelop.mobileparastatiko.api

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ntvelop_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_USERNAME = "KEY_USERNAME"
        const val KEY_VAT = "KEY_VAT"
        const val KEY_SUB_KEY = "KEY_SUB_KEY"
        const val KEY_IS_LOGGED_IN = "KEY_IS_LOGGED_IN"
    }

    fun saveCredentials(username: String, vat: String, subKey: String) {
        prefs.edit().apply {
            putString(KEY_USERNAME, username)
            putString(KEY_VAT, vat)
            putString(KEY_SUB_KEY, subKey)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getVat(): String? = prefs.getString(KEY_VAT, null)
    fun getSubscriptionKey(): String? = prefs.getString(KEY_SUB_KEY, null)

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logout() {
        // We only set the login flag to false, so the text fields will still remember the last input
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
    }
    
    fun setDocumentRole(docId: String, role: String) {
        prefs.edit().putString("ROLE_$docId", role).apply()
    }

    fun getDocumentRole(docId: String): String? {
        return prefs.getString("ROLE_$docId", null)
    }

    fun saveDocumentMark(docId: String, mark: Long) {
        prefs.edit().putLong("MARK_$docId", mark).apply()
    }

    fun getDocumentMark(docId: String): Long {
        return prefs.getLong("MARK_$docId", 0L)
    }

    fun setSandboxMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_sandbox", enabled).apply()
    }

    fun isSandboxMode(): Boolean {
        return prefs.getBoolean("is_sandbox", true)
    }
}
