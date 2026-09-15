"""SunshineWheel editör paneli için arayüz resimlerini üretir.

Kullanım: python pack/art/generate_ui.py

Çıktı: pack/art/out/panel_r{satır}c{sütun}.png

Tek bir font karakteri en fazla 256x256 piksel olabildiği için panel parçalara bölünür.
Parça ölçüleri UiSprite.java ve generate_pack.py ile birebir aynı olmak zorundadır.

Not: Dialog içeriği tek sütun ve ortalıdır; sol/sağ sütun yerleşimi mümkün değil. Bu yüzden panel
sade bir çerçeve + başlık bandıdır. Sol kenardaki cetvel, içeriğin hangi yüksekliğe düştüğünü tek
ekran görüntüsünden ölçebilmek içindir (hizalama oturunca kaldırılacak).
"""
import math
import os

from PIL import Image, ImageDraw

# UiSprite.java ve generate_pack.py ile aynı olmalı
PANEL_W, PANEL_H = 360, 420
TILE_W, TILE_H = 180, 140

PANEL = (23, 18, 51, 255)       # #171233
PANEL_SOFT = (29, 23, 62, 255)
BORDER = (42, 35, 80, 255)      # #2A2350
GOLD = (242, 181, 68, 255)      # #F2B544
GOLD_DIM = (120, 90, 34, 255)
HEADER = (18, 14, 38, 255)
RULER = (60, 50, 100, 255)

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "out")


def build_panel() -> Image.Image:
    img = Image.new("RGBA", (PANEL_W, PANEL_H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Gövde ve çerçeve
    d.rounded_rectangle([0, 0, PANEL_W - 1, PANEL_H - 1], radius=6, fill=PANEL, outline=BORDER, width=1)
    d.rounded_rectangle([3, 3, PANEL_W - 4, PANEL_H - 4], radius=5, outline=GOLD_DIM, width=1)

    # Üst başlık bandı
    d.rounded_rectangle([1, 1, PANEL_W - 2, 26], radius=5, fill=HEADER)
    d.rectangle([1, 20, PANEL_W - 2, 26], fill=HEADER)
    d.line([(1, 27), (PANEL_W - 2, 27)], fill=GOLD)

    # Başlıktaki güneş
    cx, cy = 18, 14
    d.ellipse([cx - 5, cy - 5, cx + 5, cy + 5], fill=GOLD)
    for i in range(8):
        a = math.radians(i * 45)
        d.line([(cx + math.cos(a) * 7, cy + math.sin(a) * 7), (cx + math.cos(a) * 9, cy + math.sin(a) * 9)], fill=GOLD)

    # İçerik alanı (tek sütun, ortalı)
    d.rounded_rectangle([10, 32, PANEL_W - 11, PANEL_H - 10], radius=4, fill=PANEL_SOFT, outline=BORDER, width=1)

    # Geçici cetvel: 9 pikselde bir soluk, 45 pikselde bir altın çizgi
    for y in range(32, PANEL_H - 10, 9):
        long_tick = (y - 32) % 45 == 0
        color = GOLD_DIM if long_tick else RULER
        d.line([(11, y), (11 + (8 if long_tick else 4), y)], fill=color)

    return img


# Sandık menüsü: pencere 176x222, kare ızgarası (8,18)'den başlar ve 18 piksel aralıklıdır
# (paketin kendi generic_54.png dokusundan ölçüldü). Başlık yazısı pencerenin (8,6) noktasına çizilir,
# yani resim başlıktan -8,-6 kaydırılarak pencerenin sol üst köşesine oturtulur.
MENU_W, MENU_H = 176, 222
SLOT_X, SLOT_Y, SLOT_PITCH = 8, 18, 18


def build_menu() -> Image.Image:
    img = Image.new("RGBA", (MENU_W, MENU_H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Tüm pencereyi kaplayan zemin (altındaki vanilla sandık dokusunu tamamen örter)
    d.rounded_rectangle([0, 0, MENU_W - 1, MENU_H - 1], radius=4, fill=PANEL, outline=BORDER, width=1)

    # Üst başlık bandı (başlık yazısı bunun üstüne düşer)
    d.rectangle([1, 1, MENU_W - 2, 15], fill=HEADER)
    d.line([(1, 16), (MENU_W - 2, 16)], fill=GOLD)
    cx, cy = 8, 8
    d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=GOLD)

    def cell(col: int, row: int) -> tuple[int, int]:
        return SLOT_X + col * SLOT_PITCH, SLOT_Y + row * SLOT_PITCH

    # Sol sekme sütunu (0. sütun, 6 satır)
    x0, y0 = cell(0, 0)
    d.rounded_rectangle([x0 - 2, y0 - 2, x0 + SLOT_PITCH - 2, y0 + 6 * SLOT_PITCH - 3],
                        radius=3, fill=PANEL_SOFT, outline=GOLD_DIM, width=1)

    # Orta ayar alanı (2-6. sütunlar)
    x1, _ = cell(2, 0)
    x2, _ = cell(6, 0)
    d.rounded_rectangle([x1 - 2, y0 - 2, x2 + SLOT_PITCH - 3, y0 + 6 * SLOT_PITCH - 3],
                        radius=3, fill=PANEL_SOFT, outline=BORDER, width=1)

    # Sağ önizleme kutusu (7-8. sütunlar, ilk 5 satır)
    x3, _ = cell(7, 0)
    d.rounded_rectangle([x3 - 2, y0 - 2, MENU_W - 7, y0 + 5 * SLOT_PITCH - 3],
                        radius=3, fill=(12, 9, 26, 255), outline=GOLD, width=1)

    # Oyuncu envanteri bölgesini karart (menüde dikkat dağıtmasın)
    d.rectangle([0, 124, MENU_W - 1, MENU_H - 1], fill=(10, 8, 22, 235))

    return img


def main() -> None:
    os.makedirs(OUT, exist_ok=True)
    menu = build_menu()
    menu.save(os.path.join(OUT, "menu_bg.png"))
    print("uretildi: menu_bg.png", f"{MENU_W}x{MENU_H}")
    panel = build_panel()
    for row in range(PANEL_H // TILE_H):
        for col in range(PANEL_W // TILE_W):
            tile = panel.crop((col * TILE_W, row * TILE_H, (col + 1) * TILE_W, (row + 1) * TILE_H))
            # Glif genişliği en sağdaki saydam olmayan sütundan hesaplanır; sağ kenarda tamamen
            # saydam bir sütun kalırsa parça kayar. 1 piksel neredeyse görünmez zemin ekliyoruz.
            px = tile.load()
            for y in range(TILE_H):
                if px[TILE_W - 1, y][3] == 0:
                    px[TILE_W - 1, y] = (PANEL[0], PANEL[1], PANEL[2], 1)
            name = f"panel_r{row}c{col}.png"
            tile.save(os.path.join(OUT, name))
            print("uretildi:", name, f"{TILE_W}x{TILE_H}")


if __name__ == "__main__":
    main()
