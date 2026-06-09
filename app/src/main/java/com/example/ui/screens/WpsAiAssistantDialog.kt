package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.OfficeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WpsAiAssistantDialog(
    viewModel: OfficeViewModel,
    screenType: String, // "writer", "spreadsheet", "presentation", "pdf"
    onDismissRequest: () -> Unit,
    onInsertText: (String) -> Unit = {},
    onInsertImage: (String) -> Unit = {}
) {
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val pdfChatHistory by viewModel.pdfChatHistory.collectAsState()
    val generatedImageBase64 by viewModel.generatedImageBase64.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Dialog state
    var activeTab by remember { 
        mutableStateOf(
            when (screenType) {
                "pdf" -> "chat_pdf"
                "spreadsheet" -> "data_analysis"
                "presentation" -> "generate_presentation"
                else -> "writer_helper"
            }
        )
    }

    // Input States
    var generalPrompt by remember { mutableStateOf("") }
    var summarizeLength by remember { mutableStateOf("متوسط") }
    var targetLanguage by remember { mutableStateOf("الإنجليزية") }
    var pdfQuestion by remember { mutableStateOf("") }
    var imagePrompt by remember { mutableStateOf("") }

    // WPS AI Signature Gradient Design
    val aiGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF8E2DE2), // Purple
            Color(0xFF4A00E0), // Dark Royal Blue
            Color(0xFFF000FF)  // Pink Edge
        )
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .background(Color(0xFF0F111A), RoundedCornerShape(24.dp))
                .border(BorderStroke(1.5.dp, aiGradient), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F111A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(aiGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "مساعد WPS AI الذكي",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f))
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

                // Selector tabs based on capabilities
                ScrollableTabRow(
                    selectedTabIndex = getTabIndex(activeTab),
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        val index = getTabIndex(activeTab)
                        if (index in tabPositions.indices) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                                color = Color(0xFFF000FF)
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = activeTab == "writer_helper",
                        onClick = { activeTab = "writer_helper"; viewModel.clearAiResponse() },
                        text = { Text("مساعد الكتابة", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == "summarize",
                        onClick = { activeTab = "summarize"; viewModel.clearAiResponse() },
                        text = { Text("تلخيص المستند", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == "translate",
                        onClick = { activeTab = "translate"; viewModel.clearAiResponse() },
                        text = { Text("ترجمة فورية", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == "chat_pdf",
                        onClick = { activeTab = "chat_pdf"; viewModel.clearAiResponse() },
                        text = { Text("دردشة PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == "data_analysis",
                        onClick = { activeTab = "data_analysis"; viewModel.clearAiResponse() },
                        text = { Text("تحليل البيانات", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == "generate_presentation",
                        onClick = { activeTab = "generate_presentation"; viewModel.clearAiResponse() },
                        text = { Text("صناعة العروض", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == "image_gen",
                        onClick = { activeTab = "image_gen"; viewModel.clearGeneratedImage() },
                        text = { Text("توليد الصور", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Body content based on selected tab
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    when (activeTab) {
                        "writer_helper" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "توليد نصوص ذكية أو تحسين الصياغة الحالية لزيادة جاذبية الكتابة في المستند المكتبي:",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                OutlinedTextField(
                                    value = generalPrompt,
                                    onValueChange = { generalPrompt = it },
                                    label = { Text("ماذا ترغب في كتابته؟ مثال: خطة عمل مشروع ذكاء اصطناعي", color = Color.White.copy(alpha = 0.5f)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF8E2DE2),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 4
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { 
                                            viewModel.generateContentForWriter(generalPrompt, "إثراء وإلهام") 
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E2DE2)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("توليد مسودة", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { 
                                            viewModel.improvePhrasing(generalPrompt) 
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A00E0)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تحسين الصياغة", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        "summarize" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "قم بتلخيص المحتوى الطويل تلقائياً بضغطة زر مع تخصيص عمق ومستوى تلخيص النقاط الأساسية:",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    listOf("وجيز للغاية", "متوسط", "مفصل عميق").forEach { len ->
                                        val selected = len == summarizeLength
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selected) Color(0xFFF000FF).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                                .border(BorderStroke(1.dp, if (selected) Color(0xFFF000FF) else Color.White.copy(alpha = 0.1f)), RoundedCornerShape(12.dp))
                                                .clickable { summarizeLength = len }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(len, color = if (selected) Color(0xFFF000FF) else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                                Button(
                                    onClick = { 
                                        // Get text from VM based on screen active document
                                        val activeContent = getActiveDocumentContent(viewModel, screenType)
                                        viewModel.summarizeDocument(activeContent, summarizeLength)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E2DE2)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Notes, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("بدء التلخيص الذكي للأقسام", fontSize = 12.sp)
                                }
                            }
                        }
                        "translate" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "ترجمة نصوص المستند بالكامل وبدقة فائقة مع الحفاظ على الفقرات والتنسيق الأصلي للفقرة:",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("العربية", "الإنجليزية", "الألمانية").forEach { lang ->
                                        val selected = lang == targetLanguage
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selected) Color(0xFF4A00E0).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                                .border(BorderStroke(1.dp, if (selected) Color(0xFF4A00E0) else Color.White.copy(alpha = 0.1f)), RoundedCornerShape(12.dp))
                                                .clickable { targetLanguage = lang }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(lang, color = if (selected) Color(0xFF4A00E0) else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                                Button(
                                    onClick = { 
                                        val textToTranslate = getActiveDocumentContent(viewModel, screenType)
                                        viewModel.translateDocument(textToTranslate, targetLanguage)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E2DE2)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Translate, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ترجمة المستند إلى $targetLanguage", fontSize = 12.sp)
                                }
                            }
                        }
                        "chat_pdf" -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Text(
                                        "اسأل عن محتوى PDF لمطابقة البنود والتحقق من التفاصيل فورياً:",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewModel.clearPdfChat() }) {
                                        Text("مسح الدردشة", color = Color.Red, fontSize = 11.sp)
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (pdfChatHistory.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "لم تقم بطرح أي سؤال بعد. ابدأ بسؤال المساعد WPS AI حول ملف PDF الآن!",
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    fontSize = 11.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                    items(pdfChatHistory) { chat ->
                                        val isUser = chat.first == "user"
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                        ) {
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isUser) Color(0xFF4A00E0) else Color.White.copy(alpha = 0.08f)
                                                ),
                                                shape = RoundedCornerShape(
                                                    topStart = 12.dp,
                                                    topEnd = 12.dp,
                                                    bottomStart = if (isUser) 12.dp else 4.dp,
                                                    bottomEnd = if (isUser) 4.dp else 12.dp
                                                ),
                                                modifier = Modifier.fillMaxWidth(0.85f)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(
                                                        text = if (isUser) "أنت" else "مساعد WPS AI PDF",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isUser) Color.White.copy(alpha = 0.7f) else Color(0xFFF000FF)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = chat.second,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        lineHeight = 16.sp,
                                                        textAlign = TextAlign.Start
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = pdfQuestion,
                                        onValueChange = { pdfQuestion = it },
                                        placeholder = { Text("اطرح سؤالاً حول هذا الملف...", fontSize = 11.sp, color = Color.Gray) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF4A00E0)
                                        ),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    IconButton(
                                        onClick = {
                                            if (pdfQuestion.isNotEmpty()) {
                                                val pdfContent = viewModel.pdfText.value
                                                viewModel.queryPdfContent(pdfContent, pdfQuestion)
                                                pdfQuestion = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(aiGradient, RoundedCornerShape(12.dp))
                                    ) {
                                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                                    }
                                }
                            }
                        }
                        "data_analysis" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "مساعد تحليل الجداول الذكي: يقوم بالمسح الرياضي، استخراج الاتجاهات، والأنماط، واقتراح الرسوم البيانية والجداول المحورية الملائمة:",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Button(
                                    onClick = { 
                                        // Package cell states
                                        val builder = StringBuilder()
                                        viewModel.sheetCells.value.forEach { (address, value) ->
                                            builder.append("$address:::$value|||")
                                        }
                                        viewModel.analyzeSpreadData(builder.toString())
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E2DE2)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.BarChart, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("تحليل وتفسير بيانات الجدول ذكائياً (AI)", fontSize = 12.sp)
                                }
                            }
                        }
                        "generate_presentation" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "صانع العروض المبتكر: قم بكتابة موضوع ليقوم الذكاء الاصطناعي بكتابة وتوليد كامل هيكل ومحتويات العرض والشرائح بذكاء ودقة مذهلة:",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                OutlinedTextField(
                                    value = generalPrompt,
                                    onValueChange = { generalPrompt = it },
                                    label = { Text("اكتب موضوع العرض، مثال: استدامة المدن الذكية في المملكة", color = Color.White.copy(alpha = 0.5f)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF8E2DE2),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = { viewModel.generatePresentationSlides(generalPrompt) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF000FF)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Slideshow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("توليد هيكل عرض تقديمي كامل (AI)", fontSize = 12.sp)
                                }
                            }
                        }
                        "image_gen" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "توليد صور بجودة مذهلة (WPS AI Art) لإدراجها في الشرائح كعناصر بصرية أو في مستندات الـ Writer:",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                OutlinedTextField(
                                    value = imagePrompt,
                                    onValueChange = { imagePrompt = it },
                                    label = { Text("وصف الصورة المطلوبة بالتفصيل (بالإنجليزية أو العربية)...", color = Color.White.copy(alpha = 0.5f)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFF000FF),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )
                                Button(
                                    onClick = { viewModel.triggerImageGeneration(imagePrompt) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E2DE2)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ابدأ بتوليد لوحات ذكاء اصطناعي", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // AI Processing Spinner & Response Box
                    if (isAiLoading) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFFF000FF), modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("جاري معالجة طلبك بواسطة محركات WPS AI الفائقة...", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    } else {
                        // Display visual image if image_gen tab produced a base64
                        if (activeTab == "image_gen" && generatedImageBase64 != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141522)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 4.dp),
                                border = BorderStroke(1.dp, Color(0xFFF000FF).copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("تم توليد التحفة الفنية الذكية بنجاح!", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val bitmap = remember(generatedImageBase64) {
                                        try {
                                            val decodedBytes = Base64.decode(generatedImageBase64, Base64.DEFAULT)
                                            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }

                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "AI Generated Image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(160.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(160.dp)
                                                .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("فشل ترميز الصورة اللوحية", color = Color.White)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                generatedImageBase64?.let { onInsertImage(it) }
                                                onDismissRequest()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A00E0)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("إدراج في المستند", fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.clearGeneratedImage() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("توليد مجدد", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        } else if (activeTab != "chat_pdf" && aiResponse != null) {
                            // Text output display
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141522)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 4.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("مخرجات وتوجيهات الذكاء الاصطناعي WPS AI:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Row {
                                            IconButton(
                                                onClick = { 
                                                    aiResponse?.let { 
                                                        clipboardManager.setText(AnnotatedString(it))
                                                    } 
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }

                                    Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            item {
                                                Text(
                                                    text = aiResponse ?: "",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    lineHeight = 18.sp,
                                                    textAlign = TextAlign.Justify,
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Action buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { 
                                                aiResponse?.let { onInsertText(it) }
                                                onDismissRequest()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E2DE2)),
                                            modifier = Modifier.weight(1.5f)
                                        ) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = when (screenType) {
                                                    "writer" -> "إدراج في محتوى المستند"
                                                    "spreadsheet" -> "وضع في الخلية النشطة"
                                                    "presentation" -> "إدراج كشريحة جديدة"
                                                    else -> "إدخال في النص"
                                                },
                                                fontSize = 11.sp
                                            )
                                        }
                                        TextButton(
                                            onClick = { viewModel.clearAiResponse() },
                                            modifier = Modifier.weight(0.7f)
                                        ) {
                                            Text("مسح المخرج", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Utility mapper to handle Tab indexes smoothly
private fun getTabIndex(tab: String): Int {
    return when (tab) {
        "writer_helper" -> 0
        "summarize" -> 1
        "translate" -> 2
        "chat_pdf" -> 3
        "data_analysis" -> 4
        "generate_presentation" -> 5
        "image_gen" -> 6
        else -> 0
    }
}

// Helper to grab active editing text to send to Gemini limits context sizes securely
private fun getActiveDocumentContent(viewModel: OfficeViewModel, screenType: String): String {
    return when (screenType) {
        "writer" -> viewModel.writerText.value
        "spreadsheet" -> {
            val builder = StringBuilder()
            viewModel.sheetCells.value.forEach { (coord, valStr) ->
                builder.append("$coord: $valStr\n")
            }
            builder.toString().ifEmpty { "جدول بيانات فارغ" }
        }
        "presentation" -> {
            viewModel.presentationSlides.value.joinToString("\n") { "${it.first} : ${it.second}" }
        }
        "pdf" -> viewModel.pdfText.value
        else -> ""
    }
}
