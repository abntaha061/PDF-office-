package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FileManagerTeal
import com.example.ui.viewmodel.OfficeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: OfficeViewModel,
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPremiumUser by viewModel.isPremiumUser.collectAsState()
    val appDefaultFont by viewModel.appDefaultFont.collectAsState()

    var toolbarCompactMode by remember { mutableStateOf(false) }
    var cloudBackupSync by remember { mutableStateOf(true) }
    var autoSaveEnabled by remember { mutableStateOf(true) }

    // Expanded accordion states for help guides
    var expandedGuide by remember { mutableStateOf<String?>(null) }

    // Premium UI Gradient
    val goldPremiumGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF1C40F), // Gold
            Color(0xFFE67E22), // Orange Gold
            Color(0xFFD35400)  // Deep Amber
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "إعدادات وتخصيص التطبيق المكتبي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setCurrentTab("file_manager") },
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "الرجوع"
                        )
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. ADVANCED BUSINESS / SUBSCRIPTION MODEL CARD
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(
                            width = if (isPremiumUser) 2.dp else 1.dp,
                            brush = if (isPremiumUser) goldPremiumGradient else Brush.linearGradient(
                                listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f))
                            )
                        ),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPremiumUser) Color(0xFF131109) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPremiumUser) Icons.Default.Stars else Icons.Default.Stars,
                                contentDescription = null,
                                tint = if (isPremiumUser) Color(0xFFF1C40F) else Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPremiumUser) "عضوية WPS Premium الذهبية نشطة!" else "ترقية الاشتراك إلى العضوية الفائقة (VIP)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPremiumUser) Color(0xFFF1C40F) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isPremiumUser) {
                            Box(
                                modifier = Modifier
                                    .background(goldPremiumGradient, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "PREMIUM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "حساب مجاني",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = "استمتع بمميزات الذكاء الاصطناعي الفائقة اللامحدودة، ومساحة مخصصة لحفظ وتصدير المستندات، ودعم شامل لبنية الملفات الضخمة وصناعة الصور الفنية مجاناً.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Premium Benefits list
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        BenefitRow(icon = Icons.Default.AutoAwesome, text = "وصول كامل وفوري لجميع محركات WPS AI الذكية")
                        BenefitRow(icon = Icons.Default.CloudSync, text = "حفظ ومزامنة سحابية فائقة تصل لـ 100 جيجابايت آمنة")
                        BenefitRow(icon = Icons.Default.Image, text = "توليد لوحات وصور فنية بجودات عالية لمستنداتك")
                        BenefitRow(icon = Icons.Default.Translate, text = "ترجمة احترافية لا محدودة لأكثر من 20 لغة عالمية")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Subscribe action button (Simulated Upgrade)
                    Button(
                        onClick = { viewModel.togglePremiumUser() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPremiumUser) Color(0xFFC0392B) else Color(0xFFD35400)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isPremiumUser) Icons.Default.Close else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPremiumUser) "إلغاء الاشتراك الفائق والعودة للوضع المجاني" else "تفعيل الحساب الذهبي الآن (إصدار تجريبي كامل بقيمة 15 ريال/شهر)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // ==========================================
            // 2. APPEARANCE & CUSTOMIZATION preferences
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "المظهر والتخصيص الفردي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Dark Theme Toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDarkThemeChange(!isDarkTheme) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "تفعيل اللمسة الداكنة (Dark Mode)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "تصفح مستندات مريح لحماية العين من الإجهاد",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = onDarkThemeChange,
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Font Size Preference Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "حجم الخط الافتراضي لعرض المحرر والمستندات", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(12, 14, 16, 18).forEach { size ->
                                val isSelected = size == appDefaultFont
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            BorderStroke(
                                                width = 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            ),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.setAppDefaultFont(size) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$size pt",
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. STORAGE & OFFLINE COOPERATIVE PREFERENCES
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "خيارات الحفظ السحابي والعمل دون اتصال (Offline)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Toolbar Sizing toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { toolbarCompactMode = !toolbarCompactMode }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerticalSplit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "تفعيل الحجم المدمج لشريط الأدوات", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "يقلص حجم الأيقونات لتسريع المساحة الكتابية",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = toolbarCompactMode,
                            onCheckedChange = { toolbarCompactMode = it },
                            modifier = Modifier.testTag("toolbar_sizing_switch")
                        )
                    }

                    // AutoSave Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { autoSaveEnabled = !autoSaveEnabled }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "الحفظ التلقائي الفوري لمقررات المكتب", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "يحفظ ملفاتك محلياً بشكل مستمر لتفادي فقدانها وتوفير الدعم الكلي للأوفلاين",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = autoSaveEnabled,
                            onCheckedChange = { autoSaveEnabled = it },
                            modifier = Modifier.testTag("auto_save_switch")
                        )
                    }

                    // Cloud backup toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { cloudBackupSync = !cloudBackupSync }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "مزامنة سحابية مستمرة", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "النسخ الاحتياطي التلقائي عند استعادة الاتصال بالإنترنت",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = cloudBackupSync,
                            onCheckedChange = { cloudBackupSync = it },
                            modifier = Modifier.testTag("cloud_sync_switch")
                        )
                    }

                    // Offline safe state details
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FileManagerTeal.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = FileManagerTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "دعم كامل وتلقائي للعمل الأوفلاين (Offline): جميع تعديلاتك تُطبق محلياً بنظام أمان مشفر وتزامن فور توفر الشبكة.",
                                color = FileManagerTeal,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 4. USER INSTRUCTIONAL MANUALS & GUIDES (الأدلة التعليمية للبرنامج)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "الدليل التعليمي لخصائص WPS AI والمستندات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "اختر أحد الأدلة التالية لقراءة خطوات تشغيل الميزات وكيفية مضاعفة إنتاجيتك التحريرية وميزات اللغات التفاعلية:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Accordion 1: WPS AI Assistant Guide
                    AccordionGuideItem(
                        title = "١. تشغيل واستثمار مساعد WPS AI الذكي",
                        summary = "تعرف على كيفية توليد النصوص، التلخيص الهيكلي، وتحليل البيانات بالذكاء الاصطناعي.",
                        content = "• يمكنك فتح المساعد عبر زر الفلوتينج (WPS AI) الموجود في أسفل كل شريحة/مستند.\n• اختر تبويب 'مساعد الكتابة' لكتابة مسودات ترويجية بالذكاء الاصطناعي أو 'تحسين الصياغة' لتطوير النص.\n• في الجداول (Spreadsheets)، اضغط على زر 'تحليل وتفسير بيانات الجدول' لتقصي البيانات رقمياً.\n• في العروض (Presentation)، اكتب موضوع الشريحة وسيقوم النظام ببناء هيكل كامل تفصيلي بالكامل وتوليد الرسوم بنقرة زر واحدة فورية.",
                        isSelected = expandedGuide == "ai_helper",
                        onClick = { expandedGuide = if (expandedGuide == "ai_helper") null else "ai_helper" }
                    )

                    // Accordion 2: Interactive PDF Tools
                    AccordionGuideItem(
                        title = "٢. ميزات PDF التفاعلية وتعدد اللغات",
                        summary = "نطق الكلمات والترجمة والمصطلحات الأجنبية (الألمانية والإنجليزية).",
                        content = "• يحتوي تبويب PDF على مستندات رسمية ذات كلمات حساسة ومعمقة بالإنجليزية والألمانية.\n• اضغط على أي كلمة مميزة بالألوان لفتح القاموس الفوري لعرض الترجمة والمعنى والشروح التفصيلية.\n• انقر على أيقونة المكبر الصوتي (🗣️) لسماع النطق الصوتي الصحيح باللغة الألمانية أو الإنجليزية مع مخارج الحروف لتسهيل الدراسة الأكاديمية.\n• استخدام دردشة PDF المتقدمة لكتابة وصياغة الأسئلة والحصول على إجابات مباشرة مقتطعة ومطابقة بدقة تامة.",
                        isSelected = expandedGuide == "pdf_tools",
                        onClick = { expandedGuide = if (expandedGuide == "pdf_tools") null else "pdf_tools" }
                    )

                    // Accordion 3: Scanner and OCR
                    AccordionGuideItem(
                        title = "٣. استخدام الماسح الضوئي (Scanner) واستخراج النصوص",
                        summary = "مسح المستندات وصور العقود واستخلاص الكلمات العربية بفلترة ذكية.",
                        content = "• افتح تبويب 'الماسح الضوئي' لالتقاط صورة العقد الورقي أو الهوية للتحليل.\n• اضبط الزوايا يدوياً عبر سحب الدوائر الأربعة لتسوية الورقة بصورة مسطحة وجميلة.\n• طبق فلاتر التحسين التلقائية (أبيض وأسود عالي التباين، أو محسن المخرجات).\n• انزل لتبويب OCR لاستخراج وحفظ المحتوى النصي العربي الموثق ونسخه للمحرر بنقرة زر واحدة.",
                        isSelected = expandedGuide == "scanner_ocr",
                        onClick = { expandedGuide = if (expandedGuide == "scanner_ocr") null else "scanner_ocr" }
                    )
                }
            }

            // ==========================================
            // 5. SYSTEM CREDIT INFO & RTL ASSURANCE
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "دعم كامل للغة العربية (RTL)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "التطبيق يدعم بالكامل الخطوط والاتجاهات من اليمين إلى اليسار (RTL) لحرية العمل الفني وإنتاج الجداول السليمة وتفسير النصوص بطلاقة.",
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "إصدار التطبيق الفيروزي الذهبي: v1.0.5",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BenefitRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFF1C40F),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
fun AccordionGuideItem(
    title: String,
    summary: String,
    content: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!isSelected) {
                        Text(
                            text = summary,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Icon(
                    imageVector = if (isSelected) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 8.dp))
                    Text(
                        text = content,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
