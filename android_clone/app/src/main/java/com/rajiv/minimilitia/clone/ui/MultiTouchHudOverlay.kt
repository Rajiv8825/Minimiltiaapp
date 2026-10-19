package com.rajiv.minimilitia.clone.ui

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajiv.minimilitia.clone.engine.GameLoopState
import kotlin.math.*

/**
 * High-Performance Multi-Touch HUD Overlay for Mini Militia.
 * - Independent pointer tracking (awaitPointerEventScope) for simultaneous dual-stick control.
 * - Left half: Dynamic floating / anchored movement & jetpack thrust.
 * - Right half: Dynamic floating / anchored 360° aiming & auto-firing.
 * - Visual touch feedback: Glowing rings, threshold rings, laser direction lines, and authentic Cocos2d sprites.
 * - Top-Center: Stacked Red Health & Cyan Boost bars (zero text labels).
 * - Top-Left: Tactical Scoreboard Pill & Pause button.
 * - Top-Right: Equipped Weapon Card, Ammo count, Weapon Switch, Reload, Frag & Gas Grenade buttons.
 */
@Composable
fun MultiTouchHudOverlay(
    gameState: GameLoopState,
    onExitToMenu: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val player = gameState.player
    val density = LocalDensity.current
    val maxRadiusPx = with(density) { 72.dp.toPx() }
    val topHudHeightPx = with(density) { 95.dp.toPx() }
    val topRightSafeW = with(density) { 280.dp.toPx() }
    val topLeftSafeW = with(density) { 190.dp.toPx() }

    // Multi-touch pointer states
    var leftPointerId by remember { mutableStateOf<PointerId?>(null) }
    var leftAnchor by remember { mutableStateOf<Offset?>(null) }
    var leftKnobOffset by remember { mutableStateOf(Offset.Zero) }
    var isLeftTouching by remember { mutableStateOf(false) }

    var rightPointerId by remember { mutableStateOf<PointerId?>(null) }
    var rightAnchor by remember { mutableStateOf<Offset?>(null) }
    var rightKnobOffset by remember { mutableStateOf(Offset.Zero) }
    var isRightTouching by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {

        // ================= 1. DUAL JOYSTICK TOUCH SURFACE & CANVAS =================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val halfW = size.width / 2.0f
                            val changes = event.changes

                            // 1. Process all pointer events
                            for (i in 0 until changes.size) {
                                val change = changes[i]
                                val pid = change.id
                                val pos = change.position

                                // A. Newly pressed down finger
                                if (!change.previousPressed && change.pressed) {
                                    val isInTopButtons = pos.y < topHudHeightPx &&
                                            (pos.x > size.width - topRightSafeW || pos.x < topLeftSafeW)
                                    if (!isInTopButtons) {
                                        if (pos.x < halfW && leftPointerId == null) {
                                            leftPointerId = pid
                                            val clampedX = pos.x.coerceIn(maxRadiusPx + 16.0f, halfW - maxRadiusPx - 16.0f)
                                            val clampedY = pos.y.coerceIn(maxRadiusPx + topHudHeightPx * 0.4f, size.height - maxRadiusPx - 16.0f)
                                            leftAnchor = Offset(clampedX, clampedY)
                                            leftKnobOffset = Offset.Zero
                                            isLeftTouching = true
                                            change.consume()
                                        } else if (pos.x >= halfW && rightPointerId == null) {
                                            rightPointerId = pid
                                            val clampedX = pos.x.coerceIn(halfW + maxRadiusPx + 16.0f, size.width - maxRadiusPx - 16.0f)
                                            val clampedY = pos.y.coerceIn(maxRadiusPx + topHudHeightPx * 0.4f, size.height - maxRadiusPx - 16.0f)
                                            rightAnchor = Offset(clampedX, clampedY)
                                            rightKnobOffset = Offset.Zero
                                            isRightTouching = true
                                            change.consume()
                                        }
                                    }
                                }

                                // B. Left Joystick active drag or release
                                if (pid == leftPointerId) {
                                    if (change.pressed) {
                                        val anchor = leftAnchor ?: Offset(size.width * 0.12f, size.height - size.width * 0.08f)
                                        val deltaX = pos.x - anchor.x
                                        val deltaY = pos.y - anchor.y
                                        val dist = sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toFloat()
                                        val clampedDist = dist.coerceAtMost(maxRadiusPx)
                                        val angle = if (dist > 0.001f) atan2(deltaY.toDouble(), deltaX.toDouble()).toFloat() else 0.0f
                                        val knobX = (cos(angle.toDouble()) * clampedDist).toFloat()
                                        val knobY = (sin(angle.toDouble()) * clampedDist).toFloat()
                                        leftKnobOffset = Offset(knobX, knobY)
                                        isLeftTouching = true

                                        val dx = (knobX / maxRadiusPx).coerceIn(-1.0f, 1.0f)
                                        val dy = (knobY / maxRadiusPx).coerceIn(-1.0f, 1.0f)
                                        gameState.onLeftJoystickMove(dx, dy)
                                        change.consume()
                                    } else {
                                        leftPointerId = null
                                        leftAnchor = null
                                        leftKnobOffset = Offset.Zero
                                        isLeftTouching = false
                                        gameState.onLeftJoystickRelease()
                                        change.consume()
                                    }
                                }

                                // C. Right Joystick active drag or release
                                if (pid == rightPointerId) {
                                    if (change.pressed) {
                                        val anchor = rightAnchor ?: Offset(size.width * 0.88f, size.height - size.width * 0.08f)
                                        val deltaX = pos.x - anchor.x
                                        val deltaY = pos.y - anchor.y
                                        val dist = sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toFloat()
                                        val clampedDist = dist.coerceAtMost(maxRadiusPx)
                                        val angle = if (dist > 0.001f) atan2(deltaY.toDouble(), deltaX.toDouble()).toFloat() else player.aimAngle
                                        val knobX = (cos(angle.toDouble()) * clampedDist).toFloat()
                                        val knobY = (sin(angle.toDouble()) * clampedDist).toFloat()
                                        rightKnobOffset = Offset(knobX, knobY)
                                        isRightTouching = true

                                        val distRatio = (clampedDist / maxRadiusPx).coerceIn(0.0f, 1.0f)
                                        gameState.onRightJoystickMove(angle, distRatio)
                                        change.consume()
                                    } else {
                                        rightPointerId = null
                                        rightAnchor = null
                                        rightKnobOffset = Offset.Zero
                                        isRightTouching = false
                                        gameState.onRightJoystickRelease()
                                        change.consume()
                                    }
                                }
                            }

                            // 2. Stale pointer cleanup
                            var hasLeftPressed = false
                            var hasRightPressed = false
                            for (i in 0 until changes.size) {
                                val c = changes[i]
                                if (c.pressed) {
                                    if (c.id == leftPointerId) hasLeftPressed = true
                                    if (c.id == rightPointerId) hasRightPressed = true
                                }
                            }
                            if (leftPointerId != null && !hasLeftPressed) {
                                leftPointerId = null
                                leftAnchor = null
                                leftKnobOffset = Offset.Zero
                                isLeftTouching = false
                                gameState.onLeftJoystickRelease()
                            }
                            if (rightPointerId != null && !hasRightPressed) {
                                rightPointerId = null
                                rightAnchor = null
                                rightKnobOffset = Offset.Zero
                                isRightTouching = false
                                gameState.onRightJoystickRelease()
                            }
                        }
                    }
                }
        ) {
            // Draw Authentic Joysticks & Visual Touch Feedback
            Canvas(modifier = Modifier.fillMaxSize()) {
                val padRadius = maxRadiusPx * 0.95f
                val stickRadius = maxRadiusPx * 0.44f

                val movePadBmp = gameState.spriteRepo.getBitmap("movePad")
                val moveStickBmp = gameState.spriteRepo.getBitmap("moveStick")
                val firePadBmp = gameState.spriteRepo.getBitmap("firePad")
                val fireStickBmp = gameState.spriteRepo.getBitmap("fireStick")

                // ---------------- LEFT JOYSTICK (MOVE & JETPACK) ----------------
                val defaultLeft = Offset(size.width * 0.12f, size.height - size.width * 0.08f)
                val curLeftAnchor = leftAnchor ?: defaultLeft
                val curLeftKnob = curLeftAnchor + leftKnobOffset

                // Outer Glowing Ring & Fill
                drawCircle(
                    color = if (isLeftTouching) Color(0x3300AAFF) else Color(0x1800AAFF),
                    radius = maxRadiusPx,
                    center = curLeftAnchor
                )
                drawCircle(
                    color = if (isLeftTouching) Color(0x9900CCFF) else Color(0x440077B6),
                    radius = maxRadiusPx,
                    center = curLeftAnchor,
                    style = Stroke(width = if (isLeftTouching) 2.5f else 1.5f)
                )

                // Authentic movePad Sprite Texture
                movePadBmp?.let { bmp ->
                    val dst = RectF(
                        curLeftAnchor.x - padRadius,
                        curLeftAnchor.y - padRadius,
                        curLeftAnchor.x + padRadius,
                        curLeftAnchor.y + padRadius
                    )
                    drawContext.canvas.nativeCanvas.drawBitmap(bmp, null, dst, null)
                }

                // Jetpack Thrust Pulse Aura under knob
                val isThrusting = isLeftTouching && (leftKnobOffset.y < -maxRadiusPx * 0.25f)
                if (isThrusting) {
                    drawCircle(
                        color = Color(0x6600E5FF),
                        radius = stickRadius * 1.35f,
                        center = curLeftKnob
                    )
                }

                // Left Thumb Knob (moveStick Sprite Texture)
                drawCircle(
                    color = if (isLeftTouching) Color(0x440088DD) else Color(0x22005599),
                    radius = stickRadius,
                    center = curLeftKnob
                )
                moveStickBmp?.let { bmp ->
                    val dst = RectF(
                        curLeftKnob.x - stickRadius,
                        curLeftKnob.y - stickRadius,
                        curLeftKnob.x + stickRadius,
                        curLeftKnob.y + stickRadius
                    )
                    drawContext.canvas.nativeCanvas.drawBitmap(bmp, null, dst, null)
                }
                drawCircle(
                    color = Color.White.copy(alpha = if (isLeftTouching) 0.85f else 0.4f),
                    radius = stickRadius,
                    center = curLeftKnob,
                    style = Stroke(width = 2.0f)
                )

                // ---------------- RIGHT JOYSTICK (360° AIM & AUTO-FIRE) ----------------
                val defaultRight = Offset(size.width * 0.88f, size.height - size.width * 0.08f)
                val curRightAnchor = rightAnchor ?: defaultRight
                val curRightKnob = curRightAnchor + rightKnobOffset
                val rightDist = sqrt((rightKnobOffset.x * rightKnobOffset.x + rightKnobOffset.y * rightKnobOffset.y).toDouble()).toFloat()
                val isFiringActive = isRightTouching && (rightDist > maxRadiusPx * 0.45f)

                // Outer Glowing Ring & Fill
                drawCircle(
                    color = if (isRightTouching) Color(0x33FF3333) else Color(0x18FF3333),
                    radius = maxRadiusPx,
                    center = curRightAnchor
                )
                drawCircle(
                    color = if (isRightTouching) Color(0x99FF5555) else Color(0x44CC2222),
                    radius = maxRadiusPx,
                    center = curRightAnchor,
                    style = Stroke(width = if (isRightTouching) 2.5f else 1.5f)
                )

                // Concentric Auto-Fire Threshold Ring
                val thresholdRadius = maxRadiusPx * 0.45f
                drawCircle(
                    color = if (isFiringActive) Color(0x44FF2222) else Color(0x12FF2222),
                    radius = thresholdRadius,
                    center = curRightAnchor
                )
                drawCircle(
                    color = if (isFiringActive) Color(0xFFFF2222) else Color(0x66FF4444),
                    radius = thresholdRadius,
                    center = curRightAnchor,
                    style = Stroke(width = if (isFiringActive) 2.5f else 1.5f)
                )

                // Authentic firePad Sprite Texture
                firePadBmp?.let { bmp ->
                    val dst = RectF(
                        curRightAnchor.x - padRadius,
                        curRightAnchor.y - padRadius,
                        curRightAnchor.x + padRadius,
                        curRightAnchor.y + padRadius
                    )
                    drawContext.canvas.nativeCanvas.drawBitmap(bmp, null, dst, null)
                }

                // Aiming Laser Direction Indicator Line
                if (isRightTouching && rightDist > 8.0f) {
                    val aimDirX = rightKnobOffset.x / rightDist
                    val aimDirY = rightKnobOffset.y / rightDist
                    val laserEnd = Offset(
                        curRightKnob.x + aimDirX * (maxRadiusPx * 0.65f),
                        curRightKnob.y + aimDirY * (maxRadiusPx * 0.65f)
                    )
                    drawLine(
                        color = if (isFiringActive) Color(0xFFFF3333) else Color(0x99FF7777),
                        start = curRightKnob,
                        end = laserEnd,
                        strokeWidth = if (isFiringActive) 3.5f else 2.0f
                    )
                }

                // Right Thumb Knob (fireStick Sprite Texture)
                drawCircle(
                    color = if (isFiringActive) Color(0x88FF2222) else if (isRightTouching) Color(0x44CC2222) else Color(0x22881111),
                    radius = stickRadius,
                    center = curRightKnob
                )
                fireStickBmp?.let { bmp ->
                    val dst = RectF(
                        curRightKnob.x - stickRadius,
                        curRightKnob.y - stickRadius,
                        curRightKnob.x + stickRadius,
                        curRightKnob.y + stickRadius
                    )
                    drawContext.canvas.nativeCanvas.drawBitmap(bmp, null, dst, null)
                }
                drawCircle(
                    color = if (isFiringActive) Color(0xFFFF4444) else Color.White.copy(alpha = if (isRightTouching) 0.85f else 0.4f),
                    radius = stickRadius,
                    center = curRightKnob,
                    style = Stroke(width = if (isFiringActive) 2.5f else 2.0f)
                )
            }
        }

        // ================= 2. TOP-LEFT: SCOREBOARD PILL & PAUSE BUTTON =================
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pause / Menu Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC111822))
                    .border(1.5.dp, Color(0x66557799), RoundedCornerShape(8.dp))
                    .clickable { onExitToMenu() },
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(modifier = Modifier.width(3.dp).height(14.dp).background(Color(0xFFE2E8F0)))
                    Box(modifier = Modifier.width(3.dp).height(14.dp).background(Color(0xFFE2E8F0)))
                }
            }

            // Tactical Status Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC0D141E))
                    .border(1.5.dp, Color(0x88FFCC00), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "KILLS: ${player.kills}",
                    color = Color(0xFFFFCC00),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "|",
                    color = Color(0x55FFCC00),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "BOT WAVE: ${gameState.botController.currentWave}",
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // ================= 3. TOP-CENTER: STACKED HEALTH & BOOST (ZERO TEXT LABELS) =================
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xDD0B1017))
                .border(1.5.dp, Color(0x773E5066), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Red Health Bar (Full width 180.dp, height 14.dp, pure red fill, no text)
            val healthRatio = (player.health / player.maxHealth).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF2B0A0A))
                    .border(1.dp, Color(0xFF551111), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(healthRatio)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFCC1122), Color(0xFFFF3344))
                            )
                        )
                )
            }

            // Cyan Jetpack Boost Bar (Full width 180.dp, height 9.dp, pure cyan fill, no text)
            val fuelRatio = (player.jetpackFuel / player.maxFuel).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(9.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF041522))
                    .border(1.dp, Color(0xFF083A58), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fuelRatio)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0088CC), Color(0xFF00E5FF))
                            )
                        )
                )
            }
        }

        // ================= 4. TOP-RIGHT: WEAPON CARD & TACTICAL CLUSTER =================
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 10.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Equipped Weapon Card (Tap to swap)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xDD111822))
                    .border(1.5.dp, Color(0x66557799), RoundedCornerShape(8.dp))
                    .clickable { gameState.switchWeapon() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                val currentWepBmp = gameState.spriteRepo.get(player.equippedWeapon.spriteKey)
                if (currentWepBmp != null) {
                    Image(
                        bitmap = currentWepBmp,
                        contentDescription = player.equippedWeapon.displayName,
                        modifier = Modifier.size(width = 62.dp, height = 30.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = player.equippedWeapon.displayName.uppercase(),
                        color = Color(0xFFFFCC00),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    val ammoText = if (player.isReloading) "RELOADING" else "${player.ammoInClip} / ${player.reserveAmmo}"
                    Text(
                        text = ammoText,
                        color = if (player.isReloading || player.ammoInClip <= 4) Color(0xFFFF3333) else Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Tactical Buttons: Weapon Switch, Reload, Frag Grenade, Gas Grenade
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Weapon Switch Button (wepChangeBtn.png)
                val swapBmp = gameState.spriteRepo.get("wepChangeBtn")
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xDD111822))
                        .border(1.5.dp, Color(0x77557799), CircleShape)
                        .clickable { gameState.switchWeapon() }
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (swapBmp != null) {
                        Image(
                            bitmap = swapBmp,
                            contentDescription = "Swap Weapon",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Reload Button (ammoBtn.png)
                val reloadBmp = gameState.spriteRepo.get("ammoBtn")
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xDD111822))
                        .border(1.5.dp, Color(0x77557799), CircleShape)
                        .clickable { gameState.reloadWeapon() }
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (reloadBmp != null) {
                        Image(
                            bitmap = reloadBmp,
                            contentDescription = "Reload",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Frag Grenade Button (grenadeBtn.png + badge)
                val grenadeBmp = gameState.spriteRepo.get("grenadeBtn") ?: gameState.spriteRepo.get("grenade")
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xDD111822))
                        .border(1.5.dp, Color(0x77557799), CircleShape)
                        .clickable { gameState.throwGrenade(isGas = false) }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (grenadeBmp != null) {
                        Image(
                            bitmap = grenadeBmp,
                            contentDescription = "Frag Grenade",
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    // Remaining count badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xEE000000))
                            .border(1.dp, Color(0xFFFFDD44), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${player.grenadesRemaining}",
                            color = Color(0xFFFFDD44),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Gas Grenade Button
                val gasBmp = gameState.spriteRepo.get("gasNade")
                if (gasBmp != null) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xDD1B2912))
                            .border(1.5.dp, Color(0x776B8E23), CircleShape)
                            .clickable { gameState.throwGrenade(isGas = true) }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = gasBmp,
                            contentDescription = "Gas Grenade",
                            modifier = Modifier.size(30.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}
