package com.rajiv.minimilitia.clone.engine

import com.rajiv.minimilitia.clone.audio.GameSoundManager
import kotlin.math.*
import kotlin.random.Random

/**
 * 2D Physics and Collision Simulation Engine for Mini Militia.
 * Operates in 1280x720 logical coordinates.
 */
class PhysicsWorld {

    companion object {
        const val ARENA_WIDTH = OutpostMapData.WORLD_WIDTH
        const val ARENA_HEIGHT = OutpostMapData.WORLD_HEIGHT
        const val GRAVITY = 980.0f
        const val THRUST_ACCEL = -1650.0f
        const val MAX_WALK_SPEED = 260.0f
        const val JETPACK_BURN_RATE = 32.0f
        const val JETPACK_REGEN_RATE = 22.0f
        const val SOLDIER_WIDTH = 48.0f
        const val SOLDIER_HEIGHT = 70.0f
        const val FLOOR_Y = OutpostMapData.FLOOR_Y
    }

    val platforms = mutableListOf<Platform>()
    val bullets = mutableListOf<BulletEntity>()
    val grenades = mutableListOf<GrenadeEntity>()
    val pickups = mutableListOf<WeaponPickupEntity>()
    val particles = mutableListOf<ParticleEffect>()
    val damageTexts = mutableListOf<FloatingDamageText>()

    private var bulletIdCounter = 0L
    private var grenadeIdCounter = 0L
    private var pickupIdCounter = 0L

    init {
        setupDefaultMap()
    }

    fun setupDefaultMap() {
        platforms.clear()
        // Load authentic Outpost map platforms
        platforms.addAll(OutpostMapData.PLATFORMS)

        // Spawn weapon pickups from authentic Outpost weapon caches
        pickups.clear()
        val variedWeapons = listOf(
            WeaponCatalog.SNIPER,
            WeaponCatalog.AK47,
            WeaponCatalog.ROCKET,
            WeaponCatalog.SHOTGUN,
            WeaponCatalog.M14,
            WeaponCatalog.UZI,
            WeaponCatalog.DESERT_EAGLE,
            WeaponCatalog.SAWGUN,
            WeaponCatalog.FLAMETHROWER,
            WeaponCatalog.MAGNUM,
            WeaponCatalog.M16
        )
        for ((idx, spawn) in OutpostMapData.WEAPON_SPAWNS.withIndex()) {
            val weapon = variedWeapons[idx % variedWeapons.size]
            spawnPickup(weapon, spawn.first, spawn.second - 10.0f)
        }
    }

    fun spawnBullet(shooter: SoldierEntity, aimAngleRad: Float, soundManager: GameSoundManager) {
        val weapon = shooter.equippedWeapon
        val shoulderX = shooter.x + 6.0f * shooter.facingDirection
        val shoulderY = shooter.y - 34.0f

        val pellets = weapon.pelletCount
        for (i in 0 until pellets) {
            val spreadRad = Math.toRadians((Random.nextFloat() - 0.5) * weapon.spreadAngleDeg).toFloat()
            val finalAngle = aimAngleRad + spreadRad
            val cosA = cos(finalAngle)
            val sinA = sin(finalAngle)

            val spawnX = shoulderX + cosA * weapon.barrelOffsetX
            val spawnY = shoulderY + sinA * weapon.barrelOffsetX

            val speed = weapon.bulletVelocity
            val bullet = BulletEntity(
                id = ++bulletIdCounter,
                shooterId = shooter.id,
                x = spawnX,
                y = spawnY,
                vx = cosA * speed,
                vy = sinA * speed,
                damage = weapon.damage,
                splashRadius = weapon.splashRadius,
                isRocket = weapon.id == "rocket",
                lifetime = 2.5f
            )
            bullets.add(bullet)

            // Muzzle flash particle at tip
            particles.add(
                ParticleEffect(
                    x = spawnX,
                    y = spawnY,
                    vx = cosA * 20.0f,
                    vy = sinA * 20.0f,
                    lifetime = 0.06f,
                    maxLifetime = 0.06f,
                    scale = 1.0f,
                    type = ParticleType.MUZZLE_FLASH
                )
            )
        }

        // Eject spinning brass shell casing opposite to fire direction
        val ejectAngle = aimAngleRad + Math.PI.toFloat() + (Random.nextFloat() - 0.5f) * 0.5f
        particles.add(
            ParticleEffect(
                x = shoulderX,
                y = shoulderY,
                vx = cos(ejectAngle) * (Random.nextFloat() * 120.0f + 60.0f),
                vy = sin(ejectAngle) * 80.0f - 140.0f, // loft up
                lifetime = 0.9f,
                maxLifetime = 0.9f,
                scale = 0.8f,
                type = ParticleType.SHELL_CASING
            )
        )

        soundManager.playWeaponFire(weapon.id)
    }

