set -euo pipefail
JAR=/tmp/SunshineWheel-0.1.0.jar
DEST=/home/minecraft/plugins/SunshineWheel-0.1.0.jar

echo "=== bagli oyuncu: $(ss -tn state established '( sport = :25565 )' | tail -n +2 | wc -l) ==="
[ -s "$JAR" ] || { echo "yuklenen jar yok"; exit 1; }
cp -a "$DEST" "/home/minecraft/backups/sunshinewheel_20260915_080822/SunshineWheel-0.1.0-onceki.jar"
install -m 644 "$JAR" "$DEST"
unlink "$JAR"
sha1sum "$DEST"

systemctl restart minecraft
# Eski latest.log kapanırken de güncellenir ve önceki "Done (" satırını içerir; bu yüzden
# servisin yeni başlama saatinden sonraki "Done (" satırını bekle.
STARTED=$(date -d "$(systemctl show minecraft -p ActiveEnterTimestamp --value)" +%H:%M:%S)
for i in $(seq 1 120); do
  if awk -v s="$STARTED" -F'[][]' '/Done \(/ { if ($2 >= s) found=1 } END { exit !found }' /home/minecraft/logs/latest.log 2>/dev/null; then break; fi
  sleep 3
done
grep -E 'Enabling SunshineWheel|Done \(' /home/minecraft/logs/latest.log | tail -3
echo "--- wheel kaynakli hata ---"
grep -nE 'ERROR|Exception' /home/minecraft/logs/latest.log | grep -iE 'wheel|me\.guldal' | head -5 || true
systemctl is-active minecraft velocity
