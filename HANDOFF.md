# SunshineWheel — Devir Notları

Bu dosya, projeyi devralacak kişi (veya AI) için yazıldı. Amaç: çalışanı bozmamak ve
**aynı çıkmaz sokaklara tekrar girmemek**.

Sunucu: Sunshine Survival (Minecraft Java 26.2, Paper + Velocity), `play.guldal.me`.

---

## 1. Şu an ne çalışıyor

Plugin canlı sunucuda kurulu ve hatasız çalışıyor (`/home/minecraft/plugins/SunshineWheel-0.1.0.jar`).

- **Dünyadaki çark:** Dönüyor, sağ tıkla çevriliyor, ödül veriyor. Çark görüntüsü çalışma anında
  üretiliyor (dilim sayısı ve renkler ödüllerden geliyor).
- **Ödül sistemi:** Ad, ağırlık (şans), dilim rengi, ikon eşyası, komutlar. Ekleme/silme var.
- **Maliyet ve bilet, bekleme süresi, sesler, parçacıklar, hologram** ayarları var.
- **Yerleştirme modu:** `/wheel create <id>` ve `/wheel move <id>` — sonra bir bloğa sağ tıklıyorsun.
  Zemine koyarsan üstünde durur, duvara koyarsan asılır.
- **İki editör:** `/wheel edit <id>` display entity paneli, `/wheel dialog <id>` dialog ekranı.

### Komutlar
```
/wheel create <id>     yerlestirme modu (bloga sag tikla)
/wheel move <id>       carki tasi
/wheel edit <id>       panel editor (dunyada duran panel, bakisla imlec)
/wheel dialog <id>     dialog editor (yazi girisi burada)
/wheel menu            sandik menusu hizalama testi
/wheel spin <id>       test cevirme
/wheel set <id> <ayar> <deger>
/wheel tune <ayar> <deger>      panel/metin kalibrasyonu (restart gerekmez)
/wheel ui <dx> <dy>             dialog panel resmi kaydirmasi
/wheel angle <id> <derece>      okun altindaki dilimi dogrulama
/wheel list | remove | reload | cancel
```

---

## 2. Teknik temel: yazıyla resim çizmek

**GÜNCEL (v2):** Editör paneli artık piksel piksel değil, **hazır sprite + gerçek font** ile çiziliyor.
Eski yöntem 280x170 panel için kare başına **96.223 karakter (~289 KB)** üretiyordu ve her fare
hareketinde yeniden gönderiliyordu (oyuncu başına ~2.9 MB/s) — panelin "olmadığı" yer buydu.
Ölçülmüş yeni değerler: panel karesi **~392 karakter (~1.2 KB)**, önizleme ayrı display'de ve
yalnızca ödül/renk değişince yeniden kodlanıyor (~5.000 karakter, seyrek). ~245 kat iyileşme.

Yeni dosyalar:
- `render/Ui2.java` — sprite + metin birleştirici tuval (`Ui2.Canvas`), karakter aralıkları:
  U+E900+slot\*64+harf (metin), U+F100+sprite\*32+slot (widget)
- `render/PixelText.encodeAt(...)` — görüntüyü mevcut satırın içine (x,y) koymak için (önizleme/imleç)
- `render/PixelText.pad(...)` — alt boş satırlar (merkezleme)
- `editor/EditorPanel.java` — Component tabanlı yeni panel (aynı buton/hit-test mantığı)
- `editor/EditorSession.java` — panel display'i + **ayrı önizleme display'i** (positionPreview)
- `WheelListener` — F (el değiştirme) = kapat, fare tekerleği (PlayerItemHeldEvent) = kaydırıcı ince ayar
- `pack/art/generate_ui2.py` — panel zeminini (2 parça), 15 widget'ı ve 53 metin glifini (Consolas 10,
  6x10 hücre, ilerleme 7) üretir
- `pack/generate_pack2.py` — px + ui + **ui2** fontlarını tek pack.zip'te toplar
  (`dist/sunshinewheel-pack-ui2.zip`, ui2.json ~81 KB / 775 provider)

