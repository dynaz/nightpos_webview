package com.nightpos.app.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

private const val TAG = "OdooRpcClient"

class OdooRpcClient {

    private val gson = Gson()
    private val cookies = CopyOnWriteArrayList<Cookie>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, newCookies: List<Cookie>) {
            cookies.removeIf { existing -> newCookies.any { it.name == existing.name } }
            cookies.addAll(newCookies)
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> = cookies.toList()
    }

    private val httpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor { Log.d(TAG, it) }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val json = "application/json; charset=utf-8".toMediaType()

    fun clearSession() { cookies.clear() }

    /** Authenticate via /web/session/authenticate — sets session cookie. Returns uid on success. */
    suspend fun authenticate(baseUrl: String, db: String, login: String, password: String): Int? {
        val body = buildJsonRpc(mapOf(
            "db" to db,
            "login" to login,
            "password" to password,
        ))
        return try {
            val resp = post("$baseUrl/web/session/authenticate", body)
            val result = resp?.getAsJsonObject("result")
            val uid = result?.get("uid")?.let {
                if (it.isJsonNull) null else it.asInt
            }
            uid
        } catch (e: Exception) {
            Log.e(TAG, "authenticate failed", e)
            null
        }
    }

    /** Call execute_kw on a model. Returns the raw JsonElement result. */
    suspend fun executeKw(
        baseUrl: String,
        db: String,
        uid: Int,
        password: String,
        model: String,
        method: String,
        args: List<Any> = emptyList(),
        kwargs: Map<String, Any> = emptyMap(),
    ): JsonElement? {
        val body = buildJsonRpc(mapOf(
            "service" to "object",
            "method" to "execute_kw",
            "args" to listOf(db, uid, password, model, method, args, kwargs),
        ))
        return try {
            post("$baseUrl/jsonrpc", body)?.get("result")
        } catch (e: Exception) {
            Log.e(TAG, "executeKw $model.$method failed", e)
            null
        }
    }

    private fun buildJsonRpc(params: Map<String, Any>): String = gson.toJson(mapOf(
        "jsonrpc" to "2.0",
        "method" to "call",
        "id" to 1,
        "params" to params,
    ))

    private fun post(url: String, bodyJson: String): JsonObject? {
        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody(json))
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val text = response.body?.string() ?: return null
            return gson.fromJson(text, JsonObject::class.java)
        }
    }
}
