package com.memorycare.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memorycare.app.model.DifficultyLevel
import com.memorycare.app.model.GameResult
import com.memorycare.app.model.GameType
import com.memorycare.app.model.UserRole
import com.memorycare.app.ui.components.AccessibleBigButton
import com.memorycare.app.ui.components.SpaceTopBar
import com.memorycare.app.ui.theme.ActionPrimaryBlue
import com.memorycare.app.ui.theme.ActionSuccessGreen
import com.memorycare.app.ui.theme.DeepSpaceNavy
import com.memorycare.app.ui.theme.ElderlyBgLight
import com.memorycare.app.ui.theme.ElderlyTextPrimary
import com.memorycare.app.ui.theme.ElderlyTextSecondary
import com.memorycare.app.ui.theme.OrbitalCyan
import com.memorycare.app.viewmodel.MemoryCareViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChartPoint(
    val dateLabel: String,
    val value: Float,
    val gameType: GameType,
    val x: Float = 0f,
    val y: Float = 0f
)

/**
 * Progress Analytics Screen featuring 3 comprehensive cognitive charts:
 * 1. Accuracy Over Time (Multi-line chart with tap tooltips)
 * 2. Performance by Game Type (Color-coded bar chart with games count)
 * 3. Difficulty Progression (Step chart showing Easy=1, Med=2, Hard=3 changes over time)
 */
@Composable
fun ProgressAnalyticsScreen(
    viewModel: MemoryCareViewModel,
    onNavigateBack: () -> Unit
) {
    val satelliteStatus by viewModel.satelliteStatus.collectAsState()
    val gameResults by viewModel.gameResults.collectAsState()

    Scaffold(
        topBar = {
            SpaceTopBar(
                title = "Progress Analytics",
                subtitle = "Cognitive Telemetry Trends",
                userRole = UserRole.CARETAKER,
                satelliteStatus = satelliteStatus,
                onBackClick = onNavigateBack,
                onSyncClick = { viewModel.triggerSatelliteSync() }
            )
        },
        containerColor = ElderlyBgLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Header summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSpaceNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📈", fontSize = 32.sp, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text(
                            text = "14-Day Cognitive Assessment",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Tracking session accuracy, engagement count, and difficulty progression.",
                            fontSize = 13.sp,
                            color = OrbitalCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (gameResults.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, Color(0xFFD0D7DE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📊", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Game Sessions Yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElderlyTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "As the patient plays cognitive games, accuracy trends, category breakdowns, and adaptive difficulty progression will appear here automatically.",
                            fontSize = 14.sp,
                            color = ElderlyTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // ==========================================
                // GRAPH 1: ACCURACY OVER TIME (LINE CHART)
                // ==========================================
                AccuracyOverTimeLineChartCard(results = gameResults)

                Spacer(modifier = Modifier.height(18.dp))

                // ==========================================
                // GRAPH 2: PERFORMANCE BY GAME TYPE (BAR CHART)
                // ==========================================
                PerformanceByGameBarChartCard(results = gameResults)

                Spacer(modifier = Modifier.height(18.dp))

                // ==========================================
                // GRAPH 3: DIFFICULTY PROGRESSION (STEP CHART)
                // ==========================================
                DifficultyProgressionStepChartCard(results = gameResults)
            }

            Spacer(modifier = Modifier.height(20.dp))

            AccessibleBigButton(
                text = "Back to Caretaker Dashboard",
                emoji = "⬅️",
                onClick = onNavigateBack,
                containerColor = ActionPrimaryBlue,
                modifier = Modifier.fillMaxWidth(),
                minHeight = 56.dp
            )
        }
    }
}

