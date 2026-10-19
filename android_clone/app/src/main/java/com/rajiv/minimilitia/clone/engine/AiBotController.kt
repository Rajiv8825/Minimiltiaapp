package com.rajiv.minimilitia.clone.engine

import com.rajiv.minimilitia.clone.audio.GameSoundManager
import kotlin.math.*
import kotlin.random.Random

enum class BotState {
    PATROL,
    CHASE_PLAYER,
    JETPACK_FLY,
    AIM_AND_FIRE,
    RETREAT_RELOAD
}

/**
 * Autonomous AI decision controller for enemy soldiers in Survival Arena.
 */
class AiBotController(private val physicsWorld: PhysicsWorld) {

    val bots = mutableListOf<SoldierEntity>()
    private val botStates = mutableMapOf<String, BotState>()
    private val botDecisionTimers = mutableMapOf<String, Float>()
    
    var currentWave: Int = 1
    var waveRespawnTimer: Float = 0.0f
    private var botIdSeq = 0

    fun startWave(wave: Int) {
        currentWave = wave
        bots.clear()
        botStates.clear()
        botDecisionTimers.clear()

        val botCount = (2 + wave).coerceAtMost(6)
        val spawnPoints = OutpostMapData.BOT_SPAWNS

        val outfits = listOf("doodle_army", "sarge", "assault_armor", "agent")

        for (i in 0 until botCount) {
            val spawn = spawnPoints[i % spawnPoints.size]
            val weapon = when (wave) {
                1 -> if (i == 0) WeaponCatalog.UZI else WeaponCatalog.DESERT_EAGLE
                2 -> if (i % 2 == 0) WeaponCatalog.AK47 else WeaponCatalog.M16
                3 -> listOf(WeaponCatalog.AK47, WeaponCatalog.SHOTGUN, WeaponCatalog.M16).random()
                else -> listOf(WeaponCatalog.AK47, WeaponCatalog.SNIPER, WeaponCatalog.ROCKET, WeaponCatalog.SHOTGUN).random()
            }

            val botId = "bot_${++botIdSeq}"
            val bot = SoldierEntity(
                id = botId,
                isPlayer = false,
                x = spawn.first + (Random.nextFloat() - 0.5f) * 40.0f,
                y = spawn.second,
                health = 60.0f + wave * 10.0f,
                maxHealth = 60.0f + wave * 10.0f,
                equippedWeapon = weapon,
                ammoInClip = weapon.magazineSize,
                reserveAmmo = weapon.magazineSize * 4,
                outfitPrefix = outfits[i % outfits.size],
                grenadesRemaining = if (wave >= 2) 2 else 0
            )

            bots.add(bot)
            botStates[botId] = BotState.PATROL
            botDecisionTimers[botId] = Random.nextFloat() * 1.5f
        }
    }

    fun update(dt: Float, player: SoldierEntity, soundManager: GameSoundManager) {
        val aliveBots = bots.filter { !it.isDead }

        // Wave progression check
        if (aliveBots.isEmpty()) {
            waveRespawnTimer += dt
            if (waveRespawnTimer >= 2.5f) {
                waveRespawnTimer = 0.0f
                startWave(currentWave + 1)
                soundManager.playSfx("cmonboy")
            }
            return
        }

        if (player.isDead) {
            // Player is dead, bots celebrate / patrol
            for (bot in aliveBots) {
                bot.isThrusting = false
                bot.vx = 0.0f
            }
            return
        }

        for (bot in aliveBots) {
            var timer = botDecisionTimers[bot.id] ?: 0.0f
            timer -= dt

            val distToPlayer = hypot(player.x - bot.x, player.y - bot.y)
            val dx = player.x - bot.x
            val dy = (player.y - 30.0f) - (bot.y - 30.0f)

            // Aim angle towards player
            val baseAimAngle = atan2(dy, dx)
            val aimJitter = (sin((bot.x + bot.y).toDouble()) * 0.06).toFloat()
            bot.aimAngle = baseAimAngle + aimJitter
            bot.facingDirection = if (cos(bot.aimAngle) >= 0) 1.0f else -1.0f

            // State Machine evaluation
            var state = botStates[bot.id] ?: BotState.PATROL

            if (bot.ammoInClip <= 0 && !bot.isReloading) {
                bot.isReloading = true
                bot.reloadTimer = bot.equippedWeapon.reloadTime
                soundManager.playReload()
                state = BotState.RETREAT_RELOAD
            }

            if (timer <= 0.0f) {
                timer = Random.nextFloat() * 0.8f + 0.4f
                state = when {
                    bot.isReloading -> BotState.RETREAT_RELOAD
                    dy < -60.0f && bot.jetpackFuel > 20.0f -> BotState.JETPACK_FLY
                    distToPlayer < 650.0f -> BotState.AIM_AND_FIRE
                    else -> BotState.CHASE_PLAYER
                }
                botStates[bot.id] = state
            }
            botDecisionTimers[bot.id] = timer

            // Execute State Action
            when (state) {
                BotState.PATROL -> {
                    bot.isThrusting = false
                    bot.vx = bot.facingDirection * 120.0f
                }

                BotState.CHASE_PLAYER -> {
                    bot.vx = (if (dx > 0) 1.0f else -1.0f) * 190.0f
                    // Jump/jetpack if lower than player
                    bot.isThrusting = dy < -40.0f && bot.jetpackFuel > 15.0f
                }

                BotState.JETPACK_FLY -> {
                    bot.vx = (if (dx > 0) 1.0f else -1.0f) * 150.0f
                    bot.isThrusting = dy < -20.0f && bot.jetpackFuel > 5.0f
                    // If close and line of sight, fire while flying
                    if (distToPlayer < 500.0f && bot.fireCooldownTimer <= 0.0f && bot.ammoInClip > 0) {
                        physicsWorld.spawnBullet(bot, bot.aimAngle, soundManager)
                        bot.ammoInClip--
                        bot.fireCooldownTimer = bot.equippedWeapon.fireRate
                    }
                }

                BotState.AIM_AND_FIRE -> {
                    // Hover or strafe slowly
                    bot.vx = (if (dx > 0) 1.0f else -1.0f) * 60.0f
                    bot.isThrusting = dy < -30.0f && bot.jetpackFuel > 25.0f

                    // Fire Weapon
                    if (bot.fireCooldownTimer <= 0.0f && bot.ammoInClip > 0 && !bot.isReloading) {
                        physicsWorld.spawnBullet(bot, bot.aimAngle, soundManager)
                        bot.ammoInClip--
                        bot.fireCooldownTimer = bot.equippedWeapon.fireRate
                    }

                    // Tactical Grenade throw if player is far away
                    if (bot.grenadesRemaining > 0 && distToPlayer in 250.0f..550.0f && Random.nextFloat() < 0.005f) {
                        physicsWorld.spawnGrenade(bot, isGas = bot.equippedWeapon.id == "shotgun", soundManager)
                    }
                }

                BotState.RETREAT_RELOAD -> {
                    // Run away from player to find cover while reloading
                    bot.vx = (if (dx > 0) -1.0f else 1.0f) * 220.0f
                    bot.isThrusting = bot.jetpackFuel > 40.0f && Random.nextFloat() < 0.4f
                }
            }
        }
    }
}
