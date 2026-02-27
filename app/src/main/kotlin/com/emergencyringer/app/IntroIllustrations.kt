package com.emergencyringer.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Colors ───────────────────────────────────────────────────
private val Amber      = Color(0xFFFFB703)
private val AmberLight = Color(0xFFFFF3D6)
private val Skin       = Color(0xFFFFDBA4)
private val SkinDark   = Color(0xFFF0C890)
private val Hair       = Color(0xFF3D2B1F)
private val DarkGray   = Color(0xFF2D2D2D)
private val ShirtBlue  = Color(0xFF3D5A80)
private val GreenLight = Color(0xFF4ADE80)
private val GreenAccent = Color(0xFFB8E6C8)
private val RedAccent  = Color(0xFFFF6B6B)
private val Purple     = Color(0xFFE0C5F5)
private val PurpleDark = Color(0xFFD4C4F0)
private val ScreenBg   = Color(0xFFE8F4FD)

/**
 * Screen 1 Illustration: Person holding phone with incoming call + shield badge + bell
 * Matches PhonePersonIllustration from IntroIllustrations.tsx
 */
@Composable
fun PhonePersonIllustration() {
    val inf = rememberInfiniteTransition(label = "person")

    // Breathing body
    val breathe by inf.animateFloat(
        initialValue = 1f, targetValue = 1.008f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    // Head bob
    val headTilt by inf.animateFloat(
        initialValue = -2f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "headTilt"
    )
    // Shield badge bob
    val shieldBob by inf.animateFloat(
        initialValue = -4f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shieldBob"
    )
    // Bell bob
    val bellBob by inf.animateFloat(
        initialValue = 3f, targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bellBob"
    )
    // Sound wave pulse
    val wavePulse by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "wave"
    )
    // Blink
    val blink by inf.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4000
                1f at 0; 1f at 1500; 0.1f at 1600; 1f at 1700; 1f at 3400; 0.1f at 3500; 1f at 3600
            }, RepeatMode.Restart
        ), label = "blink"
    )
    // Arm sway
    val armSway by inf.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "armSway"
    )
    // Screen glow
    val screenGlow by inf.animateFloat(
        initialValue = 0.05f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val scale = w / 360f

            // Background blob
            drawOval(
                color = AmberLight.copy(alpha = 0.6f),
                topLeft = Offset((180 - 140) * scale, (185 - 135) * scale),
                size = Size(280 * scale, 270 * scale)
            )

            // Floor shadow
            drawOval(
                color = Color(0xFFE8DFD0).copy(alpha = 0.5f),
                topLeft = Offset((180 - 100) * scale, (320 - 10) * scale),
                size = Size(200 * scale, 20 * scale)
            )

            // Bean bag chair
            drawOval(
                color = Amber.copy(alpha = 0.15f),
                topLeft = Offset(110 * scale, 215 * scale),
                size = Size(90 * scale, 75 * scale)
            )

            // === LEGS ===
            // Left leg
            drawLine(ShirtBlue, Offset(140 * scale, 270 * scale), Offset(125 * scale, 310 * scale), 10 * scale, StrokeCap.Round)
            // Left shoe
            drawCircle(DarkGray, 5 * scale, Offset(122 * scale, 312 * scale))
            // Right leg
            drawLine(ShirtBlue, Offset(170 * scale, 268 * scale), Offset(200 * scale, 308 * scale), 10 * scale, StrokeCap.Round)
            // Right shoe
            drawCircle(DarkGray, 5 * scale, Offset(203 * scale, 310 * scale))

            // === BODY (breathing) ===
            scale(scaleX = 1f, scaleY = breathe, pivot = Offset(155 * scale, 230 * scale)) {
                // Torso
                drawRoundRect(
                    color = Amber,
                    topLeft = Offset(130 * scale, 190 * scale),
                    size = Size(50 * scale, 80 * scale),
                    cornerRadius = CornerRadius(12 * scale)
                )
                // Collar line
                drawLine(Color(0xFFE5A003), Offset(140 * scale, 192 * scale), Offset(170 * scale, 192 * scale), 2 * scale, StrokeCap.Round)
                // Center line
                drawLine(Color(0xFFE5A003).copy(alpha = 0.5f), Offset(155 * scale, 195 * scale), Offset(155 * scale, 265 * scale), 1.5f * scale)
            }

            // === LEFT ARM (waving) ===
            rotate(armSway, pivot = Offset(135 * scale, 205 * scale)) {
                drawLine(Skin, Offset(135 * scale, 205 * scale), Offset(118 * scale, 252 * scale), 13 * scale, StrokeCap.Round)
                drawLine(Amber, Offset(135 * scale, 200 * scale), Offset(122 * scale, 228 * scale), 15 * scale, StrokeCap.Round)
                // Hand
                drawCircle(Skin, 7 * scale, Offset(120 * scale, 256 * scale))
            }

            // === RIGHT ARM + PHONE ===
            rotate(armSway * -0.5f, pivot = Offset(175 * scale, 200 * scale)) {
                drawLine(Skin, Offset(175 * scale, 200 * scale), Offset(210 * scale, 140 * scale), 13 * scale, StrokeCap.Round)
                drawLine(Amber, Offset(175 * scale, 200 * scale), Offset(196 * scale, 178 * scale), 15 * scale, StrokeCap.Round)
                // Hand
                drawCircle(Skin, 8 * scale, Offset(210 * scale, 138 * scale))

                // Phone
                drawRoundRect(DarkGray, Offset(196 * scale, 100 * scale), Size(32 * scale, 58 * scale), CornerRadius(7 * scale))
                drawRoundRect(ScreenBg, Offset(199 * scale, 105 * scale), Size(26 * scale, 46 * scale), CornerRadius(4 * scale))

                // Caller avatar
                drawCircle(GreenAccent.copy(alpha = 0.5f), 8 * scale, Offset(212 * scale, 118 * scale))
                drawCircle(Amber, 5 * scale, Offset(212 * scale, 118 * scale))
                drawCircle(Color.White, 2.5f * scale, Offset(212 * scale, 116 * scale))

                // Name lines
                drawRoundRect(Color(0xFFCCCCCC), Offset(204 * scale, 128 * scale), Size(16 * scale, 2 * scale), CornerRadius(1 * scale))
                drawRoundRect(Color(0xFFDDDDDD), Offset(207 * scale, 132 * scale), Size(10 * scale, 1.5f * scale), CornerRadius(0.75f * scale))

                // Accept/Decline buttons
                drawCircle(RedAccent, 4 * scale, Offset(206 * scale, 142 * scale))
                drawCircle(GreenLight, 4 * scale, Offset(218 * scale, 142 * scale))

                // Screen glow
                drawRoundRect(
                    Color(0xFF4A90D9).copy(alpha = screenGlow),
                    Offset(199 * scale, 105 * scale), Size(26 * scale, 46 * scale), CornerRadius(4 * scale)
                )
            }

            // === SOUND WAVES ===
            for (i in 0..2) {
                val waveAlpha = ((wavePulse + i * 0.25f) % 1f).let { if (it < 0.5f) it * 2 else (1 - it) * 2 }
                val waveX = 234 * scale + (i * 8 + wavePulse * 8) * scale
                drawArc(
                    color = Amber.copy(alpha = waveAlpha * (1f - i * 0.3f)),
                    startAngle = -30f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(waveX, 108 * scale),
                    size = Size(20 * scale, 25 * scale),
                    style = Stroke((2.5f - i * 0.5f) * scale, cap = StrokeCap.Round)
                )
            }

            // === HEAD ===
            rotate(headTilt, pivot = Offset(155 * scale, 185 * scale)) {
                // Neck
                drawRect(Skin, Offset(148 * scale, 185 * scale), Size(14 * scale, 10 * scale))
                // Head
                drawOval(Skin, Offset((155 - 28) * scale, (165 - 30) * scale), Size(56 * scale, 60 * scale))

                // Hair
                drawOval(Hair, Offset((155 - 30) * scale, (155 - 38) * scale), Size(60 * scale, 50 * scale))
                drawOval(Hair, Offset((155 - 28) * scale, (150 - 35) * scale), Size(56 * scale, 40 * scale))

                // Ears
                drawOval(SkinDark, Offset(122 * scale, 158 * scale), Size(10 * scale, 14 * scale))
                drawOval(SkinDark, Offset(178 * scale, 158 * scale), Size(10 * scale, 14 * scale))

                // Eyes (with blink)
                drawOval(Color.White, Offset(141 * scale, (164 - 4.5f) * scale), Size(8 * scale, (9 * blink) * scale))
                drawOval(Color.White, Offset(161 * scale, (164 - 4.5f) * scale), Size(8 * scale, (9 * blink) * scale))
                if (blink > 0.3f) {
                    drawCircle(DarkGray, 2.5f * scale, Offset(147 * scale, 164 * scale))
                    drawCircle(DarkGray, 2.5f * scale, Offset(167 * scale, 164 * scale))
                    // Eye shine
                    drawCircle(Color.White, 1 * scale, Offset(148 * scale, 163 * scale))
                    drawCircle(Color.White, 1 * scale, Offset(168 * scale, 163 * scale))
                }

                // Eyebrows
                drawLine(Hair, Offset(139 * scale, 156 * scale), Offset(152 * scale, 156 * scale), 2.5f * scale, StrokeCap.Round)
                drawLine(Hair, Offset(158 * scale, 156 * scale), Offset(171 * scale, 156 * scale), 2.5f * scale, StrokeCap.Round)

                // Nose
                drawArc(
                    Color(0xFFE8B888), -45f, 90f, false,
                    Offset(151 * scale, 168 * scale), Size(8 * scale, 6 * scale),
                    style = Stroke(1.5f * scale, cap = StrokeCap.Round)
                )

                // Mouth (smile)
                drawArc(
                    DarkGray, 0f, 180f, false,
                    Offset(146 * scale, 175 * scale), Size(18 * scale, 10 * scale),
                    style = Stroke(2.5f * scale, cap = StrokeCap.Round)
                )

                // Blush
                drawOval(Color(0xFFFFCBA4).copy(alpha = 0.45f), Offset(132 * scale, 170 * scale), Size(12 * scale, 7 * scale))
                drawOval(Color(0xFFFFCBA4).copy(alpha = 0.45f), Offset(166 * scale, 170 * scale), Size(12 * scale, 7 * scale))
            }

            // === SHIELD BADGE (floating) ===
            translate(top = shieldBob * scale) {
                drawCircle(Color.White, 22 * scale, Offset(72 * scale, 130 * scale))
                // Shield shape
                val shieldPath = Path().apply {
                    moveTo(72 * scale, 118 * scale)
                    lineTo(82 * scale, 126 * scale)
                    lineTo(82 * scale, 140 * scale)
                    cubicTo(82 * scale, 146 * scale, 72 * scale, 152 * scale, 72 * scale, 152 * scale)
                    cubicTo(72 * scale, 152 * scale, 62 * scale, 146 * scale, 62 * scale, 140 * scale)
                    lineTo(62 * scale, 126 * scale)
                    close()
                }
                drawPath(shieldPath, GreenLight)
                // Checkmark
                drawLine(Color.White, Offset(67 * scale, 134 * scale), Offset(70 * scale, 137 * scale), 2.5f * scale, StrokeCap.Round)
                drawLine(Color.White, Offset(70 * scale, 137 * scale), Offset(78 * scale, 128 * scale), 2.5f * scale, StrokeCap.Round)
            }

            // === BELL NOTIFICATION (floating) ===
            translate(top = bellBob * scale) {
                drawCircle(Color.White, 20 * scale, Offset(280 * scale, 180 * scale))
                // Bell
                val bellPath = Path().apply {
                    moveTo(272 * scale, 183 * scale)
                    cubicTo(272 * scale, 175 * scale, 276 * scale, 170 * scale, 280 * scale, 168 * scale)
                    cubicTo(284 * scale, 170 * scale, 288 * scale, 175 * scale, 288 * scale, 183 * scale)
                    close()
                }
                drawPath(bellPath, Amber)
                drawRoundRect(Amber, Offset(271 * scale, 183 * scale), Size(18 * scale, 2.5f * scale), CornerRadius(1.25f * scale))
                drawCircle(Amber, 2.5f * scale, Offset(280 * scale, 188 * scale))
            }

            // === FLOATING decorative dots ===
            drawCircle(Purple.copy(alpha = 0.6f), 5 * scale, Offset(60 * scale, (220 + shieldBob * 0.5f) * scale))
            drawCircle(GreenAccent.copy(alpha = 0.5f), 4 * scale, Offset(290 * scale, (250 - bellBob * 0.3f) * scale))
            drawCircle(Amber.copy(alpha = 0.35f), 3.5f * scale, Offset(95 * scale, (90 + shieldBob * 0.3f) * scale))
        }
    }
}


