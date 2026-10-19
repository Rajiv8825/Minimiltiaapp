import base64
import gzip
import struct
import xml.etree.ElementTree as ET
from pathlib import Path

def generate():
    base_dir = Path(r"D:\AI WORK\mini_miltia")
    unpack_dir = base_dir / "re_workspace" / "extracted_apks" / "base" / "assets" / "unpack"
    target_file = base_dir / "android_clone" / "app" / "src" / "main" / "java" / "com" / "rajiv" / "minimilitia" / "clone" / "engine" / "OutpostMapData.kt"

    tree = ET.parse(unpack_dir / "1outpost_new.tmx")
    root = tree.getroot()
    map_w = int(root.attrib["width"])
    map_h = int(root.attrib["height"])
    tile_w = int(root.attrib["tilewidth"])
    tile_h = int(root.attrib["tileheight"])

    tile_layer = next(l for l in root.findall("layer") if l.attrib.get("name") == "tile")
    decompressed = gzip.decompress(base64.b64decode(tile_layer.find("data").text.strip()))
    tile_ids = struct.unpack(f"<{len(decompressed)//4}I", decompressed)

    # Consolidate horizontal tiles
    platforms = []
    for row in range(map_h):
        start_col = None
        for col in range(map_w):
            tid = tile_ids[row * map_w + col]
            if tid != 0:
                if start_col is None:
                    start_col = col
            else:
                if start_col is not None:
                    platforms.append((start_col * tile_w, row * tile_h, (col - start_col) * tile_w, tile_h))
                    start_col = None
        if start_col is not None:
            platforms.append((start_col * tile_w, row * tile_h, (map_w - start_col) * tile_w, tile_h))

    # Parse weapon caches & spawn points
    weapon_spawns = []
    bot_spawns = []
    trees = []

    for og in root.findall("objectgroup"):
        for obj in og.findall("object"):
            name = obj.attrib.get("name", "")
            ox = float(obj.attrib.get("x", 0))
            oy = float(obj.attrib.get("y", 0))
            props = {p.attrib.get("name"): p.attrib.get("value") for p in obj.findall("./properties/property")}

            if name.startswith("wp_p_"):
                weapon_spawns.append((ox, oy, props.get("weapon", "ak47")))
            elif name.startswith("sp_p_"):
                bot_spawns.append((ox, max(60.0, oy)))
            elif "sprite" in props:
                trees.append((ox, oy, props["sprite"]))

    kt_code = f"""package com.rajiv.minimilitia.clone.engine

/**
 * Authentic Outpost Arena Geometry extracted directly from 1outpost_new.tmx
 * World coordinates: 3328 x 1152 (52 x 18 tiles of 64x64)
 */
object OutpostMapData {{
    const val WORLD_WIDTH = {map_w * tile_w}.0f
    const val WORLD_HEIGHT = {map_h * tile_h}.0f

    val PLATFORMS = listOf(
"""
    for p in platforms:
        kt_code += f"        Platform({p[0]}.0f, {p[1]}.0f, {p[2]}.0f, {p[3]}.0f, isOneWay = false, label = \"Terrain\"),\n"
    
    kt_code += """    )

    val WEAPON_SPAWNS = listOf(
"""
    for ws in weapon_spawns:
        kt_code += f"        Triple({ws[0]}f, {ws[1]}f, \"{ws[2]}\"),\n"

    kt_code += """    )

    val BOT_SPAWNS = listOf(
"""
    for bs in bot_spawns:
        kt_code += f"        Pair({bs[0]}f, {bs[1]}f),\n"

    kt_code += """    )

    val MAP_PROPS = listOf(
"""
    for tr in trees:
        kt_code += f"        Triple({tr[0]}f, {tr[1]}f, \"{tr[2]}\"),\n"

    kt_code += """    )
}
"""
    target_file.write_text(kt_code, encoding="utf-8")
    print(f"[+] OutpostMapData.kt generated successfully at {target_file}")

if __name__ == "__main__":
    generate()
