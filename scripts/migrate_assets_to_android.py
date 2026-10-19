import shutil
from pathlib import Path

def migrate():
    base_dir = Path(r"D:\AI WORK\mini_miltia")
    re_ws = base_dir / "re_workspace"
    android_assets = base_dir / "android_clone" / "app" / "src" / "main" / "assets"
    
    sprites_dest = android_assets / "sprites"
    audio_dest = android_assets / "audio"
    maps_dest = android_assets / "maps"

    sprites_dest.mkdir(parents=True, exist_ok=True)
    audio_dest.mkdir(parents=True, exist_ok=True)
    maps_dest.mkdir(parents=True, exist_ok=True)

    # 1. Copy essential sprites from sliced_sprites/all and categorized
    sliced_all = re_ws / "sliced_sprites" / "all"
    key_sprites = [
        # Weapons
        "ak47.png", "m16.png", "sniper.png", "shotgun.png", "rocket.png", 
        "uzi.png", "desertEagle.png", "magnum.png", "sawgun.png", "flamethrower.png",
        "grenade.png", "gasNade.png", "empNade.png", "m14.png", "mp5.png",
        # Soldier bodies
        "agent_arm.png", "agent_body.png", "agent_head.png", "agent_leg.png",
        "aficionado_arm.png", "aficionado_body.png", "aficionado_head.png", "aficionado_leg.png",
        "shogun_arm.png", "shogun_body.png", "shogun_head.png", "shogun_leg.png",
        "assault_armor_arm.png", "assault_armor_body.png", "assault_armor_head.png", "assault_armor_leg.png",
        # HUD & controls
        "movePad.png", "moveStick.png", "firePad.png", "fireStick.png", "aim.png",
        "healthBar.png", "ammoBar.png", "boost.png", "grenadeBtn.png", "wepChangeBtn.png",
        "ammoBtn.png", "zoomBtn.png", "bar.png", "bar_long.png", "dualBtn.png", "meleeBtn.png",
        # FX & Projectiles
        "flame.png", "flame1.png", "flame2.png", "explosion.png", "smoke.png", "laser.png",
        # Drones
        "hawkDrone.png", "wormDrone.png"
    ]

    copied_sprites = 0
    for s in key_sprites:
        src = sliced_all / s
        if src.exists():
            shutil.copy2(src, sprites_dest / s)
            copied_sprites += 1
        else:
            print(f"[-] Missing sprite: {s}")

    # Also copy standalone bullet_new.png, blood_new.png from maps
    maps_src = re_ws / "extracted_maps"
    for extra in ["bullet_new.png", "blood_new.png", "smoke_new.png"]:
        src = maps_src / extra
        if src.exists():
            shutil.copy2(src, sprites_dest / extra)
            copied_sprites += 1

    print(f"[+] Migrated {copied_sprites} authentic gameplay sprites to {sprites_dest.relative_to(base_dir)}")

    # 2. Copy Audio files
    audio_src = re_ws / "extracted_audio"
    key_audio = [
        "ak47.wav", "m16.wav", "shotgun.wav", "rocket.wav", "uzi.wav", 
        "deagle.wav", "magnum.wav", "thrust.wav", "explode.wav", "reload.wav", 
        "throw.wav", "grenade1.wav", "grenade2.wav", "pickup.wav", "switch.wav", 
        "dryfire.wav", "impact.wav", "impact2.wav", "ricochet.wav", "gas.wav", "saw.wav",
        "cmonboy.wav", "getsome.wav", "bringit.wav", "niceshot.wav", "yeah.wav", 
        "death1.wav", "death2.wav", "death3.wav", "death4.wav", "death5.wav",
        "presMix.mp3"
    ]
    copied_audio = 0
    for a in key_audio:
        src = audio_src / a
        if src.exists():
            shutil.copy2(src, audio_dest / a)
            copied_audio += 1
        else:
            print(f"[-] Missing audio: {a}")
    print(f"[+] Migrated {copied_audio} authentic sound files to {audio_dest.relative_to(base_dir)}")

    # 3. Copy Map files
    key_maps = [
        "Map_Outpost.png", "Map_Catacombs.png", "Map_Survival.png", "Map_Training.png",
        "bg_new.png", "bgDesert_new.png", "tile64_new.png", "MenuBackground.png"
    ]
    copied_maps = 0
    for m in key_maps:
        src = maps_src / m
        if src.exists():
            shutil.copy2(src, maps_dest / m)
            copied_maps += 1
        else:
            print(f"[-] Missing map: {m}")
    print(f"[+] Migrated {copied_maps} map backgrounds & textures to {maps_dest.relative_to(base_dir)}")

    print("\n[+] TASK-3.3 COMPLETE: All assets successfully migrated to Android assets folder!")

if __name__ == "__main__":
    migrate()
