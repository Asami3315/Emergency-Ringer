package com.emergencyringer.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
    // Animation phase: 0=idle, 1=fade-in, 2=scale+rotate, 3=scale-back, 4=reveal
    var phase by remember { mutableIntStateOf(0) }

    // --- Animatables ---
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.4f) }
    val logoRotation = remember { Animatable(0f) }
    val bgReveal = remember { Animatable(0f) }      // 0 = black, 1 = gradient
    val whiteLogo = remember { Animatable(0f) }      // alpha for white logo
    val textAlpha = remember { Animatable(0f) }
    val textSlide = remember { Animatable(30f) }     // slide up from 30dp

    // Drive the animation sequence
    LaunchedEffect(Unit) {
        // Phase 1: Fade in purple logo (small)
        phase = 1
        logoAlpha.animateTo(1f, tween(500, easing = EaseOutCubic))

        // Phase 2: Scale up + rotate 45°
        delay(100)
        phase = 2
        coroutineScope {
            launch { logoScale.animateTo(1.8f, tween(700, easing = EaseInOutCubic)) }
            launch { logoRotation.animateTo(45f, tween(700, easing = EaseInOutCubic)) }
        }

        // Phase 3: Scale back down + rotate back to 0°
        delay(100)
        phase = 3
        coroutineScope {
            launch { logoScale.animateTo(0.8f, tween(600, easing = EaseInOutCubic)) }
            launch { logoRotation.animateTo(0f, tween(600, easing = EaseInOutCubic)) }
        }

        // Phase 4: Reveal gradient + swap to white logo + show text
        delay(200)
        phase = 4

        // Fade out purple logo while fading in background
        coroutineScope {
            launch { logoAlpha.animateTo(0f, tween(400, easing = EaseInCubic)) }
            launch { bgReveal.animateTo(1f, tween(600, easing = EaseOutCubic)) }
        }

        // Scale white logo to final size
        coroutineScope {
            launch {
                logoScale.snapTo(0.5f)
                logoScale.animateTo(1f, tween(500, easing = EaseOutBack))
            }
            launch { whiteLogo.animateTo(1f, tween(500, easing = EaseOutCubic)) }
        }

        // "Hello" text fades in + slides up
        coroutineScope {
            launch { textAlpha.animateTo(1f, tween(500, easing = EaseOutCubic)) }
            launch { textSlide.animateTo(0f, tween(500, easing = EaseOutCubic)) }
        }

        // Hold for a moment, then navigate
        delay(800)
        onFinished()
    }

    // --- UI ---
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Black background layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .alpha(1f - bgReveal.value)
        )

        // Gradient background layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF7C3AED),
                            Color(0xFFD946EF)
                        )
                    )
                )
                .alpha(bgReveal.value)
        )

        // Purple logo (phases 1–3)
        Image(
            painter = painterResource(R.drawable.logo_pp),
            contentDescription = "Logo",
            modifier = Modifier
                .size(120.dp)
                .scale(logoScale.value)
                .rotate(logoRotation.value)
                .alpha(logoAlpha.value)
        )

        // White logo + text (phase 4)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(whiteLogo.value)
        ) {
            Image(
                painter = painterResource(R.drawable.logo_s),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(80.dp)
                    .scale(logoScale.value)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Hello",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = textSlide.value.dp)
            )
        }
    }
}
