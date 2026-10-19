# RE_REPORT.md — Mini Militia (Doodle Army 2) Reverse Engineering Report

## 1. Executive Summary & Provenance
- **Target Application:** Doodle Army 2: Mini Militia (`Mini Militia - War.io`)
- **Package Name:** `com.appsomniacs.da2`
- **Version:** `5.6.0` (Version Code: `327`)
- **Min SDK:** 21 (Android 5.0 Lollipop)
- **Source Archive:** `D:\AI WORK\mini_miltia\game\com.appsomniacs.da2_5.6.0-327_2arch_7dpi_31lang_bd581877deb1c0565a554bfc11969632_apkmirror.com.apkm` (69,467,911 bytes)
- **Target Architecture Analyzed:** `arm64-v8a` (with `armeabi-v7a` fallback)

---

## 2. APK Split Architecture
The `.apkm` container is an Android App Bundle (AAB) split archive comprising 45 files:
1. `base.apk` (47.6 MB): Contains main DEX bytecodes (`classes.dex` through `classes4.dex`), Android manifest, layout resources, and the complete `assets/unpack/` payload.
2. `split_config.arm64_v8a.apk` (47.8 MB): Native 64-bit binaries including `libcocos2dcpp.so` (44.0 MB).
3. `split_config.armeabi_v7a.apk` (33.0 MB): Native 32-bit legacy binaries including 32-bit `libcocos2dcpp.so` (30.3 MB).
4. **Density Splits (7 DPIs):** `ldpi`, `mdpi`, `hdpi`, `tvdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`.
5. **Language Splits (31 Locales):** Arabic, Bengali, German, English, Spanish, French, Hindi, Japanese, Russian, etc.

---

## 3. Native Engine & Binary Architecture (`libcocos2dcpp.so`)

### 3.1 Game Engine Identification
- **Core 2D Framework:** Cocos2d-x (C++ runtime with OpenGL ES shaders and `CCTexture2D` / `CCSpriteBatchNode` rendering).
- **Physics Engine:** **Chipmunk2D** physics engine (discovered via `cpSpace`, `cpBody`, `cpShape`, `cpSegmentQueryInfo`, `_ZN10Projectile12addBodyShapeEP7cpSpace`, `_ZN14MapShapeSquare12addBodyShapeEP7cpSpace`, `_ZN16MapShapeTriangle12addBodyShapeEP7cpSpace`).
- **Entity View/Controller Model:**
  - `SoldierView`: Renders composite skeletal soldier, transforms shoulder pivot, tracks velocity vectors (`setVelocity(cpVect)`), position (`updateBody`), and health (`setPlayerHealth(float)`).
  - `SoldierLocalController`: Handles local player touch inputs, virtual joystick axes, weapon triggers, and jetpack boost.
  - `SoldierAIController`: Single-player bot decision engine handling platform patrolling, line-of-sight targeting, jump/jetpack flight maneuvers, weapon switching (`switchWeapons`), charging (`isCharging`), and firing angles (`setFireAngle(float)`).
  - `SoldierRemoteController`: Network proxy for multiplayer peers.

### 3.2 Weapon Hierarchy Discovered
The native C++ binary reveals dedicated weapon subclasses implementing standard lifecycle hooks (`create()`, `triggerPull(float)`, `remoteTriggerPull()`, `getShellPoint()`, `getWeaponRect()`, `updateItemStep(float)`, `playSound()`):
- `AK47`: High damage automatic assault rifle.
- `SHOTGUN` & `AA12`: Multi-pellet spread weapons.
- `M16` & `M14`: Tactical rifles.
- `UZI`, `MP5`, `TEC9`: Compact submachine guns with dual-wield support (`setDualConfiguration()`).
- `SMAW` / `ROCKET`: Heavy explosive launcher firing rocket projectiles with splash damage.
- `DEAGLE` & `MAGNUM`: Heavy caliber sidearms.
- `SAWGUN`: Fires bouncing circular saw blades.
- `FLAMETHROWER`: Continuous short-range stream of burning damage.
- `GASNADE`: Toxic gas grenade creating persistent lingering cloud AOE.
- `EMPNADE`: Electronic pulse grenade.

