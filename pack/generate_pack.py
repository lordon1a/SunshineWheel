"""SunshineWheel kaynak paketini üretir.

Kullanım:
    python pack/generate_pack.py <major> [minor] [çıktı.zip]

Minecraft 26.2 için kaynak paketi formatı 88.0 (sunucu jar'ındaki version.json -> pack_version).

İki font üretilir:

1. sunshinewheel:px — 1x1 piksel fontu. Çalışma anında üretilen çizimler için (çarkın kendisi).
   U+E100+r : r. satır pikseli (height=1, ascent=1-r), r = 0..255
   U+E000+i : -2^i boşluk, U+E020+i : +2^i boşluk (i = 0..10)

2. sunshinewheel:ui — hazır arayüz resimleri. Her parça tek bir PNG ve 17 karakter
   (dikey ince ayar için ascent varyantları). UiSprite.java ile birebir aynı sırada olmalı.

Canlı sunucuda bu dosyalar Oraxen paketine birleştirilir (assets/sunshinewheel/...).
"""
import hashlib
import json
import os
import struct
import sys
import zipfile
import zlib

ROWS = 256
SPACE_BITS = 11

# UiSprite.java ile birebir aynı sıra ve ölçüler
UI_SPRITES = [
    ("panel_r0c0", 180, 140),
    ("panel_r0c1", 180, 140),
    ("panel_r1c0", 180, 140),
    ("panel_r1c1", 180, 140),
    ("panel_r2c0", 180, 140),
    ("panel_r2c1", 180, 140),
    # Sandık menüsünün arka planı: pencerenin tamamını kaplar, tek parçadır.
    ("menu_bg", 176, 222),
]
UI_BASE = 0xE200
UI_STRIDE = 512
# Dikey konum tamamen ascent ile verilir (satır atlanmaz), böylece resim dialog'da yer kaplamaz.
UI_Y_MIN = -8
UI_Y_MAX = 420

ART_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "art", "out")


def white_pixel_png() -> bytes:
    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", 1, 1, 8, 6, 0, 0, 0)  # 1x1, 8 bit, RGBA
    pixels = zlib.compress(b"\x00\xff\xff\xff\xff")
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", pixels) + chunk(b"IEND", b"")


def space_provider() -> dict:
    advances = {}
    for i in range(SPACE_BITS):
        advances[chr(0xE000 + i)] = -(1 << i)
        advances[chr(0xE020 + i)] = 1 << i
    return {"type": "space", "advances": advances}


def px_font_json() -> str:
    providers = [space_provider()]
    for r in range(ROWS):
        providers.append({
            "type": "bitmap",
            "file": "sunshinewheel:font/px.png",
            "height": 1,
            "ascent": 1 - r,
            "chars": [chr(0xE100 + r)],
        })
    return json.dumps({"providers": providers}, ensure_ascii=True, indent=1)


def ui_font_json() -> str:
    # Boşluklar iki fontta da tanımlı olmalı: çizim tek bir font altında yapılıyor.
    providers = [space_provider()]
    for index, (name, _width, height) in enumerate(UI_SPRITES):
        for y in range(UI_Y_MIN, UI_Y_MAX + 1):
            # ascent: taban çizgisinin üstündeki yükseklik. 7 = satırın üst kenarı, artan y aşağı iner.
            providers.append({
                "type": "bitmap",
                "file": f"sunshinewheel:font/ui/{name}.png",
                "height": height,
                "ascent": 7 - y,
                "chars": [chr(UI_BASE + index * UI_STRIDE + (y - UI_Y_MIN))],
            })
    return json.dumps({"providers": providers}, ensure_ascii=True, indent=1)


def main() -> None:
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    major = int(sys.argv[1])
    minor = int(sys.argv[2]) if len(sys.argv) > 2 and sys.argv[2].isdigit() else 0
    rest = [a for a in sys.argv[2:] if not a.isdigit()]
    out = rest[0] if rest else "sunshinewheel-pack.zip"

    missing = [n for n, _w, _h in UI_SPRITES if not os.path.exists(os.path.join(ART_DIR, n + ".png"))]
    if missing:
        sys.exit(f"Arayüz resimleri eksik: {missing}. Önce: python pack/art/generate_ui.py")

    mcmeta = {
        "pack": {
            "description": "SunshineWheel fontları",
            "pack_format": major,
            "min_format": [major, minor],
            "max_format": [major, minor],
        }
    }
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("pack.mcmeta", json.dumps(mcmeta, indent=2))
        z.writestr("assets/sunshinewheel/font/px.json", px_font_json())
        z.writestr("assets/sunshinewheel/textures/font/px.png", white_pixel_png())
        z.writestr("assets/sunshinewheel/font/ui.json", ui_font_json())
        for name, _width, _height in UI_SPRITES:
            with open(os.path.join(ART_DIR, name + ".png"), "rb") as f:
                z.writestr(f"assets/sunshinewheel/textures/font/ui/{name}.png", f.read())

    sha1 = hashlib.sha1(open(out, "rb").read()).hexdigest()
    print(f"{out} üretildi, sha1={sha1}")


if __name__ == "__main__":
    main()
