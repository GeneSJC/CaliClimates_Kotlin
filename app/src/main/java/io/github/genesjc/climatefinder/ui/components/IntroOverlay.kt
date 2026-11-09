package io.github.genesjc.climatefinder.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.ui.theme.LocalStateUsa
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long the app's name stays fully showing before the intro fades out (long for now, for presentations). */
private const val INTRO_HOLD_MS = 4500L

/**
 * The opening view: it picks up where the system splash screen leaves off (the round icon, centred, on the
 * same dark blue-slate), adds a soft glow behind the icon and the state's outline with its scattered dots (see
 * [StateUsaIntroBackdrop]), and fades the app's name and tagline in below the outline, then fades away into the app.
 * A tap skips it.
 */
@Composable
fun IntroOverlay(onDone: () -> Unit) {
    val exit = remember { Animatable(1f) }
    val text = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { glow.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        delay(350)
        launch { text.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        // The title is fully in 700 ms after it starts; it then stays for INTRO_HOLD_MS before fading away
        delay(700L + INTRO_HOLD_MS)
        exit.animateTo(0f, tween(450))
        onDone()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exit.value }
            .background(Color(0xFF0A1628))
            // A tap anywhere skips ahead
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() },
        contentAlignment = Alignment.Center
    ) {
        val stateUsa = LocalStateUsa.current
        // The state's outline sits behind the icon, a little above the middle; the name and tagline go below the whole outline
        val stateHeight = stateUsa.introStateHeight(maxWidth, maxHeight)
        val stateCenterY = maxHeight * INTRO_STATE_CENTER
        val textTop = stateCenterY + stateHeight / 2 + INTRO_TEXT_GAP
        // A soft glow behind the icon, in the icon's own blue
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = glow.value }
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF2B6CB0).copy(alpha = 0.40f), Color.Transparent),
                        radius = 900f
                    )
                )
        )
        // The state's outline with its scattered dots, fading in with the glow
        StateUsaIntroBackdrop(
            stateUsa = stateUsa,
            stateHeight = stateHeight,
            centerY = stateCenterY,
            iconRadius = INTRO_ICON_SIZE / 2,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = glow.value }
        )
        // The icon, the same size and place as the system splash's: a 156dp circle showing the middle of the artwork
        Box(modifier = Modifier.size(INTRO_ICON_SIZE).clip(CircleShape), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_vector),
                contentDescription = null,
                modifier = Modifier.requiredSize(234.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = textTop)
                .graphicsLayer {
                    alpha = text.value
                    translationY = (1f - text.value) * 24f
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(stateUsa.taglineRes),
                color = Color(0xFFBAE6FD),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** The icon's size on the opening view, the same as the system splash's. */
private val INTRO_ICON_SIZE = 156.dp

/** Where the middle of the state's outline sits, as a share of the view's height (the icon is at the middle, 0.5). */
private const val INTRO_STATE_CENTER = 0.47f

/** The room between the bottom of the state's outline and the app name. */
private val INTRO_TEXT_GAP = 22.dp
