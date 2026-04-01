package com.emergencyringer.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ── Colors from TSX ──────────────────────────────────────────
private val IntroBg       = Color(0xFFF5F3EF)
private val IntroPrimary  = Color(0xFFFFB703)
private val IntroText     = Color(0xFF2D2D2D)
private val IntroMuted    = Color(0xFF8A8A8A)
private val IntroDotInactive = Color(0xFFD9D5CE)

// Screen 1 colors (amber)
private val Screen1GradTop    = Color(0xFFFFF8E7)
private val Screen1GradBottom = Color(0xFFFFF1CC)
private val Screen1Accent     = Color(0xFFFFB703)

// Screen 2 colors (purple)
private val Screen2GradTop    = Color(0xFFF5F0FF)
private val Screen2GradBottom = Color(0xFFEDE4FF)
private val Screen2Accent     = Color(0xFFE0C5F5)

// Screen 3 colors (green/teal)
private val Screen3GradTop    = Color(0xFFE8FFF0)
private val Screen3GradBottom = Color(0xFFD0F5E0)
private val Screen3Accent     = Color(0xFF7DDBA3)

private data class IntroPage(
    val drawableRes: Int,
    val accentColor: Color,
    val gradTop: Color,
    val gradBottom: Color,
    val title: String,
    val description: String,
    val imageSize: Float = 0.85f
)

