package com.rajiv.minimilitia.clone.engine

/**
 * Authentic 2560 x 1440 Outpost Arena Architecture.
 * Multi-layer geometry with Left/Right Bases, Center Bridge,
 * Underground Catacomb Tunnel, Upper Floating Sniper Perches, and Cover Bushes.
 */
object OutpostMapData {
    const val WORLD_WIDTH = 2560.0f
    const val WORLD_HEIGHT = 1440.0f
    const val FLOOR_Y = 1360.0f

    val PLATFORMS = listOf(
        // Left Spawn Base
        Platform(x = 40.0f, y = 960.0f, width = 620.0f, height = 64.0f, isOneWay = false, label = "LeftBaseFloor"),
        Platform(x = 80.0f, y = 640.0f, width = 540.0f, height = 50.0f, isOneWay = false, label = "LeftBunkerRoof"),
        Platform(x = 160.0f, y = 360.0f, width = 340.0f, height = 44.0f, isOneWay = false, label = "LeftSniperTower"),

        // Right Spawn Base
        Platform(x = 1900.0f, y = 960.0f, width = 620.0f, height = 64.0f, isOneWay = false, label = "RightBaseFloor"),
        Platform(x = 1940.0f, y = 640.0f, width = 540.0f, height = 50.0f, isOneWay = false, label = "RightBunkerRoof"),
        Platform(x = 2060.0f, y = 360.0f, width = 340.0f, height = 44.0f, isOneWay = false, label = "RightSniperTower"),

        // Center Surface Bridge
        Platform(x = 800.0f, y = 640.0f, width = 960.0f, height = 54.0f, isOneWay = false, label = "CenterBridge"),
        Platform(x = 1120.0f, y = 320.0f, width = 320.0f, height = 40.0f, isOneWay = false, label = "CenterSniperPerch"),

        // Stepping Transition Platforms
        Platform(x = 640.0f, y = 800.0f, width = 180.0f, height = 40.0f, isOneWay = false, label = "LeftStep"),
        Platform(x = 1740.0f, y = 800.0f, width = 180.0f, height = 40.0f, isOneWay = false, label = "RightStep"),

        // Lower Underground Catacomb Tunnel
        Platform(x = 520.0f, y = 1240.0f, width = 1520.0f, height = 80.0f, isOneWay = false, label = "CatacombFloor"),
        Platform(x = 1180.0f, y = 1100.0f, width = 200.0f, height = 44.0f, isOneWay = false, label = "CatacombCover"),

        // Bedrock Ground
        Platform(x = 0.0f, y = FLOOR_Y, width = WORLD_WIDTH, height = 80.0f, isOneWay = false, label = "BedrockGround")
    )

    val WEAPON_SPAWNS = listOf(
        Triple(330.0f, 350.0f, "sniper"),
        Triple(2230.0f, 350.0f, "rocket"),
        Triple(1280.0f, 310.0f, "m14"),
        Triple(350.0f, 950.0f, "ak47"),
        Triple(2210.0f, 950.0f, "shotgun"),
        Triple(1050.0f, 630.0f, "flamethrower"),
        Triple(1510.0f, 630.0f, "sawgun"),
        Triple(850.0f, 1230.0f, "magnum"),
        Triple(1710.0f, 1230.0f, "uzi"),
        Triple(1280.0f, 1090.0f, "desertEagle"),
        Triple(1280.0f, 630.0f, "m16")
    )

    val BOT_SPAWNS = listOf(
        Pair(250.0f, 290.0f),
        Pair(2230.0f, 290.0f),
        Pair(1280.0f, 250.0f),
        Pair(300.0f, 890.0f),
        Pair(2200.0f, 890.0f),
        Pair(950.0f, 1170.0f),
        Pair(1600.0f, 1170.0f)
    )

    val MAP_PROPS = listOf(
        // Palm Trees on Surface
        Triple(220.0f, 430.0f, "palmTree"),
        Triple(1280.0f, 430.0f, "palmTree"),
        Triple(2300.0f, 430.0f, "palmTree"),
        // Cover Bushes
        Triple(350.0f, 560.0f, "bushPalm"),
        Triple(920.0f, 560.0f, "bushFan"),
        Triple(1640.0f, 560.0f, "bushPalm"),
        Triple(2210.0f, 560.0f, "bushFan"),
        Triple(1280.0f, 1160.0f, "bushFan")
    )
}
