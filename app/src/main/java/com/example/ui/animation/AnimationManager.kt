package com.example.ui.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.example.model.TransitionSpeed
import kotlin.math.roundToInt

/**
 * Centralized motion & spring curves tuned for 60/120fps glass transitions
 * without jank on mid-range hardware.
 */
object AnimationManager {

    fun durationMs(baseMs: Int, speed: TransitionSpeed): Int =
        (baseMs * speed.durationMultiplier).roundToInt().coerceIn(80, 650)

    fun <T> glassSpring(speed: TransitionSpeed) = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = when (speed) {
            TransitionSpeed.INSTANT -> Spring.StiffnessHigh
            TransitionSpeed.SMOOTH -> Spring.StiffnessMedium
            TransitionSpeed.CINEMATIC -> Spring.StiffnessMediumLow
        }
    )

    fun drawerEnter(speed: TransitionSpeed): EnterTransition {
        val ms = durationMs(240, speed)
        return fadeIn(tween(ms, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(ms, easing = FastOutSlowInEasing)) { it / 5 } +
            scaleIn(initialScale = 0.96f, animationSpec = tween(ms, easing = FastOutSlowInEasing))
    }

    fun drawerExit(speed: TransitionSpeed): ExitTransition {
        val ms = durationMs(190, speed)
        return fadeOut(tween(ms, easing = FastOutSlowInEasing)) +
            slideOutVertically(tween(ms, easing = FastOutSlowInEasing)) { it / 6 } +
            scaleOut(targetScale = 0.97f, animationSpec = tween(ms, easing = FastOutSlowInEasing))
    }

    fun controlPanelEnter(speed: TransitionSpeed): EnterTransition {
        val ms = durationMs(230, speed)
        return fadeIn(tween(ms, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(ms, easing = FastOutSlowInEasing)) { -it / 5 } +
            scaleIn(initialScale = 0.96f, animationSpec = tween(ms, easing = FastOutSlowInEasing))
    }

    fun controlPanelExit(speed: TransitionSpeed): ExitTransition {
        val ms = durationMs(180, speed)
        return fadeOut(tween(ms, easing = FastOutSlowInEasing)) +
            slideOutVertically(tween(ms, easing = FastOutSlowInEasing)) { -it / 6 } +
            scaleOut(targetScale = 0.97f, animationSpec = tween(ms, easing = FastOutSlowInEasing))
    }

    fun modalEnter(speed: TransitionSpeed): EnterTransition {
        val ms = durationMs(220, speed)
        return fadeIn(tween(ms, easing = FastOutSlowInEasing)) +
            scaleIn(initialScale = 0.93f, animationSpec = tween(ms, easing = FastOutSlowInEasing))
    }

    fun modalExit(speed: TransitionSpeed): ExitTransition {
        val ms = durationMs(170, speed)
        return fadeOut(tween(ms, easing = FastOutSlowInEasing)) +
            scaleOut(targetScale = 0.95f, animationSpec = tween(ms, easing = FastOutSlowInEasing))
    }
}