// -------------------------------------------------------------
// GRAPH 1: Line Chart
// -------------------------------------------------------------
@Composable
private fun AccuracyOverTimeLineChartCard(results: List<GameResult>) {
    var selectedPointInfo by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf<GameType?>(null) }

    val chronologicalResults = remember(results) {
        results.take(20).sortedBy { it.timestamp }
    }

    val memoryPoints = chronologicalResults.filter { it.gameType == GameType.MEMORY_MATCH }
    val numberPoints = chronologicalResults.filter { it.gameType == GameType.NUMBER_SEQUENCE }
    val routinePoints = chronologicalResults.filter { it.gameType == GameType.DAILY_ROUTINE || it.gameType == GameType.CATEGORY_SORTING }
    val facePoints = chronologicalResults.filter { it.gameType == GameType.FACE_NAME_ASSOCIATION }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, Color(0xFFD0D7DE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "1. Accuracy Over Time",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ElderlyTextPrimary
                    )
                    Text(
                        text = if (selectedFilter != null) {
                            "Showing ${selectedFilter?.displayName} • Tap chip to show all"
                        } else {
                            "Last 14 days • Tap legend to isolate • Tap dots for score"
                        },
                        fontSize = 12.sp,
                        color = if (selectedFilter != null) ActionPrimaryBlue else ElderlyTextSecondary
                    )
                }
            }

            // Legend
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendChip(
                    color = Color(0xFF1976D2),
                    label = "Memory",
                    isSelected = selectedFilter == GameType.MEMORY_MATCH,
                    hasActiveFilter = selectedFilter != null,
                    onClick = {
                        selectedFilter = if (selectedFilter == GameType.MEMORY_MATCH) null else GameType.MEMORY_MATCH
                    }
                )
                LegendChip(
                    color = Color(0xFFE65100),
                    label = "Day/Clock",
                    isSelected = selectedFilter == GameType.NUMBER_SEQUENCE,
                    hasActiveFilter = selectedFilter != null,
                    onClick = {
                        selectedFilter = if (selectedFilter == GameType.NUMBER_SEQUENCE) null else GameType.NUMBER_SEQUENCE
                    }
                )
                LegendChip(
                    color = Color(0xFF00838F),
                    label = "Echoes",
                    isSelected = selectedFilter == GameType.DAILY_ROUTINE,
                    hasActiveFilter = selectedFilter != null,
                    onClick = {
                        selectedFilter = if (selectedFilter == GameType.DAILY_ROUTINE) null else GameType.DAILY_ROUTINE
                    }
                )
                LegendChip(
                    color = Color(0xFF6A1B9A),
                    label = "Face-Name",
                    isSelected = selectedFilter == GameType.FACE_NAME_ASSOCIATION,
                    hasActiveFilter = selectedFilter != null,
                    onClick = {
                        selectedFilter = if (selectedFilter == GameType.FACE_NAME_ASSOCIATION) null else GameType.FACE_NAME_ASSOCIATION
                    }
                )
            }

            // Selected Point Tooltip Box
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (selectedPointInfo != null) Color(0xFFE3F2FD) else Color(0xFFF5F5F5),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selectedPointInfo ?: "Tap any dot on the chart to inspect score.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedPointInfo != null) ActionPrimaryBlue else ElderlyTextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Line Chart
            val sdf = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }
            val renderedPoints = remember { mutableListOf<ChartPoint>() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(chronologicalResults, selectedFilter) {
                            detectTapGestures { tapOffset ->
                                val hit = renderedPoints.minByOrNull {
                                    val dx = it.x - tapOffset.x
                                    val dy = it.y - tapOffset.y
                                    dx * dx + dy * dy
                                }
                                if (hit != null && (Math.hypot((hit.x - tapOffset.x).toDouble(), (hit.y - tapOffset.y).toDouble()) < 40)) {
                                    selectedPointInfo = "${hit.gameType.displayName} on ${hit.dateLabel}: ${hit.value.toInt()}% Accuracy"
                                }
                            }
                        }
                ) {
                    renderedPoints.clear()
                    val paddingLeft = 40.dp.toPx()
                    val paddingRight = 16.dp.toPx()
                    val paddingTop = 16.dp.toPx()
                    val paddingBottom = 28.dp.toPx()

                    val chartWidth = size.width - paddingLeft - paddingRight
                    val chartHeight = size.height - paddingTop - paddingBottom

                    // Draw Horizontal Gridlines (0%, 25%, 50%, 75%, 100%)
                    val gridSteps = 4
                    for (i in 0..gridSteps) {
                        val y = paddingTop + (chartHeight / gridSteps) * i
                        val pct = 100 - (i * 25)
                        drawLine(
                            color = Color(0xFFECEFF1),
                            start = Offset(paddingLeft, y),
                            end = Offset(size.width - paddingRight, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw Lines & Dots for each game type
                    fun drawGameLine(points: List<GameResult>, lineColor: Color, gameType: GameType) {
                        val matchesFilter = selectedFilter == null || selectedFilter == gameType ||
                                (selectedFilter == GameType.DAILY_ROUTINE && gameType == GameType.CATEGORY_SORTING)
                        if (!matchesFilter) return
                        if (points.isEmpty()) return

                        val path = Path()
                        val stepX = if (chronologicalResults.size > 1) chartWidth / (chronologicalResults.size - 1) else chartWidth / 2

                        var isFirst = true
                        chronologicalResults.forEachIndexed { index, session ->
                            if (points.any { it.id == session.id }) {
                                val x = paddingLeft + index * stepX
                                val y = paddingTop + chartHeight * (1f - (session.accuracyPercentage / 100f))

                                if (isFirst) {
                                    path.moveTo(x, y)
                                    isFirst = false
                                } else {
                                    path.lineTo(x, y)
                                }

                                val cp = ChartPoint(
                                    dateLabel = sdf.format(Date(session.timestamp)),
                                    value = session.accuracyPercentage.toFloat(),
                                    gameType = session.gameType,
                                    x = x,
                                    y = y
                                )
                                renderedPoints.add(cp)
                            }
                        }

                        // Draw continuous path
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(
                                width = if (selectedFilter == gameType || (selectedFilter == GameType.DAILY_ROUTINE && gameType == GameType.CATEGORY_SORTING)) 3.5.dp.toPx() else 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        // Draw Circles on Points with white halo
                        renderedPoints.filter { pt ->
                            if (gameType == GameType.DAILY_ROUTINE) {
                                pt.gameType == GameType.DAILY_ROUTINE || pt.gameType == GameType.CATEGORY_SORTING
                            } else {
                                pt.gameType == gameType
                            }
                        }.forEach { pt ->
                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = Offset(pt.x, pt.y)
                            )
                            drawCircle(
                                color = lineColor,
                                radius = 4.dp.toPx(),
                                center = Offset(pt.x, pt.y)
                            )
                        }
                    }

                    drawGameLine(memoryPoints, Color(0xFF1976D2), GameType.MEMORY_MATCH)
                    drawGameLine(numberPoints, Color(0xFFE65100), GameType.NUMBER_SEQUENCE)
                    drawGameLine(routinePoints, Color(0xFF00838F), GameType.DAILY_ROUTINE)
                    drawGameLine(facePoints, Color(0xFF6A1B9A), GameType.FACE_NAME_ASSOCIATION)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// GRAPH 2: Bar Chart (Performance by Game Type)
// -------------------------------------------------------------
@Composable
private fun PerformanceByGameBarChartCard(results: List<GameResult>) {
    val memoryResults = results.filter { it.gameType == GameType.MEMORY_MATCH }
    val numberResults = results.filter { it.gameType == GameType.NUMBER_SEQUENCE }
    val routineResults = results.filter { it.gameType == GameType.DAILY_ROUTINE || it.gameType == GameType.CATEGORY_SORTING }
    val faceResults = results.filter { it.gameType == GameType.FACE_NAME_ASSOCIATION }

    val avgMemory = if (memoryResults.isNotEmpty()) memoryResults.map { it.accuracyPercentage }.average().toInt() else 0
    val avgNumber = if (numberResults.isNotEmpty()) numberResults.map { it.accuracyPercentage }.average().toInt() else 0
    val avgRoutine = if (routineResults.isNotEmpty()) routineResults.map { it.accuracyPercentage }.average().toInt() else 0
    val avgFace = if (faceResults.isNotEmpty()) faceResults.map { it.accuracyPercentage }.average().toInt() else 0

    val barData = listOf(
        Triple("Memory", avgMemory, memoryResults.size),
        Triple("Day/Clock", avgNumber, numberResults.size),
        Triple("Echoes", avgRoutine, routineResults.size),
        Triple("Face-Name", avgFace, faceResults.size)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, Color(0xFFD0D7DE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "2. Performance by Game Type",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ElderlyTextPrimary
            )
            Text(
                text = "Average accuracy (%) and total sessions played",
                fontSize = 12.sp,
                color = ElderlyTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bars Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                barData.forEachIndexed { index, (label, avgScore, count) ->
                    val barColor = when (index) {
                        0 -> Color(0xFF1976D2)
                        1 -> Color(0xFFE65100)
                        2 -> Color(0xFF00838F)
                        else -> Color(0xFF6A1B9A)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Score label
                        Text(
                            text = "$avgScore%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = barColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Bar
                        val heightFraction = (avgScore / 100f).coerceIn(0.1f, 1f)
                        Box(
                            modifier = Modifier
                                .width(46.dp)
                                .height((110 * heightFraction).dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(barColor.copy(alpha = 0.8f), barColor)
                                    ),
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Game label
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElderlyTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        // Count of games played below bar
                        Text(
                            text = "$count played",
                            fontSize = 11.sp,
                            color = ElderlyTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// GRAPH 3: Step Chart (Difficulty Progression)
// -------------------------------------------------------------
@Composable
private fun DifficultyProgressionStepChartCard(results: List<GameResult>) {
    var selectedFilter by remember { mutableStateOf<GameType?>(null) }

    val chronologicalResults = remember(results) {
        results.take(20).sortedBy { it.timestamp }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, Color(0xFFD0D7DE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "3. Difficulty Progression",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ElderlyTextPrimary
            )
            Text(
                text = if (selectedFilter != null) {
                    "Showing ${selectedFilter?.displayName} • Tap chip to show all"
                } else {
                    "Adaptive step progression (Easy=1, Med=2, Hard=3) • Parallel non-overlapping lanes"
                },
                fontSize = 12.sp,
                color = if (selectedFilter != null) ActionPrimaryBlue else ElderlyTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendChip(
                    color = Color(0xFF1976D2),
                    label = "Memory",
                    isSelected = selectedFilter == GameType.MEMORY_MATCH,
                    hasActiveFilter = selectedFilter != null,
                    onClick = {
                        selectedFilter = if (selectedFilter == GameType.MEMORY_MATCH) null else GameType.MEMORY_MATCH
                    }
                )
                LegendChip(
                    color = Color(0xFFE65100),
                    label = "Day/Clock",
                    isSelected = selectedFilter == GameType.NUMBER_SEQUENCE,
                    hasActiveFilter = selectedFilter != null,
                    onClick = {
                        selectedFilter = if (selectedFilter == GameType.NUMBER_SEQUENCE) null else GameType.NUMBER_SEQUENCE
                    }
                )
                LegendChip(
                    color = Color(0xFF00838F),
                    label = "Echoes",
                    isSelected = selectedFilter == GameType.DAILY_ROUTINE,
                    hasActiveFilter = selectedFilter != null,
                    onClick = {
                        selectedFilter = if (selectedFilter == GameType.DAILY_ROUTINE) null else GameType.DAILY_ROUTINE
                    }
                )
                LegendChip(
                    color = Color(0xFF6A1B9A),
                    label = "Face-Name",
                    isSelected = selectedFilter == GameType.FACE_NAME_ASSOCIATION,
                    hasActiveFilter = selectedFilter != null,
                    onClick = {
                        selectedFilter = if (selectedFilter == GameType.FACE_NAME_ASSOCIATION) null else GameType.FACE_NAME_ASSOCIATION
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Step Canvas
            val sdf = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val paddingLeft = 50.dp.toPx()
                    val paddingRight = 16.dp.toPx()
                    val paddingTop = 16.dp.toPx()
                    val paddingBottom = 24.dp.toPx()

                    val chartWidth = size.width - paddingLeft - paddingRight
                    val chartHeight = size.height - paddingTop - paddingBottom

                    // Draw 3 horizontal levels (Level 3 Hard, Level 2 Medium, Level 1 Easy)
                    val levels = listOf(
                        Pair("HARD (3)", paddingTop),
                        Pair("MED (2)", paddingTop + chartHeight / 2),
                        Pair("EASY (1)", paddingTop + chartHeight)
                    )

                    levels.forEach { (lbl, y) ->
                        drawLine(
                            color = Color(0xFFECEFF1),
                            start = Offset(paddingLeft, y),
                            end = Offset(size.width - paddingRight, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Parallel non-overlapping lane offsets for each game type
                    fun getDifficultyLaneOffset(gameType: GameType): Float {
                        return if (selectedFilter != null) 0f else when (gameType) {
                            GameType.MEMORY_MATCH -> -6.dp.toPx()
                            GameType.NUMBER_SEQUENCE -> -2.dp.toPx()
                            GameType.DAILY_ROUTINE, GameType.CATEGORY_SORTING -> 2.dp.toPx()
                            GameType.FACE_NAME_ASSOCIATION -> 6.dp.toPx()
                        }
                    }

                    fun drawGameStepLine(gameType: GameType, color: Color) {
                        val matchesFilter = selectedFilter == null || selectedFilter == gameType ||
                            (selectedFilter == GameType.DAILY_ROUTINE && gameType == GameType.CATEGORY_SORTING)
                        if (!matchesFilter) return

                        val isTypeMatch = { g: GameType ->
                            if (gameType == GameType.DAILY_ROUTINE) {
                                g == GameType.DAILY_ROUTINE || g == GameType.CATEGORY_SORTING
                            } else {
                                g == gameType
                            }
                        }

                        val filtered = chronologicalResults.filter { isTypeMatch(it.gameType) }
                        if (filtered.isEmpty()) return

                        val stepX = if (chronologicalResults.size > 1) chartWidth / (chronologicalResults.size - 1) else chartWidth / 2
                        val path = Path()
                        val laneOffset = getDifficultyLaneOffset(gameType)

                        var prevX: Float? = null
                        var prevY: Float? = null

                        chronologicalResults.forEachIndexed { index, session ->
                            if (isTypeMatch(session.gameType)) {
                                val x = paddingLeft + index * stepX
                                val baseY = when (session.difficulty) {
                                    DifficultyLevel.HARD -> paddingTop
                                    DifficultyLevel.MEDIUM -> paddingTop + chartHeight / 2
                                    DifficultyLevel.EASY -> paddingTop + chartHeight
                                }
                                val y = baseY + laneOffset

                                if (prevX == null || prevY == null) {
                                    path.moveTo(x, y)
                                } else {
                                    // Step connection: horizontal then vertical
                                    path.lineTo(x, prevY!!)
                                    path.lineTo(x, y)
                                }
                                prevX = x
                                prevY = y

                                // Draw white halo + inner colored dot
                                drawCircle(
                                    color = Color.White,
                                    radius = 5.dp.toPx(),
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = color,
                                    radius = 3.5.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                        }

                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(
                                width = if (selectedFilter == gameType || (selectedFilter == GameType.DAILY_ROUTINE && gameType == GameType.CATEGORY_SORTING)) 3.dp.toPx() else 2.5.dp.toPx(),
                                cap = StrokeCap.Square
                            )
                        )
                    }

                    drawGameStepLine(GameType.MEMORY_MATCH, Color(0xFF1976D2))
                    drawGameStepLine(GameType.NUMBER_SEQUENCE, Color(0xFFE65100))
                    drawGameStepLine(GameType.DAILY_ROUTINE, Color(0xFF00838F))
                    drawGameStepLine(GameType.FACE_NAME_ASSOCIATION, Color(0xFF6A1B9A))
                }
            }
        }
    }
}

@Composable
private fun LegendChip(
    color: Color,
    label: String,
    isSelected: Boolean = false,
    hasActiveFilter: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val alpha = if (hasActiveFilter && !isSelected) 0.35f else 1f
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, color) else null,
        modifier = Modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = if (isSelected) 6.dp else 4.dp, vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color.copy(alpha = alpha), CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isSelected) color else ElderlyTextPrimary.copy(alpha = alpha)
            )
        }
    }
}
