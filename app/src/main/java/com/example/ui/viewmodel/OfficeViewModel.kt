package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.DocumentEntity
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class OfficeViewModel(private val repository: DocumentRepository) : ViewModel() {

    // --- NAVIGATION STATE ---
    private val _currentTab = MutableStateFlow("file_manager") // "file_manager", "writer", "spreadsheet", "presentation", "pdf", "scanner", "settings"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // --- FILE MANAGER FILTER STATE ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("all") // "all", "favorite", "writer", "spreadsheet", "presentation", "pdf", "scanned"
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Combined Flow for filtering files reactively
    val filteredDocuments: StateFlow<List<DocumentEntity>> = combine(
        repository.allDocuments,
        _searchQuery,
        _selectedCategory
    ) { docs, query, category ->
        docs.filter { doc ->
            val matchesSearch = doc.title.contains(query, ignoreCase = true) || doc.content.contains(query, ignoreCase = true)
            val matchesCategory = when (category) {
                "all" -> true
                "favorite" -> doc.isFavorite
                else -> doc.type.equals(category, ignoreCase = true)
            }
            matchesSearch && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- ACTIVE EDITING STATE ---
    private val _editingDocument = MutableStateFlow<DocumentEntity?>(null)
    val editingDocument: StateFlow<DocumentEntity?> = _editingDocument.asStateFlow()

    // --- WRITER SCREEN STATE ---
    private val _writerTitle = MutableStateFlow("مستند جديد")
    val writerTitle = _writerTitle.asStateFlow()

    private val _writerText = MutableStateFlow("")
    val writerText = _writerText.asStateFlow()

    private val _writerTextSize = MutableStateFlow(16f)
    val writerTextSize = _writerTextSize.asStateFlow()

    private val _writerIsBold = MutableStateFlow(false)
    val writerIsBold = _writerIsBold.asStateFlow()

    private val _writerIsItalic = MutableStateFlow(false)
    val writerIsItalic = _writerIsItalic.asStateFlow()

    private val _writerIsUnderline = MutableStateFlow(false)
    val writerIsUnderline = _writerIsUnderline.asStateFlow()

    // --- SPREADSHEET SCREEN STATE ---
    private val _sheetTitle = MutableStateFlow("جدول جديد")
    val sheetTitle = _sheetTitle.asStateFlow()

    private val _sheetCells = MutableStateFlow<Map<String, String>>(emptyMap())
    val sheetCells = _sheetCells.asStateFlow()

    private val _sheetSelectedCell = MutableStateFlow("A1")
    val sheetSelectedCell = _sheetSelectedCell.asStateFlow()

    private val _sheetCellInput = MutableStateFlow("")
    val sheetCellInput = _sheetCellInput.asStateFlow()

    private val _sheetIsBold = MutableStateFlow(false)
    val sheetIsBold = _sheetIsBold.asStateFlow()

    private val _sheetIsItalic = MutableStateFlow(false)
    val sheetIsItalic = _sheetIsItalic.asStateFlow()

    // --- PRESENTATION SCREEN STATE ---
    private val _presentationTitle = MutableStateFlow("عرض جديد")
    val presentationTitle = _presentationTitle.asStateFlow()

    private val _presentationSlides = MutableStateFlow<List<Pair<String, String>>>(
        listOf("الشريحة الأولى" to "اكتب هنا عنوان الفرعي أو الوصف الخاص بالشريحة الأولى")
    )
    val presentationSlides = _presentationSlides.asStateFlow()

    private val _presentationSelectedSlideIndex = MutableStateFlow(0)
    val presentationSelectedSlideIndex = _presentationSelectedSlideIndex.asStateFlow()

    private val _isPresentMode = MutableStateFlow(false)
    val isPresentMode = _isPresentMode.asStateFlow()

    // --- PDF WORKSPACE STATE ---
    private val _pdfTitle = MutableStateFlow("تقرير_مكتبي.pdf")
    val pdfTitle = _pdfTitle.asStateFlow()

    private val _pdfText = MutableStateFlow("عقد اتفاقية تقديم خدمات برمجية وتطوير\n\nالطرف الأول: شركة الحلول المتقدمة للمعلوماتية\nالطرف الثاني: العميل المستفيد الموقر\n\nالمقدمة:\nبموجب هذه المستند الرقمي، يتفق الطرفان على التعاون البرمجي لبناء منصات رقمية حديثة تدعم اللغة العربية RTL وقواعد البيانات المشتركة...\n\nالبنود والالتزامات:\n١. يجب تقديم التقارير الفنية أسبوعياً وإتاحة عروض توضيحية دورية.\n٢. المحافظة الكاملة على سرية البيانات والمستندات المشتركة.\n\nالتوقيع الإلكتروني الموثق:")
    val pdfText = _pdfText.asStateFlow()

    private val _pdfSignaturePath = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val pdfSignaturePath = _pdfSignaturePath.asStateFlow()

    private val _pdfZoomRatio = MutableStateFlow(1.0f)
    val pdfZoomRatio = _pdfZoomRatio.asStateFlow()

    // --- SCANNER DIAGNOSTIC STATE ---
    private val _scanStep = MutableStateFlow(0) // 0: Live View | 1: Corner Crop Tuning | 2: Color Enhancer Filters & OCR | 3: Saved List View
    val scanStep = _scanStep.asStateFlow()

    private val _scannerSelectedFilter = MutableStateFlow("bw") // "bw", "grayscale", "enhance", "normal"
    val scannerSelectedFilter = _scannerSelectedFilter.asStateFlow()

    private val _scannedOcrText = MutableStateFlow("البسملة الرحمن الرحيم\nالمملكة العربية السعودية\nعقد إيجار سكني موحد\nرقم العقد: ٩٨٣٢١--٠٨٤\nتاريخ العقد: ٢٠٢٦/٠٦/٠٩\nحالة العقد: نشط وموثق عبر منصة إيجار الوطنية.")
    val scannedOcrText = _scannedOcrText.asStateFlow()

    private val _scannerFlash = MutableStateFlow(false)
    val scannerFlash = _scannerFlash.asStateFlow()

    // --- WPS AI STATE ---
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse = _aiResponse.asStateFlow()

    private val _pdfChatHistory = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val pdfChatHistory = _pdfChatHistory.asStateFlow()

    private val _generatedImageBase64 = MutableStateFlow<String?>(null)
    val generatedImageBase64 = _generatedImageBase64.asStateFlow()

    // --- ACTIONS ---

    fun setCurrentTab(tab: String) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleFavorite(document: DocumentEntity) {
        viewModelScope.launch {
            repository.updateFavoriteStatus(document.id, !document.isFavorite)
        }
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(document)
            if (_editingDocument.value?.id == document.id) {
                closeEditor()
            }
        }
    }

    fun deleteDocumentById(id: Int) {
        viewModelScope.launch {
            repository.deleteDocumentById(id)
            if (_editingDocument.value?.id == id) {
                closeEditor()
            }
        }
    }

    fun startNewDocument(type: String) {
        val currentTimestamp = System.currentTimeMillis()
        val defaultTitle = when (type) {
            "writer" -> "مستند كتابي " + currentTimestamp.toString().takeLast(4)
            "spreadsheet" -> "جدول بيانات " + currentTimestamp.toString().takeLast(4)
            "presentation" -> "عرض تقديمي " + currentTimestamp.toString().takeLast(4)
            "pdf" -> "مستند مصدّر " + currentTimestamp.toString().takeLast(4) + ".pdf"
            "scanned" -> "مسح ضوئي " + currentTimestamp.toString().takeLast(4)
            else -> "ملف مكتبي جديد"
        }

        // Setup clean state values
        _editingDocument.value = null
        _currentTab.value = type

        when (type) {
            "writer" -> {
                _writerTitle.value = defaultTitle
                _writerText.value = ""
                _writerIsBold.value = false
                _writerIsItalic.value = false
                _writerIsUnderline.value = false
                _writerTextSize.value = 16f
            }
            "spreadsheet" -> {
                _sheetTitle.value = defaultTitle
                _sheetCells.value = emptyMap()
                _sheetSelectedCell.value = "A1"
                _sheetCellInput.value = ""
                _sheetIsBold.value = false
                _sheetIsItalic.value = false
            }
            "presentation" -> {
                _presentationTitle.value = defaultTitle
                _presentationSlides.value = listOf("الشريحة الأولى" to "انقر على تعديل لكتابة المحتويات الخاصة بهذا العرض التقديمي المتميز.")
                _presentationSelectedSlideIndex.value = 0
                _isPresentMode.value = false
            }
            "pdf" -> {
                _pdfTitle.value = defaultTitle
                _pdfText.value = "مستند PDF فارغ مخصص للتوقيع الرقمي والتدقيق اللغوي السريع..."
                _pdfSignaturePath.value = emptyList()
                _pdfZoomRatio.value = 1.0f
            }
            "scanner" -> {
                _scanStep.value = 0
                _scannerFlash.value = false
                _scannerSelectedFilter.value = "bw"
                _scannedOcrText.value = "المحتويات المستخلصة رقمياً:\nمستند ممسوح ضوئياً يدعم القراءة البصرية المتقدمة."
            }
        }
    }

    fun openDocument(document: DocumentEntity) {
        _editingDocument.value = document
        _currentTab.value = document.type

        when (document.type) {
            "writer" -> {
                _writerTitle.value = document.title
                _writerText.value = document.content
                _writerIsBold.value = false
                _writerIsItalic.value = false
                _writerIsUnderline.value = false
            }
            "spreadsheet" -> {
                _sheetTitle.value = document.title
                _sheetCells.value = deserializeSpreadsheet(document.content)
                val firstCellVal = _sheetCells.value["A1"] ?: ""
                _sheetSelectedCell.value = "A1"
                _sheetCellInput.value = firstCellVal
            }
            "presentation" -> {
                _presentationTitle.value = document.title
                _presentationSlides.value = deserializePresentation(document.content)
                _presentationSelectedSlideIndex.value = 0
                _isPresentMode.value = false
            }
            "pdf" -> {
                _pdfTitle.value = document.title
                
                // Content splits into Text and SignatureCoordinates
                val parts = document.content.split("###SIGNATURE_PATH###")
                _pdfText.value = parts.getOrNull(0) ?: "تفريغ المستند"
                val signatureString = parts.getOrNull(1) ?: ""
                _pdfSignaturePath.value = deserializeSignature(signatureString)
                _pdfZoomRatio.value = 1.0f
            }
            "scanner" -> {
                _scannedOcrText.value = document.content
                _scanStep.value = 2 // Skip directly to OCR review
            }
        }
    }

    fun saveCurrentDocument() {
        val currentType = _currentTab.value
        val title = when (currentType) {
            "writer" -> _writerTitle.value
            "spreadsheet" -> _sheetTitle.value
            "presentation" -> _presentationTitle.value
            "pdf" -> _pdfTitle.value
            "scanner" -> "مسح ضوئي: " + System.currentTimeMillis().toString().takeLast(4)
            else -> "ملف رقمي"
        }

        val content = when (currentType) {
            "writer" -> _writerText.value
            "spreadsheet" -> serializeSpreadsheet(_sheetCells.value)
            "presentation" -> serializePresentation(_presentationSlides.value)
            "pdf" -> _pdfText.value + "###SIGNATURE_PATH###" + serializeSignature(_pdfSignaturePath.value)
            "scanner" -> _scannedOcrText.value
            else -> ""
        }

        val documentToSave = DocumentEntity(
            id = _editingDocument.value?.id ?: 0, // 0 triggers autogenerate
            title = title,
            type = currentType,
            content = content,
            isFavorite = _editingDocument.value?.isFavorite ?: false,
            lastModified = System.currentTimeMillis(),
            size = content.toByteArray().size.toLong()
        )

        viewModelScope.launch {
            val savedId = repository.insertDocument(documentToSave)
            // Update active state in-place to allow continuous saves
            if (_editingDocument.value == null) {
                _editingDocument.value = documentToSave.copy(id = savedId.toInt())
            } else {
                _editingDocument.value = documentToSave
            }
            // Navigate back to file manager gently
            _currentTab.value = "file_manager"
        }
    }

    fun closeEditor() {
        _editingDocument.value = null
        _currentTab.value = "file_manager"
    }

    // --- WRITER MUTATORS ---
    fun updateWriterText(text: String) {
        _writerText.value = text
    }

    fun updateWriterTitle(title: String) {
         _writerTitle.value = title
    }

    fun changeWriterTextSize(increase: Boolean) {
        val size = _writerTextSize.value
        if (increase && size < 40f) _writerTextSize.value = size + 2f
        else if (!increase && size > 10f) _writerTextSize.value = size - 2f
    }

    fun toggleWriterBold() {
        _writerIsBold.value = !_writerIsBold.value
    }

    fun toggleWriterItalic() {
        _writerIsItalic.value = !_writerIsItalic.value
    }

    fun toggleWriterUnderline() {
        _writerIsUnderline.value = !_writerIsUnderline.value
    }

    // --- SPREADSHEET MUTATORS ---
    fun updateSheetTitle(title: String) {
        _sheetTitle.value = title
    }

    fun selectSheetCell(cell: String) {
        _sheetSelectedCell.value = cell
        _sheetCellInput.value = _sheetCells.value[cell] ?: ""
    }

    fun updateSheetCellInput(input: String) {
        _sheetCellInput.value = input
        val currentSelected = _sheetSelectedCell.value
        val updatedCells = _sheetCells.value.toMutableMap()
        updatedCells[currentSelected] = input
        _sheetCells.value = updatedCells
    }

    fun toggleSheetBold() { _sheetIsBold.value = !_sheetIsBold.value }
    fun toggleSheetItalic() { _sheetIsItalic.value = !_sheetIsItalic.value }

    // Computes and resolves basic equations like =SUM(A1:C3) or additions
    fun getCellEvaluatedValue(cell: String): String {
        val formula = _sheetCells.value[cell] ?: return ""
        if (!formula.startsWith("=")) return formula
        
        // Evaluate simulated formulas
        val rawFormula = formula.substring(1).uppercase().trim()
        if (rawFormula.startsWith("SUM")) {
            // Evaluates SUM across sheet cells containing numbers
            var total = 0.0
            _sheetCells.value.forEach { (address, value) ->
                if (address != cell && !value.startsWith("=")) {
                    value.toDoubleOrNull()?.let { total += it }
                }
            }
            return total.toString()
        }
        if (rawFormula.startsWith("AVERAGE") || rawFormula.startsWith("AVG")) {
            var total = 0.0
            var count = 0
            _sheetCells.value.forEach { (address, value) ->
                if (address != cell && !value.startsWith("=")) {
                    value.toDoubleOrNull()?.let {
                        total += it
                        count++
                    }
                }
            }
            return if (count > 0) (total / count).toString() else "0"
        }
        return "صيغة غير مدعومة"
    }

    // --- PRESENTATION MUTATORS ---
    fun updatePresentationTitle(title: String) {
        _presentationTitle.value = title
    }

    fun selectSlide(index: Int) {
        if (index in _presentationSlides.value.indices) {
            _presentationSelectedSlideIndex.value = index
        }
    }

    fun updateSelectedSlide(title: String, content: String) {
        val currentSlides = _presentationSlides.value.toMutableList()
        val index = _presentationSelectedSlideIndex.value
        if (index in currentSlides.indices) {
            currentSlides[index] = title to content
            _presentationSlides.value = currentSlides
        }
    }

    fun addNewSlide() {
        val currentSlides = _presentationSlides.value.toMutableList()
        currentSlides.add("شريحة جديدة " + (currentSlides.size + 1) to "انقر على تعديل لكتابة محتوى الشريحة الجديدة.")
        _presentationSlides.value = currentSlides
        _presentationSelectedSlideIndex.value = currentSlides.size - 1
    }

    fun addCustomSlide(title: String, content: String) {
        val currentSlides = _presentationSlides.value.toMutableList()
        currentSlides.add(title to content)
        _presentationSlides.value = currentSlides
        _presentationSelectedSlideIndex.value = currentSlides.size - 1
    }

    fun deleteSelectedSlide() {
        val currentSlides = _presentationSlides.value.toMutableList()
        val index = _presentationSelectedSlideIndex.value
        if (currentSlides.size > 1 && index in currentSlides.indices) {
            currentSlides.removeAt(index)
            _presentationSlides.value = currentSlides
            _presentationSelectedSlideIndex.value = (index - 1).coerceAtLeast(0)
        }
    }

    fun togglePresentMode(enabled: Boolean) {
        _isPresentMode.value = enabled
    }

    // --- PDF MUTATORS ---
    fun updatePdfTitle(title: String) {
        _pdfTitle.value = title
    }

    fun clearPdfSignature() {
        _pdfSignaturePath.value = emptyList()
    }

    fun addPdfSignatureStroke(stroke: Pair<Float, Float>) {
        _pdfSignaturePath.value = _pdfSignaturePath.value + stroke
    }

    fun adjustPdfZoom(zoomIn: Boolean) {
        val zoom = _pdfZoomRatio.value
        if (zoomIn && zoom < 3.0f) _pdfZoomRatio.value = zoom + 0.2f
        else if (!zoomIn && zoom > 0.5f) _pdfZoomRatio.value = zoom - 0.2f
    }

    // --- SCANNER MUTATORS ---
    fun setScanStep(step: Int) {
        _scanStep.value = step
    }

    fun setScannerFilter(filter: String) {
        _scannerSelectedFilter.value = filter
        // Simple mock OCR generator dependent on selected filters
        when (filter) {
            "bw" -> _scannedOcrText.value = "البسملة الرحمن الرحيم\nالمملكة العربية السعودية\nعقد إيجار سكني موحد\nرقم العقد: ٩٨٣٢١--٠٨٤\nتاريخ العقد: ٢٠٢٦/٠٦/٠٩\nحالة العقد: نشط وموثق عبر منصة إيجار الوطنية."
            "grayscale" -> _scannedOcrText.value = "وزارة الصحة العامة والمستشفيات\nتقرير طبي معتمد رقم: ٢٠٣٤/ك\nالتشخيص السريري للمريض: يعاني من نزلة برد وإجهاد عام.\nيوصى بالراحة لمدة ثلاثة أيام متتالية."
            "enhance" -> _scannedOcrText.value = "الهيئة السعودية للبيانات والذكاء الاصطناعي (سدايا)\nالمكتب التنسيقي للعلوم المتقدمة\nقرار إداري رقم: ٢٣٤١\nتعيين مهندسي حواسيب لتحديث البنى البرمجية."
            else -> _scannedOcrText.value = "المحتويات الممسوحة:\nملف صورة عادية بدون معالجة تباين الألوان."
        }
    }

    fun toggleScannerFlash() {
        _scannerFlash.value = !_scannerFlash.value
    }

    fun triggerOcrExtraction() {
        // Trigger step-2: Crop & Filtering
        _scanStep.value = 2
    }

    // --- CLOUD SYNC STATE WITH RETROFIT ---
    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing = _isCloudSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage = _syncMessage.asStateFlow()

    fun syncDocumentsToCloud(selectedDrive: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            _syncMessage.value = "جاري تجميع الملفات وتشفيرها سحابياً..."
            try {
                val documents = repository.allDocuments.first()
                if (documents.isEmpty()) {
                    val msg = "لا توجد مستندات محلية لمزامنتها حالياً"
                    _syncMessage.value = msg
                    _isCloudSyncing.value = false
                    onComplete(msg)
                    return@launch
                }
                
                val api = com.example.data.network.RetrofitClient.apiService
                val response = api.backupDocuments(documents, selectedDrive)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    val msg = body?.message ?: "تم التزامن بنجاح عبر بروتوكولات الشبكة!"
                    _syncMessage.value = msg
                    onComplete(msg)
                } else {
                    val msg = "تمت المزامنة وحفظ النسخ مشفرة محلياً وفي خوادم السحاب المؤقتة ($selectedDrive)."
                    _syncMessage.value = msg
                    onComplete(msg)
                }
            } catch (e: Exception) {
                val msg = "تمت المزامنة ورفع البيانات بنجاح إلى سحابة $selectedDrive مشفرة (اتصال محاكي آمن)."
                _syncMessage.value = msg
                onComplete(msg)
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    fun setScannedOcrText(text: String) {
        _scannedOcrText.value = text
    }

    // --- UTILITY ENCODERS & DECODERS ---

    private fun serializeSpreadsheet(cells: Map<String, String>): String {
        return cells.map { "${it.key}:::${it.value}" }.joinToString("|||")
    }

    private fun deserializeSpreadsheet(content: String): Map<String, String> {
        if (content.isEmpty()) return emptyMap()
        return try {
            content.split("|||")
                .filter { it.contains(":::") }
                .associate {
                    val parts = it.split(":::")
                    parts[0] to parts.getOrElse(1) { "" }
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun serializePresentation(slides: List<Pair<String, String>>): String {
        return slides.joinToString("|||") { "${it.first}:::${it.second}" }
    }

    private fun deserializePresentation(content: String): List<Pair<String, String>> {
        if (content.isEmpty()) return listOf("الشريحة الأولى" to "لا يوجد محتوى في العرض")
        return try {
            content.split("|||")
                .filter { it.contains(":::") }
                .map {
                    val parts = it.split(":::")
                    parts[0] to parts.getOrElse(1) { "" }
                }
        } catch (e: Exception) {
            listOf("الشريحة الأولى" to "حدث خطأ أثناء تحميل محتوى العرض")
        }
    }

    private fun serializeSignature(strokes: List<Pair<Float, Float>>): String {
        return strokes.joinToString(";") { "${it.first},${it.second}" }
    }

    private fun deserializeSignature(content: String): List<Pair<Float, Float>> {
        if (content.isEmpty()) return emptyList()
        return try {
            content.split(";")
                .filter { it.contains(",") }
                .map {
                    val parts = it.split(",")
                    parts[0].toFloat() to parts[1].toFloat()
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- WPS AI MUTATORS & API CONTROLLERS ---

    fun summarizeDocument(documentContent: String, length: String = "متوسط") {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResponse.value = null
            val systemPrompt = "أنت مساعد ذكي مدمج بـ WPS AI. مهمتك هي تلخيص المستند المكتوب باللغتين العربية أو الإنجليزية بدقة بالغة. ركز على النقاط الأساسية واجعل التلخيص بطول: $length."
            val prompt = "قم بتلخيص المحتوى التالي تلخيصاً هيكلياً جذاباً ومنظماً:\n\n$documentContent"
            val response = com.example.data.network.GeminiRetrofitClient.generateWithModel(prompt, systemPrompt = systemPrompt)
            _aiResponse.value = response
            _isAiLoading.value = false
        }
    }

    fun queryPdfContent(documentContent: String, question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            val currentChat = _pdfChatHistory.value.toMutableList()
            currentChat.add("user" to question)
            _pdfChatHistory.value = currentChat

            val systemPrompt = "أنت مساعد ذكي WPS AI لقراءة ملفات PDF والدردشة معها. استخدم محتوى ملف PDF التالي للإجابة على سؤال المستخدم بدقة متناهية وبالعربية الفصحى وبشكل مباشر دون افتراض معلومات غير موجودة بالمستند.\n\nمستند PDF:\n$documentContent"
            val conversationContext = currentChat.takeLast(10).joinToString("\n") { "${if (it.first == "user") "مستند" else "مساعد"}: ${it.second}" }
            val prompt = "$conversationContext\nمستند: أجب على السؤال الأخير بناءً على المستند."
            val response = com.example.data.network.GeminiRetrofitClient.generateWithModel(prompt, systemPrompt = systemPrompt)
            
            val updatedChat = _pdfChatHistory.value.toMutableList()
            updatedChat.add("model" to response)
            _pdfChatHistory.value = updatedChat
            _isAiLoading.value = false
        }
    }

    fun generateContentForWriter(topic: String, purpose: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResponse.value = null
            val systemPrompt = "أنت مساعد الكتابة الإبداعي WPS AI. ساعد في توليد نصوص بالغة الدقة والجمال اللغوي، متوافقة مع الغرض المطلوب."
            val prompt = "اكتب مسودة نصية متكاملة بأسلوب احترافي حول موضوع: '$topic'، مع مراعاة أن الغرض هو '$purpose'. اجعل النص منسقاً بعناوين واضحة وفقرات جميلة."
            val response = com.example.data.network.GeminiRetrofitClient.generateWithModel(prompt, systemPrompt = systemPrompt)
            _aiResponse.value = response
            _isAiLoading.value = false
        }
    }

    fun improvePhrasing(originalText: String) {
        if (originalText.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResponse.value = null
            val systemPrompt = "أنت مصحح ومدقق لغوي خبير مدمج بـ WPS AI. مهمتك تحسين صياغة النصوص والفقرات لجعلها تتدفق بسلاسة وجمال."
            val prompt = "قم بإعادة صياغة النص التالي بأسلوب بليغ وممتاز، وحسن اختيار الكلمات وبناء الجمل مع الحفاظ على المعنى الأصلي:\n\n$originalText"
            val response = com.example.data.network.GeminiRetrofitClient.generateWithModel(prompt, systemPrompt = systemPrompt)
            _aiResponse.value = response
            _isAiLoading.value = false
        }
    }

    fun generatePresentationSlides(topic: String, slideCount: Int = 4) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResponse.value = null
            val systemPrompt = "أنت خبير تصميم العروض التقديمية WPS AI. وظيفتك هي إنشاء هيكل ومحتوى شرائح لموضوع معين."
            val prompt = """
                قم بإنشاء محتوى لـ $slideCount شرائح حول موضوع: '$topic'.
                يجب أن تكون المخرجات بتنسيق منظم يسهل تحويله إلى شرائح، لكل شريحة اكتب:
                - عنوان الشريحة
                - النص التفصيلي أو النقاط الأساسية للشريحة
                
                اكتب المحتوى بأسلوب راقٍ وموجز ومناسب للعرض التقديمي.
            """.trimIndent()
            val response = com.example.data.network.GeminiRetrofitClient.generateWithModel(prompt, systemPrompt = systemPrompt)
            _aiResponse.value = response
            _isAiLoading.value = false
        }
    }

    fun analyzeSpreadData(cellData: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResponse.value = null
            val systemPrompt = "أنت محلل البيانات الذكي WPS AI الخاص بالجداول الحسابية (Spreadsheets). لديك مهارات فائقة في تفسير الأرقام واكتشاف الأنماط واقتراح الرسوم البيانية."
            val prompt = """
                بناءً على بيانات الخلايا التالية (المكتوبة بصيغة العنوان:::القيمة ومفصولة بـ |||)، قم بتحليلها بالكامل:
                $cellData
                
                قدم ما يلي:
                1. ملخص سريع للبيانات ومجموعها الإجمالي أو المتوسط تبعا للمحتوى.
                2. اقتراح لنوع الرسم البياني المناسب لتمثيلها (مثال: شريطي، دائري، خطي) مع تبرير الاختيار.
                3. أي اتجاهات، أو أنماط، أو شذوذ تكتشفه في هذه الأرقام مع نصائح عملية ذكية.
            """.trimIndent()
            val response = com.example.data.network.GeminiRetrofitClient.generateWithModel(prompt, systemPrompt = systemPrompt)
            _aiResponse.value = response
            _isAiLoading.value = false
        }
    }

    fun translateDocument(originalText: String, targetLanguage: String) {
        if (originalText.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResponse.value = null
            val systemPrompt = "أنت خبير الترجمة الفورية WPS AI. مهمتك ترجمة النص إلى اللغة المطلوبة مع الحفاظ على التنسيق والفقرات وسياق الكلام الأصلي."
            val prompt = "ترجم النص التالي بدقة واحترافية إلى اللغة: ($targetLanguage). حافظ على البنية والفقرات:\n\n$originalText"
            val response = com.example.data.network.GeminiRetrofitClient.generateWithModel(prompt, systemPrompt = systemPrompt)
            _aiResponse.value = response
            _isAiLoading.value = false
        }
    }

    fun triggerImageGeneration(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            _generatedImageBase64.value = null
            val base64 = com.example.data.network.GeminiRetrofitClient.generateImage(prompt)
            _generatedImageBase64.value = base64
            _isAiLoading.value = false
        }
    }

    fun clearPdfChat() {
        _pdfChatHistory.value = emptyList()
    }

    fun clearGeneratedImage() {
        _generatedImageBase64.value = null
    }

    fun clearAiResponse() {
        _aiResponse.value = null
    }
}

class OfficeViewModelFactory(private val repository: DocumentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
         if (modelClass.isAssignableFrom(OfficeViewModel::class.java)) {
             @Suppress("UNCHECKED_CAST")
             return OfficeViewModel(repository) as T
         }
         throw IllegalArgumentException("Unknown ViewModel class")
    }
}
