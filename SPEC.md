# SPEC.md — Mini Militia Native Android Game Engine Specification

## 1. Virtual Coordinate Space & Scaling
- **Logical Canvas Dimensions:** `1280.0f` width by `720.0f` height (Fixed 16:9 landscape aspect ratio).
- **Coordinate System:**
  - Origin `(0, 0)` is Top-Left.
  - Positive X extends Right (`0 -> 1280`).
  - Positive Y extends Downwards (`0 -> 720`).
- **Display Adapter Transformation:** The Compose `Canvas` measures the actual physical display (`widthPx`, `heightPx`), computes uniform scale factor $s = \min(w / 1280, h / 720)$, and centers the viewport with symmetric letterbox/pillarbox margins:
  $$\text{scale} = \min\left(\frac{W_{\text{device}}}{1280}, \frac{H_{\text{device}}}{720}\right)$$
  $$\text{offsetX} = \frac{W_{\text{device}} - 1280 \cdot s}{2}, \quad \text{offsetY} = \frac{H_{\text{device}} - 720 \cdot s}{2}$$

---

## 2. Physics & Kinematics Engine

### 2.1 Physics Constants
| Parameter | Value | Unit | Description |
| :--- | :---: | :---: | :--- |
| **Gravity ($g$)** | `+980.0` | $\text{px}/\text{s}^2$ | Constant downward acceleration |
| **Max Ground Speed** | `260.0` | $\text{px}/\text{s}$ | Horizontal walk velocity cap |
| **Ground Friction** | `14.0` | $\text{s}^{-1}$ | Exponential velocity decay when grounded without input |
| **Air Resistance** | `3.5` | $\text{s}^{-1}$ | Horizontal velocity damping while airborne |
| **Jetpack Thrust ($F_{\text{thrust}}$)** | `-1650.0` | $\text{px}/\text{s}^2$ | Upward vertical acceleration countering gravity |
| **Jetpack Max Fuel** | `100.0` | Fuel Units | Full tank capacity |
| **Jetpack Burn Rate** | `32.0` | Units/s | Fuel depletion rate during active upward thrust |
| **Jetpack Recharge Rate** | `22.0` | Units/s | Passive fuel recovery rate when grounded or drifting |
| **Max Terminal Fall Speed** | `700.0` | $\text{px}/\text{s}$ | Maximum downward velocity cap |

### 2.2 Collision Detection & Geometry
- **Platforms:** Axis-Aligned Bounding Boxes (AABB) defined by `Platform(x, y, width, height, isOneWay)`.
  - When `isOneWay == true`, collision resolves only if entity's previous bottom was above `platform.y` and vertical velocity $v_y > 0$.
- **Boundary Collisions:** Entities are clamped horizontally within $[20.0, 1260.0]$. Ground level is capped at $y = 660.0$.

---

## 3. Skeletal Soldier Architecture & 360° Rendering

### 3.1 Anatomical Assembly
Each soldier entity (Player and Bots) is composed dynamically from extracted sprite textures:
1. **Legs (`agent_leg.png` / `aficionado_leg.png`):**
   - Grounded Idle: Fixed standing sprite.
   - Running: Alternate 2-frame walking cadence based on step accumulator $\tau_{\text{walk}} = (\tau + \Delta t \cdot |v_x|) \bmod 0.3$.
2. **Torso (`agent_body.png`):**
   - Rendered above legs, carrying ammo pouches and tactical belt.
3. **Head (`agent_head.png`):**
   - Attached to top of torso with optional military helmet or sunglasses.
4. **Jetpack Flame (`flame.png`, `flame1.png`, `flame2.png`):**
   - When vertical thrust is engaged, cycles flame sprites at back of torso with flickering offset.
5. **Rotating Arm & Weapon (`agent_arm.png` + Weapon Sprite):**
   - Pivot point positioned at shoulder coordinates:
     $$X_{\text{shoulder}} = X + 6.0 \cdot \text{facingDirection}, \quad Y_{\text{shoulder}} = Y - 28.0$$
   - Angle $\theta = \operatorname{atan2}(\Delta Y_{\text{aim}}, \Delta X_{\text{aim}})$.
   - The arm and attached weapon rotate around the shoulder anchor.
   - If aiming to the left ($\cos \theta < 0$), the entity is mirrored horizontally ($\text{scaleX} = -1$) and $\theta' = \pi - \theta$ so weapons remain right-side up.

---

## 4. Weapon Arsenal Specifications

Each weapon binds to an authentic extracted sprite, dedicated sound effect, and projectile physics model:

