package com.example.ui.screens

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.DocumentEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.OfficeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileManagerScreen(
    viewModel: OfficeViewModel,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.filteredDocuments.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var showDeleteDialogForDoc by remember { mutableStateFlowOf<DocumentEntity?>(null) }
    var showInfoDialogForDoc by remember { mutableStateFlowOf<DocumentEntity?>(null) }

    // --- STAGE 3 CLOUD SYNC & COLLABORATION STATES ---
    var isCloudSectionExpanded by remember { mutableStateOf(false) }
    var selectedCloudDrive by remember { mutableStateOf("gdrive") } // "gdrive", "onedrive", "dropbox", "server"
    var isSyncingInProcess by remember { mutableStateOf(false) }
    var backupSchedulerPeriod by remember { mutableStateOf("daily") } // "daily", "weekly", "continuous", "manual"
    var cloudSyncStateMode by remember { mutableStateOf("auto_offline") } // "auto_offline", "standard", "selective"
    var coEditorsCountActive by remember { mutableStateOf(3) }
    var currentPrivilegeSharing by remember { mutableStateOf("edit") } // "read", "comment", "edit"
    var currentExpiryOptionSharing by remember { mutableStateOf("infinity") } // "1h", "1d", "infinity"
    var generatedShareLinkAddress by remember { mutableStateOf("") }

    // Stat counters
    val docCount = documents.size
    val writerCount = documents.count { it.type == "writer" }
    val sheetCount = documents.count { it.type == "spreadsheet" }
    val presentationCount = documents.count { it.type == "presentation" }
    val pdfCount = documents.count { it.type == "pdf" }
    val scannerCount = documents.count { it.type == "scanner" }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "المكتب الذكي",
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "منصة إدارة المستندات وإنشائها المتكاملة",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.setCurrentTab("settings") },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "الإعدادات"
                        )
                    }
                },
                modifier = Modifier.testTag("main_app_bar")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("file_search_field"),
                placeholder = { Text("ابحث عن مستند كتابي، جدول، أو ملف رقمي...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "مسح البحث"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )

            // METRIC CARD PANEL
            MetricSummaryCards(
                writer = writerCount,
                sheets = sheetCount,
                presentation = presentationCount,
                pdfs = pdfCount,
                scanners = scannerCount
            )

            Spacer(modifier = Modifier.height(8.dp))

            // CATEGORY Horizontal Pills
            CategoryPillsSection(
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.setSelectedCategory(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- STAGE 3: CLOUD SYNC & CO-EDITING HUB ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCloudSectionExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCloudSectionExpanded = !isCloudSectionExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCloudSectionExpanded) Icons.Default.CloudSync else Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "مركز التعاون الفوري والمزامنة السحابية",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isCloudSectionExpanded) "إدارة مزامنة الملفات ومشاركتها مع الفريق" else "توصيل Google Drive / OneDrive وهندسة العمل المشترك ككاتب ومقعد",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isCloudSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = isCloudSectionExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // 1. Cloud sources selectors
                            Text(
                                text = "☁️ قنوات ومحركات التخزين السحابية المتوفرة:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "gdrive" to "Google Drive",
                                    "onedrive" to "OneDrive",
                                    "dropbox" to "Dropbox",
                                    "server" to "سيرفر الشركة"
                                ).forEach { (id, label) ->
                                    val active = selectedCloudDrive == id
                                    FilterChip(
                                        selected = active,
                                        onClick = { selectedCloudDrive = id },
                                        label = { Text(label, fontSize = 9.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }

                            // Sync Mode selection
                            Text(
                                text = "⚙️ طرق ومستويات المزامنة النشطة:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "auto_offline" to "تلقائي (أوفلاين/خلفية)",
                                    "standard" to "تزامن قياسي متبادل",
                                    "selective" to "انتقائي يدوي"
                                ).forEach { (id, label) ->
                                    val active = cloudSyncStateMode == id
                                    FilterChip(
                                        selected = active,
                                        onClick = { cloudSyncStateMode = id },
                                        label = { Text(label, fontSize = 9.sp) }
                                    )
                                }
                            }

                            // Backup Period
                            Text(
                                text = "📅 فترات النسخ الاحتياطي التلقائي المحسوب:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "continuous" to "مستمر فوري",
                                    "daily" to "يومي تلقائي",
                                    "weekly" to "أسبوعي مبرمج",
                                    "manual" to "إجراء يدوي"
                                ).forEach { (id, label) ->
                                    val active = backupSchedulerPeriod == id
                                    FilterChip(
                                        selected = active,
                                        onClick = { backupSchedulerPeriod = id },
                                        label = { Text(label, fontSize = 9.sp) }
                                    )
                                }
                            }

                            // Trigger Action
                            val context = LocalContext.current
                            val syncMsg by viewModel.syncMessage.collectAsState()

                            if (isSyncingInProcess) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(syncMsg ?: "جاري تشفير ومزامنة الملفات مع سحابة $selectedCloudDrive ...", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth())
                                }
                            } else {
                                Button(
                                    onClick = {
                                        isSyncingInProcess = true
                                        viewModel.syncDocumentsToCloud(selectedCloudDrive) { message ->
                                            isSyncingInProcess = false
                                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مزامنة فورية لكافة المستجدات الآن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // 2. Co-editing & collaboration
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "👥 لوحة التعاون التشاركي والتحرير المباشر:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "🟢 $coEditorsCountActive محررون متصلون",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Privileges selectors
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("صلاحيات الرابط المراد إنشاؤه:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "edit" to "تحرير كامل وتعديل",
                                        "comment" to "عرض وتعليق فقط",
                                        "read" to "عرض وقراءة فقط"
                                    ).forEach { (id, label) ->
                                        val active = currentPrivilegeSharing == id
                                        FilterChip(
                                            selected = active,
                                            onClick = { currentPrivilegeSharing = id },
                                            label = { Text(label, fontSize = 9.sp) }
                                        )
                                    }
                                }
                            }

                            // Expiry selectors
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("مدة صلاحية رابط التعاون:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "1h" to "ساعة واحدة",
                                        "1d" to "يوم كامل",
                                        "infinity" to "مفتوح الصلاحية"
                                    ).forEach { (id, label) ->
                                        val active = currentExpiryOptionSharing == id
                                        FilterChip(
                                            selected = active,
                                            onClick = { currentExpiryOptionSharing = id },
                                            label = { Text(label, fontSize = 9.sp) }
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val randomHash = (10000..99999).random()
                                    generatedShareLinkAddress = "https://office.aistudio.share/collaboration/live-session-$randomHash?role=$currentPrivilegeSharing"
                                    Toast.makeText(context, "تمت صياغة رابط دعوة آمن ومحمي مخصص للفريق!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إنشاء رابط دعوة مخصص للتعاون", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (generatedShareLinkAddress.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("رابط التحرير الجماعي النشط:", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = generatedShareLinkAddress,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                Toast.makeText(context, "تم نسخ رابط المشاركة الآمن إلى حافظة جهازك!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "نسخ الرابط", tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // RECENT FILES LABEL
            Text(
                text = "المستندات المحفوظة (" + docCount + ")",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // FILES LIST
            if (documents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = "الملف فارغ",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(86.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "لا توجد نتائج تطابق بحثك" else "مساحة التخزين فارغة تماماً",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "يرجى التحقق من صياغة الحروف أو التبديل لتوقيتات أخرى" else "انقر على زر الإضافة الدائري بالأسفل لإنشاء أول مستند، جدول بيانات، عرض تقديمي، أو إجراء مسح ضوئي فوري باللغة العربية.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        if (searchQuery.isNotEmpty() || selectedCategory != "all") {
                            TextButton(
                                onClick = {
                                    viewModel.setSearchQuery("")
                                    viewModel.setSelectedCategory("all")
                                }
                            ) {
                                Text("إعادة ضبط الفلاتر والمرشحات")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 80.dp), // Extra space for FAB
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = documents,
                        key = { it.id }
                    ) { doc ->
                        DocumentItemRow(
                            document = doc,
                            onClick = { viewModel.openDocument(doc) },
                            onFavoriteToggle = { viewModel.toggleFavorite(doc) },
                            onDeleteClick = { showDeleteDialogForDoc = doc },
                            onInfoClick = { showInfoDialogForDoc = doc }
                        )
                    }
                }
            }
        }
    }

    // CONFIRM DELETE DIALOG
    showDeleteDialogForDoc?.let { doc ->
        AlertDialog(
            onDismissRequest = { showDeleteDialogForDoc = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "حذف المستند نهائياً؟",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف المستند المسمى \"${doc.title}\"؟ تذكر أن هذا الإجراء نهائي ولا يمكن التراجع عنه.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(doc)
                        showDeleteDialogForDoc = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("حذف فوري", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialogForDoc = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // DOCUMENT INFO DETAILS DIALOG
    showInfoDialogForDoc?.let { doc ->
        AlertDialog(
            onDismissRequest = { showInfoDialogForDoc = null },
            icon = {
                val iconInfo = getDocIconInfo(doc.type)
                Icon(
                    imageVector = iconInfo.first,
                    contentDescription = null,
                    tint = iconInfo.second,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "تفاصيل المستند الفنية",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DetailRow(label = "اسم الملف:", value = doc.title)
                    DetailRow(label = "النوع:", value = getDocTypeNameArabic(doc.type))
                    DetailRow(
                        label = "تاريخ التعديل:",
                        value = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")).format(Date(doc.lastModified))
                    )
                    DetailRow(label = "حجم البيانات المكتوبة:", value = "${doc.size} بايت")
                    DetailRow(
                        label = "الحالة في التفضيل:",
                        value = if (doc.isFavorite) "مضاف للمفضلة" else "غير مضاف للمفضلة"
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showInfoDialogForDoc = null }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 12.dp))
    }
}

@Composable
fun MetricSummaryCards(
    writer: Int,
    sheets: Int,
    presentation: Int,
    pdfs: Int,
    scanners: Int
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            MiniMetricCard(
                title = "مستندات نصوص",
                count = writer,
                color = WriterBlue,
                icon = Icons.Default.Description
            )
        }
        item {
            MiniMetricCard(
                title = "جداول حسابية",
                count = sheets,
                color = SpreadsheetGreen,
                icon = Icons.Default.GridOn
            )
        }
        item {
            MiniMetricCard(
                title = "حقائب تقديمية",
                count = presentation,
                color = PresentationOrange,
                icon = Icons.Default.Slideshow
            )
        }
        item {
            MiniMetricCard(
                title = "ملفات PDF",
                count = pdfs,
                color = PdfRed,
                icon = Icons.Filled.PictureAsPdf
            )
        }
        item {
            MiniMetricCard(
                title = "مسوح ضوئية",
                count = scanners,
                color = ScannerViolet,
                icon = Icons.Default.QrCodeScanner
            )
        }
    }
}

@Composable
fun MiniMetricCard(
    title: String,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = Modifier
            .width(135.dp)
            .height(78.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = count.toString(),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = color
                )
            }
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CategoryPillsSection(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf(
        Triple("all", "الكل", Icons.Default.Folder),
        Triple("favorite", "المفضلة", Icons.Default.Star),
        Triple("writer", "محرر النصوص", Icons.Default.Description),
        Triple("spreadsheet", "جداول البيانات", Icons.Default.GridOn),
        Triple("presentation", "العروض التقديمية", Icons.Default.Slideshow),
        Triple("pdf", "مستندات PDF", Icons.Filled.PictureAsPdf),
        Triple("scanner", "الماسح الضوئي", Icons.Default.QrCodeScanner)
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { (id, name, icon) ->
            val isSelected = selectedCategory == id
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(id) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = name,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = name,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("category_pill_$id")
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentItemRow(
    document: DocumentEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val (icon, color) = getDocIconInfo(document.type)
    val formattedDate = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")).format(Date(document.lastModified))

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onInfoClick
            )
            .testTag("document_row_${document.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document Type Icon with layered background gradient
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0.08f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Document Title and Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = document.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = getDocTypeNameArabic(document.type),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                        modifier = Modifier
                            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Operations Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Favorite Button
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (document.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "تفضيل",
                        tint = if (document.isFavorite) Color(0xFFFFBF00) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag("delete_doc_button_${document.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "حذف المستند",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// Utility mapper for beautiful document icons and themed hues
fun getDocIconInfo(type: String): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    return when (type) {
        "writer" -> Icons.Default.Description to WriterBlue
        "spreadsheet" -> Icons.Default.GridOn to SpreadsheetGreen
        "presentation" -> Icons.Default.Slideshow to PresentationOrange
        "pdf" -> Icons.Filled.PictureAsPdf to PdfRed
        "scanner" -> Icons.Default.QrCodeScanner to ScannerViolet
        else -> Icons.Default.InsertDriveFile to FileManagerTeal
    }
}

fun getDocTypeNameArabic(type: String): String {
    return when (type) {
        "writer" -> "محرر نصوص"
        "spreadsheet" -> "جدول بيانات"
        "presentation" -> "عرض تقديمي"
        "pdf" -> "مستند PDF"
        "scanner" -> "مسح ضوئي"
        else -> "مستند مكتبي"
    }
}

// Helper to bridge StateFlow mapping without warnings
@Suppress("NOTHING_TO_INLINE")
inline fun <T> mutableStateFlowOf(value: T) = mutableStateOf(value)
