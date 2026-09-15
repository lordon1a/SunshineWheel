"""VPS'e SSH anahtarıyla komut gönderir (şifre kullanmaz).

Kullanım: python vpsk.py "komut"   ya da   komutları stdin'den ver.
         python vpsk.py --put yerel_dosya uzak_yol
         python vpsk.py --get uzak_yol yerel_dosya
"""
import os
import sys

import paramiko

HOST = "31.42.127.208"
KEY = os.path.expanduser("~/.ssh/sunshine_vps_ed25519")


def connect() -> paramiko.SSHClient:
    # Şifre dosyaya yazılmaz; çağıran komut VPS_PW ortam değişkeniyle verir.
    c = paramiko.SSHClient()
    c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    c.connect(HOST, username="root", password=os.environ["VPS_PW"], timeout=20, look_for_keys=False, allow_agent=False)
    return c


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    c = connect()
    try:
        if len(sys.argv) >= 4 and sys.argv[1] in ("--put", "--get"):
            sftp = c.open_sftp()
            if sys.argv[1] == "--put":
                sftp.put(sys.argv[2], sys.argv[3])
            else:
                sftp.get(sys.argv[2], sys.argv[3])
            sftp.close()
            print(f"{sys.argv[1][2:]} tamam: {sys.argv[2]} -> {sys.argv[3]}")
            return
        cmd = (sys.stdin.read() if len(sys.argv) < 2 else sys.argv[1]).lstrip("﻿")
        _, out, err = c.exec_command(cmd, timeout=600)
        code = out.channel.recv_exit_status()
        sys.stdout.write(out.read().decode("utf-8", "replace"))
        e = err.read().decode("utf-8", "replace")
        if e.strip():
            sys.stdout.write("\n[stderr]\n" + e)
        if code:
            sys.stdout.write(f"\n[exit {code}]\n")
    finally:
        c.close()


if __name__ == "__main__":
    main()
