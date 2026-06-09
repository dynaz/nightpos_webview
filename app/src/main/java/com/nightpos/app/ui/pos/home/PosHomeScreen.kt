package com.nightpos.app.ui.pos.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightpos.app.data.model.BestSellerItem
import com.nightpos.app.data.model.TopCategoryItem
import java.text.NumberFormat
import java.util.Locale

private val PosBackground = Color(0xFFF5F5F5)
private val CardBackground = Color.White
private val HeaderGray = Color(0xFF616161)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentBlue = Color(0xFF2196F3)
private val AccentOrange = Color(0xFFFF9800)
private val AccentRed = Color(0xFFF44336)

private fun formatBaht(amount: Double): String {
    val fmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2; minimumFractionDigits = 2 }
    return "฿${fmt.format(amount)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosHomeScreen(viewModel: PosHomeViewModel, shopName: String) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBackground),
            )
        },
        containerColor = PosBackground,
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (state.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::refresh) { Text("Retry") }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (shopName.isNotBlank()) {
                Text(shopName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = HeaderGray)
            }

            // Current Bills
            PosCard(title = "Current Bills") {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatBaht(state.currentBills.totalCollectable), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("Total Collectable", fontSize = 12.sp, color = HeaderGray)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    MetricBox("Dine In", state.currentBills.dineInCount.toString(), Modifier.weight(1f))
                    VerticalDivider()
                    MetricBox("Take Away", state.currentBills.takeAwayCount.toString(), Modifier.weight(1f))
                }
            }

            // Sales Summary
            PosCard(title = "Sales Summary") {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatBaht(state.salesSummary.totalCollected), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("Total Amount Collected", fontSize = 12.sp, color = HeaderGray)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    MetricBox("Transactions", state.salesSummary.transactionCount.toString(), Modifier.weight(1f))
                    VerticalDivider()
                    MetricBox("Average", formatBaht(state.salesSummary.averagePerTransaction), Modifier.weight(1f))
                }
            }

            // Top Category
            if (state.topCategories.isNotEmpty()) {
                PosCard(title = "Top Category", subtitle = "Past 7 Days") {
                    val colors = listOf(AccentOrange, AccentBlue, AccentGreen)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        state.topCategories.take(3).forEachIndexed { i, cat ->
                            CategoryBar(cat, colors.getOrElse(i) { AccentBlue}, Modifier.weight(1f))
                        }
                    }
                }
            }

            // Best Seller
            if (state.bestSellers.isNotEmpty()) {
                PosCard(title = "Best Seller", subtitle = "Past 7 Days") {
                    state.bestSellers.forEach { item ->
                        BestSellerRow(item)
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PosCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = HeaderGray)
                if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = Color(0xFF9E9E9E))
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun MetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        Text(label, fontSize = 11.sp, color = HeaderGray)
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(Color(0xFFE0E0E0))
    )
}

@Composable
private fun CategoryBar(item: TopCategoryItem, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("%.2f%%".format(item.percentage), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        Text(item.name.take(12), fontSize = 10.sp, color = HeaderGray, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { item.percentage / 100f },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
        )
    }
}

@Composable
private fun BestSellerRow(item: BestSellerItem) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(item.name, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text("%.0f".format(item.qty), modifier = Modifier.width(60.dp), fontSize = 13.sp, color = HeaderGray)
        Text(formatBaht(item.amount), fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