    fun spawnGrenade(thrower: SoldierEntity, isGas: Boolean, soundManager: GameSoundManager) {
        if (thrower.grenadesRemaining <= 0) return
        thrower.grenadesRemaining--

        val shoulderX = thrower.x + 6.0f * thrower.facingDirection
        val shoulderY = thrower.y - 34.0f
        val angle = thrower.aimAngle

        val speed = 720.0f
        val grenade = GrenadeEntity(
            id = ++grenadeIdCounter,
            throwerId = thrower.id,
            x = shoulderX,
            y = shoulderY,
            vx = cos(angle) * speed + thrower.vx * 0.4f,
            vy = sin(angle) * speed - 150.0f, // loft arc
            rotation = 0.0f,
            rotSpeed = if (cos(angle) >= 0) 10.0f else -10.0f,
            fuseTimer = 2.4f,
            isGas = isGas
        )
        grenades.add(grenade)
        soundManager.playGrenadeThrow()
    }

    fun spawnPickup(weapon: WeaponType, x: Float, y: Float) {
        pickups.add(
            WeaponPickupEntity(
                id = ++pickupIdCounter,
                weapon = weapon,
                x = x,
                y = y,
                ammo = weapon.magazineSize * 2
            )
        )
    }

    fun addBlood(x: Float, y: Float) {
        for (i in 0 until 6) {
            val angle = Random.nextFloat() * 2.0f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 120.0f + 30.0f
            particles.add(
                ParticleEffect(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 40.0f,
                    lifetime = 0.5f,
                    maxLifetime = 0.5f,
                    scale = Random.nextFloat() * 0.8f + 0.4f,
                    type = ParticleType.BLOOD
                )
            )
        }
    }

    fun addExplosion(x: Float, y: Float, radius: Float, soundManager: GameSoundManager) {
        soundManager.playExplosion()
        // Shockwave smoke and sparks
        for (i in 0 until 18) {
            val angle = Random.nextFloat() * 2.0f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 250.0f + 60.0f
            particles.add(
                ParticleEffect(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    lifetime = 0.65f,
                    maxLifetime = 0.65f,
                    scale = Random.nextFloat() * 1.5f + 0.8f,
                    type = ParticleType.EXPLOSION
                )
            )
        }
        for (i in 0 until 10) {
            val angle = Random.nextFloat() * 2.0f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 180.0f + 20.0f
            particles.add(
                ParticleEffect(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 60.0f,
                    lifetime = 0.9f,
                    maxLifetime = 0.9f,
                    scale = Random.nextFloat() * 1.2f + 0.5f,
                    type = ParticleType.SMOKE
                )
            )
        }
    }

