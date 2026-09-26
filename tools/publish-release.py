#!/usr/bin/env python3
"""
Automatisches Release-Skript fuer artnet2dmx.
Verwendung:
    python tools/publish-release.py patch   # z. B. 1.0.1 -> 1.0.2 (Bugfixes / kleine Aenderungen)
    python tools/publish-release.py minor   # z. B. 1.0.1 -> 1.1.0 (Grosse Updates / Features)
    python tools/publish-release.py 1.0.2   # Explizite Versionsnummer
"""

import sys
import os
import re
import subprocess
import urllib.request
import urllib.error
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.dirname(SCRIPT_DIR)
POM_PATH = os.path.join(PROJECT_DIR, "pom.xml")
UPDATE_SERVICE_PATH = os.path.join(PROJECT_DIR, "src", "main", "java", "de", "artnet2dmx", "service", "UpdateService.java")
INSTALL_SH_PATH = os.path.join(PROJECT_DIR, "install.sh")
INSTALL_BAT_PATH = os.path.join(PROJECT_DIR, "install.bat")

REPO = "exitishere42/artnet2dmx"

def get_current_version():
    with open(POM_PATH, "r", encoding="utf-8") as f:
        content = f.read()
    m = re.search(r"<groupId>de\.artnet2dmx</groupId>\s*<artifactId>artnet2dmx</artifactId>\s*<version>([^<]+)</version>", content)
    if not m:
        raise ValueError("Konnte aktuelle Version in pom.xml nicht finden.")
    return m.group(1).strip()

def calculate_new_version(current, mode):
    parts = [int(p) for p in current.split(".")]
    while len(parts) < 3:
        parts.append(0)
    
    if mode == "patch":
        parts[2] += 1
    elif mode == "minor":
        parts[1] += 1
        parts[2] = 0
    elif mode == "major":
        parts[0] += 1
        parts[1] = 0
        parts[2] = 0
    else:
        # Explizite Version angegeben
        clean = mode.lstrip("v")
        if re.match(r"^\d+\.\d+\.\d+$", clean):
            return clean
        raise ValueError(f"Unbekannter Modus oder ungueltige Version: {mode}")
    
    return f"{parts[0]}.{parts[1]}.{parts[2]}"

def update_file(path, pattern, replacement):
    if not os.path.exists(path):
        return
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    new_content = re.sub(pattern, replacement, content)
    with open(path, "w", encoding="utf-8") as f:
        f.write(new_content)

def get_github_token():
    p = subprocess.run(['git', 'credential', 'fill'], input=b'protocol=https\nhost=github.com\n\n', capture_output=True)
    creds = dict(line.split('=', 1) for line in p.stdout.decode().splitlines() if '=' in line)
    token = creds.get('password')
    if not token:
        raise RuntimeError("Kein GitHub Token via 'git credential fill' verfuegbar.")
    return token

