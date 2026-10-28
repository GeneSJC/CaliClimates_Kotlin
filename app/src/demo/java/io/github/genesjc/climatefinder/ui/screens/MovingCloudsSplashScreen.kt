package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.ui.res.stringResource
import io.github.genesjc.climatefinder.R
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Moving Clouds v2.0 Animation Screen
 * Features a multi-cloud horizontal stratus drift system:
 * - A lead & companion cloud pair gliding across the sky in tandem
 * - A solo offset cloud cruising across the lower sky
 * - All cloud speeds slowed down by 50% for an unhurried meteorological float
 * - Flowing wind streamlines, golden sun corona, and a swaying California coastal palm tree.
 */
@Composable
fun MovingCloudsSplashScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Infinite transition driver for smooth, unhurried animations
    val transition = rememberInfiniteTransition(label = "MovingCloudsAnimTransition")

    // Cloud Pair (Lead + Companion) Drift: 38 seconds (50% slowed)
    val cloudPairProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 38000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CloudPairDrift"
    )

    // Solo Cloud Drift: 50 seconds (50% slowed)
    val soloCloudProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 50000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SoloCloudDrift"
    )

    // High Wispy Cloudlet: 60 seconds (50% slowed)
    val wispCloudProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WispCloudDrift"
    )

    // Wind Streamline Flow Cycle: 5.6 seconds
    val windFlowProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WindStreamlinesFlow"
    )

    // Golden Sun Corona Breathing: 3.2 seconds
    val sunPulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SunPulse"
    )

    // California Coastal Palm Tree Sway: 3.6 seconds (-4.5° to +4.5°)
    val palmSwayDegrees by transition.animateFloat(
        initialValue = -4.5f,
        targetValue = 4.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PalmSway"
    )

    // Screen Layout
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF38BDF8), // Sky Blue 400
                        Color(0xFF0284C7), // Sky Blue 600
                        Color(0xFF08203E)  // Deep Marine Navy
                    )
                )
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.35f),
                modifier = Modifier
                    .clickable(onClick = onNavigateBack)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "←",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Animation Previews",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.White.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "v2.0 Moving Clouds",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center Animated Stage
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            MovingCloudsAnimationStage(
                cloudPairProgress = cloudPairProgress,
                soloCloudProgress = soloCloudProgress,
                wispCloudProgress = wispCloudProgress,
                windFlowProgress = windFlowProgress,
                sunPulse = sunPulse,
                palmSwayDegrees = palmSwayDegrees
            )
        }

        // Title and Description Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.45f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Moving Clouds v2.0 Animation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBAE6FD)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Multi-cloud horizontal stratus drift (paired lead & companion clouds, plus solo offset cloud slowed down by 50%), flowing wind streamlines, and a swaying coastal palm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

/**
 * Animated Canvas Stage for Moving Clouds v2.0
 */
