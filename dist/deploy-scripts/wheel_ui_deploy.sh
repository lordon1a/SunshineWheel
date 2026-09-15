set -euo pipefail
VCFG=/home/velocity/plugins/velocityresourcepacks/config.yml
NEWPACK=/tmp/sunshinewheel-pack.zip
JAR=/tmp/SunshineWheel-0.1.0.jar
BACKUP=/home/minecraft/backups/sunshinewheel_ui_$(date +%Y%m%d_%H%M%S)

echo "=== bagli oyuncu: $(ss -tn state established '( sport = :25565 )' | tail -n +2 | wc -l) ==="
[ -s "$NEWPACK" ] || { echo "paket yuklenmemis"; exit 1; }
[ -s "$JAR" ] || { echo "jar yuklenmemis"; exit 1; }

# Mevcut paketi Velocity ayarindan oku (hash gomulu degil)
OLD=$(grep -oE '[0-9a-f]{40}' "$VCFG" | head -1)
SRC=/var/www/rehber/pack-$OLD.zip
echo "temel paket: $SRC"
[ -f "$SRC" ] || { echo "temel paket bulunamadi"; exit 1; }

mkdir -p "$BACKUP"
cp -a "$VCFG" "$BACKUP/velocityresourcepacks-config.yml"
echo "$OLD" > "$BACKUP/onceki-pack-hash.txt"
cp -a /home/minecraft/plugins/SunshineWheel-0.1.0.jar "$BACKUP/SunshineWheel-onceki.jar" 2>/dev/null || true
echo "yedek: $BACKUP"

echo; echo "=== paket birlestirme (eski sunshinewheel dosyalari degistirilir) ==="
python3 - "$SRC" "$NEWPACK" <<'EOF'
import sys, zipfile
src, add, out = sys.argv[1], sys.argv[2], "/tmp/pack-merged.zip"
updates = {}
with zipfile.ZipFile(add) as a:
    for name in a.namelist():
        if name.startswith("assets/sunshinewheel/"):
            updates[name] = a.read(name)
replaced = 0
with zipfile.ZipFile(src) as s, zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as o:
    for item in s.infolist():
        if item.filename in updates:
            replaced += 1
            continue
        o.writestr(item, s.read(item.filename))
    for name, data in updates.items():
        o.writestr(name, data)
print(f"{len(updates)} dosya yazildi ({replaced} tanesi guncelleme)")
with zipfile.ZipFile(out) as z:
    bad = z.testzip()
    if bad:
        raise SystemExit("bozuk zip: " + bad)
print("zip butunluk OK")
EOF

NEW=$(sha1sum /tmp/pack-merged.zip | cut -d' ' -f1)
echo "yeni sha1: $NEW"
install -m 644 /tmp/pack-merged.zip "/var/www/rehber/pack-$NEW.zip"
curl -s -o /tmp/pack-check.zip -w "https: %{http_code}\n" "https://rehber.guldal.me/pack-$NEW.zip"
GOT=$(sha1sum /tmp/pack-check.zip | cut -d' ' -f1)
[ "$GOT" = "$NEW" ] || { echo "INDIRILEN PAKET ESLESMIYOR ($GOT)"; exit 1; }
echo "indirilen sha1 eslesiyor"
unlink /tmp/pack-check.zip
unlink /tmp/pack-merged.zip

echo; echo "=== Oraxen kaynak klasoru ==="
python3 -c "import zipfile; zipfile.ZipFile('$NEWPACK').extractall('/tmp/swpack')"
cp -r /tmp/swpack/assets/sunshinewheel /home/minecraft/plugins/Oraxen/pack/assets/
find /home/minecraft/plugins/Oraxen/pack/assets/sunshinewheel -type f | sed 's|.*/assets|assets|'
rm -rf /tmp/swpack

echo; echo "=== velocity ayari ==="
sed -i "s/$OLD/$NEW/g" "$VCFG"
grep -nE 'url:|hash:' "$VCFG" | head -2

echo; echo "=== plugin ==="
install -m 644 "$JAR" /home/minecraft/plugins/SunshineWheel-0.1.0.jar
sha1sum /home/minecraft/plugins/SunshineWheel-0.1.0.jar

echo; echo "=== yeniden baslatma ==="
systemctl restart minecraft
systemctl restart velocity
STARTED=$(date -d "$(systemctl show minecraft -p ActiveEnterTimestamp --value)" +%H:%M:%S)
for i in $(seq 1 120); do
  if awk -v s="$STARTED" -F'[][]' '/Done \(/ { if ($2 >= s) found=1 } END { exit !found }' /home/minecraft/logs/latest.log 2>/dev/null; then break; fi
  sleep 3
done
grep -E 'Enabling SunshineWheel|Done \(' /home/minecraft/logs/latest.log | tail -3
echo "--- wheel kaynakli hata ---"
grep -nE 'ERROR|Exception' /home/minecraft/logs/latest.log | grep -iE 'wheel|me\.guldal' | head -5 || true
systemctl is-active minecraft velocity

echo
echo "GERI ALMA: sed -i 's/$NEW/$OLD/g' $VCFG && systemctl restart velocity"