| Weapon | Extracted Sprite | Audio SFX | Damage | Fire Rate | Mag Size | Reload | Velocity | Spread | Type |
| :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **M4 Assault** | `m16.png` | `m16.wav` | 20 | 0.12s | 30 | 1.5s | 1450 px/s | 3.5° | Rapid Tracer Bullet |
| **AK-47** | `ak47.png` | `ak47.wav` | 26 | 0.16s | 30 | 1.8s | 1350 px/s | 5.0° | Heavy Kinetic Bullet |
| **Shotgun** | `shotgun.png` | `shotgun.wav` | 14 × 6 | 0.70s | 6 | 2.2s | 1100 px/s | 18.0° | 6-Pellet Spread Burst |
| **Sniper Rifle** | `sniper.png` | `zoom.wav` + `m14.wav` | 95 | 1.20s | 5 | 2.4s | 2400 px/s | 0.5° | High-Velocity Armor Piercing |
| **Rocket (SMAW)**| `rocket.png` | `rocket.wav` + `explode.wav` | 120 | 1.40s | 2 | 2.8s | 750 px/s | 1.0° | Explosive Area of Effect (90px) |
| **Desert Eagle** | `desertEagle.png` | `deagle.wav` | 46 | 0.35s | 7 | 1.4s | 1500 px/s | 2.0° | Precision Heavy Sidearm |
| **Uzi** | `uzi.png` | `uzi.wav` | 15 | 0.08s | 32 | 1.2s | 1250 px/s | 8.0° | High RPM Submachine Gun |
| **Frag Grenade** | `grenade.png` | `throw.wav` + `explode.wav` | 130 | — | 3 | — | 700 px/s | Arc | Parabolic bounce ($e=0.5$), 2.5s fuse |
| **Gas Grenade** | `gasNade.png` | `gas.wav` | 8/sec | — | 2 | — | 650 px/s | Arc | Toxic cloud AOE duration 6.0s |

---

## 5. Single-Player AI Decision Machine (`AiBotController`)

The survival bots operate an autonomous finite state machine with 6 tactical behaviors:
```
           +-----------------------------+
           |           SPAWN             |
           +--------------+--------------+
                          |
                          v
                   +---------------+
                   |    PATROL     |<---------------------+
                   +-------+-------+                      |
                           |                              |
                  (Player in Range)                       |
                           |                              |
                           v                              |
                   +---------------+                      |
         +-------->|  CHASE_PLAYER |                      |
         |         +-------+-------+                      |
         |                 |                              |
(Elevation Higher)         |                              |
         |                 v                              |
         |         +---------------+                      |
         +---------+  JETPACK_FLY  |                      |
                   +-------+-------+                      |
                           |                              |
                    (Line of Sight)                       |
                           |                              |
                           v                              |
                   +---------------+                      |
                   |  AIM_AND_FIRE |                      |
                   +-------+-------+                      |
                           |                              |
                   (Clip Depleted)                        |
                           |                              |
                           v                              |
                   +---------------+                      |
                   | RETREAT_RELOAD+----------------------+
                   +-------+-------+
                           |
                     (Health <= 0)
                           |
                           v
                   +---------------+
                   |  DROP_WEAPON  |
                   |   AND_DIE     |
                   +---------------+
```

1. **Patrol:** Moves horizontally across its current platform toward edges or waypoints.
2. **Chase & Jetpack Fly:** If player elevation is $> 60$ px higher than bot, the bot engages its jetpack thrust until reaching parity elevation.
3. **Aim & Fire:** Rotates its arm directly towards player with small stochastic error margin ($\pm 4^\circ$) and pulls the trigger when line of sight is clear.
4. **Weapon Drop:** When defeated, the bot drops its equipped weapon as a `WeaponPickup` on the ground with authentic clank sound.
5. **Wave Escalation:** Each wave increments bot counts and weapon tier (Wave 1: Pistols/Uzi; Wave 2: AK47/M4; Wave 3+: Shotguns, Snipers, and RPGs).

---

