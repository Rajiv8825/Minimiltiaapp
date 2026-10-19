import os
import re
import shutil
import plistlib
from pathlib import Path
from PIL import Image

def parse_rect(rect_str):
    """Parse '{{x,y},{w,h}}' or '{x,y,w,h}' into integers (x, y, w, h)."""
    if isinstance(rect_str, (list, tuple)) and len(rect_str) == 4:
        return map(int, rect_str)
    nums = re.findall(r'-?\d+', str(rect_str))
    if len(nums) >= 4:
        return int(nums[0]), int(nums[1]), int(nums[2]), int(nums[3])
    return None

def categorize_sprite(name):
    n = name.lower()
    # Weapons
    if any(k in n for k in [
        'gun', 'ak47', 'ak45', 'm16', 'm14', 'mp5', 'shotgun', 'sniper', 
        'rocket', 'uzi', 'magnum', 'deagle', 'deserteagle', 'grenade', 'saw', 
        'laser', 'flamethrower', 'emp', 'smaw', 'aa12', 'tavor', 'tec9', 
        'xm8', 'm93', 'hunting', 'rifle', 'phasr', 'machete', 'nade', 'ammo',
        'm61', 'rg6', 'mortar'
    ]):
        if any(h in n for h in ['pad', 'stick', 'bar', 'btn', 'area']):
            return 'hud'
        return 'weapons'
    # Soldier body parts & cosmetics
    if any(k in n for k in [
        '_arm', '_body', '_head', '_leg', 'soldier', 'avatar', 'beard', 
        'hair', 'hat', 'eyes', 'mouth', 'boots', 'pants', 'vest', 'gloves', 
        'face', 'glasses', 'outfit', 'shogun', 'agent', 'aficionado', 'bandito'
    ]):
        return 'soldier'
    # HUD & Dual Joysticks & Controls
    if any(k in n for k in [
        'pad', 'stick', 'btn', 'bar', 'aim', 'boost', 'health', 'crosshair', 
        'hud', 'joystick', 'pointer', 'switch', 'dual', 'control'
    ]):
        return 'hud'
    # Visual Effects & Explosions & Jetpack Flames
    if any(k in n for k in [
        'flame', 'explosion', 'smoke', 'blood', 'bullet', 'blast', 'tracer', 
        'shrapnel', 'shell', 'vfx', 'flare', 'ray', 'shine'
    ]):
        return 'fx'
    # Drones & Robots
    if any(k in n for k in ['drone', 'hawk', 'worm', 'robot', 'mecha']):
        return 'drones'
    # Cards & Badges
    if any(k in n for k in ['card', 'badge', 'icon', 'button', 'popup', 'frame', 'tab', 'banner']):
        return 'ui_cards'
    return 'misc'

