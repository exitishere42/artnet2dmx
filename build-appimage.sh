#!/usr/bin/env bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="/tmp/artnet2dmx-appimage"
JAR_SRC="$PROJECT_DIR/target/artnet2dmx-1.0.0-all.jar"

if [ ! -f "$JAR_SRC" ]; then
    echo "[!] Fat JAR nicht gefunden unter $JAR_SRC. Bitte zuerst 'mvn package' ausführen."
    exit 1
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/AppDir/usr/lib/artnet2dmx"
mkdir -p "$BUILD_DIR/AppDir/usr/share/icons/hicolor/256x256/apps"
mkdir -p "$BUILD_DIR/AppDir/usr/share/applications"

cp "$JAR_SRC" "$BUILD_DIR/AppDir/usr/lib/artnet2dmx/artnet2dmx-1.0.0-all.jar"
cp "$PROJECT_DIR/artnet2dmx.png" "$BUILD_DIR/AppDir/artnet2dmx.png"
cp "$PROJECT_DIR/artnet2dmx.png" "$BUILD_DIR/AppDir/.DirIcon"
cp "$PROJECT_DIR/artnet2dmx.png" "$BUILD_DIR/AppDir/usr/share/icons/hicolor/256x256/apps/artnet2dmx.png"
cp "$PROJECT_DIR/artnet2dmx.svg" "$BUILD_DIR/AppDir/artnet2dmx.svg"

cat > "$BUILD_DIR/AppDir/artnet2dmx.desktop" << 'EOF'
[Desktop Entry]
Type=Application
Name=artnet2dmx
Comment=artnet2dmx - Art-Net to DMX512 Bridge
Exec=AppRun
Icon=artnet2dmx
Categories=AudioVideo;Utility;
Terminal=false
StartupNotify=true
EOF
cp "$BUILD_DIR/AppDir/artnet2dmx.desktop" "$BUILD_DIR/AppDir/usr/share/applications/artnet2dmx.desktop"

cat > "$BUILD_DIR/AppDir/AppRun" << 'EOF'
#!/usr/bin/env bash
HERE="$(dirname "$(readlink -f "${0}")")"
JAR_FILE="$HERE/usr/lib/artnet2dmx/artnet2dmx-1.0.0-all.jar"
if ! command -v java &> /dev/null; then
    echo "[!] Fehler: Java 21+ (java) wurde nicht gefunden. Bitte openjdk-21-jre installieren." >&2
    exit 1
fi
exec java -jar "$JAR_FILE" "$@"
EOF
chmod +x "$BUILD_DIR/AppDir/AppRun"

curl -fsSL -o "$BUILD_DIR/runtime-x86_64" "https://github.com/AppImage/type2-runtime/releases/download/continuous/runtime-x86_64"
mksquashfs "$BUILD_DIR/AppDir" "$BUILD_DIR/app.squashfs" -root-owned -noappend -comp gzip > /dev/null
cat "$BUILD_DIR/runtime-x86_64" "$BUILD_DIR/app.squashfs" > "$PROJECT_DIR/artnet2dmx-x86_64.AppImage"
chmod +x "$PROJECT_DIR/artnet2dmx-x86_64.AppImage"
echo "[OK] AppImage erfolgreich erstellt: $PROJECT_DIR/artnet2dmx-x86_64.AppImage"
