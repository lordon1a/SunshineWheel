# SunshineWheel — Devir Notları

Bu dosya, projeyi devralacak kişi (veya AI) için yazıldı. Amaç: çalışanı bozmamak ve
**aynı çıkmaz sokaklara tekrar girmemek**.

Sunucu: Sunshine Survival (Minecraft Java 26.2, Paper + Velocity), `play.guldal.me`.

---

## 1. Şu an ne çalışıyor

Plugin canlı sunucuda kurulu ve hatasız çalışıyor (`/home/minecraft/plugins/SunshineWheel-1.0.0.jar`).
Sürüm 1.0.0 (2026-09-15): Java 21 hedefi, Paper 1.21 API, messages.yml (EN/TR), örnek çarklar,
paket otomatik servisi. Release zibi: `build/distributions/SunshineWheel-release-1.0.0.zip`
(jar + README + LICENSE + CHANGELOG + web/ + pack/sunshinewheel-pack.zip + examples/).
GitHub Releases'te v1.0.0 (private): jar + tam zip. Değişiklik günlüğü: CHANGELOG.md.
Faz 1 kullanıcı testinden geçti (çevirme/TR mesajlar/web düzenleme). Mağaza taslağı: STORE_LISTING.md.
**BuiltByBit ilanı yayında** (2026-09-15 akşamı).

- **Dünyadaki çark:** Dönüyor, sağ tıkla çevriliyor, ödül veriyor. Çark görüntüsü çalışma anında
  üretiliyor (dilim sayısı ve renkler ödüllerden geliyor).
- **Ödül sistemi:** Ad (<chance> destekli), ağırlık (şans), dilim rengi, ikon eşyası (+adet,
  lore, glow, custom-model-data), komutlar (%player% %reward% %wheel%), ödül izni, ödül sesi/mesajı,
  eşya olarak verme, aç/kapa. Ekleme/silme var.
- **Maliyet:** Bilet eşyası + para (Vault, bakiyeli) + XP seviyesi + konsol komutları; hepsi birlikte
  denetlenir, yarım ödeme olmaz. Ücretsiz çevirme hakkı (`/wheel give|take`, data.yml'de saklanır).
- **Kutlama:** Kazanma patlaması (parçacık) + efekt (yok/havai fişek/roket/sarmal) + dönüş sırası
  ortam parçacığı; yayın (broadcast) açılabilir; çark/ödül izni ve aç/kapa.
- **Görünüm:** Piksel/boyut + çerçeve/merkez/ok renkleri (çark çalışma anında yeniden çizilir).
- **Oyuncu önizlemesi:** Eğilerek sağ tık veya `/wheel preview <id>` → sohbette ödül + yüzde listesi.
- **Admin:** `/wheel ticket give|take`, `/wheel stats [oyuncu]`, `/wheel resetcooldown <oyuncu> <çark>`.
- **Entegrasyon:** PlaceholderAPI (`%sunshinewheel_<id>_spins|wins|free|cooldown%`), developer
  event'leri (`WheelSpinStartEvent`, `WheelSpinWinEvent`, iptal edilebilir).
- **Dil:** Oyun içi mesajlar `messages.yml` (EN varsayılan) + `messages_tr.yml`; `config.yml`te
  `language: en|tr`. Web arayüzü hep İngilizce. Konsol logları İngilizce.
- **Örnek içerik:** İlk kurulumda daily / premium / vote çarkları otomatik oluşur (konumsuz,
  `/wheel move` ile yerleştirilir); `/wheel create` 3 gerçek ödüllü başlangıç seti verir.
  (2026-09-15 öncesi test çarkları silindi; canlıda a + test1 var.)
- **Güvenlik (2026-09-15 denetimi):** Ücret iadesi (yarıda kesilen spin), kalıcı bekleme süresi,
  ses anahtarı doğrulaması, ödül/lore/komut limitleri (64/32/64), `/wheel remove` spin koruması,
  `POST /api/logout` ile oturum iptali, nginx rate-limit (`/wheel/api/` 10r/sn), CORS yok,
  bilet tanınmazsa uyarı + kapalı sayma, bozuk isim koruması. Bilinen sınırlar: oturum 12 saat
  (çalınırsa TTL sonuna kadar geçerli), aynı anda iki admin yazarsa son yazan kazanır, restart
  bekleyen spin'i iptal eder (ücret iade edilir).
- **Kurulum rehberi (EN):** https://rehber.guldal.me/wheel-setup.html (dosya: /var/www/rehber/
  wheel-setup.html). `/wheel web` çıktısı ve README/listing bu linki gösterir; `web.guide-url`
  ile değiştirilebilir. Messages artık jar varsayılanlarıyla birleşir (eksik anahtar = Missing
  message hatası olmaz).
