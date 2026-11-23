package com.rishugh.docal.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rishugh.docal.model.Task
import com.rishugh.docal.viewmodel.TaskViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Constants, Cached Formatters & Palette (Zero Subcomposition Architecture)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
private val DRAWER_WIDTH       = 292.dp
private val HOUR_HEIGHT        = 64.dp
private val TIME_COL_WIDTH     = 44.dp
private val DAY_HEADER_HEIGHT  = 56.dp
private val RESIZE_HANDLE_ZONE = 18.dp
private const val HOLD_DURATION_MS = 300L

private val BgSurface     = Color(0xFFF8FAFC)
private val CardWhite     = Color(0xFFFFFFFF)
private val TextPrimary   = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF64748B)
private val TextMuted     = Color(0xFF94A3B8)
private val BorderSubtle  = Color(0xFFE2E8F0)
private val GridLineColor = Color(0xFFEDF2F7)
private val BrandPrimary  = Color(0xFF4F46E5)
private val BrandLight    = Color(0xFFEEF2FF)
private val DangerRed     = Color(0xFFEF4444)

private val AppTextSelectionColors = TextSelectionColors(
    handleColor = Color(0xFF3B82F6),
    backgroundColor = Color(0xFF3B82F6).copy(alpha = 0.35f)
)

private val FMT_MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val FMT_MMM: DateTimeFormatter        = DateTimeFormatter.ofPattern("MMM")
private val FMT_MMM_YEAR: DateTimeFormatter   = DateTimeFormatter.ofPattern("MMM yyyy")
private val FMT_TIME_AMPM: DateTimeFormatter  = DateTimeFormatter.ofPattern("h:mm a")
private val FMT_TIME_SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm")
private val FMT_DAY_DATE: DateTimeFormatter   = DateTimeFormatter.ofPattern("EEE, MMM d")

private val HOUR_LABELS = Array(24) { h ->
    when {
        h == 0  -> "12 AM"
        h < 12  -> "$h AM"
        h == 12 -> "12 PM"
        else    -> "${h - 12} PM"
    }
}

private val PALETTE = listOf(
    0xFF4F46E5L, // Indigo
    0xFF0EA5E9L, // Sky Blue
    0xFF10B981L, // Emerald
    0xFFF59E0BL, // Amber
    0xFFEF4444L, // Rose Red
    0xFF8B5CF6L, // Violet
    0xFFEC4899L, // Pink
    0xFF14B8A6L  // Teal
)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Drag State
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
internal enum class DragMode { MOVE, RESIZE_TOP, RESIZE_BOTTOM }

