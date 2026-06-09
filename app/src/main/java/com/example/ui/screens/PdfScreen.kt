package com.example.ui.screens

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PdfRed
import com.example.ui.viewmodel.OfficeViewModel
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

// --- STAGE 3 INTERACTIVE PDF DATA MODELS ---
data class InteractiveTerm(
    val word: String,
    val translation: String,
    val description: String,
    val url: String,
    val language: String // "de": German, "ar": Arabic, "en": English
)

data class PdfAnnotation(
    val id: String,
    val type: String, // "highlight", "underline", "sticky_note", "arrow", "rect"
    val content: String,
    val color: Color,
    val pageIndex: Int = 0
)

data class PdfFormFields(
    val fullName: String = "",
    val jobTitle: String = "",
    val dateFiled: String = "",
    val checkedAgreed: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfScreen(
    viewModel: OfficeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val title by viewModel.pdfTitle.collectAsState()
    val text by viewModel.pdfText.collectAsState()
    val signaturePath by viewModel.pdfSignaturePath.collectAsState()
    val zoomRatio by viewModel.pdfZoomRatio.collectAsState()

    // Workspace main tabs: "view" (Interactive words), "edit" (Annotations), "manage" (Splits, merge, compress), "security" (Forms & Lock)
    var activeWorkspaceTab by remember { mutableStateOf("view") }

    // TTS state controllers
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val speech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
            }
        }
        tts = speech
        onDispose {
            speech.shutdown()
        }
    }

    val speakWord: (String, String) -> Unit = { termWord, langCode ->
        if (ttsReady && tts != null) {
            val locale = when (langCode) {
                "de" -> Locale.GERMAN
                "ar" -> Locale("ar")
                "en" -> Locale.ENGLISH
                else -> Locale.US
            }
            tts?.language = locale
            tts?.speak(termWord, TextToSpeech.QUEUE_FLUSH, null, null)
            Toast.makeText(context, "🗣️ نطق: $termWord", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "الخدمة الصوتية غير جاهزة حالياً", Toast.LENGTH_SHORT).show()
        }
    }

    // Interactive words custom dictionary state
    var interactiveTerms by remember {
        mutableStateOf(
            listOf(
                InteractiveTerm(
                    word = "Fachrichtung",
                    translation = "تخصص مهني / فرع أكاديمي",
                    description = "يُقصد به التوجه الأكاديمي أو التخصص المهني الأساسي الذي يدرسه المتخصص.",
                    url = "https://www.duden.de/rechtschreibung/Fachrichtung",
                    language = "de"
                ),
                InteractiveTerm(
                    word = "Arbeitsverhältnis",
                    translation = "عقد عمالة / علاقة عمل",
                    description = "الرابطة التعاقدية القانونية التي تنشأ بين العامل والمؤسسة.",
                    url = "https://www.duden.de/rechtschreibung/Arbeitsverhaeltnis",
                    language = "de"
                ),
                InteractiveTerm(
                    word = "برمجة",
                    translation = "Software Development / Programming",
                    description = "تطوير وكتابة الشفرات البرمجية لتشغيل الآلات والحواسيب وصيانتها.",
                    url = "https://ar.wikipedia.org/wiki/برمجة",
                    language = "ar"
                ),
                InteractiveTerm(
                    word = "تطوير",
                    translation = "Evolution & Development",
                    description = "عمليات التحديث البرمجية المستمرة للارتقاء بجودة وأمن النظم والملفات.",
                    url = "https://ar.wikipedia.org/wiki/تطوير_ويب",
                    language = "ar"
                ),
                InteractiveTerm(
                    word = "Vertrag",
                    translation = "عقد رسمي ملزم",
                    description = "اتفاقية رسمية ملزمة قانونياً بين طرفين أو أكثر تحدد الواجبات والجاهزية.",
                    url = "https://www.duden.de/rechtschreibung/Vertrag",
                    language = "de"
                ),
                InteractiveTerm(
                    word = "Entwicklung",
                    translation = "التطوير والتصحيح التقني",
                    description = "تعني عمليات التطوير البرمجي والهندسي المستمر لتحديث المنصات الرقمية.",
                    url = "https://www.duden.de/rechtschreibung/Entwicklung",
                    language = "de"
                )
            )
        )
    }

    var selectedTerm by remember { mutableStateOf<InteractiveTerm?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var customPastedCSV by remember { mutableStateOf("") }
    
    // Annotations states
    var annotationsList by remember {
        mutableStateOf(
            listOf(
                PdfAnnotation("1", "highlight", "عقد اتفاقية تقديم خدمات برمجية وتطوير", Color(0xFFFFEB3B)),
                PdfAnnotation("2", "underline", "شركة الحلول المتقدمة للمعلوماتية", Color(0xFF4CAF50)),
                PdfAnnotation("3", "sticky_note", "مهم: تسليم التقارير الفنية أسبوعياً للعميل الموقر.", Color(0xFFFF9800))
            )
        )
    }

    var showAddAnnotationDialog by remember { mutableStateOf(false) }
    var annotationDialogType by remember { mutableStateOf("sticky_note") } // "sticky_note", "highlight", "underline", "rect"
    var annotationDialogContent by remember { mutableStateOf("") }
    var annotationDialogColor by remember { mutableStateOf(Color(0xFFFFEB3B)) }

    // Forms States
    var formFields by remember { mutableStateOf(PdfFormFields()) }
    var isPasswordLocked by remember { mutableStateOf(false) }
    var pdfPassword by remember { mutableStateOf("") }
    var showPasswordSetupDialog by remember { mutableStateOf(false) }

    // Pages / Document management states
    var pageRotations by remember { mutableStateOf(listOf(0f, 0f, 0f)) } // simulated 3 pages
    var visiblePagesCount by remember { mutableStateOf(1) } // simulated multiline pagination toggle
    var activePageSelectedForManagement by remember { mutableStateOf(0) }
    var isCompressingFile by remember { mutableStateOf(false) }
    var compressionValuePercent by remember { mutableStateOf(0) }
    var simulatedCompressedSize by remember { mutableStateOf("480 كيلوبايت") }

    var showTitleEditDialog by remember { mutableStateOf(false) }
    var tempTitle by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            tempTitle = title
                            showTitleEditDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = null,
                            tint = PdfRed,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "تعديل، تعليق وتوقيع تفاعلي",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "تعديل الاسم",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeEditor() },
                        modifier = Modifier.testTag("pdf_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "الرجوع"
                        )
                    }
                },
                actions = {
                    // Quick conversion menu simulation
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "جاري تحويل المستند إلى تنسيق Word Docx جاهز...", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.testTag("pdf_export_mock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Transform,
                            contentDescription = "تحويل وتصدير",
                            tint = PdfRed
                        )
                    }

                    // Save Button
                    Button(
                        onClick = { viewModel.saveCurrentDocument() },
                        colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("pdf_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // PDF VIEW TOOLS HEADER (ZOOM & SEARCH)
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search bar filter within PDF
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pdf_search_filter"),
                            placeholder = { Text("بحث وتصفح النصوص...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PdfRed,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Simulated Zoom Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(onClick = { viewModel.adjustPdfZoom(zoomIn = false) }) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOut,
                                    contentDescription = "تصغير",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${(zoomRatio * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PdfRed,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                            IconButton(onClick = { viewModel.adjustPdfZoom(zoomIn = true) }) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = "تكبير",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // TABS FOR STAGE 3 ACTION PANELS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "view" to "📖 المعجم الذكي",
                            "edit" to "🖋️ التعليقات التوضيحية",
                            "manage" to "⚙️ إدارة الصفحات والتحويل",
                            "forms" to "🔒 الأمان والنماذج التفاعلية"
                        ).forEach { (id, label) ->
                            val active = activeWorkspaceTab == id
                            FilterChip(
                                selected = active,
                                onClick = { activeWorkspaceTab = id },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PdfRed.copy(alpha = 0.15f),
                                    selectedLabelColor = PdfRed
                                )
                            )
                        }
                    }
                }
            }

            // MAIN WORKSPACE PANELS
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // TAB 1: WORKSPACE TAB PANELS - DETAILS INPUTS
                when (activeWorkspaceTab) {
                    "view" -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📖 معجم الترجمة المزدوج وخدمة النطق الصوتية (TTS)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PdfRed
                                    )
                                    Button(
                                        onClick = { showImportDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("استيراد كلمات", fontSize = 10.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "الكلمات التفاعلية محددة بخصائص بصرية ولونية في مستند الـ PDF أدناه. انقر على أي كلمة مظللة لنطقها صوتياً باللغة الأم أو مراجعة معانيها اللغوية وروابط القاموس.",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    "edit" -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "🖋️ أدوات التعليق والتحرير التلقائي للمستند",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PdfRed
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            annotationDialogType = "sticky_note"
                                            annotationDialogContent = "ملاحظة توجيهية هامة..."
                                            annotationDialogColor = Color(0xFFFFEB3B)
                                            showAddAnnotationDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.StickyNote2, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ملاحظة لاصقة", fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = {
                                            annotationDialogType = "highlight"
                                            annotationDialogContent = "عقد اتفاقية"
                                            annotationDialogColor = Color(0xFFFFE082)
                                            showAddAnnotationDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.Highlight, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("قلم تظليل", fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = {
                                            annotationDialogType = "underline"
                                            annotationDialogContent = "شركة الحلول المتقدمة"
                                            annotationDialogColor = Color(0xFF81C784)
                                            showAddAnnotationDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.FormatUnderlined, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تسطير النص", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    "manage" -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "⚙️ خدمات تقسيم، دمج وضغط ملف الـ PDF",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PdfRed
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // File Compression Display
                                if (isCompressingFile) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text("جاري معالجة وتقليص الحجم الزائد للمستند...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = compressionValuePercent / 100f,
                                            color = PdfRed,
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                                        )
                                        Text("$compressionValuePercent%", fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("الحجم الحالي: $simulatedCompressedSize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Button(
                                            onClick = {
                                                isCompressingFile = true
                                                compressionValuePercent = 0
                                                // Simulated compression task
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("ضغط حجم الملف", fontSize = 10.sp)
                                        }
                                    }
                                }

                                if (isCompressingFile) {
                                    LaunchedEffect(isCompressingFile) {
                                        while (compressionValuePercent < 100) {
                                            kotlinx.coroutines.delay(100)
                                            compressionValuePercent += 10
                                        }
                                        isCompressingFile = false
                                        simulatedCompressedSize = "112 كيلوبايت (تخفيض 76%)"
                                        Toast.makeText(context, "تم ضغط الملف بنجاح فائق!", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                // Merge / Split mock
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            // Mock Merge
                                            val augmentedText = text + "\n\n=== مستند مدمج ملحق ===\nتحقيق الشروط المرجعية الموحدة والسرية للطرفين."
                                            viewModel.startNewDocument("pdf")
                                            viewModel.updatePdfTitle("مستند_مدمج_جديد.pdf")
                                            Toast.makeText(context, "تم دمج الملف مع مستند كتابي ملحق بنجاح!", Toast.LENGTH_LONG).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("دمج ملفات PDF", fontSize = 9.sp)
                                    }

                                    Button(
                                        onClick = {
                                            // Mock Split
                                            Toast.makeText(context, "تم تقسيم المستند إلى ملفين: الجزء ١ والجزء ٢", Toast.LENGTH_LONG).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تقسيم PDF", fontSize = 10.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Reorder rotate simulated pages section
                                Text("إدارة تدوير الصفحات وصف الخصائص:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("صفحة ١", "صفحة ٢", "صفحة ٣").forEachIndexed { idx, label ->
                                        val active = activePageSelectedForManagement == idx
                                        Card(
                                            border = BorderStroke(1.dp, if (active) PdfRed else Color.LightGray.copy(alpha = 0.5f)),
                                            colors = CardDefaults.cardColors(containerColor = if (active) PdfRed.copy(alpha = 0.08f) else Color.White),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { activePageSelectedForManagement = idx }
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text("تدوير: ${pageRotations[idx].toInt()}°", fontSize = 9.sp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Icon(
                                                        imageVector = Icons.Default.RotateRight,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clickable {
                                                                val newList = pageRotations.toMutableList()
                                                                newList[idx] = (newList[idx] + 90f) % 360f
                                                                pageRotations = newList
                                                                Toast.makeText(context, "تم تدوير الصفحة ${idx+1} بمقدار 90 درجة", Toast.LENGTH_SHORT).show()
                                                            },
                                                        tint = PdfRed
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteSweep,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clickable {
                                                                Toast.makeText(context, "تم حذف الصفحة ${idx+1} من ملف الـ PDF", Toast.LENGTH_SHORT).show()
                                                            },
                                                        tint = Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "forms" -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔒 تامين وكلمة مرور وحقل نماذج PDF",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PdfRed
                                    )
                                    TextButton(
                                        onClick = { showPasswordSetupDialog = true },
                                        colors = ButtonDefaults.textButtonColors(contentColor = PdfRed)
                                    ) {
                                        Icon(imageVector = if (isPasswordLocked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isPasswordLocked) "تغيير الرمز" else "حماية المستند", fontSize = 10.sp)
                                    }
                                }

                                if (isPasswordLocked) {
                                    Text("🔑 المستند محمي بكلمة مرور مشفرة حالياً.", color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("تعبئة حقول النماذج التفاعلية المباشرة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                OutlinedTextField(
                                    value = formFields.fullName,
                                    onValueChange = { formFields = formFields.copy(fullName = it) },
                                    label = { Text("الاسم الكامل للموقع المفوض", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                )

                                OutlinedTextField(
                                    value = formFields.jobTitle,
                                    onValueChange = { formFields = formFields.copy(jobTitle = it) },
                                    label = { Text("المسمى الوظيفي والصفة", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = formFields.checkedAgreed,
                                        onCheckedChange = { formFields = formFields.copy(checkedAgreed = it) }
                                    )
                                    Text("أوافق على جميع الشروط والمقررات والمسؤوليات القانونية المترتبة.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // PANEL 2: STATIC/STYLISH PDF PAGE SIMULATION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding((20 * zoomRatio).dp) // Responsive spacing according to zoom
                    ) {
                        // PDF Document Header Sheet
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مستند بي دي إف رسمي موثق",
                                fontSize = (11 * zoomRatio).sp,
                                fontWeight = FontWeight.Bold,
                                color = PdfRed
                            )
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = PdfRed,
                                modifier = Modifier.size((16 * zoomRatio).dp)
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = (10 * zoomRatio).dp),
                            color = Color.LightGray.copy(alpha = 0.5f)
                        )

                        // Render each line with interactive word detection or standard search highlighting
                        val textLines = text.split("\n")
                        textLines.forEach { line ->
                            val containsQuery = searchQuery.isNotEmpty() && line.contains(searchQuery, ignoreCase = true)
                            
                            // Build rich text styling for current interactive words dictionary
                            val richText = buildAnnotatedString {
                                var lastIndex = 0
                                // Sort terms by length descending to match correctly
                                val sortedTerms = interactiveTerms.sortedByDescending { it.word.length }
                                val wordRanges = mutableListOf<Pair<IntRange, InteractiveTerm>>()

                                sortedTerms.forEach { term ->
                                    var startIdx = line.indexOf(term.word, 0, ignoreCase = true)
                                    while (startIdx != -1) {
                                        val endIdx = startIdx + term.word.length
                                        val range = startIdx until endIdx
                                        if (wordRanges.none { it.first.first <= startIdx && it.first.last >= startIdx || it.first.first <= endIdx && it.first.last >= endIdx }) {
                                            wordRanges.add(range to term)
                                        }
                                        startIdx = line.indexOf(term.word, startIdx + 1, ignoreCase = true)
                                    }
                                }
                                wordRanges.sortBy { it.first.first }

                                wordRanges.forEach { (range, term) ->
                                    if (range.first > lastIndex) {
                                        append(line.substring(lastIndex, range.first))
                                    }
                                    pushStringAnnotation(tag = "INTERACTIVE_WORD", annotation = term.word)
                                    withStyle(
                                        style = SpanStyle(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = TextDecoration.Underline,
                                            background = when (term.language) {
                                                "de" -> Color(0xFF1E88E5)
                                                "en" -> Color(0xFFD84315)
                                                else -> Color(0xFF2E7D32)
                                            }
                                        )
                                    ) {
                                        append(line.substring(range.first, range.last + 1))
                                    }
                                    pop()
                                    lastIndex = range.last + 1
                                }
                                if (lastIndex < line.length) {
                                    append(line.substring(lastIndex))
                                }
                            }

                            // Render ClickableText
                            ClickableText(
                                text = richText,
                                style = TextStyle(
                                    fontSize = (14 * zoomRatio).sp,
                                    lineHeight = (22 * zoomRatio).sp,
                                    color = if (containsQuery) Color.Black else Color.DarkGray,
                                    textAlign = TextAlign.Start
                                ),
                                onClick = { offset ->
                                    richText.getStringAnnotations(tag = "INTERACTIVE_WORD", start = offset, end = offset)
                                        .firstOrNull()?.let { annotation ->
                                            val clickedWord = annotation.item
                                            val matchedTerm = interactiveTerms.find { it.word.equals(clickedWord, ignoreCase = true) }
                                            if (matchedTerm != null) {
                                                selectedTerm = matchedTerm
                                            }
                                        }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (containsQuery) Color.Yellow.copy(alpha = 0.4f) else Color.Transparent)
                                    .padding(vertical = (2 * zoomRatio).dp)
                            )
                        }

                        // --- DRAWING APPLIED ANNOTATIONS LIST ---
                        Spacer(modifier = Modifier.height(10.dp))
                        annotationsList.forEach { annotation ->
                            when (annotation.type) {
                                "highlight" -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(annotation.color.copy(alpha = 0.4f))
                                            .padding(2.dp)
                                    ) {
                                        Text(text = "🔸 تظليل: ${annotation.content}", fontSize = (11 * zoomRatio).sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                                "underline" -> {
                                    Text(
                                        text = "🔸 تسطير مخصص: ${annotation.content}",
                                        fontSize = (11 * zoomRatio).sp,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray
                                    )
                                }
                                "sticky_note" -> {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.StickyNote2, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = annotation.content, fontSize = (11 * zoomRatio).sp, color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom dynamic filled form fields section
                        if (formFields.fullName.isNotEmpty() || formFields.jobTitle.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding((10 * zoomRatio).dp)) {
                                    Text("📝 الحقول التفاعلية المعبأة بالنموذج:", fontSize = (10 * zoomRatio).sp, fontWeight = FontWeight.Bold, color = PdfRed)
                                    if (formFields.fullName.isNotEmpty()) {
                                        Text("الاسم المكتمل: ${formFields.fullName}", fontSize = (11 * zoomRatio).sp)
                                    }
                                    if (formFields.jobTitle.isNotEmpty()) {
                                        Text("المسمى الوظيفي: ${formFields.jobTitle}", fontSize = (11 * zoomRatio).sp)
                                    }
                                    if (formFields.checkedAgreed) {
                                        Text("✔️ تم إقرار التوقيع والمبادئ.", fontSize = (10 * zoomRatio).sp, color = Color(0xFF2E7D32))
                                    }
                                }
                            }
                        }

                        // Bottom stamp and re-drawn signature path
                        if (signaturePath.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "التوقيع الرقمي للمفوض:",
                                        fontSize = (11 * zoomRatio).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.LightGray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Custom visual Canvas re-drawing saved signature
                                    Canvas(
                                        modifier = Modifier
                                            .size((120 * zoomRatio).dp, (50 * zoomRatio).dp)
                                            .border(0.5.dp, Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFAFAF9))
                                    ) {
                                        val strokeCount = signaturePath.size
                                        for (i in 0 until strokeCount - 1) {
                                            val p1 = signaturePath[i]
                                            val p2 = signaturePath[i + 1]
                                            
                                            if (p1.first != -1f && p2.first != -1f) {
                                                drawLine(
                                                    color = Color(0xFF000080),
                                                    start = Offset(p1.first * size.width / 300f, p1.second * size.height / 150f),
                                                    end = Offset(p2.first * size.width / 300f, p2.second * size.height / 150f),
                                                    strokeWidth = 2f * zoomRatio,
                                                    cap = StrokeCap.Round
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // PANEL 3: DIGITAL SIGNATURE PAD CANVAS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Gesture,
                                    contentDescription = null,
                                    tint = PdfRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "لوح التوقيع الرقمي الفوري",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            if (signaturePath.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.clearPdfSignature() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = PdfRed),
                                    modifier = Modifier.testTag("clear_signature_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مسح اللوح", fontSize = 11.sp)
                                }
                            }
                        }

                        Text(
                            text = "قم برسم توقيعك المعتمد بإصبعك داخل المستطيل الرمادي أدناه، وسنقوم تلقائياً بختمه على صفحات الملف.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // ACTIVE DRAWING BOX CANVAS
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            viewModel.addPdfSignatureStroke(offset.x to offset.y)
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val currentPosition = change.position
                                            viewModel.addPdfSignatureStroke(currentPosition.x to currentPosition.y)
                                        },
                                        onDragEnd = {
                                            viewModel.addPdfSignatureStroke(-1f to -1f)
                                        }
                                    )
                                }
                                .testTag("signature_canvas")
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeCount = signaturePath.size
                                for (i in 0 until strokeCount - 1) {
                                    val p1 = signaturePath[i]
                                    val p2 = signaturePath[i + 1]
                                    if (p1.first != -1f && p2.first != -1f) {
                                        drawLine(
                                            color = Color(0xFF000080),
                                            start = Offset(p1.first, p1.second),
                                            end = Offset(p2.first, p2.second),
                                            strokeWidth = 4f,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                            }

                            if (signaturePath.isEmpty()) {
                                Text(
                                    text = "ارسم توقيعك هنا...",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // INTERACTIVE WORD DIALOG (TTS & External dictionary)
    selectedTerm?.let { term ->
        AlertDialog(
            onDismissRequest = { selectedTerm = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = PdfRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "مفردة معجمية: ${term.word}",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("اللغة المصدر:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = if (term.language == "de") "الألمانية (Deutsch)" else if (term.language == "en") "الإنجليزية (English)" else "العربية",
                            color = PdfRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                            Text("الترجمة والمفهوم المعرب:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text(term.translation, fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(term.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Native TTS trigger button
                    Button(
                        onClick = { speakWord(term.word, term.language) },
                        colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("نطق اللفظ صوتياً باللفظ الأصلي (TTS)")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Open external link
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(term.url))
                        context.startActivity(intent)
                        selectedTerm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فتح القاموس الخارجي")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTerm = null }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // CSV IMPORT DIALOG
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("استيراد قائمة كلمات تفاعلية", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "يمكنك كتابة الكلمات وفق التنسيق التالي أو اختيار رزم جاهزة بالأسفل:\nالكلمة:::الترجمة:::الوصف:::الرابط:::اللغات",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customPastedCSV,
                        onValueChange = { customPastedCSV = it },
                        placeholder = { Text("مثال:\nFachgebiet:::مجال التخصص:::.واصل بحثك:::https://duden.de:::de") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        textStyle = TextStyle(fontSize = 11.sp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("استيراد رزم مصطلحات مصنفة جاهزة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val germanPack = listOf(
                                    InteractiveTerm("Schriftverkehr", "المراسلات الكتابية", "تعني إرسال وتلقي الرسائل القانونية والمكتبية الموثقة.", "https://duden.de", "de"),
                                    InteractiveTerm("Unterschrift", "التوقيع اليدوي", "المصادقة الخطية باليد لإثبات الموافقة.", "https://duden.de", "de")
                                )
                                interactiveTerms = interactiveTerms + germanPack
                                showImportDialog = false
                                Toast.makeText(context, "تم تحميل رزمة المصطلحات الألمانية المكتبية بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("ألماني مكتبي", fontSize = 9.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val technicalPack = listOf(
                                    InteractiveTerm("Cloud", "التخزين السحابي", "حفظ وتنسيق البيانات البعيدة ومزامنتها تلقائيًا.", "https://wikipedia.org", "en"),
                                    InteractiveTerm("Encryption", "التشفير التام", "ترميز البيانات لتأمين سريتها عبر شبكة المنصة.", "https://wikipedia.org", "en")
                                )
                                interactiveTerms = interactiveTerms + technicalPack
                                showImportDialog = false
                                Toast.makeText(context, "تم تحميل رزمة المصطلحات التقنية بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("مصطلحات تقنية", fontSize = 9.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customPastedCSV.contains(":::")) {
                            try {
                                val parts = customPastedCSV.split(":::")
                                val newWord = parts.getOrNull(0)?.trim() ?: ""
                                val newTrans = parts.getOrNull(1)?.trim() ?: ""
                                val newDesc = parts.getOrNull(2)?.trim() ?: ""
                                val newUrl = parts.getOrNull(3)?.trim() ?: "https://google.com"
                                val newLang = parts.getOrNull(4)?.trim() ?: "de"

                                if (newWord.isNotEmpty() && newTrans.isNotEmpty()) {
                                    interactiveTerms = interactiveTerms + InteractiveTerm(newWord, newTrans, newDesc, newUrl, newLang)
                                    Toast.makeText(context, "تم استيراد الكلمة بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "حدث خطأ في قراءة التنسيق", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showImportDialog = false
                        customPastedCSV = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PdfRed)
                ) {
                    Text("استيراد المدخل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // ADD ANNOTATION DIALOG
    if (showAddAnnotationDialog) {
        AlertDialog(
            onDismissRequest = { showAddAnnotationDialog = false },
            title = { Text("إدراج تعليق توضيحي جديد", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اختر نوع ولون التعليق التوضيحي:", fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "sticky_note" to "ملاحظة لاصقة",
                            "highlight" to "قلم تحديد",
                            "underline" to "تسطير"
                        ).forEach { (typeId, typeLabel) ->
                            val isSel = annotationDialogType == typeId
                            FilterChip(
                                selected = isSel,
                                onClick = { annotationDialogType = typeId },
                                label = { Text(typeLabel, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("محتوى أو النص المستهدف بالتعليق:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = annotationDialogContent,
                        onValueChange = { annotationDialogContent = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("لون الملاحظة التوضيحية البصرية:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            Color(0xFFFFEB3B) to "أصفر",
                            Color(0xFF81C784) to "أخضر",
                            Color(0xFF64B5F6) to "أزرق",
                            Color(0xFFFFB74D) to "برتقالي"
                        ).forEach { (col, _) ->
                            val selCol = annotationDialogColor == col
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(2.dp, if (selCol) Color.Black else Color.Transparent, CircleShape)
                                    .clickable { annotationDialogColor = col }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (annotationDialogContent.trim().isNotEmpty()) {
                            val newAnn = PdfAnnotation(
                                id = (annotationsList.size + 1).toString(),
                                type = annotationDialogType,
                                content = annotationDialogContent,
                                color = annotationDialogColor
                            )
                            annotationsList = annotationsList + newAnn
                            Toast.makeText(context, "تم إدراج التعليق التوضيحي بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                        showAddAnnotationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PdfRed)
                ) {
                    Text("إدراج")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAnnotationDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // SECURITY SETUP DIALOG
    if (showPasswordSetupDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordSetupDialog = false },
            title = { Text(if (isPasswordLocked) "إلغاء حماية مستند الـ PDF" else "حماية المستند بكلمة مرور", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isPasswordLocked) "تنبيه: سيتم إلغاء التشفير البرمجي وفتح المستند للمعاينة الحرة."
                        else "قم بإدخال كلمة مرور قوية لتأمين معلومات هذا الملف من التعديل العشوائي:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isPasswordLocked) {
                        OutlinedTextField(
                            value = pdfPassword,
                            onValueChange = { pdfPassword = it },
                            label = { Text("رمز الحماية والكلمة السرية") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isPasswordLocked) {
                            isPasswordLocked = false
                            pdfPassword = ""
                            Toast.makeText(context, "تم إلغاء حماية المستند بنجاح فوري!", Toast.LENGTH_SHORT).show()
                        } else {
                            if (pdfPassword.trim().isNotEmpty()) {
                                isPasswordLocked = true
                                Toast.makeText(context, "تم تفعيل التشفير وقفل المستند المكتبي!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showPasswordSetupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PdfRed)
                ) {
                    Text(if (isPasswordLocked) "إزالة الحماية" else "تفعيل القفل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordSetupDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // EDIT TITLE DIALOG
    if (showTitleEditDialog) {
        AlertDialog(
            onDismissRequest = { showTitleEditDialog = false },
            title = { Text("تعديل اسم ملف الـ PDF", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text("اسم الملف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempTitle.trim().isNotEmpty()) {
                            viewModel.updatePdfTitle(tempTitle)
                        }
                        showTitleEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PdfRed)
                ) {
                    Text("حفظ الاسم")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTitleEditDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
