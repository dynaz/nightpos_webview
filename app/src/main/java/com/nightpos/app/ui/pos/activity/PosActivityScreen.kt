package com.nightpos.app.ui.pos.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightpos.app.data.model.PosOrderItem
import java.text.NumberFormat
import java.util.Locale

private val CardBackground = Color.White
private val PosBackground = Color(0xFFF5F5F5)
private val HeaderGray = Color(0xFF616161)

private fun formatBaht(amount: Double): String {
    val fmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2; minimumFractionDigits = 2 }
    return "฿${fmt.format(amount)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosActivityScreen(viewModel: PosActivityViewModel) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBackground),
            )
        },
        containerColor = PosBackground,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchChange,
                placeholder = { Text("Search by Reference Number", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                ),
                singleLine = true,
            )

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                        Button(onClick = viewModel::refresh) { Text("Retry") }
                    }
                }
                state.orders.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found", color = HeaderGray)
                }
                else -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    items(state.orders, key = { it.id }) { order ->
                        ActivityRow(order)
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(order: PosOrderItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE3F2FD)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(formatBaht(order.amountTotal), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(order.name, fontSize = 12.sp, color = HeaderGray)
        }
        if (order.paymentMethod.isNotBlank()) {
            Text(
                order.paymentMethod,
                fontSize = 11.sp,
                color = Color(0xFF1565C0),
                modifier = Modifier
                    .background(Color(0xFFE3F2FD), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(order.dateOrder, fontSize = 12.sp, color = HeaderGray)
    }
}
