import os
import glob
import zipfile
import json
from pathlib import Path

def unpack():
    base_dir = Path(r"D:\AI WORK\mini_miltia")
    game_dir = base_dir / "game"
    re_ws = base_dir / "re_workspace"
    unpacked_apkm_dir = re_ws / "unpacked_apkm"
    extracted_apks_dir = re_ws / "extracted_apks"

    unpacked_apkm_dir.mkdir(parents=True, exist_ok=True)
    extracted_apks_dir.mkdir(parents=True, exist_ok=True)

    # 1. Locate .apkm
    apkm_files = list(game_dir.glob("*.apkm")) + list(game_dir.glob("*.xapk")) + list(game_dir.glob("*.zip"))
    if not apkm_files:
        raise FileNotFoundError(f"No .apkm found in {game_dir}")
    
    apkm_path = apkm_files[0]
    print(f"[*] Found APKM archive: {apkm_path.name} ({apkm_path.stat().st_size:,} bytes)")

    # 2. Extract APKM bundle
    print(f"[*] Extracting APKM bundle to {unpacked_apkm_dir}...")
    with zipfile.ZipFile(apkm_path, 'r') as z:
        z.extractall(unpacked_apkm_dir)

    print("[+] APKM bundle extracted successfully.")
    
    # 3. Enumerate files in unpacked_apkm
    unpacked_items = list(unpacked_apkm_dir.glob("*"))
    print(f"[*] Contents of unpacked APKM ({len(unpacked_items)} items):")
    for item in unpacked_items:
        print(f"  - {item.name} ({item.stat().st_size:,} bytes)")

    # 4. Extract each .apk into extracted_apks/<name>/
    apk_files = list(unpacked_apkm_dir.glob("*.apk"))
    print(f"\n[*] Found {len(apk_files)} APK files inside bundle:")
    for apk_file in apk_files:
        target_dir = extracted_apks_dir / apk_file.stem
        target_dir.mkdir(parents=True, exist_ok=True)
        print(f"[*] Extracting {apk_file.name} -> {target_dir.relative_to(base_dir)}...")
        with zipfile.ZipFile(apk_file, 'r') as z:
            z.extractall(target_dir)
        print(f"  [+] Extracted {apk_file.name} ({len(z.namelist())} files).")

    print("\n[+] TASK-1.1 COMPLETE: All APK splits extracted into re_workspace/extracted_apks/")

if __name__ == "__main__":
    unpack()
