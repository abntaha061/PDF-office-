package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PresentationOrange
import com.example.ui.viewmodel.OfficeViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

// Data classes representing Concept Map node structure
data class MindNode(
    val id: Int,
    var name: String,
    var x: Float,
    var y: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationScreen(
    viewModel: OfficeViewModel,
    modifier: Modifier = Modifier
) {
    val title by viewModel.presentationTitle.collectAsState()
    val slides by viewModel.presentationSlides.collectAsState()
    val selectedIndex by viewModel.presentationSelectedSlideIndex.collectAsState()
    val isPresentMode by viewModel.isPresentMode.collectAsState()

    var showTitleEditDialog by remember { mutableStateOf(false) }
    var tempTitle by remember { mutableStateOf("") }

    val activeSlide = slides.getOrNull(selectedIndex) ?: ("الشريحة" to "")

    var slideTitleInput by remember { mutableStateOf("") }
    var slideContentInput by remember { mutableStateOf("") }

    // Sync input states when active slide changes
    LaunchedEffect(selectedIndex, slides) {
        if (selectedIndex in slides.indices) {
            slideTitleInput = slides[selectedIndex].first
            slideContentInput = slides[selectedIndex].second
        }
    }

    // Capture active input changes back to VM list
    val updateSlides = {
        viewModel.updateSelectedSlide(slideTitleInput, slideContentInput)
    }

    // --- PHASE 2 DETAILED STATES ---
    var selectedExtension by remember { mutableStateOf(".pptx") }
    var activeTheme by remember { mutableStateOf("classic") } // "classic", "navy", "lavender", "charcoal", "royal"
    var activeTransition by remember { mutableStateOf("slide") } // "fade", "slide", "zoom"
    
    // Animations for active slide elements
    var elementEntranceAnimType by remember { mutableStateOf("fade") } // "fade", "bounce", "slide"
    var isAnimationPreviewing by remember { mutableStateOf(false) }
    var animationScale by remember { mutableStateOf(1f) }

    // Floating Items inserted in slides
    var insertedShapesList by remember { mutableStateOf(listOf<String>()) }
    var slideLayoutType by remember { mutableStateOf("two_columns") } // "title_only", "two_columns", "master_header"

    // Presenter Tools & Rehearsals Timers
    var isRecordingPresent by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableStateOf(0) }
    var voiceDecibelsHz by remember { mutableStateOf(listOf(10, 20, 45, 10, 95, 30, 60)) }
    var isLaserActive by remember { mutableStateOf(false) }
    var laserPointOffset by remember { mutableStateOf(Pair(200f, 200f)) }
    
    var showPresenterNotesPanel by remember { mutableStateOf(false) }
    var speakerSpeakerNotes by remember { mutableStateOf("تأكيد مبيعات الربع الثالث وتوضيح نسب النمو العكسي بالتكنولوجيا السحابية...") }
    var rehearseTimingsMap by remember { mutableStateOf(mapOf(0 to 45, 1 to 120)) } // slideIndex to seconds

    // Scribing tools: pen draw line offset targets on full slide
    val penScribbleLines = remember { mutableStateListOf<Pair<Float, Float>>() }
    var isPenActive by remember { mutableStateOf(false) }

    // Dialog for Concept/Mind Map Designer
    var showConceptMapWizard by remember { mutableStateOf(false) }
    val mindNodes = remember {
        mutableStateListOf(
            MindNode(1, "أوفيس سحابي", 300f, 100f),
            MindNode(2, "مستندات كاتب", 150f, 250f),
            MindNode(3, "توقيعات رقمية", 450f, 250f)
        )
    }

    // Recording duration loop
    LaunchedEffect(isRecordingPresent) {
        if (isRecordingPresent) {
            while (isRecordingPresent) {
                delay(1000)
                recordingDurationSec++
                val newHz = List(7) { Random.nextInt(5, 100) }
                voiceDecibelsHz = newHz
            }
        } else {
            recordingDurationSec = 0
        }
    }

    // Animation previewing trigger
    LaunchedEffect(isAnimationPreviewing) {
        if (isAnimationPreviewing) {
            animationScale = 0.5f
            delay(100)
            animationScale = 1.1f
            delay(150)
            animationScale = 1.0f
            isAnimationPreviewing = false
        }
    }

    // COLOR SCHEMING DEFINITIONS FOR THEMES
    val currentBgColor = when (activeTheme) {
        "navy" -> Color(0xFF1B263B)
        "lavender" -> Color(0xFFF2EAF9)
        "charcoal" -> Color(0xFF262626)
        "royal" -> Color(0xFFFAF6EE)
        else -> Color.White
    }

    val currentTextColor = when (activeTheme) {
        "navy" -> Color(0xFFFAF0CA)
        "charcoal" -> Color(0xFFE5C158)
        "royal" -> Color(0xFF8C6D31)
        else -> PresentationOrange
    }

    val currentBodyColor = when (activeTheme) {
        "navy" -> Color.White
        "charcoal" -> Color.LightGray
        "royal" -> Color(0xFF2C2518)
        else -> Color.DarkGray
    }

    val currentBorderStroke = when (activeTheme) {
        "charcoal" -> BorderStroke(2.dp, Color(0xFFE5C158))
        "royal" -> BorderStroke(1.5.dp, Color(0xFFD4AF37))
        else -> BorderStroke(1.dp, PresentationOrange.copy(alpha = 0.4f))
    }

    if (isPresentMode) {
        // --- MULTIPANEL/PRESENTER FULL THEATER MODE ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F111A))
                .testTag("full_screen_presentation")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Theater top ribbon header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Badge(containerColor = PresentationOrange, modifier = Modifier.padding(start = 8.dp)) {
                                Text("وضع العرض الاحترافي", color = Color.White, fontSize = 8.sp)
                            }
                        }
                        Text("الشريحة ${selectedIndex + 1} من ${slides.size} • سمة [${activeTheme.uppercase()}]", color = Color.Gray, fontSize = 11.sp)
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Laser Pointer Toggle
                        IconButton(
                            onClick = { 
                                isLaserActive = !isLaserActive 
                                isPenActive = false
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = if (isLaserActive) Color.Red else Color.Transparent)
                        ) {
                            Icon(Icons.Default.Adjust, contentDescription = "مؤشر ليزر تفاعلي", tint = Color.White)
                        }

                        // Pen Tool Toggle
                        IconButton(
                            onClick = { 
                                isPenActive = !isPenActive
                                isLaserActive = false
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = if (isPenActive) PresentationOrange else Color.Transparent)
                        ) {
                            Icon(Icons.Default.Gesture, contentDescription = "قلم رسم توضيحي", tint = Color.White)
                        }

                        // Presenter side notes button toggles
                        FilterChip(
                            selected = showPresenterNotesPanel,
                            onClick = { showPresenterNotesPanel = !showPresenterNotesPanel },
                            label = { Text("ملحوظات المتحدث", color = Color.White, fontSize = 11.sp) }
                        )

                        // Recorder Simulation buttons
                        IconButton(
                            onClick = { isRecordingPresent = !isRecordingPresent },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = if (isRecordingPresent) Color.Red else Color.DarkGray)
                        ) {
                            Icon(
                                imageVector = if (isRecordingPresent) Icons.Default.RadioButtonChecked else Icons.Default.Mic,
                                contentDescription = "تسجيل الشاشة وتدريب الصوت",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { 
                                penScribbleLines.clear()
                                viewModel.togglePresentMode(false) 
                            },
                            modifier = Modifier.testTag("exit_presentation")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }
                    }
                }

                // SCREEN RECORDING ACTIVE BAR PREVIEW
                AnimatedVisibility(visible = isRecordingPresent) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color.Red, RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("جاري تسجيل وتدقيق مستوى الإلقاء الصوتي للخطيب: ${recordingDurationSec}ث", color = Color.White, fontSize = 11.sp)
                            }
                            
                            // Beautiful Voice Decibel Waves
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.height(20.dp)
                            ) {
                                voiceDecibelsHz.forEach { hz ->
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .fillMaxHeight(hz / 100f)
                                            .background(Color.Green)
                                    )
                                }
                            }
                        }
                    }
                }

                // THE TWO PANEL SPLITPRESENT SPLITVIEWER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // LEFT COLUMN: LARGE GRAPHIC SLIDE DISPLAY (WITH GESTURES INPUT FOR PEN OR LASER)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = currentBgColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                            .border(currentBorderStroke.width, currentBorderStroke.brush, RoundedCornerShape(16.dp))
                            .pointerInput(isLaserActive, isPenActive) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (isLaserActive) {
                                        laserPointOffset = Pair(change.position.x, change.position.y)
                                    } else if (isPenActive) {
                                        penScribbleLines.add(change.position.x to change.position.y)
                                    }
                                }
                            }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Elements animations transition wrapper
                            AnimatedContent(
                                targetState = selectedIndex,
                                transitionSpec = {
                                    when (activeTransition) {
                                        "fade" -> fadeIn() togetherWith fadeOut()
                                        "zoom" -> scaleIn() togetherWith scaleOut()
                                        else -> slideInHorizontally { w -> w } + fadeIn() togetherWith slideOutHorizontally { w -> -w } + fadeOut()
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                label = "playSlideAni"
                            ) { targetIdx ->
                                val activeS = slides.getOrNull(targetIdx) ?: ("" to "")
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = activeS.first,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = currentTextColor,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = activeS.second,
                                        fontSize = 15.sp,
                                        color = currentBodyColor,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 24.sp,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )

                                    // Render elements shapes inline if inserted
                                    if (insertedShapesList.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            insertedShapesList.forEach { shape ->
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = currentTextColor.copy(alpha = 0.2f)),
                                                    border = BorderStroke(1.dp, currentTextColor)
                                                ) {
                                                    Text(shape, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = currentTextColor, modifier = Modifier.padding(6.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // LAYER: SCRIBBLE PEN PATH DRAWING CANVAS
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                if (penScribbleLines.size > 1) {
                                    for (i in 0 until penScribbleLines.size - 1) {
                                        drawLine(
                                            color = Color.Red,
                                            start = androidx.compose.ui.geometry.Offset(penScribbleLines[i].first, penScribbleLines[i].second),
                                            end = androidx.compose.ui.geometry.Offset(penScribbleLines[i+1].first, penScribbleLines[i+1].second),
                                            strokeWidth = 6f
                                        )
                                    }
                                }
                            }

                            // LAYER: LASER POINTER CIRCLE LIGHT
                            if (isLaserActive) {
                                Box(
                                    modifier = Modifier
                                        .offset(dpX = laserPointOffset.first.dp / 2f, dpY = laserPointOffset.second.dp / 2f)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red.copy(alpha = 0.7f))
                                        .border(2.dp, Color.White, CircleShape)
                                )
                            }
                        }
                    }

                    // RIGHT COLUMN: PRESENTER HELPER NOTES & REHEARSALS TIMINGS DRAWER
                    AnimatedVisibility(visible = showPresenterNotesPanel, modifier = Modifier.weight(1f)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131520)),
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxHeight(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("ملحوظات الملقي والمدرب الخاص", fontWeight = FontWeight.Bold, color = PresentationOrange, fontSize = 12.sp)
                                OutlinedTextField(
                                    value = speakerSpeakerNotes,
                                    onValueChange = { speakerSpeakerNotes = it },
                                    textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(100.dp)
                                )
                                
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                                
                                Text("التوقيت والتسجيل التقديري للشرائح:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                                slides.forEachIndexed { i, s ->
                                    val sec = rehearseTimingsMap[i] ?: 15
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${i+1}. ${s.first}", color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text("${sec} ثانية", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Play Navigation Control bar at the bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (selectedIndex > 0) viewModel.selectSlide(selectedIndex - 1)
                        },
                        enabled = selectedIndex > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "السابق",
                            tint = if (selectedIndex > 0) Color.White else Color.DarkGray,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(40.dp))

                    IconButton(
                        onClick = {
                            if (selectedIndex < slides.size - 1) viewModel.selectSlide(selectedIndex + 1)
                        },
                        enabled = selectedIndex < slides.size - 1
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "التالي",
                            tint = if (selectedIndex < slides.size - 1) Color.White else Color.DarkGray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    } else {
        // --- STANDARD DESIGN ENVIRONMENT WORKSPACE ---
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
                                imageVector = Icons.Default.Slideshow,
                                contentDescription = null,
                                tint = PresentationOrange,
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
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = "حقائب عروض تقديمية ذكية متصلة بـ MS PowerPoint",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.closeEditor() },
                            modifier = Modifier.testTag("presentation_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "الرجوع"
                            )
                        }
                    },
                    actions = {
                        // Interactive Concept Maps Mind Map Wizard
                        IconButton(onClick = { showConceptMapWizard = true }) {
                            Icon(
                                imageVector = Icons.Default.BubbleChart,
                                contentDescription = "مخطط خريطة المفاهيم",
                                tint = PresentationOrange
                            )
                        }

                        // Play Button
                        IconButton(
                            onClick = { viewModel.togglePresentMode(true) },
                            modifier = Modifier.testTag("play_presentation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "تشغيل العرض",
                                tint = PresentationOrange
                            )
                        }

                        // Save Button
                        Button(
                            onClick = { viewModel.saveCurrentDocument() },
                            colors = ButtonDefaults.buttonColors(containerColor = PresentationOrange),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("presentation_save_button")
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
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // TOP ENV CONFIG: THEMES, EXPORTS, TRANSITIONS
                ElevatedCard(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(0.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Theme Selector Horizontal strip
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                Text("سمات الماستر سلايد:", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(end = 4.dp))
                            }
                            item {
                                val themes = listOf("classic" to "الأبيض", "navy" to "النيلي الداكن", "lavender" to "اللافندري", "charcoal" to "الفحمي المذهب", "royal" to "الملكي")
                                themes.forEach { (tKey, tLabel) ->
                                    val isSelected = activeTheme == tKey
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) PresentationOrange else Color.LightGray.copy(alpha = 0.2f)
                                            )
                                            .clickable { activeTheme = tKey }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(tLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color.Black)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // PowerPoint Compatibility Formats
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                Text("تصدير متوافق PowerPoint:", fontSize = 10.sp, color = Color.Gray)
                            }
                            item {
                                listOf(".pptx", ".ppt", ".pps", ".potm").forEach { ext ->
                                    val isExtSelected = selectedExtension == ext
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isExtSelected) PresentationOrange else Color.LightGray.copy(alpha = 0.2f))
                                            .clickable { selectedExtension = ext }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(ext, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isExtSelected) Color.White else Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }

                // HORIZONTAL THUMBNAIL CAROUSEL OF SLIDES
                Surface(
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تسلسل شرائح حقيبة العمل (${slides.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PresentationOrange
                            )

                            // Add Slide Button
                            TextButton(
                                onClick = { viewModel.addNewSlide() },
                                modifier = Modifier.testTag("add_slide_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إدخال شريحة جديدة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                        ) {
                            itemsIndexed(slides) { idx, s ->
                                val isActive = idx == selectedIndex
                                Card(
                                    elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 1.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isActive) currentBgColor else Color.White
                                    ),
                                    border = BorderStroke(
                                        width = if (isActive) 1.5.dp else 1.dp,
                                        color = if (isActive) currentTextColor else Color.LightGray.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier
                                        .width(115.dp)
                                        .fillMaxHeight()
                                        .clickable { viewModel.selectSlide(idx) }
                                        .testTag("slide_thumb_$idx")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}. ${s.first}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            color = if (isActive) currentTextColor else Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // MAIN SLIDE TEMPLATE LAYOUT PREVIEW WITH STYLING PAIRING
                Card(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .border(currentBorderStroke.width, currentBorderStroke.brush, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = currentBgColor),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = if (slideTitleInput.isEmpty()) "عنوان الشريحة الرئيسية" else slideTitleInput,
                                fontSize = (24 * animationScale).sp,
                                fontWeight = FontWeight.Bold,
                                color = currentTextColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (slideContentInput.isEmpty()) "تحرير النقاط التفصيلية والمواضيع المحددة للعرض..." else slideContentInput,
                                fontSize = 14.sp,
                                color = currentBodyColor,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        }

                        // Theme indicator badge
                        Badge(
                            containerColor = currentTextColor,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text("Theme: ${activeTheme.uppercase()}", color = if (activeTheme == "classic") Color.White else Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                        }
                    }
                }

                // WORKSPACE ACTIVE SLIDE CONTROLLER DETAILS & ANIMATION TIMINGS
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("حركات عناصر الشريحة والتنقل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PresentationOrange)
                            
                            // Anim type trigger choices
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { isAnimationPreviewing = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PresentationOrange),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("معاينة الحركة", fontSize = 10.sp)
                                }

                                if (slides.size > 1) {
                                    TextButton(
                                        onClick = { viewModel.deleteSelectedSlide() },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("حذف الشريحة", fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        // Slide inputs
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = slideTitleInput,
                                onValueChange = {
                                    slideTitleInput = it
                                    updateSlides()
                                },
                                label = { Text("عنوان الشريحة", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 11.sp)
                            )

                            OutlinedTextField(
                                value = slideContentInput,
                                onValueChange = {
                                    slideContentInput = it
                                    updateSlides()
                                },
                                label = { Text("تفاصيل الشريحة", fontSize = 10.sp) },
                                modifier = Modifier.weight(1.5f).height(48.dp),
                                textStyle = TextStyle(fontSize = 11.sp)
                            )
                        }

                        // Grid of transition speed and element animation settings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Transitions Choice
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("انتقال الشريحة:", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(end = 4.dp))
                                listOf("fade" to "بهتان", "slide" to "تزحلق", "zoom" to "تكبير").forEach { (k, l) ->
                                    val isSel = activeTransition == k
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSel) PresentationOrange else Color.LightGray.copy(alpha = 0.15f))
                                            .clickable { activeTransition = k }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(l, fontSize = 9.sp, color = if (isSel) Color.White else Color.Black)
                                    }
                                }
                            }

                            // Object animations choice
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("حركة النص:", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(end = 4.dp))
                                listOf("bounce" to "ارتداد", "fade" to "دخول ناعم").forEach { (k, l) ->
                                    val isSel = elementEntranceAnimType == k
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSel) PresentationOrange else Color.LightGray.copy(alpha = 0.15f))
                                            .clickable { elementEntranceAnimType = k }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(l, fontSize = 9.sp, color = if (isSel) Color.White else Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- HIGHLY INTERACTIVE MIND MAP / CONCEPT MAP DESIGNER OVERLAY ---
    if (showConceptMapWizard) {
        AlertDialog(
            onDismissRequest = { showConceptMapWizard = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BubbleChart, contentDescription = null, tint = PresentationOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مصمم خرائط المفاهيم التفاعلي", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    Text("اسحب الدوائر بإصبعك لتحديد موقع الأفكار، أو انقر لإضافة عقدة جديدة لربط المفاهيم:", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFFFAFAF7), RoundedCornerShape(12.dp))
                            .border(1.dp, PresentationOrange, RoundedCornerShape(12.dp))
                    ) {
                        // Drawing arrows connecting nodes
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            if (mindNodes.size >= 2) {
                                // Draw lines between first node and secondary nodes
                                val origin = mindNodes.first()
                                for (i in 1 until mindNodes.size) {
                                    val target = mindNodes[i]
                                    drawLine(
                                        color = Color(0xFFCA6F1E),
                                        start = androidx.compose.ui.geometry.Offset(origin.x, origin.y),
                                        end = androidx.compose.ui.geometry.Offset(target.x, target.y),
                                        strokeWidth = 4f
                                    )
                                }
                            }
                        }

                        // Drawing interactive draggable nodes
                        mindNodes.forEachIndexed { idx, node ->
                            Box(
                                modifier = Modifier
                                    .offset(dpX = node.x.dp / 2f, dpY = node.y.dp / 2f)
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(if (idx == 0) PresentationOrange else Color(0xFFFAF0CA))
                                    .border(1.5.dp, PresentationOrange, CircleShape)
                                    .pointerInput(node) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            node.x += dragAmount.x
                                            node.y += dragAmount.y
                                            // Hack to trigger list compose state update
                                            val listCopy = mindNodes.toList()
                                            mindNodes.clear()
                                            mindNodes.addAll(listCopy)
                                        }
                                    }
                                    .clickable {
                                        node.name = if (node.name.length > 5) "مفهوم" else node.name + " +"
                                        val listCopy = mindNodes.toList()
                                        mindNodes.clear()
                                        mindNodes.addAll(listCopy)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = node.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = if (idx == 0) Color.White else Color.Black,
                                    maxLines = 2,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val nextId = mindNodes.size + 1
                                mindNodes.add(MindNode(nextId, "فكرة $nextId", 200f + Random.nextInt(-40, 40), 200f + Random.nextInt(-40, 40)))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PresentationOrange)
                        ) {
                            Text("إضافة عقدة فكرية فرعية", fontSize = 10.sp)
                        }
                        TextButton(onClick = { mindNodes.clear() }) {
                            Text("مسح اللوحة", color = Color.Red, fontSize = 10.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showConceptMapWizard = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PresentationOrange)
                ) {
                    Text("حفظ وتثبيت على الشريحة")
                }
            }
        )
    }

    // --- TITLE RE-EDIT DIALOG ---
    if (showTitleEditDialog) {
        AlertDialog(
            onDismissRequest = { showTitleEditDialog = false },
            title = { Text("تعديل اسم العرض التقديمي", fontWeight = FontWeight.Bold) },
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
                            viewModel.updatePresentationTitle(tempTitle)
                        }
                        showTitleEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PresentationOrange)
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

// Inline helper for offsets
private fun Modifier.offset(dpX: androidx.compose.ui.unit.Dp, dpY: androidx.compose.ui.unit.Dp) = this.then(
    Modifier.offset(x = dpX, y = dpY)
)
