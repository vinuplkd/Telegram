package com.cbsedudesk.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private val Navy = Color(0xFF102B4E)
private val Page = Color(0xFFF4F7FB)
private val Muted = Color(0xFF66758A)
private val White = Color.White

private enum class AppPage { LOGIN, SET_PIN, UNLOCK, HOME }

class MainActivity : ComponentActivity() {
    private var page by mutableStateOf(AppPage.LOGIN)
    private var signedInUser by mutableStateOf("")
    private var signedInUserId by mutableStateOf("")
    private var sessionToken by mutableStateOf("")
    private var busy by mutableStateOf(false)
    private var error by mutableStateOf("")
    private var partnerName by mutableStateOf("")
    private var partnerId by mutableStateOf("")
    private var messages by mutableStateOf<List<JSONObject>>(emptyList())
    private var loadingMessages by mutableStateOf(false)
    private var liveSocket: okhttp3.WebSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionToken = SecureVault.get(this, "session_token").orEmpty()
        signedInUser = SecureVault.get(this, "username").orEmpty()
        signedInUserId = SecureVault.get(this, "user_id").orEmpty()
        if (sessionToken.isNotBlank() && signedInUser.isNotBlank()) {
            page = if (SecureVault.get(this, "pin_verifier").isNullOrBlank()) AppPage.SET_PIN else AppPage.UNLOCK
        }