**Kritik kural:** `pack/generate_pack2.py`, `render/Ui2.java` ve `editor/EditorPanel.java`
içindeki konum listeleri (TEXT_Y, ART_Y, PANEL_W/H, TEXT_CHARS) birebir aynı olmalı.
Konum eklemek istersen üçünü birden güncelle, sonra:
`python pack/art/generate_ui2.py` → `python pack/generate_pack2.py 88 0` → `gradlew build`.

**Paket dağıtımı:** ui2 fontu canlıya giderken Oraxen zincirine de kopyalanmalı
(`wheel_ui_deploy.sh` bunu yapıyor; assets/sunshinewheel/ui2.json + ui2 dokuları).
Font canlı pakette yoksa panel boş/kutu görünür — panelin "çalışmıyor" sanılmasının
bir diğer sebebi bu.

Minecraft'ta sunucu tarafından özel arayüz çizmenin yolu **kaynak paketine font koymak**.
Resimler birer "harf" olur, negatif boşluk karakterleriyle istenen yere kaydırılır.

Pakette iki font var (`assets/sunshinewheel/`):

| Font | Ne işe yarar |
|---|---|
| `sunshinewheel:px` | 1x1 piksel. Çalışma anında üretilen çizimler (çarkın kendisi, editör paneli). |
| `sunshinewheel:ui` | Hazır PNG'ler tek karakter olarak. Panel arka planları. |

**Önemli kurallar (hepsi denenerek öğrenildi):**
- Bir glif en fazla **256x256** piksel. Daha büyük resim parçalara bölünmeli.
- Glif genişliği en sağdaki saydam olmayan sütundan hesaplanır, ilerlemesi `genişlik + 1`.
  Sağ kenarda tamamen saydam sütun kalırsa resim kayar (üreticide 1 piksel zemin ekleniyor).
- Dikey konum `ascent` ile verilir: `ascent = 7 - y`. Satır atlamak yerine ascent varyantı
  kullanılmalı, yoksa **çizim dialog içinde yer kaplar ve içeriği aşağı iter**.
- Bir satırın **net ilerlemesi 0** bırakılmalı, yoksa oyun satırı ortalarken resmi sağa kaydırır.
- Dialog gövdesinde genişlik küçük verilirse oyun satırı kırar ve **resmi ikiye böler**.

İlgili sınıflar: `render/PixelText.java` (piksel→metin), `render/PixelImage.java`,
`render/Glyphs.java` (5x7 font), `render/UiCanvas.java` + `render/UiSprite.java` (hazır resimler),
`render/WheelArt.java` (çark çizimi).

---

## 3. Denenen üç arayüz mimarisi ve neden tutmadıkları

Hedef: Sam's Wheel eklentisindeki editör görünümü (sol sekme sütunu, ortada ayarlar, sağda canlı
önizleme). **Üçü de denendi.**

### A) Dialog API (`/wheel dialog`)
Gerçek yazı kutuları ve kaydırıcılar var, çalışıyor. **Ama:**
- İçerik tek sütun ve ortalı; sol sütun/sağ sütun yerleşimi mümkün değil.
- Ekran ikiye bölünmüş: üstte **kayan** içerik alanı, altta **sabit** buton şeridi.
  Arka plan resmi içerik alanına ait olduğu için butonların arkasına geçemiyor.
  Paneli uzatmak işe yaramıyor — ikisi ayrı bölgelerde ve ayrı kayıyorlar.

### B) Sandık menüsü (`/wheel menu`)
Altyapısı kuruldu (`menu/ChestMenu.java`) ve çalışıyor. Kare ızgarası ölçüldü:
**(8, 18)'den başlıyor, 18 piksel aralıklı**, başlık yazısı pencerenin **(8, 6)** noktasına çiziliyor.
Tıklanan yerler sabit, hizalama kesin. **Ama** görünüm Minecraft'ın pikselli yazı tipiyle sınırlı ve
kullanıcı bunu istemedi.

### C) Display entity paneli (`/wheel edit`) — **doğru olan buydu**
Dünyaya konan, oyuncuya dönük tek bir TextDisplay. İmleç, oyuncunun bakış ışınının panel düzlemiyle
kesişiminden bulunuyor (`editor/EditorSession.java`). Sol tıkla butona basılıyor.

