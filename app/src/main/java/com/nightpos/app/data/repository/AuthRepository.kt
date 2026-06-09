package com.nightpos.app.data.repository

import com.google.gson.JsonArray
import com.nightpos.app.data.api.OdooRpcClient
import com.nightpos.app.data.model.PosEmployee

sealed class PinResult {
    data class Success(val employee: PosEmployee) : PinResult()
    data class InvalidPin(val message: String = "Invalid PIN") : PinResult()
    data class Error(val message: String) : PinResult()
}

class AuthRepository(private val client: OdooRpcClient) {

    private var cachedBaseUrl = ""
    private var cachedDb = ""
    private var cachedUid: Int = 0
    private var cachedPassword = ""
    var lastEmployee: PosEmployee? = null
        private set

    suspend fun authenticate(baseUrl: String, db: String, login: String, password: String): Boolean {
        val uid = client.authenticate(baseUrl, db, login, password) ?: return false
        cachedBaseUrl = baseUrl
        cachedDb = db
        cachedUid = uid
        cachedPassword = password
        return true
    }

    fun isAuthenticated() = cachedUid > 0

    fun makeRepository() = OdooRepository(client, cachedBaseUrl, cachedDb, cachedUid, cachedPassword)

    suspend fun verifyPin(pin: String): PinResult {
        if (!isAuthenticated()) return PinResult.Error("Not connected to Odoo")
        return try {
            val result = client.executeKw(
                cachedBaseUrl, cachedDb, cachedUid, cachedPassword,
                model = "hr.employee",
                method = "search_read",
                args = listOf(listOf(listOf("pin", "=", pin))),
                kwargs = mapOf(
                    "fields" to listOf("name", "pin", "user_id", "job_title"),
                    "limit" to 1,
                ),
            )
            val arr = result as? JsonArray
            if (arr == null || arr.size() == 0) {
                PinResult.InvalidPin()
            } else {
                val emp = arr[0].asJsonObject
                val id = emp.get("id")?.asInt ?: 0
                val name = emp.get("name")?.asString ?: ""
                val userIdEl = emp.get("user_id")
                val userId = when {
                    userIdEl == null || userIdEl.isJsonNull -> null
                    userIdEl.isJsonArray -> userIdEl.asJsonArray.getOrNull(0)?.asInt
                    else -> userIdEl.asInt
                }
                val jobTitle = emp.get("job_title")?.takeIf { !it.isJsonNull }?.asString ?: ""
                val isManager = jobTitle.contains("manager", ignoreCase = true) ||
                        jobTitle.contains("admin", ignoreCase = true) ||
                        checkIsManager(userId)
                val employee = PosEmployee(id, name, pin, userId, isManager)
                lastEmployee = employee
                PinResult.Success(employee)
            }
        } catch (e: Exception) {
            PinResult.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun checkIsManager(userId: Int?): Boolean {
        if (userId == null) return false
        return try {
            val result = client.executeKw(
                cachedBaseUrl, cachedDb, cachedUid, cachedPassword,
                model = "res.users",
                method = "has_group",
                args = listOf(userId, "point_of_sale.group_pos_manager"),
            )
            result?.asBoolean ?: false
        } catch (_: Exception) { false }
    }
}
