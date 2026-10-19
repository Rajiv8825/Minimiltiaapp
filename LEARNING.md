# LEARNING.md — Mini Militia Reverse Engineering & Native Android Clone

Key engineering learnings, architectural patterns, and solutions discovered during the reverse engineering of **Doodle Army 2: Mini Militia** and its 1:1 rebuild in **100% Native Kotlin + Jetpack Compose**.

---

## 1. Reverse Engineering & Asset Extraction

* **APKM Structure:**
  * APKM files are ZIP archives containing multiple split APKs (`base.apk`, `split_config.arm64_v8a.apk`, `split_config.xxhdpi.apk`).
  * Game code and assets reside inside `base.apk` under `assets/unpack/` and `lib/arm64-v8a/libcocos2dcpp.so`.

* **Native Binary Inspection (`libcocos2dcpp.so`):**
  * ELF string extraction revealed Cocos2d-x engine hooks and Chipmunk2D physics (`cpSpace`, `cpShape`, `cpBody`).
  * Key classes identified: `SoldierController`, `SoldierAIController`, `ArmoryManager`, and weapon constants (M4, AK47, Shotgun, Sniper, SMAW).

* **TexturePacker Atlas Slicing:**
  * Sprites in `.plist` files store bounding frames (`x, y, w, h`) inside large `.png` atlases.
  * **The Rotation Trap:** Many frames have `textureRotated: true`. TexturePacker rotates them 90° clockwise to pack tightly. Slicing scripts must crop `(h, w)` and rotate 90° counter-clockwise to restore original dimensions.

---

## 2. 2D Game Architecture in Jetpack Compose

* **Compose Canvas vs Traditional Views:**
  * Standard Compose UI recomposition (`State<T>`) is too slow for 60 FPS games due to layout recalculation overhead.
  * Solution: Use a single fullscreen Compose `Canvas` driven by `withFrameNanos`.
  * Only compute physics and positions in memory (`GameLoopState`); the `Canvas` simply draws raw bitmaps using matrix transforms in a single render pass.

* **Delta-Time ($\Delta t$) Frame Independence:**
  * Never tie speed directly to frames (e.g. `x += 5`).
  * Calculate delta time: `dt = (currentTimeNanos - previousTimeNanos) / 1_000_000_000f`.
  * Update velocity and position with $\Delta t$: `position += velocity * dt`. This ensures identical speed on 60 Hz, 90 Hz, and 120 Hz displays.

---

## 3. Skeletal Soldier Animation & Chibi Proportions

* **8-Layer Draw Order:**
  To match authentic Mini Militia visuals, body parts must be drawn back-to-front:
  1. Boot Thruster Flames (`flame1.png`, `flame2.png`)
  2. Boots / Feet
  3. Legs
  4. Torso & Belt
  5. Tactical Vest
  6. Head & Eyes
  7. Helmet / Cap
  8. Rotating Arm & Equipped Weapon

* **Independent 360° Weapon Aiming:**
  * The body flips horizontally (`scaleX = 1f` or `-1f`) based on whether the aim angle is pointing left or right.
  * The arm and weapon rotate independently by matrix angle `atan2(dy, dx)`, keeping the soldier's head facing the crosshair.

---

## 4. 2D Physics & Ballistics Engine

* **Kinematics Equations:**
  * **Gravity:** Continuous downward acceleration (`velocity.y += 980f * dt`).
  * **Jetpack Flight:** Vertical upward thrust vector (`-1600f * dt`) that overrides gravity when boost fuel is active.
  * **Horizontal Drag:** Air resistance damping (`velocity.x *= 0.92f`) so the soldier decelerates naturally when releasing the joystick.

* **Collision Handling:**
  * **AABB (Axis-Aligned Bounding Box):** Simple rectangle intersection checks between player/bots and static platform edges.
  * **Grenades:** Elastic bounce simulation using restitution damping (`velocity.y = -velocity.y * 0.5f`) until energy dissipates, followed by a 2.5-second fuse detonation.

---

## 5. Multi-Touch Joysticks in Jetpack Compose

* **Why Standard Gestures Failed:**
  * Compose's high-level gesture modifiers (`detectDragGestures`) only track one pointer at a time or lock up when two fingers touch the screen simultaneously.

* **Low-Level Solution (`awaitPointerEventScope`):**
  * Use low-level `awaitPointerEventScope` and process each active touch point by its unique `PointerId`:
    * **Left Screen Half (`x < screenWidth / 2`):** Movement and jetpack thrust.
    * **Right Screen Half (`x >= screenWidth / 2`):** 360° aiming and auto-fire trigger.
  * When a finger lands anywhere on either half, dynamically place the joystick base under that finger and calculate offset distance `(dx, dy)`.
  * Trigger auto-fire automatically when the right thumb drag exceeds 45% of the joystick radius.

---

## 6. Low-Latency Audio Engine

* **MediaPlayer vs SoundPool:**
  * `MediaPlayer` causes audio stutter and noticeable latency (~100-300ms) because it decodes audio streams on demand.
  * Android `SoundPool` pre-decodes `.wav` and `.mp3` clips directly into uncompressed PCM memory buffers.
  * Result: Sub-10ms instantaneous fire audio when tapping the trigger.

* **Jetpack Looping Audio:**
  * Store the active stream ID returned by `soundPool.play(..., loop = -1)`.
  * Call `soundPool.pause(streamId)` or `soundPool.stop(streamId)` immediately when flight ceases to prevent endless looping.

---

## 7. Tactical AI State Machine (FSM)

* **6-State Bot Behavior:**
  1. **Patrol:** Ground movement toward map waypoints.
  2. **Jetpack Chase:** Vertical rocket thrust when the player is elevated or across obstacles.
  3. **Aim:** 360° raycast calculation toward player coordinates with slight random inaccuracy.
  4. **Fire:** Weapon burst firing within effective range.
  5. **Dodge:** Reverse jetpack thrust when incoming grenades or high player fire are detected.
  6. **Respawn:** Dynamic wave escalation and respawn timer.

---

## 8. Git & Deployment Hygiene

* **Large Binary Discipline:**
  * Never commit original APKM archives (~70 MB), decompressed APK dumps (~275 MB), or Android build caches (`app/build/` ~100 MB).
  * Use a strict `.gitignore` so the repository stays fast, clean, and under 30 MB while preserving all extracted assets, analysis JSONs, and Kotlin code.