    fun update(
        dt: Float,
        player: SoldierEntity,
        bots: MutableList<SoldierEntity>,
        soundManager: GameSoundManager
    ) {
        val safeDt = dt.coerceIn(0.001f, 0.05f)
        val allSoldiers = mutableListOf<SoldierEntity>().apply {
            add(player)
            addAll(bots)
        }

        // 1. Update Soldiers Kinematics
        for (s in allSoldiers) {
            if (s.isDead) {
                s.deathTimer += safeDt
                continue
            }

            // Weapon Reload Step
            if (s.isReloading) {
                s.reloadTimer -= safeDt
                if (s.reloadTimer <= 0.0f) {
                    s.isReloading = false
                    val needed = s.equippedWeapon.magazineSize - s.ammoInClip
                    val take = min(needed, s.reserveAmmo)
                    s.ammoInClip += take
                    s.reserveAmmo -= take
                }
            }
            if (s.fireCooldownTimer > 0.0f) {
                s.fireCooldownTimer -= safeDt
            }

            // Jetpack Thrust vs Gravity
            if (s.isThrusting && s.jetpackFuel > 0.0f) {
                s.vy += THRUST_ACCEL * safeDt
                s.jetpackFuel = (s.jetpackFuel - JETPACK_BURN_RATE * safeDt).coerceAtLeast(0.0f)
                // Jetpack Boot Thruster Flame Particles
                if (Random.nextFloat() < 0.85f) {
                    val bootX = if (Random.nextBoolean()) s.x - 10.0f else s.x + 8.0f
                    val bootY = s.y + 4.0f
                    particles.add(
                        ParticleEffect(
                            x = bootX,
                            y = bootY,
                            vx = (Random.nextFloat() - 0.5f) * 25.0f - s.vx * 0.2f,
                            vy = 180.0f + Random.nextFloat() * 60.0f,
                            lifetime = 0.22f,
                            maxLifetime = 0.22f,
                            scale = 0.85f,
                            type = ParticleType.JETPACK_FLAME
                        )
                    )
                    // Rising smoke trail puff
                    if (Random.nextFloat() < 0.25f) {
                        particles.add(
                            ParticleEffect(
                                x = bootX,
                                y = bootY + 10.0f,
                                vx = (Random.nextFloat() - 0.5f) * 30.0f,
                                vy = 15.0f,
                                lifetime = 0.65f,
                                maxLifetime = 0.65f,
                                scale = Random.nextFloat() * 0.7f + 0.4f,
                                type = ParticleType.SMOKE
                            )
                        )
                    }
                }
            } else {
                s.vy += GRAVITY * safeDt
                // Passive Fuel Regen when not thrusting
                s.jetpackFuel = (s.jetpackFuel + JETPACK_REGEN_RATE * safeDt).coerceAtMost(s.maxFuel)
            }

            // Terminal velocity
            s.vy = s.vy.coerceIn(-800.0f, 750.0f)

            // Predict Next Position
            val prevY = s.y
            s.x += s.vx * safeDt
            s.y += s.vy * safeDt

            // Horizontal bounds clamp
            s.x = s.x.coerceIn(24.0f, ARENA_WIDTH - 24.0f)

            // Platform Collisions
            s.isGrounded = false
            for (p in platforms) {
                if (s.x >= p.x && s.x <= p.x + p.width) {
                    // One-way landing check: previous feet must be at or above platform top
                    if (prevY <= p.y + 4.0f && s.y >= p.y && s.vy >= 0.0f) {
                        s.y = p.y
                        s.vy = 0.0f
                        s.isGrounded = true
                        break
                    }
                }
            }

            // Floor clamp
            if (s.y >= FLOOR_Y) {
                s.y = FLOOR_Y
                s.vy = 0.0f
                s.isGrounded = true
            }

            // Ground friction damping
            if (s.isGrounded) {
                s.vx *= (1.0f - 12.0f * safeDt).coerceAtLeast(0.0f)
            } else {
                s.vx *= (1.0f - 2.5f * safeDt).coerceAtLeast(0.0f)
            }

            // Walking animation cadence
            if (s.isGrounded && abs(s.vx) > 10.0f) {
                s.walkFrameTimer += safeDt * (abs(s.vx) / 80.0f)
            } else {
                s.walkFrameTimer = 0.0f
            }
        }

        // 2. Update Bullets
        val bIter = bullets.iterator()
        while (bIter.hasNext()) {
            val b = bIter.next()
            b.lifetime -= safeDt
            if (b.lifetime <= 0.0f) {
                bIter.remove()
                continue
            }

            val nextX = b.x + b.vx * safeDt
            val nextY = b.y + b.vy * safeDt

            var bulletRemoved = false

            // Check hit against platforms
            for (p in platforms) {
                if (nextX in p.x..(p.x + p.width) && nextY in p.y..(p.y + p.height)) {
                    if (b.isRocket) {
                        addExplosion(nextX, nextY, b.splashRadius, soundManager)
                        applySplashDamage(nextX, nextY, b.splashRadius, b.damage, b.shooterId, allSoldiers, soundManager)
                    } else {
                        soundManager.playSfx("impact", volume = 0.4f)
                    }
                    bIter.remove()
                    bulletRemoved = true
                    break
                }
            }
            if (bulletRemoved) continue

            // Check hit against soldiers
            for (s in allSoldiers) {
                if (s.isDead || s.id == b.shooterId) continue

                // Soldier AABB bounding box: [x-20, y-65, 40, 65]
                val minX = s.x - 20.0f
                val maxX = s.x + 20.0f
                val minY = s.y - 65.0f
                val maxY = s.y

                if (nextX in minX..maxX && nextY in minY..maxY) {
                    if (b.isRocket) {
                        addExplosion(nextX, nextY, b.splashRadius, soundManager)
                        applySplashDamage(nextX, nextY, b.splashRadius, b.damage, b.shooterId, allSoldiers, soundManager)
                    } else {
                        applyDamageToSoldier(s, b.damage, b.shooterId, allSoldiers, soundManager)
                        addBlood(nextX, nextY)
                        soundManager.playSfx("ricochet", volume = 0.5f)
                    }
                    bIter.remove()
                    bulletRemoved = true
                    break
                }
            }
            if (bulletRemoved) continue

            b.x = nextX
            b.y = nextY
        }

        // 3. Update Grenades
        val gIter = grenades.iterator()
        while (gIter.hasNext()) {
            val g = gIter.next()
            g.fuseTimer -= safeDt
            g.vy += GRAVITY * safeDt
            g.x += g.vx * safeDt
            g.y += g.vy * safeDt
            g.rotation += g.rotSpeed * safeDt

            // Bounce off platforms
            for (p in platforms) {
                if (g.x in p.x..(p.x + p.width) && g.y >= p.y && g.y <= p.y + p.height && g.vy > 0.0f) {
                    g.y = p.y - 2.0f
                    g.vy = -g.vy * 0.45f // Restitution
                    g.vx *= 0.65f
                    g.rotSpeed *= 0.6f
                    break
                }
            }

            // Boundary bounce
            if (g.x <= 15.0f || g.x >= ARENA_WIDTH - 15.0f) {
                g.vx = -g.vx * 0.6f
                g.x = g.x.coerceIn(15.0f, ARENA_WIDTH - 15.0f)
            }
            if (g.y >= FLOOR_Y - 2.0f) {
                g.y = FLOOR_Y - 2.0f
                g.vy = -g.vy * 0.45f
                g.vx *= 0.65f
            }

            // Detonation
            if (g.fuseTimer <= 0.0f) {
                if (g.isGas) {
                    soundManager.playSfx("gas", volume = 0.9f)
                    for (i in 0 until 24) {
                        val angle = Random.nextFloat() * 2.0f * Math.PI.toFloat()
                        val speed = Random.nextFloat() * 70.0f
                        particles.add(
                            ParticleEffect(
                                x = g.x + cos(angle) * speed,
                                y = g.y + sin(angle) * speed,
                                vx = (Random.nextFloat() - 0.5f) * 20.0f,
                                vy = -Random.nextFloat() * 25.0f,
                                lifetime = 4.5f,
                                maxLifetime = 4.5f,
                                scale = Random.nextFloat() * 2.0f + 1.0f,
                                type = ParticleType.GAS_CLOUD
                            )
                        )
                    }
                    applySplashDamage(g.x, g.y, 110.0f, 60.0f, g.throwerId, allSoldiers, soundManager)
                } else {
                    addExplosion(g.x, g.y, 130.0f, soundManager)
                    applySplashDamage(g.x, g.y, 140.0f, 130.0f, g.throwerId, allSoldiers, soundManager)
                }
                gIter.remove()
            }
        }

        // 4. Update Weapon Pickups
        val pIter = pickups.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.lifetime -= safeDt
            if (p.lifetime <= 0.0f) {
                pIter.remove()
                continue
            }

            if (!p.isGrounded) {
                p.vy += GRAVITY * safeDt
                p.y += p.vy * safeDt
                for (plat in platforms) {
                    if (p.x in plat.x..(plat.x + plat.width) && p.y >= plat.y && p.y <= plat.y + 12.0f) {
                        p.y = plat.y
                        p.vy = 0.0f
                        p.isGrounded = true
                        break
                    }
                }
                if (p.y >= FLOOR_Y) {
                    p.y = FLOOR_Y
                    p.vy = 0.0f
                    p.isGrounded = true
                }
            }

            // Proximity check to player for auto/manual swap
            val dist = hypot(player.x - p.x, player.y - p.y)
            if (dist < 45.0f && !player.isDead) {
                // If player walks directly over, auto pickup if secondary or swap if current empty
                if (player.equippedWeapon.id != p.weapon.id) {
                    // Pick up sound
                    soundManager.playPickup()
                    // Swap weapon
                    val oldWeapon = player.equippedWeapon
                    player.equippedWeapon = p.weapon
                    player.ammoInClip = p.weapon.magazineSize
                    player.reserveAmmo = p.weapon.magazineSize * 3
                    player.isReloading = false
                    // Replace pickup with dropped weapon
                    p.x = player.x
                    p.y = player.y - 10.0f
                    // Update pickup entity in place
                    // or remove
                    pIter.remove()
                    spawnPickup(oldWeapon, player.x + player.facingDirection * 20.0f, player.y - 10.0f)
                    break
                }
            }
        }