- **Maliyet ve bilet, bekleme süresi, sesler, parçacıklar, hologram** ayarları var.
- **Yerleştirme modu:** `/wheel create <id>` ve `/wheel move <id>` — sonra bir bloğa sağ tıklıyorsun.
  Zemine koyarsan üstünde durur, duvara koyarsan asılır.
- **Web editör (asıl arayüz):** Tek gömülü sunucu (`web/WebServer.java`): `/` editör arayüzü
  (jar içinden), `/api/*` JSON API, `/pack.zip` kaynak paketi — hepsi `web.bind:web.port` adresinde.
  Alıcı kurulumu yok: `web.bind: 0.0.0.0` yapıp `http://IP:8770/` adresini açması yeterli.
  Canlıda nginx HTTPS proxy (`/wheel/` → 127.0.0.1:8770) kullanılıyor; `web.bind` canlıda
  açıkça 127.0.0.1'e sabitli (jar varsayılanı 0.0.0.0). Hız limiti eklenti içinde
  (genel 120/dk, giriş 10/dk; localhost arkasında X-Forwarded-For okunur).
  Giriş: oyunda `/wheel web` → 10 dk geçerli tek kullanımlık anahtar → tarayıcıda oturum (12 saat).
  Arayüz `/var/www/wheel` altında statik dosyalar (index.html, app.js, style.css) — restart
  gerekmeden güncellenir. Düzen Sam's gibi: solda bölüm kenar çubuğu (General … Particles) +
  altta Save & apply, ortada formlar, sağda canvas canlı önizleme. Arayüz tamamen İngilizce;
  API hata mesajları, MotionStyle adları ve konsol logları da İngilizce.
  Kaydet & Uygula, YAML'ı yazar ve çarkı canlıda yeniden doğurur; çark dönerken kaydetme 409 ile
  engellenir.
