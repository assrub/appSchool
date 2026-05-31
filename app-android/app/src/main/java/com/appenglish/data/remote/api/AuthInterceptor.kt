package com.appenglish.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    companion object {
        var token: String? = null
        var userId: Int? = null
        var username: String? = null
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val authRequest = if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else request
        return chain.proceed(authRequest)
    }
}
