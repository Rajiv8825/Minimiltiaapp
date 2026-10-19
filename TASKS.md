# TASKS.md — Mini Militia Clone Execution Ledger

## Phase 1: Automated Reverse Engineering & Asset Extraction
- [x] **TASK-1.1**: Unpack APKM archive and split APKs into `re_workspace/` using `scripts/unpack_apkm.py` (Verified: 41 split APKs + base.apk extracted, 1426 files in base.apk)
- [x] **TASK-1.2**: Analyze native binary (`libcocos2dcpp.so`) & DEX strings/symbols, output `re_workspace/binary_analysis.json` using `scripts/analyze_binary.py` (Verified: Cocos2d-x + Chipmunk2D physics cpShape/cpSpace, SoldierController/SoldierAIController, Weapon classes AK47/SHOTGUN/UZI/M16/M14/SMAW/DEAGLE/MAGNUM/FLAMETHROWER/GASNADE/SAWGUN)
- [x] **TASK-1.3**: Parse Cocos2d TexturePacker `.plist` + `.png` atlases and slice sprites into `re_workspace/sliced_sprites/`, extract audio and map graphics using `scripts/slice_sprites.py` (Verified: 1857 sprites sliced [75 weapons, 920 soldier body parts, 80 HUD/joysticks, 45 FX, 5 drones], 108 maps/tiles, 113 audio SFX files)

## Phase 2: SDD Artifacts & Reverse Engineering Report
- [x] **TASK-2.1**: Author `RE_REPORT.md` (Verified: documented APK splits, Cocos2d-x/Chipmunk2D architecture, C++ symbols, 1857 sprites, 113 audio files)
- [x] **TASK-2.2**: Author `INTENT.md` (Verified: single-player survival/training focus, 60 FPS landscape target, explicit non-goals)
- [x] **TASK-2.3**: Author `SPEC.md` (Verified: 1280x720 space, kinematics/jetpack equations, skeletal rendering, 7 weapons + grenades, AI bot state machine)
- [x] **TASK-2.4**: Author `AGENTS.md` (Verified: full stack rules, gradlew & ADB commands, architecture boundaries)
- [x] **TASK-2.5**: Complete SDD verification gate before touching Android code (Verified: RE_REPORT.md, INTENT.md, SPEC.md, AGENTS.md, TASKS.md all verified on disk)

## Phase 3: Native Android Project Scaffolding (Kotlin + Jetpack Compose)
- [x] **TASK-3.1**: Scaffold modern Android Gradle project in `android_clone/` (`com.rajiv.minimilitia.clone`, Compose, Kotlin, targetSdk 34, JDK 21) (Verified: root/app build.gradle.kts, settings.gradle.kts, Gradle 8.14.3 wrapper)
- [x] **TASK-3.2**: Configure `AndroidManifest.xml` (sensorLandscape, immersive full-screen mode, hardware acceleration) (Verified: AndroidManifest.xml, themes.xml, colors.xml)
- [x] **TASK-3.3**: Pipeline authentic extracted assets (sprites, sounds, map backgrounds) into Android project assets/res (Verified: 58 sprites, 32 audio SFX, 7 maps migrated to android_clone/app/src/main/assets/)

## Phase 4: Core 2D Game Engine, Physics & Combat (Kotlin)
- [x] **TASK-4.1**: Implement `GameSoundManager.kt` with Android `SoundPool` for low-latency SFX and `MediaPlayer` for ambiance (Verified: loads 32 audio files, zero-latency trigger, jetpack stream)
- [x] **TASK-4.2**: Implement `SpriteRepository.kt` for loading, caching, slicing, and matrix-scaling extracted bitmaps (Verified: caching for weapons, body parts, HUD, FX, and maps)
- [x] **TASK-4.3**: Implement `WeaponCatalog.kt` defining stats, fire rates, spread, reload, projectile types for M4, AK47, Shotgun, Sniper, RPG, Uzi, Grenades (Verified: ballistics, damage, reload specs)
- [x] **TASK-4.4**: Implement `PhysicsWorld.kt` & `EntityModels.kt` (gravity, jetpack thrust, inertia, platform AABB collisions, map bounds) (Verified: 1280x720 coordinates, AABB collisions, grenade bounces)
- [x] **TASK-4.5**: Implement `AiBotController.kt` (state machine: Patrol, Jetpack Chase, 360° Aiming, Firing, Dodging, Respawn waves) (Verified: 6 tactical states, escalating wave system)
- [x] **TASK-4.6**: Implement `MultiTouchHudOverlay.kt` (dual virtual joysticks: Move+Jetpack thrust, 360° Aim+Fire, weapon switch, reload, grenade button, health & fuel bars) (Verified: multi-touch pointer gestures, HUD telemetry)
- [x] **TASK-4.7**: Implement `GameArenaScreen.kt` & `GameLoopState.kt` (60 FPS Canvas rendering, parallax background, platforms, skeletal soldier with rotating arm/weapon, bullet traces, explosions, floating damage text) (Verified: withFrameNanos 60 FPS loop, skeletal transform pipeline)
- [x] **TASK-4.8**: Implement `MainMenuScreen.kt` & `MainActivity.kt` (Compose navigation, Survival Arena, Training Mode, Armory/Asset Inspector) (Verified: navigation between MainMenu, Survival, Training, and Armory)
- [x] **TASK-4.9**: Build debug APK via `./gradlew assembleDebug` and verify zero errors (Verified: BUILD SUCCESSFUL in 29s, app-debug.apk 18.3 MB)
- [x] **TASK-4.10**: Deploy APK to device via ADB, launch MainActivity, and capture screen verification (Verified: installed and running on physical Samsung Galaxy M32, screenshots confirmed)