### 3.3 Audio Constants & Dialogue Lines
Extracted string tables confirm native bindings for sound effects:
- Weapon sounds: `_ZN3dam9constants5sound9k_shotgunE`, `k_rocketE`, `k_magnumE`, `k_deagleE`, `k_grenade1E`, `k_grenade2E`, `k_gasE`, `k_sawE`, `k_laserE`.
- Iconic Sarge training quotes:
  - *"SARGE: TRY THROWING A FEW GRENADES!"*
  - *"SARGE: LET'S MOVE! WE GOTTA FIND THAT AWOL ROBOT!"*
  - *"SARGE: MECHA-DOODLE HAS CONTROL OF ALL ROBOT UNITS!"*
  - *"SARGE: STAND DOWN, ROBOT! THAT'S AN ORDER!"*
  - *"SARGE: WHAT IS YOUR MAJOR MALFUNCTION? ROBOT!"*

---

## 4. Extracted Asset Manifest

The automated extraction pipeline (`scripts/unpack_apkm.py` and `scripts/slice_sprites.py`) cleanly separated 1,857 authentic sprites and 113 sound effects:

| Category | File Count | Key Assets Extracted |
| :--- | :---: | :--- |
| **Weapons** | 75 | `ak47.png`, `m16.png`, `sniper.png`, `shotgun.png`, `rocket.png`, `uzi.png`, `desertEagle.png`, `magnum.png`, `sawgun.png`, `flamethrower.png`, `grenade.png`, `gasNade.png`, `empNade.png` |
| **Soldier Skeleton** | 920 | Complete modular body parts for `agent_*`, `aficionado_*`, `bandito_*`, `assault_armor_*`, `shogun_*` (`*_arm.png`, `*_body.png`, `*_head.png`, `*_leg.png`), helmets, beards, boots |
| **HUD & Joysticks** | 80 | `movePad.png`, `moveStick.png` (Left Move Joystick), `firePad.png`, `fireStick.png` (Right Aim/Fire Joystick), `aim.png` (Reticle), `healthBar.png`, `ammoBar.png`, `boost.png`, `grenadeBtn.png`, `wepChangeBtn.png` |
| **Visual Effects (FX)** | 45 | `flame.png`, `flame1.png`, `flame2.png` (Jetpack thrust flames), `explosion.png`, `smoke.png`, `bullet_new.png`, `blood_new.png`, `laser.png` |
| **Robots / Drones** | 5 | `hawkDrone.png`, `wormDrone.png`, `wormDroneSaw.png`, `wormDroneT1.png`, `wormDroneT2.png` |
| **Maps & Backgrounds**| 108 | `Map_Outpost.png`, `Map_Catacombs.png`, `Map_HighTower.png`, `Map_Crossfire.png`, `Map_Survival.png`, `bg_new.png`, `bgDesert_new.png`, `tile64_new.png` |
| **Audio SFX** | 113 | `ak47.wav`, `m16.wav`, `shotgun.wav`, `rocket.wav`, `uzi.wav`, `deagle.wav`, `magnum.wav`, `thrust.wav`, `explode.wav`, `reload.wav`, `death1.wav`–`death9.wav`, `presMix.mp3` |

---

## 5. Architectural Implications for Native Kotlin Clone
1. **Rendering Model:** Replicate the skeletal assembly:
   - Torso positioned at `(posX, posY)`.
   - Legs render walking cycle when horizontal velocity `|vx| > 0.1f`, or idle stance when grounded.
   - Head positioned at top of torso.
   - Jetpack flame attached at back when vertical thrust is active.
   - Rotating Arm + Weapon: Pivots 360° at the shoulder point `(posX + shoulderOffsetX, posY + shoulderOffsetY)` using `atan2(aimY, aimX)`. If aiming left (`cos(angle) < 0`), the entire soldier flips horizontally (`scale(-1, 1)`).
2. **Virtual Coordinate Space:** Native resolution of `1280x720` (16:9) matches Cocos2d-x landscape game canvas.
3. **Sound Architecture:** Android `SoundPool` for ultra-low latency weapon clicks, gunshot bursts, jetpack hiss, and explosions; `MediaPlayer` for menu music (`presMix.mp3`).
