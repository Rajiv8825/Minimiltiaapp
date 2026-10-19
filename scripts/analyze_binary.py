import re
import json
from pathlib import Path

def extract_strings(file_bytes, min_len=4):
    """Extract printable ASCII and UTF-8 strings from binary."""
    pattern = re.compile(rb'[\x20-\x7E]{' + str(min_len).encode() + rb',}')
    for match in pattern.finditer(file_bytes):
        yield match.group().decode('ascii', errors='ignore')

def analyze():
    base_dir = Path(r"D:\AI WORK\mini_miltia")
    re_ws = base_dir / "re_workspace"
    extracted_dir = re_ws / "extracted_apks"
    
    so_path = extracted_dir / "split_config.arm64_v8a" / "lib" / "arm64-v8a" / "libcocos2dcpp.so"
    if not so_path.exists():
        so_path = extracted_dir / "split_config.armeabi_v7a" / "lib" / "armeabi-v7a" / "libcocos2dcpp.so"
    
    print(f"[*] Reading native binary: {so_path.relative_to(base_dir)} ({so_path.stat().st_size:,} bytes)...")
    so_bytes = so_path.read_bytes()

    # Read info.json from APKM
    info_json_path = re_ws / "unpacked_apkm" / "info.json"
    apkm_info = {}
    if info_json_path.exists():
        try:
            apkm_info = json.loads(info_json_path.read_text(encoding='utf-8'))
        except Exception as e:
            apkm_info = {"error": str(e)}

    # Engine identification
    cocos_versions = set()
    box2d_indicators = set()
    stage_indicators = set()

    # Keywords for game mechanics
    target_keywords = [
        "Weapon", "Gun", "Bullet", "Grenade", "Jetpack", "Avatar", 
        "Soldier", "Bot", "Robot", "Health", "Damage", "Reload", 
        "FireRate", "Zoom", "Laser", "Rocket", "Shotgun", "Sniper", 
        "Rifle", "M4", "AK47", "Uzi", "Pistol", "Gas", "Saw", "Shield",
        "b2World", "b2Body", "b2Contact", "b2Vec2", "b2Fixture"
    ]

    matched_strings = {k: set() for k in target_keywords}
    demangled_or_class_hints = set()

    print("[*] Scanning binary strings...")
    for s in extract_strings(so_bytes, min_len=4):
        # Engine checks
        if "cocos2d" in s.lower() or "cocos" in s.lower():
            if len(s) < 80:
                cocos_versions.add(s)
        if "box2d" in s.lower() or "b2world" in s.lower() or "b2body" in s.lower():
            if len(s) < 80:
                box2d_indicators.add(s)
        if "stage" in s.lower() or "gameworld" in s.lower() or "gamescene" in s.lower():
            if len(s) < 80:
                stage_indicators.add(s)
        
        # Keyword matching
        for kw in target_keywords:
            if kw.lower() in s.lower() and len(s) <= 100:
                matched_strings[kw].add(s)
        
        # Class or method pattern
        if "::" in s and any(kw.lower() in s.lower() for kw in target_keywords) and len(s) < 120:
            demangled_or_class_hints.add(s)

    # Clean up sets to sorted lists (limit to top 40 most relevant per category)
    telemetry = {
        "apkm_metadata": apkm_info,
        "binary_path": str(so_path),
        "binary_size_bytes": len(so_bytes),
        "engine_detection": {
            "cocos2d_strings": sorted(list(cocos_versions))[:40],
            "box2d_indicators": sorted(list(box2d_indicators))[:40],
            "game_scene_stage_indicators": sorted(list(stage_indicators))[:40],
        },
        "class_and_method_symbols": sorted(list(demangled_or_class_hints))[:150],
        "gameplay_keyword_findings": {
            k: sorted(list(v))[:30] for k, v in matched_strings.items() if v
        }
    }

    # DEX string scan (from classes.dex in base)
    dex_classes = []
    base_dex_path = extracted_dir / "base" / "classes.dex"
    if base_dex_path.exists():
        print(f"[*] Scanning DEX strings in {base_dex_path.relative_to(base_dir)}...")
        dex_bytes = base_dex_path.read_bytes()
        for s in extract_strings(dex_bytes, min_len=6):
            if "appsomniacs" in s.lower() or "minimilitia" in s.lower() or "cocos2d" in s.lower():
                if len(s) < 120 and s not in dex_classes:
                    dex_classes.append(s)
    telemetry["dex_classes_and_references"] = dex_classes[:60]

    out_file = re_ws / "binary_analysis.json"
    out_file.write_text(json.dumps(telemetry, indent=2), encoding="utf-8")
    print(f"[+] TASK-1.2 COMPLETE: Binary telemetry saved to {out_file.relative_to(base_dir)}")
    print(f"    - Cocos2d references: {len(cocos_versions)}")
    print(f"    - Box2D physics references: {len(box2d_indicators)}")
    print(f"    - Gameplay keywords matched: {sum(len(v) for v in matched_strings.values())}")
    print(f"    - Class/Method signatures: {len(demangled_or_class_hints)}")

if __name__ == "__main__":
    analyze()
