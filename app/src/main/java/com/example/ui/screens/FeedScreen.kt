package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lead
import com.example.data.model.LeadFilter
import com.example.ui.theme.*
import com.example.viewmodel.LeadViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: LeadViewModel,
    leads: List<Lead>,
    isScanning: Boolean,
    filter: LeadFilter,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFilterExpanded by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf(filter.searchText) }

    Column(modifier = modifier.fillMaxSize()) {
        // Upper Header / Pulse Indicator
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isScanning) {
                    RadarScanner(modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Đang quét mạng xã hội...",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Hệ thống AI lọc bài viết 5s/lần",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Radar,
                            contentDescription = "Radar Off",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Radar Lắng Nghe Đang Tắt",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Nhấp nút quét bên dưới để bắt đầu tìm khách",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                Button(
                    onClick = { viewModel.toggleScanning() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("toggle_scan_button")
                ) {
                    Icon(
                        if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Quét"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isScanning) "Dừng" else "Quét")
                }
            }
        }

        // Quick Filter and Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchInput,
                onValueChange = {
                    searchInput = it
                    viewModel.updateFilter(filter.copy(searchText = it))
                },
                placeholder = { Text("Tìm kiếm nội dung, tên...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm") },
                trailingIcon = {
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = {
                            searchInput = ""
                            viewModel.updateFilter(filter.copy(searchText = ""))
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_leads_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { isFilterExpanded = !isFilterExpanded },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .size(48.dp)
            ) {
                Icon(
                    if (isFilterExpanded) Icons.Default.FilterListOff else Icons.Default.FilterList,
                    contentDescription = "Bộ lọc",
                    tint = if (isFilterExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Expanded Filters Panel
        AnimatedVisibility(visible = isFilterExpanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Bộ lọc lắng nghe", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Filters
                    Text("Lĩnh vực thi công:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Nội thất", "Cơ khí").forEach { category ->
                            val isSelected = category in filter.categories
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newCats = if (isSelected) filter.categories - category else filter.categories + category
                                    viewModel.updateFilter(filter.copy(categories = newCats))
                                },
                                label = { Text(category) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Location Filters
                    Text("Khu vực thị trường:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("TP.HCM", "Bình Dương", "Đồng Nai").forEach { loc ->
                            val isSelected = loc in filter.locations
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newLocs = if (isSelected) filter.locations - loc else filter.locations + loc
                                    viewModel.updateFilter(filter.copy(locations = newLocs))
                                },
                                label = { Text(loc) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // Feed List
        if (leads.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.SentimentDissatisfied,
                        contentDescription = "No Lead",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isScanning) "Đang phân tích dòng bài viết mạng xã hội..." else "Không tìm thấy lead phù hợp",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    if (!isScanning && leads.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.toggleScanning() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Nhấp Bắt Đầu Quét Mới")
                        }
                    }
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
                items(leads, key = { it.id }) { lead ->
                    LeadFeedItemCard(
                        lead = lead,
                        onSave = { viewModel.saveLeadToCrm(lead) },
                        onRemove = { viewModel.deleteLead(lead) }
                    )
                }
            }
        }
    }
}

// Custom Scanning Animation Radar
@Composable
fun RadarScanner(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pulsingRadius by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = EaseOutExpo),
            repeatMode = RepeatMode.Restart
        )
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.width / 2

        // Draw background radar lines
        drawCircle(
            color = primaryColor.copy(alpha = 0.1f),
            radius = outerRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.15f),
            radius = outerRadius * 0.6f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw expanding pulse circle
        drawCircle(
            color = primaryColor.copy(alpha = (1f - pulsingRadius) * 0.25f),
            radius = outerRadius * pulsingRadius,
            center = center
        )

        // Draw rotating scanner sweep line
        val sweepBrush = Brush.sweepGradient(
            colors = listOf(Color.Transparent, primaryColor.copy(alpha = 0.5f), primaryColor),
            center = center
        )
        drawCircle(
            brush = sweepBrush,
            radius = outerRadius,
            center = center,
            alpha = 0.6f
        )

        // Draw radar core center
        drawCircle(
            color = primaryColor,
            radius = 4.dp.toPx(),
            center = center
        )
    }
}

@Composable
fun LeadFeedItemCard(
    lead: Lead,
    onSave: () -> Unit,
    onRemove: () -> Unit
) {
    val isSpam = lead.category == "Không phù hợp"
    val cardBg = if (isSpam) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lead_card_${lead.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSpam) 0.dp else 1.dp),
        shape = RoundedCornerShape(16.dp),
        border = if (lead.isSaved) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Platform & Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Platform Badge
                val (badgeColor, icon) = when (lead.platform) {
                    "Facebook" -> Pair(Color(0xFF1877F2), Icons.Default.Facebook)
                    "Threads" -> Pair(Color(0xFF000000), Icons.Default.AlternateEmail)
                    "TikTok" -> Pair(Color(0xFFEE1D52), Icons.Default.Audiotrack)
                    else -> Pair(Color(0xFF2F80ED), Icons.Default.ChatBubbleOutline) // Zalo
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(badgeColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = lead.platform,
                        tint = badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = lead.authorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (lead.groupName != null) {
                            Text(
                                text = " • ${lead.groupName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()).format(Date(lead.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Category Tag
                val (categoryBg, categoryText, categoryColor) = when (lead.category) {
                    "Nội thất" -> Triple(FurnCategory.copy(alpha = 0.15f), "NỘI THẤT", FurnCategory)
                    "Cơ khí" -> Triple(MechCategory.copy(alpha = 0.15f), "CƠ KHÍ", MechCategory)
                    else -> Triple(SpamCategory.copy(alpha = 0.12f), "KHÔNG PHÙ HỢP", SpamCategory)
                }
                Box(
                    modifier = Modifier
                        .background(categoryBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = categoryText,
                        color = categoryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post content
            Text(
                text = lead.content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                color = if (isSpam) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // AI Insight box (Hidden or faded for spam)
            if (!isSpam) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Phân tích nhu cầu thực tế (AI):",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = lead.intentDescription,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Lead Metadata Row (Location, Phone)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Location badge
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Địa điểm", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(lead.location, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Phone badge if available
                if (lead.phoneNumber != null) {
                    Box(
                        modifier = Modifier
                            .background(MechCategory.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = "Sđt", tint = MechCategory, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(lead.phoneNumber, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MechCategory)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action Buttons
                if (isSpam) {
                    IconButton(
                        onClick = onRemove,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa rác")
                    }
                } else if (lead.isSaved) {
                    IconButton(
                        onClick = {},
                        enabled = false,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = "Đã lưu", tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp).testTag("save_lead_btn_${lead.id}")
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = "Lưu Lead", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lưu Lead", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
