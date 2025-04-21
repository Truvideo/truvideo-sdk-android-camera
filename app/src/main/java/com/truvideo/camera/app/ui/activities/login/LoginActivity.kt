package com.truvideo.camera.app.ui.activities.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.truvideo.camera.app.ui.activities.home.MainActivity
import com.truvideo.camera.app.ui.theme.TruvideoSdkAppCameraTheme
import com.truvideo.sdk.core.TruvideoSdk
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            TruvideoSdkAppCameraTheme {
                Content()
            }
        }
    }

    @Composable
    private fun Content() {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        fun authenticate() {
            scope.launch {
                try {

                    if (!TruvideoSdk.isAuthenticated() || TruvideoSdk.isAuthenticationExpired()) {
                        val apiKey = "VS2SG9WK"
                        val secret = "ST2K33GR"
                        val payload = TruvideoSdk.generatePayload()
                        val signature = encodeSignature(payload, secret)
                        TruvideoSdk.authenticate(
                            apiKey = apiKey,
                            payload = payload,
                            signature = signature
                        )
                    }
                    TruvideoSdk.initAuthentication()

                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                } catch (exception: Exception) {
                    exception.printStackTrace()

                    AlertDialog.Builder(context)
                        .setTitle("Error")
                        .setMessage(exception.localizedMessage ?: "Unknown error")
                        .setPositiveButton("Retry") { _, _ ->
                            authenticate()
                        }
                        .setNegativeButton("Cancel") { _, _ ->
                            finish()
                        }
                }
            }
        }

        LaunchedEffect(Unit) {
            authenticate()
        }
    }

    private fun encodeSignature(
        payload: String,
        @Suppress("SameParameterValue") secret: String
    ): String {
        val keyBytes: ByteArray = secret.toByteArray(StandardCharsets.UTF_8)
        val messageBytes: ByteArray = payload.toByteArray(StandardCharsets.UTF_8)

        try {
            val hmacSha256 = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(keyBytes, "HmacSHA256")
            hmacSha256.init(secretKeySpec)
            val signatureBytes = hmacSha256.doFinal(messageBytes)
            return bytesToHex(signatureBytes)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        }
        return ""
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val hex = StringBuilder(bytes.size * 2)
        for (i in bytes.indices) {
            val value = bytes[i].toInt() and 0xFF
            hex.append(hexChars[value shr 4])
            hex.append(hexChars[value and 0x0F])
        }
        return hex.toString()
    }
}