package com.rajiv.minimilitia.clone.engine

/**
 * Core entity state models for Mini Militia game world.
 */

data class Platform(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val isOneWay: Boolean = true,
    val label: String = ""
) {
    fun contains(px: Float, py: Float): Boolean {
        return px in x..(x + width) && py in y..(y + height)
    }
}

data class SoldierEntity(
    val id: String,
    val isPlayer: Boolean,
    var x: Float,
    var y: Float, // Feet / center-bottom position
    var vx: Float = 0.0f,
    var vy: Float = 0.0f,
    var health: Float = 100.0f,
    val maxHealth: Float = 100.0f,
    var jetpackFuel: Float = 100.0f,
    val maxFuel: Float = 100.0f,
    var aimAngle: Float = 0.0f, // Radians 0..2PI
    var facingDirection: Float = 1.0f, // +1 right, -1 left
    var equippedWeapon: WeaponType = WeaponCatalog.M16,
    var secondaryWeapon: WeaponType = WeaponCatalog.DESERT_EAGLE,
    var ammoInClip: Int = WeaponCatalog.M16.magazineSize,
    var reserveAmmo: Int = 90,
    var isReloading: Boolean = false,
    var reloadTimer: Float = 0.0f,
    var fireCooldownTimer: Float = 0.0f,
    var grenadesRemaining: Int = 3,
    var isGrounded: Boolean = false,
    var isThrusting: Boolean = false,
    var walkFrameTimer: Float = 0.0f,
    var outfitPrefix: String = "agent",
    var isDead: Boolean = false,
    var deathTimer: Float = 0.0f,
    var kills: Int = 0,
    var score: Int = 0
)

data class BulletEntity(
    val id: Long,
    val shooterId: String,
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Float,
    val splashRadius: Float = 0.0f,
    var lifetime: Float = 2.0f,
    val isRocket: Boolean = false
)

data class GrenadeEntity(
    val id: Long,
    val throwerId: String,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Float = 0.0f,
    var rotSpeed: Float = 8.0f,
    var fuseTimer: Float = 2.5f,
    val isGas: Boolean = false,
    var isExploded: Boolean = false
)

data class WeaponPickupEntity(
    val id: Long,
    val weapon: WeaponType,
    var x: Float,
    var y: Float,
    var vy: Float = 0.0f,
    var ammo: Int = weapon.magazineSize,
    var isGrounded: Boolean = false,
    var lifetime: Float = 30.0f
)

enum class ParticleType {
    EXPLOSION,
    SMOKE,
    BLOOD,
    JETPACK_FLAME,
    BULLET_SPARK,
    GAS_CLOUD,
    MUZZLE_FLASH,
    SHELL_CASING
}

data class ParticleEffect(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var lifetime: Float,
    val maxLifetime: Float,
    var scale: Float,
    val type: ParticleType,
    var alpha: Float = 1.0f
)

data class FloatingDamageText(
    var x: Float,
    var y: Float,
    val text: String,
    val color: Long, // 0xAARRGGBB
    var lifetime: Float = 0.8f,
    val maxLifetime: Float = 0.8f
)
