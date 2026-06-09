package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.theme.*
import com.example.ui.viewmodel.OfficeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainOfficeScreen(
    viewModel: OfficeViewModel,
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importDocumentFromUri(context, it)
        }
    }

    // FORCE FULL RTL (Arabic Layout alignment direction from right to left)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val showBottomNavigation = currentTab == "file_manager" || currentTab == "settings"

        Scaffold(
            bottomBar = {
                if (showBottomNavigation) {
                    Column {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.background,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .testTag("app_navigation_bar")
                        ) {
                            // Tab 1: Documents list
                            NavigationBarItem(
                                selected = currentTab == "file_manager",
                                onClick = { viewModel.setCurrentTab("file_manager") },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == "file_manager") Icons.Filled.Folder else Icons.Outlined.Folder,
                                        contentDescription = "الملفات والحلائب"
                                    )
                                },
                                label = { Text("المستندات", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("nav_tab_documents")
                            )

                            // Tab 2: Settings and themes
                            NavigationBarItem(
                                selected = currentTab == "settings",
                                onClick = { viewModel.setCurrentTab("settings") },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == "settings") Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "التخصيص والإعدادات"
                                    )
                                },
                                label = { Text("الإعدادات", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("nav_tab_settings")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentTab == "file_manager") {
                    FloatingActionButton(
                        onClick = { showCreateDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .testTag("main_create_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إنشاء مستند جديد",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // SWITCH BOARD SCREENS ACCORDING TO NAVIGATION STATE
                when (currentTab) {
                    "file_manager" -> FileManagerScreen(viewModel = viewModel)
                    "writer" -> WriterScreen(viewModel = viewModel)
                    "spreadsheet" -> SpreadsheetScreen(viewModel = viewModel)
                    "presentation" -> PresentationScreen(viewModel = viewModel)
                    "pdf" -> PdfScreen(viewModel = viewModel)
                    "scanner" -> ScannerScreen(viewModel = viewModel)
                    "settings" -> SettingsScreen(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        onDarkThemeChange = onDarkThemeChange
                    )
                }
            }
        }

        // FLOATING ACTION SELECTION DIALOG (IN ARABIC)
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = {
                    Text(
                        text = "اختر نوع المستند الجديد",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "سنقوم بتهيئة وإعداد قالب مخصص فوري لملفاتك:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        // 1. Writer launcher
                        CreateItemRow(
                            title = "محرر نصوص كتابي (Word)",
                            description = "تعديل تقارير، كتابة خطابات ومذكرات",
                            icon = Icons.Default.Description,
                            color = WriterBlue,
                            onClick = {
                                viewModel.startNewDocument("writer")
                                showCreateDialog = false
                            },
                            tag = "create_writer_option"
                        )

                        // 2. Spreadsheet launcher
                        CreateItemRow(
                            title = "جدول حسابي وبيانات (Excel)",
                            description = "رياضيات وحسابات ومعادلات وخطوط شبكية",
                            icon = Icons.Default.GridOn,
                            color = SpreadsheetGreen,
                            onClick = {
                                viewModel.startNewDocument("spreadsheet")
                                showCreateDialog = false
                            },
                            tag = "create_spreadsheet_option"
                        )

                        // 3. Presentation launcher
                        CreateItemRow(
                            title = "عروض تقديمية وبوربوينت",
                            description = "تصميم حقائب تدريبية وعروض مرئية معقدة",
                            icon = Icons.Default.Slideshow,
                            color = PresentationOrange,
                            onClick = {
                                viewModel.startNewDocument("presentation")
                                showCreateDialog = false
                            },
                            tag = "create_presentation_option"
                        )

                        // 4. PDF digital document
                        CreateItemRow(
                            title = "ملف وثائقي PDF رقمي",
                            description = "صياغة عقود والتوقيع عليها بأصابع اليد فورا",
                            icon = Icons.Filled.PictureAsPdf,
                            color = PdfRed,
                            onClick = {
                                viewModel.startNewDocument("pdf")
                                showCreateDialog = false
                            },
                            tag = "create_pdf_option"
                        )

                        // 5. Document Scanner
                        CreateItemRow(
                            title = "ماسك مستندات ضوئي (Scanner)",
                            description = "مسح الفواتير بالهاتف واستخلاص نصوص العرب",
                            icon = Icons.Default.QrCodeScanner,
                            color = ScannerViolet,
                            onClick = {
                                viewModel.startNewDocument("scanner")
                                showCreateDialog = false
                            },
                            tag = "create_scanner_option"
                        )

                        // 6. Real File Importer
                        CreateItemRow(
                            title = "فتح ملف حقيقي من الهاتف",
                            description = "استيراد وقراءة ملفاتك الحقيقية (PDF, TXT, CSV, JSON)",
                            icon = Icons.Default.FolderOpen,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = {
                                filePickerLauncher.launch("*/*")
                                showCreateDialog = false
                            },
                            tag = "import_real_file_option"
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("إغلاق وإبطال")
                    }
                }
            )
        }
    }
}

@Composable
fun CreateItemRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        color = color.copy(alpha = 0.06f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
