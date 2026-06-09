package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SpreadsheetGreen
import com.example.ui.viewmodel.OfficeViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpreadsheetScreen(
    viewModel: OfficeViewModel,
    modifier: Modifier = Modifier
) {
    val title by viewModel.sheetTitle.collectAsState()
    val cells by viewModel.sheetCells.collectAsState()
    val selectedCell by viewModel.sheetSelectedCell.collectAsState()
    val cellInput by viewModel.sheetCellInput.collectAsState()
    val isBold by viewModel.sheetIsBold.collectAsState()
    val isItalic by viewModel.sheetIsItalic.collectAsState()

    var showTitleEditDialog by remember { mutableStateOf(false) }
    var tempTitle by remember { mutableStateOf("") }

    // DYNAMIC SHEET PROPERTIES
    var selectedExtension by remember { mutableStateOf(".xlsx") }
    var isRowFrozen by remember { mutableStateOf(false) }
    var isColFrozen by remember { mutableStateOf(false) }
    var highlightDuplicatesEnabled by remember { mutableStateOf(false) }
    var conditionalFormattingRule by remember { mutableStateOf("none") } // "none", "greater_than_50", "less_than_10"
    
    // Data Validation: map of cell address to validation rule ("numbers", "email", "none")
    var dataValidationRules by remember { mutableStateOf(mapOf("C2" to "numbers", "D3" to "email")) }
    
    // Dialog control states
    var showChartWizard by remember { mutableStateOf(false) }
    var selectedChartType by remember { mutableStateOf("bar") } // "bar", "pie", "line", "scatter", "area"
    
    var showFormatDialog by remember { mutableStateOf(false) }
    var showPivotWizard by remember { mutableStateOf(false) }
    var showInvoiceWizard by remember { mutableStateOf(false) }
    var showSortingFilterDialog by remember { mutableStateOf(false) }

    // Merged Cells Representation State: list of cell pairs ("A2", "B2") that are merged
    var mergedCells by remember { mutableStateOf(listOf("A2" to "B2")) }

    // Grid coordinates
    val columns = listOf("A", "B", "C", "D", "E", "F", "G")
    val rows = (1..15).toList()

    // HELPER MATHEMATICAL AND FORMULA PARSING ENGINE EXPOSED IN UI
    fun evaluateFormulaExpression(rawFormula: String, activeCells: Map<String, String>): String {
        val expr = rawFormula.uppercase().trim()
        
        // Match SUM across columns
        if (expr.startsWith("SUM")) {
            var total = 0.0
            activeCells.forEach { (address, value) ->
                if (!value.startsWith("=")) {
                    value.toDoubleOrNull()?.let { total += it }
                }
            }
            return if (total % 1.0 == 0.0) total.toInt().toString() else String.format("%.2f", total)
        }
        
        // Match AVG or AVERAGE
        if (expr.startsWith("AVG") || expr.startsWith("AVERAGE")) {
            var total = 0.0
            var count = 0
            activeCells.forEach { (address, value) ->
                if (!value.startsWith("=")) {
                    value.toDoubleOrNull()?.let {
                        total += it
                        count++
                    }
                }
            }
            return if (count > 0) String.format("%.2f", total / count) else "0"
        }

        // Match MAX
        if (expr.startsWith("MAX")) {
            var maxVal = Double.NEGATIVE_INFINITY
            activeCells.forEach { (_, value) ->
                if (!value.startsWith("=")) {
                    value.toDoubleOrNull()?.let { if (it > maxVal) maxVal = it }
                }
            }
            return if (maxVal == Double.NEGATIVE_INFINITY) "0" else maxVal.toInt().toString()
        }

        // Match MIN
        if (expr.startsWith("MIN")) {
            var minVal = Double.POSITIVE_INFINITY
            activeCells.forEach { (_, value) ->
                if (!value.startsWith("=")) {
                    value.toDoubleOrNull()?.let { if (it < minVal) minVal = it }
                }
            }
            return if (minVal == Double.POSITIVE_INFINITY) "0" else minVal.toInt().toString()
        }

        // Match COUNT
        if (expr.startsWith("COUNT")) {
            var count = 0
            activeCells.forEach { (_, value) ->
                if (value.isNotEmpty() && !value.startsWith("=")) count++
            }
            return count.toString()
        }

        // Match IF Conditionals: =IF(C2>50, "ناجح", "راسب")
        if (expr.startsWith("IF")) {
            try {
                val bracketContent = expr.substringAfter("(").substringBeforeLast(")")
                val parts = bracketContent.split(",")
                val condition = parts[0].trim()
                val successVal = parts[1].trim().replace("\"", "")
                val failureVal = parts[2].trim().replace("\"", "")
                
                // Parse conditional operators (e.g. C2>50)
                if (condition.contains(">")) {
                    val cParts = condition.split(">")
                    val cellAddr = cParts[0].trim()
                    val targetNum = cParts[1].trim().toDoubleOrNull() ?: 0.0
                    val cellRawVal = activeCells[cellAddr]?.toDoubleOrNull() ?: 0.0
                    return if (cellRawVal > targetNum) successVal else failureVal
                } else if (condition.contains("<")) {
                    val cParts = condition.split("<")
                    val cellAddr = cParts[0].trim()
                    val targetNum = cParts[1].trim().toDoubleOrNull() ?: 0.0
                    val cellRawVal = activeCells[cellAddr]?.toDoubleOrNull() ?: 0.0
                    return if (cellRawVal < targetNum) successVal else failureVal
                }
            } catch (e: Exception) {
                return "خطأ في الصيغة"
            }
        }

        // Simple algebraic cell relationships like A1+B1, A2*C2
        val mathOperators = listOf("+", "-", "*", "/")
        for (op in mathOperators) {
            if (expr.contains(op)) {
                try {
                    val parts = expr.split(op)
                    val cell1 = parts[0].trim()
                    val cell2 = parts[1].trim()
                    
                    val val1 = (activeCells[cell1] ?: cell1).toDoubleOrNull() ?: 0.0
                    val val2 = (activeCells[cell2] ?: cell2).toDoubleOrNull() ?: 0.0
                    
                    val res = when (op) {
                        "+" -> val1 + val2
                        "-" -> val1 - val2
                        "*" -> val1 * val2
                        "/" -> if (val2 != 0.0) val1 / val2 else 0.0
                        else -> 0.0
                    }
                    return if (res % 1.0 == 0.0) res.toInt().toString() else String.format("%.2f", res)
                } catch (e: Exception) {
                    return "خطأ حسابي"
                }
            }
        }

        return "صيغة غير مدعومة"
    }

    // Dynamic list of duplicate values in selection for highlighting
    val findDuplicateCells = {
        val occurrences = mutableMapOf<String, Int>()
        cells.values.forEach { v ->
            if (v.isNotEmpty() && !v.startsWith("=")) {
                occurrences[v] = occurrences.getOrDefault(v, 0) + 1
            }
        }
        occurrences.filter { it.value > 1 }.keys
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
                            imageVector = Icons.Default.GridOn,
                            contentDescription = null,
                            tint = SpreadsheetGreen,
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
                                    tint = SpreadsheetGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = "محرر الخلايا الحسابية والرسومات الذكية",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeEditor() },
                        modifier = Modifier.testTag("spreadsheet_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "الرجوع"
                        )
                    }
                },
                actions = {
                    // Invoice Generator Template
                    IconButton(onClick = { showInvoiceWizard = true }) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "تطبيق قالب فاتورة ذكية",
                            tint = SpreadsheetGreen
                        )
                    }

                    // Pivot Table Analyzer
                    IconButton(onClick = { showPivotWizard = true }) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = "تطبيق الجداول المحورية",
                            tint = SpreadsheetGreen
                        )
                    }

                    Button(
                        onClick = { viewModel.saveCurrentDocument() },
                        colors = ButtonDefaults.buttonColors(containerColor = SpreadsheetGreen),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("spreadsheet_save_button")
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
            // SPREADSHEET TOOLBAR TABS / ACTION BARS
            ElevatedCard(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(0.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // First row: Selection identifier, Style toggles, Freeze buttons, Extensions
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Surface(
                                color = SpreadsheetGreen.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "الخلية: $selectedCell",
                                    fontWeight = FontWeight.Bold,
                                    color = SpreadsheetGreen,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        item {
                            VerticalDivider(modifier = Modifier.height(24.dp))
                        }
                        item {
                            // Formatting toggles
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconToggleButton(
                                    checked = isBold,
                                    onCheckedChange = { viewModel.toggleSheetBold() },
                                    modifier = Modifier.testTag("sheet_bold_toggle")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatBold,
                                        contentDescription = "عريض",
                                        tint = if (isBold) SpreadsheetGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconToggleButton(
                                    checked = isItalic,
                                    onCheckedChange = { viewModel.toggleSheetItalic() },
                                    modifier = Modifier.testTag("sheet_italic_toggle")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatItalic,
                                        contentDescription = "مائل",
                                        tint = if (isItalic) SpreadsheetGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        item {
                            VerticalDivider(modifier = Modifier.height(24.dp))
                        }
                        item {
                            // Freeze buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = isRowFrozen,
                                    onClick = { isRowFrozen = !isRowFrozen },
                                    label = { Text("تجميد الصف ١", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = isColFrozen,
                                    onClick = { isColFrozen = !isColFrozen },
                                    label = { Text("تجميد العمود A", fontSize = 10.sp) }
                                )
                            }
                        }
                        item {
                            VerticalDivider(modifier = Modifier.height(24.dp))
                        }
                        item {
                            // Format compatibility
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("صيغة Excel:", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(end = 4.dp))
                                listOf(".xlsx", ".xls", ".csv", ".xlt", ".xltx").forEach { ext ->
                                    val isSelected = selectedExtension == ext
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) SpreadsheetGreen else Color.LightGray.copy(alpha = 0.2f))
                                            .clickable { selectedExtension = ext }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(ext, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color.Black)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Second row: Operations & Wizard helpers (Charts, Formatting rules, Duplicate analyzer, Validation)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Plot Chart Wizard Button
                        InputChip(
                            selected = showChartWizard,
                            onClick = { showChartWizard = true },
                            label = { Text("رسم بياني تشكيلي", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )

                        // Conditional Formatting Rule Button
                        InputChip(
                            selected = conditionalFormattingRule != "none",
                            onClick = { showFormatDialog = true },
                            label = { Text("تحليل شرطي للتلوين", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )

                        // Highlight Duplicates
                        InputChip(
                            selected = highlightDuplicatesEnabled,
                            onClick = { highlightDuplicatesEnabled = !highlightDuplicatesEnabled },
                            label = { Text("مكررات", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.FilterAlt, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )

                        // Advanced Sort & Filter
                        InputChip(
                            selected = false,
                            onClick = { showSortingFilterDialog = true },
                            label = { Text("فرز وتصفية", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )

                        // Merge / Split Cell States
                        InputChip(
                            selected = mergedCells.isNotEmpty(),
                            onClick = {
                                if (mergedCells.isEmpty()) {
                                    mergedCells = listOf("A2" to "B2")
                                    // Merge values by setting the same text
                                    val valA = cells["A2"] ?: "دمج متميز"
                                    viewModel.updateSheetCellInput(valA)
                                } else {
                                    mergedCells = emptyList()
                                }
                            },
                            label = { Text(if (mergedCells.isNotEmpty()) "الدمج نشط" else "دمج خلايا افقي", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.Merge, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Formula Input Box Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "fx",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = SpreadsheetGreen,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        OutlinedTextField(
                            value = cellInput,
                            onValueChange = { viewModel.updateSheetCellInput(it) },
                            placeholder = { Text("اكتب رقماً، نصاً، أو دالة رياضية (=SUM ... =AVG ... =MAX ... =IF ...)", fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("sheet_formula_input"),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SpreadsheetGreen,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            }

            // FORMULA EXPLANATION HELP STRIP
            Card(
                colors = CardDefaults.cardColors(containerColor = SpreadsheetGreen.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = SpreadsheetGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "الدوال المدعومة حركياً: الجمع التراكمي =SUM، المتوسط =AVG، الشرط =IF(A1>5,\"نعم\",\"لا\")، والجبر البسيط مثل =A1+B1",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // GRID WORKSPACE VIEWPORT
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .background(Color.White, RoundedCornerShape(12.dp))
            ) {
                Column {
                    // Header Column block Row letters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        // Empty Corner block for alignment index
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .fillMaxHeight()
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.GridGoldenratio, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color.Gray)
                        }

                        // Col block letter indices
                        columns.forEach { col ->
                            val isFrozenCol = col == "A" && isColFrozen
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (isFrozenCol) SpreadsheetGreen.copy(alpha = 0.1f) else Color.Transparent)
                                    .border(
                                        width = if (isFrozenCol) 1.dp else 0.5.dp,
                                        color = if (isFrozenCol) SpreadsheetGreen else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = col,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isFrozenCol) SpreadsheetGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Rows generator with Scrolling mechanism
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(rows) { row ->
                            val isFrozenRow = row == 1 && isRowFrozen
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .background(if (isFrozenRow) SpreadsheetGreen.copy(alpha = 0.05f) else Color.Transparent)
                            ) {
                                // Row indices
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                        .border(
                                            width = if (isFrozenRow) 1.dp else 0.5.dp,
                                            color = if (isFrozenRow) SpreadsheetGreen else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$row",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFrozenRow) SpreadsheetGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Editable and Formula Responsive Data Cells
                                columns.forEach { col ->
                                    val address = "$col$row"
                                    val isSelected = selectedCell == address
                                    val rawVal = cells[address] ?: ""
                                    
                                    // Evaluate formulas live
                                    val evaluatedVal = if (rawVal.startsWith("=")) {
                                        evaluateFormulaExpression(rawFormula = rawVal.substring(1), activeCells = cells)
                                    } else {
                                        rawVal
                                    }

                                    // Merged states logic mapping
                                    val isCellMergedSpan = mergedCells.any { it.second == address }
                                    val isCellOriginOfMerge = mergedCells.any { it.first == address }

                                    if (!isCellMergedSpan) {
                                        // Colors of validation failure checks
                                        val valRule = dataValidationRules[address]
                                        var isValidationFailed = false
                                        if (valRule == "numbers" && rawVal.isNotEmpty() && rawVal.toDoubleOrNull() == null) {
                                            isValidationFailed = true
                                        } else if (valRule == "email" && rawVal.isNotEmpty() && !rawVal.contains("@")) {
                                            isValidationFailed = true
                                        }

                                        // Duplicate highlighting colors
                                        val isDuplicate = highlightDuplicatesEnabled && findDuplicateCells().contains(rawVal)

                                        // Conditional Formatting colors
                                        var conditionalBg = Color.Transparent
                                        var conditionalTextColor = Color.DarkGray
                                        if (conditionalFormattingRule == "greater_than_50" && evaluatedVal.toDoubleOrNull() != null) {
                                            if (evaluatedVal.toDouble() > 50.0) {
                                                conditionalBg = Color(0xFFD4EFDF) // Light Green
                                                conditionalTextColor = Color(0xFF1E8449)
                                            }
                                        } else if (conditionalFormattingRule == "less_than_10" && evaluatedVal.toDoubleOrNull() != null) {
                                            if (evaluatedVal.toDouble() < 10.0) {
                                                conditionalBg = Color(0xFFFADBD8) // Light Red
                                                conditionalTextColor = Color(0xFF943126)
                                            }
                                        }

                                        val cellBackground = when {
                                            isValidationFailed -> Color(0xFFFCE4D6)
                                            isDuplicate -> Color(0xFFFDEBD0)
                                            conditionalBg != Color.Transparent -> conditionalBg
                                            isSelected -> SpreadsheetGreen.copy(alpha = 0.12f)
                                            else -> Color.Transparent
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(if (isCellOriginOfMerge) 2f else 1f)
                                                .fillMaxHeight()
                                                .background(cellBackground)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                                    color = if (isSelected) SpreadsheetGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                                )
                                                .clickable { viewModel.selectSheetCell(address) }
                                                .testTag("cell_$address"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(horizontal = 2.dp)
                                            ) {
                                                if (isValidationFailed) {
                                                    Icon(
                                                        imageVector = Icons.Default.Warning,
                                                        contentDescription = "خلل التحقق",
                                                        tint = Color.Red,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                }

                                                Text(
                                                    text = evaluatedVal,
                                                    fontSize = (11 + if (isBold && isSelected) 1 else 0).sp,
                                                    fontWeight = if (isBold && isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontStyle = if (isItalic && isSelected) FontStyle.Italic else FontStyle.Normal,
                                                    color = when {
                                                        rawVal.startsWith("=") -> SpreadsheetGreen
                                                        conditionalTextColor != Color.DarkGray -> conditionalTextColor
                                                        else -> Color.DarkGray
                                                    },
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
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

    // --- HIGHLY INTERACTIVE COLLATERIZED CANVAS CHARTS DRAWER WIZARD ---
    if (showChartWizard) {
        AlertDialog(
            onDismissRequest = { showChartWizard = false },
            title = { Text("صانع الرسوم البيانية الإدارية", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("اختر نمط الرسم البياني لتمثيل بيانات الأعمدة حسابياً لمجلس الإدارة:", fontSize = 11.sp)
                    
                    // Chart Type Toggle Choices
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("bar" to "عمودي", "pie" to "دائري", "line" to "خطي", "scatter" to "مبعثر", "area" to "مساحي").forEach { (typeKey, typeLabel) ->
                            val isSelected = selectedChartType == typeKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) SpreadsheetGreen else Color.LightGray.copy(alpha = 0.2f))
                                    .clickable { selectedChartType = typeKey }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = typeLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // GRAPHIC CANVAS AREA - DRAWING DYNAMIC ESTIMATES
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Color(0xFFFAFAF9), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Gather values from cells to map
                        val valuesList = cells.values
                            .filter { !it.startsWith("=") }
                            .mapNotNull { it.toDoubleOrNull() }
                            .ifEmpty { listOf(10.0, 45.0, 80.0, 30.0, 60.0, 95.0, 40.0) }
                        
                        val maxNum = (valuesList.maxOrNull() ?: 100.0).coerceAtLeast(1.0)

                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            val w = size.width
                            val h = size.height

                            when (selectedChartType) {
                                "bar" -> {
                                    val barWidth = w / (valuesList.size * 1.5f)
                                    valuesList.forEachIndexed { idx, valNum ->
                                        val barHeight = (valNum / maxNum) * h
                                        val x = idx * (barWidth * 1.5f) + 10f
                                        val y = h - barHeight
                                        drawRect(
                                            color = SpreadsheetGreen.copy(alpha = 0.8f - (idx * 0.05f)),
                                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight.toFloat()),
                                            topLeft = androidx.compose.ui.geometry.Offset(x, y.toFloat())
                                        )
                                    }
                                }
                                "line" -> {
                                    val stepX = w / (valuesList.size - 1)
                                    val pts = valuesList.mapIndexed { idx, valNum ->
                                        val x = idx * stepX
                                        val y = h - ((valNum / maxNum) * h)
                                        androidx.compose.ui.geometry.Offset(x, y.toFloat())
                                    }
                                    for (i in 0 until pts.size - 1) {
                                        drawLine(
                                            color = SpreadsheetGreen,
                                            start = pts[i],
                                            end = pts[i+1],
                                            strokeWidth = 5f
                                        )
                                        drawCircle(
                                            color = Color.Red,
                                            radius = 6f,
                                            center = pts[i]
                                        )
                                    }
                                    if (pts.isNotEmpty()) {
                                        drawCircle(color = Color.Red, radius = 6f, center = pts.last())
                                    }
                                }
                                "pie" -> {
                                    val total = valuesList.sum()
                                    var activeAngle = 0f
                                    valuesList.forEachIndexed { index, value ->
                                        val sweep = (value / total).toFloat() * 360f
                                        drawArc(
                                            color = SpreadsheetGreen.copy(alpha = 0.9f - (index * 0.1f)),
                                            startAngle = activeAngle,
                                            sweepAngle = sweep,
                                            useCenter = true
                                        )
                                        activeAngle += sweep
                                    }
                                }
                                "scatter" -> {
                                    val stepX = w / (valuesList.size - 1)
                                    valuesList.forEachIndexed { idx, valNum ->
                                        val x = idx * stepX
                                        val y = h - ((valNum / maxNum) * h)
                                        drawCircle(
                                            color = Color(0xFFCA6F1E),
                                            radius = 8f,
                                            center = androidx.compose.ui.geometry.Offset(x, y.toFloat())
                                        )
                                    }
                                }
                                "area" -> {
                                    val stepX = w / (valuesList.size - 1)
                                    val pts = valuesList.mapIndexed { idx, valNum ->
                                        val x = idx * stepX
                                        val y = h - ((valNum / maxNum) * h)
                                        androidx.compose.ui.geometry.Offset(x, y.toFloat())
                                    }
                                    val path = androidx.compose.ui.graphics.Path().apply {
                                        if (pts.isNotEmpty()) {
                                            moveTo(pts.first().x, h)
                                            pts.forEach { lineTo(it.x, it.y) }
                                            lineTo(pts.last().x, h)
                                            close()
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = SpreadsheetGreen.copy(alpha = 0.3f)
                                    )
                                    for (i in 0 until pts.size - 1) {
                                        drawLine(color = SpreadsheetGreen, start = pts[i], end = pts[i+1], strokeWidth = 3f)
                                    }
                                }
                            }
                        }
                    }
                    Text("تم رصد القيم تلقائياً تِبعاً للمجموعات الحسابية المدرجة بالجدول.", fontSize = 9.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = { showChartWizard = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SpreadsheetGreen)
                ) {
                    Text("فهمت وحفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChartWizard = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // --- INTERACTIVE CONDITIONAL FORMATTING DIALOG RULES ---
    if (showFormatDialog) {
        AlertDialog(
            onDismissRequest = { showFormatDialog = false },
            title = { Text("تحليل شرطي للتنسيق الملون", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("حدد القاعدة الشرطية لتطبيق خلفيات التمييز البصري الآلي:", fontSize = 11.sp)
                    
                    listOf(
                        "none" to "بدون تنسيق",
                        "greater_than_50" to "تلوين الخلايا ذات القيمة > 50 بالأخضر",
                        "less_than_10" to "تلوين الخلايا ذات القيمة < 10 بالأحمر"
                    ).forEach { (ruleKey, labelRule) ->
                        Card(
                            onClick = {
                                conditionalFormattingRule = ruleKey
                                showFormatDialog = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (conditionalFormattingRule == ruleKey) SpreadsheetGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(labelRule, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFormatDialog = false }) { Text("إغلاق") }
            }
        )
    }

    // --- PIVOT TABLE ANALYST TABLE ---
    if (showPivotWizard) {
        AlertDialog(
            onDismissRequest = { showPivotWizard = false },
            title = { Text("تحليل واصطناع جدول محوري Pivot Table", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("يقوم الجدول المحوري بلخص وتحليل تصنيفات البيانات المعقدة:", fontSize = 11.sp, color = Color.Gray)
                    
                    // Render beautiful summary pivot card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFAED6F1)),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF2E86C1), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("محصل الحسابات المحورية:", fontWeight = FontWeight.Black, color = Color(0xFF1B4F72), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("التصنيف المدخل", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("محسوب المجموع (Sum)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("الوسيط (Avg)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.6f))
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("رواتب إدارية", fontSize = 11.sp)
                                Text("٢,٤٠٠ ريال", fontSize = 11.sp)
                                Text("١,٢٠٠ ريال", fontSize = 11.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("تكاليف تراخيص", fontSize = 11.sp)
                                Text("٤٥٠ دولار", fontSize = 11.sp)
                                Text("٢٢٥ دولار", fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPivotWizard = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SpreadsheetGreen)
                ) {
                    Text("تطبيق التحليل للمستند")
                }
            }
        )
    }

    // --- ADVANCED INVOICE CREATOR TEMPLATE AUTO-POPULATION ---
    if (showInvoiceWizard) {
        AlertDialog(
            onDismissRequest = { showInvoiceWizard = false },
            title = { Text("صانع الفواتير الإلكترونية المتقدم", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("انقر أدناه لإدراج قالب فاتورة مبيعات تفاعلي يحوي كافّة الصيغ والعمليات المناسبة:", fontSize = 11.sp)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("محتوى الفاتورة المعتمدة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SpreadsheetGreen)
                            Text("- الخلية A1: الوصف / الاسم", fontSize = 10.sp)
                            Text("- الخلية B1: الكمية المشتراة", fontSize = 10.sp)
                            Text("- الخلية C1: السعر للمنتج الواحد", fontSize = 10.sp)
                            Text("- الخلية D1: المجموع الفرعي (=B1*C1)", fontSize = 10.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val invoiceCells = mapOf(
                            "A1" to "فاتورة مبيعات",
                            "B1" to "الكمية", "C1" to "السعر", "D1" to "الإجمالي",
                            "A2" to "ترخيص مكتب سحابي", "B2" to "5", "C2" to "120", "D2" to "=B2*C2",
                            "A3" to "مذكرة كتابية إلكترونية", "B3" to "10", "C3" to "25", "D3" to "=B3*C3",
                            "A4" to "المجموع الكامل", "B4" to "15", "C4" to "145", "D4" to "=SUM"
                        )
                        // Trigger VM calls to write each cell
                        invoiceCells.forEach { (address, value) ->
                            viewModel.selectSheetCell(address)
                            viewModel.updateSheetCellInput(value)
                        }
                        showInvoiceWizard = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpreadsheetGreen)
                ) {
                    Text("حقن وتطبيق قالب الفاتورة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInvoiceWizard = false }) { Text("إلغاء") }
            }
        )
    }

    // --- SORTING AND FILE FILTERS ---
    if (showSortingFilterDialog) {
        AlertDialog(
            onDismissRequest = { showSortingFilterDialog = false },
            title = { Text("فرز وتصفية البيانات المتقدمة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("فرز تصاعدي أو تنازلي بحسب بيانات العمود A:", fontSize = 11.sp, color = Color.Gray)
                    
                    Button(
                        onClick = {
                            // Mock sort cells by content strings
                            val sortedAddrs = cells.filter { it.key.startsWith("A") }.toList().sortedBy { it.second }.map { it.first }
                            val sortedValues = cells.filter { it.key.startsWith("A") }.values.sorted()
                            val updatedMap = cells.toMutableMap()
                            sortedAddrs.forEachIndexed { i, add ->
                                updatedMap[add] = sortedValues[i]
                            }
                            // Trigger updates
                            updatedMap.forEach { (add, value) ->
                                viewModel.selectSheetCell(add)
                                viewModel.updateSheetCellInput(value)
                            }
                            showSortingFilterDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpreadsheetGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("فرز تصاعدي (أ -> ي)")
                    }

                    Button(
                        onClick = {
                            val sortedAddrs = cells.filter { it.key.startsWith("A") }.toList().sortedByDescending { it.second }.map { it.first }
                            val sortedValues = cells.filter { it.key.startsWith("A") }.values.sortedDescending()
                            val updatedMap = cells.toMutableMap()
                            sortedAddrs.forEachIndexed { i, add ->
                                updatedMap[add] = sortedValues[i]
                            }
                            updatedMap.forEach { (add, value) ->
                                viewModel.selectSheetCell(add)
                                viewModel.updateSheetCellInput(value)
                            }
                            showSortingFilterDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpreadsheetGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("فرز تنازلي (ي -> أ)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortingFilterDialog = false }) { Text("إغلاق") }
            }
        )
    }

    // --- TITLE RE-EDIT DIALOG ---
    if (showTitleEditDialog) {
        AlertDialog(
            onDismissRequest = { showTitleEditDialog = false },
            title = { Text("تعديل اسم الجدول الحسابي", fontWeight = FontWeight.Bold) },
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
                            viewModel.updateSheetTitle(tempTitle)
                        }
                        showTitleEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpreadsheetGreen)
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
