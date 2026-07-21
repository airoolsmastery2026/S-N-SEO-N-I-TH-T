package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leads")
data class Lead(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorName: String,
    val authorAvatar: String? = null,
    val platform: String, // Facebook, Threads, TikTok, Zalo
    val postType: String, // Bài đăng, Bình luận
    val groupName: String? = null, // e.g. "Hội Cư Dân Chung Cư TP.HCM"
    val content: String,
    val category: String, // Nội thất, Cơ khí, Không phù hợp
    val location: String, // TP.HCM, Bình Dương, Đồng Nai, Long An, ...
    val phoneNumber: String? = null,
    val intentDescription: String, // Tóm tắt nhu cầu từ AI
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Mới", // Mới, Đang tư vấn, Đã chốt, Hủy
    val isSaved: Boolean = false,
    val notes: String? = null,
    val sourceUrl: String = "https://facebook.com",
    val estimatedPrice: String? = null,
    val aiConsultingText: String? = null,
    val appointmentDate: String? = null,
    val appointmentTime: String? = null,
    val assignedStaff: String? = null,
    val appointmentStatus: String? = "Chưa lên lịch",
    val customerReplyText: String? = null,
    val isAgreedFromReply: Boolean? = null,
    val extractedAppointmentTime: String? = null,
    val extractedAddress: String? = null,
    val extractedPhone: String? = null
)

data class LeadFilter(
    val categories: Set<String> = setOf("Nội thất", "Cơ khí"),
    val locations: Set<String> = setOf("TP.HCM", "Bình Dương", "Đồng Nai", "Long An"),
    val platforms: Set<String> = setOf("Facebook", "Threads", "TikTok", "Zalo"),
    val minConfidence: Float = 0.6f,
    val searchText: String = ""
)