@Composable
private fun MovingCloudsAnimationStage(
    cloudPairProgress: Float,
    soloCloudProgress: Float,
    wispCloudProgress: Float,
    windFlowProgress: Float,
    sunPulse: Float,
    palmSwayDegrees: Float,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .size(320.dp, 300.dp)
            .clip(RoundedCornerShape(28.dp))
    ) {
        val stageWidthDp = maxWidth.value

        // 1. Warm Golden Sun in Upper Right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-30).dp, y = 20.dp)
                .size(72.dp)
                .graphicsLayer {
                    scaleX = sunPulse
                    scaleY = sunPulse
                },
            contentAlignment = Alignment.Center
        ) {
            // Soft Outer Corona
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color(0xFFFDE047).copy(alpha = 0.25f), CircleShape)
            )
            // Mid Sun Glow
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xFFFBBF24).copy(alpha = 0.55f), CircleShape)
            )
            // Bright Core
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFFFBEB), Color(0xFFF59E0B))
                        ),
                        CircleShape
                    )
            )
        }

        // 2. Wind Streamlines Canvas (Flowing West to East)
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawV2WindStreamlines(windFlowProgress, size.width, size.height)
        }

        // 3. Drifting Multi-Cloud Stratus System (Calculated directly in Dp)
        // A. High Wispy Cloudlet (Top sky)
        val wispX = ((wispCloudProgress + 0.4f) % 1f) * (stageWidthDp + 80f) - 40f
        Box(
            modifier = Modifier
                .offset(x = wispX.dp, y = 18.dp)
        ) {
            HorizontalStratusCloud(width = 54.dp, height = 18.dp, alpha = 0.7f)
        }

        // B. Cloud Pair - Lead Cloud (Upper-mid sky, 88dp wide)
        val leadX = ((cloudPairProgress + 0.65f) % 1f) * (stageWidthDp + 120f) - 60f
        Box(
            modifier = Modifier
                .offset(x = leadX.dp, y = 60.dp)
        ) {
            HorizontalStratusCloud(width = 88.dp, height = 28.dp, alpha = 0.95f)
        }

        // C. Cloud Pair - Companion Cloud (Traveling close behind lead cloud, 64dp wide)
        val companionX = ((cloudPairProgress + 0.52f) % 1f) * (stageWidthDp + 120f) - 60f
        Box(
            modifier = Modifier
                .offset(x = companionX.dp, y = 88.dp)
        ) {
            HorizontalStratusCloud(width = 64.dp, height = 22.dp, alpha = 0.90f)
        }

        // D. Solo Cloud (Lower-mid sky, offset pace, 76dp wide)
        val soloX = ((soloCloudProgress + 0.2f) % 1f) * (stageWidthDp + 100f) - 50f
        Box(
            modifier = Modifier
                .offset(x = soloX.dp, y = 140.dp)
        ) {
            HorizontalStratusCloud(width = 76.dp, height = 26.dp, alpha = 0.88f)
        }

        // 4. Swaying California Coastal Palm Tree in Bottom Right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-16).dp, y = (-6).dp)
                .graphicsLayer {
                    rotationZ = palmSwayDegrees
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1.0f)
                }
        ) {
            CaliforniaCoastalPalm(width = 72.dp, height = 96.dp)
        }
    }
}

/**
 * Reusable Horizontal Stratus Cloud for v2.0
 */
@Composable
private fun HorizontalStratusCloud(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    alpha: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width, height)
            .graphicsLayer { this.alpha = alpha }
    ) {
        // Base stratus elongated pill
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(height * 0.70f)
                .background(Color(0xFFC5DDF5), RoundedCornerShape(12.dp))
        )
        // White foreground dome
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-2).dp)
                .fillMaxWidth(0.96f)
                .height(height * 0.65f)
                .background(Color.White, RoundedCornerShape(12.dp))
        )
        // Left lobe
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = width * 0.12f, y = -(height * 0.25f))
                .size(height * 0.75f)
                .background(Color.White, CircleShape)
        )
        // Center higher lobe
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = -(width * 0.05f), y = -(height * 0.35f))
                .size(height * 0.85f)
                .background(Color.White, CircleShape)
        )
        // Right lobe
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = -(width * 0.14f), y = -(height * 0.22f))
                .size(height * 0.68f)
                .background(Color.White, CircleShape)
        )
    }
}

/**
 * Draws gentle meteorological wind streamlines for v2.0
 */
