package com.rajiv.minimilitia.clone.engine

/**
 * Data specification for every weapon archetype in Mini Militia.
 */
data class WeaponType(
    val id: String,
    val displayName: String,
    val spriteKey: String,
    val sfxKey: String,
    val damage: Float,
    val fireRate: Float, // seconds between shots
    val magazineSize: Int,
    val reloadTime: Float, // seconds
    val bulletVelocity: Float, // px/sec
    val spreadAngleDeg: Float,
    val pelletCount: Int = 1,
    val splashRadius: Float = 0.0f,
    val isAutomatic: Boolean = true,
    val zoomFactor: Float = 1.0f,
    val barrelOffsetX: Float = 36.0f,
    val barrelOffsetY: Float = -2.0f
)

object WeaponCatalog {

    val M16 = WeaponType(
        id = "m16",
        displayName = "M4 / M16",
        spriteKey = "m16",
        sfxKey = "m16",
        damage = 20.0f,
        fireRate = 0.12f,
        magazineSize = 30,
        reloadTime = 1.5f,
        bulletVelocity = 1450.0f,
        spreadAngleDeg = 3.5f,
        isAutomatic = true,
        barrelOffsetX = 44.0f
    )

    val AK47 = WeaponType(
        id = "ak47",
        displayName = "AK-47",
        spriteKey = "ak47",
        sfxKey = "ak47",
        damage = 26.0f,
        fireRate = 0.16f,
        magazineSize = 30,
        reloadTime = 1.8f,
        bulletVelocity = 1350.0f,
        spreadAngleDeg = 5.0f,
        isAutomatic = true,
        barrelOffsetX = 42.0f
    )

    val SHOTGUN = WeaponType(
        id = "shotgun",
        displayName = "Shotgun",
        spriteKey = "shotgun",
        sfxKey = "shotgun",
        damage = 14.0f,
        fireRate = 0.70f,
        magazineSize = 6,
        reloadTime = 2.2f,
        bulletVelocity = 1100.0f,
        spreadAngleDeg = 18.0f,
        pelletCount = 6,
        isAutomatic = false,
        barrelOffsetX = 40.0f
    )

    val SNIPER = WeaponType(
        id = "sniper",
        displayName = "Sniper Rifle",
        spriteKey = "sniper",
        sfxKey = "m16",
        damage = 95.0f,
        fireRate = 1.20f,
        magazineSize = 5,
        reloadTime = 2.4f,
        bulletVelocity = 2400.0f,
        spreadAngleDeg = 0.5f,
        isAutomatic = false,
        zoomFactor = 1.35f,
        barrelOffsetX = 52.0f
    )

    val ROCKET = WeaponType(
        id = "rocket",
        displayName = "Rocket Launcher (SMAW)",
        spriteKey = "rocket",
        sfxKey = "rocket",
        damage = 120.0f,
        fireRate = 1.40f,
        magazineSize = 2,
        reloadTime = 2.8f,
        bulletVelocity = 750.0f,
        spreadAngleDeg = 1.0f,
        splashRadius = 90.0f,
        isAutomatic = false,
        barrelOffsetX = 46.0f
    )

    val DESERT_EAGLE = WeaponType(
        id = "desertEagle",
        displayName = "Desert Eagle",
        spriteKey = "desertEagle",
        sfxKey = "deagle",
        damage = 46.0f,
        fireRate = 0.35f,
        magazineSize = 7,
        reloadTime = 1.4f,
        bulletVelocity = 1500.0f,
        spreadAngleDeg = 2.0f,
        isAutomatic = false,
        barrelOffsetX = 30.0f
    )

    val UZI = WeaponType(
        id = "uzi",
        displayName = "Micro Uzi",
        spriteKey = "uzi",
        sfxKey = "uzi",
        damage = 15.0f,
        fireRate = 0.08f,
        magazineSize = 32,
        reloadTime = 1.2f,
        bulletVelocity = 1250.0f,
        spreadAngleDeg = 8.0f,
        isAutomatic = true,
        barrelOffsetX = 32.0f
    )

    val M14 = WeaponType(
        id = "m14",
        displayName = "M14 DMR",
        spriteKey = "m14",
        sfxKey = "m16",
        damage = 65.0f,
        fireRate = 0.40f,
        magazineSize = 10,
        reloadTime = 2.0f,
        bulletVelocity = 2100.0f,
        spreadAngleDeg = 1.0f,
        isAutomatic = false,
        zoomFactor = 1.25f,
        barrelOffsetX = 48.0f
    )

    val MAGNUM = WeaponType(
        id = "magnum",
        displayName = ".44 Magnum",
        spriteKey = "magnum",
        sfxKey = "magnum",
        damage = 75.0f,
        fireRate = 0.50f,
        magazineSize = 6,
        reloadTime = 1.8f,
        bulletVelocity = 1600.0f,
        spreadAngleDeg = 1.5f,
        isAutomatic = false,
        barrelOffsetX = 34.0f
    )

    val SAWGUN = WeaponType(
        id = "sawgun",
        displayName = "Saw Gun",
        spriteKey = "sawgun",
        sfxKey = "saw",
        damage = 110.0f,
        fireRate = 0.65f,
        magazineSize = 3,
        reloadTime = 2.2f,
        bulletVelocity = 900.0f,
        spreadAngleDeg = 2.0f,
        isAutomatic = false,
        barrelOffsetX = 38.0f
    )

    val FLAMETHROWER = WeaponType(
        id = "flamethrower",
        displayName = "Flamethrower",
        spriteKey = "flamethrower",
        sfxKey = "gas",
        damage = 18.0f,
        fireRate = 0.05f,
        magazineSize = 60,
        reloadTime = 2.5f,
        bulletVelocity = 850.0f,
        spreadAngleDeg = 12.0f,
        splashRadius = 35.0f,
        isAutomatic = true,
        barrelOffsetX = 42.0f
    )

    val ALL_WEAPONS = listOf(
        M16, AK47, SHOTGUN, SNIPER, ROCKET, DESERT_EAGLE, UZI,
        M14, MAGNUM, SAWGUN, FLAMETHROWER
    )

    fun getById(id: String): WeaponType {
        return ALL_WEAPONS.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: M16
    }

    fun getRandomWeapon(): WeaponType {
        return ALL_WEAPONS.random()
    }
}