        setContent {
            MaterialTheme {
                var username by remember { mutableStateOf(signedInUser) }
                var password by remember { mutableStateOf("") }
                var pin by remember(page) { mutableStateOf("") }
                var confirmPin by remember(page) { mutableStateOf("") }
                var localError by remember(page) { mutableStateOf("") }
                val passwordFocusRequester = remember { FocusRequester() }
                val focusManager = LocalFocusManager.current

                LaunchedEffect(page, sessionToken) {
                    if (page == AppPage.HOME && sessionToken.isNotBlank()) loadConversation()
                }

                Surface(Modifier.fillMaxSize(), color = Page) {
                    when (page) {
                        AppPage.LOGIN -> Column(
                            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("CBSE", color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                            Text("EDU DESK", color = Navy, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(8.dp))
                            Text("A private space for two authorised users", color = Muted, fontSize = 13.sp)
                            Spacer(Modifier.height(34.dp))
                            Text("PRIVATE SIGN IN", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(18.dp))
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it; error = "" },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Assigned username") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it; error = "" },
                                modifier = Modifier.fillMaxWidth().focusRequester(passwordFocusRequester),
                                label = { Text("Private server password") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    error = ""
                                    if (username.isBlank() || password.isBlank()) error = "Enter your assigned username and server password."
                                    else {
                                        busy = true
                                        CloudflareApi.login(username.trim(), password) { result ->
                                            runOnUiThread {
                                                busy = false
                                                result.onSuccess { response ->
                                                    val user = response.optJSONObject("user")
                                                    val token = response.optString("accessToken")
                                                    val userName = user?.optString("username").orEmpty()
                                                    val userId = user?.optString("id").orEmpty()
                                                    if (token.isBlank() || userName.isBlank() || userId.isBlank()) error = "The server returned an incomplete sign-in response."
                                                    else {
                                                        sessionToken = token
                                                        signedInUser = userName
                                                        signedInUserId = userId
                                                        SecureVault.put(this@MainActivity, "session_token", token)
                                                        SecureVault.put(this@MainActivity, "username", userName)
                                                        SecureVault.put(this@MainActivity, "user_id", userId)
                                                        password = ""
                                                        page = if (SecureVault.get(this@MainActivity, "pin_verifier").isNullOrBlank()) AppPage.SET_PIN else AppPage.UNLOCK
                                                    }
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
                                if (busy) CircularProgressIndicator(color = White, strokeWidth = 2.dp)
                                else Text("SIGN IN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                            if (error.isNotEmpty()) ErrorText(error)
                            Spacer(Modifier.height(20.dp))
                            Text("Access is restricted. There is no public registration.", color = Muted, fontSize = 12.sp)
                        }

                        AppPage.SET_PIN -> Column(
                            Modifier.fillMaxSize().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("CREATE YOUR 4-DIGIT PIN", color = Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("This PIN stays on this device. It is not sent to the server.", color = Muted, fontSize = 13.sp)
                            Spacer(Modifier.height(20.dp))
                            PinField(pin, { pin = it.take(4).filter(Char::isDigit); localError = "" }, "Create 4-digit PIN")
                            Spacer(Modifier.height(12.dp))
                            PinField(confirmPin, { confirmPin = it.take(4).filter(Char::isDigit); localError = "" }, "Confirm 4-digit PIN")
                            Spacer(Modifier.height(18.dp))
                            Button(onClick = {
                                if (pin.length != 4 || confirmPin.length != 4) localError = "Enter four digits in both fields."
                                else if (pin != confirmPin) { localError = "The PINs do not match. Try again."; confirmPin = "" }
                                else {
                                    SecureVault.put(this@MainActivity, "pin_verifier", PinSecurity.createVerifier(pin))
                                    page = AppPage.HOME
                                    localError = ""
                                }
                            }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Navy)) {
                                Text("SAVE PIN", fontWeight = FontWeight.Bold)
                            }
                            if (localError.isNotEmpty()) ErrorText(localError)
                        }

                        AppPage.UNLOCK -> Column(
                            Modifier.fillMaxSize().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("WELCOME BACK", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(signedInUser, color = Muted, fontSize = 14.sp)
                            Spacer(Modifier.height(22.dp))
                            PinField(pin, { pin = it.take(4).filter(Char::isDigit); localError = "" }, "Enter your 4-digit PIN")
                            Spacer(Modifier.height(18.dp))
                            Button(onClick = {
                                val verifier = SecureVault.get(this@MainActivity, "pin_verifier")
                                if (verifier != null && PinSecurity.verify(pin, verifier)) { page = AppPage.HOME; localError = "" }
                                else { localError = "Incorrect PIN. Please try again."; pin = "" }
                            }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Navy)) {
                                Text("UNLOCK", fontWeight = FontWeight.Bold)
                            }
                            if (localError.isNotEmpty()) ErrorText(localError)
                        }

                        AppPage.HOME -> ConversationScreen(
                            username = signedInUser,
                            currentUserId = signedInUserId,
                            partner = partnerName,
                            messages = messages,
                            loading = loadingMessages,
                            onRefresh = { loadConversation() },
                            onSendMessage = { text ->
                                if (partnerId.isNotBlank() && text.isNotBlank()) {
                                    val encoded = android.util.Base64.encodeToString(text.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                                    CloudflareApi.sendEncryptedMessage(sessionToken, partnerId, encoded, "plain-v1") { result ->
                                        runOnUiThread {
                                            result.onSuccess { response ->
                                                val sent = JSONObject()
                                                    .put("id", response.optString("id"))
                                                    .put("senderId", signedInUserId)
                                                    .put("recipientId", partnerId)
                                                    .put("ciphertext", encoded)
                                                    .put("nonce", "plain-v1")
                                                    .put("createdAt", response.optString("createdAt"))
                                                messages = messages + sent
                                            }.onFailure { error = it.message ?: "Message could not be sent." }
                                        }
                                    }
                                }
                            },
                            onSignOut = {
                                SecureVault.clearSession(this@MainActivity)
                                SecureVault.remove(this@MainActivity, "pin_verifier")
                                sessionToken = ""
                                signedInUser = ""
                                signedInUserId = ""
                                partnerName = ""
                                partnerId = ""
                                messages = emptyList()
                                username = ""
                                page = AppPage.LOGIN
                            }
                        )
                    }
                }
            }
        }
    }

    private fun loadConversation() {
        if (sessionToken.isBlank()) return
        loadingMessages = true
        CloudflareApi.fetchPartner(sessionToken) { partnerResult ->
            runOnUiThread {
                partnerResult.onSuccess { response ->
                    val partner = response.optJSONObject("partner")
                    partnerName = partner?.optString("username").orEmpty()
                    partnerId = partner?.optString("id").orEmpty()
                    if (partnerId.isNotBlank() && liveSocket == null) {
                        liveSocket = CloudflareApi.connectLive(sessionToken, object : okhttp3.WebSocketListener() {
                            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                                runOnUiThread {
                                    try {
                                        val event = JSONObject(text)
                                        if (event.optString("type") == "message" &&
                                            event.optString("recipientId") == signedInUserId &&
                                            messages.none { it.optString("id") == event.optString("id") }) {
                                            messages = messages + event
                                        }
                                    } catch (_: Exception) { }
                                }
                            }
                            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                                liveSocket = null
                            }
                            override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                                liveSocket = null
                            }
                        })
                    }
                    if (partnerId.isBlank()) {
                        messages = emptyList()
                        loadingMessages = false
                    } else {
                        CloudflareApi.fetchMessages(sessionToken, partnerId) { messageResult ->
                            runOnUiThread {
                                messageResult.onSuccess { result ->
                                    val array = result.optJSONArray("messages") ?: JSONArray()
                                    messages = (0 until array.length()).mapNotNull { array.optJSONObject(it) }
                                }.onFailure { error = it.message ?: "Could not load messages." }
                                loadingMessages = false
                            }
                        }
                    }
                }.onFailure {
                    error = it.message ?: "Could not find the other authorised user."
                    loadingMessages = false
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        liveSocket?.close(1000, "App backgrounded")
        liveSocket = null
        if (sessionToken.isNotBlank() && page != AppPage.LOGIN) {
            page = if (SecureVault.get(this, "pin_verifier").isNullOrBlank()) AppPage.SET_PIN else AppPage.UNLOCK
        }
    }
}

@Composable
private fun ConversationScreen(
    username: String,
    currentUserId: String,
    partner: String,
    messages: List<JSONObject>,
    loading: Boolean,
    onRefresh: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSignOut: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var openedChat by remember { mutableStateOf(false) }
    val lastMessage = messages.lastOrNull()
    val preview = lastMessage?.let {
        if (it.optString("nonce") == "plain-v1") {
            try { String(android.util.Base64.decode(it.optString("ciphertext"), android.util.Base64.NO_WRAP), Charsets.UTF_8) }
            catch (_: Exception) { "Message" }
        } else "Attachment"
    } ?: "Tap to start chatting"

    if (!openedChat) {
        Column(Modifier.fillMaxSize().background(Color(0xFFF5F7FA))) {
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF527DA3)).padding(horizontal = 18.dp, vertical = 17.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("☰", color = White, fontSize = 24.sp, modifier = Modifier.padding(end = 22.dp))
                Text("CBSE Edu Desk", color = White, fontSize = 21.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text("⌕", color = White, fontSize = 28.sp)
            }
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF527DA3)).padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Text("CHATS", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("CONTACTS", color = Color(0xFFD7E5F0), fontSize = 13.sp)
            }
            if (partner.isBlank()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (loading) CircularProgressIndicator(color = Color(0xFF527DA3))
                        else Text("Your authorised chat is not available.", color = Muted)
                        TextButton(onClick = onRefresh) { Text("Retry") }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().clickable { openedChat = true }.padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(54.dp).background(Color(0xFF70A7D2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(partner.take(1).uppercase(), color = White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(partner, color = Color(0xFF202A33), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(preview, color = Color(0xFF78838D), fontSize = 14.sp, maxLines = 1)
                    }
                    Text("now", color = Color(0xFF89939D), fontSize = 11.sp)
                }
                HorizontalDivider(color = Color(0xFFE8EBEF), thickness = 0.7.dp)
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth().background(White).padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text("Chats", color = Color(0xFF527DA3), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Calls", color = Muted, fontSize = 12.sp)
                    Text("Settings", color = Muted, fontSize = 12.sp)
                }
            }
            Row(Modifier.fillMaxWidth().background(White).padding(horizontal = 14.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Signed in as $username", color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = onSignOut) { Text("Sign out", color = Muted, fontSize = 11.sp) }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().background(Color(0xFFE7EBEF))) {
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF527DA3)).padding(horizontal = 10.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("‹", color = White, fontSize = 32.sp, modifier = Modifier.clickable { openedChat = false }.padding(horizontal = 9.dp))
                Box(Modifier.size(42.dp).background(Color(0xFF70A7D2), CircleShape), contentAlignment = Alignment.Center) {
                    Text(partner.take(1).uppercase(), color = White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(partner, color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("last seen recently", color = Color(0xFFDCE7EF), fontSize = 11.sp)
                }
                Text("⋮", color = White, fontSize = 25.sp, modifier = Modifier.clickable { onRefresh() }.padding(horizontal = 8.dp))
            }
            if (messages.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(color = Color(0xFFEAF2F8), shape = RoundedCornerShape(12.dp)) {
                        Text(if (loading) "Loading messages…" else "This is the beginning of your private chat.", color = Color(0xFF607383), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(messages, key = { it.optString("id") }) { message ->
                        val mine = message.optString("senderId") == currentUserId
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                            Surface(
                                color = if (mine) Color(0xFFE2FFC7) else White,
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 1.dp,
                                modifier = Modifier.widthIn(max = 310.dp)
                            ) {
                                Column(Modifier.padding(horizontal = 11.dp, vertical = 7.dp)) {
                                    val content = if (message.optString("nonce") == "plain-v1") {
                                        try { String(android.util.Base64.decode(message.optString("ciphertext"), android.util.Base64.NO_WRAP), Charsets.UTF_8) }
                                        catch (_: Exception) { "[Message could not be displayed]" }
                                    } else "[Attachment]"
                                    Text(content, color = Color(0xFF202A33), fontSize = 15.sp)
                                    Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                                        Text(message.optString("createdAt").take(16).replace("T", " "), color = Color(0xFF82908A), fontSize = 9.sp)
                                        if (mine) Text("  ✓✓", color = Color(0xFF55A6C7), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Surface(color = Color(0xFFF5F5F5), shadowElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("＋", color = Color(0xFF71808C), fontSize = 26.sp, modifier = Modifier.padding(horizontal = 5.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message") },
                        singleLine = false,
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (draft.isNotBlank() && !sending) {
                                sending = true
                                onSendMessage(draft.trim())
                                draft = ""
                                sending = false
                            }
                        })
                    )
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (draft.isNotBlank() && !sending) {
                                sending = true
                                onSendMessage(draft.trim())
                                draft = ""
                                sending = false
                            }
                        },
                        enabled = draft.isNotBlank() && !sending,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF527DA3)),
                        shape = CircleShape
                    ) { Text("➤", color = White, fontSize = 17.sp) }
                }
            }
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
