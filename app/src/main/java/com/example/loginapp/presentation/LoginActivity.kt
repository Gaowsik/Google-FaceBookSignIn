package com.example.loginapp.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.loginapp.collectLatestLifeCycleFlow
import com.example.loginapp.data.auth.FaceBookAuthUIClient
import com.example.loginapp.data.auth.GoogleAuthUiClient
import com.example.loginapp.databinding.ActivityLoginBinding
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            onTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    private val facebookAuthUiClient by lazy {
        FaceBookAuthUIClient(
            applicationContext
        )
    }

    private val viewModel: AuthViewModel by viewModels()

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let {
                    lifecycleScope.launch {
                        val signInResult = googleAuthUiClient.signInWithIntent(
                            intent = result.data ?: return@launch
                        )
                        viewModel.onSignInResult(signInResult)
                    }
                }
            }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        facebookAuthUiClient.callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)

    }


    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        init()

    }

    private fun init() {
        bindUi()
        setUpListener()
        setUpObserver()
        checkUserLoggedIn()
        handleUserSignOutStateUi()
    }

    private fun bindUi() {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun handleUserSignOutStateUi() {
        binding.constraintProfile.visibility = View.GONE
        binding.btnGoogleSignIn.visibility = View.VISIBLE
        binding.btnFbSignIn.visibility = View.VISIBLE
    }

    private fun handleUserSignInStateUi() {
        binding.constraintProfile.visibility = View.VISIBLE
        binding.btnGoogleSignIn.visibility = View.GONE
        binding.btnFbSignIn.visibility = View.GONE
    }

    private fun setUpListener() {
        binding.btnGoogleSignIn.setOnClickListener {
            lifecycleScope.launch {
                val signInIntentSender = googleAuthUiClient.signIn()
                signInLauncher.launch(
                    IntentSenderRequest.Builder(
                        signInIntentSender ?: return@launch
                    ).build()
                )
            }

        }

        binding.loginButton.setOnClickListener {
            this.let { activity ->
                lifecycleScope.launch {
                    facebookAuthUiClient.signIn(activity)
                    facebookAuthUiClient.finishFacebookLoginToThirdParty { loginResult ->
                        lifecycleScope.launch {
                            val signInResult = facebookAuthUiClient.handleFacebookAccessToken(loginResult.accessToken)
                            // Handle the sign-in result, e.g., update UI or show a message
                            if (signInResult.data != null) {
                                Log.d("FacebookLogin", "User signed in: ${signInResult.data.userName}")
                            } else {
                                Log.e("FacebookLogin", "Sign-in failed: ${signInResult.errorMessage}")
                            }
                        }
                    }
                }
            }

        }

        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                googleAuthUiClient.signOut()
                viewModel.resetState()
                handleUserSignOutStateUi()
            }
        }
    }

    private fun setUpObserver() {
        this.collectLatestLifeCycleFlow(viewModel.state) { state ->
            if (state.isSignInSuccessful) {
                Toast.makeText(this, "Sign In SuccessFull", Toast.LENGTH_SHORT).show()
                checkUserLoggedIn()
            }
        }
    }

    private fun checkUserLoggedIn() {
        googleAuthUiClient.getSignedInUser()?.let {
            handleUserSignInStateUi()
            binding.textUserName.text = it.userName
        }
    }
}