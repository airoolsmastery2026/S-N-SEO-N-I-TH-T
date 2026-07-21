package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.*
import com.example.viewmodel.LeadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LeadViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val testerState by viewModel.testerState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // API Key availability indicator
    val hasApiKey = remember {
        BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
    }

    // Default sample texts to let user quickly test
    val sampleTexts = listOf(
        "Cần tìm đơn vị đóng tủ bếp gỗ công nghiệp khu vực Thuận An, Bình Dương gấp.",
        "Mọi người biết ai nhận hàn lan can cửa sắt giá tốt ở Quận 12 không ạ, sđt 0903112233",
        "Tuyển nhân viên bán hàng nội thất lương cao tại showroom Quận 7." // Spam
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: AI Status Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasApiKey) MechCategory.copy(alpha = 0.1f) else SpamCategory.copy(alpha = 0.08f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (hasApiKey) MechCategory.copy(alpha = 0.2f) else SpamCategory.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (hasApiKey) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                        contentDescription = "API Status",
                        tint = if (hasApiKey) MechCategory else SpamCategory
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasApiKey) "Đã cấu hình Gemini API Key" else "Đang dùng Mô hình Phân tích Dự phòng",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (hasApiKey) MechCategory else SpamCategory
                    )
                    Text(
                        text = if (hasApiKey) "Hệ thống sử dụng Mô hình gemini-3.5-flash trực tiếp" else "Cung cấp API Key trong AI Studio Secrets để trải nghiệm phân tích AI thời gian thực",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section: AI Intent Extraction Playground (Playground)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Phòng Thử Nghiệm AI (Playground)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Nhập bài viết hoặc bình luận từ mạng xã hội để kiểm tra khả năng bóc tách lead của AI:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Input Text Field
                OutlinedTextField(
                    value = testerState.inputText,
                    onValueChange = { viewModel.updateTesterInput(it) },
                    placeholder = { Text("Ví dụ: Mình cần thi công rèm cửa chung cư 2 phòng ngủ ở Thủ Đức, báo giá inbox nhé...", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("ai_tester_input"),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gợi ý test:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    sampleTexts.forEachIndexed { idx, txt ->
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                .clickable { viewModel.updateTesterInput(txt) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Mẫu ${idx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.analyzeCustomText(testerState.inputText)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("ai_analyze_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !testerState.isAnalyzing
                ) {
                    if (testerState.isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang phân tích...")
                    } else {
                        Icon(Icons.Default.ModelTraining, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Phân Tích Bằng AI", fontWeight = FontWeight.Bold)
                    }
                }

                // AI Response Section
                AnimatedVisibility(visible = testerState.result != null || testerState.error != null) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (testerState.error != null) {
                            Text(
                                text = testerState.error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        testerState.result?.let { extracted ->
                            Text(
                                text = "Kết quả bóc tách dữ liệu bởi AI:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Display parsed elements
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Category Element
                                ResultElementRow(
                                    label = "Lĩnh vực (Category):",
                                    value = extracted.category,
                                    badgeColor = when (extracted.category) {
                                        "Nội thất" -> FurnCategory
                                        "Cơ khí" -> MechCategory
                                        else -> SpamCategory
                                    }
                                )

                                // Location Element
                                ResultElementRow(
                                    label = "Khu vực (Location):",
                                    value = extracted.location,
                                    icon = Icons.Default.LocationOn
                                )

                                // Phone Element
                                ResultElementRow(
                                    label = "Số điện thoại (Phone):",
                                    value = extracted.phone ?: "Không phát hiện thấy",
                                    icon = Icons.Default.Phone,
                                    valueColor = if (extracted.phone != null) MechCategory else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Intent summary Element
                                ResultElementRow(
                                    label = "Tóm tắt ý định (Intent):",
                                    value = extracted.intent,
                                    icon = Icons.Default.Subject,
                                    isBold = true
                                )

                                // Confidence score
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = WarmGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Độ tin cậy của Lead:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("${(extracted.confidence * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WarmGold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Keywords & Blacklist Rules Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DisplaySettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cấu hình quy tắc lắng nghe",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Keyword filters
                Text("Từ khóa quét khách hàng:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "cần thi công, cần làm, tìm xưởng, xin báo giá, nhờ tư vấn, tìm thợ gỗ, cửa sắt nghệ thuật, tủ áo chung cư...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Từ khóa loại trừ Spam (Rác):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SpamCategory)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "tuyển dụng, xưởng bên em nhận, giá rẻ tận gốc, thanh lý sofa cũ, xả kho giường tủ...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        // Section: System Database controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quản lý cơ sở dữ liệu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Clear Unsaved Button
                OutlinedButton(
                    onClick = { viewModel.clearUnsavedLeads() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("clear_unsaved_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Xóa tất cả Tin đã quét (Chưa lưu)", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reset Database Button
                Button(
                    onClick = { viewModel.clearAllData() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("reset_db_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Đặt lại toàn bộ Dữ liệu", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ResultElementRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    badgeColor: Color? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        if (badgeColor != null) {
            Box(
                modifier = Modifier
                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = value.uppercase(),
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 13.sp,
                color = valueColor,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