/**
 * Screen 2 Illustration: Globe with orbiting people + central shield
 * Matches ProtectedCircleIllustration from IntroIllustrations.tsx
 */
@Composable
fun ProtectedCircleIllustration() {
    val inf = rememberInfiniteTransition(label = "circle")

    // Orbit rotation
    val orbitAngle by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit"
    )
    // Shield pulse
    val shieldPulse by inf.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shieldPulse"
    )
    // Pulse ring expand
    val ringPulse by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ringPulse"
    )
    // Heart beat
    val heartBeat by inf.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 1200
                1f at 0; 1.15f at 200; 1f at 400; 1.15f at 600; 1f at 800
            }, RepeatMode.Restart
        ), label = "heartBeat"
    )
    // People bob
    val peopleBob by inf.animateFloat(
        initialValue = -2f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "peopleBob"
    )
    // Blink
    val blink by inf.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3500
                1f at 0; 1f at 1500; 0.1f at 1600; 1f at 1700; 1f at 3000; 0.1f at 3100; 1f at 3200
            }, RepeatMode.Restart
        ), label = "blink2"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val w = size.width
            val h = size.height
            val s = w / 360f
            val cx = 180 * s
            val cy = 175 * s

            // Background blob
            drawOval(Color(0xFFF0E8FF).copy(alpha = 0.5f), Offset((180 - 145) * s, (180 - 140) * s), Size(290 * s, 280 * s))

            // Central globe
            drawCircle(Color.White.copy(alpha = 0.6f), 65 * s, Offset(cx, cy))
            drawCircle(PurpleDark, 65 * s, Offset(cx, cy), style = Stroke(1.5f * s))
            // Globe grid lines
            drawOval(PurpleDark.copy(alpha = 0.4f), Offset((180 - 65) * s, (175 - 30) * s), Size(130 * s, 60 * s), style = Stroke(1 * s))
            drawOval(PurpleDark.copy(alpha = 0.4f), Offset((180 - 30) * s, (175 - 65) * s), Size(60 * s, 130 * s), style = Stroke(1 * s))
            drawLine(PurpleDark.copy(alpha = 0.3f), Offset(115 * s, cy), Offset(245 * s, cy), 1 * s)
            drawLine(PurpleDark.copy(alpha = 0.3f), Offset(cx, 110 * s), Offset(cx, 240 * s), 1 * s)

            // Orbit ring (rotating dashed)
            rotate(orbitAngle, pivot = Offset(cx, cy)) {
                drawCircle(PurpleDark.copy(alpha = 0.5f), 85 * s, Offset(cx, cy), style = Stroke(1.5f * s))
                drawCircle(Amber, 4 * s, Offset((180 + 85) * s, cy))
            }

            // === PERSON 1 - TOP (Woman, professional) ===
            translate(top = peopleBob * s) {
                val px = 180 * s
                val py = 68 * s
                drawCircle(Color.White, 35 * s, Offset(px, py))
                drawCircle(Purple, 35 * s, Offset(px, py), style = Stroke(1.5f * s))
                // Body
                drawOval(Purple, Offset((180 - 13) * s, 78 * s), Size(26 * s, 12 * s))
                // Head
                drawCircle(Skin, 14 * s, Offset(px, 60 * s))
                // Hair
                drawOval(DarkGray, Offset((180 - 15) * s, (55 - 18) * s), Size(30 * s, 28 * s))
                // Eyes
                if (blink > 0.3f) {
                    drawCircle(DarkGray, 1.8f * s, Offset(175 * s, 59 * s))
                    drawCircle(DarkGray, 1.8f * s, Offset(185 * s, 59 * s))
                }
                // Smile
                drawArc(DarkGray, 0f, 180f, false, Offset(176 * s, 63 * s), Size(8 * s, 5 * s), style = Stroke(1.5f * s, cap = StrokeCap.Round))
                // Phone icon
                drawRoundRect(Amber.copy(alpha = 0.6f), Offset(195 * s, 50 * s), Size(8 * s, 13 * s), CornerRadius(2 * s))
                // Connection line
                drawLine(Purple.copy(alpha = 0.5f), Offset(px, 103 * s), Offset(px, 130 * s), 2 * s, StrokeCap.Round)
            }

            // === PERSON 2 - RIGHT (Doctor) ===
            translate(top = peopleBob * -0.7f * s) {
                val px = 268 * s
                val py = 145 * s
                drawCircle(Color.White, 32 * s, Offset(px, py))
                drawCircle(GreenAccent, 32 * s, Offset(px, py), style = Stroke(1.5f * s))
                // Body (white coat)
                drawOval(Color.White, Offset((268 - 12) * s, 155 * s), Size(24 * s, 11 * s))
                // Red cross
                drawRoundRect(RedAccent, Offset(265.5f * s, 157 * s), Size(5 * s, 1.5f * s), CornerRadius(0.75f * s))
                drawRoundRect(RedAccent, Offset(267.25f * s, 155.25f * s), Size(1.5f * s, 5 * s), CornerRadius(0.75f * s))
                // Head
                drawCircle(Color(0xFFC68642), 13 * s, Offset(px, 138 * s))
                // Hair
                drawOval(Color(0xFF1A1A1A), Offset((268 - 14) * s, (134 - 12) * s), Size(28 * s, 20 * s))
                // Glasses
                drawCircle(Color(0xFF666666), 4.5f * s, Offset(263 * s, 137 * s), style = Stroke(1.2f * s))
                drawCircle(Color(0xFF666666), 4.5f * s, Offset(273 * s, 137 * s), style = Stroke(1.2f * s))
                drawLine(Color(0xFF666666), Offset(267.5f * s, 137 * s), Offset(268.5f * s, 137 * s), 1 * s)
                // Eyes
                if (blink > 0.3f) {
                    drawCircle(DarkGray, 1.5f * s, Offset(263 * s, 137 * s))
                    drawCircle(DarkGray, 1.5f * s, Offset(273 * s, 137 * s))
                }
                // Smile
                drawArc(Color(0xFF1A1A1A), 0f, 180f, false, Offset(264 * s, 142 * s), Size(8 * s, 4 * s), style = Stroke(1.3f * s, cap = StrokeCap.Round))
                // Connection line
                drawLine(GreenAccent.copy(alpha = 0.5f), Offset(236 * s, 155 * s), Offset(215 * s, 168 * s), 2 * s, StrokeCap.Round)
            }

            // === PERSON 3 - BOTTOM RIGHT (Friend with headphones) ===
            translate(top = peopleBob * 0.8f * s) {
                val px = 248 * s
                val py = 268 * s
                drawCircle(Color.White, 32 * s, Offset(px, py))
                drawCircle(Amber, 32 * s, Offset(px, py), style = Stroke(1.5f * s))
                // Body (orange shirt)
                drawOval(Color(0xFFFF8C42), Offset((248 - 12) * s, 278 * s), Size(24 * s, 11 * s))
                // Head
                drawCircle(Skin, 13 * s, Offset(px, 260 * s))
                // Curly hair
                drawOval(Color(0xFFD4854A), Offset((248 - 14) * s, (256 - 14) * s), Size(28 * s, 22 * s))
                drawCircle(Color(0xFFD4854A), 3 * s, Offset(237 * s, 252 * s))
                drawCircle(Color(0xFFD4854A), 3 * s, Offset(259 * s, 252 * s))
                // Happy closed eyes (arcs)
                drawArc(DarkGray, 0f, -180f, false, Offset(243 * s, 257 * s), Size(4 * s, 3 * s), style = Stroke(1.8f * s, cap = StrokeCap.Round))
                drawArc(DarkGray, 0f, -180f, false, Offset(249 * s, 257 * s), Size(4 * s, 3 * s), style = Stroke(1.8f * s, cap = StrokeCap.Round))
                // Smile
                drawArc(DarkGray, 0f, 180f, false, Offset(243 * s, 263 * s), Size(10 * s, 6 * s), style = Stroke(1.5f * s, cap = StrokeCap.Round))
                // Headphones
                drawArc(Color(0xFF555555), 180f, 180f, false, Offset(235 * s, 242 * s), Size(26 * s, 15 * s), style = Stroke(2.5f * s, cap = StrokeCap.Round))
                drawCircle(Color(0xFF555555), 3.5f * s, Offset(235 * s, 257 * s))
                drawCircle(Color(0xFF555555), 3.5f * s, Offset(261 * s, 257 * s))
                // Connection line
                drawLine(Amber.copy(alpha = 0.5f), Offset(222 * s, 250 * s), Offset(205 * s, 215 * s), 2 * s, StrokeCap.Round)
            }

            // === PERSON 4 - BOTTOM LEFT (Dad) ===
            translate(top = peopleBob * -0.5f * s) {
                val px = 112 * s
                val py = 268 * s
                drawCircle(Color.White, 32 * s, Offset(px, py))
                drawCircle(Amber, 32 * s, Offset(px, py), style = Stroke(1.5f * s))
                // Body (blue shirt)
                drawOval(ShirtBlue, Offset((112 - 12) * s, 278 * s), Size(24 * s, 11 * s))
                // Head
                drawCircle(Skin, 13 * s, Offset(px, 260 * s))
                // Gray hair
                drawOval(Color(0xFF888888), Offset((112 - 14) * s, (256 - 14) * s), Size(28 * s, 20 * s))
                // Eyes
                if (blink > 0.3f) {
                    drawCircle(DarkGray, 1.8f * s, Offset(108 * s, 259 * s))
                    drawCircle(DarkGray, 1.8f * s, Offset(116 * s, 259 * s))
                }
                // Smile
                drawArc(DarkGray, 0f, 180f, false, Offset(108 * s, 263 * s), Size(8 * s, 5 * s), style = Stroke(1.3f * s, cap = StrokeCap.Round))
                // Waving hand
                drawLine(Skin, Offset(126 * s, 278 * s), Offset(134 * s, 268 * s), 4 * s, StrokeCap.Round)
                drawCircle(Skin, 3 * s, Offset(134 * s, 267 * s))
                // Connection line
                drawLine(Amber.copy(alpha = 0.5f), Offset(138 * s, 250 * s), Offset(155 * s, 215 * s), 2 * s, StrokeCap.Round)
            }

            // === PERSON 5 - LEFT (Mom/Sister) ===
            translate(top = peopleBob * 0.6f * s) {
                val px = 92 * s
                val py = 145 * s
                drawCircle(Color.White, 32 * s, Offset(px, py))
                drawCircle(Purple, 32 * s, Offset(px, py), style = Stroke(1.5f * s))
                // Body (purple)
                drawOval(Purple, Offset((92 - 12) * s, 155 * s), Size(24 * s, 11 * s))
                // Head
                drawCircle(Color(0xFF8D5524), 13 * s, Offset(px, 138 * s))
                // Dark hair
                drawOval(Color(0xFF1A1A1A), Offset((92 - 14) * s, (134 - 14) * s), Size(28 * s, 22 * s))
                // Long hair sides
                drawLine(Color(0xFF1A1A1A), Offset(79 * s, 134 * s), Offset(80 * s, 158 * s), 4 * s, StrokeCap.Round)
                drawLine(Color(0xFF1A1A1A), Offset(105 * s, 134 * s), Offset(104 * s, 158 * s), 4 * s, StrokeCap.Round)
                // Eyes
                if (blink > 0.3f) {
                    drawCircle(DarkGray, 1.8f * s, Offset(88 * s, 137 * s))
                    drawCircle(DarkGray, 1.8f * s, Offset(96 * s, 137 * s))
                }
                // Smile
                drawArc(Color(0xFF1A1A1A), 0f, 180f, false, Offset(88 * s, 141 * s), Size(8 * s, 5 * s), style = Stroke(1.3f * s, cap = StrokeCap.Round))
                // Blush
                drawOval(Color(0xFFD4854A).copy(alpha = 0.3f), Offset(80 * s, 139 * s), Size(7 * s, 4 * s))
                drawOval(Color(0xFFD4854A).copy(alpha = 0.3f), Offset(97 * s, 139 * s), Size(7 * s, 4 * s))
                // Connection line
                drawLine(Purple.copy(alpha = 0.5f), Offset(120 * s, 155 * s), Offset(145 * s, 168 * s), 2 * s, StrokeCap.Round)
            }

            // === HEART (beating) near left person ===
            scale(heartBeat, heartBeat, pivot = Offset(79 * s, 130 * s)) {
                val heartPath = Path().apply {
                    moveTo(74 * s, 128 * s)
                    cubicTo(74 * s, 125 * s, 77 * s, 123 * s, 79 * s, 125 * s)
                    cubicTo(81 * s, 123 * s, 84 * s, 125 * s, 84 * s, 128 * s)
                    cubicTo(84 * s, 132 * s, 79 * s, 135 * s, 79 * s, 135 * s)
                    cubicTo(79 * s, 135 * s, 74 * s, 132 * s, 74 * s, 128 * s)
                    close()
                }
                drawPath(heartPath, RedAccent)
            }

            // === CENTRAL SHIELD (pulsing) ===
            scale(shieldPulse, shieldPulse, pivot = Offset(cx, 178 * s)) {
                val shieldPath = Path().apply {
                    moveTo(180 * s, 148 * s)
                    lineTo(198 * s, 160 * s)
                    lineTo(198 * s, 188 * s)
                    cubicTo(198 * s, 198 * s, 180 * s, 208 * s, 180 * s, 208 * s)
                    cubicTo(180 * s, 208 * s, 162 * s, 198 * s, 162 * s, 188 * s)
                    lineTo(162 * s, 160 * s)
                    close()
                }
                drawPath(shieldPath, Amber)
                // Checkmark
                drawLine(Color.White, Offset(172 * s, 178 * s), Offset(178 * s, 184 * s), 3.5f * s, StrokeCap.Round)
                drawLine(Color.White, Offset(178 * s, 184 * s), Offset(190 * s, 170 * s), 3.5f * s, StrokeCap.Round)
            }

            // === PULSE RINGS ===
            val ringR = 30 * s * (1 + ringPulse * 0.8f)
            drawCircle(Amber.copy(alpha = 0.4f * (1 - ringPulse)), ringR, Offset(cx, 178 * s), style = Stroke(1.5f * s))
            val ringR2 = 30 * s * (1 + ((ringPulse + 0.35f) % 1f) * 0.8f)
            drawCircle(Amber.copy(alpha = 0.3f * (1 - (ringPulse + 0.35f) % 1f)), ringR2, Offset(cx, 178 * s), style = Stroke(1 * s))

            // === FLOATING SPARKLES ===
            val sparkle1 = shieldPulse
            drawCircle(Amber.copy(alpha = 0.35f), 3 * s, Offset(305 * s, (80 + peopleBob) * s))
            drawCircle(Purple.copy(alpha = 0.3f), 3 * s, Offset(50 * s, (310 - peopleBob) * s))
            drawCircle(GreenAccent.copy(alpha = 0.25f), 2.5f * s, Offset(310 * s, (300 + peopleBob * 0.5f) * s))
        }
    }
}
