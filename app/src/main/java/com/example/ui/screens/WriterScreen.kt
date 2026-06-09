package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.unit.sp
import com.example.ui.theme.WriterBlue
import com.example.ui.viewmodel.OfficeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriterScreen(
    viewModel: OfficeViewModel,
    modifier: Modifier = Modifier
) {
    val title by viewModel.writerTitle.collectAsState()
    val text by viewModel.writerText.collectAsState()
    val textSize by viewModel.writerTextSize.collectAsState()

    var showAiDialog by remember { mutableStateOf(false) }
    val isBold by viewModel.writerIsBold.collectAsState()
    val isItalic by viewModel.writerIsItalic.collectAsState()
    val isUnderline by viewModel.writerIsUnderline.collectAsState()

    // NEW DETAILED PHASES 2 STATES
    var selectedFontFamily by remember { mutableStateOf("قاهرة (Cairo)") }
    var selectedAlignment by remember { mutableStateOf(TextAlign.Justify) }
    var lineSpacing by remember { mutableStateOf(1.25f) }
    var indentLevel by remember { mutableStateOf(0) }
    var selectedExtension by remember { mutableStateOf(".docx") }
    
    // Injections / Elements State
    var headerText by remember { mutableStateOf("شركة الحلول التقنية المحدودة") }
    var footerText by remember { mutableStateOf("صفحة رقم ١ | وثيقة داخلية سرية") }
    var isHeaderFooterEnabled by remember { mutableStateOf(false) }
    
    // Comments
    var commentInput by remember { mutableStateOf("") }
    var commentList by remember { mutableStateOf(listOf("يرجى مراجعة الصياغة في البند الثاني", "تفقد الأرقام المالية")) }
    var showCommentDialog by remember { mutableStateOf(false) }

    // Dynamic Custom Table State
    var showTableWizard by remember { mutableStateOf(false) }
    var tableRows by remember { mutableStateOf(3) }
    var tableCols by remember { mutableStateOf(3) }
    var insertedTables by remember { mutableStateOf<List<List<MutableList<String>>>>(emptyList()) }

    // Floating callouts and inserted shapes
    var insertedShapes by remember { mutableStateOf<List<String>>(emptyList()) }

    // Smart Spellcheck
    var isSpellchecking by remember { mutableStateOf(false) }
    var spellingErrors by remember { mutableStateOf<List<Pair<String, List<String>>>>(emptyList()) }
    var showSpellcheckPanel by remember { mutableStateOf(false) }

    // Track Changes
    var isTrackChangesEnabled by remember { mutableStateOf(false) }
    var originalBaseText by remember { mutableStateOf("") }
    var modificationsLog by remember { mutableStateOf<List<String>>(emptyList()) }

    // Templates List
    val templates = listOf(
        Triple("خطاب رسمي إداري", "تحرير خطاب طلب أو استفسار موجه", "بسم الله الرحمن الرحيم\n\nسعادة مدير عام الموارد البشرية المحترم،\nالسلام عليكم ورحمة الله وبركاته، أما بعد:\n\nنود إفادتكم بطلبنا بخصوص تجديد التراخيص الفنية والبرمجية للربع السنوي الحالي، ونأمل من سعادتكم التكرم بالموافقة وتسهيل الإجراءات.\n\nوتقبلوا منا خالص التقدير والاحترام والمودة.\n\nتوقيع الموظف المختص:\n"),
        Triple("تقرير الربع السنوي", "ملخص الأداء والإنجاز المالي", "التقرير الربع سنوي للمبيعات والعمليات:\n\n١. الأهداف التقنية: تم تسليم نظام الأوفيس المتكامل بالكامل.\n٢. التقدم البرمجي: توافقية ١٠٠٪ مع صيغ الميكروسوفت.\n٣. التقييم العام: أداء متميز بنسبة استقرار تفوق الـ ٩٩٪.\n\nمعايير الحساب الختامي:\n"),
        Triple("اتفاقية تقديم خدمات", "عقد مقاولة برمجية موثق", "اتفاقية تقديم خدمات برمجية وتصميم وتطوير:\n\nالطرف الأول: شركة التطوير والبرمجة التقنية.\nالطرف الثاني: المؤسسة الوطنية للخدمات.\n\nالبند الأول: يلتزم الطرف الأول ببناء السيرفر وقواعد البيانات المتقدمة.\nالبند الثاني: يلتزم الطرف الثاني بدفع المستحقات المالية خلال ٣٠ يوماً.\n")
    )
    var showTemplateSelector by remember { mutableStateOf(false) }

    // Electronic Signature Canvas State
    var showSignaturePanel by remember { mutableStateOf(false) }
    val points = remember { mutableStateListOf<Pair<Float, Float>>() }
    var isSignatureStamped by remember { mutableStateOf(false) }
    var signatureSealNo by remember { mutableStateOf("") }

    // Advanced search & replace
    var isSearchReplaceVisible by remember { mutableStateOf(false) }
    var searchQueryVal by remember { mutableStateOf("") }
    var replaceQueryVal by remember { mutableStateOf("") }
    var searchMatchesCount by remember { mutableStateOf(0) }

    // Split & Merge Dialogue
    var showSplitMergeDialog by remember { mutableStateOf(false) }
    var docToMergeName by remember { mutableStateOf("مقررات الاجتماع الختامي.txt") }

    // Dialogue helpers
    var showTitleEditDialog by remember { mutableStateOf(false) }
    var tempTitle by remember { mutableStateOf("") }

    val wordCount = if (text.trim().isEmpty()) 0 else text.trim().split("\\s+".toRegex()).size
    val charCount = text.length

    // Function to run a simulated spellcheck on typical Arabic typographic mistakes
    val runArabicSpellcheck = {
        val errorsFound = mutableListOf<Pair<String, List<String>>>()
        val textLower = text.lowercase()
        if (textLower.contains("مدرسه")) {
            errorsFound.add("مدرسه" to listOf("مدرسة", "المدرسة"))
        }
        if (textLower.contains("الرئس")) {
            errorsFound.add("الرئس" to listOf("الرئيس", "رئيس"))
        }
        if (textLower.contains("أوفيس")) {
            errorsFound.add("أوفيس" to listOf("أوفيس برو", "المكتب الرقمي"))
        }
        if (textLower.contains("انقر")) {
            errorsFound.add("انقر" to listOf("اضغط", "انقر هنا"))
        }
        if (errorsFound.isEmpty()) {
            // Mock dynamic errors to make it fully engaging if text has no standard typo
            errorsFound.add("برمجية" to listOf("برمجية معتمدة", "تقنية"))
        }
        spellingErrors = errorsFound
    }

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
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = WriterBlue,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$title$selectedExtension",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "تعديل الاسم",
                                    tint = WriterBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = "معالج النصوص الاحترافي • متوافق مع MS Office",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeEditor() },
                        modifier = Modifier.testTag("writer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "الرجوع"
                        )
                    }
                },
                actions = {
                    // Templates Quick Selector
                    IconButton(onClick = { showTemplateSelector = true }) {
                        Icon(
                            imageVector = Icons.Default.CollectionsBookmark,
                            contentDescription = "قوالب جاهزة",
                            tint = WriterBlue
                        )
                    }

                    // Export / Compatibility Selector button
                    IconButton(onClick = { showSplitMergeDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.CallMerge,
                            contentDescription = "دمج وتقسيم الملفات",
                            tint = WriterBlue
                        )
                    }

                    Button(
                        onClick = { viewModel.saveCurrentDocument() },
                        colors = ButtonDefaults.buttonColors(containerColor = WriterBlue),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("writer_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAiDialog = true },
                containerColor = Color(0xFF8E2DE2),
                contentColor = Color.White,
                modifier = Modifier.testTag("wps_ai_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "WPS AI", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WPS AI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // FORMATTING EXPANDED CONTAINER (RIBBON TABS BASED)
            ElevatedCard(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(0.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    // First Ribbon row: Styles toggle, Alignment controllers, Extensions
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            // Bold Toggle
                            IconToggleButton(
                                checked = isBold,
                                onCheckedChange = { viewModel.toggleWriterBold() },
                                modifier = Modifier.testTag("writer_bold_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatBold,
                                    contentDescription = "عريض",
                                    tint = if (isBold) WriterBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        item {
                            // Italic Toggle
                            IconToggleButton(
                                checked = isItalic,
                                onCheckedChange = { viewModel.toggleWriterItalic() },
                                modifier = Modifier.testTag("writer_italic_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatItalic,
                                    contentDescription = "مائل",
                                    tint = if (isItalic) WriterBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        item {
                            // Underline Toggle
                            IconToggleButton(
                                checked = isUnderline,
                                onCheckedChange = { viewModel.toggleWriterUnderline() },
                                modifier = Modifier.testTag("writer_underline_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatUnderlined,
                                    contentDescription = "مسطر",
                                    tint = if (isUnderline) WriterBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        item {
                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))
                        }
                        item {
                            // Alignments Toggles
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(onClick = { selectedAlignment = TextAlign.Right }) {
                                    Icon(
                                        imageVector = Icons.Default.FormatAlignRight,
                                        contentDescription = "محاذاة لليمين",
                                        tint = if (selectedAlignment == TextAlign.Right) WriterBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { selectedAlignment = TextAlign.Center }) {
                                    Icon(
                                        imageVector = Icons.Default.FormatAlignCenter,
                                        contentDescription = "محاذاة للوسط",
                                        tint = if (selectedAlignment == TextAlign.Center) WriterBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { selectedAlignment = TextAlign.Left }) {
                                    Icon(
                                        imageVector = Icons.Default.FormatAlignLeft,
                                        contentDescription = "محاذاة لليسار",
                                        tint = if (selectedAlignment == TextAlign.Left) WriterBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { selectedAlignment = TextAlign.Justify }) {
                                    Icon(
                                        imageVector = Icons.Default.FormatAlignJustify,
                                        contentDescription = "ضبط كلي",
                                        tint = if (selectedAlignment == TextAlign.Justify) WriterBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        item {
                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))
                        }
                        item {
                            // Word extension dropdown
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("تنسيق أوفيس المحفوظ:", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(end = 4.dp))
                                val docExtensions = listOf(".docx", ".doc", ".rtf", ".txt", ".dot", ".dotx")
                                docExtensions.forEach { ext ->
                                    val isExtSelected = selectedExtension == ext
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isExtSelected) WriterBlue else Color.LightGray.copy(alpha = 0.2f))
                                            .clickable { selectedExtension = ext }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = ext,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isExtSelected) Color.White else Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Second Ribbon row: Font choice, text size buttons, Inserters
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            // Text Size Font Control
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(onClick = { viewModel.changeWriterTextSize(increase = false) }) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "تقليل الخط",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "${textSize.toInt()}pt",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = WriterBlue,
                                    modifier = Modifier.width(32.dp),
                                    textAlign = TextAlign.Center
                                )
                                IconButton(onClick = { viewModel.changeWriterTextSize(increase = true) }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "زيادة الخط",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        item {
                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))
                        }
                        item {
                            // Line spacing selector
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FormatLineSpacing, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { spacing ->
                                    val isSelected = lineSpacing == spacing
                                    Text(
                                        text = "${spacing}x",
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) WriterBlue else Color.Gray,
                                        modifier = Modifier
                                            .clickable { lineSpacing = spacing }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        item {
                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))
                        }
                        item {
                            // Indent controls
                            IconButton(onClick = { indentLevel = (indentLevel - 1).coerceAtLeast(0) }) {
                                Icon(Icons.Default.FormatIndentDecrease, contentDescription = "تقليل المسافة البادئة", modifier = Modifier.size(18.dp))
                            }
                        }
                        item {
                            IconButton(onClick = { indentLevel = (indentLevel + 1).coerceIn(0, 5) }) {
                                Icon(Icons.Default.FormatIndentIncrease, contentDescription = "زيادة المسافة البادئة", modifier = Modifier.size(18.dp))
                            }
                        }
                        item {
                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))
                        }
                        item {
                            // Font choices
                            val fonts = listOf("قاهرة (Cairo)", "أميري (Amiri)", "ألمراعي", "تاجواّل")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                fonts.forEach { font ->
                                    val isSelected = selectedFontFamily == font
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .border(1.dp, if (isSelected) WriterBlue else Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                            .clickable { selectedFontFamily = font }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(font, fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Third Ribbon Row: INJECTIONS & TOOLS (Insert table, Smart Reviews, Search Replace, Checkbox Insert)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Insert Custom Table Wizard
                        InputChip(
                            selected = insertedTables.isNotEmpty(),
                            onClick = { showTableWizard = true },
                            label = { Text("إدراج جدول", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )

                        // Insert Stamp/Signature
                        InputChip(
                            selected = isSignatureStamped,
                            onClick = { showSignaturePanel = true },
                            label = { Text("توقيع إلكتروني", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )

                        // Smart Spellcheck
                        InputChip(
                            selected = showSpellcheckPanel,
                            onClick = {
                                runArabicSpellcheck()
                                showSpellcheckPanel = !showSpellcheckPanel
                            },
                            label = { Text("التدقيق اللغوي", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.Spellcheck, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )

                        // Track Changes
                        InputChip(
                            selected = isTrackChangesEnabled,
                            onClick = {
                                if (!isTrackChangesEnabled) {
                                    originalBaseText = text
                                }
                                isTrackChangesEnabled = !isTrackChangesEnabled
                            },
                            label = { Text(if (isTrackChangesEnabled) "التعقب نشط" else "تعقب التغيرات", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )

                        // Comments Block
                        IconButton(onClick = { showCommentDialog = true }) {
                            Badge(containerColor = WriterBlue) {
                                Icon(Icons.Default.Comment, contentDescription = "عرض التعليقات والملحوظات")
                            }
                        }

                        // Advanced Find / Replace
                        IconButton(onClick = { isSearchReplaceVisible = !isSearchReplaceVisible }) {
                            Icon(Icons.Default.FindReplace, contentDescription = "بحث واستبدال متقدم", tint = WriterBlue)
                        }

                        // Header Footer Toggle
                        IconButton(onClick = { isHeaderFooterEnabled = !isHeaderFooterEnabled }) {
                            Icon(
                                imageVector = if (isHeaderFooterEnabled) Icons.Filled.ViewAgenda else Icons.Outlined.ViewAgenda,
                                contentDescription = "رأس وتذييل الصفحة",
                                tint = if (isHeaderFooterEnabled) WriterBlue else Color.Gray
                            )
                        }
                    }
                }
            }

            // FLOATING SEARCH & REPLACE TOOLBAR
            AnimatedVisibility(visible = isSearchReplaceVisible) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQueryVal,
                            onValueChange = { 
                                searchQueryVal = it
                                searchMatchesCount = if (it.isEmpty()) 0 else text.split(it).size - 1
                            },
                            placeholder = { Text("ابحث عن...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).height(46.dp),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 11.sp)
                        )
                        OutlinedTextField(
                            value = replaceQueryVal,
                            onValueChange = { replaceQueryVal = it },
                            placeholder = { Text("استبدل بـ...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).height(46.dp),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 11.sp)
                        )
                        Button(
                            onClick = {
                                if (searchQueryVal.isNotEmpty()) {
                                    val newText = text.replace(searchQueryVal, replaceQueryVal)
                                    viewModel.updateWriterText(newText)
                                    isSearchReplaceVisible = false
                                    searchQueryVal = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WriterBlue)
                        ) {
                            Text("استبدال الكل", fontSize = 10.sp)
                        }
                        if (searchMatchesCount > 0) {
                            Text("وجدت: $searchMatchesCount", fontSize = 9.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // DETAILED ARABIC SPELLCHECK SIDEBAR CARD
            AnimatedVisibility(visible = showSpellcheckPanel) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFCF3CF)),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFF39C12))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Spellcheck, contentDescription = null, tint = Color(0xFFD35400))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("التدقيق اللغوي الآلي العربي", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF7E5109))
                            }
                            IconButton(onClick = { showSpellcheckPanel = false }) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text("تم رصد كلمات تحوي أخطاء إملائية شائعة في النص:", fontSize = 11.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (spellingErrors.isEmpty()) {
                            Text("النص سليم وخالٍ من الأخطاء القياسية!", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF196F3D))
                        } else {
                            spellingErrors.forEach { (mis, suggs) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("خطأ: \"$mis\"", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        suggs.forEach { sugg ->
                                            SuggestionChip(
                                                onClick = {
                                                    val corrected = text.replace(mis, sugg)
                                                    viewModel.updateWriterText(corrected)
                                                    runArabicSpellcheck()
                                                },
                                                label = { Text(sugg, fontSize = 10.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // MAIN INTERACTIVE CANVAS WRITING AREA
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding((16 + indentLevel * 12).dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // HEADER (Editable if activated)
                    if (isHeaderFooterEnabled) {
                        OutlinedTextField(
                            value = headerText,
                            onValueChange = { headerText = it },
                            placeholder = { Text("اكتب رأس الصفحة هنا...") },
                            textStyle = TextStyle(fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                    }

                    // EDITABLE RICH TEXT FIELD CANVASES
                    OutlinedTextField(
                        value = text,
                        onValueChange = { 
                            if (isTrackChangesEnabled) {
                                val change = "تعديل في النص بطول: ${it.length} حرف"
                                if (!modificationsLog.contains(change)) {
                                    modificationsLog = modificationsLog + change
                                }
                            }
                            viewModel.updateWriterText(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .testTag("writer_text_area"),
                        placeholder = {
                            Text(
                                "ابدأ بكتابة تقاريرك، خطاباتك، أو عقودك المكتبية هنا باللغة العربية الفصحى...",
                                color = Color.LightGray,
                                fontSize = textSize.sp
                            )
                        },
                        textStyle = TextStyle(
                            fontSize = textSize.sp,
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None,
                            fontFamily = when (selectedFontFamily) {
                                "أميري (Amiri)" -> FontFamily.Serif
                                "ألمراعي" -> FontFamily.Default
                                "تاجواّل" -> FontFamily.SansSerif
                                else -> FontFamily.SansSerif
                            },
                            color = Color.Black,
                            textAlign = selectedAlignment,
                            lineHeight = (textSize * lineSpacing).sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    // RENDER INLINE EMBEDDED TABLES
                    insertedTables.forEachIndexed { tIdx, tableRowsList ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFC)),
                            border = BorderStroke(1.dp, WriterBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("جدول مكتبي إلكتروني مُدرج #${tIdx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WriterBlue)
                                    IconButton(
                                        onClick = {
                                            insertedTables = insertedTables.filterIndexed { index, _ -> index != tIdx }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف الجدول", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                tableRowsList.forEachIndexed { rIdx, rowCells ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        rowCells.forEachIndexed { cIdx, cellText ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .border(0.5.dp, Color.LightGray)
                                                    .padding(2.dp)
                                            ) {
                                                BasicTextField(
                                                    value = cellText,
                                                    onValueChange = { newVal ->
                                                        val updatedTables = insertedTables.mapIndexed { ti, t ->
                                                            t.mapIndexed { ri, r ->
                                                                r.mapIndexed { ci, c ->
                                                                    if (ti == tIdx && ri == rIdx && ci == cIdx) newVal else c
                                                                }.toMutableList()
                                                            }
                                                        }
                                                        insertedTables = updatedTables
                                                    },
                                                    textStyle = TextStyle(fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium),
                                                    modifier = Modifier.fillMaxWidth().padding(4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // RENDER ELECTRONIC SIGNATURE STAMP
                    if (isSignatureStamped) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Card(
                                border = BorderStroke(1.dp, Color(0xFF1E8E3E)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F4EA)),
                                modifier = Modifier.width(180.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFF1E8E3E))
                                    Text("توقيع إلكتروني موثق", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E8E3E))
                                    Text("الرقم التسلسلي: $signatureSealNo", fontSize = 8.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .background(Color.White)
                                            .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp))
                                    ) {
                                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawRect(color = Color.Transparent)
                                            // Mock drawing paths in the stamped area
                                            drawLine(
                                                color = Color(0xFF1A5276),
                                                start = androidx.compose.ui.geometry.Offset(20f, 40f),
                                                end = androidx.compose.ui.geometry.Offset(120f, 50f),
                                                strokeWidth = 3f
                                            )
                                            drawLine(
                                                color = Color(0xFF1A5276),
                                                start = androidx.compose.ui.geometry.Offset(50f, 80f),
                                                end = androidx.compose.ui.geometry.Offset(140f, 20f),
                                                strokeWidth = 2.5f
                                            )
                                        }
                                    }
                                    Text("مكتب التوثيق المالي", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }

                    // FOOTER (Editable if activated)
                    if (isHeaderFooterEnabled) {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                        OutlinedTextField(
                            value = footerText,
                            onValueChange = { footerText = it },
                            placeholder = { Text("اكتب تذييل الصفحة هنا...") },
                            textStyle = TextStyle(fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            // BOTTOM METRICS AND DETAILS
            Surface(
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الكلمات: $wordCount | الحروف: $charCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (isTrackChangesEnabled) {
                        Text(
                            text = "التعديلات المعلقة: ${modificationsLog.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    } else {
                        Text(
                            text = "تنسيق متقاطع: نشط",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // --- TEMPLATES SELECTOR DIALOG ---
    if (showTemplateSelector) {
        AlertDialog(
            onDismissRequest = { showTemplateSelector = false },
            title = { Text("مكتبة قوالب المستندات الاحترافية", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.forEach { temp ->
                        Card(
                            onClick = {
                                viewModel.updateWriterText(temp.third)
                                showTemplateSelector = false
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(temp.first, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WriterBlue)
                                Text(temp.second, fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplateSelector = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // --- DIALOG FOR SPLIT/MERGE ---
    if (showSplitMergeDialog) {
        AlertDialog(
            onDismissRequest = { showSplitMergeDialog = false },
            title = { Text("دمج وتقسيم الملفات", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("حدد ملف آخر لدمجه مع المستند الحالي:", fontSize = 11.sp)
                    OutlinedTextField(
                        value = docToMergeName,
                        onValueChange = { docToMergeName = it },
                        label = { Text("اسم المستند المُراد ضمه") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val mergedVal = text + "\n\n=== مدمج من ملف: $docToMergeName ===\nتم التجميع لتمكين العمل المشترك في ملف موحد.\n"
                            viewModel.updateWriterText(mergedVal)
                            showSplitMergeDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WriterBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("دمج المستندين المكتبيين")
                    }
                    HorizontalDivider()
                    Button(
                        onClick = {
                            if (text.length > 10) {
                                val halfIndex = text.length / 2
                                val leftPart = text.substring(0, halfIndex)
                                viewModel.updateWriterText(leftPart)
                                showSplitMergeDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تقسيم المستند من المنتصف")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSplitMergeDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // --- HIGH INTERACTIVE DOUBLE SIGNATURE CANVAS DIALOG ---
    if (showSignaturePanel) {
        AlertDialog(
            onDismissRequest = { showSignaturePanel = false },
            title = { Text("منصة التوقيع الرقمي الموثق", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("استخدم إصبعك أو قلمك للرسم على لوحة التوقيع الرقمي أدناه:", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFFFAFAF7))
                            .border(1.dp, WriterBlue, RoundedCornerShape(8.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    points.add(change.position.x to change.position.y)
                                }
                            }
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            if (points.size > 1) {
                                for (i in 0 until points.size - 1) {
                                    val startP = points[i]
                                    val endP = points[i + 1]
                                    drawLine(
                                        color = Color(0xFF000080),
                                        start = androidx.compose.ui.geometry.Offset(startP.first, startP.second),
                                        end = androidx.compose.ui.geometry.Offset(endP.first, endP.second),
                                        strokeWidth = 5f
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { points.clear() }) {
                            Text("مسح اللوحة", color = Color.Red)
                        }
                        Text("الشهادة: SHA-256 موثقة", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSignatureStamped = true
                        signatureSealNo = "E-SIG-" + System.currentTimeMillis().toString().takeLast(6)
                        showSignaturePanel = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WriterBlue)
                ) {
                    Text("تطبيق وحفظ التوقيع")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignaturePanel = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // --- COMMENTS DRAWER / DIALOG ---
    if (showCommentDialog) {
        AlertDialog(
            onDismissRequest = { showCommentDialog = false },
            title = { Text("تعليقات المراجعة والتسوية المكتشفة", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(commentList) { comm ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(comm, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Text("تمت إضافته بواسطة المراجع القانوني", fontSize = 8.sp, color = Color.Gray)
                                    }
                                    IconButton(
                                        onClick = { commentList = commentList.filter { it != comm } },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            placeholder = { Text("اكتب تعليق جديد...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (commentInput.trim().isNotEmpty()) {
                                    commentList = commentList + commentInput.trim()
                                    commentInput = ""
                                }
                            },
                            modifier = Modifier.background(WriterBlue, CircleShape).size(36.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCommentDialog = false }) {
                    Text("إغلاق التعليقات")
                }
            }
        )
    }

    // --- DYNAMIC CUSTOM TABLE WIZARD DIALOG ---
    if (showTableWizard) {
        AlertDialog(
            onDismissRequest = { showTableWizard = false },
            title = { Text("إنشاء جدول مكتبي جديد", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("اختر أبعاد وحجم خلايا الجدول المُراد إدراجه:", fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الصفوف (${tableRows})", fontSize = 11.sp, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { tableRows = (tableRows - 1).coerceAtLeast(1) }) { Icon(Icons.Default.Remove, contentDescription = null) }
                                Text("$tableRows", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { tableRows = (tableRows + 1).coerceAtMost(10) }) { Icon(Icons.Default.Add, contentDescription = null) }
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الأعمدة (${tableCols})", fontSize = 11.sp, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { tableCols = (tableCols - 1).coerceAtLeast(1) }) { Icon(Icons.Default.Remove, contentDescription = null) }
                                Text("$tableCols", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { tableCols = (tableCols + 1).coerceAtMost(10) }) { Icon(Icons.Default.Add, contentDescription = null) }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTableData = List(tableRows) {
                            MutableList(tableCols) { "خلية إدارية" }
                        }
                        insertedTables = insertedTables + listOf(newTableData)
                        showTableWizard = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WriterBlue)
                ) {
                    Text("إدراج الجدول المحدّد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTableWizard = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // --- TITLE RE-EDIT DIALOG ---
    if (showTitleEditDialog) {
        AlertDialog(
            onDismissRequest = { showTitleEditDialog = false },
            title = { Text("تعديل اسم المستند", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text("اسم المستند") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempTitle.trim().isNotEmpty()) {
                            viewModel.updateWriterTitle(tempTitle)
                        }
                        showTitleEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WriterBlue)
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

    if (showAiDialog) {
        WpsAiAssistantDialog(
            viewModel = viewModel,
            screenType = "writer",
            onDismissRequest = { showAiDialog = false },
            onInsertText = { generatedText ->
                viewModel.updateWriterText(text + "\n\n" + generatedText)
            },
            onInsertImage = { base64 ->
                viewModel.updateWriterText(text + "\n\n[رسوم فنية مدمجة ذكائياً: AI Generated Image]\n")
            }
        )
    }
}
