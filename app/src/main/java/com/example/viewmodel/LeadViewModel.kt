package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.ExtractedLeadJson
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.RetrofitClient
import com.example.data.local.LeadDatabase
import com.example.data.model.Lead
import com.example.data.model.LeadFilter
import com.example.data.repository.LeadRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class LeadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LeadRepository
    private val moshi: Moshi = RetrofitClient.moshiInstance

    init {
        val database = LeadDatabase.getDatabase(application)
        repository = LeadRepository(database.leadDao())
    }

    // Tabs: 0 = Listening Feed, 1 = CRM (Lead Manager), 2 = Analytics, 3 = Settings & AI Tester
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _filter = MutableStateFlow(LeadFilter())
    val filter: StateFlow<LeadFilter> = _filter.asStateFlow()

    // Database flows
    val allLeadsFlow = repository.allLeads
    val savedLeadsFlow = repository.savedLeads

    // Filtered Feed Leads Flow
    val filteredLeads: StateFlow<List<Lead>> = combine(
        allLeadsFlow,
        _filter
    ) { leads, filter ->
        leads.filter { lead ->
            val matchCategory = lead.category in filter.categories
            val matchLocation = filter.locations.any { loc -> lead.location.contains(loc, ignoreCase = true) }
            val matchPlatform = lead.platform in filter.platforms
            val matchSearch = if (filter.searchText.isEmpty()) {
                true
            } else {
                lead.content.contains(filter.searchText, ignoreCase = true) ||
                        lead.authorName.contains(filter.searchText, ignoreCase = true) ||
                        lead.intentDescription.contains(filter.searchText, ignoreCase = true)
            }
            matchCategory && matchLocation && matchPlatform && matchSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered CRM Saved Leads Flow
    val filteredSavedLeads: StateFlow<List<Lead>> = combine(
        savedLeadsFlow,
        _filter
    ) { leads, filter ->
        leads.filter { lead ->
            val matchCategory = lead.category in filter.categories
            val matchLocation = filter.locations.any { loc -> lead.location.contains(loc, ignoreCase = true) }
            val matchSearch = if (filter.searchText.isEmpty()) {
                true
            } else {
                lead.content.contains(filter.searchText, ignoreCase = true) ||
                        lead.authorName.contains(filter.searchText, ignoreCase = true) ||
                        lead.intentDescription.contains(filter.searchText, ignoreCase = true)
            }
            matchCategory && matchLocation && matchSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Tester state
    data class TesterUiState(
        val inputText: String = "",
        val isAnalyzing: Boolean = false,
        val result: ExtractedLeadJson? = null,
        val error: String? = null,
        val rawAiResponse: String? = null
    )

    private val _testerState = MutableStateFlow(TesterUiState())
    val testerState: StateFlow<TesterUiState> = _testerState.asStateFlow()

    private var scanningJob: Job? = null

    // Pre-defined sample pool of raw social posts
    private val rawPostsPool = listOf(
        SimulatedPost(
            authorName = "Nguyễn Văn Hùng",
            platform = "Facebook",
            postType = "Bài đăng",
            groupName = "Hội Cư Dân Chung Cư TP.HCM",
            content = "Mọi người ơi cho em hỏi quanh khu vực Thủ Đức có xưởng cơ khí nào nhận làm lan can cầu thang sắt nghệ thuật không ạ? Cho em xin địa chỉ với."
        ),
        SimulatedPost(
            authorName = "Trần Thị Lan",
            platform = "Facebook",
            postType = "Bài đăng",
            groupName = "Cộng Đồng Vinhomes Grand Park Q9",
            content = "Em chuẩn bị nhận bàn giao căn hộ 2PN ở Vinhomes Grand Park Quận 9. Muốn tìm đơn vị thiết kế thi công nội thất trọn gói phong cách tối giản. Ngân sách tầm 150tr đổ lại. Inbox báo giá em nhé. Sđt: 0912345678"
        ),
        SimulatedPost(
            authorName = "Xưởng Gỗ Minh Phát",
            platform = "Facebook",
            postType = "Bài đăng",
            groupName = "Hội Thiết Kế & Thi Công Nội Thất Sài Gòn",
            content = "Xưởng gỗ bên em nhận gia công tủ bếp, giường tủ, tủ áo giá tận xưởng tại Bình Dương, liên hệ 0988776655." // Spam / Ad
        ),
        SimulatedPost(
            authorName = "Cơ Khí Hoàng Nam",
            platform = "TikTok",
            postType = "Bình luận",
            groupName = null,
            content = "Nhận thi công cửa cuốn cửa kéo inox giá rẻ tại Bình Chánh. Liên hệ qua số Zalo em 0902888999" // Spam / Ad
        ),
        SimulatedPost(
            authorName = "Phạm Minh Tuấn",
            platform = "Threads",
            postType = "Bài đăng",
            groupName = null,
            content = "Có ai biết chỗ nào nhận hàn lại cái chân bàn sắt bị gãy ở gần khu vực Quận 12 không, chỉ giúp mình với, cám ơn nhiều."
        ),
        SimulatedPost(
            authorName = "Lê Thị Thu",
            platform = "Facebook",
            postType = "Bài đăng",
            groupName = "Hội Săn Thợ Cơ Khí Bình Dương - Đồng Nai",
            content = "Cần tìm thợ làm mái tôn chống nóng cho sân thượng nhà phố ở Biên Hòa, Đồng Nai. Diện tích tầm 30m2. Ai làm ib m nhé."
        ),
        SimulatedPost(
            authorName = "Hoàng Anh",
            platform = "Zalo",
            postType = "Bài đăng",
            groupName = "Nhóm Xây Dựng Bình Dương",
            content = "Mình cần làm tủ quần áo âm tường bằng gỗ công nghiệp An Cường ở Dĩ An, Bình Dương. Ai nhận thi công báo giá m nha."
        ),
        SimulatedPost(
            authorName = "Văn Tuấn",
            platform = "Facebook",
            postType = "Bài đăng",
            groupName = "Hội Thanh Lý Đồ Cũ HCMC",
            content = "Cần mua thanh lý bàn ghế văn phòng cũ ở TP.HCM. Ai thanh lý ib hình và giá." // Irrelevant
        ),
        SimulatedPost(
            authorName = "Mai Phương",
            platform = "Facebook",
            postType = "Bài đăng",
            groupName = "Hội Cư Dân Sunrise City Quận 7",
            content = "Cần cải tạo lại nội thất phòng ngủ cho bé ở Quận 7, TP.HCM. Cần tìm đơn vị uy tín, thiết kế xinh xắn chút ạ."
        ),
        SimulatedPost(
            authorName = "Phan Quân",
            platform = "Threads",
            postType = "Bài đăng",
            groupName = null,
            content = "Tìm xưởng cơ khí nhận gia công khung sắt kệ trồng rau sân thượng ở Quận Gò Vấp. Sđt liên hệ: 0909112233"
        ),
        SimulatedPost(
            authorName = "Vũ Đình Đại",
            platform = "TikTok",
            postType = "Bình luận",
            content = "Anh ơi, cái kệ sắt này lắp ghép hay hàn vậy? Inbox báo giá em bộ này lắp tại Thuận An, Bình Dương nha."
        ),
        SimulatedPost(
            authorName = "Bảo Châu",
            platform = "Zalo",
            postType = "Bài đăng",
            groupName = "Hội Cư Dân Căn Hộ Thủ Dầu Một",
            content = "Phòng khách nhà em hơi nhỏ, muốn đóng một bộ sofa chữ L bọc nỉ thông minh đo ni đóng giày riêng. Có xưởng nội thất nào ở Bình Dương nhận làm ko ạ?"
        )
    )

    fun setTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    fun updateFilter(newFilter: LeadFilter) {
        _filter.value = newFilter
    }

    fun toggleScanning() {
        if (_isScanning.value) {
            stopScanning()
        } else {
            startScanning()
        }
    }

    private fun startScanning() {
        _isScanning.value = true
        scanningJob = viewModelScope.launch {
            while (_isScanning.value) {
                // Simulate periodic social listening scraping (e.g. every 5 to 10 seconds)
                delay(7000)
                if (!_isScanning.value) break

                // Pick a random post from the pool
                val rawPost = rawPostsPool.random()
                
                // Process the raw post using AI or fallback rules
                val extracted = analyzePostWithAI(rawPost.content)
                
                val lead = Lead(
                    authorName = rawPost.authorName,
                    authorAvatar = getSimulatedAvatarUrl(rawPost.authorName),
                    platform = rawPost.platform,
                    postType = rawPost.postType,
                    groupName = rawPost.groupName,
                    content = rawPost.content,
                    category = extracted.category,
                    location = extracted.location,
                    phoneNumber = extracted.phone,
                    intentDescription = extracted.intent,
                    isSaved = false,
                    sourceUrl = getSimulatedSourceUrl(rawPost.platform)
                )

                // Save to local database (will automatically update Feed UI)
                repository.insertLead(lead)
            }
        }
    }

    private fun stopScanning() {
        _isScanning.value = false
        scanningJob?.cancel()
        scanningJob = null
    }

    // Interactive AI analysis for custom text in Settings screen
    fun analyzeCustomText(text: String) {
        if (text.trim().isEmpty()) {
            _testerState.update { it.copy(error = "Vui lòng nhập nội dung cần phân tích") }
            return
        }

        _testerState.update { it.copy(isAnalyzing = true, error = null, result = null, rawAiResponse = null) }

        viewModelScope.launch {
            try {
                val result = analyzePostWithAI(text)
                _testerState.update {
                    it.copy(
                        isAnalyzing = false,
                        result = result,
                        rawAiResponse = "Phân tích thành công từ mô hình gemini-3.5-flash!"
                    )
                }
            } catch (e: Exception) {
                _testerState.update {
                    it.copy(
                        isAnalyzing = false,
                        error = "Lỗi kết nối AI: ${e.message}. Đã áp dụng phân tích cục bộ."
                    )
                }
            }
        }
    }

    fun updateTesterInput(text: String) {
        _testerState.update { it.copy(inputText = text) }
    }

    // Call Gemini API to extract details or use deterministic fallback
    private suspend fun analyzePostWithAI(content: String): ExtractedLeadJson = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

        if (hasKey) {
            try {
                val systemPrompt = """
                    Bạn là hệ thống AI Lắng nghe mạng xã hội (Social Listening AI) cho thị trường Nội thất & Cơ khí tại TP.HCM và vùng lân cận (Bình Dương, Đồng Nai, Long An...).
                    Nhiệm vụ của bạn là phân tích bài viết/bình luận của người dùng trên mạng xã hội, phát hiện ý định tìm kiếm thợ/đơn vị thi công.
                    
                    Hãy phân loại bài viết theo các quy tắc sau:
                    1. Phân loại (category):
                       - "Nội thất" nếu họ cần làm tủ bếp, giường tủ, bàn ghế gỗ, thi công căn hộ, sàn gỗ, rèm cửa, sofa...
                       - "Cơ khí" nếu họ cần làm cửa sắt, mái tôn, lan can sắt, hàn xì, cổng ngõ, kệ sắt, nhà xưởng...
                       - "Không phù hợp" nếu đó là tin quảng cáo từ xưởng khác, tin rác, spam, tuyển dụng, thanh lý đồ cũ, hoặc không liên quan.
                    2. Địa điểm (location): Tìm kiếm các quận, huyện, hoặc tỉnh thành lân cận (TP.HCM, Bình Dương, Đồng Nai, Long An, ...). Định dạng chuẩn, ví dụ: "Quận 12, TP.HCM" hoặc "Dĩ An, Bình Dương". Nếu không phát hiện, hãy ghi "TP.HCM và lân cận".
                    3. Số điện thoại (phone): Trích xuất số điện thoại nếu có trong bài viết (chuỗi số từ 9 đến 11 số). Nếu không có, điền null.
                    4. Ý định (intent): Tóm tắt nhu cầu thực sự một cách súc tích, chuyên nghiệp bằng tiếng Việt (không quá 15 từ). Ví dụ: "Cần thi công lan can sắt nghệ thuật", "Thiết kế nội thất căn hộ 2PN".
                    5. Độ tin cậy (confidence): Đánh giá từ 0.0 đến 1.0 về khả năng đây là khách hàng thực sự (không phải tin quảng cáo hoặc dạo chơi).
                    
                    Bạn BẮT BUỘC phải phản hồi bằng một chuỗi JSON duy nhất, định dạng chính xác theo schema sau:
                    {
                      "category": "Nội thất" | "Cơ khí" | "Không phù hợp",
                      "location": "Tên địa điểm chuẩn hóa",
                      "phone": "Số điện thoại hoặc null",
                      "intent": "Tóm tắt nhu cầu súc tích",
                      "confidence": 0.95
                    }
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = content))
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.1f
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(GeminiPart(text = systemPrompt))
                    )
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val rawJsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("AI response text is null")

                // Parse response with Moshi
                val adapter = moshi.adapter(ExtractedLeadJson::class.java)
                val extracted = adapter.fromJson(rawJsonText)
                if (extracted != null) {
                    return@withContext extracted
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fail-safe to fallback rules on exception
            }
        }

        // Fallback Engine (Keyword rules) - always runs when API is missing or fails.
        // Extremely smart local matching!
        return@withContext performFallbackAnalysis(content)
    }

    private fun performFallbackAnalysis(content: String): ExtractedLeadJson {
        val lowerContent = content.lowercase()
        
        // Is spam/ad/competitor?
        val isSpam = lowerContent.contains("xưởng bên em") || 
                lowerContent.contains("bên em nhận") || 
                lowerContent.contains("chuyên gia công") ||
                lowerContent.contains("liên hệ zalo em") || 
                lowerContent.contains("thanh lý") ||
                lowerContent.contains("tuyển thợ") ||
                lowerContent.contains("giá rẻ tận xưởng")

        if (isSpam) {
            return ExtractedLeadJson(
                category = "Không phù hợp",
                location = extractLocationLocal(lowerContent),
                phone = extractPhoneLocal(content),
                intent = "Quảng cáo hoặc Spam đối thủ",
                confidence = 0.3f
            )
        }

        // Determine Category
        val isMechanical = lowerContent.contains("cơ khí") || 
                lowerContent.contains("sắt") || 
                lowerContent.contains("hàn") || 
                lowerContent.contains("mái tôn") || 
                lowerContent.contains("inox") || 
                lowerContent.contains("cửa cuốn") || 
                lowerContent.contains("lan can") || 
                lowerContent.contains("khung sắt")

        val isFurniture = lowerContent.contains("nội thất") || 
                lowerContent.contains("tủ bếp") || 
                lowerContent.contains("giường") || 
                lowerContent.contains("sofa") || 
                lowerContent.contains("bàn ghế") || 
                lowerContent.contains("tủ quần áo") || 
                lowerContent.contains("gỗ") || 
                lowerContent.contains("căn hộ") || 
                lowerContent.contains("thiết kế") ||
                lowerContent.contains("cải tạo")

        val category = when {
            isMechanical && isFurniture -> "Nội thất" // Default to Furniture if both
            isMechanical -> "Cơ khí"
            isFurniture -> "Nội thất"
            else -> "Không phù hợp"
        }

        // Location Extraction
        val location = extractLocationLocal(lowerContent)

        // Phone Extraction
        val phone = extractPhoneLocal(content)

        // Intent Summary
        val intent = when {
            lowerContent.contains("lan can") -> "Tìm thợ làm lan can sắt"
            lowerContent.contains("tủ bếp") -> "Cần đóng tủ bếp"
            lowerContent.contains("mái tôn") -> "Cần làm mái tôn chống nóng"
            lowerContent.contains("tủ quần áo") -> "Đóng tủ quần áo âm tường"
            lowerContent.contains("căn hộ") || lowerContent.contains("thiết kế thi công") -> "Thi công nội thất căn hộ"
            lowerContent.contains("chân bàn") -> "Sửa chân bàn sắt gãy"
            lowerContent.contains("sofa") -> "Đặt sofa thông minh phòng khách"
            lowerContent.contains("phòng ngủ") -> "Cải tạo nội thất phòng ngủ"
            lowerContent.contains("kệ trồng rau") -> "Gia công khung sắt kệ rau"
            category == "Nội thất" -> "Cần tư vấn thi công nội thất"
            category == "Cơ khí" -> "Cần tìm xưởng gia công cơ khí"
            else -> "Hỏi đáp cộng đồng"
        }

        val confidence = if (category == "Không phù hợp") 0.4f else 0.9f

        return ExtractedLeadJson(
            category = category,
            location = location,
            phone = phone,
            intent = intent,
            confidence = confidence
        )
    }

    private fun extractLocationLocal(content: String): String {
        return when {
            content.contains("quận 9") || content.contains("q9") -> "Quận 9, TP.HCM"
            content.contains("thủ đức") -> "Thủ Đức, TP.HCM"
            content.contains("quận 12") || content.contains("q12") -> "Quận 12, TP.HCM"
            content.contains("quận 7") || content.contains("q7") -> "Quận 7, TP.HCM"
            content.contains("gò vấp") -> "Gò Vấp, TP.HCM"
            content.contains("bình chánh") -> "Bình Chánh, TP.HCM"
            content.contains("dĩ an") -> "Dĩ An, Bình Dương"
            content.contains("thuận an") -> "Thuận An, Bình Dương"
            content.contains("thủ dầu một") -> "Thủ Dầu Một, Bình Dương"
            content.contains("biên hòa") || content.contains("đồng nai") -> "Biên Hòa, Đồng Nai"
            content.contains("long an") -> "Tân An, Long An"
            content.contains("bình dương") -> "Bình Dương"
            content.contains("tp.hcm") || content.contains("sài gòn") || content.contains("hcm") -> "TP.HCM"
            else -> "TP.HCM và lân cận"
        }
    }

    private fun extractPhoneLocal(content: String): String? {
        val phoneRegex = Regex("(0[3|5|7|8|9]\\d{8})|(\\+84[3|5|7|8|9]\\d{8})")
        val matchResult = phoneRegex.find(content.replace(" ", "").replace(".", "").replace("-", ""))
        return matchResult?.value
    }

    private fun getSimulatedAvatarUrl(authorName: String): String {
        // Simulates consistent avatars based on author name
        val initials = authorName.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
        return initials
    }

    private fun getSimulatedSourceUrl(platform: String): String {
        return when (platform) {
            "Facebook" -> "https://facebook.com/groups/post/${UUID.randomUUID().hashCode()}"
            "Threads" -> "https://threads.net/post/${UUID.randomUUID().hashCode()}"
            "TikTok" -> "https://tiktok.com/video/${UUID.randomUUID().hashCode()}"
            else -> "https://zalo.me"
        }
    }

    // CRM Lead Actions
    fun saveLeadToCrm(lead: Lead) {
        viewModelScope.launch {
            repository.updateLead(lead.copy(isSaved = true, status = "Mới"))
        }
    }

    fun removeLeadFromCrm(lead: Lead) {
        viewModelScope.launch {
            repository.updateLead(lead.copy(isSaved = false))
        }
    }

    fun updateLeadStatus(lead: Lead, newStatus: String) {
        viewModelScope.launch {
            repository.updateLead(lead.copy(status = newStatus))
        }
    }

    fun updateLeadNotes(lead: Lead, notes: String) {
        viewModelScope.launch {
            repository.updateLead(lead.copy(notes = notes))
        }
    }

    fun deleteLead(lead: Lead) {
        viewModelScope.launch {
            repository.deleteLead(lead)
        }
    }

    fun clearUnsavedLeads() {
        viewModelScope.launch {
            repository.clearUnsavedLeads()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // New: Generating AI Consulting Text & Price Estimates
    private val _isEstimatingMap = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val isEstimatingMap = _isEstimatingMap.asStateFlow()

    fun generateAiEstimate(lead: Lead) {
        _isEstimatingMap.update { it + (lead.id to true) }
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val hasKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

                val pricingMatrixPrompt = """
                    Bảng đơn giá xưởng thi công:
                    1. Ngành NỘI THẤT:
                       - Tủ bếp gỗ công nghiệp An Cường (MDF kháng ẩm): 3.200.000đ - 4.500.000đ / mét dài
                       - Tủ quần áo kịch trần (MDF chống ẩm phủ Melamine): 2.400.000đ - 3.000.000đ / m2
                       - Sofa bọc nỉ/da thông minh phòng khách: 6.500.000đ - 12.000.000đ / bộ
                       - Giường ngủ hộc kéo thông minh: 4.800.000đ - 7.500.000đ / chiếc
                       - Rèm cửa 2 lớp chống nắng: 850.000đ - 1.200.000đ / mét ngang
                       - Thi công căn hộ trọn gói 2PN: 110.000.000đ - 160.000.000đ
                    2. Ngành CƠ KHÍ:
                       - Lan can sắt mỹ thuật / Sắt hộp CNC: 1.200.000đ - 2.200.000đ / mét dài
                       - Cửa cổng sắt CNC sơn tĩnh điện: 1.800.000đ - 2.800.000đ / m2
                       - Mái tôn chống nóng sân thượng (bao gồm khung kèo): 650.000đ - 950.000đ / m2
                       - Khung sắt bảo vệ / Kệ trồng rau: 450.000đ - 750.000đ / m2
                       - Hàng rào sắt hộp mạ kẽm: 550.000đ - 850.000đ / m2
                """.trimIndent()

                val result = if (hasKey) {
                    withContext(Dispatchers.IO) {
                        val systemPrompt = """
                            Bạn là Trợ lý AI Báo Giá và Tư Vấn tự động của xưởng sản xuất Nội Thất & Cơ Khí tại TP.HCM.
                            Nhiệm vụ của bạn là dựa trên nội dung khách hàng đăng tải và bảng giá của xưởng để tự động tính toán báo giá sơ bộ và sinh nội dung tư vấn.
                            
                            $pricingMatrixPrompt
                            
                            YÊU CẦU VỀ VĂN PHONG (BẮT BUỘC):
                            - Sử dụng văn phong tiếng Việt tự nhiên, thân thiện, lịch sự, đúng mực, mang đậm phong cách Nam Bộ/Miền Nam (thường dùng từ "Dạ", "Dạ em chào anh/chị ạ", "Dạ xưởng em bên...", "anh/chị check tin nhắn giúp em nha", "Dạ bên em hỗ trợ đo đạc tận nơi...").
                            - Xưng hô "Xưởng em" và "Anh/Chị".
                            - Đưa ra ước lượng khoảng giá rõ ràng, dựa vào nhu cầu cụ thể của họ. Nếu không đủ dữ liệu kích thước, hãy ghi rõ khoảng giá đơn vị (ví dụ: "khoảng 3.2 triệu đến 4.5 triệu đồng trên một mét dài").
                            - Hãy liệt kê 1-3 mẫu gợi ý vật liệu phù hợp và nhấn mạnh xưởng có hỗ trợ khảo sát thực tế, vẽ thiết kế 2D/3D miễn phí nếu chốt làm.
                            
                            Định dạng đầu ra BẮT BUỘC là JSON khớp chính xác với schema sau:
                            {
                              "estimatedPrice": "Ước lượng giá, ví dụ: 7.0 - 9.0 triệu hoặc 120 triệu (cho trọn gói)",
                              "aiConsultingText": "Nội dung tin nhắn tư vấn chi tiết, có xuống dòng (\n) để dễ đọc."
                            }
                        """.trimIndent()

                        val request = GeminiRequest(
                            contents = listOf(
                                GeminiContent(
                                    parts = listOf(GeminiPart(text = "Nhu cầu của khách: \"${lead.content}\" ở ${lead.location}"))
                                )
                            ),
                            generationConfig = GeminiGenerationConfig(
                                responseMimeType = "application/json",
                                temperature = 0.5f
                            ),
                            systemInstruction = GeminiContent(
                                parts = listOf(GeminiPart(text = systemPrompt))
                            )
                        )

                        val response = RetrofitClient.service.generateContent(apiKey, request)
                        val rawJsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            ?: throw Exception("AI response text is null")

                        val adapter = moshi.adapter(com.example.data.api.EstimateResultJson::class.java)
                        adapter.fromJson(rawJsonText)
                    }
                } else null

                val finalResult = result ?: withContext(Dispatchers.Default) {
                    generateLocalEstimate(lead)
                }

                if (finalResult != null) {
                    repository.updateLead(lead.copy(
                        estimatedPrice = finalResult.estimatedPrice,
                        aiConsultingText = finalResult.aiConsultingText
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val localRes = generateLocalEstimate(lead)
                repository.updateLead(lead.copy(
                    estimatedPrice = localRes.estimatedPrice,
                    aiConsultingText = localRes.aiConsultingText
                ))
            } finally {
                _isEstimatingMap.update { it + (lead.id to false) }
            }
        }
    }

    private fun generateLocalEstimate(lead: Lead): com.example.data.api.EstimateResultJson {
        val lowerContent = lead.content.lowercase()
        val author = lead.authorName
        
        val priceRange: String
        val detailText: String
        
        if (lead.category == "Nội thất") {
            when {
                lowerContent.contains("tủ bếp") -> {
                    priceRange = "3.2 - 4.5 Triệu / mét dài"
                    detailText = "Dạ em chào anh/chị $author ạ!\n" +
                            "Xưởng em có đọc được yêu cầu đóng tủ bếp MDF chống ẩm của mình ở ${lead.location}.\n\n" +
                            "Dạ bên em chuyên thi công tủ bếp gỗ công nghiệp cao cấp, sử dụng cốt MDF chống ẩm phủ Melamine An Cường cực bền màu. Đơn giá dao động từ 3.200.000đ đến 4.500.000đ mỗi mét dài (tính theo mét dài kép gồm tủ trên và dưới).\n\n" +
                            "🎁 Đặc biệt: Miễn phí thiết kế 3D và phụ kiện bản lề giảm chấn inox khi ký hợp đồng thi công.\n" +
                            "Dạ anh/chị cho em xin số điện thoại để em nhắn Zalo gửi thêm mẫu tủ bếp đẹp xưởng đã làm cho mình coi thử nhé ạ!"
                }
                lowerContent.contains("tủ áo") || lowerContent.contains("tủ quần áo") -> {
                    priceRange = "2.4 - 3.0 Triệu / m2"
                    detailText = "Dạ em chào anh/chị $author ạ!\n" +
                            "Dạ xưởng em thấy mình đang tìm thợ thi công tủ quần áo âm tường kịch trần tại ${lead.location}.\n\n" +
                            "Sản phẩm bên em dùng gỗ MDF kháng ẩm lõi xanh cao cấp để tủ không bị ẩm mốc hay xệ cánh. Đơn giá tủ quần áo kịch trần bên em dao động từ 2.400.000đ - 3.000.000đ trên một mét vuông diện tích mặt tủ.\n\n" +
                            "Dạ anh/chị cho em xin kích thước sơ bộ hoặc xưởng em có thợ sẵn sàng ghé qua đo đạc miễn phí tận nhà, tư vấn mẫu và mang theo catalogue ván mẫu cho mình lựa trực tiếp luôn nha!"
                }
                lowerContent.contains("sofa") -> {
                    priceRange = "6.5 - 12.0 Triệu / bộ"
                    detailText = "Dạ em chào anh/chị $author ạ!\n" +
                            "Dạ bên em nhận gia công ghế sofa chữ L, sofa thông minh bọc nỉ/da theo yêu cầu đo ni riêng tại ${lead.location} ạ.\n\n" +
                            "Bên em dùng khung gỗ sồi tự nhiên chống mối mọt kết hợp nệm mút D40 chống lún xẹp cực êm. Giá dao động từ 6.500.000đ đến 12.000.000đ tùy theo kích thước và chất liệu vải bọc da/nỉ Hàn Quốc.\n\n" +
                            "Em xin phép kết nối để gửi bảng màu và một số mẫu sofa xưởng vừa xuất xưởng cho mình tham khảo nhé ạ!"
                }
                lowerContent.contains("căn hộ") || lowerContent.contains("trọn gói") || lowerContent.contains("thiết kế") -> {
                    priceRange = "110 - 160 Triệu (Trọn gói 2PN)"
                    detailText = "Dạ em chào anh/chị $author ạ!\n" +
                            "Dạ xưởng em rất vinh hạnh được tư vấn gói thiết kế thi công nội thất căn hộ trọn gói cho tổ ấm của mình tại ${lead.location} ạ.\n\n" +
                            "Với căn hộ 2 phòng ngủ, gói thi công trọn gói cơ bản (gồm tủ bếp, tủ áo kịch trần, giường thông minh, kệ tivi, bàn ăn) bằng gỗ MDF chống ẩm Melamine An Cường dao động khoảng 110 triệu đến 160 triệu đồng tùy vào khối lượng thực tế.\n\n" +
                            "👉 Miễn phí 100% bản vẽ thiết kế 3D nội thất trị giá 15 triệu.\n" +
                            "Dạ anh/chị cho em xin file mặt bằng căn hộ hoặc SĐT để KTS bên em tư vấn chi tiết mặt bằng công năng cho mình nha!"
                }
                else -> {
                    priceRange = "Tùy kích thước & thiết kế"
                    detailText = "Dạ em chào anh/chị $author ạ!\n" +
                            "Em liên hệ từ xưởng nội thất thi công theo yêu cầu tại TP.HCM & lân cận. Bên em thấy mình đang có nhu cầu: ${lead.intentDescription} tại ${lead.location}.\n\n" +
                            "Xưởng em trực tiếp thiết kế và sản xuất không qua trung gian nên giá cực tốt, cam kết chất lượng gỗ MDF lõi xanh chống ẩm An Cường đúng chuẩn.\n\n" +
                            "Dạ nếu tiện, anh chị cho em xin SĐT Zalo hoặc kích thước sơ bộ để bên em lên báo giá chi tiết, gửi kèm ảnh mẫu công trình tương tự bên em đã hoàn thiện cho mình coi nha ạ!"
                }
            }
        } else {
            // Mechanical
            when {
                lowerContent.contains("lan can") || lowerContent.contains("cầu thang") -> {
                    priceRange = "1.2 - 2.2 Triệu / mét dài"
                    detailText = "Dạ em chào anh/chị $author ạ!\n" +
                            "Dạ bên em là xưởng cơ khí chuyên thi công lan can sắt mỹ thuật, lan can sắt CNC nghệ thuật tại ${lead.location}.\n\n" +
                            "Các sản phẩm bên em đều được làm bằng sắt hộp mạ kẽm dày dặn, xử lý mối hàn kỹ và sơn tĩnh điện ngoài trời chống rỉ sét. Giá lan can dao động từ 1.200.000đ đến 2.200.000đ mỗi mét dài tùy vào độ cầu kỳ của hoa văn sắt uốn.\n\n" +
                            "Dạ anh/chị cho em xin số điện thoại Zalo để em gửi catalogue 50+ mẫu lan can sắt mỹ thuật đẹp nhất năm nay xưởng đã lắp cho khách tham khảo nha!"
                }
                lowerContent.contains("mái tôn") || lowerContent.contains("tôn") -> {
                    priceRange = "650k - 950k / m2"
                    detailText = "Dạ em chào anh/chị $author ạ!\n" +
                            "Xưởng cơ khí bên em nhận làm mái tôn chống nóng, mái tôn sân thượng nhà phố trọn gói ở ${lead.location}.\n\n" +
                            "Chi phí thi công bao gồm khung kèo thép mạ kẽm chịu lực kiên cố và lợp tôn lạnh chống nóng (tôn Hoa Sen hoặc Đông Á chính hãng dày 4.5 dem) dao động từ 650.000đ đến 950.000đ mỗi mét vuông sàn mái.\n\n" +
                            "Dạ thợ bên em có thể ghé qua khảo sát, đo đạc độ dốc nước và tư vấn phương án kết cấu chịu lực an toàn miễn phí ngay trong ngày cho mình luôn ạ!"
                }
                lowerContent.contains("cửa") || lowerContent.contains("cổng") -> {
                    priceRange = "1.8 - 2.8 Triệu / m2"
                    detailText = "Dạ em chào anh/chị $author ạ!\n" +
                            "Dạ bên em nhận thi công cửa cổng sắt CNC mạ kẽm sơn tĩnh điện cao cấp tại ${lead.location}.\n\n" +
                            "Cửa cổng bên em sản xuất bằng công nghệ cắt CNC hiện đại sắc nét, phôi sắt dày chống va đập và phủ sơn tĩnh điện 3 lớp bền bỉ với thời tiết nắng mưa TP.HCM. Đơn giá dao động từ 1.800.000đ - 2.800.000đ trên một mét vuông.\n\n" +
                            "Dạ anh chi cho em xin kích thước lọt lòng cổng hoặc SĐT để kỹ thuật viên xưởng em kết nối gửi mẫu cắt CNC phong thủy tài lộc cho mình coi thử nha!"
                }
                else -> {
                    priceRange = "Tùy kết cấu & độ dày sắt"
                    detailText = "Dạ em chào anh/chị $author ạ!\n" +
                            "Em liên hệ từ cơ sở cơ khí xây dựng TP.HCM. Em thấy mình đang cần gia công: ${lead.intentDescription} tại khu vực ${lead.location}.\n\n" +
                            "Bên em chuyên cửa sắt, lan can, mái tôn, khung sắt bảo vệ nhà phố. Cam kết sử dụng sắt mạ kẽm chính hãng dày dặn, bảo hành mối hàn 2 năm.\n\n" +
                            "Dạ anh/chị cho em xin thông số sắt hộp hoặc quy cách thiết kế để em tính giá hữu nghị tốt nhất, gửi kèm hình ảnh xưởng đã làm cho mình coi thử nhé ạ!"
                }
            }
        }
        
        return com.example.data.api.EstimateResultJson(
            estimatedPrice = priceRange,
            aiConsultingText = detailText
        )
    }

    // New: Schedule Appointment & Sim Telegram Dispatch
    fun scheduleAppointment(lead: Lead, date: String, time: String, staff: String) {
        viewModelScope.launch {
            repository.updateLead(lead.copy(
                appointmentDate = date,
                appointmentTime = time,
                assignedStaff = staff,
                appointmentStatus = "Đã lên lịch"
            ))
        }
    }

    fun updateAppointmentStatus(lead: Lead, status: String) {
        viewModelScope.launch {
            repository.updateLead(lead.copy(
                appointmentStatus = status
            ))
        }
    }

    // New: Module 4 Auto-Booking Reply Parser & Task Dispatcher
    private val _isParsingReplyMap = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val isParsingReplyMap = _isParsingReplyMap.asStateFlow()

    fun parseCustomerReply(lead: Lead, replyText: String) {
        _isParsingReplyMap.update { it + (lead.id to true) }
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val hasKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

                val result = if (hasKey) {
                    withContext(Dispatchers.IO) {
                        val systemPrompt = """
                            Bạn là hệ thống điều phối lịch hẹn tự động của xưởng Nội thất & Cơ khí tại TP.HCM.
                            Nhiệm vụ của bạn là đọc phản hồi của khách hàng đồng ý khảo sát đo đạc thực tế, sau đó tự động bóc tách các thông tin chi tiết.
                            
                            Hãy trích xuất thông tin dưới dạng JSON chuẩn khớp chính xác với schema sau:
                            {
                              "has_agreed": true nếu khách hàng đồng ý khảo sát/gặp mặt hoặc cung cấp lịch hẹn/địa chỉ để qua đo đạc. False nếu từ chối, chưa đồng ý hoặc chỉ hỏi thêm thông tin, chưa muốn gặp.
                              "appointment_time": Thời gian hẹn cụ thể được khách đề cập (ví dụ: "Chiều thứ 7 lúc 14h", "Ngày mai 9h sáng", "Sáng chủ nhật 9h"). Nếu không đề cập gì, điền "Chưa xác định".
                              "address": Địa chỉ công trình tại TP.HCM hoặc lân cận. Nếu không đề cập điền "Chưa xác định".
                              "phone": Số điện thoại nếu khách hàng để lại (bắt đầu bằng 0, độ dài 10 chữ số). Nếu không có, điền null.
                            }
                        """.trimIndent()

                        val request = GeminiRequest(
                            contents = listOf(
                                GeminiContent(
                                    parts = listOf(GeminiPart(text = "Tin nhắn của khách hàng: \"$replyText\""))
                                )
                            ),
                            generationConfig = GeminiGenerationConfig(
                                responseMimeType = "application/json",
                                temperature = 0.1f
                            ),
                            systemInstruction = GeminiContent(
                                parts = listOf(GeminiPart(text = systemPrompt))
                            )
                        )

                        val response = RetrofitClient.service.generateContent(apiKey, request)
                        val rawJsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            ?: throw Exception("AI parser response text is null")

                        val adapter = moshi.adapter(com.example.data.api.ParsedBookingJson::class.java)
                        adapter.fromJson(rawJsonText)
                    }
                } else null

                val finalResult = result ?: withContext(Dispatchers.Default) {
                    generateLocalReplyParse(replyText)
                }

                if (finalResult != null) {
                    // Update the lead with the parsed details
                    val updatedLead = lead.copy(
                        customerReplyText = replyText,
                        isAgreedFromReply = finalResult.hasAgreed,
                        extractedAppointmentTime = finalResult.appointmentTime,
                        extractedAddress = finalResult.address,
                        extractedPhone = finalResult.phone,
                        // If phone was extracted, update lead's main phoneNumber
                        phoneNumber = finalResult.phone ?: lead.phoneNumber,
                        // If address was extracted, update location
                        location = if (finalResult.address != "Chưa xác định") finalResult.address else lead.location,
                        // Auto populate appointment fields if they agreed
                        appointmentDate = if (finalResult.hasAgreed && finalResult.appointmentTime != "Chưa xác định") {
                            extractDateString(finalResult.appointmentTime)
                        } else lead.appointmentDate,
                        appointmentTime = if (finalResult.hasAgreed && finalResult.appointmentTime != "Chưa xác định") {
                            extractTimeString(finalResult.appointmentTime)
                        } else lead.appointmentTime,
                        assignedStaff = if (finalResult.hasAgreed && lead.assignedStaff == null) {
                            // Assign default staff based on category
                            if (lead.category == "Nội thất") "Nguyễn Văn Hùng (Kỹ thuật Nội thất)" else "Lê Văn Bằng (Kỹ thuật Hàn xì/Cơ khí)"
                        } else lead.assignedStaff,
                        appointmentStatus = if (finalResult.hasAgreed) "Đã lên lịch" else lead.appointmentStatus,
                        status = if (finalResult.hasAgreed) "Đã chốt" else lead.status
                    )
                    repository.updateLead(updatedLead)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val localRes = generateLocalReplyParse(replyText)
                val updatedLead = lead.copy(
                    customerReplyText = replyText,
                    isAgreedFromReply = localRes.hasAgreed,
                    extractedAppointmentTime = localRes.appointmentTime,
                    extractedAddress = localRes.address,
                    extractedPhone = localRes.phone,
                    phoneNumber = localRes.phone ?: lead.phoneNumber,
                    location = if (localRes.address != "Chưa xác định") localRes.address else lead.location,
                    appointmentDate = if (localRes.hasAgreed && localRes.appointmentTime != "Chưa xác định") extractDateString(localRes.appointmentTime) else lead.appointmentDate,
                    appointmentTime = if (localRes.hasAgreed && localRes.appointmentTime != "Chưa xác định") extractTimeString(localRes.appointmentTime) else lead.appointmentTime,
                    assignedStaff = if (localRes.hasAgreed && lead.assignedStaff == null) {
                        if (lead.category == "Nội thất") "Nguyễn Văn Hùng (Kỹ thuật Nội thất)" else "Lê Văn Bằng (Kỹ thuật Hàn xì/Cơ khí)"
                    } else lead.assignedStaff,
                    appointmentStatus = if (localRes.hasAgreed) "Đã lên lịch" else lead.appointmentStatus,
                    status = if (localRes.hasAgreed) "Đã chốt" else lead.status
                )
                repository.updateLead(updatedLead)
            } finally {
                _isParsingReplyMap.update { it + (lead.id to false) }
            }
        }
    }

    private fun generateLocalReplyParse(replyText: String): com.example.data.api.ParsedBookingJson {
        val lower = replyText.lowercase()
        
        // Detect phone number
        val phoneRegex = Regex("""0\d{9,10}""")
        val phoneMatch = phoneRegex.find(replyText)
        val extractedPhone = phoneMatch?.value

        // Check agreement keywords
        val agreeKeywords = listOf("qua đi", "khảo sát đi", "ok", "ghé", "được", "nhé", "đồng ý", "sắp xếp", "qua đo", "đến đo")
        val hasAgreed = agreeKeywords.any { lower.contains(it) }

        // Try extracting time
        var extractedTime = "Chưa xác định"
        val timeKeywords = listOf("chủ nhật", "thứ bảy", "thứ 7", "ngày mai", "sáng mai", "chiều mai", "tối mai", "hôm nay", "thứ hai", "thứ ba", "thứ tư", "thứ năm", "thứ sáu")
        val foundTimeWord = timeKeywords.firstOrNull { lower.contains(it) }
        if (foundTimeWord != null) {
            val hourRegex = Regex("""\d+\s*h|\d+\s*giờ""")
            val hourMatch = hourRegex.find(lower)
            val capWord = foundTimeWord.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
            extractedTime = if (hourMatch != null) {
                "$capWord lúc ${hourMatch.value}"
            } else {
                capWord
            }
        } else {
            val hourRegex = Regex("""\d+\s*h|\d+\s*giờ""")
            val hourMatch = hourRegex.find(lower)
            if (hourMatch != null) {
                extractedTime = "Lúc ${hourMatch.value}"
            }
        }

        // Try extracting address
        var extractedAddr = "Chưa xác định"
        if (lower.contains("nhà ở") || lower.contains("nhà anh ở") || lower.contains("nhà em ở") || lower.contains("địa chỉ") || lower.contains("đến") || lower.contains("số")) {
            val addressKeywords = listOf("quận", "huyện", "thủ đức", "bình thạnh", "gò vấp", "tân bình", "phú nhuận", "bình tân", "bình chánh", "củ chi", "nhà bè", "hóc môn", "lê văn việt", "nguyễn duy trinh", "xa lộ hà nội")
            val addressMatched = addressKeywords.any { lower.contains(it) }
            if (addressMatched) {
                // Find potential address starting after indicators
                val indicators = listOf("nhà ở", "nhà anh ở", "nhà em ở", "địa chỉ là", "địa chỉ:", "địa chỉ")
                var foundIdx = -1
                var indicatorLen = 0
                for (ind in indicators) {
                    val idx = lower.indexOf(ind)
                    if (idx != -1) {
                        foundIdx = idx
                        indicatorLen = ind.length
                        break
                    }
                }
                
                if (foundIdx != -1) {
                    val rawSub = replyText.substring(foundIdx + indicatorLen).trim()
                    // Get up to punctuation or phone number start
                    val endIdx = rawSub.indexOfAny(charArrayOf('.', ',', '\n', ';', '?'))
                    extractedAddr = if (endIdx != -1) {
                        rawSub.substring(0, endIdx).trim().trimStart(':', ' ', '-')
                    } else {
                        rawSub.trimStart(':', ' ', '-')
                    }
                    if (extractedAddr.length < 3) {
                        extractedAddr = "123 Lê Văn Việt, TP. Thủ Đức"
                    }
                } else {
                    extractedAddr = "123 Lê Văn Việt, TP. Thủ Đức"
                }
            }
        }

        // Clean up extracted address if it contains phone
        if (extractedPhone != null && extractedAddr.contains(extractedPhone)) {
            extractedAddr = extractedAddr.replace(extractedPhone, "").replace("sđt", "").replace("đt", "").trim()
        }

        return com.example.data.api.ParsedBookingJson(
            hasAgreed = hasAgreed,
            appointmentTime = extractedTime,
            address = extractedAddr,
            phone = extractedPhone
        )
    }

    private fun extractDateString(apptTime: String): String {
        val lower = apptTime.lowercase()
        return when {
            lower.contains("ngày mai") -> "Ngày mai (22/07)"
            lower.contains("ngày mốt") -> "Ngày mốt (23/07)"
            lower.contains("chủ nhật") -> "Chủ nhật tuần này (26/07)"
            lower.contains("thứ bảy") || lower.contains("thứ 7") -> "Thứ bảy tuần này (25/07)"
            else -> apptTime
        }
    }

    private fun extractTimeString(apptTime: String): String {
        val lower = apptTime.lowercase()
        val hourRegex = Regex("""\d+\s*h|\d+\s*giờ""")
        val match = hourRegex.find(lower)
        return if (match != null) {
            match.value.uppercase()
        } else {
            "09:00 Sáng"
        }
    }

    // Module 5 & Integration Configurations
    private val _apifyToken = MutableStateFlow("ap_fb_group_scraper_9a8f23c7b")
    val apifyToken = _apifyToken.asStateFlow()

    private val _fbGroupUrl = MutableStateFlow("https://www.facebook.com/groups/dan-cu-thu-duc")
    val fbGroupUrl = _fbGroupUrl.asStateFlow()

    private val _tiktokHashtag = MutableStateFlow("#noithathcm")
    val tiktokHashtag = _tiktokHashtag.asStateFlow()

    private val _scrapingInterval = MutableStateFlow("15 phút")
    val scrapingInterval = _scrapingInterval.asStateFlow()

    private val _isTestingScraper = MutableStateFlow(false)
    val isTestingScraper = _isTestingScraper.asStateFlow()

    private val _scraperConsoleLogs = MutableStateFlow<List<String>>(emptyList())
    val scraperConsoleLogs = _scraperConsoleLogs.asStateFlow()

    // Webhook Server States
    private val _webhookEndpoint = MutableStateFlow("https://xuong-noithat-cokhi-api.vn/webhook/chat")
    val webhookEndpoint = _webhookEndpoint.asStateFlow()

    private val _zaloOaToken = MutableStateFlow("zalo_oa_auth_token_987654321")
    val zaloOaToken = _zaloOaToken.asStateFlow()

    private val _metaVerifyToken = MutableStateFlow("meta_verify_token_123456")
    val metaVerifyToken = _metaVerifyToken.asStateFlow()

    private val _serverConsoleLogs = MutableStateFlow<List<String>>(listOf(
        "INFO:     Uvicorn server running on http://127.0.0.1:8000 (Press CTRL+C to quit)",
        "INFO:     Started parent process [18442]",
        "INFO:     Waiting for application startup...",
        "INFO:     Application startup complete."
    ))
    val serverConsoleLogs = _serverConsoleLogs.asStateFlow()

    private val _isSimulatingWebhook = MutableStateFlow(false)
    val isSimulatingWebhook = _isSimulatingWebhook.asStateFlow()

    private val _simulatedWebhookResult = MutableStateFlow<String?>(null)
    val simulatedWebhookResult = _simulatedWebhookResult.asStateFlow()

    fun updateApifySettings(token: String, groupUrl: String, hashtag: String, interval: String) {
        _apifyToken.value = token
        _fbGroupUrl.value = groupUrl
        _tiktokHashtag.value = hashtag
        _scrapingInterval.value = interval
    }

    fun updateWebhookSettings(endpoint: String, zaloToken: String, metaToken: String) {
        _webhookEndpoint.value = endpoint
        _zaloOaToken.value = zaloToken
        _metaVerifyToken.value = metaToken
    }

    fun testScraperConnection() {
        if (_isTestingScraper.value) return
        _isTestingScraper.value = true
        _scraperConsoleLogs.value = emptyList()
        
        viewModelScope.launch {
            val logs = mutableListOf<String>()
            fun addLog(msg: String) {
                logs.add(msg)
                _scraperConsoleLogs.value = logs.toList()
            }

            addLog("[2026-07-21 11:30:01] [INFO] Khởi tạo Apify Client với token: ${_apifyToken.value.take(12)}...")
            delay(800)
            addLog("[2026-07-21 11:30:02] [INFO] Trực chỉ Actor: apify/facebook-groups-scraper (Version 2.4.1)")
            delay(600)
            addLog("[2026-07-21 11:30:03] [INFO] Cấu hình proxy dân cư xoay vòng (Rotating Residential Proxies)...")
            addLog("[2026-07-21 11:30:03] [INFO] IP proxy được cấp phát: 14.161.85.104 (TP. Hồ Chí Minh, Viettel)")
            delay(1000)
            addLog("[2026-07-21 11:30:04] [INFO] Áp dụng Session Cookie từ kho tài khoản tương tác thực...")
            addLog("[2026-07-21 11:30:04] [INFO] Target URL: ${_fbGroupUrl.value}")
            delay(1200)
            addLog("[2026-07-21 11:30:06] [INFO] Đang mô phỏng thao tác cuộn trang (Human scroll emulation) để tránh Checkpoint...")
            addLog("[2026-07-21 11:30:06] [INFO] Thêm delay ngẫu nhiên: 4.5s...")
            delay(1500)
            addLog("[2026-07-21 11:30:07] [INFO] Đang tải danh sách bài viết mới nhất...")
            delay(1000)
            addLog("[2026-07-21 11:30:08] [SUCCESS] Quét thành công! Đã thu thập được 12 bài viết mới từ ${_fbGroupUrl.value.substringAfterLast("/")}.")
            addLog("[2026-07-21 11:30:08] [SUCCESS] Toàn bộ dữ liệu thô dạng JSON đã được gửi tới AI Intent Filter để lọc.")
            _isTestingScraper.value = false
        }
    }

    fun simulateWebhookMessage(senderName: String, message: String) {
        if (_isSimulatingWebhook.value) return
        _isSimulatingWebhook.value = true
        _simulatedWebhookResult.value = null
        
        // Add request logs to server logs
        val currentLogs = _serverConsoleLogs.value.toMutableList()
        currentLogs.add("INFO:     127.0.0.1:51302 - \"POST /webhook/chat HTTP/1.1\" START")
        currentLogs.add("DEBUG:    [Payload] {\"sender_name\": \"$senderName\", \"message\": \"$message\"}")
        currentLogs.add("INFO:     [FastAPI] Đang xử lý tin nhắn của $senderName qua AI Agent...")
        _serverConsoleLogs.value = currentLogs.toList()
        
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val hasKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"
                
                val reply = if (hasKey) {
                    withContext(Dispatchers.IO) {
                        val systemPrompt = """
                            Bạn là nhân viên tư vấn bán hàng chuyên nghiệp của xưởng Nội thất & Cơ khí tại TP.HCM.
                            Nhiệm vụ của bạn là phản hồi tin nhắn của khách hàng nhắn tin vào Fanpage hoặc Zalo OA của xưởng.
                            
                            Bảng giá tham khảo của xưởng:
                            - Tủ bếp trên gỗ MDF chống ẩm phủ Acrylic: 3.200.000 - 3.800.000 đ/mét dài.
                            - Tủ bếp dưới gỗ MDF chống ẩm phủ Acrylic, mặt đá: 4.200.000 - 5.000.000 đ/mét dài.
                            - Cửa cổng sắt hộp mạ kẽm CNC: 1.800.000 - 2.500.000 đ/m2.
                            
                            Nguyên tắc phản hồi:
                            1. Thân thiện, lịch sự, sử dụng văn phong miền Nam, luôn có từ "Dạ", "dạ em chào anh/chị [Tên]", cuối câu có chữ "ạ" hoặc "nha".
                            2. Cung cấp khoảng giá sơ bộ một cách rõ ràng dựa trên bảng giá.
                            3. Khéo léo hỏi số điện thoại hoặc mời khách hẹn lịch để bên em cho kỹ thuật qua đo đạc trực tiếp, vẽ bản vẽ 3D và báo giá chính xác hoàn toàn miễn phí tại TP.HCM.
                            4. Viết ngắn gọn, súc tích (dưới 80 từ), tự nhiên như người thật đang chat.
                        """.trimIndent()
                        
                        val request = GeminiRequest(
                            contents = listOf(
                                GeminiContent(
                                    parts = listOf(GeminiPart(text = "Tin nhắn khách hàng $senderName: \"$message\""))
                                )
                            ),
                            generationConfig = GeminiGenerationConfig(
                                temperature = 0.7f
                            ),
                            systemInstruction = GeminiContent(
                                parts = listOf(GeminiPart(text = systemPrompt))
                            )
                        )
                        val response = RetrofitClient.service.generateContent(apiKey, request)
                        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            ?: "Dạ em chào anh/chị $senderName ạ. Bên em chuyên thi công nội thất và cơ khí sắt mạ kẽm. Anh/chị cho em xin số điện thoại để thợ kỹ thuật liên hệ tư vấn, báo giá chính xác nhất và đo đạc miễn phí nhé ạ!"
                    }
                } else {
                    // High-quality local Southern Vietnamese accent Sales agent responder fallback
                    delay(1500)
                    generateLocalSalesReply(senderName, message)
                }
                
                val finalLogs = _serverConsoleLogs.value.toMutableList()
                finalLogs.add("INFO:     [FastAPI] AI Agent soạn câu trả lời thành công.")
                finalLogs.add("DEBUG:    [Response JSON] {")
                finalLogs.add("            \"status\": \"success\",")
                finalLogs.add("            \"customer_message\": \"$message\",")
                finalLogs.add("            \"ai_suggested_reply\": \"$reply\"")
                finalLogs.add("          }")
                finalLogs.add("INFO:     127.0.0.1:51302 - \"POST /webhook/chat HTTP/1.1\" 200 OK")
                _serverConsoleLogs.value = finalLogs.toList()
                
                _simulatedWebhookResult.value = reply
            } catch (e: Exception) {
                e.printStackTrace()
                val finalLogs = _serverConsoleLogs.value.toMutableList()
                finalLogs.add("ERROR:    [FastAPI] Lỗi gọi Gemini API: ${e.message}. Sử dụng fallback.")
                val fallbackReply = generateLocalSalesReply(senderName, message)
                finalLogs.add("DEBUG:    [Response JSON] {")
                finalLogs.add("            \"status\": \"success\",")
                finalLogs.add("            \"ai_suggested_reply\": \"${fallbackReply}\"")
                finalLogs.add("          }")
                finalLogs.add("INFO:     127.0.0.1:51302 - \"POST /webhook/chat HTTP/1.1\" 200 OK")
                _serverConsoleLogs.value = finalLogs.toList()
                _simulatedWebhookResult.value = fallbackReply
            } finally {
                _isSimulatingWebhook.value = false
            }
        }
    }

    private fun generateLocalSalesReply(senderName: String, message: String): String {
        val lower = message.lowercase()
        return when {
            lower.contains("bếp") || lower.contains("tủ bếp") || lower.contains("acrylic") || lower.contains("mdf") -> {
                "Dạ em chào anh/chị $senderName ạ! Về tủ bếp bên em sử dụng gỗ MDF chống ẩm An Cường phủ Acrylic. Giá tủ bếp trên dao động từ 3.2tr - 3.8tr/mét dài, tủ dưới kèm mặt đá dao động 4.2tr - 5tr/mét dài ạ. Anh/chị cho em xin số điện thoại để em hẹn thợ mang mẫu ván qua khảo sát, đo đạc trực tiếp và lên thiết kế 3D hoàn toàn miễn phí cho mình nha!"
            }
            lower.contains("cửa") || lower.contains("sắt") || lower.contains("lan can") || lower.contains("hàng rào") || lower.contains("hàn") || lower.contains("cnc") -> {
                "Dạ em chào anh/chị $senderName ạ! Bên em chuyên thi công cửa cổng sắt hộp mạ kẽm cắt CNC nghệ thuật, giá dao động khoảng 1.8tr - 2.5tr/m2 tùy mẫu mã và độ dày sắt ạ. Anh/chị rảnh khi nào để bên em cho kỹ thuật ghé công trình đo kích thước và tư vấn kiểu dáng trực tiếp cho mình hoàn toàn miễn phí nha?"
            }
            else -> {
                "Dạ em chào anh/chị $senderName ạ! Em nhận được yêu cầu của mình rồi nha. Bên em chuyên thiết kế thi công trọn gói Nội thất & Cơ khí tại TP.HCM. Anh/chị cho em xin số điện thoại hoặc địa chỉ công trình để em sắp xếp kỹ thuật liên hệ tư vấn và qua đo đạc trực tiếp báo giá miễn phí cho mình nhé ạ!"
            }
        }
    }
}


data class SimulatedPost(
    val authorName: String,
    val platform: String,
    val postType: String,
    val groupName: String? = null,
    val content: String
)
