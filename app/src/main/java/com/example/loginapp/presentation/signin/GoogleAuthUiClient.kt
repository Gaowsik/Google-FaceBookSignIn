package com.example.loginapp.presentation.signin

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.example.loginapp.R
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class GoogleAuthUiClient(
    private val context: Context, private val onTapClient: SignInClient
) {

    private val auth = Firebase.auth

    suspend fun signIn(): IntentSender? {
        return try {
            val result = onTapClient.beginSignIn(buildSignInRequest()).await()
            result?.pendingIntent?.intentSender
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            null
        }
    }

    suspend fun signInWithIntent(intent: Intent): SignInResult {
        val credential = onTapClient.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)

        return try {
            val user = auth.signInWithCredential(googleCredentials).await().user
            SignInResult(
                data = user?.run {
                    UserData(
                        userId = uid, userName = displayName
                    )
                }, errorMessage = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e)
            SignInResult(
                data = null, errorMessage = e.message
            )
        }


    }

    suspend fun signOut() {
        try {
            onTapClient.signOut().await()
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


    private fun buildSignInRequest(): BeginSignInRequest {

        return BeginSignInRequest.Builder().setGoogleIdTokenRequestOptions(
            GoogleIdTokenRequestOptions.builder().setSupported(true)
                .setFilterByAuthorizedAccounts(false).setServerClientId(
                    context.getString(
                        R.string.web_client_id
                    )
                ).build()
        ).setAutoSelectEnabled(true).build()
    }
}