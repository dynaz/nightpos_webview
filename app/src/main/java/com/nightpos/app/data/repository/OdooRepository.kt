package com.nightpos.app.data.repository

import com.google.gson.JsonArray
import com.nightpos.app.data.api.OdooRpcClient
import com.nightpos.app.data.model.BestSellerItem
import com.nightpos.app.data.model.CurrentBillsData
import com.nightpos.app.data.model.PosOrderItem
import com.nightpos.app.data.model.SalesSummaryData
import com.nightpos.app.data.model.TopCategoryItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class OdooRepository(
    private val client: OdooRpcClient,
    private val baseUrl: String,
    private val db: String,
    private val uid: Int,
    private val password: String,
) {

    private val utcFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val displayFmt = SimpleDateFormat("HH:mm", Locale.US)

    private fun todayStart(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return utcFmt.format(cal.time)
    }

    private fun sevenDaysAgo(): String {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        return utcFmt.format(cal.time)
    }

    suspend fun fetchCurrentBills(): CurrentBillsData {
        val result = client.executeKw(
            baseUrl, db, uid, password,
            model = "pos.order",
            method = "search_read",
            args = listOf(listOf(
                listOf("state", "in", listOf("draft", "new")),
                listOf("date_order", ">=", todayStart()),
            )),
            kwargs = mapOf(
                "fields" to listOf("name", "amount_total", "lines"),
                "limit" to 500,
            ),
        )
        val orders = result as? JsonArray ?: return CurrentBillsData(0.0, 0, 0)
        val total = orders.sumOf { it.asJsonObject.get("amount_total")?.asDouble ?: 0.0 }
        return CurrentBillsData(
            totalCollectable = total,
            dineInCount = orders.size(),
            takeAwayCount = 0,
        )
    }

    suspend fun fetchSalesSummary(): SalesSummaryData {
        val result = client.executeKw(
            baseUrl, db, uid, password,
            model = "pos.order",
            method = "search_read",
            args = listOf(listOf(
                listOf("state", "in", listOf("paid", "done", "invoiced")),
                listOf("date_order", ">=", todayStart()),
            )),
            kwargs = mapOf(
                "fields" to listOf("amount_total", "date_order"),
                "limit" to 1000,
            ),
        )
        val orders = result as? JsonArray ?: return SalesSummaryData(0.0, 0, 0.0, "")
        val total = orders.sumOf { it.asJsonObject.get("amount_total")?.asDouble ?: 0.0 }
        val count = orders.size()
        val avg = if (count > 0) total / count else 0.0
        val from = orders.firstOrNull()
            ?.asJsonObject?.get("date_order")?.asString
            ?.let { runCatching { displayFmt.format(utcFmt.parse(it) ?: Date()) }.getOrElse { it } }
            ?: ""
        return SalesSummaryData(total, count, avg, from)
    }

    suspend fun fetchTopCategories(): List<TopCategoryItem> {
        val result = client.executeKw(
            baseUrl, db, uid, password,
            model = "pos.order.line",
            method = "read_group",
            args = listOf(
                listOf(listOf("order_id.date_order", ">=", sevenDaysAgo())),
                listOf("price_subtotal_incl", "product_id"),
                listOf("product_id.categ_id"),
            ),
            kwargs = mapOf("lazy" to false),
        )
        val groups = result as? JsonArray ?: return emptyList()
        val items = groups.mapNotNull { g ->
            val obj = g.asJsonObject
            val nameEl = obj.get("product_id.categ_id")
            val name = when {
                nameEl == null || nameEl.isJsonNull -> "No Category"
                nameEl.isJsonArray -> nameEl.asJsonArray.getOrNull(1)?.asString ?: "No Category"
                else -> nameEl.asString
            }
            val amount = obj.get("price_subtotal_incl")?.asDouble ?: 0.0
            TopCategoryItem(name, amount, 0f)
        }.sortedByDescending { it.amount }
        val grandTotal = items.sumOf { it.amount }
        return items.take(3).map { it.copy(percentage = if (grandTotal > 0) (it.amount / grandTotal * 100).toFloat() else 0f) }
    }

    suspend fun fetchBestSellers(): List<BestSellerItem> {
        val result = client.executeKw(
            baseUrl, db, uid, password,
            model = "pos.order.line",
            method = "read_group",
            args = listOf(
                listOf(listOf("order_id.date_order", ">=", sevenDaysAgo())),
                listOf("qty", "price_subtotal_incl", "product_id"),
                listOf("product_id"),
            ),
            kwargs = mapOf("orderby" to "qty desc", "limit" to 5),
        )
        val groups = result as? JsonArray ?: return emptyList()
        return groups.mapNotNull { g ->
            val obj = g.asJsonObject
            val nameEl = obj.get("product_id")
            val name = when {
                nameEl == null || nameEl.isJsonNull -> return@mapNotNull null
                nameEl.isJsonArray -> nameEl.asJsonArray.getOrNull(1)?.asString ?: return@mapNotNull null
                else -> nameEl.asString
            }
            val qty = obj.get("qty")?.asDouble ?: 0.0
            val amount = obj.get("price_subtotal_incl")?.asDouble ?: 0.0
            BestSellerItem(name, qty, amount)
        }
    }

    suspend fun fetchActivity(limit: Int = 100, searchQuery: String = ""): List<PosOrderItem> {
        val domain = mutableListOf<Any>(
            listOf("state", "in", listOf("paid", "done", "invoiced")),
        )
        if (searchQuery.isNotBlank()) {
            domain.add(listOf("name", "ilike", searchQuery))
        }
        val result = client.executeKw(
            baseUrl, db, uid, password,
            model = "pos.order",
            method = "search_read",
            args = listOf(domain),
            kwargs = mapOf(
                "fields" to listOf("name", "amount_total", "date_order", "payment_ids", "state"),
                "limit" to limit,
                "order" to "date_order desc",
            ),
        )
        val orders = result as? JsonArray ?: return emptyList()
        return orders.mapNotNull { el ->
            val obj = el.asJsonObject
            val id = obj.get("id")?.asInt ?: return@mapNotNull null
            val name = obj.get("name")?.asString ?: ""
            val amount = obj.get("amount_total")?.asDouble ?: 0.0
            val dateRaw = obj.get("date_order")?.asString ?: ""
            val dateDisplay = runCatching { utcFmt.parse(dateRaw)?.let { displayFmt.format(it) } ?: dateRaw }.getOrElse { dateRaw }
            PosOrderItem(id, name, amount, dateDisplay, "", "done")
        }
    }
}
