package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class ExtractedLeadJson(
    @Json(name = "category") val category: String, // "Nội thất", "Cơ khí", "Không phù hợp"
    @Json(name = "location") val location: String, // e.g. "Quận 12, TP.HCM"
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "intent") val intent: String, // Tóm tắt nhu cầu thi công ngắn gọn, hấp dẫn
    @Json(name = "confidence") val confidence: Float = 1.0f
)

@JsonClass(generateAdapter = true)
data class EstimateResultJson(
    @Json(name = "estimatedPrice") val estimatedPrice: String,
    @Json(name = "aiConsultingText") val aiConsultingText: String
)

@JsonClass(generateAdapter = true)
data class ParsedBookingJson(
    @Json(name = "has_agreed") val hasAgreed: Boolean,
    @Json(name = "appointment_time") val appointmentTime: String,
    @Json(name = "address") val address: String,
    @Json(name = "phone") val phone: String? = null
)

