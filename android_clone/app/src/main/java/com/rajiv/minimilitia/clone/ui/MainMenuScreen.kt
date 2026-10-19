package com.rajiv.minimilitia.clone.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajiv.minimilitia.clone.assets.SpriteRepository
import com.rajiv.minimilitia.clone.audio.GameSoundManager
import com.rajiv.minimilitia.clone.engine.WeaponCatalog

enum class AppScreen {
    MAIN_MENU,
    SURVIVAL_ARENA,
    TRAINING_RANGE,
    CATACOMBS_ARENA,
    ARMORY_INSPECTOR
}

@Composable
fun MainMenuScreen(
    spriteRepo: SpriteRepository,
    soundManager: GameSoundManager,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        soundManager.startThemeMusic()
    }

    val menuBg = spriteRepo.get("MenuBackground")
    val logoGame = spriteRepo.get("Logo_Game")

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Authentic Menu Background Image
        if (menuBg != null) {
            Image(
                bitmap = menuBg,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Dark gradient overlay for optimal tactical contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88060B12))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                        )
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // ================= 1. TOP HEADER: AUTHENTIC LOGO_GAME =================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (logoGame != null) {
                    Image(
                        bitmap = logoGame,
                        contentDescription = "Mini Militia",
                        modifier = Modifier
                            .width(240.dp)
                            .height(52.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = "MINI MILITIA",
                        color = Color(0xFFFFCC00),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
                Text(
                    text = "DOODLE ARMY 2 • NATIVE KOTLIN ENGINE",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // ================= 2. MAIN CONTENT ROW (LEFT: AVATAR CARD | RIGHT: 4 BUTTONS) =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT SIDE: Officer Sarge Graphic + Live Soldier Preview
                SoldierPreviewCard(
                    spriteRepo = spriteRepo,
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight(0.96f)
                )

                Spacer(modifier = Modifier.width(20.dp))

                // RIGHT SIDE: 4 Authentic Military Stencil Plate Buttons
                Column(
                    modifier = Modifier
                        .width(420.dp)
                        .fillMaxHeight(0.96f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Button 1: Survival (Sarge's Training)
                    MilitaryStencilButton(
                        title = "SURVIVAL (SARGE'S TRAINING)",
                        subtitle = "Endless bot waves in Outpost • Solo survival",
                        accentColor = Color(0xFF22C55E),
                        iconText = "★",
                        onClick = {
                            soundManager.stopThemeMusic()
                            soundManager.playSfx("cmonboy")
                            onNavigate(AppScreen.SURVIVAL_ARENA)
                        }
                    )

                    // Button 2: Deathmatch vs Bots (Outpost)
                    MilitaryStencilButton(
                        title = "DEATHMATCH VS BOTS (OUTPOST)",
                        subtitle = "Tactical arena combat • High-tier weapon spawns",
                        accentColor = Color(0xFF00AAFF),
                        iconText = "⚔",
                        onClick = {
                            soundManager.stopThemeMusic()
                            soundManager.playSfx("readyup")
                            onNavigate(AppScreen.SURVIVAL_ARENA)
                        }
                    )

                    // Button 3: Catacombs Arena
                    MilitaryStencilButton(
                        title = "CATACOMBS ARENA",
                        subtitle = "Close-quarters underground combat",
                        accentColor = Color(0xFFF59E0B),
                        iconText = "☠",
                        onClick = {
                            soundManager.stopThemeMusic()
                            soundManager.playSfx("getsome")
                            onNavigate(AppScreen.CATACOMBS_ARENA)
                        }
                    )

                    // Button 4: Armory & Avatar
                    MilitaryStencilButton(
                        title = "ARMORY & AVATAR",
                        subtitle = "Inspect 1,857 extracted sprites & authentic SFX",
                        accentColor = Color(0xFFFF7700),
                        iconText = "⚙",
                        onClick = {
                            soundManager.playSfx("switch")
                            onNavigate(AppScreen.ARMORY_INSPECTOR)
                        }
                    )
                }
            }

            // ================= 3. ARCHITECTURE FOOTER =================
            Text(
                text = "AUTHENTIC COCOS2D-X ASSETS • 2560x1440 OUTPOST ARENA • 60 FPS COMPOSE CANVAS",
                color = Color(0x77FFFFFF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Live Interactive Doodle Soldier & Officer Sarge Preview Card
 */
@Composable
fun SoldierPreviewCard(
    spriteRepo: SpriteRepository,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breatheOffset by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheAnim"
    )

    Card(
        modifier = modifier
            .border(1.5.dp, Color(0x663B82F6), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xDD0D1522)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SARGE'S SQUAD",
                    color = Color(0xFFFFCC00),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "LVL 42 • VETERAN",
                    color = Color(0xFF38BDF8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Center Area: Officer Sarge Portrait + Live Animated Soldier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Officer Sarge Badge
                val sargeIcon = spriteRepo.get("sarge_icon") ?: spriteRepo.get("sarge")
                if (sargeIcon != null) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFFFFCC00), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = sargeIcon,
                            contentDescription = "Officer Sarge",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Live Doodle Soldier Drawing
                Canvas(
                    modifier = Modifier.size(width = 110.dp, height = 120.dp)
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f + 4f

                    val headBmp = spriteRepo.getBitmap("sarge_head") ?: spriteRepo.getBitmap("agent_head") ?: spriteRepo.getBitmap("doodle_army_head")
                    val bodyBmp = spriteRepo.getBitmap("sarge_body") ?: spriteRepo.getBitmap("agent_body")
                    val legBmp = spriteRepo.getBitmap("sarge_leg") ?: spriteRepo.getBitmap("agent_leg")
                    val armBmp = spriteRepo.getBitmap("sarge_arm") ?: spriteRepo.getBitmap("agent_arm")
                    val wepBmp = spriteRepo.getBitmap("m16") ?: spriteRepo.getBitmap("ak47")
                    val packBmp = spriteRepo.getBitmap("pack")

                    drawContext.canvas.nativeCanvas.save()
                    drawContext.canvas.nativeCanvas.scale(1.9f, 1.9f, cx, cy)

                    // 1. Back Arm
                    armBmp?.let { bmp ->
                        val dst = android.graphics.RectF(cx + 8f, cy - 18f + breatheOffset, cx + 32f, cy + 6f + breatheOffset)
                        drawContext.canvas.nativeCanvas.drawBitmap(bmp, null, dst, null)
                    }

                    // 2. Jetpack Backpack
                    packBmp?.let { bmp ->
                        val dst = android.graphics.RectF(cx - 30f, cy - 22f + breatheOffset, cx - 10f, cy + 12f + breatheOffset)
                        drawContext.canvas.nativeCanvas.drawBitmap(bmp, null, dst, null)
                    }

                    // 3. Legs
                    legBmp?.let { bmp ->
                        val dstL = android.graphics.RectF(cx - 16f, cy + 10f, cx - 2f, cy + 42f)
                        drawContext.canvas.nativeCanvas.drawBitmap(bmp, null, dstL, null)
                        val dstR = android.graphics.RectF(cx + 2f, cy + 10f, cx + 16f, cy + 42f)
                        drawContext.canvas.nativeCanvas.drawBitmap(bmp, null, dstR, null)
                    }

                    // 4. Torso
                    bodyBmp?.let { bmp ->
                        val dst = android.graphics.RectF(cx - 18f, cy - 14f + breatheOffset, cx + 18f, cy + 18f + breatheOffset)
                        drawContext.canvas.nativeCanvas.drawBitmap(bmp, null, dst, null)
                    }

                    // 5. Head
                    headBmp?.let { bmp ->
                        val dst = android.graphics.RectF(cx - 24f, cy - 50f + breatheOffset, cx + 24f, cy - 8f + breatheOffset)
                        drawContext.canvas.nativeCanvas.drawBitmap(bmp, null, dst, null)
                    }

                    // 6. Weapon
                    wepBmp?.let { bmp ->
                        val dst = android.graphics.RectF(cx - 4f, cy - 8f + breatheOffset, cx + 44f, cy + 14f + breatheOffset)
                        drawContext.canvas.nativeCanvas.drawBitmap(bmp, null, dst, null)
                    }

                    // 7. Front Arm
                    armBmp?.let { bmp ->
                        val dst = android.graphics.RectF(cx - 8f, cy - 12f + breatheOffset, cx + 16f, cy + 12f + breatheOffset)
                        drawContext.canvas.nativeCanvas.drawBitmap(bmp, null, dst, null)
                    }

                    drawContext.canvas.nativeCanvas.restore()
                }
            }

            // Bottom Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STATUS: COMBAT READY",
                    color = Color(0xFF22C55E),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "CHIEF WARRANT OFFICER",
                    color = Color(0xFFA0AEC0),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Authentic Military Stencil Plate Button
 */
@Composable
fun MilitaryStencilButton(
    title: String,
    subtitle: String,
    accentColor: Color,
    iconText: String = "▶",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() }
            .border(1.5.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xDD0E1724)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tactical Icon Box
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.2f))
                        .border(1.dp, accentColor, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconText,
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Text
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        maxLines = 1
                    )
                    Text(
                        text = subtitle,
                        color = Color(0xFFA0AEC0),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            // Right Chevron Accent Arrow
            Text(
                text = "»",
                color = accentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun ArmoryInspectorScreen(
    spriteRepo: SpriteRepository,
    soundManager: GameSoundManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedWeapon by remember { mutableStateOf(WeaponCatalog.M16) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
            ) {
                Text("BACK TO MENU", fontWeight = FontWeight.Bold)
            }
            Text(
                text = "ARMORY & REVERSE-ENGINEERED ASSETS",
                color = Color(0xFFFFCC00),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "1,857 SPRITES • 113 SFX",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Weapon Selector Carousel
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(WeaponCatalog.ALL_WEAPONS) { weapon ->
                val isSelected = weapon.id == selectedWeapon.id
                Card(
                    modifier = Modifier
                        .width(140.dp)
                        .height(80.dp)
                        .clickable {
                            selectedWeapon = weapon
                            soundManager.playWeaponFire(weapon.id)
                        }
                        .border(
                            2.dp,
                            if (isSelected) Color(0xFFFFCC00) else Color(0x33FFFFFF),
                            RoundedCornerShape(8.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0B1120)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val bmp = spriteRepo.get(weapon.spriteKey)
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = weapon.displayName,
                                modifier = Modifier.size(50.dp, 30.dp)
                            )
                        }
                        Text(
                            text = weapon.displayName,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Weapon Details Card
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Weapon Blueprint Preview
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val bmp = spriteRepo.get(selectedWeapon.spriteKey)
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = selectedWeapon.displayName,
                            modifier = Modifier.size(180.dp, 100.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = selectedWeapon.displayName,
                        color = Color(0xFFFFCC00),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { soundManager.playWeaponFire(selectedWeapon.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                    ) {
                        Text("TEST FIRE SFX", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Specs & Ballistics Telemetry
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("BALLISTICS & RE TELEMETRY", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Black)
                    StatRow("Base Damage", "${selectedWeapon.damage.toInt()} HP")
                    StatRow("Rate of Fire", "${(1.0f / selectedWeapon.fireRate).toInt()} RPM")
                    StatRow("Magazine Size", "${selectedWeapon.magazineSize} rounds")
                    StatRow("Reload Duration", "${selectedWeapon.reloadTime}s")
                    StatRow("Muzzle Velocity", "${selectedWeapon.bulletVelocity.toInt()} px/s")
                    StatRow("Accuracy Spread", "${selectedWeapon.spreadAngleDeg}°")
                    StatRow("Splash Radius", if (selectedWeapon.splashRadius > 0) "${selectedWeapon.splashRadius.toInt()} px (AOE)" else "None (Kinetic)")
                    StatRow("Firing Mode", if (selectedWeapon.isAutomatic) "Full-Auto" else "Semi-Auto")
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
