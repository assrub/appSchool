package com.appenglish.data.remote.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    companion object {
        var token: String? = null
        var userId: Int? = null
        var username: String? = null

        fun saveSession(context: Context) {
            val prefs = context.getSharedPreferences("appschool", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("token", token)
                .putInt("userId", userId ?: 0)
                .putString("username", username)
                .apply()
        }

        fun loadSession(context: Context): Boolean {
            val prefs = context.getSharedPreferences("appschool", Context.MODE_PRIVATE)
            token = prefs.getString("token", null)
            userId = prefs.getInt("userId", 0).takeIf { it > 0 }
            username = prefs.getString("username", null)
            return !token.isNullOrEmpty()
        }

        fun clearSession(context: Context) {
            token = null; userId = null; username = null
            context.getSharedPreferences("appschool", Context.MODE_PRIVATE).edit().clear().apply()
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val authRequest = if (token != null) {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        } else request
        return chain.proceed(authRequest)
    }
}
