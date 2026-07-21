package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lead
import com.example.ui.theme.*
import com.example.viewmodel.LeadViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrmScreen(
    viewModel: LeadViewModel,
    savedLeads: List<Lead>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedStageTab by remember { mutableStateOf(0) }
    val stages = listOf("Mới", "Đang tư vấn", "Đã chốt", "Hủy")

    // Filter leads by selected stage tab
    val stageLeads = remember(savedLeads, selectedStageTab) {
        val currentStage = stages[selectedStageTab]
        savedLeads.filter { it.status == currentStage }
    }

    // Simulation feedback dialog state
    var showSimFeedback by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }
    var feedbackTitle by remember { mutableStateOf("") }
    var feedbackIcon by remember { mutableStateOf(Icons.Default.Phone) }

    Column(modifier = modifier.fillMaxSize()) {
        // Stage Tab Selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedStageTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (selectedStageTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedStageTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                stages.forEachIndexed { index, stage ->
                    val count = savedLeads.count { it.status == stage }
                    Tab(
                        selected = selectedStageTab == index,
                        onClick = { selectedStageTab = index },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stage,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedStageTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                                Badge(
                                    containerColor = if (selectedStageTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selectedStageTab == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Text("$count")
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .testTag("crm_tab_$index")
                    )
                }
            }
        }

        // Active Leads in Stage
        if (stageLeads.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Không có lead nào trong nhóm \"${stages[selectedStageTab]}\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(stageLeads, key = { it.id }) { lead ->
                    CrmLeadCard(
                        lead = lead,
                        viewModel = viewModel,
                        onStatusChange = { newStatus -> viewModel.updateLeadStatus(lead, newStatus) },
                        onNotesChange = { newNotes -> viewModel.updateLeadNotes(lead, newNotes) },
                        onDelete = { viewModel.removeLeadFromCrm(lead) },
                        onSimulateCall = {
                            feedbackTitle = "Kết nối cuộc gọi giả lập"
                            feedbackMessage = "Hệ thống đang kết nối máy trực tiếp tới khách hàng ${lead.authorName} (${lead.phoneNumber ?: "Không có SĐT"})...\n\nTrạng thái: Đang đổ chuông..."
                            feedbackIcon = Icons.Default.PhoneInTalk
                            showSimFeedback = true
                        },
                        onSimulateMessage = {
                            val categoryText = if (lead.category == "Nội thất") "báo giá gói thiết kế nội thất" else "báo giá cổng sắt & thi công cơ khí"
                            feedbackTitle = "Gửi tin nhắn chào hàng mẫu"
                            feedbackMessage = "Tin nhắn sau đã được chuẩn bị và gửi tới ${lead.authorName}:\n\n\"Chào Anh/Chị, em liên hệ từ đơn vị thi công của mình. Em thấy anh chị có nhu cầu ${lead.intentDescription}. Em xin gửi báo giá sơ bộ để anh chị tham khảo nhé...\""
                            feedbackIcon = Icons.Default.SendAndArchive
                            showSimFeedback = true
                        },
                        onShowDialog = { title, msg, icon ->
                            feedbackTitle = title
                            feedbackMessage = msg
                            feedbackIcon = icon
                            showSimFeedback = true
                        }
                    )
                }
            }
        }
    }

    // Simulated Feedback Dialog
    if (showSimFeedback) {
        AlertDialog(
            onDismissRequest = { showSimFeedback = false },
            icon = { Icon(feedbackIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) },
            title = { Text(feedbackTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text(feedbackMessage, fontSize = 14.sp, lineHeight = 20.sp) },
            confirmButton = {
                TextButton(onClick = { showSimFeedback = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrmLeadCard(
    lead: Lead,
    viewModel: LeadViewModel,
    onStatusChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDelete: () -> Unit,
    onSimulateCall: () -> Unit,
    onSimulateMessage: () -> Unit,
    onShowDialog: (title: String, message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) -> Unit
) {
    var notesText by remember { mutableStateOf(lead.notes ?: "") }
    var isNotesEditing by remember { mutableStateOf(false) }
    var isStatusMenuExpanded by remember { mutableStateOf(false) }
    
    // AI Estimator states
    val isEstimatingMap by viewModel.isEstimatingMap.collectAsState()
    val isEstimating = isEstimatingMap[lead.id] == true
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    // Appointment scheduling form states
    var isSchedulingExpanded by remember { mutableStateOf(false) }
    var selectedDateIndex by remember { mutableStateOf(0) }
    var selectedTimeIndex by remember { mutableStateOf(0) }
    var selectedStaffIndex by remember { mutableStateOf(0) }
    var manualDateText by remember { mutableStateOf("") }
    var isApptStatusMenuExpanded by remember { mutableStateOf(false) }
    
    var replyInputText by remember { mutableStateOf(lead.customerReplyText ?: "") }
    val isParsingReplyMap by viewModel.isParsingReplyMap.collectAsState()
    val isParsingReply = isParsingReplyMap[lead.id] == true

    val datePresets = listOf("Ngày mai (22/07)", "Ngày mốt (23/07)", "Cuối tuần (25/07)", "Tự nhập ngày")
    val timePresets = listOf("09:00 Sáng", "14:30 Chiều", "17:00 Chiều")
    val staffPresets = if (lead.category == "Nội thất") {
        listOf("Nguyễn Văn Hùng (Kỹ thuật Nội thất)", "Trần Minh Tâm (Sale/Tư vấn mẫu)", "Phạm Đăng Khoa (KTS đo đạc)")
    } else {
        listOf("Lê Văn Bằng (Kỹ thuật Hàn xì/Cơ khí)", "Vũ Đình Đại (Khảo sát kết cấu)", "Trần Minh Tâm (Sale/Báo giá)")
    }

    val platformColor = when (lead.platform) {
        "Facebook" -> Color(0xFF1877F2)
        "Threads" -> Color(0xFF000000)
        "TikTok" -> Color(0xFFEE1D52)
        else -> Color(0xFF2F80ED)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("crm_lead_card_${lead.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // CRM Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Name & Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = lead.authorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Category Chip
                        val tagColor = if (lead.category == "Nội thất") FurnCategory else MechCategory
                        Box(
                            modifier = Modifier
                                .background(tagColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(lead.category, color = tagColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "Kênh: ${lead.platform} • ${lead.location}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Stage Dropdown Trigger
                Box {
                    Button(
                        onClick = { isStatusMenuExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("change_status_btn_${lead.id}")
                    ) {
                        Text(lead.status, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Trạng thái", modifier = Modifier.size(14.dp))
                    }
                    DropdownMenu(
                        expanded = isStatusMenuExpanded,
                        onDismissRequest = { isStatusMenuExpanded = false }
                    ) {
                        listOf("Mới", "Đang tư vấn", "Đã chốt", "Hủy").forEach { stage ->
                            DropdownMenuItem(
                                text = { Text(stage, fontSize = 14.sp) },
                                onClick = {
                                    onStatusChange(stage)
                                    isStatusMenuExpanded = false
                                },
                                modifier = Modifier.testTag("status_option_$stage")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI summary highlighting
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("NHU CẦU: ${lead.intentDescription}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lead.content,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==================== MODULE 3: AI CONSULTANT & ESTIMATOR ====================
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI Estimation",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Trợ lý Tư vấn & Báo giá tự động (AI Agent)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Ma trận giá xưởng", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (lead.estimatedPrice == null) {
                        // AI not yet run
                        if (isEstimating) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "AI đang tính toán báo giá & sinh tin nhắn...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "Báo giá sơ bộ và kịch bản chat tự động (chuẩn giọng Nam Bộ thân thiện) chưa được tạo cho khách hàng này.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Button(
                                onClick = { viewModel.generateAiEstimate(lead) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tự động báo giá & Tạo kịch bản tư vấn", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // AI estimate exists
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ước lượng giá sơ bộ:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = lead.estimatedPrice,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { viewModel.generateAiEstimate(lead) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Tính lại",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = lead.aiConsultingText ?: "",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFF333333)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(lead.aiConsultingText ?: ""))
                                        onShowDialog(
                                            "Đã sao chép kịch bản",
                                            "Kịch bản tư vấn của khách hàng ${lead.authorName} đã được sao chép vào bộ nhớ tạm để gửi thủ công!",
                                            Icons.Default.ContentCopy
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy kịch bản", fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        onShowDialog(
                                            "Báo giá tự động gửi thành công",
                                            "Hệ thống Multi-Agent 24/7 đã tự động gửi kịch bản tư vấn & bảng báo giá sơ bộ kèm theo catalogue sản phẩm thực tế của xưởng vào Messenger/Zalo của ${lead.authorName} thành công thông qua Meta Business Suite API!",
                                            Icons.Default.Send
                                        )
                                        onStatusChange("Đang tư vấn")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tự động gửi Inbox", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==================== MODULE 4: AUTOMATIC APPOINTMENTS & TEAM ASSIGNMENT ====================
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Appointments",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Lịch hẹn khảo sát & Giao việc (Telegram)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        
                        val apptStatus = lead.appointmentStatus ?: "Chưa lên lịch"
                        val statusBgColor = when (apptStatus) {
                            "Đã lên lịch" -> Color(0xFF1877F2).copy(alpha = 0.15f)
                            "Đang khảo sát" -> Color(0xFFF2994A).copy(alpha = 0.15f)
                            "Hoàn thành" -> Color(0xFF27AE60).copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val statusTextColor = when (apptStatus) {
                            "Đã lên lịch" -> Color(0xFF1877F2)
                            "Đang khảo sát" -> Color(0xFFD37C2C)
                            "Hoàn thành" -> Color(0xFF27AE60)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        if (lead.appointmentDate != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusBgColor)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = apptStatus,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusTextColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // AI Customer Reply Extraction Area
                    Text(
                        text = "Mô phỏng hội thoại & Bóc tách lịch tự động (AI Auto-Booking):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = replyInputText,
                        onValueChange = { replyInputText = it },
                        placeholder = { Text("Nhập phản hồi của khách hàng đồng ý khảo sát...", fontSize = 11.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Presets Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "Mẫu 1 (Thủ Đức)" to "Em cứ cho thợ qua khảo sát đi, nhà anh ở 123 Lê Văn Việt, Thủ Đức. Qua vào sáng chủ nhật lúc 9h nhé, sđt anh 0901234567.",
                            "Mẫu 2 (Quận 7)" to "Ok em, qua đo đạc báo giá trực tiếp giúp anh nhé. Địa chỉ chung cư Sunrise City Quận 7, rảnh tối mai lúc 19h nha, đt 0987654321.",
                            "Mẫu 3 (Từ chối)" to "Anh chưa rảnh lắm, có gì tuần sau anh nhắn lại sau nha em."
                        ).forEach { (label, text) ->
                            Button(
                                onClick = { replyInputText = text },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                            ) {
                                Text(label, fontSize = 8.sp, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isParsingReply) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "AI đang bóc tách hội thoại (M3/M4)...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                if (replyInputText.isNotEmpty()) {
                                    viewModel.parseCustomerReply(lead, replyInputText)
                                }
                            },
                            enabled = replyInputText.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Bóc tách lịch & Điều phối tự động", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Display Extracted Results Box if parsed
                    if (lead.customerReplyText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (lead.isAgreedFromReply == true) Color(0xFFE2F0D9) else Color(0xFFF2F2F2),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (lead.isAgreedFromReply == true) Color(0xFF385723).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (lead.isAgreedFromReply == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (lead.isAgreedFromReply == true) Color(0xFF385723) else Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (lead.isAgreedFromReply == true) "Khách đồng ý khảo sát công trình!" else "Khách chưa đồng ý hẹn lịch.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (lead.isAgreedFromReply == true) Color(0xFF385723) else Color.DarkGray
                                )
                            }
                            
                            if (lead.isAgreedFromReply == true) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("⏰ Thời gian: ${lead.extractedAppointmentTime ?: "Chưa xác định"}", fontSize = 11.sp, color = Color(0xFF333333))
                                Text("📍 Địa chỉ: ${lead.extractedAddress ?: "Chưa xác định"}", fontSize = 11.sp, color = Color(0xFF333333))
                                Text("📞 Điện thoại: ${lead.extractedPhone ?: "Chưa xác định"}", fontSize = 11.sp, color = Color(0xFF333333))
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                // Action button to trigger automated dispatch
                                Button(
                                    onClick = {
                                        val telegramPayload = """
                                            📅 CÓ LỊCH KHẢO SÁT MỚI ĐƯỢC CHỐT! (TELEGRAM BOT 24/7)
                                            
                                            👤 Khách hàng: ${lead.authorName}
                                            📞 Số điện thoại: ${lead.extractedPhone ?: lead.phoneNumber ?: "Chưa có"}
                                            ⏰ Thời gian hẹn: ${lead.extractedAppointmentTime}
                                            📍 Địa chỉ công trình: ${lead.extractedAddress}
                                            👤 Nhân sự: ${lead.assignedStaff ?: "Nhóm kỹ thuật"}
                                            🔗 Link nguồn: ${lead.sourceUrl}
                                            
                                            👉 Đề nghị đội ngũ kỹ thuật liên hệ xác nhận trước khi qua.
                                        """.trimIndent()

                                        val calendarPayload = """
                                            📅 GOOGLE CALENDAR SYNCED SUCCESSFULLY
                                            
                                            Sự kiện: [Khảo sát công trình] ${lead.authorName}
                                            Thời gian: ${lead.extractedAppointmentTime}
                                            Nhân sự: ${lead.assignedStaff ?: "Nhóm kỹ thuật"}
                                            Địa điểm: ${lead.extractedAddress}
                                        """.trimIndent()

                                        onShowDialog(
                                            "Hệ thống tự động Dispatch",
                                            "Đã hoàn thành tự động hóa 100%:\n\n1. Tạo sự kiện Google Calendar thành công.\n\n2. Bắn thông báo giao việc đến Telegram nhóm Kỹ thuật/Sale:\n\n$telegramPayload\n\n$calendarPayload",
                                            Icons.Default.SendAndArchive
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF385723)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .height(26.dp)
                                ) {
                                    Icon(Icons.Default.SendAndArchive, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bắn Telegram & Calendar", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (lead.appointmentDate == null) {
                        // Form is collapsed or visible
                        if (!isSchedulingExpanded) {
                            Text(
                                "Chưa lên lịch hẹn khảo sát đo đạc thực tế tại công trình/nhà khách hàng.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { isSchedulingExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            ) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lên lịch hẹn & Giao thợ ngay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Date selection row
                            Text("Chọn ngày hẹn khảo sát:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                datePresets.forEachIndexed { idx, preset ->
                                    val isSelected = selectedDateIndex == idx
                                    Button(
                                        onClick = { selectedDateIndex = idx },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(28.dp)
                                    ) {
                                        Text(preset, fontSize = 9.sp, maxLines = 1)
                                    }
                                }
                            }

                            if (selectedDateIndex == 3) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = manualDateText,
                                    onValueChange = { manualDateText = it },
                                    placeholder = { Text("Ví dụ: 2026-07-28", fontSize = 11.sp) },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Time selection
                            Text("Chọn giờ khảo sát:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                timePresets.forEachIndexed { idx, preset ->
                                    val isSelected = selectedTimeIndex == idx
                                    Button(
                                        onClick = { selectedTimeIndex = idx },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(28.dp)
                                    ) {
                                        Text(preset, fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Staff Assignment selection
                            Text("Phân công nhân sự tiếp quản:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                staffPresets.forEachIndexed { idx, staffName ->
                                    val isSelected = selectedStaffIndex == idx
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedStaffIndex = idx },
                                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(staffName, fontSize = 11.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { isSchedulingExpanded = false },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Hủy", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val finalDate = if (selectedDateIndex == 3) {
                                            if (manualDateText.isEmpty()) "2026-07-28" else manualDateText
                                        } else {
                                            datePresets[selectedDateIndex]
                                        }
                                        val finalTime = timePresets[selectedTimeIndex]
                                        val finalStaffName = staffPresets[selectedStaffIndex]

                                        viewModel.scheduleAppointment(lead, finalDate, finalTime, finalStaffName)
                                        isSchedulingExpanded = false

                                        // Sim notification telegram and google calendar payload details
                                        val telegramPayload = """
                                            🔔 THÔNG BÁO GIAO VIỆC KHẨN CẤP (TELEGRAM BOT 24/7)
                                            
                                            👤 Khách hàng: ${lead.authorName}
                                            📞 Số điện thoại: ${lead.phoneNumber ?: "Không có SĐT - Liên hệ qua tin nhắn"}
                                            📍 Khu vực: ${lead.location}
                                            📋 Yêu cầu: ${lead.intentDescription}
                                            📅 Lịch hẹn khảo sát: $finalDate lúc $finalTime
                                            👤 Nhân sự đảm nhiệm: $finalStaffName
                                            
                                            👉 Chi tiết nội dung bài đăng: "${lead.content}"
                                            👉 Link nguồn: ${lead.sourceUrl}
                                            
                                            📲 Vui lòng gọi điện/nhắn tin xác nhận với khách hàng trước khi di chuyển!
                                        """.trimIndent()

                                        val calendarPayload = """
                                            📅 GOOGLE CALENDAR SYNCED SUCCESSFULLY
                                            
                                            Sự kiện: [Khảo sát công trình] ${lead.authorName}
                                            Thời gian: $finalDate, lúc $finalTime
                                            Nhân sự chủ trì: $finalStaffName
                                            Địa điểm: ${lead.location}
                                            Mô tả chi tiết: Tiến hành đo đạc thực tế tại công trình khách hàng, tư vấn bản vẽ 2D/3D miễn phí và ký kết biên bản thiết kế. SĐT khách hàng: ${lead.phoneNumber ?: "Chưa có"}
                                        """.trimIndent()

                                        onShowDialog(
                                            "Đặt lịch hẹn & Giao việc 24/7",
                                            "Hệ thống đã tự động hoàn tất:\n\n1. Tạo sự kiện Google Calendar và gửi thư mời làm việc tới email của $finalStaffName.\n\n2. Bắn thông báo khẩn cấp Telegram Bot giao việc đến nhóm nhân viên của xưởng:\n\n$telegramPayload\n\n$calendarPayload",
                                            Icons.Default.SendAndArchive
                                        )
                                        onStatusChange("Đã chốt")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(2.5f).height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Xác nhận & Giao việc (Telegram)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Appointment details display
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ngày khảo sát: ${lead.appointmentDate}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Thời gian hẹn: ${lead.appointmentTime}",
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Nhân sự đảm nhiệm: ${lead.assignedStaff}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cập nhật:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(6.dp))

                                Box {
                                    OutlinedButton(
                                        onClick = { isApptStatusMenuExpanded = true },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(lead.appointmentStatus ?: "Đã lên lịch", fontSize = 10.sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(12.dp))
                                    }

                                    DropdownMenu(
                                        expanded = isApptStatusMenuExpanded,
                                        onDismissRequest = { isApptStatusMenuExpanded = false }
                                    ) {
                                        listOf("Đã lên lịch", "Đang khảo sát", "Hoàn thành").forEach { state ->
                                            DropdownMenuItem(
                                                text = { Text(state, fontSize = 12.sp) },
                                                onClick = {
                                                    viewModel.updateAppointmentStatus(lead, state)
                                                    isApptStatusMenuExpanded = false
                                                    onShowDialog(
                                                        "Cập nhật tiến độ thành công",
                                                        "Đã cập nhật trạng thái lịch hẹn của ${lead.authorName} thành \"$state\". Thông báo tự động cũng đã gửi về Telegram cho kỹ thuật viên tiếp quản!",
                                                        Icons.Default.CheckCircle
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                OutlinedIconButton(
                                    onClick = {
                                        val telegramPayload = """
                                            🔄 CẬP NHẬT GIAO VIỆC (TELEGRAM REMINDER 24/7)
                                            
                                            👤 Khách hàng: ${lead.authorName}
                                            📞 Số điện thoại: ${lead.phoneNumber ?: "Không có SĐT"}
                                            📅 Lịch hẹn khảo sát: ${lead.appointmentDate} lúc ${lead.appointmentTime}
                                            👤 Nhân sự: ${lead.assignedStaff}
                                            🔔 Trạng thái hiện tại: ${lead.appointmentStatus}
                                            
                                            ⚠️ Ghi chú: Kỹ thuật viên nhớ mang theo catalogue vật liệu mới và biên bản thỏa thuận thiết kế nhé!
                                        """.trimIndent()

                                        onShowDialog(
                                            "Bắn lại thông báo Telegram",
                                            "Đã gửi tin nhắn Telegram khẩn cấp nhắc nhở công việc tới ${lead.assignedStaff} qua kênh Telegram xưởng:\n\n$telegramPayload",
                                            Icons.Default.SendAndArchive
                                        )
                                    },
                                    modifier = Modifier.size(28.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.SendAndArchive, contentDescription = "Bắn Telegram lại", modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contact & Notes Workspace
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call / Message Quick actions if Phone number exists
                if (lead.phoneNumber != null) {
                    FilledIconButton(
                        onClick = onSimulateCall,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MechCategory.copy(alpha = 0.15f), contentColor = MechCategory),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(36.dp).testTag("sim_call_btn")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Gọi ngay", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = onSimulateMessage,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = FurnCategory.copy(alpha = 0.15f), contentColor = FurnCategory),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(36.dp).testTag("sim_msg_btn")
                    ) {
                        Icon(Icons.Default.Message, contentDescription = "Zalo/SMS", modifier = Modifier.size(16.dp))
                    }
                } else {
                    // Sim social message
                    FilledIconButton(
                        onClick = onSimulateMessage,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = platformColor.copy(alpha = 0.15f), contentColor = platformColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(36.dp).testTag("sim_social_btn")
                    ) {
                        Icon(Icons.Default.Forum, contentDescription = "Nhắn tin MXH", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Không có SĐT • Nhắn tin qua Link", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Delete Lead from Board button
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.size(36.dp).testTag("delete_lead_crm_btn")
                ) {
                    Icon(Icons.Default.BookmarkRemove, contentDescription = "Bỏ lưu")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contractor Notes Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Notes, contentDescription = "Ghi chú", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ghi chú xưởng/sales:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.weight(1f))
                    
                    if (isNotesEditing) {
                        IconButton(
                            onClick = {
                                onNotesChange(notesText)
                                isNotesEditing = false
                            },
                            modifier = Modifier.size(24.dp).testTag("save_notes_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Lưu ghi chú", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        IconButton(
                            onClick = { isNotesEditing = true },
                            modifier = Modifier.size(24.dp).testTag("edit_notes_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Sửa ghi chú", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                if (isNotesEditing) {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        placeholder = { Text("Thêm tiến độ làm việc, ghi chú báo giá...", fontSize = 12.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("notes_text_field")
                    )
                } else {
                    Text(
                        text = if (lead.notes.isNullOrEmpty()) "Chưa có ghi chú nào. Hãy nhấp nút bút chì để thêm thông tin..." else lead.notes,
                        fontSize = 12.sp,
                        color = if (lead.notes.isNullOrEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