@Immutable
internal data class DragState(
    val task: Task,
    val mode: DragMode,
    val windowPos: Offset,
    val startWindowPos: Offset,
    val grabOffsetY: Float = 0f,
    val snappedDate: LocalDate? = task.date,
    val snappedTime: LocalTime? = task.time,
    val snappedDuration: Int = task.durationMinutes,
    val originDate: LocalDate? = task.date,
    val originTime: LocalTime? = task.time,
    val originDuration: Int = task.durationMinutes,
    val fromDrawer: Boolean = task.date == null,
    val isOverDrawer: Boolean = task.date == null
)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Root Composable (Zero Subcomposition, Single ScrollState, Instant Startup)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoCalApp(viewModel: TaskViewModel = viewModel()) {
    val tasks          by viewModel.tasks.collectAsState()
    val weekStart      by viewModel.currentWeekStart.collectAsState()
    val isDrawerOpen   by viewModel.isDrawerOpen.collectAsState()
    val selectedTaskId by viewModel.selectedTaskId.collectAsState()

    val haptic             = LocalHapticFeedback.current
    val density            = LocalDensity.current
    val focusManager       = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope              = rememberCoroutineScope()

    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    LaunchedEffect(isDrawerOpen) {
        if (!isDrawerOpen) {
            dismissKeyboard()
        }
    }

    // Single shared verticalScroll and smooth Animatable horizontal slide offset (Zero Pager overhead!)
    val vertScroll = rememberScrollState(initial = with(density) { (HOUR_HEIGHT * 8).roundToPx() })
    val weekSlideOffsetPx = remember { Animatable(0f) }

    fun animateToWeek(targetMonday: LocalDate, dir: Int) {
        scope.launch {
            val slideDist = with(density) { 180.dp.toPx() }
            viewModel.setWeekStart(targetMonday)
            weekSlideOffsetPx.snapTo(dir * slideDist)
            weekSlideOffsetPx.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        }
    }

    var drag           by remember { mutableStateOf<DragState?>(null) }
    var viewportBounds by remember { mutableStateOf<Rect?>(null) }
    var rootCoords     by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val itemBoundsMap  = remember { mutableStateMapOf<String, Rect>() }

    var activeSheetTask by remember { mutableStateOf<Task?>(null) }

    // ── Stable, Direct Snap Helper ──────────────────────────────────────────
    fun snapToGrid(windowPos: Offset, grabOffsetY: Float, currentMonday: LocalDate = weekStart): Pair<LocalDate?, LocalTime?> {
        val vp = viewportBounds ?: return null to null
        val gutterPx  = with(density) { TIME_COL_WIDTH.toPx() }
        val headerPx  = with(density) { DAY_HEADER_HEIGHT.toPx() }
        val hourPx    = with(density) { HOUR_HEIGHT.toPx() }

        val gridLeft   = vp.left + gutterPx
        val gridRight  = vp.right
        val gridTop    = vp.top + headerPx
        val gridBottom = vp.bottom

        if (windowPos.x < gridLeft - 48f || windowPos.x > gridRight + 48f ||
            windowPos.y < gridTop - 64f || windowPos.y > gridBottom + 64f
        ) {
            return null to null
        }

        val gridW = (gridRight - gridLeft).coerceAtLeast(1f)
        val dayPx = gridW / 7f

        val relX = (windowPos.x - gridLeft).coerceIn(0f, gridW - 1f)
        val relYInScroll = (windowPos.y - gridTop) + vertScroll.value - grabOffsetY
        val maxScrollY = hourPx * 24f - 1f
        val clampedY = relYInScroll.coerceIn(0f, maxScrollY)

        val dayIdx = (relX / dayPx).toInt().coerceIn(0, 6)
        val rawMinutes = (clampedY / hourPx * 60f).roundToInt()
        val snappedTotalMin = ((rawMinutes / 15f).roundToInt() * 15).coerceIn(0, 23 * 60 + 45)
        val h = snappedTotalMin / 60
        val m = snappedTotalMin % 60

        return currentMonday.plusDays(dayIdx.toLong()) to LocalTime.of(h, m)
    }

    // ── Smooth Google Calendar-Style Edge Week Switching & Vertical Auto-Scroll ──
    val isDraggingMove = drag?.mode == DragMode.MOVE && drag?.isOverDrawer == false
    LaunchedEffect(isDraggingMove) {
        if (isDraggingMove) {
            var edgeHoldMs = 0L
            var lastEdgeDir = 0

            while (true) {
                delay(24)
                val current = drag ?: break
                if (current.mode != DragMode.MOVE || current.isOverDrawer) break
                val vp = viewportBounds ?: continue

                val gutterPx = with(density) { TIME_COL_WIDTH.toPx() }
                val headerPx = with(density) { DAY_HEADER_HEIGHT.toPx() }
                val gridLeft = vp.left + gutterPx
                val gridRight = vp.right
                val gridTop = vp.top + headerPx
                val gridBottom = vp.bottom

                // 1. Vertical auto-scroll near top or bottom
                val vZonePx = with(density) { 54.dp.toPx() }
                val maxVSpeed = with(density) { 11.dp.toPx() }
                val wy = current.windowPos.y
                var didVScroll = false
                if (wy in gridTop..(gridTop + vZonePx) && vertScroll.value > 0) {
                    val ratio = 1f - ((wy - gridTop) / vZonePx).coerceIn(0f, 1f)
                    vertScroll.scrollBy(-maxVSpeed * ratio)
                    didVScroll = true
                } else if (wy in (gridBottom - vZonePx)..gridBottom && vertScroll.value < vertScroll.maxValue) {
                    val ratio = 1f - ((gridBottom - wy) / vZonePx).coerceIn(0f, 1f)
                    vertScroll.scrollBy(maxVSpeed * ratio)
                    didVScroll = true
                }
                if (didVScroll) {
                    val (nd, nt) = snapToGrid(current.windowPos, current.grabOffsetY)
                    if (nd != null && nt != null) {
                        drag = current.copy(snappedDate = nd, snappedTime = nt)
                    }
                }

                // 2. Smooth horizontal week switching near left or right edge
                val hZonePx = with(density) { 36.dp.toPx() }
                val wx = current.windowPos.x
                val dir = when {
                    wx <= gridLeft + hZonePx -> -1
                    wx >= gridRight - hZonePx -> 1
                    else -> 0
                }

                if (dir != 0 && !weekSlideOffsetPx.isRunning) {
                    if (dir == lastEdgeDir) {
                        edgeHoldMs += 24L
                    } else {
                        lastEdgeDir = dir
                        edgeHoldMs = 0L
                    }

                    if (edgeHoldMs >= 360L) {
                        edgeHoldMs = -240L
                        val targetMonday = weekStart.plusWeeks(dir.toLong())
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        animateToWeek(targetMonday, dir)

                        val latest = drag ?: break
                        val (newDate, newTime) = snapToGrid(latest.windowPos, latest.grabOffsetY, targetMonday)
                        drag = latest.copy(
                            snappedDate = newDate ?: latest.snappedDate,
                            snappedTime = newTime ?: latest.snappedTime
                        )
                    }
                } else {
                    lastEdgeDir = 0
                    edgeHoldMs = 0L
                }
            }
        }
    }

    // ── Drag Callbacks ──────────────────────────────────────────────────────
    val onMoveStart: (Task, Offset, Float, Boolean) -> Unit = { task, windowPos, grabOffsetY, fromDrawer ->
        dismissKeyboard()
        if (!fromDrawer) {
            viewModel.selectTask(task.id)
        }
        val (initDate, initTime) = if (!fromDrawer) {
            task.date to task.time
        } else {
            null to null
        }
        drag = DragState(
            task = task,
            mode = DragMode.MOVE,
            windowPos = windowPos,
            startWindowPos = windowPos,
            grabOffsetY = grabOffsetY,
            snappedDate = initDate,
            snappedTime = initTime,
            snappedDuration = task.durationMinutes,
            originDate = task.date,
            originTime = task.time,
            originDuration = task.durationMinutes,
            fromDrawer = fromDrawer,
            isOverDrawer = fromDrawer && isDrawerOpen
        )
    }

    val onResizeStart: (Task, Offset, DragMode) -> Unit = { task, windowPos, mode ->
        dismissKeyboard()
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.selectTask(task.id)
        drag = DragState(
            task = task,
            mode = mode,
            windowPos = windowPos,
            startWindowPos = windowPos,
            grabOffsetY = 0f,
            snappedDate = task.date,
            snappedTime = task.time,
            snappedDuration = task.durationMinutes,
            originDate = task.date,
            originTime = task.time,
            originDuration = task.durationMinutes,
            fromDrawer = false,
            isOverDrawer = false
        )
    }

    val onDragDelta: (Offset) -> Unit = { delta ->
        drag?.let { d ->
            val newPos = d.windowPos + delta
            val hourPx = with(density) { HOUR_HEIGHT.toPx() }

            when (d.mode) {
                DragMode.MOVE -> {
                    val rootW    = rootCoords?.size?.width?.toFloat() ?: 0f
                    val drawerPx = with(density) { DRAWER_WIDTH.toPx() }
                    val localPos = try { rootCoords?.windowToLocal(newPos) ?: newPos } catch (_: Exception) { newPos }
                    val inDrawerArea = isDrawerOpen && localPos.x >= (rootW - drawerPx)

                    if (inDrawerArea) {
                        val unscheduled = tasks.filter { it.date == null }
                        if (unscheduled.isNotEmpty()) {
                            var targetIdx: Int? = null
                            unscheduled.forEachIndexed { index, item ->
                                val b = itemBoundsMap[item.id]
                                if (b != null && newPos.y in b.top..b.bottom) {
                                    targetIdx = index
                                }
                            }
                            if (targetIdx == null) {
                                val firstBounds = unscheduled.firstOrNull()?.let { itemBoundsMap[it.id] }
                                val lastBounds  = unscheduled.lastOrNull()?.let { itemBoundsMap[it.id] }
                                if (firstBounds != null && newPos.y < firstBounds.top) targetIdx = 0
                                else if (lastBounds != null && newPos.y > lastBounds.bottom) targetIdx = unscheduled.lastIndex
                            }
                            targetIdx?.let { idx ->
                                val currentIdx = unscheduled.indexOfFirst { it.id == d.task.id }
                                if (currentIdx != idx) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.reorderTask(d.task.id, idx)
                                }
                            }
                        }
                        drag = d.copy(
                            windowPos = newPos,
                            snappedDate = null,
                            snappedTime = null,
                            isOverDrawer = true
                        )
                    } else {
                        if (isDrawerOpen && d.fromDrawer && localPos.x < (rootW - drawerPx - 24f)) {
                            viewModel.closeDrawer()
                        }
                        val (gridDate, gridTime) = snapToGrid(newPos, d.grabOffsetY)
                        val nextDate = gridDate ?: d.snappedDate ?: d.originDate
                        val nextTime = gridTime ?: d.snappedTime ?: d.originTime
                        if (nextDate != d.snappedDate || nextTime != d.snappedTime) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        drag = d.copy(
                            windowPos = newPos,
                            snappedDate = nextDate,
                            snappedTime = nextTime,
                            isOverDrawer = false
                        )
                    }
                }

                DragMode.RESIZE_BOTTOM -> {
                    val totalDeltaY = newPos.y - d.startWindowPos.y
                    val deltaMin = (totalDeltaY / hourPx * 60f).roundToInt()
                    val rawDuration = d.originDuration + deltaMin
                    val startMin = (d.originTime?.hour ?: 0) * 60 + (d.originTime?.minute ?: 0)
                    val maxDuration = (24 * 60 - startMin).coerceAtLeast(15)
                    val snapped = ((rawDuration / 15f).roundToInt() * 15).coerceIn(15, maxDuration)
                    if (snapped != d.snappedDuration) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    drag = d.copy(windowPos = newPos, snappedDuration = snapped)
                }

                DragMode.RESIZE_TOP -> {
                    val origTime = d.originTime ?: LocalTime.of(9, 0)
                    val origStartMin = origTime.hour * 60 + origTime.minute
                    val origEndMin   = origStartMin + d.originDuration

                    val totalDeltaY = newPos.y - d.startWindowPos.y
                    val deltaMin = (totalDeltaY / hourPx * 60f).roundToInt()
                    val rawNewStart = origStartMin + deltaMin
                    val snappedStart = ((rawNewStart / 15f).roundToInt() * 15)
                        .coerceIn(0, (origEndMin - 15).coerceAtLeast(0))
                    val newDuration = (origEndMin - snappedStart).coerceAtLeast(15)
                    val newTime = LocalTime.of(snappedStart / 60, snappedStart % 60)

                    if (newDuration != d.snappedDuration || newTime != d.snappedTime) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    drag = d.copy(
                        windowPos = newPos,
                        snappedTime = newTime,
                        snappedDuration = newDuration
                    )
                }
            }
        }
    }

    val onDragEnd: () -> Unit = {
        drag?.let { d ->
            when (d.mode) {
                DragMode.MOVE -> {
                    if (d.isOverDrawer) {
                        if (!d.fromDrawer) {
                            viewModel.updateTaskTime(d.task.id, null, null)
                        }
                    } else if (d.snappedDate != null && d.snappedTime != null) {
                        viewModel.updateTaskTime(d.task.id, d.snappedDate, d.snappedTime)
                    } else if (!d.fromDrawer && d.originDate != null && d.originTime != null) {
                        viewModel.updateTaskTime(d.task.id, d.originDate, d.originTime)
                    }
                }
                DragMode.RESIZE_BOTTOM -> {
                    viewModel.updateTaskDuration(d.task.id, d.snappedDuration)
                }
                DragMode.RESIZE_TOP -> {
                    viewModel.updateTaskTimeAndDuration(
                        d.task.id,
                        d.snappedDate ?: d.originDate,
                        d.snappedTime ?: d.originTime,
                        d.snappedDuration
                    )
                }
            }
        }
        drag = null
    }

    // ── Drawer animation (GPU-accelerated via graphicsLayer) ────────────────
    val drawerSlidePx = with(density) { (DRAWER_WIDTH + 20.dp).toPx() }
    val drawerProgress by animateFloatAsState(
        targetValue = if (isDrawerOpen) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "drawer"
    )
    val scrimAlpha = drawerProgress * 0.32f

    val unscheduledCount = remember(tasks) { tasks.count { it.date == null } }

    CompositionLocalProvider(LocalTextSelectionColors provides AppTextSelectionColors) {
        Box(
            Modifier
                .fillMaxSize()
                .background(BgSurface)
                .onGloballyPositioned { rootCoords = it }
        ) {
            Scaffold(
                topBar = {
                    CalendarTopBar(
                        weekStart        = weekStart,
                        unscheduledCount = unscheduledCount,
                        isDrawerOpen     = isDrawerOpen,
                        onPrev           = { animateToWeek(weekStart.minusWeeks(1), -1) },
                        onNext           = { animateToWeek(weekStart.plusWeeks(1), 1) },
                        onToday          = {
                            val todayMonday = LocalDate.now().with(DayOfWeek.MONDAY)
                            if (todayMonday != weekStart) {
                                val dir = if (todayMonday.isAfter(weekStart)) 1 else -1
                                animateToWeek(todayMonday, dir)
                            }
                        },
                        onDrawer         = viewModel::toggleDrawer
                    )
                },
                containerColor = BgSurface
            ) { padding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    CalendarWeeklyView(
                        tasks             = tasks,
                        weekStart         = weekStart,
                        weekSlideOffsetPx = weekSlideOffsetPx.value,
                        vertScroll        = vertScroll,
                        selectedTaskId    = selectedTaskId,
                        dragState         = drag,
                        onViewportBounds  = { viewportBounds = it },
                        onEmptyTap        = { viewModel.selectTask(null) },
                        onTaskTap         = { task ->
                            viewModel.selectTask(null)
                            activeSheetTask = task
                        },
                        onTaskHold        = { task ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.selectTask(task.id)
                        },
                        onSwipeWeek       = { dir ->
                            animateToWeek(weekStart.plusWeeks(dir.toLong()), dir)
                        },
                        onMoveStart       = { task, pos, grabY -> onMoveStart(task, pos, grabY, false) },
                        onResizeStart     = onResizeStart,
                        onDragDelta       = onDragDelta,
                        onDragEnd         = onDragEnd
                    )

                    // Scrim when drawer is open
                    if (scrimAlpha > 0.005f) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = scrimAlpha))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    dismissKeyboard()
                                    viewModel.closeDrawer()
                                }
                        )
                    }

                    // Right-side Taskbar / Drawer
                    TaskDrawerPanel(
                        tasks          = tasks,
                        dragState      = drag,
                        onCloseDrawer  = {
                            dismissKeyboard()
                            viewModel.closeDrawer()
                        },
                        onAddTaskModal = {
                            dismissKeyboard()
                            activeSheetTask = Task(title = "", color = PALETTE.first())
                        },
                        onQuickAddTask = { title ->
                            viewModel.addTask(title = title, color = PALETTE.random())
                        },
                        onDeleteTask   = viewModel::deleteTask,
                        onTaskTap      = {
                            dismissKeyboard()
                            activeSheetTask = it
                        },
                        onToggle       = viewModel::toggleTaskComplete,
                        onItemBounds   = { id, rect -> itemBoundsMap[id] = rect },
                        onMoveStart    = { task, pos ->
                            dismissKeyboard()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val halfChipPx = with(density) { (HOUR_HEIGHT * task.durationMinutes / 120f).toPx() }
                            onMoveStart(task, pos, halfChipPx, true)
                        },
                        onDragDelta    = onDragDelta,
                        onDragEnd      = onDragEnd,
                        modifier       = Modifier
                            .align(Alignment.CenterEnd)
                            .graphicsLayer {
                                translationX = (1f - drawerProgress) * drawerSlidePx
                            }
                    )
                }
            }

            // ── Floating drag card ONLY when dragging inside the right Taskbar drawer ──
            drag?.let { d ->
                if (d.mode == DragMode.MOVE && (d.isOverDrawer || d.snappedDate == null)) {
                    val lp = try { rootCoords?.windowToLocal(d.windowPos) ?: d.windowPos } catch (_: Exception) { d.windowPos }
                    val chipW = with(density) { 176.dp.toPx() }
                    val chipH = with(density) { 44.dp.toPx() }
                    val evtColor = Color(d.task.color)

                    Box(
                        Modifier
                            .zIndex(50f)
                            .offset {
                                IntOffset(
                                    (lp.x - chipW / 2f).roundToInt(),
                                    (lp.y - chipH / 2f).roundToInt()
                                )
                            }
                            .size(176.dp, 44.dp)
                            .shadow(12.dp, RoundedCornerShape(10.dp))
                            .background(CardWhite, RoundedCornerShape(10.dp))
                            .border(1.5.dp, evtColor, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(evtColor, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                d.task.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Minimal, Clean, Unified Task Properties & Edit Bottom Sheet ─
            activeSheetTask?.let { sheetTask ->
                val existing = tasks.find { it.id == sheetTask.id }
                val isNew = existing == null
                val currentTask = existing ?: sheetTask

                MinimalTaskSheet(
                    task       = currentTask,
                    isNew      = isNew,
                    onDismiss  = { activeSheetTask = null },
                    onSave     = { title, desc, color, dur, completed ->
                        if (isNew) {
                            viewModel.addTask(title, desc, color, dur)
                        } else {
                            viewModel.updateTask(currentTask.id, title, desc, color, dur)
                            if (completed != currentTask.isCompleted) {
                                viewModel.toggleTaskComplete(currentTask.id)
                            }
                        }
                        activeSheetTask = null
                    },
                    onUnschedule = {
                        viewModel.updateTaskTime(currentTask.id, null, null)
                        activeSheetTask = null
                    },
                    onDelete   = {
                        viewModel.deleteTask(currentTask.id)
                        activeSheetTask = null
                    }
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Top Bar (Clean & Minimal, zero heavy icon dependencies)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTopBar(
    weekStart: LocalDate,
    unscheduledCount: Int,
    isDrawerOpen: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onDrawer: () -> Unit
) {
    val weekEnd = weekStart.plusDays(6)
    val monthText = remember(weekStart) {
        if (weekStart.month == weekEnd.month) {
            weekStart.format(FMT_MONTH_YEAR)
        } else {
            "${weekStart.format(FMT_MMM)} – ${weekEnd.format(FMT_MMM_YEAR)}"
        }
    }

    Surface(color = CardWhite) {
        Column {
            TopAppBar(
                title = {
                    Text(
                        text = monthText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(
                        onClick = onPrev,
                        modifier = Modifier.size(34.dp)
                    ) {
                        ChevronIcon(isLeft = true, tint = TextSecondary)
                    }
                    TextButton(
                        onClick = onToday,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = BrandLight,
                            contentColor = BrandPrimary
                        )
                    ) {
                        Text("Today", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(34.dp)
                    ) {
                        ChevronIcon(isLeft = false, tint = TextSecondary)
                    }

                    Spacer(Modifier.width(4.dp))

                    BadgedBox(
                        badge = {
                            if (unscheduledCount > 0) {
                                Badge(
                                    containerColor = BrandPrimary,
                                    contentColor = Color.White
                                ) {
                                    Text(unscheduledCount.toString(), fontSize = 10.sp)
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        FilledIconToggleButton(
                            checked = isDrawerOpen,
                            onCheckedChange = { onDrawer() },
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = IconButtonDefaults.filledIconToggleButtonColors(
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = TextPrimary,
                                checkedContainerColor = BrandPrimary,
                                checkedContentColor = Color.White
                            )
                        ) {
                            ChecklistIcon(
                                tint = if (isDrawerOpen) Color.White else TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardWhite)
            )
            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Zero-Subcomposition Weekly View
// - Single verticalScroll shared by BOTH the time gutter and the 7-day grid
// - All 23 hour labels are drawn directly on Canvas using cached TextLayoutResults
// - Zero BoxWithConstraints, zero HorizontalPager overhead
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
internal fun CalendarWeeklyView(
    tasks: List<Task>,
    weekStart: LocalDate,
    weekSlideOffsetPx: Float,
    vertScroll: ScrollState,
    selectedTaskId: String?,
    dragState: DragState?,
    onViewportBounds: (Rect) -> Unit,
    onEmptyTap: () -> Unit,
    onTaskTap: (Task) -> Unit,
    onTaskHold: (Task) -> Unit,
    onSwipeWeek: (Int) -> Unit,
    onMoveStart: (Task, Offset, Float) -> Unit,
    onResizeStart: (Task, Offset, DragMode) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val dayWidth = remember(screenWidthDp) {
        ((screenWidthDp - TIME_COL_WIDTH) / 7f).coerceAtLeast(36.dp)
    }

    var lastViewportRect by remember { mutableStateOf<Rect?>(null) }
    val weekEnd = remember(weekStart) { weekStart.plusDays(6) }

    // Pre-measure the 23 hour labels ONCE so the grid has ZERO Text composables!
    val textMeasurer = rememberTextMeasurer()
    val hourTextStyle = remember {
        TextStyle(
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted
        )
    }
    val measuredHourLabels = remember(textMeasurer) {
        Array(24) { h ->
            if (h == 0) null else textMeasurer.measure(HOUR_LABELS[h], hourTextStyle)
        }
    }

    // Keep the currently dragged task in weekTasks even when weekStart changes to next/prev week
    // so its CalendarEventChip (and its active pointerInput gesture!) never unmounts mid-drag!
    val weekTasks = remember(tasks, weekStart, dragState?.task?.id) {
        tasks.filter { task ->
            val d = task.date
            val isInThisWeek = d != null && !d.isBefore(weekStart) && !d.isAfter(weekEnd)
            val isBeingDraggedFromCalendar = dragState?.task?.id == task.id && d != null
            isInThisWeek || isBeingDraggedFromCalendar
        }
    }

    var horizSwipeAccum by remember { mutableFloatStateOf(0f) }

    Column(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                val b = it.boundsInWindow()
                if (b != lastViewportRect) {
                    lastViewportRect = b
                    onViewportBounds(b)
                }
            }
            .pointerInput(weekStart, dragState == null) {
                if (dragState != null) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { horizSwipeAccum = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        horizSwipeAccum += dragAmount
                    },
                    onDragEnd = {
                        if (horizSwipeAccum > 90f) onSwipeWeek(-1)
                        else if (horizSwipeAccum < -90f) onSwipeWeek(1)
                        horizSwipeAccum = 0f
                    }
                )
            }
    ) {
        // ── 7-Day Header Row ────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .height(DAY_HEADER_HEIGHT)
                .background(CardWhite)
                .drawBehind {
                    drawLine(BorderSubtle, Offset(0f, size.height), Offset(size.width, size.height), 1.5f)
                }
        ) {
            Box(
                Modifier
                    .width(TIME_COL_WIDTH)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "GMT",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = weekSlideOffsetPx }
            ) {
                for (i in 0..6) {
                    val date = weekStart.plusDays(i.toLong())
                    val isToday = date == today

                    Column(
                        Modifier
                            .width(dayWidth)
                            .fillMaxHeight()
                            .background(if (isToday) BrandLight.copy(alpha = 0.4f) else Color.Transparent)
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = date.dayOfWeek.name.take(3),
                            style = TextStyle(
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isToday) BrandPrimary else TextSecondary,
                                textAlign = TextAlign.Center,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                        Spacer(Modifier.height(3.dp))
                        Box(
                            Modifier
                                .size(28.dp)
                                .background(
                                    if (isToday) BrandPrimary else Color.Transparent,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    lineHeight = 13.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isToday) Color.White else TextPrimary,
                                    textAlign = TextAlign.Center,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.Both
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }

        // ── Single Unified Vertical Scroll Body (Time Gutter + 7-Day Grid together!) ──
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(vertScroll)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(HOUR_HEIGHT * 24)
                    .background(CardWhite)
            ) {
                // Left Time Gutter drawn with a SINGLE Canvas (0 Text composables!)
                Canvas(
                    Modifier
                        .width(TIME_COL_WIDTH)
                        .fillMaxHeight()
                ) {
                    val dh = size.height / 24f
                    val endPad = 5.dp.toPx()
                    for (h in 1..23) {
                        val layoutResult = measuredHourLabels[h] ?: continue
                        val x = size.width - layoutResult.size.width - endPad
                        val y = h * dh - layoutResult.size.height / 2f
                        drawText(layoutResult, topLeft = Offset(x, y))
                    }
                }

                // Right 7-Day Grid + Scheduled Events
                Box(
                    Modifier
                        .width(dayWidth * 7)
                        .fillMaxHeight()
                        .graphicsLayer { translationX = weekSlideOffsetPx }
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onEmptyTap() })
                        }
                ) {
                    val todayIdx = ChronoUnit.DAYS.between(weekStart, today).toInt()
                    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(5f, 5f)) }

                    Canvas(Modifier.fillMaxSize()) {
                        val dh = size.height / 24f
                        val dw = size.width / 7f

                        if (todayIdx in 0..6) {
                            drawRect(
                                color = BrandLight.copy(alpha = 0.22f),
                                topLeft = Offset(todayIdx * dw, 0f),
                                size = androidx.compose.ui.geometry.Size(dw, size.height)
                            )
                        }

                        drawLine(GridLineColor, Offset(0f, 0f), Offset(0f, size.height), 1.5f)

                        for (i in 0..23) {
                            if (i > 0) {
                                drawLine(GridLineColor, Offset(0f, i * dh), Offset(size.width, i * dh), 1.2f)
                            }
                            val y = (i + 0.5f) * dh
                            drawLine(
                                GridLineColor.copy(alpha = 0.65f),
                                Offset(0f, y),
                                Offset(size.width, y),
                                1f,
                                pathEffect = dashEffect
                            )
                        }
                        for (i in 1..6) {
                            drawLine(GridLineColor, Offset(i * dw, 0f), Offset(i * dw, size.height), 1.2f)
                        }
                    }

                    // ── Scheduled Events ────────────────────────────────────
                    weekTasks.forEach { task ->
                        key(task.id) {
                            val isThisDragging = dragState?.task?.id == task.id
                            val isMovingThis = isThisDragging && dragState.mode == DragMode.MOVE
                            val showHandles = (selectedTaskId == task.id && !isThisDragging) ||
                                (isThisDragging && (dragState.mode == DragMode.RESIZE_TOP || dragState.mode == DragMode.RESIZE_BOTTOM))

                            val displayDate = if (isThisDragging && (dragState?.mode == DragMode.RESIZE_TOP || dragState?.mode == DragMode.RESIZE_BOTTOM)) {
                                dragState?.snappedDate ?: task.date
                            } else {
                                task.date
                            }

                            val displayTime = if (isThisDragging && dragState?.mode == DragMode.RESIZE_TOP) {
                                dragState?.snappedTime ?: task.time
                            } else {
                                task.time
                            }

                            if (displayDate != null && displayTime != null) {
                                val rawDayOff = ChronoUnit.DAYS.between(weekStart, displayDate).toInt()
                                if (rawDayOff in 0..6 || isMovingThis) {
                                    val displayDur = if (isThisDragging && (dragState?.mode == DragMode.RESIZE_TOP || dragState?.mode == DragMode.RESIZE_BOTTOM)) {
                                        dragState?.snappedDuration ?: task.durationMinutes
                                    } else {
                                        task.durationMinutes
                                    }

                                    val dayOff = rawDayOff.coerceIn(0, 6)
                                    val yFrac = displayTime.hour + displayTime.minute / 60f
                                    val yDp   = HOUR_HEIGHT * yFrac
                                    val hDp   = (HOUR_HEIGHT * displayDur / 60f).coerceAtLeast(20.dp)

                                    CalendarEventChip(
                                        task           = task.copy(date = displayDate, time = displayTime, durationMinutes = displayDur),
                                        widthDp        = dayWidth - 3.dp,
                                        heightDp       = hDp,
                                        showHandles    = showHandles,
                                        isBeingMoved   = isMovingThis,
                                        isBeingResized = isThisDragging && (dragState?.mode == DragMode.RESIZE_TOP || dragState?.mode == DragMode.RESIZE_BOTTOM),
                                        modifier       = Modifier
                                            .alpha(if (isMovingThis) 0f else 1f)
                                            .zIndex(if (isThisDragging) 30f else if (showHandles) 15f else 1f)
                                            .absoluteOffset(x = dayWidth * dayOff + 1.5.dp, y = yDp),
                                        onTap          = { onTaskTap(task) },
                                        onHold         = { onTaskHold(task) },
                                        onMoveStart    = { winPos, grabY -> onMoveStart(task, winPos, grabY) },
                                        onResizeStart  = { winPos, mode -> onResizeStart(task, winPos, mode) },
                                        onDragDelta    = onDragDelta,
                                        onDragEnd      = onDragEnd
                                    )
                                }
                            }
                        }
                    }

                    // ── Active Moving Task Chip on Target Day/Time ──────────
                    dragState?.let { d ->
                        if (d.mode == DragMode.MOVE && !d.isOverDrawer && d.snappedDate != null && d.snappedTime != null) {
                            val dayOff = ChronoUnit.DAYS.between(weekStart, d.snappedDate).toInt()
                            if (dayOff in 0..6) {
                                val yFrac = d.snappedTime.hour + d.snappedTime.minute / 60f
                                val yDp   = HOUR_HEIGHT * yFrac
                                val hDp   = (HOUR_HEIGHT * d.snappedDuration / 60f).coerceAtLeast(20.dp)
                                val evtColor = Color(d.task.color)
                                val endTime = d.snappedTime.plusMinutes(d.snappedDuration.toLong())

                                Box(
                                    Modifier
                                        .zIndex(40f)
                                        .absoluteOffset(x = dayWidth * dayOff + 1.5.dp, y = yDp)
                                        .size(dayWidth - 3.dp, hDp)
                                        .shadow(8.dp, RoundedCornerShape(6.dp))
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CardWhite)
                                        .background(evtColor.copy(alpha = 0.25f))
                                        .border(2.dp, evtColor, RoundedCornerShape(6.dp))
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxHeight()
                                            .width(3.dp)
                                            .background(evtColor)
                                    )
                                    Column(
                                        Modifier
                                            .fillMaxSize()
                                            .padding(start = 5.dp, top = 2.dp, end = 2.dp, bottom = 2.dp)
                                    ) {
                                        Text(
                                            text = d.task.title,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = if (hDp > 40.dp) 2 else 1,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 11.5.sp
                                        )
                                        if (hDp > 28.dp) {
                                            Text(
                                                text = "${d.snappedTime.format(FMT_TIME_SHORT)}–${endTime.format(FMT_TIME_SHORT)}",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = evtColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Current-time red line ───────────────────────────────
                    if (todayIdx in 0..6) {
                        val now = LocalTime.now()
                        val yFrac = now.hour + now.minute / 60f
                        val yDp   = HOUR_HEIGHT * yFrac
                        Box(
                            Modifier
                                .zIndex(14f)
                                .absoluteOffset(x = dayWidth * todayIdx - 3.5.dp, y = yDp - 4.dp)
                                .size(8.dp)
                                .background(DangerRed, CircleShape)
                                .border(1.dp, Color.White, CircleShape)
                        )
                        Box(
                            Modifier
                                .zIndex(13f)
                                .absoluteOffset(x = dayWidth * todayIdx, y = yDp - 1.dp)
                                .width(dayWidth)
                                .height(2.dp)
                                .background(DangerRed)
                        )
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Calendar Event Chip
// Unified 300ms Gesture Detector:
// - Single tap (< 300ms): opens Task Properties Sheet
// - Hold for 300ms (even with 0 movement!): immediately shows Top & Bottom Resize Handles!
// - Dragging after 300ms hold: smoothly moves the task across days & weeks
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
private fun CalendarEventChip(
    task: Task,
    widthDp: Dp,
    heightDp: Dp,
    showHandles: Boolean,
    isBeingMoved: Boolean,
    isBeingResized: Boolean,
    modifier: Modifier,
    onTap: () -> Unit,
    onHold: () -> Unit,
    onMoveStart: (Offset, Float) -> Unit,
    onResizeStart: (Offset, DragMode) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val evtColor = Color(task.color)
    var chipCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val updatedOnTap by rememberUpdatedState(onTap)
    val updatedOnHold by rememberUpdatedState(onHold)
    val updatedOnMoveStart by rememberUpdatedState(onMoveStart)
    val updatedOnResizeStart by rememberUpdatedState(onResizeStart)
    val updatedOnDragDelta by rememberUpdatedState(onDragDelta)
    val updatedOnDragEnd by rememberUpdatedState(onDragEnd)

    Box(
        modifier
            .size(widthDp, heightDp)
            .onGloballyPositioned { chipCoords = it }
            .pointerInput(task.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    val touchSlop = viewConfiguration.touchSlop
                    var exceededSlopBeforeHold = false

                    val releasedOrMovedEarly = withTimeoutOrNull(HOLD_DURATION_MS) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                            if (!change.pressed) {
                                change.consume()
                                return@withTimeoutOrNull true
                            }
                            if ((change.position - downPos).getDistance() > touchSlop) {
                                exceededSlopBeforeHold = true
                                return@withTimeoutOrNull false
                            }
                        }
                        @Suppress("UNREACHABLE_CODE")
                        false
                    }

                    if (releasedOrMovedEarly == true) {
                        updatedOnTap()
                    } else if (releasedOrMovedEarly == null && !exceededSlopBeforeHold) {
                        updatedOnHold()

                        val coords = chipCoords?.takeIf { it.isAttached }
                        val startWindowPos = coords?.localToWindow(downPos) ?: downPos
                        var moveStarted = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            val delta = change.positionChange()
                            if (!moveStarted) {
                                if ((change.position - downPos).getDistance() > 2f) {
                                    moveStarted = true
                                    updatedOnMoveStart(startWindowPos, downPos.y)
                                    val initialDelta = change.position - downPos
                                    change.consume()
                                    updatedOnDragDelta(initialDelta)
                                }
                            } else {
                                if (delta != Offset.Zero) {
                                    change.consume()
                                    updatedOnDragDelta(delta)
                                }
                            }
                        }

                        if (moveStarted) {
                            updatedOnDragEnd()
                        }
                    }
                }
            }
    ) {
        val isHighlighted = showHandles || isBeingMoved || isBeingResized

        Box(
            Modifier
                .fillMaxSize()
                .shadow(
                    elevation = if (isBeingMoved) 8.dp else if (isHighlighted) 4.dp else 0.dp,
                    shape = RoundedCornerShape(6.dp)
                )
                .clip(RoundedCornerShape(6.dp))
                .background(CardWhite)
                .background(evtColor.copy(alpha = if (isHighlighted) 0.24f else 0.15f))
                .border(
                    BorderStroke(
                        width = if (isHighlighted) 1.8.dp else 1.dp,
                        color = if (isHighlighted) evtColor else evtColor.copy(alpha = 0.4f)
                    ),
                    RoundedCornerShape(6.dp)
                )
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(evtColor)
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(start = 5.dp, top = 2.dp, end = 2.dp, bottom = 2.dp)
            ) {
                Text(
                    text = task.title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = if (heightDp > 40.dp) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 11.5.sp,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )
                if (heightDp > 28.dp && task.time != null) {
                    val endTime = task.time.plusMinutes(task.durationMinutes.toLong())
                    Text(
                        text = if (isBeingMoved || isBeingResized) {
                            "${task.time.format(FMT_TIME_SHORT)}–${endTime.format(FMT_TIME_SHORT)}"
                        } else {
                            task.time.format(FMT_TIME_AMPM)
                        },
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = evtColor,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }

        // ── Top & Bottom Resize Handles (Shown after 300ms hold!) ───────────
        if (showHandles) {
            var lastTopWinPos by remember { mutableStateOf(Offset.Zero) }
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp)
                    .fillMaxWidth()
                    .height(RESIZE_HANDLE_ZONE)
                    .zIndex(25f)
                    .pointerInput(task.id, "top_handle") {
                        detectDragGestures(
                            onDragStart = { local ->
                                val coords = chipCoords?.takeIf { it.isAttached }
                                val winPos = coords?.localToWindow(local) ?: local
                                lastTopWinPos = winPos
                                updatedOnResizeStart(winPos, DragMode.RESIZE_TOP)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val coords = chipCoords?.takeIf { it.isAttached }
                                if (coords != null) {
                                    val curWin = coords.localToWindow(change.position)
                                    val delta = curWin - lastTopWinPos
                                    lastTopWinPos = curWin
                                    updatedOnDragDelta(delta)
                                }
                            },
                            onDragEnd = { updatedOnDragEnd() },
                            onDragCancel = { updatedOnDragEnd() }
                        )
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    Modifier
                        .size(22.dp, 10.dp)
                        .shadow(2.dp, CircleShape)
                        .background(evtColor, CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(8.dp, 1.5.dp).background(Color.White, RoundedCornerShape(1.dp)))
                }
            }

            var lastBotWinPos by remember { mutableStateOf(Offset.Zero) }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 8.dp)
                    .fillMaxWidth()
                    .height(RESIZE_HANDLE_ZONE)
                    .zIndex(25f)
                    .pointerInput(task.id, "bottom_handle") {
                        detectDragGestures(
                            onDragStart = { local ->
                                val coords = chipCoords?.takeIf { it.isAttached }
                                val winPos = coords?.localToWindow(Offset(local.x, coords.size.height.toFloat() + local.y)) ?: local
                                lastBotWinPos = winPos
                                updatedOnResizeStart(winPos, DragMode.RESIZE_BOTTOM)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val coords = chipCoords?.takeIf { it.isAttached }
                                if (coords != null) {
                                    val curWin = coords.localToWindow(Offset(change.position.x, coords.size.height.toFloat() + change.position.y))
                                    val delta = curWin - lastBotWinPos
                                    lastBotWinPos = curWin
                                    updatedOnDragDelta(delta)
                                }
                            },
                            onDragEnd = { updatedOnDragEnd() },
                            onDragCancel = { updatedOnDragEnd() }
                        )
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    Modifier
                        .size(22.dp, 10.dp)
                        .shadow(2.dp, CircleShape)
                        .background(evtColor, CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(8.dp, 1.5.dp).background(Color.White, RoundedCornerShape(1.dp)))
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Right-Side Taskbar Drawer (Zero Subcomposition: Column + verticalScroll)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
internal fun TaskDrawerPanel(
    tasks: List<Task>,
    dragState: DragState?,
    onCloseDrawer: () -> Unit,
    onAddTaskModal: () -> Unit,
    onQuickAddTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onTaskTap: (Task) -> Unit,
    onToggle: (String) -> Unit,
    onItemBounds: (String, Rect) -> Unit,
    onMoveStart: (Task, Offset) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var quickAddText by remember { mutableStateOf("") }
    val unscheduled  = remember(tasks) { tasks.filter { it.date == null } }
    val drawerScroll = rememberScrollState()

    Surface(
        modifier = modifier
            .width(DRAWER_WIDTH)
            .fillMaxHeight(),
        color = CardWhite,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── Minimal Header ──────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tasks",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onAddTaskModal,
                    modifier = Modifier.size(32.dp)
                ) {
                    PlusIcon(tint = BrandPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onCloseDrawer,
                    modifier = Modifier.size(32.dp)
                ) {
                    CloseIcon(tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }

            // ── Minimal Quick-Add Input ─────────────────────────────────────
            val quickAddTextStyle = TextStyle(
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = TextPrimary,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .height(44.dp)
                    .background(BgSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = quickAddText,
                        onValueChange = { quickAddText = it },
                        singleLine = true,
                        textStyle = quickAddTextStyle,
                        cursorBrush = SolidColor(BrandPrimary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (quickAddText.isNotBlank()) {
                                    onQuickAddTask(quickAddText.trim())
                                    quickAddText = ""
                                }
                            }
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (quickAddText.isEmpty()) {
                                    Text(
                                        text = "Add a task…",
                                        style = quickAddTextStyle.copy(color = TextMuted)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (quickAddText.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(24.dp)
                                .background(BrandPrimary, CircleShape)
                                .clickable {
                                    onQuickAddTask(quickAddText.trim())
                                    quickAddText = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            PlusIcon(tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = BorderSubtle)

            // ── Task List (Direct Column + verticalScroll -> zero LazyColumn subcomposition lag) ──
            if (unscheduled.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No unscheduled tasks",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(drawerScroll)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    unscheduled.forEach { task ->
                        key(task.id) {
                            DrawerTaskCard(
                                task        = task,
                                isDragging  = dragState?.task?.id == task.id,
                                onTap       = { onTaskTap(task) },
                                onDelete    = { onDeleteTask(task.id) },
                                onToggle    = { onToggle(task.id) },
                                onBounds    = { rect -> onItemBounds(task.id, rect) },
                                onMoveStart = onMoveStart,
                                onDragDelta = onDragDelta,
                                onDragEnd   = onDragEnd
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerTaskCard(
    task: Task,
    isDragging: Boolean,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    onBounds: (Rect) -> Unit,
    onMoveStart: (Task, Offset) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val taskColor = Color(task.color)
    var cardCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val updatedTask by rememberUpdatedState(task)
    val updatedOnMoveStart by rememberUpdatedState(onMoveStart)
    val updatedOnDragDelta by rememberUpdatedState(onDragDelta)
    val updatedOnDragEnd by rememberUpdatedState(onDragEnd)

    var lastCardBounds by remember { mutableStateOf<Rect?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isDragging) 0.3f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .background(BgSurface, RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (isDragging) taskColor else BorderSubtle,
                shape = RoundedCornerShape(12.dp)
            )
            .onGloballyPositioned {
                cardCoords = it
                val b = it.boundsInWindow()
                if (b != lastCardBounds) {
                    lastCardBounds = b
                    onBounds(b)
                }
            }
            .pointerInput(task.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { local ->
                        val coords = cardCoords?.takeIf { it.isAttached }
                        val winPos = coords?.localToWindow(local) ?: local
                        updatedOnMoveStart(updatedTask, winPos)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        updatedOnDragDelta(amount)
                    },
                    onDragEnd = { updatedOnDragEnd() },
                    onDragCancel = { updatedOnDragEnd() }
                )
            }
            .pointerInput(task.id) {
                detectTapGestures(onTap = { onTap() })
            }
            .padding(start = 10.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Complete checkbox
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(1.8.dp, taskColor, CircleShape)
                .background(if (task.isCompleted) taskColor else Color.Transparent, CircleShape)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (task.isCompleted) {
                CheckIcon(tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }

        Spacer(Modifier.width(10.dp))

        // Task Title & subtle duration
        Column(Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontSize = 13.5.sp,
                color = if (task.isCompleted) TextMuted else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${task.durationMinutes}m",
                fontSize = 10.5.sp,
                color = TextSecondary
            )
        }

        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(26.dp)
        ) {
            CloseIcon(tint = TextMuted, modifier = Modifier.size(12.dp))
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Minimal, Unified Task Properties & Edit Bottom Sheet
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinimalTaskSheet(
    task: Task,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, color: Long, duration: Int, isCompleted: Boolean) -> Unit,
    onUnschedule: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title       by remember(task.id) { mutableStateOf(task.title) }
    var desc        by remember(task.id) { mutableStateOf(task.description) }
    var color       by remember(task.id) { mutableLongStateOf(task.color) }
    var duration    by remember(task.id) { mutableIntStateOf(task.durationMinutes) }
    var isCompleted by remember(task.id) { mutableStateOf(task.isCompleted) }

    val activeColor = Color(color)
    val sheetSelectionColors = remember(color) {
        TextSelectionColors(
            handleColor = activeColor,
            backgroundColor = activeColor.copy(alpha = 0.32f)
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isNew && title.isNotBlank()) {
                onSave(title.trim(), desc.trim(), color, duration, isCompleted)
            } else {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                Box(Modifier.size(36.dp, 4.dp).background(BorderSubtle, RoundedCornerShape(2.dp)))
            }
        }
    ) {
        CompositionLocalProvider(LocalTextSelectionColors provides sheetSelectionColors) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                val titleTextStyle = TextStyle(
                    fontSize = 19.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isNew) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .border(2.dp, activeColor, CircleShape)
                                .background(if (isCompleted) activeColor else Color.Transparent, CircleShape)
                                .clickable { isCompleted = !isCompleted },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                CheckIcon(tint = Color.White, modifier = Modifier.size(13.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                    }

                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        textStyle = titleTextStyle,
                        cursorBrush = SolidColor(activeColor),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (title.isEmpty()) {
                                    Text(
                                        text = "Task title",
                                        style = titleTextStyle.copy(color = TextMuted)
                                    )
                                }
                                inner()
                            }
                        }
                    )

                    if (!isNew) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(34.dp)
                        ) {
                            TrashIcon(tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // ── Schedule Pill (if scheduled on calendar) ────────────────
                if (task.date != null && task.time != null) {
                    Spacer(Modifier.height(10.dp))
                    val endTime = task.time.plusMinutes(duration.toLong())
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(BgSurface, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "${task.date.format(FMT_DAY_DATE)} · ${task.time.format(FMT_TIME_AMPM)} – ${endTime.format(FMT_TIME_AMPM)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "Unschedule",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = activeColor,
                            modifier = Modifier.clickable { onUnschedule() }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Minimal Notes Input ─────────────────────────────────────
                val notesStyle = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    color = TextPrimary,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
                BasicTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    maxLines = 3,
                    textStyle = notesStyle,
                    cursorBrush = SolidColor(activeColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgSurface, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    decorationBox = { inner ->
                        Box(
                            contentAlignment = Alignment.TopStart,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (desc.isEmpty()) {
                                Text("Add notes…", style = notesStyle.copy(color = TextMuted))
                            }
                            inner()
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))

                // ── Compact Duration Pills ──────────────────────────────────
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15, 30, 45, 60, 90, 120).forEach { mins ->
                        val selected = duration == mins
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) activeColor else BgSurface)
                                .clickable { duration = mins }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (mins >= 60 && mins % 60 == 0) "${mins / 60}h" else "${mins}m",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) Color.White else TextSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Color Dots + Save Button Row ────────────────────────────
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        PALETTE.forEach { c ->
                            val selected = c == color
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(c), CircleShape)
                                    .then(
                                        if (selected) Modifier.border(2.dp, TextPrimary, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { color = c }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val finalTitle = title.trim().ifEmpty { "New Task" }
                            onSave(finalTitle, desc.trim(), color, duration, isCompleted)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            if (isNew) "Add" else "Save",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Lightweight Canvas Icons (Replaces 30MB material-icons-extended library!)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
private fun ChevronIcon(isLeft: Boolean, tint: Color, modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = androidx.compose.ui.graphics.Path().apply {
            if (isLeft) {
                moveTo(w * 0.62f, h * 0.22f)
                lineTo(w * 0.34f, h * 0.5f)
                lineTo(w * 0.62f, h * 0.78f)
            } else {
                moveTo(w * 0.38f, h * 0.22f)
                lineTo(w * 0.66f, h * 0.5f)
                lineTo(w * 0.38f, h * 0.78f)
            }
        }
        drawPath(path, color = tint, style = stroke)
    }
}

@Composable
private fun ChecklistIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Top checkmark
        val checkPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.15f, h * 0.32f)
            lineTo(w * 0.25f, h * 0.42f)
            lineTo(w * 0.42f, h * 0.22f)
        }
        drawPath(checkPath, color = tint, style = stroke)
        // Lines
        drawLine(tint, Offset(w * 0.54f, h * 0.32f), Offset(w * 0.86f, h * 0.32f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(tint, radius = 1.3.dp.toPx(), center = Offset(w * 0.26f, h * 0.68f))
        drawLine(tint, Offset(w * 0.54f, h * 0.68f), Offset(w * 0.86f, h * 0.68f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
private fun PlusIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sw = 2.dp.toPx()
        drawLine(tint, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.82f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.18f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
private fun CloseIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.8.dp.toPx()
        drawLine(tint, Offset(w * 0.22f, h * 0.22f), Offset(w * 0.78f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.22f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
private fun CheckIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.20f, h * 0.52f)
            lineTo(w * 0.42f, h * 0.74f)
            lineTo(w * 0.80f, h * 0.28f)
        }
        drawPath(path, color = tint, style = stroke)
    }
}

@Composable
private fun TrashIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.6.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Lid
        drawLine(tint, Offset(w * 0.20f, h * 0.26f), Offset(w * 0.80f, h * 0.26f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.38f, h * 0.16f), Offset(w * 0.62f, h * 0.16f), strokeWidth = sw, cap = StrokeCap.Round)
        // Body
        val body = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.26f, h * 0.26f)
            lineTo(w * 0.30f, h * 0.84f)
            lineTo(w * 0.70f, h * 0.84f)
            lineTo(w * 0.74f, h * 0.26f)
        }
        drawPath(body, color = tint, style = stroke)
    }
}
