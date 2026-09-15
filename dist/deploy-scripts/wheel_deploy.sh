set -euo pipefail
OLD=50e3d3f497ec8d745570633038f4d35fbabf6e28
VCFG=/home/velocity/plugins/velocityresourcepacks/config.yml

echo "=== 1) oyuncu kontrolu ==="
N=$(ss -tn state established '( sport = :25565 )' | tail -n +2 | wc -l)
echo "bagli: $N"
if [ "$N" -ne 0 ]; then echo "OYUNCU VAR, DURDURULDU"; exit 1; fi

echo; echo "=== 2) paket birlestirme ==="
python3 - <<'EOF'
import hashlib, shutil, zipfile
src = "/var/www/rehber/pack-50e3d3f497ec8d745570633038f4d35fbabf6e28.zip"
add = "/tmp/sunshinewheel-pack.zip"
out = "/tmp/pack-merged.zip"
shutil.copyfile(src, out)
with zipfile.ZipFile(add) as a, zipfile.ZipFile(out, "a", zipfile.ZIP_DEFLATED) as z:
    existing = set(z.namelist())
    for name in a.namelist():
        if name.startswith("assets/sunshinewheel/"):
            if name in existing:
                raise SystemExit("zaten var: " + name)
            z.writestr(name, a.read(name))
            print("eklendi:", name)
with zipfile.ZipFile(out) as z:
    bad = z.testzip()
    if bad:
        raise SystemExit("bozuk zip girdisi: " + bad)
print("zip butunluk OK")
EOF
NEW=$(sha1sum /tmp/pack-merged.zip | cut -d' ' -f1)
echo "yeni sha1: $NEW"
install -m 644 /tmp/pack-merged.zip "/var/www/rehber/pack-$NEW.zip"
curl -s -o /tmp/pack-check.zip -w "https: %{http_code}\n" "https://rehber.guldal.me/pack-$NEW.zip"
GOT=$(sha1sum /tmp/pack-check.zip | cut -d' ' -f1)
if [ "$GOT" != "$NEW" ]; then echo "INDIRILEN PAKET ESLESMIYOR ($GOT), DURDURULDU"; exit 1; fi
echo "indirilen sha1 eslesiyor"
unlink /tmp/pack-check.zip
unlink /tmp/pack-merged.zip

echo; echo "=== 3) Oraxen kaynak klasorune font ==="
python3 -c "import zipfile; zipfile.ZipFile('/tmp/sunshinewheel-pack.zip').extractall('/tmp/swpack')"
cp -r /tmp/swpack/assets/sunshinewheel /home/minecraft/plugins/Oraxen/pack/assets/
find /home/minecraft/plugins/Oraxen/pack/assets/sunshinewheel -type f

echo; echo "=== 4) velocity paket ayari ==="
sed -i "s/$OLD/$NEW/g" "$VCFG"
grep -nE 'url:|hash:' "$VCFG" | head -2

echo; echo "=== 5) plugin ==="
install -m 644 /tmp/SunshineWheel-0.1.0.jar /home/minecraft/plugins/SunshineWheel-0.1.0.jar
ls -la /home/minecraft/plugins/SunshineWheel-0.1.0.jar

echo; echo "=== 6) yeniden baslatma ==="
START=$(date +%s)
systemctl restart minecraft
systemctl restart velocity
for i in $(seq 1 100); do
  if [ -f /home/minecraft/logs/latest.log ] && [ "$(stat -c %Y /home/minecraft/logs/latest.log)" -ge "$START" ] && grep -q 'Done (' /home/minecraft/logs/latest.log; then break; fi
  sleep 3
done
grep -E 'SunshineWheel|Done \(' /home/minecraft/logs/latest.log | tail -6
systemctl is-active minecraft velocity
grep -iE 'sunshinepack|error' /home/velocity/logs/latest.log | tail -4 || true

echo
echo "GERI ALMA: sed -i 's/$NEW/$OLD/g' $VCFG ; plugins/SunshineWheel-0.1.0.jar dosyasini kaldir ; systemctl restart velocity minecraft"
