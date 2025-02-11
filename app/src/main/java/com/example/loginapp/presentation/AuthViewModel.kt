package com.example.loginapp.presentation

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loginapp.data.auth.GoogleAuthUiClient
import com.example.loginapp.data.auth.SignInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val googleAuthUiClient: GoogleAuthUiClient
) : ViewModel() {



    private val _signInResult = MutableStateFlow<SignInResult?>(null)
    val signInResult: StateFlow<SignInResult?> = _signInResult.asStateFlow()

    fun signIn(context: Context, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        viewModelScope.launch {
            val intentSender = googleAuthUiClient.signIn()
            intentSender?.let {
                val intentSenderRequest = IntentSenderRequest.Builder(it).build()
                launcher.launch(intentSenderRequest)
            }
        }
    }

    fun handleSignInResult(intent: Intent) {
        viewModelScope.launch {
            _signInResult.value = googleAuthUiClient.signInWithIntent(intent)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            googleAuthUiClient.signOut()
            _signInResult.value = null
        }
    }
}