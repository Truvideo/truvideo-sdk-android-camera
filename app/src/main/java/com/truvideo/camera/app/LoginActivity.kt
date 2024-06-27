package com.truvideo.camera.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truvideo.sdk.components.login.TruvideoLoginComponent
import com.truvideo.sdk.core.TruvideoSdk
import truvideo.sdk.common.model.TruvideoSdkEnvironment
import truvideo.sdk.common.sdk_common

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TruvideoSdk.clearAuthentication()
//        sdk_common.configuration.environment = TruvideoSdkEnvironment.PROD

        setContent {
            Content()
        }
    }

    @Composable
    private fun Content() {
        val apiKey = when (sdk_common.configuration.environment) {
            TruvideoSdkEnvironment.BETA -> "VS2SG9WK"
//            TruvideoSdkEnvironment.RC -> "VS2SG9WK" // Ours
            TruvideoSdkEnvironment.RC -> "0EeGlpbESu" // Reynolds
//            TruvideoSdkEnvironment.PROD -> "EPhPPsbv7e" // ours
            TruvideoSdkEnvironment.PROD -> "5esxyUUl0t" // Reynolds
        }

        val secret = when (sdk_common.configuration.environment) {
            TruvideoSdkEnvironment.BETA -> "ST2K33GR"
//            TruvideoSdkEnvironment.RC -> "ST2K33GR" // Ours
            TruvideoSdkEnvironment.RC -> "QDjx0T9RyD" // Reynolds
//            TruvideoSdkEnvironment.PROD -> "9lHCnkfeLl" // Ours
            TruvideoSdkEnvironment.PROD -> "PCRE0bdAce" // Reynolds
        }


        Box(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TruvideoLoginComponent(
                apiKey = apiKey,
                secret = secret,
                isAuthenticated = { TruvideoSdk.isAuthenticated },
                isAuthenticationExpired = { TruvideoSdk.isAuthenticationExpired },
                generatePayload = { TruvideoSdk.generatePayload() },
                authenticate = { apiKey, payload, secret -> TruvideoSdk.authenticate(apiKey, payload, secret) },
                init = { TruvideoSdk.initAuthentication() },
                callback = {
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent)
                }
            )
        }
    }
}