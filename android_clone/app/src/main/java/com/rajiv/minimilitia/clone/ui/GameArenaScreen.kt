package com.rajiv.minimilitia.clone.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajiv.minimilitia.clone.assets.SpriteRepository
import com.rajiv.minimilitia.clone.engine.*
import kotlin.math.*
import kotlin.random.Random
import kotlinx.coroutines.isActive

/**
 * 60 FPS Compose Canvas 2D Game Arena for Mini Militia.
 * Implements authentic 4-layer Outpost Arena (2560x1440), smooth camera follow with aim look-ahead,
 * dynamic weapon scope zoom, Chibi/Doodle soldier proportions, and off-screen radar.
 */
@Composable
fun GameArenaScreen(
    gameState: GameLoopState,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 60 FPS Delta-Time Game Loop
    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L) {
                    val dt = ((frameNanos - lastFrameNanos) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
                    gameState.tick(dt)
                }
                lastFrameNanos = frameNanos
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {

        // Canvas 2D World Renderer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val viewScale = (size.height / 720.0f) * gameState.cameraZoom
            val screenCenterX = size.width / 2.0f
            val screenCenterY = size.height / 2.0f

            // ================= 1. WORLD SPACE TRANSFORM =================
            withTransform({
                translate(screenCenterX, screenCenterY)
                scale(viewScale, viewScale, Offset.Zero)
                translate(-gameState.cameraX, -gameState.cameraY)
            }) {
                // ---------- LAYER 1: PARALLAX SKY & DISTANT HORIZON ----------
                val bgBmp = gameState.spriteRepo.get("bg_new")
                if (bgBmp != null) {
                    val bgTileW = 900
                    val totalTiles = (OutpostMapData.WORLD_WIDTH / bgTileW).toInt() + 2
                    val parallaxShift = (gameState.cameraX * 0.30f).toInt() % bgTileW
                    for (i in -1..totalTiles) {
                        drawImage(
                            image = bgBmp,
                            dstOffset = IntOffset(i * bgTileW - parallaxShift, 0),
                            dstSize = IntSize(bgTileW, OutpostMapData.WORLD_HEIGHT.toInt())
                        )
                    }
                } else {
                    drawRect(
                        color = Color(0xFF1E2838),
                        size = Size(OutpostMapData.WORLD_WIDTH, OutpostMapData.WORLD_HEIGHT)
                    )
                }

                // ---------- LAYER 2: BACKGROUND STRUCTURES, TUNNELS & PROPS ----------
                // Lower Underground Catacomb Cave Backdrop
                drawRect(
                    color = Color(0xFF18130E),
                    topLeft = Offset(520.0f, 680.0f),
                    size = Size(1520.0f, 560.0f)
                )

                // Bunker Walls & Support Pillars at Bases
                val bunkerPillarColor = Color(0xFF2E241B)
                val bunkerPillars = listOf(
                    Offset(100.0f, 680.0f), Offset(560.0f, 680.0f),
                    Offset(1960.0f, 680.0f), Offset(2420.0f, 680.0f),
                    Offset(180.0f, 400.0f), Offset(2280.0f, 400.0f)
                )
                for (pillar in bunkerPillars) {
                    drawRect(
                        color = bunkerPillarColor,
                        topLeft = pillar,
                        size = Size(36.0f, 280.0f)
                    )
                }

                // Foliage Props: Palm Trees & Bushes
                val palmBmp = gameState.spriteRepo.get("palmTree")
                val bushPalmBmp = gameState.spriteRepo.get("bushPalm")
                val bushFanBmp = gameState.spriteRepo.get("bushFan")

                for (prop in OutpostMapData.MAP_PROPS) {
                    val px = prop.first
                    val py = prop.second
                    when (prop.third) {
                        "palmTree" -> {
                            if (palmBmp != null) {
                                drawImage(
                                    image = palmBmp,
                                    dstOffset = IntOffset((px - palmBmp.width / 2f).toInt(), (py - palmBmp.height).toInt()),
                                    alpha = 0.90f
                                )
                            }
                        }
                        "bushPalm" -> {
                            if (bushPalmBmp != null) {
                                drawImage(
                                    image = bushPalmBmp,
                                    dstOffset = IntOffset((px - bushPalmBmp.width / 2f).toInt(), (py - bushPalmBmp.height).toInt()),
                                    alpha = 0.92f
                                )
                            }
                        }
                        "bushFan" -> {
                            if (bushFanBmp != null) {
                                drawImage(
                                    image = bushFanBmp,
                                    dstOffset = IntOffset((px - bushFanBmp.width / 2f).toInt(), (py - bushFanBmp.height).toInt()),
                                    alpha = 0.92f
                                )
                            }
                        }
                    }
                }

                // ---------- LAYER 3: TEXTURED EARTH/GRASS TERRAIN BLOCKS ----------
                val groundTileBmp = gameState.spriteRepo.get("tile_ground")

                for (p in gameState.physicsWorld.platforms) {
                    val cols = ceil(p.width / 64f).toInt()
                    val rows = ceil(p.height / 64f).toInt()

                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            val tileX = (p.x + c * 64f).toInt()
                            val tileY = (p.y + r * 64f).toInt()
                            val w = min(64f, p.x + p.width - tileX).toInt()
                            val h = min(64f, p.y + p.height - tileY).toInt()
                            if (w <= 0 || h <= 0) continue

                            if (groundTileBmp != null) {
                                drawImage(
                                    image = groundTileBmp,
                                    dstOffset = IntOffset(tileX, tileY),
                                    dstSize = IntSize(w, h)
                                )
                            } else {
                                drawRect(
                                    color = Color(0xFF6B5842),
                                    topLeft = Offset(tileX.toFloat(), tileY.toFloat()),
                                    size = Size(w.toFloat(), h.toFloat())
                                )
                            }
                        }
                    }

                    // Platform surface grass/moss terrain cap
                    drawLine(
                        color = Color(0xFF4C7A34), // Forest Green Grass Cap
                        start = Offset(p.x, p.y + 1f),
                        end = Offset(p.x + p.width, p.y + 1f),
                        strokeWidth = 5f
                    )
                    drawLine(
                        color = Color(0xFF7AB848), // Bright Foliage Highlight
                        start = Offset(p.x, p.y),
                        end = Offset(p.x + p.width, p.y),
                        strokeWidth = 2.5f
                    )
                }

                // ---------- LAYER 4: INTERACTIVE WEAPON & HEALTH SPAWNERS ----------
                val bob = sin((System.currentTimeMillis() % 2000) / 1000f * Math.PI.toFloat() * 2f) * 5f
                for (pickup in gameState.physicsWorld.pickups) {
                    // Glowing circular spawn pad
                    drawCircle(
                        color = Color(0x55FFCC00),
                        radius = 24.0f,
                        center = Offset(pickup.x, pickup.y + 2.0f)
                    )
                    drawCircle(
                        color = Color(0x33FFCC00),
                        radius = 32.0f,
                        center = Offset(pickup.x, pickup.y + 2.0f)
                    )

                    // Bobbing Weapon Sprite
                    val weaponBmp = gameState.spriteRepo.get(pickup.weapon.spriteKey)
                    if (weaponBmp != null) {
                        drawImage(
                            image = weaponBmp,
                            dstOffset = IntOffset(
                                (pickup.x - weaponBmp.width / 2f).toInt(),
                                (pickup.y - weaponBmp.height + bob).toInt()
                            )
                        )
                    } else {
                        drawCircle(
                            color = Color(0xFFFFCC00),
                            radius = 12f,
                            center = Offset(pickup.x, pickup.y - 10f + bob)
                        )
                    }
                }

                // Grenades
                for (g in gameState.physicsWorld.grenades) {
                    val nadeKey = if (g.isGas) "gasNade" else "grenade"
                    val nadeBmp = gameState.spriteRepo.get(nadeKey)
                    if (nadeBmp != null) {
                        withTransform({
                            rotate(Math.toDegrees(g.rotation.toDouble()).toFloat(), Offset(g.x, g.y))
                        }) {
                            drawImage(
                                image = nadeBmp,
                                dstOffset = IntOffset(
                                    (g.x - nadeBmp.width / 2f).toInt(),
                                    (g.y - nadeBmp.height / 2f).toInt()
                                )
                            )
                        }
                    } else {
                        drawCircle(
                            color = if (g.isGas) Color(0xFFDDDD33) else Color(0xFF33AA33),
                            radius = 7f,
                            center = Offset(g.x, g.y)
                        )
                    }
                }

                // Bullets & Rockets
                for (b in gameState.physicsWorld.bullets) {
                    if (b.isRocket) {
                        val rocketBmp = gameState.spriteRepo.get("rocket")
                        val angleDeg = Math.toDegrees(atan2(b.vy.toDouble(), b.vx.toDouble())).toFloat()
                        if (rocketBmp != null) {
                            withTransform({
                                rotate(angleDeg, Offset(b.x, b.y))
                            }) {
                                drawImage(
                                    image = rocketBmp,
                                    dstOffset = IntOffset(
                                        (b.x - rocketBmp.width / 2f).toInt(),
                                        (b.y - rocketBmp.height / 2f).toInt()
                                    )
                                )
                            }
                        } else {
                            drawCircle(color = Color.Red, radius = 6f, center = Offset(b.x, b.y))
                        }
                    } else {
                        val length = 22f
                        val angle = atan2(b.vy, b.vx)
                        val tailX = b.x - cos(angle) * length
                        val tailY = b.y - sin(angle) * length
                        drawLine(
                            color = Color(0xFFFFEE44),
                            start = Offset(tailX, tailY),
                            end = Offset(b.x, b.y),
                            strokeWidth = 3f
                        )
                    }
                }

                // Particles (Flames, Explosions, Shells, Smoke)
                for (p in gameState.physicsWorld.particles) {
                    when (p.type) {
                        ParticleType.JETPACK_FLAME -> {
                            val flameBmp = if ((System.currentTimeMillis() / 70) % 2 == 0L) {
                                gameState.spriteRepo.get("flame1") ?: gameState.spriteRepo.get("flame")
                            } else {
                                gameState.spriteRepo.get("flame2") ?: gameState.spriteRepo.get("flame")
                            }
                            if (flameBmp != null) {
                                drawImage(
                                    image = flameBmp,
                                    dstOffset = IntOffset((p.x - 10f).toInt(), (p.y - 10f).toInt()),
                                    dstSize = IntSize((20 * p.scale).toInt(), (24 * p.scale).toInt()),
                                    alpha = p.alpha
                                )
                            } else {
                                drawCircle(color = Color(0xFFFFAA00), radius = 6f * p.scale, center = Offset(p.x, p.y), alpha = p.alpha)
                            }
                        }
                        ParticleType.EXPLOSION -> {
                            val expBmp = gameState.spriteRepo.get("explosion")
                            if (expBmp != null) {
                                val s = (expBmp.width * p.scale).toInt()
                                drawImage(
                                    image = expBmp,
                                    dstOffset = IntOffset((p.x - s / 2f).toInt(), (p.y - s / 2f).toInt()),
                                    dstSize = IntSize(s, s),
                                    alpha = p.alpha
                                )
                            } else {
                                drawCircle(color = Color(0xFFFF4400), radius = 24f * p.scale, center = Offset(p.x, p.y), alpha = p.alpha)
                            }
                        }
                        ParticleType.SMOKE, ParticleType.GAS_CLOUD -> {
                            val smokeBmp = gameState.spriteRepo.get("smoke")
                            if (smokeBmp != null) {
                                val s = (smokeBmp.width * p.scale).toInt()
                                drawImage(
                                    image = smokeBmp,
                                    dstOffset = IntOffset((p.x - s / 2f).toInt(), (p.y - s / 2f).toInt()),
                                    dstSize = IntSize(s, s),
                                    alpha = p.alpha * 0.7f
                                )
                            } else {
                                drawCircle(
                                    color = if (p.type == ParticleType.GAS_CLOUD) Color(0x99AACC22) else Color(0x66888888),
                                    radius = 18f * p.scale,
                                    center = Offset(p.x, p.y),
                                    alpha = p.alpha
                                )
                            }
                        }
                        ParticleType.BLOOD -> {
                            val bloodBmp = gameState.spriteRepo.get("blood_new")
                            if (bloodBmp != null) {
                                drawImage(
                                    image = bloodBmp,
                                    dstOffset = IntOffset((p.x - 8f).toInt(), (p.y - 8f).toInt()),
                                    alpha = p.alpha
                                )
                            } else {
                                drawCircle(color = Color(0xCCBB1111), radius = 4f * p.scale, center = Offset(p.x, p.y), alpha = p.alpha)
                            }
                        }
                        ParticleType.SHELL_CASING -> {
                            val rotAngle = p.lifetime * 900f
                            withTransform({
                                rotate(rotAngle, Offset(p.x, p.y))
                            }) {
                                drawRect(
                                    color = Color(0xFFFFD700),
                                    topLeft = Offset(p.x - 3f, p.y - 1.5f),
                                    size = Size(6f * p.scale, 3f * p.scale),
                                    alpha = p.alpha
                                )
                            }
                        }
                        ParticleType.MUZZLE_FLASH -> {
                            drawCircle(
                                color = Color(0xFFFFF077),
                                radius = 10f * p.scale,
                                center = Offset(p.x, p.y),
                                alpha = p.alpha
                            )
                            drawCircle(
                                color = Color(0xFFFF6600),
                                radius = 5f * p.scale,
                                center = Offset(p.x, p.y),
                                alpha = p.alpha
                            )
                        }
                        ParticleType.BULLET_SPARK -> {
                            drawCircle(
                                color = Color(0xFFFFFF88),
                                radius = 3f * p.scale,
                                center = Offset(p.x, p.y),
                                alpha = p.alpha
                            )
                        }
                    }
                }

                // ---------- SKELETAL DOODLE SOLDIERS (Player + AI Bots) ----------
                val allSoldiers = mutableListOf<SoldierEntity>().apply {
                    addAll(gameState.botController.bots)
                    add(gameState.player)
                }

                for (s in allSoldiers) {
                    if (s.isDead) continue
                    drawDoodleSoldier(
                        soldier = s,
                        spriteRepo = gameState.spriteRepo
                    )
                }

                // Signature Laser Sight for Player
                if (!gameState.player.isDead) {
                    val p = gameState.player
                    val isFacingLeft = p.facingDirection < 0.0f
                    val shoulderX = p.x + (if (isFacingLeft) -6f else 6f)
                    val shoulderY = p.y - 36f
                    val cosA = cos(p.aimAngle)
                    val sinA = sin(p.aimAngle)
                    val barrelDist = p.equippedWeapon.barrelOffsetX
                    val muzzleX = shoulderX + cosA * barrelDist
                    val muzzleY = shoulderY + sinA * barrelDist

                    val sightDist = when (p.equippedWeapon.id) {
                        "sniper", "m14" -> 540f
                        "rocket", "smaw" -> 380f
                        else -> 300f
                    }
                    val targetX = muzzleX + cosA * sightDist
                    val targetY = muzzleY + sinA * sightDist

                    // Outer laser glow
                    drawLine(
                        color = Color(0x33FF2222),
                        start = Offset(muzzleX, muzzleY),
                        end = Offset(targetX, targetY),
                        strokeWidth = 3.5f
                    )
                    // Core bright laser line
                    drawLine(
                        color = Color(0xCCFF4444),
                        start = Offset(muzzleX, muzzleY),
                        end = Offset(targetX, targetY),
                        strokeWidth = 1.5f
                    )

                    // Target Reticle (aim.png)
                    val aimBmp = gameState.spriteRepo.get("aim")
                    if (aimBmp != null) {
                        drawImage(
                            image = aimBmp,
                            dstOffset = IntOffset((targetX - aimBmp.width / 2f).toInt(), (targetY - aimBmp.height / 2f).toInt())
                        )
                    } else {
                        drawCircle(color = Color(0xCCFF2222), radius = 8f, center = Offset(targetX, targetY))
                        drawCircle(color = Color.White, radius = 2f, center = Offset(targetX, targetY))
                    }
                }

                // Floating Damage Numbers
                val nativePaint = android.graphics.Paint().apply {
                    textSize = 22f
                    isFakeBoldText = true
                    setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                }
                for (dt in gameState.physicsWorld.damageTexts) {
                    nativePaint.color = dt.color.toInt()
                    nativePaint.alpha = ((dt.lifetime / dt.maxLifetime) * 255).toInt().coerceIn(0, 255)
                    drawContext.canvas.nativeCanvas.drawText(dt.text, dt.x, dt.y, nativePaint)
                }
            }

            // ================= 2. SCREEN SPACE OVERLAYS =================
            // Off-Screen Enemy Threat Radar Indicators
            val screenMargin = 34f
            for (bot in gameState.botController.bots) {
                if (bot.isDead) continue

                val sx = screenCenterX + (bot.x - gameState.cameraX) * viewScale
                val sy = screenCenterY + (bot.y - gameState.cameraY) * viewScale

                val isOffScreen = sx < screenMargin || sx > size.width - screenMargin ||
                        sy < screenMargin || sy > size.height - screenMargin

                if (isOffScreen) {
                    val angle = atan2(sy - screenCenterY, sx - screenCenterX)
                    val cosA = cos(angle)
                    val sinA = sin(angle)

                    val maxExtentX = (size.width / 2f) - screenMargin
                    val maxExtentY = (size.height / 2f) - screenMargin

                    val scaleX = if (cosA != 0f) abs(maxExtentX / cosA) else Float.MAX_VALUE
                    val scaleY = if (sinA != 0f) abs(maxExtentY / sinA) else Float.MAX_VALUE
                    val intersectDist = min(scaleX, scaleY)

                    val indX = screenCenterX + cosA * intersectDist
                    val indY = screenCenterY + sinA * intersectDist

                    val arrowDeg = Math.toDegrees(angle.toDouble()).toFloat()
                    withTransform({
                        rotate(arrowDeg, Offset(indX, indY))
                    }) {
                        val chevron = Path().apply {
                            moveTo(indX + 12f, indY)
                            lineTo(indX - 8f, indY - 9f)
                            lineTo(indX - 4f, indY)
                            lineTo(indX - 8f, indY + 9f)
                            close()
                        }
                        drawPath(path = chevron, color = Color(0xEEFF2222))
                        drawCircle(color = Color.White, radius = 2.5f, center = Offset(indX, indY))
                    }
                }
            }
        }

        // Multi-Touch Dual Joysticks & Controls Overlay
        MultiTouchHudOverlay(
            gameState = gameState,
            onExitToMenu = onExitToMenu
        )

        // ================= ELIMINATION / RESPAWN OVERLAY =================
        if (gameState.player.isDead) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "YOU WERE ELIMINATED",
                        color = Color(0xFFFF2222),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "RESPAWNING IN ${max(1, 4 - gameState.respawnCooldown.toInt())}...",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { gameState.restartGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AA44))
                        ) {
                            Text("RESPAWN NOW", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onExitToMenu() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF666666))
                        ) {
                            Text("MAIN MENU", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders an authentic Doodle Army soldier with signature Chibi proportions:
 * Large expressive head (~90-100% of torso height) atop compact armored body.
 * Draw order: Back Arm -> Jetpack -> Back Leg -> Torso -> Front Leg -> Head -> Weapon -> Front Arm.
 * Boots emit downward thrust flames when flying.
 */
private fun DrawScope.drawDoodleSoldier(
    soldier: SoldierEntity,
    spriteRepo: SpriteRepository
) {
    val prefix = soldier.outfitPrefix
    val legBmp = spriteRepo.get("${prefix}_leg") ?: spriteRepo.get("sarge_leg") ?: spriteRepo.get("agent_leg")
    val bodyBmp = spriteRepo.get("${prefix}_body") ?: spriteRepo.get("sarge_body") ?: spriteRepo.get("agent_body")
    val headBmp = spriteRepo.get("${prefix}_head") ?: spriteRepo.get("sarge_head") ?: spriteRepo.get("agent_head")
    val armBmp = spriteRepo.get("${prefix}_arm") ?: spriteRepo.get("sarge_arm") ?: spriteRepo.get("agent_arm")
    val packBmp = spriteRepo.get("boost")
    val flame1Bmp = spriteRepo.get("flame1")
    val flame2Bmp = spriteRepo.get("flame2")
    val weaponBmp = spriteRepo.get(soldier.equippedWeapon.spriteKey)

    val isFacingLeft = soldier.facingDirection < 0.0f
    val posX = soldier.x
    val posY = soldier.y

    // Authentic Chibi / Doodle proportions:
    // Large head (~56x54) sitting on compact torso (~46x46) and short legs (~28x32)
    val legH = 30.0f
    val legW = 26.0f
    val bodyH = 46.0f
    val bodyW = 44.0f
    val headH = 52.0f
    val headW = 56.0f
    val torsoY = posY - legH - bodyH + 10.0f

    // Dynamic Walk Cycle & Flight Poses
    val isAirborne = !soldier.isGrounded
    val walkOffset = if (!isAirborne && abs(soldier.vx) > 10f) {
        sin(soldier.walkFrameTimer * Math.PI.toFloat() * 4f) * 4f
    } else 0f

    val frontLegAngle = when {
        isAirborne -> -22.0f // Tilted backward in flight
        abs(soldier.vx) > 10f -> sin(soldier.walkFrameTimer * Math.PI.toFloat() * 4f) * 24.0f
        else -> 0.0f
    }
    val backLegAngle = when {
        isAirborne -> -35.0f // Tilted further backward in flight
        abs(soldier.vx) > 10f -> -sin(soldier.walkFrameTimer * Math.PI.toFloat() * 4f) * 24.0f
        else -> 0.0f
    }

    // Effective 360° Aim Angle for Arm & Weapon
    val effectiveAimAngle = if (isFacingLeft) {
        Math.PI.toFloat() - soldier.aimAngle
    } else {
        soldier.aimAngle
    }
    val armAngleDeg = Math.toDegrees(effectiveAimAngle.toDouble()).toFloat()

    withTransform({
        if (isFacingLeft) {
            scale(-1.0f, 1.0f, Offset(posX, posY))
        }
    }) {
        // 1. Back Arm (Layer 1)
        if (armBmp != null) {
            withTransform({
                rotate((armAngleDeg * 0.45f - 14.0f).coerceIn(-40f, 40f), Offset(posX - 4f, torsoY + 16f))
            }) {
                drawImage(
                    image = armBmp,
                    dstOffset = IntOffset((posX - 10f).toInt(), (torsoY + 12f).toInt()),
                    dstSize = IntSize(34, 18),
                    alpha = 0.85f
                )
            }
        }

        // 2. Jetpack Backpack Strapped to Back (Layer 2)
        if (packBmp != null) {
            drawImage(
                image = packBmp,
                dstOffset = IntOffset((posX - 22f).toInt(), (torsoY + 8f).toInt()),
                dstSize = IntSize(16, 32)
            )
        }

        // 3. Back Leg & Boot (Layer 3)
        if (legBmp != null) {
            withTransform({
                rotate(backLegAngle, Offset(posX - 4f, posY - legH))
            }) {
                drawImage(
                    image = legBmp,
                    dstOffset = IntOffset((posX - legW / 2f - 4f).toInt(), (posY - legH - walkOffset).toInt()),
                    dstSize = IntSize(legW.toInt(), legH.toInt()),
                    alpha = 0.82f
                )
            }
        }

        // 4. Torso + Military Vest (Layer 4)
        if (bodyBmp != null) {
            drawImage(
                image = bodyBmp,
                dstOffset = IntOffset((posX - bodyW / 2f).toInt(), torsoY.toInt()),
                dstSize = IntSize(bodyW.toInt(), bodyH.toInt())
            )
        }

        // 5. Front Leg & Boot (Layer 5)
        if (legBmp != null) {
            withTransform({
                rotate(frontLegAngle, Offset(posX + 4f, posY - legH))
            }) {
                drawImage(
                    image = legBmp,
                    dstOffset = IntOffset((posX - legW / 2f + 2f).toInt(), (posY - legH + walkOffset).toInt()),
                    dstSize = IntSize(legW.toInt(), legH.toInt())
                )
            }
        }

        // 6. Large Chibi Doodle Head + Helmet (Layer 6)
        if (headBmp != null) {
            // Tilts slightly toward aim angle (clamped to ±25°)
            val headTilt = (armAngleDeg * 0.16f).coerceIn(-25f, 25f)
            withTransform({
                rotate(headTilt, Offset(posX, torsoY - 2f))
            }) {
                drawImage(
                    image = headBmp,
                    dstOffset = IntOffset((posX - headW / 2f).toInt(), (torsoY - headH + 8f).toInt()),
                    dstSize = IntSize(headW.toInt(), headH.toInt())
                )
            }
        }

        // 7. Weapon (Layer 7) & 8. Front Arm & Hand (Layer 8)
        withTransform({
            rotate(armAngleDeg, Offset(posX + 6f, torsoY + 16f))
        }) {
            // Front Arm gripping weapon
            if (armBmp != null) {
                drawImage(
                    image = armBmp,
                    dstOffset = IntOffset(posX.toInt(), (torsoY + 10f).toInt()),
                    dstSize = IntSize(34, 18)
                )
            }
            // Equipped Weapon Sprite
            if (weaponBmp != null) {
                drawImage(
                    image = weaponBmp,
                    dstOffset = IntOffset((posX + 12f).toInt(), (torsoY + 16f - weaponBmp.height / 2f).toInt())
                )
            }
        }
    }

    // Rocket Boot Thrusters: Underneath BOTH boots
    if (soldier.isThrusting) {
        val flameBmp = if ((System.currentTimeMillis() / 70) % 2 == 0L) flame1Bmp else flame2Bmp
        if (flameBmp != null) {
            val flameAngle = (soldier.vx * -0.06f).coerceIn(-20f, 20f)
            val flameH = 24.0f
            val flameW = 16.0f

            // Left Boot Flame
            withTransform({
                rotate(flameAngle, Offset(posX - 8f, posY + 2f))
            }) {
                drawImage(
                    image = flameBmp,
                    dstOffset = IntOffset((posX - 16f).toInt(), (posY + 2f).toInt()),
                    dstSize = IntSize(flameW.toInt(), flameH.toInt())
                )
            }
            // Right Boot Flame
            withTransform({
                rotate(flameAngle, Offset(posX + 6f, posY + 2f))
            }) {
                drawImage(
                    image = flameBmp,
                    dstOffset = IntOffset((posX - 2f).toInt(), (posY + 2f).toInt()),
                    dstSize = IntSize(flameW.toInt(), flameH.toInt())
                )
            }
        }
    }

    // Overhead Team Health Bar
    val barWidth = 44f
    val barHeight = 5f
    val barX = posX - barWidth / 2f
    val barY = torsoY - headH + 2f

    drawRect(
        color = Color(0xAA000000),
        topLeft = Offset(barX - 1f, barY - 1f),
        size = Size(barWidth + 2f, barHeight + 2f)
    )
    val healthRatio = (soldier.health / soldier.maxHealth).coerceIn(0f, 1f)
    drawRect(
        color = if (soldier.isPlayer) Color(0xFF00EE55) else Color(0xFFFF3333),
        topLeft = Offset(barX, barY),
        size = Size(barWidth * healthRatio, barHeight)
    )
}
