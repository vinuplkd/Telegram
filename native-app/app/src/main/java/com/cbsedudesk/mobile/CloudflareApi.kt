package com.cbsedudesk.mobile

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONObject

/**
 * Thin native Android client for the CBSE Edu Desk Cloudflare Worker API.
 * The API URL is injected through BuildConfig at build time.
 *
 * Message plaintext must be encrypted on the device before calling sendEncryptedMessage.
 * Never pass the four-digit app-lock PIN to this API.
 */
object CloudflareApi {
    private val http = OkHttpClient()
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val baseUrl get() = BuildConfig.API_BASE_URL.trimEnd('/')

    fun login(username: String, password: String, callback: (Result<JSONObject>) -> Unit) {
        val payload = JSONObject()
            .put("username", username)
            .put("password", password)
        post("/v1/auth/login", payload, null, callback)
    }

    fun fetchPartner(token: String, callback: (Result<JSONObject>) -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl/v1/contacts")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        execute(request, callback)
    }

    fun fetchMessages(token: String, otherUserId: String, callback: (Result<JSONObject>) -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl/v1/messages?with=$otherUserId")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        execute(request, callback)
    }

    fun sendEncryptedMessage(
        token: String,
        recipientId: String,
        ciphertext: String,
        nonce: String,
        callback: (Result<JSONObject>) -> Unit
    ) {
        val payload = JSONObject()
            .put("recipientId", recipientId)
            .put("ciphertext", ciphertext)
            .put("nonce", nonce)
        post("/v1/messages", payload, token, callback)
    }

    fun uploadEncryptedAttachment(
        token: String,
        recipientId: String,
        encryptedBytes: ByteArray,
        callback: (Result<JSONObject>) -> Unit
    ) {
        val request = Request.Builder()
            .url("$baseUrl/v1/media")
            .header("Authorization", "Bearer $token")
            .header("X-Recipient-Id", recipientId)
            .header("Content-Type", "application/octet-stream")
            .post(encryptedBytes.toRequestBody("application/octet-stream".toMediaType()))
            .build()
        execute(request, callback)
    }

    fun downloadEncryptedAttachment(
        token: String,
        mediaId: String,
        callback: (Result<ByteArray>) -> Unit
    ) {
        val request = Request.Builder()
            .url("$baseUrl/v1/media/$mediaId")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        http.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(Result.failure(e))
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(Result.failure(IOException("HTTP ${it.code}")))
                    } else {
                        callback(Result.success(it.body?.bytes() ?: byteArrayOf()))
                    }
                }
            }
        })
    }

    fun connectLive(token: String, listener: okhttp3.WebSocketListener): okhttp3.WebSocket {
        val liveUrl = baseUrl.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "/v1/live"
        val request = Request.Builder()
            .url(liveUrl)
            .header("Authorization", "Bearer $token")
            .build()
        return http.newWebSocket(request, listener)
    }

    private fun post(
        path: String,
        payload: JSONObject,
        token: String?,
        callback: (Result<JSONObject>) -> Unit
    ) {
        val builder = Request.Builder()
            .url(baseUrl + path)
            .post(payload.toString().toRequestBody(jsonType))
        if (token != null) builder.header("Authorization", "Bearer $token")
        execute(builder.build(), callback)
    }

    private fun execute(request: Request, callback: (Result<JSONObject>) -> Unit) {
        http.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(Result.failure(e))
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val text = it.body?.string().orEmpty()
                    if (!it.isSuccessful) {
                        callback(Result.failure(IOException("HTTP ${it.code}: $text")))
                    } else {
                        try {
                            callback(Result.success(JSONObject(text)))
                        } catch (e: Exception) {
                            callback(Result.failure(e))
                        }
                    }
                }
            }
        })
    }
}
