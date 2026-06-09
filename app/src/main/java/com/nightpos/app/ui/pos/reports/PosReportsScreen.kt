package com.nightpos.app.ui.pos.reports

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ReportItem(val nameEn: String, val nameTh: String, val path: String)
private data class ReportSection(val titleEn: String, val titleTh: String, val items: List<ReportItem>)

private val REPORT_SECTIONS = listOf(
    ReportSection("Sales Reports", "รายงานการขาย", listOf(
        ReportItem("Sales Summary", "สรุปยอดขาย", "/odoo/point-of-sale/reporting/sales"),
        ReportItem("Payment Method Report", "รายงานวิธีการชำระเงิน", "/odoo/point-of-sale/reporting"),
        ReportItem("Sales by User Session", "ยอดขายแบ่งตามรอบผู้ใช้งาน", "/odoo/point-of-sale/reporting/sessions"),
        ReportItem("Sales by Member", "ยอดขายตามสมาชิก", "/odoo/point-of-sale/reporting/customers"),
    )),
    ReportSection("Transaction Reports", "รายงานรายการ", listOf(
        ReportItem("Sales by Category", "ยอดขายแบ่งตามประเภท", "/odoo/point-of-sale/reporting/products"),
        ReportItem("Best Sellers", "รายการขายดี", "/odoo/point-of-sale/reporting/products"),
        ReportItem("Sub-menu Items", "เมนูย่อย", "/odoo/point-of-sale/reporting/products"),
    )),
    ReportSection("Discount Reports", "รายงานส่วนลด", listOf(
        ReportItem("Discount Report", "รายงานส่วนลด", "/odoo/point-of-sale/reporting"),
    )),
    ReportSection("Cash Drawer Reports", "รายงานรอบลั้นชักเงินสด", listOf(
        ReportItem("Cash Drawer Session", "รายงานรอบลั้นชักเงินสด", "/odoo/point-of-sale/reporting/sessions"),
    )),
    ReportSection("Sensitive Details", "รายการละเอียดอ่อน", listOf(
        ReportItem("Refund Amount", "จำนวนเงินคืน", "/odoo/point-of-sale/reporting"),
        ReportItem("Cancelled Orders", "รายการที่ถูกยกเลิก", "/odoo/point-of-sale/reporting"),
        ReportItem("Deleted Orders", "รายการที่ถูกลบ", "/odoo/point-of-sale/reporting"),
        ReportItem("Deleted Bills", "บิลที่ถูกลบ", "/odoo/point-of-sale/reporting"),
    )),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosReportsScreen(baseUrl: String) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = Color(0xFFF5F5F5),
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            REPORT_SECTIONS.forEach { section ->
                item {
                    Text(
                        section.titleTh,
                        fontSize = 12.sp,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                items(section.items) { report ->
                    Surface(
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val url = "$baseUrl${report.path}"
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            },
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(report.nameTh, Modifier.weight(1f), fontSize = 14.sp)
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null,
                                modifier = Modifier.size(16.dp), tint = Color(0xFFBDBDBD))
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}
