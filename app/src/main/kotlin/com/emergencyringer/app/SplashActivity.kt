package com.emergencyringer.app

import android.content.Intent
import android.os.Bundle
import android.media.MediaPlayer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashScreen {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current

    // ── Phase state: logo → text → exit ──────────────────────
    var phase by remember { mutableStateOf<SplashPhase>(SplashPhase.Logo) }

    // ── Animatables ──────────────────────────────────────────
    val logoAlpha   = remember { Animatable(0f) }
    val logoScale   = remember { Animatable(0.3f) }
    val logoRotate  = remember { Animatable(-45f) }
    val ringAlpha1  = remember { Animatable(0f) }
    val ringScale1  = remember { Animatable(0.5f) }
    val ringAlpha2  = remember { Animatable(0f) }
    val ringScale2  = remember { Animatable(0.3f) }
    val glowAlpha   = remember { Animatable(0f) }
    val glowScale   = remember { Animatable(0.5f) }
    val textAlpha   = remember { Animatable(0f) }
    val textOffset  = remember { Animatable(20f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val dotsAlpha   = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    // Infinite float for logo
    val float = rememberInfiniteTransition(label = "float")
    val floatY by float.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    // Dot pulse (3 dots)
    val dotPulse = rememberInfiniteTransition(label = "dots")
    val dot0 by dotPulse.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "d0"
    )
    val dot1 by dotPulse.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, 150, EaseInOutCubic), RepeatMode.Reverse),
        label = "d1"
    )
    val dot2 by dotPulse.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, 300, EaseInOutCubic), RepeatMode.Reverse),
        label = "d2"
    )

    // ── Animation sequence ────────────────────────────────────
    LaunchedEffect(Unit) {
        // Play bell sound
        try {
            val mediaPlayer = MediaPlayer.create(context, R.raw.splash_bell)
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Rings + glow appear
        launch {
            ringAlpha1.animateTo(0.08f, tween(1500, easing = EaseOut))
            ringScale1.animateTo(1f,    tween(1500, easing = EaseOut))
        }
        launch {
            delay(200)
            ringAlpha2.animateTo(0.05f, tween(1800, easing = EaseOut))
            ringScale2.animateTo(1f,    tween(1800, easing = EaseOut))
        }
        launch {
            delay(100)
            glowAlpha.animateTo(0.3f, tween(1200, easing = EaseOut))
            glowScale.animateTo(1f,   tween(1200, easing = EaseOut))
        }

        // Logo springs in (scale 0.3 → 1, rotate -45 → 0)
        launch {
            logoAlpha.animateTo(1f, tween(300))
        }
        launch {
            logoScale.animateTo(
                1f,
                tween(800, easing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f))
            )
        }
        launch {
            logoRotate.animateTo(0f, tween(800, easing = EaseOutCubic))
        }

        // At 800ms → show text phase
        delay(800)
        phase = SplashPhase.Text
        launch { textAlpha.animateTo(1f, tween(500, easing = EaseOut)) }
        launch { textOffset.animateTo(0f, tween(500, easing = EaseOut)) }
        launch {
            delay(200)
            subtitleAlpha.animateTo(1f, tween(400, easing = EaseOut))
        }
        launch {
            delay(500)
            dotsAlpha.animateTo(1f, tween(300))
        }

        // At 2600ms → exit phase (fade out whole screen)
        delay(1800)
        phase = SplashPhase.Exit
        screenAlpha.animateTo(0f, tween(500, easing = EaseInOutCubic))

        // At 3200ms → navigate
        delay(100)
        onFinished()
    }

    // ── UI ────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFB703),
                        Color(0xFFFFC733),
                        Color(0xFFFF9500)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // ── Decorative rings (Canvas circles) ─────────────────
        Canvas(
            modifier = Modifier
                .size(500.dp)
                .scale(ringScale1.value)
                .alpha(ringAlpha1.value)
        ) {
            drawCircle(
                color = Color.White,
                radius = size.minDimension / 2f,
                style = Stroke(width = 60.dp.toPx())
            )
        }
        Canvas(
            modifier = Modifier
                .size(750.dp)
                .scale(ringScale2.value)
                .alpha(ringAlpha2.value)
        ) {
            drawCircle(
                color = Color.White,
                radius = size.minDimension / 2f,
                style = Stroke(width = 40.dp.toPx())
            )
        }

        // ── White radial glow behind logo ─────────────────────
        Canvas(
            modifier = Modifier
                .size(192.dp)
                .scale(glowScale.value)
                .alpha(glowAlpha.value)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent)
                )
            )
        }

        // ── Centre content: logo + text ───────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Floating logo
            Image(
                painter = painterResource(R.drawable.logo_s),
                contentDescription = "Emergency Ringer Logo",
                modifier = Modifier
                    .size(128.dp)
                    .offset(y = floatY.dp)
                    .scale(logoScale.value)
                    .rotate(logoRotate.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(Modifier.height(24.dp))

            // App name — fades in at phase Text
            Text(
                text = "Emergency Ringer",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = textOffset.value.dp)
            )

            Spacer(Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "Always reachable when it matters",
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.3.sp,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
        }

        // ── Pulsing dots at the bottom ────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .alpha(dotsAlpha.value),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(dot0, dot1, dot2).forEach { pulse ->
                Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(Color.White.copy(alpha = pulse * 0.6f + 0.4f))
                }
            }
        }
    }
}

private enum class SplashPhase { Logo, Text, Exit }