        // 5. Update Particles
        val partIter = particles.iterator()
        while (partIter.hasNext()) {
            val part = partIter.next()
            part.lifetime -= safeDt
            if (part.lifetime <= 0.0f) {
                partIter.remove()
                continue
            }
            if (part.type == ParticleType.SHELL_CASING) {
                part.vy += 600.0f * safeDt
            }
            part.x += part.vx * safeDt
            part.y += part.vy * safeDt
            part.alpha = (part.lifetime / part.maxLifetime).coerceIn(0.0f, 1.0f)
        }

        // 6. Update Damage Numbers
        val dtIter = damageTexts.iterator()
        while (dtIter.hasNext()) {
            val d = dtIter.next()
            d.lifetime -= safeDt
            if (d.lifetime <= 0.0f) {
                dtIter.remove()
                continue
            }
            d.y -= 30.0f * safeDt // Float upwards
        }
    }

    private fun applySplashDamage(
        originX: Float,
        originY: Float,
        radius: Float,
        maxDamage: Float,
        attackerId: String,
        soldiers: List<SoldierEntity>,
        soundManager: GameSoundManager
    ) {
        for (s in soldiers) {
            if (s.isDead) continue
            val dist = hypot(s.x - originX, (s.y - 30.0f) - originY)
            if (dist < radius) {
                val falloff = 1.0f - (dist / radius)
                val damage = maxDamage * falloff
                applyDamageToSoldier(s, damage, attackerId, soldiers, soundManager)
                // Knockback force
                val angle = atan2((s.y - 30.0f) - originY, s.x - originX)
                s.vx += cos(angle) * 400.0f * falloff
                s.vy += sin(angle) * 400.0f * falloff - 100.0f
            }
        }
    }

    private fun applyDamageToSoldier(
        target: SoldierEntity,
        damage: Float,
        attackerId: String,
        soldiers: List<SoldierEntity>,
        soundManager: GameSoundManager
    ) {
        target.health -= damage
        damageTexts.add(
            FloatingDamageText(
                x = target.x + (Random.nextFloat() - 0.5f) * 20.0f,
                y = target.y - 65.0f,
                text = "-${damage.toInt()}",
                color = if (target.isPlayer) 0xFFFF3333 else 0xFFFFCC00
            )
        )

        if (target.health <= 0.0f && !target.isDead) {
            target.health = 0.0f
            target.isDead = true
            target.deathTimer = 0.0f
            soundManager.playRandomDeath()

            // Credit kill
            soldiers.firstOrNull { it.id == attackerId }?.let { attacker ->
                attacker.kills++
                attacker.score += 100
                if (attacker.isPlayer) {
                    soundManager.playSargeQuote()
                }
            }

            // Drop weapon
            spawnPickup(target.equippedWeapon, target.x, target.y - 20.0f)
        }
    }
}
