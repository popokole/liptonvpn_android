package com.lipton.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lipton.vpn.service.LiptonVpnService.VpnStatus
import com.lipton.vpn.ui.theme.*

@Composable
fun ConnectButton(
    status: VpnStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isPending = status == VpnStatus.CONNECTING || status == VpnStatus.DISCONNECTING

    val infiniteAnim = rememberInfiniteTransition(label = "btn")

    // 0→1 breathing for CONNECTED glow (slow, calm)
    val glowPulse by infiniteAnim.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "glow",
    )

    // Spinning arc for CONNECTING / DISCONNECTING
    val spinAngle by infiniteAnim.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "spin",
    )

    // Expanding invitation ring for DISCONNECTED
    val invitePulse by infiniteAnim.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing), RepeatMode.Restart
        ),
        label = "invite",
    )

    // Scale pulse while CONNECTING
    val connectPulse by infiniteAnim.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "cpulse",
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !isPending) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "press",
    )

    val btnColor by animateColorAsState(
        targetValue = when (status) {
            VpnStatus.CONNECTED              -> GreenSoft
            VpnStatus.CONNECTING,
            VpnStatus.DISCONNECTING          -> GreenMid
            VpnStatus.ERROR                  -> RedSoft
            else                             -> Color(0x0AFF453A)
        },
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "btn_color",
    )
    val iconColor by animateColorAsState(
        targetValue = when (status) {
            VpnStatus.CONNECTED,
            VpnStatus.CONNECTING,
            VpnStatus.DISCONNECTING -> Green
            else                    -> Red
        },
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "icon_color",
    )
    val borderColor by animateColorAsState(
        targetValue = when (status) {
            VpnStatus.CONNECTED              -> Green.copy(alpha = 0.55f)
            VpnStatus.CONNECTING,
            VpnStatus.DISCONNECTING          -> Green.copy(alpha = 0.45f)
            VpnStatus.ERROR                  -> Red.copy(alpha = 0.45f)
            else                             -> Red.copy(alpha = 0.35f)
        },
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "border",
    )

    val effectiveScale = if (isPending) connectPulse else pressScale

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(168.dp)) {

        Canvas(modifier = Modifier.size(168.dp)) {
            val cx     = size.width  / 2f
            val cy     = size.height / 2f
            val outerR = size.width  / 2f
            val innerR = size.width  * 0.446f
            // Pre-compute pixel values used in multiple branches
            val sw1   = 1.dp.toPx()
            val sw15  = 1.5.dp.toPx()
            val sw2   = 2.dp.toPx()
            val sw3   = 3.dp.toPx()
            val gap   = 4.dp.toPx()

            when (status) {
                VpnStatus.CONNECTED -> {
                    // Soft backdrop fill — single cheap circle
                    drawCircle(
                        color  = Green.copy(alpha = 0.035f + glowPulse * 0.07f),
                        radius = outerR * 0.88f,
                    )
                    // Glow rings — stacked strokes with decreasing alpha (no shader!)
                    val baseAlpha = 0.15f + glowPulse * 0.35f
                    drawCircle(color = Green.copy(alpha = baseAlpha),          radius = innerR,           style = Stroke(sw2))
                    drawCircle(color = Green.copy(alpha = baseAlpha * 0.50f),  radius = innerR + gap,     style = Stroke(sw2))
                    drawCircle(color = Green.copy(alpha = baseAlpha * 0.28f),  radius = innerR + gap * 2, style = Stroke(sw2))
                    drawCircle(color = Green.copy(alpha = baseAlpha * 0.14f),  radius = innerR + gap * 3, style = Stroke(sw15))
                    // Outer subtle ring
                    drawCircle(
                        color  = Green.copy(alpha = 0.04f + glowPulse * 0.08f),
                        radius = outerR,
                        style  = Stroke(sw1),
                    )
                }
                VpnStatus.CONNECTING, VpnStatus.DISCONNECTING -> {
                    drawCircle(
                        color  = Green.copy(alpha = 0.10f),
                        radius = outerR,
                        style  = Stroke(sw1),
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, Green.copy(alpha = 0.22f), Green),
                            center = Offset(cx, cy),
                        ),
                        startAngle = spinAngle,
                        sweepAngle = 260f,
                        useCenter  = false,
                        topLeft    = Offset(cx - innerR, cy - innerR),
                        size       = Size(innerR * 2, innerR * 2),
                        style      = Stroke(width = sw3, cap = StrokeCap.Round),
                    )
                }
                VpnStatus.ERROR -> {
                    drawCircle(color = Red.copy(alpha = 0.35f), radius = innerR, style = Stroke(sw15))
                    drawCircle(color = Red.copy(alpha = 0.12f), radius = outerR, style = Stroke(sw1))
                }
                else -> {
                    // Expanding invitation pulse
                    val pulseR = innerR + (outerR - innerR) * invitePulse
                    drawCircle(
                        color  = Green.copy(alpha = (1f - invitePulse) * 0.20f),
                        radius = pulseR,
                        style  = Stroke(sw15),
                    )
                    drawCircle(color = Green.copy(alpha = 0.10f), radius = innerR, style = Stroke(sw15))
                }
            }
        }

        Surface(
            modifier = Modifier
                .size(120.dp)
                .scale(effectiveScale)
                .clickable(
                    interactionSource = interactionSource,
                    indication        = null,
                    enabled           = !isPending,
                    onClick           = onClick,
                ),
            shape  = CircleShape,
            color  = btnColor,
            border = BorderStroke(1.5.dp, borderColor),
        ) {
            Box(contentAlignment = Alignment.Center) {
                PowerIcon(color = iconColor)
            }
        }
    }
}

@Composable
private fun PowerIcon(color: Color) {
    Canvas(modifier = Modifier.size(44.dp)) {
        val s       = size.width / 24f
        val strokeW = 2.dp.toPx()
        val radius  = 9f * s
        val arcCx   = 12f * s
        val arcCy   = 13f * s

        drawArc(
            color      = color,
            startAngle = -45f,
            sweepAngle = 270f,
            useCenter  = false,
            topLeft    = Offset(arcCx - radius, arcCy - radius),
            size       = Size(radius * 2f, radius * 2f),
            style      = Stroke(width = strokeW, cap = StrokeCap.Round),
        )
        drawLine(
            color       = color,
            start       = Offset(12f * s, 2f * s),
            end         = Offset(12f * s, 12f * s),
            strokeWidth = strokeW,
            cap         = StrokeCap.Round,
        )
    }
}
