package com.example.comforterproject.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.example.comforterproject.model.AuthResponse
import com.example.comforterproject.model.LoginRequest
import com.example.comforterproject.model.SignupRequest
import com.example.comforterproject.repository.AuthRepository
import kotlinx.coroutines.launch
class AuthViewModel : ViewModel() {
    private val repo = AuthRepository()
    private val _signupResult = MutableLiveData<AuthResponse>()
    val signupResult: LiveData<AuthResponse> = _signupResult
    private val _loginResult = MutableLiveData<AuthResponse>()
    val loginResult: LiveData<AuthResponse> = _loginResult
    fun signup(request: SignupRequest) {
        viewModelScope.launch {
            try {
                val response = repo.signup(request)
                if (response.isSuccessful && response.body() != null) {
                    _signupResult.postValue(response.body())
                } else {
                    val error = response.errorBody()?.string()
                    Log.e("API_ERROR", error ?: "Unknown error")

                    _signupResult.postValue(
                        AuthResponse(false, error ?: "Signup Failed")
                    )
                }
            } catch (e: Exception) {
                _signupResult.postValue(
                    AuthResponse(false, "Exception: ${e.message}")
                )
            }
        }
    }
    fun login(request: LoginRequest) {
        viewModelScope.launch {
            try {
                val response = repo.login(request)

                if (response.isSuccessful && response.body() != null) {
                    _loginResult.postValue(response.body())
                } else {
                    val error = response.errorBody()?.string()
                    Log.e("API_ERROR", error ?: "Unknown error")

                    _loginResult.postValue(
                        AuthResponse(false, error ?: "Login Failed")
                    )
                }
            } catch (e: Exception) {
                _loginResult.postValue(
                    AuthResponse(false, "Exception: ${e.message}")
                )
            }
        }
    }
}