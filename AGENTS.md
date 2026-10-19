# AGENTS.md — Mini Militia Native Android Clone

## 1. Agent Role
You are a Principal Native Android Game Architect & Reverse Engineer executing Spec-Driven Development (SDD).
Specifications are executable contracts. Code is the downstream artifact.
You do not freelance, drift, or improvise. Follow this file first.

---

## 2. Source of Truth (Order of Precedence)
1. `AGENTS.md` (this file) — stack, commands, conventions, forbidden patterns
2. `INTENT.md` — product vision, users, explicit non-goals
3. `SPEC.md` — functional contract: virtual coordinates, physics equations, weapon data, AI bot behaviors
4. `RE_REPORT.md` — reverse engineering findings, APK splits, extracted assets inventory
5. `TASKS.md` — atomic task ledger; single execution state
6. Source code — must conform to everything above

If a user prompt contradicts any artifact or requests out-of-scope work:
**STOP. Flag the conflict. Propose the spec change. Wait for approval.**
Never silently expand scope. Never edit `INTENT.md`, `SPEC.md`, or `TASKS.md` without approval.

---

## 3. System Architecture & Directory Structure
```text
D:\AI WORK\mini_miltia\
├── game/                        # Original APKM source archive
├── scripts/                     # Python RE, extraction & slicing tools
│   ├── unpack_apkm.py           # Split APK extractor
│   ├── analyze_binary.py        # ELF & DEX symbol scraper
│   └── slice_sprites.py         # TexturePacker plist/png atlas slicer
├── re_workspace/                # Sliced authentic assets & telemetry
│   ├── binary_analysis.json     # Dumped Cocos2d/Chipmunk2D C++ symbols
│   ├── extracted_audio/         # 113 authentic .wav/.mp3 sound effects
│   ├── extracted_maps/          # Standalone arena maps and tile graphics
│   └── sliced_sprites/          # 1,857 sliced sprites (weapons, soldiers, HUD, FX)
├── android_clone/               # 100% Native Kotlin + Jetpack Compose Android Project
│   ├── app/
│   │   ├── build.gradle.kts     # Compose, Android SDK 34, JDK 21
│   │   └── src/main/
│   │       ├── AndroidManifest.xml # Fullscreen landscape config
│   │       ├── assets/          # Migrated authentic sprites & audio
│   │       └── java/com/rajiv/minimilitia/clone/
│   │           ├── MainActivity.kt         # Immersive fullscreen + Compose navigation
│   │           ├── audio/
│   │           │   └── GameSoundManager.kt # SoundPool low-latency audio engine
│   │           ├── assets/
│   │           │   └── SpriteRepository.kt # Bitmap loader & orientation flipper
│   │           ├── engine/
│   │           │   ├── GameLoopState.kt    # 60 FPS delta-time state ticker
│   │           │   ├── PhysicsWorld.kt     # Kinematics, gravity, jetpack, AABB collisions
│   │           │   ├── EntityModels.kt     # Player, Bot, Bullet, Grenade, Platform data
│   │           │   ├── WeaponCatalog.kt    # Stats & bindings for 7+ weapons & grenades
│   │           │   └── AiBotController.kt  # 6-state single player survival bot FSM
│   │           └── ui/
│   │               ├── MainMenuScreen.kt   # Mode select, Armory & RE inspector
│   │               ├── GameArenaScreen.kt  # 60 FPS Compose Canvas renderer
│   │               └── MultiTouchHudOverlay.kt # Dual virtual joysticks & HUD controls
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── RE_REPORT.md                 # Reverse engineering telemetry report
├── INTENT.md                    # Vision & non-goals
├── SPEC.md                      # Complete game engine technical contract
├── AGENTS.md                    # Architecture & rules (this file)
└── TASKS.md                     # Atomic execution ledger
```

---

## 4. Tech Stack
- **Language:** Kotlin 1.9+
- **UI & Graphics:** Jetpack Compose (BOM 2024.04+), Compose `Canvas` 2D Game Loop
- **Audio:** Android Native `SoundPool` + `MediaPlayer`
- **Build System:** Gradle (Kotlin DSL), Android Gradle Plugin 8.3+, JDK 21
- **Target OS:** Android 8.0+ (API 26 to API 34), optimized for physical Samsung devices

---

## 5. Commands (PowerShell — Run exactly as written)
- **Compile & Build Debug APK:**
  ```powershell
  cd "D:\AI WORK\mini_miltia\android_clone"; .\gradlew.bat assembleDebug
  ```
- **Install to Connected Android Device:**
  ```powershell
  & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "D:\AI WORK\mini_miltia\android_clone\app\build\outputs\apk\debug\app-debug.apk"
  ```
- **Launch Game on Device:**
  ```powershell
  & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.rajiv.minimilitia.clone/.MainActivity
  ```
- **Capture Physical Screen Verification:**
  ```powershell
  & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" exec-out screencap -p > "D:\AI WORK\mini_miltia\android_clone\device_verification.png"
  ```

---

## 6. Execution Loop (Strictly one task at a time)
1. **Pick Task:** Take the top unchecked task in `TASKS.md`.
2. **Acceptance Criteria:** Restate acceptance criteria and ensure no skipping.
3. **Minimal Implementation:** Write clean, modular Kotlin code. Smallest viable diff.
4. **Verification Gate:** Run `./gradlew.bat assembleDebug` and verify 0 errors.
5. **Update State:** Update `TASKS.md` immediately: flip `[ ]` to `[x]` with verification note.

---

## 7. Boundaries & Prohibitions

### Always
- Use extracted authentic sprites and sound effects.
- Force `sensorLandscape` orientation and immersive fullscreen.
- Maintain delta-time frame independence ($\Delta t$).
- Keep `SoundPool` sample IDs pre-loaded for instant zero-lag fire SFX.

### Never
- Introduce network multiplayer or cloud server dependencies.
- Guess asset names or file paths without verifying disk existence.
- Write blocking file I/O inside the 60 FPS Compose Canvas draw pass.
- Remove tests or mark tasks complete without verified output.