- **Kaynak paketi (ürün):** Çark `sunshinewheel:px` fontuyla çizilir. Eklenti ilk açılışta
  `plugins/SunshineWheel/pack.zip` üretir (mcmeta format 34 + px.json + 1x1 px.png, ~2KB,
  `web/PackServer.java`, AWT'siz) ve `pack.port` (varsayılan 8765) üzerinden servis eder; konsola
  URL + sha1 basar (`server.properties`: resource-pack / resource-pack-sha1). Birleştirmek isteyen
  aynı zip'ten `assets/sunshinewheel` klasörünü alır. Canlıda mevcut Velocity zinciri kullanılıyor;
  8765 yalnızca localhost testinde doğrulandı (2026-09-15).
- **Konumsuz çark koruması:** `WheelInstance.base()` world null ise null döner (2026-09-15'te
  1.21 testinde yakalandı: örnek çarklar `Bukkit.getWorld(null)` ile açılışı çökertiyordu).
- **Yerel 1.21 testi:** `run/paper121/` (git'e girmez) — Paper 1.21.1 build 133 + JDK 21,
  portsuz çakışma için 25566. Başlatma: Start-Process ile bağımsız (Start-Job oturumlar arası
  ölüyor). İlk açılışta daily/premium/vote oluşur; pack+API localhost'tan doğrulanır.
- **In-game editör KALDIRILDI (2026-09-15 akşamı):** HUD/dialog editörleri (`/wheel edit`,
  `/wheel dialog`, `/wheel menu`, `/wheel ui`) silindi; `editor/`, `menu/` paketleri ve
  `render/Ui2.java`, `UiCanvas.java`, `UiSprite.java`, `Glyphs.java` artık yok. Karar: karmaşık
  ayarlar için tarayıcı arayüzü daha hızlı ve kaliteli; Sam's Wheel benzeri HUD denemsi bir gün
  sürdü, merkezleme/kalibrasyon sorunlarıyla uğraşmak yerine web'e geçildi.
  Paketteki `sunshinewheel:ui2` fontu ve dokuları artık kullanılmıyor (paketten temizlenebilir).

### Komutlar
```
/wheel web              web editor baglantisi uretir (tek kullanimlik, 10 dk)
/wheel create <id>      yerlestirme modu (bloga sag tikla)
/wheel move <id>        carki tasi
/wheel spin <id>        test cevirme
/wheel set <id> <ayar> <deger>
/wheel tune <ayar> <deger>      metin kalibrasyonu (restart gerekmez)
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

### C) Display entity paneli (`/wheel edit`) — denendi, sonra terk edildi
Önce dünyaya sabit panel + bakış ışını imleci, sonra kamera önü HUD + sanal imleç (`cursor-sensitivity`)
denendi. Çalıştı ama görsel kalite ve karmaşık formlar (ödül listesi, renk/ikon seçici) için
display-entity yaklaşımı çok emek istiyordu; merkezleme (yarım panel kayması) ve hareket hâlinde
resim yeniden örneklemesi (bulanıklık) sorunları vardı. **2026-09-15: terk edildi, kaldırıldı.**
Şu an tek arayüz tarayıcıdaki web editör.

Sam's Wheel'in kendi yöntemi (dokümantasyondan): display entity'lerle in-world HUD, "look around to
move the cursor", F ile kapat. Kaynak: <https://docs.samscreations.eu/> — *"a revolutionary UI built
entirely from display entities"*, *"the UI is projected in front of your camera"*.

### Neden web editör?
- Gerçek formlar, kaydırıcılar, renk seçici, aranabilir eşya listesi; Türkçe dahil sınırsız metin.
- Önizleme canvas'ta anında; oyuna girmeden düzenleme, kaydetmeden deneme.
- API localhost'ta; arayüz dosyaları restart'sız güncellenebilir (`/var/www/wheel`).

### Sam's dokümanından çıkan diğer bilgiler (henüz uygulanmadı)
- Görünüm modeli: `appearance.radius` (blok), `frame-color`, `hub-color`, `pointer-color`,
  `appearance.variant` (7 hazır tasarım).
- Tasarımlar dilim sayısına göre **64x64 PNG** dosyaları: `wheel_2.png` … `wheel_12.png`.
- Ödül adları dilimlerin altında; görünme modu `ALWAYS` / `LOOK` / `ITEM`, ayrıca "name gap" ayarı.
- Paket eklentinin kendi HTTP sunucusuyla dağıtılıyor (varsayılan port 8765).

---

## 4. Derleme ve yayınlama

```powershell
# derleme (JDK 26 ile, Java 21 hedefli; Paper 1.21+ uyumluluğu için 1.21 API'ye karşı)
$env:JAVA_HOME="C:\Program Files\Java\jdk-26.0.2"
.\gradlew.bat build

# kaynak paketi (once resimler, sonra zip)
python pack\art\generate_ui.py
python pack\generate_pack.py 88 0 run\sunshinewheel-pack.zip
```

Sunucuya yükleme:
- `wheel_update.sh` benzeri akış: jar'ı `/tmp`'ye at → `/home/minecraft/plugins/` içine kopyala →
  `systemctl restart minecraft` (0 oyuncu iken yap; açılış ~3-4 dk sürüyor).
- Web arayüzü: `web/index.html|style.css|app.js` → `/var/www/wheel/` (restart YOK, sadece kopyala).
- nginx: `/etc/nginx/snippets/wheel.conf` + rehber server bloğunda include; `/wheel/` statik,
  `/wheel/api/` → `127.0.0.1:8770`. Yedek: `/etc/nginx/sites-available/default.bak-wheel-*`.

**Paket zinciri (önemli):** Oraxen paketi üretir → optimize edilmiş kopya `rehber.guldal.me`'de durur →
Velocity `velocityresourcepacks/config.yml` içindeki url ve hash ile oyunculara gönderir.
Yeni bir şey eklenince **Oraxen kaynak klasörüne de** kopyalanmalı, yoksa paket bir dahaki
üretilişinde kaybolur. (Paketteki `sunshinewheel:ui2` artık kullanılmıyor; ilk paket yeniden
üretiminde temizlenebilir.)

**Geri alma:** Eski paket dosyaları silinmiyor. Velocity config'indeki hash'i eskisine çevirip
`systemctl restart velocity` demek yeterli.

**Yedekler:** `/home/minecraft/backups/sunshinewheel_*` (Velocity config + önceki jar + Oraxen listesi).

---

## 5. Yapılacaklar

Plan/karar dosyası: `C:\CLLL\.opencode\plans\sunshinewheel-editor-hud.md` (HUD dönemi; geçmiş).

1. ~~Çarkın çevresine ödül adları ve yüzdeler~~ — oyuncu önizlemesiyle (eğil+sağ tık, `/wheel preview`)
   kapsandı; çark yüzüne yazı basılmadı (karışık görünürdü).
2. ~~Görünüm renkleri~~ — çerçeve/merkez/ok rengi yapıldı. Kalan: 7 hazır PNG tema
   (Sam's `wheel_2.png`…`wheel_12.png` tarzı); istenirse yapılır.
3. Örnek ödüller hâlâ **test modunda** — sadece mesaj gönderiyorlar, gerçek ödül vermiyorlar.
   Web editörden komutlar + `giveItem` ile gerçek ödüllere çevrilmeli.
4. ~~Para kontrolü~~ — Vault bakiyeli para + XP maliyeti yapıldı.
5. Web v2 fikirleri: ödül satırlarını sürükle-bırak sıralama, eşya ikonlarının gerçek görselleri,
   işlem geçmişi (kim ne zaman kaydetti).
6. Paketten `sunshinewheel:ui2` font/dokularının temizlenmesi (in-game editörle birlikte öksüz kaldı).

---

## 6. Güvenlik notu

Sunucunun root şifresi bu proje sırasında sohbette paylaşıldı. **Değiştirilmeli** (`passwd`) ve
tercihen SSH anahtarına geçilmeli. Proje dosyalarında şifre yok; bağlantı scriptleri şifreyi
ortam değişkeninden okuyordu.
