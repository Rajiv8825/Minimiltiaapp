package com.rajiv.minimilitia.clone.engine

import android.content.Context
import com.rajiv.minimilitia.clone.assets.SpriteRepository
import com.rajiv.minimilitia.clone.audio.GameSoundManager

/**
 * Encapsulates the runtime single-player game state, tickers, and player controls.
 */
class GameLoopState(
    val context: Context,
    val soundManager: GameSoundManager,
    val spriteRepo: SpriteRepository
) {
    val physicsWorld = PhysicsWorld()
    val botController = AiBotController(physicsWorld)

    val player = SoldierEntity(
        id = "player_local",
        isPlayer = true,
        x = 1000.0f,
        y = 600.0f,
        health = 100.0f,
        maxHealth = 100.0f,
        jetpackFuel = 100.0f,
        maxFuel = 100.0f,
        equippedWeapon = WeaponCatalog.M16,
        secondaryWeapon = WeaponCatalog.DESERT_EAGLE,
        ammoInClip = WeaponCatalog.M16.magazineSize,
        reserveAmmo = WeaponCatalog.M16.magazineSize * 4,
        outfitPrefix = "sarge",
        grenadesRemaining = 3
    )

    var cameraX = 1000.0f
    var cameraY = 620.0f
    var cameraZoom = 1.0f

    val targetZoom: Float
        get() = when (player.equippedWeapon.id) {
            "sniper", "m14" -> 0.68f
            "rocket", "smaw" -> 0.68f
            "ak47", "m16" -> 0.85f
            else -> 1.0f
        }

    var isLeftStickActive = false
    var leftStickVectorX = 0.0f
    var leftStickVectorY = 0.0f

    var isRightStickActive = false
    var rightStickAngleRad = 0.0f
    var isFiring = false

    var isGameOver = false
    var respawnCooldown = 0.0f

    init {
        botController.startWave(1)
    }

    fun restartGame() {
        player.x = 1000.0f
        player.y = 600.0f
        cameraX = 1000.0f
        cameraY = 600.0f
        cameraZoom = 1.0f
        player.vx = 0.0f
        player.vy = 0.0f
        player.health = 100.0f
        player.jetpackFuel = 100.0f
        player.equippedWeapon = WeaponCatalog.M16
        player.secondaryWeapon = WeaponCatalog.DESERT_EAGLE
        player.ammoInClip = WeaponCatalog.M16.magazineSize
        player.reserveAmmo = WeaponCatalog.M16.magazineSize * 4
        player.grenadesRemaining = 3
        player.isDead = false
        player.isReloading = false
        player.deathTimer = 0.0f
        player.kills = 0
        player.score = 0
        isGameOver = false

        physicsWorld.bullets.clear()
        physicsWorld.grenades.clear()
        physicsWorld.pickups.clear()
        physicsWorld.particles.clear()
        physicsWorld.damageTexts.clear()
        physicsWorld.setupDefaultMap()

        botController.startWave(1)
    }

    fun onLeftJoystickMove(dx: Float, dy: Float) {
        leftStickVectorX = dx
        leftStickVectorY = dy
        isLeftStickActive = dx != 0.0f || dy != 0.0f

        if (!player.isDead) {
            // Horizontal speed
            player.vx = dx * PhysicsWorld.MAX_WALK_SPEED

            // Upward stick activates jetpack thrust
            val wasThrusting = player.isThrusting
            player.isThrusting = dy < -0.25f && player.jetpackFuel > 0.0f
            if (player.isThrusting && !wasThrusting) {
                soundManager.startJetpackThrust()
            } else if (!player.isThrusting && wasThrusting) {
                soundManager.stopJetpackThrust()
            }
        }
    }

    fun onLeftJoystickRelease() {
        isLeftStickActive = false
        leftStickVectorX = 0.0f
        leftStickVectorY = 0.0f
        if (player.isThrusting) {
            player.isThrusting = false
            soundManager.stopJetpackThrust()
        }
    }

    fun onRightJoystickMove(angleRad: Float, distanceRatio: Float) {
        rightStickAngleRad = angleRad
        isRightStickActive = true
        if (!player.isDead) {
            player.aimAngle = angleRad
            player.facingDirection = if (kotlin.math.cos(angleRad) >= 0) 1.0f else -1.0f
            isFiring = distanceRatio > 0.45f
        }
    }

    fun onRightJoystickRelease() {
        isRightStickActive = false
        isFiring = false
    }

    fun switchWeapon() {
        if (player.isDead) return
        val temp = player.equippedWeapon
        player.equippedWeapon = player.secondaryWeapon
        player.secondaryWeapon = temp
        player.ammoInClip = player.equippedWeapon.magazineSize
        player.reserveAmmo = player.equippedWeapon.magazineSize * 3
        player.isReloading = false
        soundManager.playWeaponSwitch()
    }

    fun reloadWeapon() {
        if (player.isDead || player.isReloading) return
        if (player.ammoInClip < player.equippedWeapon.magazineSize && player.reserveAmmo > 0) {
            player.isReloading = true
            player.reloadTimer = player.equippedWeapon.reloadTime
            soundManager.playReload()
        }
    }

    fun throwGrenade(isGas: Boolean = false) {
        if (player.isDead) return
        physicsWorld.spawnGrenade(player, isGas, soundManager)
    }

    fun tick(dt: Float) {
        // Player firing logic
        if (isFiring && !player.isDead) {
            if (player.fireCooldownTimer <= 0.0f && player.ammoInClip > 0 && !player.isReloading) {
                physicsWorld.spawnBullet(player, player.aimAngle, soundManager)
                player.ammoInClip--
                player.fireCooldownTimer = player.equippedWeapon.fireRate
            } else if (player.ammoInClip <= 0 && !player.isReloading && player.reserveAmmo > 0) {
                reloadWeapon()
            } else if (player.ammoInClip <= 0 && player.reserveAmmo <= 0) {
                soundManager.playDryFire()
            }
        }

        // Tick physics world
        physicsWorld.update(dt, player, botController.bots, soundManager)

        // Tick AI bots
        botController.update(dt, player, soundManager)

        // Smooth camera follow with velocity lookahead and aim lookahead
        val lookAheadX = if (isRightStickActive) kotlin.math.cos(player.aimAngle) * 160.0f else 0.0f
        val lookAheadY = if (isRightStickActive) kotlin.math.sin(player.aimAngle) * 110.0f else 0.0f
        val targetCamX = player.x + player.vx * 0.25f + lookAheadX
        val targetCamY = player.y + player.vy * 0.15f + lookAheadY
        cameraX += (targetCamX - cameraX) * (dt * 5.0f).coerceIn(0.02f, 0.35f)
        cameraY += (targetCamY - cameraY) * (dt * 5.0f).coerceIn(0.02f, 0.35f)
        cameraZoom += (targetZoom - cameraZoom) * (dt * 4.0f).coerceIn(0.02f, 0.25f)

        // Viewport bounds clamp
        val halfViewW = (1280.0f / cameraZoom) / 2.0f
        val halfViewH = (720.0f / cameraZoom) / 2.0f
        if (OutpostMapData.WORLD_WIDTH > halfViewW * 2f) {
            cameraX = cameraX.coerceIn(halfViewW, OutpostMapData.WORLD_WIDTH - halfViewW)
        } else {
            cameraX = OutpostMapData.WORLD_WIDTH / 2.0f
        }
        if (OutpostMapData.WORLD_HEIGHT > halfViewH * 2f) {
            cameraY = cameraY.coerceIn(halfViewH, OutpostMapData.WORLD_HEIGHT - halfViewH)
        } else {
            cameraY = OutpostMapData.WORLD_HEIGHT / 2.0f
        }

        // Player respawn timer
        if (player.isDead) {
            respawnCooldown += dt
            if (respawnCooldown >= 4.0f) {
                respawnCooldown = 0.0f
                player.x = 1000.0f
                player.y = 600.0f
                player.vx = 0.0f
                player.vy = 0.0f
                player.health = 100.0f
                player.jetpackFuel = 100.0f
                player.ammoInClip = player.equippedWeapon.magazineSize
                player.reserveAmmo = player.equippedWeapon.magazineSize * 3
                player.isDead = false
                player.isReloading = false
                soundManager.playSfx("cmonboy")
            }
        }
    }
}
