import os
import glob
import re
import json
import plistlib
from pathlib import Path
from PIL import Image

def parse_box(val):
    if isinstance(val, (list, tuple)):
        return [int(x) for x in val]
    nums = [int(n) for n in re.findall(r"-?\d+", str(val))]
    return nums

def slice_and_export():
    base_dir = Path(r"D:\AI WORK\mini_miltia")
    unpack_dir = base_dir / "re_workspace" / "extracted_apks" / "base" / "assets" / "unpack"
    target_sprites_dir = base_dir / "android_clone" / "app" / "src" / "main" / "assets" / "sprites"
    target_maps_dir = base_dir / "android_clone" / "app" / "src" / "main" / "assets" / "maps"
    target_sprites_dir.mkdir(parents=True, exist_ok=True)
    target_maps_dir.mkdir(parents=True, exist_ok=True)

    print("=" * 60)
    print("PHASE 1: INSPECTION OF MAP AND LEVEL FILES")
    print("=" * 60)

    map_files = []
    for root, dirs, files in os.walk(base_dir / "re_workspace" / "extracted_apks" / "base" / "assets"):
        for f in files:
            fl = f.lower()
            if any(k in fl for k in ["outpost", "catacomb", "map", "tile", "bg", "bush", "rock", "bunker", ".tmx", ".lvl"]):
                full_p = os.path.join(root, f)
                map_files.append((f, full_p, os.path.getsize(full_p)))

    print(f"Discovered {len(map_files)} map/level related files in APK:")
    for fn, fp, sz in sorted(map_files, key=lambda x: x[0]):
        print(f" - {fn:<30} ({sz:>8} bytes)")

    print("\n" + "=" * 60)
    print("PHASE 1: EXTRACTION & SLICING OF AUTHENTIC SPRITES")
    print("=" * 60)

    atlases_to_process = [
        ("Generic-540p.plist", "Generic-540p.png", "Generic.plist", "Generic.png"),
        ("menuTexture.plist", "menuTexture.png", "menuTexture.plist", "menuTexture.png"),
        ("AvatarCards.plist", "AvatarCards.png", "AvatarCards.plist", "AvatarCards.png"),
        ("avatarOutfits.plist", "avatarOutfits.png", "avatarOutfits.plist", "avatarOutfits.png"),
        ("avatarPieces.plist", "avatarPieces.png", "avatarPieces.plist", "avatarPieces.png"),
    ]

    manifest = {}

    for plist_name_hd, png_name_hd, plist_name_sd, png_name_sd in atlases_to_process:
        plist_path = unpack_dir / plist_name_hd
        png_path = unpack_dir / png_name_hd
        if not plist_path.exists() or not png_path.exists():
            plist_path = unpack_dir / plist_name_sd
            png_path = unpack_dir / png_name_sd

        if not plist_path.exists() or not png_path.exists():
            print(f"[WARN] Atlas missing: {plist_name_hd}")
            continue

        print(f"\nProcessing Atlas: {plist_path.name} ({png_path.name})...")
        try:
            with open(plist_path, "rb") as f:
                data = plistlib.load(f)
            atlas_img = Image.open(png_path).convert("RGBA")
        except Exception as e:
            print(f"[ERR] Failed to load atlas {plist_path.name}: {e}")
            continue

        frames = data.get("frames", {})
        print(f" -> Found {len(frames)} frames")

        for sprite_name, info in frames.items():
            try:
                # Frame rectangle
                if "frame" in info:
                    f_nums = parse_box(info["frame"])
                    if len(f_nums) == 4:
                        fx, fy, fw, fh = f_nums
                    else:
                        continue
                elif "textureRect" in info:
                    f_nums = parse_box(info["textureRect"])
                    if len(f_nums) == 4:
                        fx, fy, fw, fh = f_nums
                    else:
                        continue
                else:
                    continue

                if fw <= 0 or fh <= 0:
                    continue

                rotated = info.get("rotated", False) or info.get("textureRotated", False)

                # Crop sprite
                if rotated:
                    # In Cocos2d TexturePacker, rotated sprites are stored 90 degrees counter-clockwise
                    sub = atlas_img.crop((fx, fy, fx + fh, fy + fw))
                    sub = sub.rotate(90, expand=True)
                else:
                    sub = atlas_img.crop((fx, fy, fx + fw, fy + fh))

                # Save clean sprite
                out_path = target_sprites_dir / sprite_name
                sub.save(out_path, format="PNG")

                manifest[sprite_name] = {
                    "atlas": plist_path.name,
                    "width": sub.width,
                    "height": sub.height,
                    "rotated": rotated
                }

            except Exception as ex:
                pass

    # Copy Standalone Maps & Backgrounds
    standalone_maps = [
        "Map_Outpost.png", "bg_new.png", "MenuBackground.png", "tile64_new.png"
    ]
    for sm in standalone_maps:
        src = unpack_dir / sm
        if src.exists():
            dst = target_maps_dir / sm
            Image.open(src).save(dst)
            manifest[sm] = {
                "atlas": "standalone",
                "width": Image.open(src).width,
                "height": Image.open(src).height,
                "rotated": False
            }
            print(f"Copied standalone map asset: {sm}")

    # Generate Manifest File
    manifest_file = base_dir / "re_workspace" / "authentic_sprite_manifest.json"
    with open(manifest_file, "w") as f:
        json.dump(manifest, f, indent=2)

    print("\n" + "=" * 60)
    print(f"AUTHENTIC EXTRACTION COMPLETE: {len(manifest)} sprites registered in manifest!")
    print(f"Manifest saved to: {manifest_file}")
    print("=" * 60)

if __name__ == "__main__":
    slice_and_export()
