package com.cbsedudesk.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF102B4E)
private val Page = Color(0xFFF4F7FB)
private val Muted = Color(0xFF66758A)

private enum class AppPage { LOGIN, SET_PIN, UNLOCK, HOME }

class MainActivity : ComponentActivity() {
    private var page by mutableStateOf(AppPage.LOGIN)
    private var signedInUser by mutableStateOf("")
    private var signedInUserId by mutableStateOf("")
    private var sessionToken by mutableStateOf("")
    private var busy by mutableStateOf(false)
    private var error by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionToken = SecureVault.get(this, "session_token").orEmpty()
        signedInUser = SecureVault.get(this, "username").orEmpty()
        signedInUserId = SecureVault.get(this, "user_id").orEmpty()
        if (sessionToken.isNotBlank() && signedInUser.isNotBlank()) {
            page = if (SecureVault.get(this, "pin_verifier").isNullOrBlank()) {
                AppPage.SET_PIN
            } else {
                AppPage.UNLOCK
            }
        }

        setContent {
            MaterialTheme {
                var username by remember { mutableStateOf(signedInUser) }
                var password by remember { mutableStateOf("") }
                var pin by remember(page) { mutableStateOf("") }
                var confirmPin by remember(page) { mutableStateOf("") }
                var localError by remember(page) { mutableStateOf("") }

                Surface(Modifier.fillMaxSize(), color = Page) {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("CBSE", color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                        Text("EDU DESK", color = Navy, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(8.dp))
                        Text("A private space for two authorised users", color = Muted, fontSize = 13.sp)
                        Spacer(Modifier.height(34.dp))

                        when (page) {
                            AppPage.LOGIN -> {
                                Text("PRIVATE SIGN IN", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                Spacer(Modifier.height(18.dp))
                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it; error = "" },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Assigned username") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it; error = "" },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Private server password") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        error = ""
                                        if (username.isBlank() || password.isBlank()) {
                                            error = "Enter your assigned username and server password."
                                        } else {
                                            busy = true
                                            CloudflareApi.login(username.trim(), password) { result ->
                                                runOnUiThread {
                                                    busy = false
                                                    result.onSuccess { response ->
                                                        val user = response.optJSONObject("user")
                                                        val token = response.optString("accessToken")
                                                        val userName = user?.optString("username").orEmpty()
                                                        val userId = user?.optString("id").orEmpty()
                                                        if (token.isBlank() || userName.isBlank() || userId.isBlank()) {
                                                            error = "The server returned an incomplete sign-in response."
                                                        } else {
                                                            sessionToken = token
                                                            signedInUser = userName
                                                            signedInUserId = userId
                                                            SecureVault.put(this@MainActivity, "session_token", token)
                                                            SecureVault.put(this@MainActivity, "username", userName)
                                                            SecureVault.put(this@MainActivity, "user_id", userId)
                                                            password = ""
                                                            page = if (SecureVault.get(this@MainActivity, "pin_verifier").isNullOrBlank()) AppPage.SET_PIN else AppPage.UNLOCK
                                                        }
                                                    }.onFailure {
                                                        error = it.message ?: "Sign-in failed. Please try again."
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    enabled = !busy,
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (busy) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                                    else Text("SIGN IN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                                if (error.isNotEmpty()) ErrorText(error)
                                Spacer(Modifier.height(20.dp))
                                Text("Access is restricted. There is no public registration.", color = Muted, fontSize = 12.sp)
                            }

                            AppPage.SET_PIN -> {
                                Text("CREATE YOUR 4-DIGIT PIN", color = Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text("This PIN stays on this device. It is not sent to the server.", color = Muted, fontSize = 13.sp)
                                Spacer(Modifier.height(20.dp))
                                PinField(pin, { pin = it.take(4).filter(Char::isDigit); localError = "" }, "Create 4-digit PIN")
                                Spacer(Modifier.height(12.dp))
                                PinField(confirmPin, { confirmPin = it.take(4).filter(Char::isDigit); localError = "" }, "Confirm 4-digit PIN")
                                Spacer(Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        if (pin.length != 4 || confirmPin.length != 4) {
                                            localError = "Enter four digits in both fields."
                                        } else if (pin != confirmPin) {
                                            localError = "The PINs do not match. Try again."
                                            confirmPin = ""
                                        } else {
                                            SecureVault.put(this@MainActivity, "pin_verifier", PinSecurity.createVerifier(pin))
                                            page = AppPage.HOME
                                            localError = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("SAVE PIN", fontWeight = FontWeight.Bold) }
                                if (localError.isNotEmpty()) ErrorText(localError)
                            }

                            AppPage.UNLOCK -> {
                                Text("WELCOME BACK", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text(signedInUser, color = Muted, fontSize = 14.sp)
                                Spacer(Modifier.height(22.dp))
                                PinField(pin, { pin = it.take(4).filter(Char::isDigit); localError = "" }, "Enter your 4-digit PIN")
                                Spacer(Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        val verifier = SecureVault.get(this@MainActivity, "pin_verifier")
                                        if (verifier != null && PinSecurity.verify(pin, verifier)) {
                                            page = AppPage.HOME
                                            localError = ""
                                        } else {
                                            localError = "Incorrect PIN. Please try again."
                                            pin = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("UNLOCK", fontWeight = FontWeight.Bold) }
                                if (localError.isNotEmpty()) ErrorText(localError)
                            }

                            AppPage.HOME -> {
                                Text("PRIVATE MESSENGER", color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(12.dp))
                                Text("Signed in as $signedInUser", color = Navy, fontSize = 16.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Your local PIN lock is active. The chat interface and end-to-end encryption are the next development milestone.", color = Muted, fontSize = 13.sp)
                                Spacer(Modifier.height(24.dp))
                                OutlinedButton(
                                    onClick = {
                                        SecureVault.clearSession(this@MainActivity)
                                        SecureVault.remove(this@MainActivity, "pin_verifier")
                                        sessionToken = ""
                                        signedInUser = ""
                                        signedInUserId = ""
                                        username = ""
                                        page = AppPage.LOGIN
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("SIGN OUT") }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (sessionToken.isNotBlank() && page != AppPage.LOGIN) {
            page = AppPage.UNLOCK
        }
    }
}

@Composable
private fun PinField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun ErrorText(message: String) {
    Text(message, color = Color(0xFFB3261E), fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp))
}
