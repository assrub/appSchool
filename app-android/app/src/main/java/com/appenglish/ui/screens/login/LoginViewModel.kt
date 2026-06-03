package com.appenglish.ui.screens.login

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appenglish.data.remote.api.AuthInterceptor
import com.appenglish.util.ApiConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val app: Application
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun checkSavedSession() {
        if (AuthInterceptor.loadSession(app)) {
            _uiState.value = _uiState.value.copy(isLoggedIn = true)
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = withContext(Dispatchers.IO) {
                    val client = com.appenglish.util.HttpClientFactory.getInstance()
                    val json = JSONObject().apply {
                        put("username", username)
                        put("password", password)
                    }
                    val body = json.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${ApiConfig.BASE_URL}auth/app/login")
                        .post(body)
                        .build()
                    client.newCall(request).execute()
                }

                if (!result.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Usuario o contraseña incorrectos"
                    )
                    return@launch
                }

                val responseJson = JSONObject(result.body?.string() ?: "")
                val token = responseJson.getString("access_token")
                val userId = responseJson.getInt("userId")
                val displayName = responseJson.getString("displayName")

                AuthInterceptor.token = token
                AuthInterceptor.userId = userId
                AuthInterceptor.username = displayName
                AuthInterceptor.saveSession(app)

                _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error de conexión: ${e.message}"
                )
            }
        }
    }
}