def slice_sprites():
    base_dir = Path(r"D:\AI WORK\mini_miltia")
    re_ws = base_dir / "re_workspace"
    unpack_dir = re_ws / "extracted_apks" / "base" / "assets" / "unpack"
    
    sliced_dir = re_ws / "sliced_sprites"
    audio_dir = re_ws / "extracted_audio"
    maps_dir = re_ws / "extracted_maps"

    sliced_dir.mkdir(parents=True, exist_ok=True)
    audio_dir.mkdir(parents=True, exist_ok=True)
    maps_dir.mkdir(parents=True, exist_ok=True)

    cat_dirs = {
        'weapons': sliced_dir / 'weapons',
        'soldier': sliced_dir / 'soldier',
        'hud': sliced_dir / 'hud',
        'fx': sliced_dir / 'fx',
        'drones': sliced_dir / 'drones',
        'ui_cards': sliced_dir / 'ui_cards',
        'misc': sliced_dir / 'misc',
        'all': sliced_dir / 'all'
    }
    for cd in cat_dirs.values():
        cd.mkdir(parents=True, exist_ok=True)

    print("[*] Slicing TexturePacker sprite sheets with robust 0-size protection...")
    
    # Prioritize non-540p higher-res sheets, fallback to 540p
    plist_files = sorted(list(unpack_dir.glob("*.plist")))
    base_stems = set(p.stem for p in plist_files if not p.stem.endswith('-540p'))
    filtered_plists = []
    for p in plist_files:
        if p.stem.endswith('-540p') and p.stem[:-5] in base_stems:
            continue
        filtered_plists.append(p)

    total_sliced = 0
    cat_counts = {k: 0 for k in cat_dirs}

    for pl_path in filtered_plists:
        try:
            raw_bytes = pl_path.read_bytes()
            if not raw_bytes.startswith(b'<?xml'):
                continue
            data = plistlib.loads(raw_bytes)
            if not isinstance(data, dict) or 'frames' not in data:
                continue
            
            # Find matching PNG texture
            meta = data.get('metadata', {})
            tex_name = meta.get('realTextureFileName') or meta.get('textureFileName')
            png_path = None
            if tex_name and (unpack_dir / tex_name).exists():
                png_path = unpack_dir / tex_name
            elif (unpack_dir / (pl_path.stem + '.png')).exists():
                png_path = unpack_dir / (pl_path.stem + '.png')
            elif (unpack_dir / (pl_path.stem + '-540p.png')).exists():
                png_path = unpack_dir / (pl_path.stem + '-540p.png')
            
            if not png_path or not png_path.exists():
                continue

            atlas = Image.open(png_path).convert('RGBA')
            frames = data.get('frames', {})
            print(f"[*] Processing {pl_path.name} -> {png_path.name} ({len(frames)} frames)...")

            for frame_name, frame_info in frames.items():
                try:
                    rect_str = frame_info.get('frame') or frame_info.get('textureRect')
                    if not rect_str:
                        continue
                    rect = parse_rect(rect_str)
                    if not rect:
                        continue
                    
                    x, y, w, h = rect
                    if w <= 0 or h <= 0:
                        continue
                    
                    rotated = frame_info.get('rotated', False)

                    if rotated:
                        crop_box = (x, y, x + h, y + w)
                        cropped = atlas.crop(crop_box).transpose(Image.Transpose.ROTATE_270)
                    else:
                        crop_box = (x, y, x + w, y + h)
                        cropped = atlas.crop(crop_box)

                    clean_name = Path(frame_name).name
                    if not clean_name.lower().endswith('.png'):
                        clean_name += '.png'

                    # Save to all/
                    cropped.save(cat_dirs['all'] / clean_name, 'PNG')

                    # Categorize and save
                    category = categorize_sprite(clean_name)
                    cropped.save(cat_dirs[category] / clean_name, 'PNG')

                    cat_counts[category] += 1
                    cat_counts['all'] += 1
                    total_sliced += 1
                except Exception as frame_err:
                    print(f"    [!] Skipping problematic frame {frame_name}: {frame_err}")

        except Exception as e:
            print(f"[!] Error processing {pl_path.name}: {e}")

    print(f"\n[+] Sliced {total_sliced} sprites total across categories:")
    for cat, count in cat_counts.items():
        if cat != 'all':
            print(f"  - {cat:10s}: {count} sprites")

    # Copy Standalone Maps & Backgrounds
    print("\n[*] Copying standalone maps and level graphics...")
    map_patterns = ["Map_*.png", "bg*.png", "tile64*.png", "bullet_new*.png", "blood_new*.png", "smoke_new*.png", "*.tmx", "*.tsx"]
    map_copied = 0
    for pat in map_patterns:
        for f in unpack_dir.glob(pat):
            shutil.copy2(f, maps_dir / f.name)
            map_copied += 1
    print(f"[+] Extracted {map_copied} map & tile files into {maps_dir.relative_to(base_dir)}")

    # Copy Standalone Audio Files
    print("\n[*] Copying audio files...")
    audio_copied = 0
    for f in unpack_dir.glob("*"):
        if f.suffix.lower() in [".wav", ".mp3", ".ogg"]:
            shutil.copy2(f, audio_dir / f.name)
            audio_copied += 1
    print(f"[+] Extracted {audio_copied} audio files into {audio_dir.relative_to(base_dir)}")

    print("\n[+] TASK-1.3 COMPLETE: Authentic game assets extracted and categorized!")

if __name__ == "__main__":
    slice_sprites()