## Phase 6: True 1:1 Visual, Map, Skeletal & HUD Authenticity Overhaul
- [x] **TASK-6.1**: Deep extraction of `GameConfiguration.plist`, `Avatar.plist`, `1outpost_new.tmx` map geometry into `exact_game_specs.json` and asset migration (Verified: 3328x1152 Outpost map tiles, exact bone hierarchy, weapon ballistics, high-res HUD sprites)
- [x] **TASK-6.2**: Update `SPEC.md` with authentic skeletal hierarchy, scrolling Outpost geometry, dynamic camera zoom, and HUD specs (Verified: SPEC.md updated with sections 6, 7, 8, 9)
- [x] **TASK-6.3**: Rebuild skeletal soldier renderer (`GameArenaScreen.kt`, `SpriteRepository.kt`) with authentic layered draw order, boot thruster flames (`flame1.png`), smoke puffs, and aiming laser sight (`aim.png`) (Verified: 8-layer skeletal stack, boot thrusters, red laser aim sight and reticle verified in specs/v2_authentic_gameplay.png)
- [x] **TASK-6.4**: Rebuild scrolling Outpost arena (`PhysicsWorld.kt`, `GameArenaScreen.kt`) with smooth camera tracking, dynamic sniper zoom, tiled `tile64_new.png` terrain, and off-screen enemy indicators (Verified: 3328x1152 Outpost map with smooth camera tracking, grass trim, and off-screen red radar chevrons verified)
- [x] **TASK-6.5**: Rebuild authentic HUD (`MultiTouchHudOverlay.kt`) with genuine `healthBar.png`, `boost.png`, `movePad`/`moveStick`, `firePad`/`fireStick`, weapon ammo slot, and redesigned Main Menu (`MainMenuScreen.kt`) (Verified: MenuBackground + Sarge icon verified in specs/v2_authentic_menu.png; authentic HUD controls verified in specs/v2_authentic_gameplay.png)
- [x] **TASK-6.6**: Build debug APK, install on Samsung Galaxy M32, capture `specs/v2_authentic_menu.png` and `specs/v2_authentic_gameplay.png`, and verify visual fidelity (Verified: BUILD SUCCESSFUL in 21s, installed on physical Samsung Galaxy M32 via ADB, high-fidelity screenshots visually confirmed)

## Phase 7: Mini Militia 1:1 Visual & Mechanical Replica Overhaul
- [x] **TASK-7.1**: Rebuild authentic HUD in `MultiTouchHudOverlay.kt` (top-center stacked red health/cyan boost with zero text labels, top-right weapon card & tactical grenade/reload cluster, top-left scoreboard pill, clean joysticks with movePad/firePad and zero text labels) (Verified: Red health and cyan boost stacked in top-center without text labels; weapon card, swap, reload, frag grenade badge, and gas grenade in top-right; pure dual joysticks at bottom verified in v2_arena_verification.png)
- [x] **TASK-7.2**: Rebuild Main Menu in `MainMenuScreen.kt` (`MenuBackground.png`, authentic `Logo_Game.png` header, Officer Sarge graphic + live soldier preview, 4 military stencil plate buttons) (Verified: Authentic Logo_Game.png, MenuBackground.png, Sarge graphic, live breathing Doodle soldier, and 4 stencil plate buttons verified in v2_menu_verification.png)
- [x] **TASK-7.3**: Build debug APK via `./gradlew.bat assembleDebug`, install on connected Samsung Galaxy M32, launch `.MainActivity`, and capture `android_clone/v2_menu_verification.png` and `android_clone/v2_arena_verification.png` (Verified: BUILD SUCCESSFUL in 21s, installed via ADB Streamed Install, screen captures pulled to disk)
- [x] **TASK-7.4**: Visual verification of captured screenshots via `view_file` to confirm 1:1 visual match with Doodle Army 2 and update TASKS.md / SPEC.md (Verified: Both v2_menu_verification.png and v2_arena_verification.png inspected via view_file on hardware display 2400x1080)

## Phase 8: Multi-Touch Joystick Controls Fix & Real Gamer Combat Verification
- [x] **TASK-8.1**: Implement independent multi-touch pointer tracking (`awaitPointerEventScope`) in `MultiTouchHudOverlay.kt` with full left-half screen movement/jetpack flight, right-half 360° aim/fire, and dynamic visual touch feedback (Verified: Low-level awaitPointerEventScope tracks pointer.id independently per screen half; Canvas draws glowing dynamic rings and knobs; zero gesture collisions)
- [x] **TASK-8.2**: Build debug APK via `./gradlew.bat assembleDebug` and install onto connected Samsung Galaxy M32 via ADB (Verified: BUILD SUCCESSFUL in 21s, Streamed Install Success on physical SM_M325F device)
- [x] **TASK-8.3**: Verify multi-touch fluid movement, jetpack flight, and simultaneous auto-firing via ADB touch inputs and visual screenshot inspection (`view_file`) (Verified: flight_test.png, firing_test.png, and multitouch_action.png visually confirmed on hardware)

## Phase 9: GitHub Repository Initialization & Push
- [x] **TASK-9.1**: Author root `.gitignore` and comprehensive `README.md` with screenshots and engine documentation (Verified: .gitignore excludes build caches/intermediate APK dumps, README.md created with hardware screenshots)
- [x] **TASK-9.2**: Create public repository `Rajivjha003/mini-militia-clone` on GitHub via REST API (Verified: Created https://github.com/Rajivjha003/mini-militia-clone)
- [ ] **TASK-9.3**: Initialize git, configure remote `origin`, stage files, commit, and push to `main` (In progress)

