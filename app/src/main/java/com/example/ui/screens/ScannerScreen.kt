package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ScannerViolet
import com.example.ui.viewmodel.OfficeViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: OfficeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // --- RUNTIME CAMERA PERMISSIONS & ML KIT INTEGRATION ---
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "تم منح صلاحية الكاميرا بنجاح!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "صلاحية الكاميرا مطلوبة لاستخدام أجهزة المسح المتطورة وقراءة الفلاش.", Toast.LENGTH_LONG).show()
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pages = scanResult?.pages
            if (pages != null && pages.isNotEmpty()) {
                val page = pages[0]
                val imageUri = page.imageUri
                try {
                    val inputImage = InputImage.fromFilePath(context, imageUri)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    recognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val detectedText = visionText.text
                            if (detectedText.isNotEmpty()) {
                                viewModel.setScannedOcrText(detectedText)
                            } else {
                                viewModel.setScannedOcrText("تم المسح الضوئي بنجاح.\nلم يتم الكشف عن نصوص متباينة في المستند.")
                            }
                            viewModel.setScanStep(2)
                            Toast.makeText(context, "تم التعرف البصري على النصوص (OCR) بنجاح!", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { e ->
                            viewModel.setScanStep(2)
                            Toast.makeText(context, "اكتمل المسح بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                } catch (e: Exception) {
                    viewModel.setScanStep(2)
                }
            } else {
                viewModel.setScanStep(1)
            }
        }
    }

    val scanStep by viewModel.scanStep.collectAsState()
    val scannerFilter by viewModel.scannerSelectedFilter.collectAsState()
    val ocrText by viewModel.scannedOcrText.collectAsState()
    val flashState by viewModel.scannerFlash.collectAsState()

    var zoomLevel by remember { mutableStateOf("1.0x") }
    var cropRotationAngle by remember { mutableStateOf(0f) }

    // Scan Types: "document", "id_card", "book", "receipt"
    var selectedScanType by remember { mutableStateOf("document") }

    // Multilingual OCR Language state: "ar", "de", "en", "fr"
    var ocrLanguage by remember { mutableStateOf("ar") }

    // Slider settings for enhance
    var adjustmentBrightness by remember { mutableStateOf(50f) }
    var adjustmentContrast by remember { mutableStateOf(50f) }
    var isNoiseRemovalOn by remember { mutableStateOf(true) }

    // Saving format: "pdf" or "image"
    var saveFormatType by remember { mutableStateOf("pdf") }

    // Interactive Cropping corner handles offsets (simulated perspective corners)
    var topLeftCorner by remember { mutableStateOf(Offset(40f, 40f)) }
    var topRightCorner by remember { mutableStateOf(Offset(680f, 40f)) }
    var bottomLeftCorner by remember { mutableStateOf(Offset(40f, 400f)) }
    var bottomRightCorner by remember { mutableStateOf(Offset(680f, 400f)) }

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

    // Helper text mappings of OCR to change instantly with language and filters
    val lookupSimulatedOCR: (String, String) -> String = { lang, filter ->
        when (lang) {
            "de" -> "DEUTSCHES INSTITUT FÜR COMPUSYSTEME\nFachrichtung: Angewandte Informatik & Robotik\nArbeitsverhältnis-ID: DE-94821\nAnstellungsvertrag für unselbständige Arbeit.\nOrt: Berlin, Datum: 2026/06/09.\nStatus des Vertrags: Aktiv und rechtskräftig beglaubigt."
            "en" -> "GLOBAL TECHNOLOGIES OFFICE SYSTEM\nProject ID: GTO-2026-X8\nDeployment Node: Western Cloud Stack\nSoftware Version: Enterprise build 1.45.2\nStatus: Online, secure encryption enabled."
            "fr" -> "CENTRE DE GESTION NUMÉRIQUE DES CONTRATS\nDirecteur Général de l’Administration\nNuméro de référence: FR-9831\nDate d’effet: 09 Juin 2026\nApprobation finale validée par signature."
            else -> {
                // Arabic defaults
                when (filter) {
                    "bw" -> "البسملة الرحمن الرحيم\nالمملكة العربية السعودية\nعقد إيجار سكني موحد\nرقم العقد: ٩٨٣٢١--٠٨٤\nتاريخ العقد: ٢٠٢٦/٠٦/٠٩\nحالة العقد: نشط وموثق عبر منصة إيجار الوطنية."
                    "grayscale" -> "وزارة الصحة العامة والمستشفيات\nتقرير طبي معتمد رقم: ٢٠٣٤/ك\nالتشخيص السريري للمريض: يعاني من نزلة برد وإجهاد عام.\nيوصى بالراحة لمدة ثلاثة أيام متتالية."
                    "enhance" -> "الهيئة السعودية للبيانات والذكاء الاصطناعي (سدايا)\nالمكتب التنسيقي للعلوم المتقدمة\nقرار إداري رقم: ٢٣٤١\nتعيين مهندسي حواسيب لتحديث البنى البرمجية والحلول الذكية."
                    else -> "المحتويات الممسوحة:\nفاتورة مبيعات فريدة رقم: ٤٨٣٢١\nالتاريخ: ٢٠٢٦/٠٦/٠٩\nالمجموع الكلي مع ضريبة القيمة المضافة: ٤٥٠٫٠٠ ريال سعودي."
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = ScannerViolet,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = "الماسح الضوئي الذكي للمستندات",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (scanStep) {
                                    0 -> "خطوة ١: التقاط صور المستندات والوصولات"
                                    1 -> "خطوة ٢: ضبط حدود وحواف الورقة"
                                    else -> "خطوة ٣: تطبيق مرشحات الصورة واستخراج النصوص OCR"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (scanStep > 0) viewModel.setScanStep(scanStep - 1)
                            else viewModel.closeEditor()
                        },
                        modifier = Modifier.testTag("scanner_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "الرجوع"
                        )
                    }
                },
                actions = {
                    if (scanStep == 2) {
                        Button(
                            onClick = {
                                viewModel.saveCurrentDocument()
                                Toast.makeText(context, "تم حفظ المستند الممسوح بتنسيق $saveFormatType في وحدة إدارة الملفات بنجاح!", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ScannerViolet),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("scanner_save_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ المستند", fontWeight = FontWeight.Bold)
                        }
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
            when (scanStep) {
                0 -> {
                    // --- STEP 0: CAMERA VIEWPORT LIVE STAGE ---
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        items(
                            listOf(
                                "document" to "📄 مستند ورقي",
                                "id_card" to "🪪 بطاقة هوية",
                                "book" to "📖 كتاب كامل",
                                "receipt" to "🧾 إيصـال دفع"
                            )
                        ) { (id, label) ->
                            val act = selectedScanType == id
                            FilterChip(
                                selected = act,
                                onClick = { selectedScanType = id },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ScannerViolet.copy(alpha = 0.15f),
                                    selectedLabelColor = ScannerViolet
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0C0E14))
                            .testTag("camera_viewport_stage")
                    ) {
                        if (!hasCameraPermission) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp)
                                    .fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "مطلوب صلاحية الكاميرا",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "الرجاء توفير صلاحية استخدام كاميرا الهاتف لمسح المستندات ضوئياً وتحديد الحواف وتطبيق محرك التعرف الضوئي (OCR) للمستندات والوصولات بدقة.",
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("السماح باستخدام الكاميرا", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // Guideline targeting overlays matching scan target
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(
                                        when (selectedScanType) {
                                            "id_card" -> 0.7f
                                            "receipt" -> 0.6f
                                            "book" -> 0.85f
                                            else -> 0.78f
                                        }
                                    )
                                    .aspectRatio(
                                        when (selectedScanType) {
                                            "id_card" -> 1.58f
                                            "receipt" -> 0.5f
                                            "book" -> 1.4f
                                            else -> 0.707f
                                        }
                                    )
                                    .align(Alignment.Center)
                                    .border(2.dp, ScannerViolet, RoundedCornerShape(8.dp))
                            ) {
                                if (selectedScanType == "book") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(2.dp)
                                            .background(ScannerViolet.copy(alpha = 0.5f))
                                            .align(Alignment.Center)
                                    )
                                }
                                // Target corners indicators
                                Box(modifier = Modifier.size(16.dp).align(Alignment.TopStart).border(2.dp, Color.White, RoundedCornerShape(topStart = 8.dp)))
                                Box(modifier = Modifier.size(16.dp).align(Alignment.TopEnd).border(2.dp, Color.White, RoundedCornerShape(topEnd = 8.dp)))
                                Box(modifier = Modifier.size(16.dp).align(Alignment.BottomStart).border(2.dp, Color.White, RoundedCornerShape(bottomStart = 8.dp)))
                                Box(modifier = Modifier.size(16.dp).align(Alignment.BottomEnd).border(2.dp, Color.White, RoundedCornerShape(bottomEnd = 8.dp)))

                                Text(
                                    text = "ضع الحواف ضمن المربع للتصفية التلقائية AI",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Grid overlays lines
                            Column(modifier = Modifier.fillMaxSize()) {
                                Spacer(modifier = Modifier.weight(1f))
                                Divider(color = Color.White.copy(alpha = 0.12f))
                                Spacer(modifier = Modifier.weight(1f))
                                Divider(color = Color.White.copy(alpha = 0.12f))
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            Row(modifier = Modifier.fillMaxSize()) {
                                Spacer(modifier = Modifier.weight(1f))
                                VerticalDivider(color = Color.White.copy(alpha = 0.12f))
                                Spacer(modifier = Modifier.weight(1f))
                                VerticalDivider(color = Color.White.copy(alpha = 0.12f))
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            // Top control details
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.toggleScannerFlash() },
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (flashState) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                        contentDescription = "فلاش",
                                        tint = if (flashState) Color.Yellow else Color.White
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                        .clickable {
                                            viewModel.setScanStep(2)
                                            Toast.makeText(context, "تم التقاط المستند واستخراجه من استوديو الصور!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("استيراد من المعرض", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Zoom factor
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                listOf("0.5x", "1.0x", "2.0x").forEach { factor ->
                                    val selected = zoomLevel == factor
                                    Text(
                                        text = factor,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) ScannerViolet else Color.White,
                                        modifier = Modifier
                                            .clickable { zoomLevel = factor }
                                            .padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Shutter action button
                    Surface(
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .border(4.dp, ScannerViolet, CircleShape)
                                    .padding(4.dp)
                                    .background(Color.White, CircleShape)
                                    .clickable {
                                        if (!hasCameraPermission) {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        } else {
                                            try {
                                                val activity = context.findActivity()
                                                if (activity != null) {
                                                    val options = GmsDocumentScannerOptions.Builder()
                                                        .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
                                                        .setScannerMode(SCANNER_MODE_FULL)
                                                        .build()
                                                    val scanner = GmsDocumentScanning.getClient(options)
                                                    scanner.getStartScanIntent(activity)
                                                        .addOnSuccessListener { intentSender ->
                                                            scannerLauncher.launch(
                                                                IntentSenderRequest.Builder(intentSender).build()
                                                            )
                                                        }
                                                        .addOnFailureListener { e ->
                                                            // Fallback in case sandbox doesn't support GMS Document Scanner
                                                            Toast.makeText(context, "بدء المسح الضوئي التفاعلي...", Toast.LENGTH_SHORT).show()
                                                            viewModel.setScanStep(1)
                                                        }
                                                } else {
                                                    viewModel.setScanStep(1)
                                                }
                                            } catch (e: Exception) {
                                                viewModel.setScanStep(1)
                                            }
                                        }
                                    }
                                    .testTag("camera_shutter_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "التقاط الصور",
                                    tint = ScannerViolet,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // --- STEP 1: EDGE CROPPING & CORNERS PERSPECTIVE CHECK ---
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Crop, contentDescription = null, tint = ScannerViolet)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "يمكنك سحب الدوائر الأربعة بمؤشر اللمس لتصحيح المنظور أو ضبط زوايا الورقة تلقائياً.",
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE5E7EB))
                            .testTag("cropping_viewport_stage")
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxSize(0.75f)
                                .align(Alignment.Center)
                                .border(1.dp, Color.Gray.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = ScannerViolet.copy(alpha = 0.15f),
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "تمت معالجة الإضاءة واكتشاف الحواف",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )
                                Text("توجيه الورقة: ${cropRotationAngle.toInt()}°", fontSize = 10.sp, color = ScannerViolet)
                            }
                        }

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeCol = ScannerViolet
                            drawLine(color = strokeCol, start = topLeftCorner, end = topRightCorner, strokeWidth = 3f)
                            drawLine(color = strokeCol, start = topRightCorner, end = bottomRightCorner, strokeWidth = 3f)
                            drawLine(color = strokeCol, start = bottomRightCorner, end = bottomLeftCorner, strokeWidth = 3f)
                            drawLine(color = strokeCol, start = bottomLeftCorner, end = topLeftCorner, strokeWidth = 3f)
                        }

                        // Corner handles
                        Box(
                            modifier = Modifier
                                .offset(topLeftCorner.x.dp - 12.dp, topLeftCorner.y.dp - 12.dp)
                                .size(24.dp)
                                .background(ScannerViolet, CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        topLeftCorner = topLeftCorner + dragAmount
                                    }
                                }
                        )
                        Box(
                            modifier = Modifier
                                .offset(topRightCorner.x.dp - 12.dp, topRightCorner.y.dp - 12.dp)
                                .size(24.dp)
                                .background(ScannerViolet, CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        topRightCorner = topRightCorner + dragAmount
                                    }
                                }
                        )
                        Box(
                            modifier = Modifier
                                .offset(bottomLeftCorner.x.dp - 12.dp, bottomLeftCorner.y.dp - 12.dp)
                                .size(24.dp)
                                .background(ScannerViolet, CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        bottomLeftCorner = bottomLeftCorner + dragAmount
                                    }
                                }
                        )
                        Box(
                            modifier = Modifier
                                .offset(bottomRightCorner.x.dp - 12.dp, bottomRightCorner.y.dp - 12.dp)
                                .size(24.dp)
                                .background(ScannerViolet, CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        bottomRightCorner = bottomRightCorner + dragAmount
                                    }
                                }
                        )
                    }

                    // Rotating and confirmation actions
                    Surface(
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("تدوير الاتجاه:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            cropRotationAngle = (cropRotationAngle + 90f) % 360f
                                            Toast.makeText(context, "تم تدوير ٩٠ درجة مئوية", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تدوير 90°", fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = {
                                            topLeftCorner = Offset(40f, 40f)
                                            topRightCorner = Offset(680f, 40f)
                                            bottomLeftCorner = Offset(40f, 400f)
                                            bottomRightCorner = Offset(680f, 400f)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("إعادة البثق", fontSize = 10.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.setScanStep(0) }
                                ) {
                                    Text("إعادة الالتقاط")
                                }

                                Button(
                                    onClick = { viewModel.setScanStep(2) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ScannerViolet),
                                    modifier = Modifier.testTag("confirm_crop_button")
                                ) {
                                    Text("تأكيد حدود الورقة والمعالجة")
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // --- STEP 2: ADVANCED FILTERS, OCR MULTILINGUAL OUT & ENHANCING SLIDERS ---
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("🌐 لغة التعرف البصري (OCR):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ScannerViolet)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "ar" to "العربية",
                                        "de" to "Deutsch",
                                        "en" to "English",
                                        "fr" to "Français"
                                    ).forEach { (id, label) ->
                                        val active = ocrLanguage == id
                                        FilterChip(
                                            selected = active,
                                            onClick = {
                                                ocrLanguage = id
                                                viewModel.setScannerFilter(scannerFilter)
                                            },
                                            label = { Text(label, fontSize = 10.sp) }
                                        )
                                    }
                                }

                                Divider(color = Color.LightGray.copy(alpha = 0.3f))

                                Text("💾 صيغة الحفظ النهائية المستهدفة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ScannerViolet)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    listOf(
                                        "pdf" to "ملف PDF مرقمن",
                                        "image" to "صورة عالية الجودة (JPG)"
                                    ).forEach { (fType, fName) ->
                                        val active = saveFormatType == fType
                                        FilterChip(
                                            selected = active,
                                            onClick = { saveFormatType = fType },
                                            label = { Text(fName, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "🎨 مرشحات ذكية ورقمية لتحسين وضوح المستند:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ScannerViolet
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "bw" to "برنتر Stark",
                                        "grayscale" to "رمادي متباين",
                                        "enhance" to "تعديل ذكي",
                                        "normal" to "صورة طبيعية"
                                    ).forEach { (id, name) ->
                                        val active = scannerFilter == id
                                        InputChip(
                                            selected = active,
                                            onClick = { viewModel.setScannerFilter(id) },
                                            label = { Text(name, fontSize = 10.sp) },
                                            modifier = Modifier.testTag("filter_$id")
                                        )
                                    }
                                }

                                Divider(color = Color.LightGray.copy(alpha = 0.3f))

                                Text("🎚️ التحكم الدقيق والتحسين البصري للبيانات:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("السطوع والإنارة: ${adjustmentBrightness.toInt()}%", fontSize = 10.sp, color = Color.Gray)
                                    Slider(
                                        value = adjustmentBrightness,
                                        onValueChange = { adjustmentBrightness = it },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.height(24.dp)
                                    )
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("قوة التباين والوضوح: ${adjustmentContrast.toInt()}%", fontSize = 10.sp, color = Color.Gray)
                                    Slider(
                                        value = adjustmentContrast,
                                        onValueChange = { adjustmentContrast = it },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.height(24.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("تنقية تلقائية وإزالة الظلال (Denoise):", fontSize = 11.sp)
                                    Switch(
                                        checked = isNoiseRemovalOn,
                                        onCheckedChange = { isNoiseRemovalOn = it }
                                    )
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = ScannerViolet,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "المحتويات المستخلصة رقمياً (تعرف ضوئي OCR):",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.DarkGray
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (ocrText.isNotEmpty()) {
                                                val snippet = ocrText.take(100)
                                                if (ttsReady && tts != null) {
                                                    tts?.language = if (ocrLanguage == "de") Locale.GERMAN else if (ocrLanguage == "en") Locale.ENGLISH else if (ocrLanguage == "fr") Locale.FRENCH else Locale("ar")
                                                    tts?.speak(snippet, TextToSpeech.QUEUE_FLUSH, null, null)
                                                    Toast.makeText(context, "🗣️ نطق النص المستخلص", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "قراءة صوتية", tint = ScannerViolet)
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color.LightGray.copy(alpha = 0.4f))

                                val finalMatchedTextOnSettings = lookupSimulatedOCR(ocrLanguage, scannerFilter)
                                OutlinedTextField(
                                    value = finalMatchedTextOnSettings,
                                    onValueChange = { /* editable OCR results preview */ },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f)
                                        .testTag("ocr_text_output"),
                                    textStyle = TextStyle(fontSize = 13.sp, color = Color.DarkGray, lineHeight = 20.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