def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    mode = sys.argv[1].lower()
    cur_ver = get_current_version()
    new_ver = calculate_new_version(cur_ver, mode)
    tag = f"v{new_ver}"

    print("==================================================================")
    print(f"  artnet2dmx - Release Automation: {cur_ver} -> {new_ver} ({tag})")
    print("==================================================================")

    # 1. Dateien aktualisieren
    print("[1/6] Aktualisiere Versionsnummern in Projektdateien...")
    update_file(POM_PATH, r"(<artifactId>artnet2dmx</artifactId>\s*<version>)[^<]+(</version>)", rf"\g<1>{new_ver}\g<2>")
    update_file(UPDATE_SERVICE_PATH, r'(CURRENT_VERSION\s*=\s*")[^"]+(")', rf'\g<1>{new_ver}\g<2>')
    update_file(INSTALL_SH_PATH, r'(JAR_NAME="artnet2dmx-)[^"]*(-all\.jar")', rf'\g<1>{new_ver}\g<2>')
    update_file(INSTALL_BAT_PATH, r'(set "JAR_NAME=artnet2dmx-)[^"]*(-all\.jar")', rf'\g<1>{new_ver}\g<2>')

    # 2. Maven Build
    print(f"[2/6] Baue Fat-JAR fuer v{new_ver} (mvn clean package)...")
    subprocess.run(["mvn", "clean", "package", "-DskipTests"], cwd=PROJECT_DIR, check=True, shell=True)

    jar_file = os.path.join(PROJECT_DIR, "target", f"artnet2dmx-{new_ver}-all.jar")
    if not os.path.exists(jar_file):
        raise RuntimeError(f"Erwartete Datei nicht gefunden: {jar_file}")

    # Fallback-Kopien anlegen
    fallback_jar = os.path.join(PROJECT_DIR, "target", "artnet2dmx-1.0.0-all.jar")
    subprocess.run(["powershell", "-Command", f"Copy-Item '{jar_file}' '{fallback_jar}'"], check=True)

    # 3. AppImage erstellen
    print("[3/6] Erstelle AppImage via WSL Debian...")
    try:
        subprocess.run(["wsl", "-d", "Debian", "bash", "-c", f"cd /mnt/{PROJECT_DIR[0].lower()}{PROJECT_DIR[2:].replace(chr(92), '/')} && ./build-appimage.sh"], check=True)
    except Exception as e:
        print(f"[!] Warnung beim AppImage-Bau: {e}")

    # 4. Git Commit, Tag & Push
    print("[4/6] Committe und erstelle Git-Tag...")
    subprocess.run(["git", "add", "."], cwd=PROJECT_DIR, check=True)
    subprocess.run(["git", "commit", "-m", f"Release {tag}"], cwd=PROJECT_DIR, check=True)
    subprocess.run(["git", "tag", tag], cwd=PROJECT_DIR, check=True)
    subprocess.run(["git", "push", "origin", "main", "--tags"], cwd=PROJECT_DIR, check=True)

    # 5. GitHub Release erstellen & Assets hochladen
    print(f"[5/6] Veroeffentliche Release {tag} auf GitHub...")
    token = get_github_token()

    release_body = f"""## artnet2dmx {tag}

### Installation
- **Linux (`install.sh`)**:
  ```bash
  curl -fsSL https://github.com/{REPO}/releases/latest/download/install.sh | bash
  ```
- **Windows (`install.bat`)**:
  Herunterladen und ausfuehren fuer automatische Installation inkl. Startmenue- und Desktop-Verknuepfung.
- **AppImage**:
  `artnet2dmx-x86_64.AppImage` herunterladen, `chmod +x` und direkt starten.
"""

    create_url = f"https://api.github.com/repos/{REPO}/releases"
    data = json.dumps({
        "tag_name": tag,
        "target_commitish": "main",
        "name": f"artnet2dmx {tag}",
        "body": release_body,
        "draft": False,
        "prerelease": False
    }).encode('utf-8')

    req = urllib.request.Request(create_url, data=data, method="POST", headers={
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "Content-Type": "application/json",
        "User-Agent": "artnet2dmx-releaser"
    })

    with urllib.request.urlopen(req) as resp:
        res = json.loads(resp.read().decode())
        upload_url = res["upload_url"].split("{")[0]

    files_to_upload = [
        (jar_file, f"artnet2dmx-{new_ver}-all.jar", "application/java-archive"),
        (fallback_jar, "artnet2dmx-1.0.0-all.jar", "application/java-archive"),
        (os.path.join(PROJECT_DIR, "artnet2dmx-x86_64.AppImage"), "artnet2dmx-x86_64.AppImage", "application/x-executable"),
        (os.path.join(PROJECT_DIR, "install.sh"), "install.sh", "application/x-sh"),
        (os.path.join(PROJECT_DIR, "install.bat"), "install.bat", "application/x-bat"),
        (os.path.join(PROJECT_DIR, "uninstall.sh"), "uninstall.sh", "application/x-sh"),
        (os.path.join(PROJECT_DIR, "uninstall.bat"), "uninstall.bat", "application/x-bat"),
        (os.path.join(PROJECT_DIR, "artnet2dmx.png"), "artnet2dmx.png", "image/png"),
        (os.path.join(PROJECT_DIR, "artnet2dmx.svg"), "artnet2dmx.svg", "image/svg+xml"),
        (os.path.join(PROJECT_DIR, "artnet2dmx.ico"), "artnet2dmx.ico", "image/x-icon"),
        (os.path.join(PROJECT_DIR, "pyftdi_worker.py"), "pyftdi_worker.py", "text/x-python")
    ]

    print("[6/6] Lade Release-Assets hoch...")
    for local_path, asset_name, mime in files_to_upload:
        if not os.path.exists(local_path):
            continue
        print(f"  • {asset_name} ({os.path.getsize(local_path)} Bytes)...")
        with open(local_path, "rb") as f:
            file_bytes = f.read()
        up_req = urllib.request.Request(f"{upload_url}?name={asset_name}", data=file_bytes, method="POST", headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
            "Content-Type": mime,
            "User-Agent": "artnet2dmx-releaser"
        })
        urllib.request.urlopen(up_req).close()

    print("\n==================================================================")
    print(f"  [OK] Release {tag} erfolgreich veroeffentlicht!")
    print(f"  URL: https://github.com/{REPO}/releases/tag/{tag}")
    print("==================================================================")

if __name__ == "__main__":
    main()