private fun DrawScope.drawV2WindStreamlines(progress: Float, width: Float, height: Float) {
    val strokeWidth = 2.dp.toPx()

    // Line 1: High subtle wind wisp
    val line1X = ((progress + 0.1f) % 1f) * (width + 160f) - 80f
    val path1 = Path().apply {
        moveTo(line1X, height * 0.15f)
        cubicTo(
            line1X + 40f, height * 0.12f,
            line1X + 80f, height * 0.18f,
            line1X + 130f, height * 0.14f
        )
    }
    drawPath(
        path = path1,
        color = Color.White.copy(alpha = 0.5f),
        style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round)
    )

    // Line 2: Mid-altitude streamline with gentle curl
    val line2X = ((progress + 0.55f) % 1f) * (width + 200f) - 100f
    val path2 = Path().apply {
        moveTo(line2X, height * 0.42f)
        cubicTo(
            line2X + 50f, height * 0.38f,
            line2X + 100f, height * 0.46f,
            line2X + 150f, height * 0.41f
        )
        cubicTo(
            line2X + 162f, height * 0.39f,
            line2X + 168f, height * 0.36f,
            line2X + 164f, height * 0.33f
        )
    }
    drawPath(
        path = path2,
        color = Color(0xFFBAE6FD).copy(alpha = 0.65f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Line 3: Lower coastal sea-breeze stream
    val line3X = ((progress + 0.3f) % 1f) * (width + 180f) - 90f
    val path3 = Path().apply {
        moveTo(line3X, height * 0.72f)
        cubicTo(
            line3X + 45f, height * 0.68f,
            line3X + 95f, height * 0.76f,
            line3X + 145f, height * 0.71f
        )
    }
    drawPath(
        path = path3,
        color = Color(0xFFE0F2FE).copy(alpha = 0.45f),
        style = Stroke(width = strokeWidth * 0.9f, cap = StrokeCap.Round)
    )
}

/**
 * Handcrafted California coastal palm tree
 */
@Composable
private fun CaliforniaCoastalPalm(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width, height)) {
        val w = size.width
        val h = size.height

        // 1. Curved Trunk (amber-brown with highlight)
        val trunkBase = Path().apply {
            moveTo(w * 0.54f, h)
            cubicTo(
                w * 0.53f, h * 0.70f,
                w * 0.47f, h * 0.45f,
                w * 0.49f, h * 0.26f
            )
        }
        drawPath(
            path = trunkBase,
            color = Color(0xFFD97706),
            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = trunkBase,
            color = Color(0xFFFBBF24),
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // 2. Palm Fronds (lush filled leaf silhouettes radiating from crown)
        val crownX = w * 0.49f
        val crownY = h * 0.26f

        // Frond 1: Right-drooping leaf
        val f1 = Path().apply {
            moveTo(crownX, crownY)
            cubicTo(crownX + 22.dp.toPx(), crownY - 14.dp.toPx(), crownX + 48.dp.toPx(), crownY - 6.dp.toPx(), crownX + 62.dp.toPx(), crownY + 20.dp.toPx())
            cubicTo(crownX + 52.dp.toPx(), crownY + 16.dp.toPx(), crownX + 34.dp.toPx(), crownY + 4.dp.toPx(), crownX, crownY)
            close()
        }
        drawPath(f1, Color(0xFF10B981))

        // Frond 2: Right high-arched leaf
        val f2 = Path().apply {
            moveTo(crownX, crownY)
            cubicTo(crownX + 16.dp.toPx(), crownY - 30.dp.toPx(), crownX + 42.dp.toPx(), crownY - 34.dp.toPx(), crownX + 52.dp.toPx(), crownY - 14.dp.toPx())
            cubicTo(crownX + 42.dp.toPx(), crownY - 16.dp.toPx(), crownX + 25.dp.toPx(), crownY - 16.dp.toPx(), crownX, crownY)
            close()
        }
        drawPath(f2, Color(0xFF34D399))

        // Frond 3: Center crown leaf
        val f3 = Path().apply {
            moveTo(crownX, crownY)
            cubicTo(crownX + 3.dp.toPx(), crownY - 38.dp.toPx(), crownX + 16.dp.toPx(), crownY - 48.dp.toPx(), crownX + 20.dp.toPx(), crownY - 30.dp.toPx())
            cubicTo(crownX + 14.dp.toPx(), crownY - 30.dp.toPx(), crownX + 6.dp.toPx(), crownY - 20.dp.toPx(), crownX, crownY)
            close()
        }
        drawPath(f3, Color(0xFF6EE7B7))

        // Frond 4: Left high leaf
        val f4 = Path().apply {
            moveTo(crownX, crownY)
            cubicTo(crownX - 16.dp.toPx(), crownY - 34.dp.toPx(), crownX - 38.dp.toPx(), crownY - 30.dp.toPx(), crownX - 48.dp.toPx(), crownY - 11.dp.toPx())
            cubicTo(crownX - 36.dp.toPx(), crownY - 16.dp.toPx(), crownX - 19.dp.toPx(), crownY - 16.dp.toPx(), crownX, crownY)
            close()
        }
        drawPath(f4, Color(0xFF34D399))

        // Frond 5: Left drooping leaf
        val f5 = Path().apply {
            moveTo(crownX, crownY)
            cubicTo(crownX - 20.dp.toPx(), crownY - 11.dp.toPx(), crownX - 44.dp.toPx(), crownY - 3.dp.toPx(), crownX - 55.dp.toPx(), crownY + 24.dp.toPx())
            cubicTo(crownX - 45.dp.toPx(), crownY + 18.dp.toPx(), crownX - 30.dp.toPx(), crownY + 5.dp.toPx(), crownX, crownY)
            close()
        }
        drawPath(f5, Color(0xFF10B981))

        // 3. Palm heart coconuts
        drawCircle(Color(0xFF78350F), radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(crownX - 2.dp.toPx(), crownY + 3.dp.toPx()))
        drawCircle(Color(0xFF78350F), radius = 3.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(crownX + 4.dp.toPx(), crownY + 4.dp.toPx()))
    }
}
