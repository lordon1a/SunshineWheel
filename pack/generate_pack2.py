"""ui2 fontunu (sprite + metin) uretir ve test icin tek parca pack zip'i cikarir.

px ve ui fontlari mevcut generate_pack.py'den aynen alinir; ui2 eklenir.

Kullanim:
    python pack/art/generate_ui2.py          # once resimleri uret
    python pack/generate_pack2.py 88 0       # sonra pack (varsayilan: dist/sunshinewheel-pack-ui2.zip)
"""
import hashlib
import importlib.util
import json
import os
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ART2 = os.path.join(HERE, "art", "out2")

TEXT_CHARS = " ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,:-+%()/!<>#=_*"
TEXT_CHARSET = list(TEXT_CHARS)
TEXT_W, TEXT_H = 6, 10

# Java: render/Ui2.java ile birebir ayni listeler
TEXT_Y = [6, 30, 36, 48, 66, 72, 84, 102, 108, 120, 126, 138, 156, 192]
ART_Y = {
    "panel_l": [0],
    "panel_r": [0],
    "tab_off": [24, 42, 60, 78, 96],
    "tab_on": [24, 42, 60, 78, 96],
    "tab_hover": [24, 42, 60, 78, 96],
    "wide_off": [120],
    "wide_hover": [120],
    "small_off": [186],
    "small_hover": [186],
    "save_off": [186],
    "save_hover": [186],
    "step_off": [42, 78, 114],
    "step_hover": [42, 78, 114],
    "track": [150],
    "knob": [148],
    "knob_hover": [148],
}
SPRITES = list(ART_Y.keys())
TEXT_BASE = 0xE900
TEXT_STRIDE = 64
ART_BASE = 0xF100
ART_STRIDE = 32
SPACE_BITS = 11


def spec_of(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def space_provider() -> dict:
    advances = {}
    for i in range(SPACE_BITS):
        advances[chr(0xE000 + i)] = -(1 << i)
        advances[chr(0xE020 + i)] = 1 << i
    return {"type": "space", "advances": advances}


def ui2_font_json(gp, sizes) -> str:
    providers = [space_provider()]
    for slot, y in enumerate(TEXT_Y):
        for index in range(len(TEXT_CHARSET)):
            providers.append({
                "type": "bitmap",
                "file": f"sunshinewheel:font/ui2/text/t{index}.png",
                "height": TEXT_H,
                "ascent": 1 - y,
                "chars": [chr(TEXT_BASE + slot * TEXT_STRIDE + index)],
            })
    for index, name in enumerate(SPRITES):
        height = sizes[name][1]
        for slot, y in enumerate(ART_Y[name]):
            providers.append({
                "type": "bitmap",
                "file": f"sunshinewheel:font/ui2/{name}.png",
                "height": height,
                "ascent": 1 - y,
                "chars": [chr(ART_BASE + index * ART_STRIDE + slot)],
            })
    return json.dumps({"providers": providers}, ensure_ascii=True, separators=(",", ":"))


def main() -> None:
    major = int(sys.argv[1]) if len(sys.argv) > 1 else 88
    minor = int(sys.argv[2]) if len(sys.argv) > 2 and sys.argv[2].isdigit() else 0
    rest = [a for a in sys.argv[2:] if not a.isdigit()]
    out = rest[0] if rest else os.path.join(HERE, "..", "dist", "sunshinewheel-pack-ui2.zip")
    out = os.path.abspath(out)

    if not os.path.isdir(ART2):
        sys.exit("Once: python pack/art/generate_ui2.py")

    gp = spec_of(os.path.join(HERE, "generate_pack.py"), "gp")
    gen = spec_of(os.path.join(HERE, "art", "generate_ui2.py"), "ui2gen")
    sizes = {name: (w, h) for name, w, h in gen.WIDGETS}
    sizes["panel_l"] = (gen.TILE_W, gen.PANEL_H)
    sizes["panel_r"] = (gen.TILE_W, gen.PANEL_H)
    missing = [n for n in SPRITES if n not in sizes]
    if missing:
        sys.exit(f"generate_ui2.py WIDGETS listesinde eksik sprite: {missing}")
    mcmeta = {"pack": {"description": "SunshineWheel (px + ui + ui2)",
                       "pack_format": major, "min_format": [major, minor], "max_format": [major, minor]}}

    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("pack.mcmeta", json.dumps(mcmeta, indent=2))
        z.writestr("assets/sunshinewheel/font/px.json", gp.px_font_json())
        z.writestr("assets/sunshinewheel/textures/font/px.png", gp.white_pixel_png())
        if os.path.isdir(gp.ART_DIR):
            z.writestr("assets/sunshinewheel/font/ui.json", gp.ui_font_json())
            for name, _w, _h in gp.UI_SPRITES:
                path = os.path.join(gp.ART_DIR, name + ".png")
                if os.path.exists(path):
                    z.writestr(f"assets/sunshinewheel/textures/font/ui/{name}.png", open(path, "rb").read())
        z.writestr("assets/sunshinewheel/font/ui2.json", ui2_font_json(gp, sizes))
        for name in SPRITES:
            z.writestr(f"assets/sunshinewheel/textures/font/ui2/{name}.png",
                       open(os.path.join(ART2, name + ".png"), "rb").read())
        for index in range(len(TEXT_CHARSET)):
            z.writestr(f"assets/sunshinewheel/textures/font/ui2/text/t{index}.png",
                       open(os.path.join(ART2, "text", f"t{index}.png"), "rb").read())

    sha1 = hashlib.sha1(open(out, "rb").read()).hexdigest()
    print(f"{out} uretildi, sha1={sha1}")


if __name__ == "__main__":
    main()
