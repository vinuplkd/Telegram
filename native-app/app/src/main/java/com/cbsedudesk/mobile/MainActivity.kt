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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var username by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var busy by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf("") }
                var signedInAs by remember { mutableStateOf("") }

                Surface(Modifier.fillMaxSize(), color = Page) {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("CBSE", color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                        Text("EDU DESK", color = Navy, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(8.dp))
                        Text("A private space for two authorised users", color = Color(0xFF66758A), fontSize = 13.sp)
                        Spacer(Modifier.height(34.dp))

                        if (signedInAs.isNotEmpty()) {
                            Text("Welcome", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(signedInAs, color = Navy, fontSize = 17.sp)
                            Text("Your private account is authenticated.", color = Color(0xFF66758A), modifier = Modifier.padding(top = 8.dp))
                            TextButton(onClick = { signedInAs = ""; password = "" }) { Text("Sign out") }
                        } else {
                            Text("PRIVATE SIGN IN", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(18.dp))
                            OutlinedTextField(username, { username = it; error = "" }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                password, { password = it; error = "" }, Modifier.fillMaxWidth(),
                                label = { Text("Server password") }, singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    error = ""
                                    if (username.isBlank() || password.isBlank()) {
                                        error = "Enter your assigned username and password."
                                    } else if (BuildConfig.API_BASE_URL.contains("configure-cloudflare-worker")) {
                                        error = "The private server URL has not been configured yet."
                                    } else {
                                        busy = true
                                        CloudflareApi.login(username.trim(), password) { result ->
                                            runOnUiThread {
                                                busy = false
                                                result.onSuccess { response ->
                                                    signedInAs = response.optJSONObject("user")?.optString("username") ?: username.trim()
                                                    password = ""
                                                }.onFailure { error = it.message ?: "Sign-in failed. Please try again." }
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
                            if (error.isNotEmpty()) Text(error, color = Color(0xFFB3261E), fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp))
                            Spacer(Modifier.height(20.dp))
                            Text("Access is restricted. There is no public registration.", color = Color(0xFF66758A), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}