private val pages = listOf(
    IntroPage(
        drawableRes = R.drawable.intro_cell_phone,
        accentColor = Screen1Accent,
        gradTop = Screen1GradTop,
        gradBottom = Screen1GradBottom,
        title = "Never Miss an Emergency",
        description = "Emergency Ringer ensures critical calls from your trusted contacts always ring loud — even when your phone is on silent or Do Not Disturb.",
        imageSize = 0.95f
    ),
    IntroPage(
        drawableRes = R.drawable.intro_adult_talking,
        accentColor = Screen2Accent,
        gradTop = Screen2GradTop,
        gradBottom = Screen2GradBottom,
        title = "Your Circle, Protected",
        description = "Add your emergency contacts — family, doctors, close friends — and customize ringtone, volume, and alerts. Stay connected when it truly matters."
    ),
    IntroPage(
        drawableRes = R.drawable.intro_always_guard,
        accentColor = Screen3Accent,
        gradTop = Screen3GradTop,
        gradBottom = Screen3GradBottom,
        title = "Always on Guard",
        description = "Emergency Ringer runs quietly in the background. Simply grant silent-mode access, and we’ll handle the rest — keeping you safe 24/7 without draining your battery.",
        imageSize = 0.90f
    )
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun IntroScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IntroBg)
    ) {
        // ── Pager ────────────────────────────────────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val page = pages[pageIndex]
            IntroPageContent(page = page)
        }

        // ── Bottom controls (always visible) ─────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, IntroBg, IntroBg)
                    )
                )
                .padding(horizontal = 32.dp)
                .padding(top = 24.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                pages.forEachIndexed { index, _ ->
                    val isActive = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isActive) 28.dp else 8.dp,
                        animationSpec = tween(300), label = "dot"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .background(
                                if (isActive) IntroPrimary else IntroDotInactive,
                                CircleShape
                            )
                    )
                }
            }

            // Button
            val isLastScreen = pagerState.currentPage == pages.size - 1
            Button(
                onClick = {
                    if (isLastScreen) {
                        onComplete()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IntroPrimary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    if (isLastScreen) "Get Started" else "Next",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            // "Skip for now" on last screen
            AnimatedVisibility(visible = isLastScreen) {
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        "Skip for now",
                        fontSize = 14.sp,
                        color = IntroMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroPageContent(page: IntroPage) {
    Column(modifier = Modifier.fillMaxSize()) {
        // ── Top illustration area (55%) ──────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.48f)
                .background(
                    Brush.verticalGradient(listOf(page.gradTop, page.gradBottom))
                ),
            contentAlignment = Alignment.Center
        ) {
            // ── Animated background elements ─────────────────
            val inf = rememberInfiniteTransition(label = "bg")

            // Large pulsing circle
            val pulse1 by inf.animateFloat(
                initialValue = 0.6f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "pulse1"
            )
            // Floating offset 1
            val float1 by inf.animateFloat(
                initialValue = -15f, targetValue = 15f,
                animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "float1"
            )
            // Floating offset 2
            val float2 by inf.animateFloat(
                initialValue = 12f, targetValue = -12f,
                animationSpec = infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "float2"
            )
            // Floating offset 3
            val float3 by inf.animateFloat(
                initialValue = -10f, targetValue = 10f,
                animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "float3"
            )
            // Slow rotation
            val rotate1 by inf.animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
                label = "rotate1"
            )
            // Pulse opacity
            val glowAlpha by inf.animateFloat(
                initialValue = 0.08f, targetValue = 0.18f,
                animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "glowAlpha"
            )

            // Big soft circle (top-right area)
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .offset(x = 80.dp, y = (-30 + float1).dp)
                    .scale(pulse1)
                    .alpha(glowAlpha)
                    .background(page.accentColor, CircleShape)
            )

            // Medium circle (bottom-left)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = (-60).dp, y = (100 + float2).dp)
                    .scale(pulse1 * 0.9f)
                    .alpha(glowAlpha * 0.7f)
                    .background(page.accentColor, CircleShape)
            )

            // Small floating dots
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .offset(x = (-40 + float3).dp, y = (-60 + float1).dp)
                    .alpha(0.3f)
                    .background(page.accentColor, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .offset(x = (100 + float2).dp, y = (80 + float3).dp)
                    .alpha(0.25f)
                    .background(page.accentColor, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .offset(x = (60 + float1).dp, y = (-80 + float2).dp)
                    .alpha(0.2f)
                    .background(page.accentColor, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(x = (-80 + float2).dp, y = (30 + float3).dp)
                    .alpha(0.35f)
                    .background(page.accentColor, CircleShape)
            )

            // Rotating ring
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .rotate(rotate1)
                    .alpha(0.06f)
                    .background(Color.Transparent)
                    .then(Modifier)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = page.accentColor,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }

            // Small rotating ring
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .offset(x = (-50).dp, y = (-40).dp)
                    .rotate(-rotate1 * 0.7f)
                    .alpha(0.05f)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = page.accentColor,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // ── PNG illustration (on top) ────────────────────
            val illustrationAlpha = remember { Animatable(0f) }
            val illustrationScale = remember { Animatable(0.85f) }
            LaunchedEffect(Unit) {
                launch {
                    illustrationAlpha.animateTo(1f, tween(600, delayMillis = 150))
                }
                illustrationScale.animateTo(1f, tween(600, delayMillis = 150, easing = FastOutSlowInEasing))
            }

            Image(
                painter = painterResource(id = page.drawableRes),
                contentDescription = page.title,
                modifier = Modifier
                    .fillMaxSize(page.imageSize)
                    .scale(illustrationScale.value)
                    .alpha(illustrationAlpha.value),
                contentScale = ContentScale.Fit
            )

            // ── Animated wave transition at bottom ───────────
            val wavePhase1 by inf.animateFloat(
                initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
                animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
                label = "wave1"
            )
            val wavePhase2 by inf.animateFloat(
                initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
                animationSpec = infiniteRepeatable(tween(5500, easing = LinearEasing), RepeatMode.Restart),
                label = "wave2"
            )
            val wavePhase3 by inf.animateFloat(
                initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
                animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
                label = "wave3"
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
            ) {
                val w = size.width
                val h = size.height
                val bgColor = IntroBg

                // Wave 1 — front wave (most visible)
                val path1 = Path().apply {
                    moveTo(0f, h)
                    for (x in 0..w.toInt() step 2) {
                        val xf = x.toFloat()
                        val y = h * 0.35f + kotlin.math.sin((xf / w * 2 * Math.PI + wavePhase1).toDouble()).toFloat() * h * 0.18f
                        lineTo(xf, y)
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(path1, bgColor.copy(alpha = 0.95f))

                // Wave 2 — middle wave
                val path2 = Path().apply {
                    moveTo(0f, h)
                    for (x in 0..w.toInt() step 2) {
                        val xf = x.toFloat()
                        val y = h * 0.45f + kotlin.math.sin((xf / w * 2.5 * Math.PI + wavePhase2).toDouble()).toFloat() * h * 0.14f
                        lineTo(xf, y)
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(path2, bgColor.copy(alpha = 0.6f))

                // Wave 3 — back wave (subtle)
                val path3 = Path().apply {
                    moveTo(0f, h)
                    for (x in 0..w.toInt() step 2) {
                        val xf = x.toFloat()
                        val y = h * 0.55f + kotlin.math.sin((xf / w * 1.8 * Math.PI + wavePhase3).toDouble()).toFloat() * h * 0.12f
                        lineTo(xf, y)
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(path3, bgColor.copy(alpha = 0.35f))
            }
        }

        // ── Bottom text area (45%) ───────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(IntroBg)
                .padding(horizontal = 32.dp)
                .padding(top = 24.dp, bottom = 170.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val textAlpha = remember { Animatable(0f) }
            val textOffset = remember { Animatable(15f) }
            LaunchedEffect(Unit) {
                launch {
                    textAlpha.animateTo(1f, tween(500, delayMillis = 250))
                }
                textOffset.animateTo(0f, tween(500, delayMillis = 250))
            }

            Text(
                page.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = IntroText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = textOffset.value.dp)
                    .padding(bottom = 12.dp)
            )

            val descAlpha = remember { Animatable(0f) }
            val descOffset = remember { Animatable(15f) }
            LaunchedEffect(Unit) {
                launch {
                    descAlpha.animateTo(1f, tween(500, delayMillis = 350))
                }
                descOffset.animateTo(0f, tween(500, delayMillis = 350))
            }

            Text(
                page.description,
                fontSize = 15.sp,
                color = IntroMuted,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier
                    .alpha(descAlpha.value)
                    .offset(y = descOffset.value.dp)
                    .padding(horizontal = 8.dp)
            )
        }
    }
}