**Sam's Wheel'in yöntemi budur.** Kaynak: <https://docs.samscreations.eu/> — SamsRank'in
"In-Game Editor" sayfasında aynen şöyle yazıyor: *"a revolutionary UI built entirely from display
entities"* ve *"Move with your mouse to move the cursor. The cursor sensitivity can be adjusted in
the config.yml"*.

> **Uyarı:** Bu yaklaşım bir kez yazılıp, ekran görüntülerine bakılarak "yanlış yol" sanılıp silindi;
> sonra dokümanı okuyunca doğru olduğu anlaşılıp baştan yazıldı. Aynı hatayı tekrarlama —
> **önce dokümanı oku.**

### Sam's dokümanından çıkan diğer bilgiler (henüz uygulanmadı)
- Görünüm modeli: `appearance.radius` (blok), `frame-color`, `hub-color`, `pointer-color`,
  `appearance.variant` (7 hazır tasarım).
- Tasarımlar dilim sayısına göre **64x64 PNG** dosyaları: `wheel_2.png` … `wheel_12.png`.
- Ödül adları dilimlerin altında; görünme modu `ALWAYS` / `LOOK` / `ITEM`, ayrıca "name gap" ayarı.
- Paket eklentinin kendi HTTP sunucusuyla dağıtılıyor (varsayılan port 8765).

---

## 4. Derleme ve yayınlama

```powershell
# derleme (JDK 26 ile, Java 25 hedefli)
$env:JAVA_HOME="C:\Program Files\Java\jdk-26.0.2"
.\gradlew.bat build

# kaynak paketi (once resimler, sonra zip)
python pack\art\generate_ui.py
python pack\generate_pack.py 88 0 run\sunshinewheel-pack.zip
```

Sunucuya yükleme için `scratchpad` klasöründeki scriptler kullanıldı:
- `wheel_update.sh` — sadece jar günceller, Minecraft'ı yeniden başlatır (paket değişmediyse bu yeter).
- `wheel_ui_deploy.sh` — paketi de günceller: canlı paketin **kopyasına** `assets/sunshinewheel/`
  dosyalarını yazar, yeni sha1 hesaplar, `/var/www/rehber/pack-<sha1>.zip` olarak koyar,
  Velocity ayarını çevirir, https üzerinden indirip sha1 doğrular, Oraxen kaynak klasörünü günceller.

**Paket zinciri (önemli):** Oraxen paketi üretir → optimize edilmiş kopya `rehber.guldal.me`'de durur →
Velocity `velocityresourcepacks/config.yml` içindeki url ve hash ile oyunculara gönderir.
Yeni bir şey eklenince **Oraxen kaynak klasörüne de** kopyalanmalı, yoksa paket bir dahaki
üretilişinde kaybolur.

**Geri alma:** Eski paket dosyaları silinmiyor. Velocity config'indeki hash'i eskisine çevirip
`systemctl restart velocity` demek yeterli.

**Yedekler:** `/home/minecraft/backups/sunshinewheel_*` (Velocity config + önceki jar + Oraxen listesi).

---

## 5. Yapılacaklar

1. Panelin görünümünü Sam's seviyesine çıkarmak (şu an sade; hizalama çalışıyor).
2. Çarkın çevresine ödül adları ve yüzdeler (Sam'de var, bizde yok).
3. Görünüm modelini Sam's gibi değiştirmek: blok cinsinden yarıçap + çerçeve/merkez/ok rengi + tasarım.
4. Panelde imleç hassasiyeti ayarı.
5. Örnek ödüller hâlâ **test modunda** — sadece mesaj gönderiyorlar, gerçek ödül vermiyorlar
   (`WheelDefinition.withSampleRewards`). Canlıya alınırken komutları gerçek ödüllerle değiştir.
6. Para kontrolü yok: ücret komutla alınıyor, bakiye kontrol edilmiyor (Vault bağlantısı gerekir).

---

## 6. Güvenlik notu

Sunucunun root şifresi bu proje sırasında sohbette paylaşıldı. **Değiştirilmeli** (`passwd`) ve
tercihen SSH anahtarına geçilmeli. Proje dosyalarında şifre yok; bağlantı scriptleri şifreyi
ortam değişkeninden okuyordu.
