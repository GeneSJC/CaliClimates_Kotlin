package io.github.genesjc.microclimatefinder.ui.screens

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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fixed Cloud v1.0 Animation Screen
 * Features a canonical sculpted hero cloud hovering gently in place,
 * accompanied by radiant sun rays, wind streamlines, and a swaying California coastal palm tree.
 */
@Composable
fun FixedCloudSplashScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Infinite transition drivers for animations
    val transition = rememberInfiniteTransition(label = "FixedCloudAnimTransition")

    // Gentle vertical hover for the hero cloud (4.8s cycle)
    val cloudHoverY by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CloudHoverY"
    )

    // Sun corona pulse (3.0s cycle)
    val sunPulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SunPulse"
    )

    // Sun ray rotation (28s cycle)
    val sunRayAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SunRayAngle"
    )

    // Wind streamline flow cycle (3.6s)
    val windProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WindProgress"
    )

    // Palm tree sinusoidal sway (-4° to +4°)
    val palmSwayDegrees by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PalmSwayDegrees"
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
                    text = "v1.0 Fixed Cloud",
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
            FixedCloudAnimationStage(
                cloudHoverY = cloudHoverY,
                sunPulse = sunPulse,
                sunRayAngle = sunRayAngle,
                windProgress = windProgress,
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
                    text = "Micro Climate Finder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Fixed Cloud v1.0 Animation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBAE6FD)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sculpted central cumulus cloud hovering gently in place with radiant sun corona, flowing wind streamlines, and a swaying California coastal palm.",
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
 * Animated Canvas Stage for Fixed Cloud v1.0
 */
@Composable
private fun FixedCloudAnimationStage(
    cloudHoverY: Float,
    sunPulse: Float,
    sunRayAngle: Float,
    windProgress: Float,
    palmSwayDegrees: Float,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .size(320.dp, 300.dp)
            .clip(RoundedCornerShape(28.dp))
    ) {
        val stageWidthDp = maxWidth.value

        // 1. Radiant Golden Sun with Rays in Upper Left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 24.dp, y = 20.dp)
                .size(76.dp)
                .graphicsLayer {
                    scaleX = sunPulse
                    scaleY = sunPulse
                },
            contentAlignment = Alignment.Center
        ) {
            // Sun Rays Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerPx = size.width / 2f
                val rayLength = 34.dp.toPx()
                val coreRadius = 20.dp.toPx()
                for (i in 0 until 8) {
                    val angleDeg = sunRayAngle + (i * 45f)
                    val rad = Math.toRadians(angleDeg.toDouble())
                    val startX = centerPx + (cos(rad) * coreRadius).toFloat()
                    val startY = centerPx + (sin(rad) * coreRadius).toFloat()
                    val endX = centerPx + (cos(rad) * rayLength).toFloat()
                    val endY = centerPx + (sin(rad) * rayLength).toFloat()
                    drawLine(
                        color = Color(0xFFFDE047).copy(alpha = 0.75f),
                        start = androidx.compose.ui.geometry.Offset(startX, startY),
                        end = androidx.compose.ui.geometry.Offset(endX, endY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Soft Outer Corona
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFFFDE047).copy(alpha = 0.35f), CircleShape)
            )
            // Bright Core
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFFFBEB), Color(0xFFF59E0B))
                        ),
                        CircleShape
                    )
            )
        }

        // 2. Flowing Wind Streamlines
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawV1WindStreamlines(windProgress, size.width, size.height)
        }

        // 3. Central Hero Cloud with Hovering Motion
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-10).dp, y = cloudHoverY.dp)
        ) {
            SculptedHeroCloud(width = 170.dp, height = 110.dp)
        }

        // 4. Swaying California Coastal Palm Tree in Bottom Right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-16).dp, y = (-8).dp)
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
 * Sculpted Multi-Lobed Hero Cloud for v1.0
 */
@Composable
private fun SculptedHeroCloud(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(width, height)
    ) {
        // Base rounded pill
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.92f)
                .height(44.dp)
                .background(Color(0xFFC5DDF5), RoundedCornerShape(22.dp))
        )
        // Foreground white pill
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-3).dp)
                .fillMaxWidth(0.90f)
                .height(40.dp)
                .background(Color.White, RoundedCornerShape(20.dp))
        )
        // Left lobe
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 18.dp, y = (-18).dp)
                .size(56.dp)
                .background(Color.White, CircleShape)
        )
        // Central tall dome
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = (-4).dp, y = (-32).dp)
                .size(72.dp)
                .background(Color.White, CircleShape)
        )
        // Right lobe
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-22).dp, y = (-16).dp)
                .size(50.dp)
                .background(Color.White, CircleShape)
        )
    }
}

/**
 * Draws gentle meteorological wind streamlines
 */
private fun DrawScope.drawV1WindStreamlines(progress: Float, width: Float, height: Float) {
    val strokeWidth = 2.dp.toPx()

    // Line 1: Upper breeze across the top sky
    val line1X = ((progress + 0.2f) % 1f) * (width + 120f) - 60f
    val path1 = Path().apply {
        moveTo(line1X, height * 0.22f)
        cubicTo(
            line1X + 40f, height * 0.20f,
            line1X + 70f, height * 0.24f,
            line1X + 110f, height * 0.21f
        )
    }
    drawPath(
        path = path1,
        color = Color.White.copy(alpha = 0.55f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Line 2: Center gust flowing below cloud with gentle curl
    val line2X = ((progress + 0.6f) % 1f) * (width + 150f) - 75f
    val path2 = Path().apply {
        moveTo(line2X, height * 0.62f)
        cubicTo(
            line2X + 50f, height * 0.60f,
            line2X + 90f, height * 0.65f,
            line2X + 130f, height * 0.61f
        )
        cubicTo(
            line2X + 140f, height * 0.59f,
            line2X + 145f, height * 0.57f,
            line2X + 140f, height * 0.55f
        )
    }
    drawPath(
        path = path2,
        color = Color(0xFFBAE6FD).copy(alpha = 0.65f),
        style = Stroke(width = strokeWidth * 1.1f, cap = StrokeCap.Round)
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
