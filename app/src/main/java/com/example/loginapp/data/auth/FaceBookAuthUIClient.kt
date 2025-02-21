package com.example.loginapp.data.auth

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resumeWithException


class FaceBookAuthUIClient(private val context: Context) {

    private val auth = Firebase.auth
    val callbackManager = CallbackManager.Factory.create()


    fun signIn(activity: Activity) {
        LoginManager.getInstance().logInWithReadPermissions(
            activity, listOf("email", "public_profile")
        )
    }


    suspend fun handleFacebookAccessToken(token: AccessToken): SignInResult {
        Log.d(TAG, "handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        return try {
            val user = auth.signInWithCredential(credential).await().user
            SignInResult(
                data = user?.run {
                    UserData(
                        userId = uid, userName = displayName
                    )
                }, errorMessage = null
            )

        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            SignInResult(
                data = null, errorMessage = e.message
            )

        }

    }

    fun signOut() {
        try {
            LoginManager.getInstance().logOut()
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
        }
    }

    fun getSignedInUser(): UserData? = auth.currentUser?.run {
        UserData(
            userId = uid, userName = displayName
        )
    }

    /////


    @ExperimentalCoroutinesApi
    suspend fun getFacebookToken(): LoginResult =
        suspendCancellableCoroutine { continuation ->
            LoginManager.getInstance()
                .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {

                    override fun onSuccess(loginResult: LoginResult) {
                        continuation.resume(loginResult) { }
                    }

                    override fun onCancel() {
                        // handling cancelled flow (probably don't need anything here)
                        continuation.cancel()
                    }

                    override fun onError(exception: FacebookException) {
                        // Facebook authorization error
                        continuation.resumeWithException(exception)
                    }
                })
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun finishFacebookLoginToThirdParty(
        onCredential: (LoginResult) -> Unit
    ) {
        try {
            val loginResult: LoginResult = getFacebookToken()
            onCredential(loginResult)
        } catch (e: FacebookException) {
            Log.e("Facebook Error", e.toString())
        }
    }


}

