"""SunshineWheel editör paneli v2: piksel piksel çizim yerine hazır sprite + font.

Neden: panel eskiden 1x1 piksel fontuyla tek tek pikseller hâlinde çiziliyordu; 280x170
bir panel tek karede ~96.000 karakter (~290 KB) üretiyordu ve her fare hareketinde yeniden
gönderiliyordu. Bu dosya paneli 2 arka plan parçası + ~15 widget + metin glifleri hâlinde
üretir; kare başına maliyet ~250-400 karaktere düşer.

Çıktılar:
  pack/art/out2/panel_l.png, panel_r.png   (2 x 180x220 = 360x220 panel zemini)
  pack/art/out2/<widget>.png               (~15 widget)
  pack/art/out2/text/t<idx>.png            (6x10 monospace metin glifleri)
  pack/art/out2/ui2.json                   (font tanımı, pack'e girer)

Kullanım: python pack/art/generate_ui2.py
"""
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

# --- Palet (mevcut panel ile aynı aile) ---
PANEL = (27, 21, 56, 255)
HEADER = (18, 14, 38, 255)
BORDER = (47, 39, 87, 255)
GOLD = (242, 181, 68, 255)
GOLD_DIM = (120, 90, 34, 255)
SIDEBAR = (25, 19, 51, 255)
CONTENT = (30, 24, 64, 255)
PREVIEW = (15, 12, 34, 255)
BTN = (36, 29, 69, 255)
BTN_HOVER = (74, 60, 112, 255)
BTN_ACTIVE = (61, 47, 18, 255)
TRACK = (18, 14, 38, 255)
TEXT_COL = (232, 228, 240, 255)

# --- Ölçüler (Java: render/Ui2.java ile birebir aynı olmalı) ---
PANEL_W, PANEL_H = 360, 220
TILE_W = 180

WIDGETS = [
    ("tab_off", 112, 18),
    ("tab_on", 112, 18),
    ("tab_hover", 112, 18),
    ("wide_off", 112, 20),
    ("wide_hover", 112, 20),
    ("small_off", 56, 20),
    ("small_hover", 56, 20),
    ("save_off", 112, 20),
    ("save_hover", 112, 20),
    ("step_off", 14, 14),
    ("step_hover", 14, 14),
    ("track", 120, 6),
    ("knob", 8, 10),
    ("knob_hover", 8, 10),
    ("cursor", 9, 9),
]

TEXT_CHARS = " ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,:-+%()/!<>#=_*"
TEXT_W, TEXT_H = 6, 10
TEXT_BASELINE = 8
FONT_CANDIDATES = ["consola.ttf", "lucon.ttf", "cour.ttf"]
FONT_SIZE = 10

Y_SLOTS = list(range(0, PANEL_H, 6))     # metin dikey konum adımları
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "out2")


def font():
    for name in FONT_CANDIDATES:
        path = os.path.join(os.environ.get("WINDIR", "C:\\Windows"), "Fonts", name)
        if os.path.exists(path):
            return ImageFont.truetype(path, FONT_SIZE), name
    sys.exit("Monospace TTF bulunamadi: " + ", ".join(FONT_CANDIDATES))


def rounded(draw, box, fill, outline=BORDER, radius=4, width=1):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def build_panel() -> Image.Image:
    """Panel zemini: başlık bandı, sol sekme kutusu, orta içerik, sağ önizleme kutusu."""
    img = Image.new("RGBA", (PANEL_W, PANEL_H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    rounded(d, [0, 0, PANEL_W - 1, PANEL_H - 1], PANEL, BORDER, radius=6, width=2)
    rounded(d, [2, 2, PANEL_W - 3, 22], HEADER, BORDER, radius=5)
    d.line([(3, 23), (PANEL_W - 4, 23)], fill=GOLD)
    d.ellipse([9, 8, 17, 16], fill=GOLD)                      # başlık güneşi
    for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        d.point((13 + dx * 6, 12 + dy * 6), fill=GOLD_DIM)

    rounded(d, [8, 30, 127, 213], SIDEBAR, BORDER, radius=4)      # sekmeler
    rounded(d, [134, 30, 272, 213], CONTENT, BORDER, radius=4)    # içerik
    rounded(d, [278, 30, 352, 213], PREVIEW, GOLD_DIM, radius=4)  # önizleme
    return img


def build_widget(name: str, w: int, h: int) -> Image.Image:
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    if name == "track":
        rounded(d, [0, 0, w - 1, h - 1], TRACK, BORDER, radius=2)
        return img
    if name.startswith("knob"):
        rounded(d, [0, 0, w - 1, h - 1], BTN_HOVER if name.endswith("hover") else GOLD, BORDER, radius=3)
        return img
    if name.startswith("tab"):
        fill = BTN_ACTIVE if name == "tab_on" else (BTN_HOVER if name.endswith("hover") else BTN)
        rounded(d, [0, 0, w - 1, h - 1], fill, GOLD if name == "tab_on" else BORDER, radius=4)
        d.line([(3, h - 2), (w - 4, h - 2)], fill=GOLD if name == "tab_on" else GOLD_DIM)
        return img
    if name.startswith("save"):
        rounded(d, [0, 0, w - 1, h - 1], BTN_HOVER if name.endswith("hover") else BTN_ACTIVE, GOLD, radius=4)
        return img
    rounded(d, [0, 0, w - 1, h - 1], BTN_HOVER if name.endswith("hover") else BTN, BORDER, radius=4)
    return img


def build_text_glyphs() -> None:
    """Her karakteri 6x10 hücreye, taban çizgisi 8 olacak şekilde çizer."""
    f, _name = font()
    for index, ch in enumerate(TEXT_CHARS):
        img = Image.new("RGBA", (TEXT_W, TEXT_H), (0, 0, 0, 0))
        if ch != " ":
            ImageDraw.Draw(img).text((0, TEXT_BASELINE), ch, font=f, fill=TEXT_COL, anchor="ls")
        # İlerleme = en sağdaki saydam olmayan sütun + 1. Monospace hizalaması için son sütuna
        # görünmez (alpha=1) bir piksel koyuyoruz; tüm glifler böylece aynı ilerlemede olur.
        img.putpixel((TEXT_W - 1, 0), (TEXT_COL[0], TEXT_COL[1], TEXT_COL[2], 1))
        img.save(os.path.join(OUT, "text", f"t{index}.png"))


def main() -> None:
    os.makedirs(os.path.join(OUT, "text"), exist_ok=True)
    _f, used = font()
    print("yazi tipi:", used)

    panel = build_panel()
    panel.crop((0, 0, TILE_W, PANEL_H)).save(os.path.join(OUT, "panel_l.png"))
    panel.crop((TILE_W, 0, PANEL_W, PANEL_H)).save(os.path.join(OUT, "panel_r.png"))
    for name, w, h in WIDGETS:
        build_widget(name, w, h).save(os.path.join(OUT, name + ".png"))
    build_text_glyphs()

    print(f"{len(WIDGETS) + 2} sprite ve {len(TEXT_CHARS)} metin glifi uretildi -> {OUT}")
    print("sonraki adim: python pack/generate_pack2.py  (ui2.json ve pack.zip)")
if __name__ == "__main__":
    main()

