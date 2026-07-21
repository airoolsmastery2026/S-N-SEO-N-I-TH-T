package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lead
import com.example.ui.theme.*

@Composable
fun AnalyticsScreen(
    savedLeads: List<Lead>,
    allLeads: List<Lead>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Calculated Statistics
    val totalScanned = allLeads.size
    val totalSaved = savedLeads.size
    val activeLeadsCount = savedLeads.count { it.status == "Mới" || it.status == "Đang tư vấn" }
    val closedLeadsCount = savedLeads.count { it.status == "Đã chốt" }
    
    val conversionRate = if (totalSaved > 0) {
        (closedLeadsCount.toFloat() / totalSaved.toFloat() * 100).toInt()
    } else 0

    val furnitureCount = savedLeads.count { it.category == "Nội thất" }
    val mechanicalCount = savedLeads.count { it.category == "Cơ khí" }

    // Area-wise Distribution
    val areaCounts = remember(savedLeads) {
        val distribution = mutableMapOf(
            "Thủ Đức" to 0,
            "Quận 12" to 0,
            "Bình Dương" to 0,
            "Đồng Nai" to 0,
            "Gò Vấp" to 0,
            "Khác" to 0
        )
        savedLeads.forEach { lead ->
            val loc = lead.location.lowercase()
            when {
                loc.contains("thủ đức") -> distribution["Thủ Đức"] = distribution["Thủ Đức"]!! + 1
                loc.contains("quận 12") || loc.contains("q12") -> distribution["Quận 12"] = distribution["Quận 12"]!! + 1
                loc.contains("bình dương") || loc.contains("dĩ an") || loc.contains("thuận an") -> distribution["Bình Dương"] = distribution["Bình Dương"]!! + 1
                loc.contains("đồng nai") || loc.contains("biên hòa") -> distribution["Đồng Nai"] = distribution["Đồng Nai"]!! + 1
                loc.contains("gò vấp") -> distribution["Gò Vấp"] = distribution["Gò Vấp"]!! + 1
                else -> distribution["Khác"] = distribution["Khác"]!! + 1
            }
        }
        distribution.toList().sortedByDescending { it.second }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // KPI Dashboard Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                title = "Đã Quét (Social)",
                value = "$totalScanned",
                icon = Icons.Default.Troubleshoot,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Lead Đã Lưu (CRM)",
                value = "$totalSaved",
                icon = Icons.Default.BookmarkBorder,
                tint = MechCategory,
                modifier = Modifier.weight(1f)
            )
        }

        // KPI Dashboard Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                title = "Đang Tư Vấn",
                value = "$activeLeadsCount",
                icon = Icons.Default.ContactSupport,
                tint = FurnCategory,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Tỉ Lệ Chốt Hợp Đồng",
                value = "$conversionRate%",
                icon = Icons.Default.Percent,
                tint = WarmGold,
                modifier = Modifier.weight(1f)
            )
        }

        // Section: Industry Donut Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tỷ lệ lĩnh vực nhu cầu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (totalSaved == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Lưu thêm Lead để hiển thị biểu đồ phân tích",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Donut Canvas
                        IndustryDonutChart(
                            furnitureCount = furnitureCount,
                            mechanicalCount = mechanicalCount,
                            modifier = Modifier
                                .size(120.dp)
                                .padding(8.dp)
                        )

                        Spacer(modifier = Modifier.width(24.dp))

                        // Legends
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            LegendItem(
                                color = FurnCategory,
                                label = "Nội Thất",
                                count = furnitureCount,
                                percentage = (furnitureCount.toFloat() / totalSaved * 100).toInt()
                            )
                            LegendItem(
                                color = MechCategory,
                                label = "Cơ Khí",
                                count = mechanicalCount,
                                percentage = (mechanicalCount.toFloat() / totalSaved * 100).toInt()
                            )
                        }
                    }
                }
            }
        }

        // Section: Area Distribution Bar Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Phân bổ khách hàng theo khu vực",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (totalSaved == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Chưa có dữ liệu phân tích khu vực",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    AreaBarChart(
                        data = areaCounts,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }

        // Sales Pipeline Funnel Overview
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Báo cáo hiệu suất Sales (Phễu Chốt)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Pipeline stages
                val crmNew = savedLeads.count { it.status == "Mới" }
                val crmInTalk = savedLeads.count { it.status == "Đang tư vấn" }
                val crmWon = savedLeads.count { it.status == "Đã chốt" }
                val crmLost = savedLeads.count { it.status == "Hủy" }

                PipelineRow(title = "1. Lead mới", count = crmNew, ratio = 1.0f, color = GlowingCopper)
                Spacer(modifier = Modifier.height(10.dp))
                PipelineRow(title = "2. Đang liên hệ / Tư vấn", count = crmInTalk, ratio = if (totalSaved > 0) (crmInTalk + crmWon + crmLost).toFloat() / totalSaved else 0f, color = FurnCategory)
                Spacer(modifier = Modifier.height(10.dp))
                PipelineRow(title = "3. Đã ký hợp đồng chốt", count = crmWon, ratio = if (totalSaved > 0) crmWon.toFloat() / totalSaved else 0f, color = MechCategory)
            }
        }

        // ==================== MODULE 5: AUTOMATED REPORTING DIGEST ====================
        var showDigestDialog by remember { mutableStateOf(false) }
        val totalScannedToday = allLeads.size
        val totalConsultedToday = savedLeads.count { it.estimatedPrice != null }
        val totalRepliedToday = savedLeads.count { it.customerReplyText != null }
        val totalApptsToday = savedLeads.count { it.appointmentDate != null }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = "Notification",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Module 5: Báo cáo tự động (Daily Digest)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "BOT VẬN HÀNH",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Bản tin tổng hợp tình hình kinh doanh của xưởng được tổng hợp tự động lúc 21:00 mỗi tối và bắn thẳng về nhóm chat Telegram của Ban quản lý & Chủ xưởng:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Telegram Bubble Preview Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.SendAndArchive,
                            contentDescription = "Telegram",
                            tint = Color(0xFF1877F2),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Telegram Bot • 21:00 (Hôm nay)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = """
                            📊 BÁO CÁO KẾT QUẢ VẬN HÀNH XƯỞNG TỰ ĐỘNG
                            📅 Ngày báo cáo: 21/07/2026 (Realtime)
                            ───────────────────
                            🔍 1. Tổng số Lead quét được trong ngày: $totalScannedToday
                            🤖 2. Số khách đã được AI tư vấn báo giá tự động: $totalConsultedToday
                            💬 3. Số khách phản hồi đồng ý gặp/chốt lịch khảo sát: $totalRepliedToday / $totalApptsToday
                            📈 4. Tỷ lệ chuyển đổi hôm nay: $conversionRate%
                            
                            👉 Ghi chú: Hệ thống Multi-Agent đang vận hành mượt mà 24/7. Thợ kỹ thuật đã nhận đầy đủ lịch khảo sát đo đạc qua Telegram giao việc riêng.
                        """.trimIndent(),
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { showDigestDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Bắn thử báo cáo",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Gửi báo cáo cuối ngày (Simulate Telegram Digest)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Elegant M3 Dialog to display the simulation feedback
        if (showDigestDialog) {
            AlertDialog(
                onDismissRequest = { showDigestDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bắn báo cáo Telegram thành công", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        text = "Hệ thống Automation Engine đã biên soạn thành công bản tin tổng hợp cuối ngày (Daily Digest) và truyền tải thành công tới Telegram Chat ID của Ban Quản Trị xưởng:\n\n" +
                               "• Người nhận: Chủ xưởng & Đội trưởng Kỹ thuật\n" +
                               "• Trạng thái: Đã gửi (200 OK)\n" +
                               "• Nội dung: Bao gồm kết quả quét, tỷ lệ chuyển đổi và cập nhật lịch hẹn khảo sát thực tế.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showDigestDialog = false }) {
                        Text("Đóng", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .background(tint.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(6.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun IndustryDonutChart(
    furnitureCount: Int,
    mechanicalCount: Int,
    modifier: Modifier = Modifier
) {
    val total = (furnitureCount + mechanicalCount).toFloat()
    val furnAngle = if (total > 0f) (furnitureCount.toFloat() / total) * 360f else 180f
    val mechAngle = if (total > 0f) (mechanicalCount.toFloat() / total) * 360f else 180f

    Canvas(modifier = modifier) {
        val size = size
        val strokeWidth = 14.dp.toPx()
        val radiusSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val offset = Offset(strokeWidth / 2, strokeWidth / 2)

        if (total == 0f) {
            drawArc(
                color = Color.Gray.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = offset,
                size = radiusSize,
                style = Stroke(width = strokeWidth)
            )
        } else {
            // Furniture Arc (Blue)
            drawArc(
                color = FurnCategory,
                startAngle = -90f,
                sweepAngle = furnAngle,
                useCenter = false,
                topLeft = offset,
                size = radiusSize,
                style = Stroke(width = strokeWidth)
            )

            // Mechanical Arc (Green)
            drawArc(
                color = MechCategory,
                startAngle = -90f + furnAngle,
                sweepAngle = mechAngle,
                useCenter = false,
                topLeft = offset,
                size = radiusSize,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String,
    count: Int,
    percentage: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count đơn ($percentage%)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AreaBarChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val maxCount = remember(data) { data.map { it.second }.maxOrNull() ?: 1 }
    val barColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val barWidth = 32.dp.toPx()
        val space = (width - (barWidth * data.size)) / (data.size + 1)

        data.forEachIndexed { index, pair ->
            val count = pair.second
            // Scale bar height to leave some space at the top for labels
            val scaleRatio = if (maxCount > 0) count.toFloat() / maxCount else 0f
            val barHeight = height * scaleRatio * 0.7f

            val x = space + index * (barWidth + space)
            val y = height - barHeight - 24.dp.toPx() // Padding for area label at bottom

            // Draw shadow bar
            drawRoundRect(
                color = Color.Gray.copy(alpha = 0.08f),
                topLeft = Offset(x, 0f),
                size = Size(barWidth, height - 24.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
            )

            // Draw active bar with a subtle top-down gradient
            if (barHeight > 0) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(barColor.copy(alpha = 0.9f), barColor)
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
            }

            // Draw count text above bar
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 10.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                drawText(
                    "$count",
                    x + barWidth / 2,
                    y - 6.dp.toPx(),
                    paint
                )

                // Draw area label below bar
                val labelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.LTGRAY
                    textSize = 9.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(
                    pair.first,
                    x + barWidth / 2,
                    height - 6.dp.toPx(),
                    labelPaint
                )
            }
        }
    }
}

@Composable
fun PipelineRow(
    title: String,
    count: Int,
    ratio: Float,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("$count khách", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        
        // Progress representation of funnel step
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (ratio > 0f) ratio.coerceIn(0f, 1f) else 0.02f)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(6.dp))
            )
        }
    }
}