## 6. Multi-Touch Dual Joystick Touch HUD
1. **Independent Multi-Touch Architecture (`awaitPointerEventScope`):**
   - The HUD overlay tracks every active finger independently by its unique `PointerId`. Simultaneous touch events across the left and right halves never collide or cancel one another.
   - **Left Screen Half ($x < W / 2$):** Movement & Rocket Flight. Initial touch dynamically establishes the joystick center or uses the resting corner anchor. Dragging horizontally controls walk speed ($V_x = \Delta X / R \cdot V_{\max}$), and dragging upward ($\Delta Y / R < -0.25$) fires rocket boot thrusters and plays looping jetpack audio.
   - **Right Screen Half ($x \ge W / 2$):** 360° Aiming & Automatic Firing. Initial touch dynamically anchors the aim stick. Vector angle $\theta = \operatorname{atan2}(\Delta Y, \Delta X)$ rotates player aim and laser sight. Dragging past the concentric threshold ring ($r > 0.45 \times R$) continuously triggers auto-fire.
   - **Visual Touch Feedback:** Rendered with Compose `Canvas` at 60 FPS featuring glowing translucent outer rings, concentric red auto-fire threshold ring, aiming laser vector line, jetpack thruster pulse flare, and authentic TexturePacker sprites (`movePad`, `moveStick`, `firePad`, `fireStick`).
2. **Top-Right Tactical Action Cluster:**
   - Active Weapon Slot displaying the weapon sprite, current mag and reserve count in military stencil typography; tap to switch weapons.
   - Circular Weapon Switch button (`wepChangeBtn.png`, 44×44).
   - Circular Reload button (`ammoBtn.png`, 44×44).
   - Circular Frag Grenade button (`grenadeBtn.png`, 46×46) with remaining count badge.
   - Circular Gas Grenade button (`gasNade.png`, 44×44).
3. **Status Telemetry:**
   - Top-Center: Stacked Red Health Bar (180×14 dp) and Cyan Jetpack Boost Bar (180×9 dp) with strictly zero text labels for 1:1 authentic HUD presentation.
   - Top-Left: Military status pill displaying `KILLS: X | BOT WAVE: Y` plus dedicated Pause/Menu button.

---

## 7. Authentic Skeletal Soldier Hierarchy & Boot Thrusters
- **Render Layer Stack (Bottom to Top):**
  1. Back Arm (`*_arm.png`)
  2. Jetpack Unit (attached to back of torso)
  3. Back Leg (`*_leg.png`) with walk-cadence swing or angled flight pose
  4. Torso / Body (`*_body.png` / `assault_armor_body.png`)
  5. Front Leg (`*_leg.png`)
  6. Head (`*_head.png`) tilting slightly along aim angle
  7. Equipped Weapon (anchored at shoulder joint, rotated $360^\circ$)
  8. Front Arm (`*_arm.png`) gripping weapon fore-end
- **Boot Thrusters & Particle Emitters:**
  - In Mini Militia, jetpack thrust originates from the **bottom of the soldier's boots**:
    $$X_{\text{thruster}} = X \pm 8.0, \quad Y_{\text{thruster}} = Y - 2.0$$
  - When thrusting, cycles `flame1.png` and `flame2.png` beneath boots with flickering displacement, and emits rising `smoke.png` trail puffs.
- **Laser Sight & Reticle:**
  - Translucent red/white laser line extending from the gun muzzle to the target position, capped with the authentic `aim.png` reticle.
  - On muzzle fire, renders a flash sprite and ejects spinning shell casings.

---

## 8. Scrolling Outpost Arena & Dynamic Camera Zoom
- **World Coordinate Space:** `3328.0f` width by `1152.0f` height (52 columns × 18 rows of 64×64 tiles extracted from `1outpost_new.tmx`).
- **Textured Terrain:**
  - Solid collision polygons mapped from layer `tile` rendered with tiled `tile64_new.png` textures.
  - Multi-tier terrain: Upper sniper towers, middle bunker, lower underground cavern paths, and ground floor.
- **Camera Follow System:**
  - Viewport dimensions match device aspect ratio (scaled virtual view).
  - Target position smoothly follows player:
    $$\text{camX} = \text{lerp}(\text{camX}, X_{\text{player}} + V_x \cdot 0.25, 0.10)$$
    $$\text{camY} = \text{lerp}(\text{camY}, Y_{\text{player}} + V_y \cdot 0.15, 0.10)$$
  - Clamped so viewport never extends beyond world boundaries $[0, 3328] \times [0, 1152]$.
- **Dynamic Weapon Zoom:**
  - Standard Weapons (Pistol, Uzi, M16, AK47): `zoom = 1.0f`.
  - Heavy Launcher (RPG / SMAW): `zoom = 0.85f`.
  - Long-Range Rifles (M14, Sniper): `zoom = 0.72f` (smoothly expanding field of view for extreme-range combat).

---

## 9. Off-Screen Threat Radar Indicators
- When AI bots or survival drones (`hawkDrone.png`) are located outside the current camera viewport frustum, the engine computes the screen-space intersection angle and renders a pulsing red indicator chevron/arrow along the screen border pointing toward the enemy's off-screen location.

