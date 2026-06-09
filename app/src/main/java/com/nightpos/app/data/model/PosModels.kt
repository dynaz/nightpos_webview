package com.nightpos.app.data.model

data class PosEmployee(
    val id: Int,
    val name: String,
    val pin: String?,
    val userId: Int?,
    val isManager: Boolean,
)

data class CurrentBillsData(
    val totalCollectable: Double,
    val dineInCount: Int,
    val takeAwayCount: Int,
)

data class SalesSummaryData(
    val totalCollected: Double,
    val transactionCount: Int,
    val averagePerTransaction: Double,
    val fromTime: String,
)

data class TopCategoryItem(
    val name: String,
    val amount: Double,
    val percentage: Float,
)

data class BestSellerItem(
    val name: String,
    val qty: Double,
    val amount: Double,
)

data class PosOrderItem(
    val id: Int,
    val name: String,
    val amountTotal: Double,
    val dateOrder: String,
    val paymentMethod: String,
    val state: String,
)

data class PosHomeState(
    val shopName: String = "",
    val currentBills: CurrentBillsData = CurrentBillsData(0.0, 0, 0),
    val salesSummary: SalesSummaryData = SalesSummaryData(0.0, 0, 0.0, ""),
    val topCategories: List<TopCategoryItem> = emptyList(),
    val bestSellers: List<BestSellerItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class PosActivityState(
    val orders: List<PosOrderItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
)
