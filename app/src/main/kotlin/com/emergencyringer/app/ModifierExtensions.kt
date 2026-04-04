package com.emergencyringer.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A reusable modifier that creates a "Magnetic Snapping" or "Cursor Affinity" effect.
 * The component will subtly leaning/pulling towards the user's finger during touch.
 */
@OptIn(androidx.compose.ui.InternalComposeUiApi::class)
fun Modifier.magneticAffinity(
    strength: Float = 0.15f,
    maxOffset: androidx.compose.ui.unit.Dp = 12.dp
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    this
        .onGloballyPositioned { size = it.size }
        .pointerInput(size) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull()
                    
                    if (change != null && change.pressed) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val deltaX = change.position.x - centerX
                        val deltaY = change.position.y - centerY
                        
                        val maxPx = with(density) { maxOffset.toPx() }
                        val targetX = (deltaX * strength).coerceIn(-maxPx, maxPx)
                        val targetY = (deltaY * strength).coerceIn(-maxPx, maxPx)
                        
                        scope.launch {
                            offset.animateTo(Offset(targetX, targetY), spring(stiffness = 700f))
                        }
                    } else {
                        scope.launch {
                            offset.animateTo(
                                Offset.Zero, 
                                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                            )
                        }
                    }
                }
            }
        }
        .graphicsLayer {
            translationX = offset.value.x
            translationY = offset.value.y
        }
}
/**
 * A reusable modifier that creates a "Weighted Spring" or "Elastic" effect.
 * The component scales down on press (foam feel) and pops back with a bounce on release.
 */
fun Modifier.weightedSpring(): Modifier = composed {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val move = event.changes.firstOrNull()
                if (move != null) {
                    if (move.pressed) {
                        scope.launch {
                            scale.animateTo(0.95f, spring(stiffness = 1500f))
                        }
                    } else {
                        scope.launch {
                            // Medium bouncy naturally overshoots to ~1.02
                            scale.animateTo(
                                1f, 
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                    }
                }
            }
        }
    }
    .graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
