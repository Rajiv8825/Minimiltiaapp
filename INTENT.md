# INTENT.md — Mini Militia Native Android Clone

## 1. Product Vision
Deliver a high-frame-rate, authentic, offline **Native Android Single-Player Mini Militia Clone** written in **100% Native Kotlin + Jetpack Compose** (`Canvas` 2D Game Loop + `SoundPool` Audio + Dual-Joystick Touch HUD).

The game faithfully recreates the frantic dual-stick combat of *Doodle Army 2: Mini Militia*, utilizing the authentic reverse-engineered sprites, weapon graphics, soldier skeletal parts, map backgrounds, and audio sound effects extracted directly from the original APKM archive.

---

## 2. Target Users & Gameplay Modes
1. **Survival Bot Arena:** Player faces escalating waves of autonomous AI soldiers and robotic survival drones across multi-tier floating platforms. Players must manage jetpack fuel, pick up dropped enemy weapons, throw grenades, and survive as long as possible.
2. **Training Ground (Sarge Obstacle Course):** Practice range to test weapons (M4, AK47, Shotgun, Sniper, RPG, Uzi), master 360° aiming and jetpack hovering, and trigger Sarge training voice lines.
3. **Armory & Asset Inspector:** Visual interactive preview screen allowing players to inspect extracted weapons, outfits, and stats.

---

## 3. Performance & Platform Requirements
- **Display & Orientation:** Strict landscape mode (`android:screenOrientation="sensorLandscape"`), full-screen immersive (`WindowInsetsControllerCompat` hiding status and navigation bars).
- **Frame Rate Target:** Consistent 60 FPS (`withFrameNanos` / delta-time physics integration).
- **Virtual Resolution:** Fixed `1280x720` (16:9) virtual game world scaled cleanly to any physical device aspect ratio.
- **Audio Latency:** Sub-20ms instant trigger response via Android `SoundPool` (cached audio buffers for gunfire, jetpack thrust hiss, reload clicks, and explosion rumbles).

---

## 4. Explicit Non-Goals (Scope Boundary)
- **NO Online Multiplayer Networking:** No WebSockets, RakNet, UDP matchmaking, or peer-to-peer sync.
- **NO Cloud Accounts / Monitizing Gacha:** No Facebook login, Google Play Games sign-in, BattlePass purchases, or microtransactions.
- **NO 3rd-Party Heavy Engines:** No Unity, Unreal, or Godot runtimes. Pure, lightweight native Kotlin + Jetpack Compose.
- **NO Destructive Terrain Modification:** Map terrain uses fixed multi-level platforms and boundary collision geometry.
