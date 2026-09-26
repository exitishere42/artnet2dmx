#!/usr/bin/env bash
# ==============================================================================
# artnet2dmx - Linux Deinstallations-Skript (Uninstaller)
# Entfernt den Programmordner (~/artnet2dmx), das App-Logo und den Desktop-Entry.
# ==============================================================================

set -e

INSTALL_DIR="${INSTALL_DIR:-$HOME/artnet2dmx}"
DESKTOP_FILE="$HOME/.local/share/applications/artnet2dmx.desktop"
ICON_PNG="$HOME/.local/share/icons/hicolor/256x256/apps/artnet2dmx.png"
ICON_SVG="$HOME/.local/share/icons/hicolor/scalable/apps/artnet2dmx.svg"

echo "=================================================================="
echo "  artnet2dmx - Linux Uninstaller"
echo "=================================================================="

# 1. Laufende Instanzen beenden (falls aktiv)
if pgrep -f "artnet2dmx-1.0.0-all.jar|artnet2dmx-x86_64.AppImage" &> /dev/null; then
    echo "[*] Beende laufende artnet2dmx-Instanz..."
    pkill -f "artnet2dmx-1.0.0-all.jar|artnet2dmx-x86_64.AppImage" || true
    sleep 1
fi

# 2. Desktop-Entry und Icons entfernen
if [ -f "$DESKTOP_FILE" ]; then
    echo "[*] Entferne Desktop-Entry: $DESKTOP_FILE"
    rm -f "$DESKTOP_FILE"
fi

rm -f "$HOME/Desktop/artnet2dmx.desktop" "$HOME/Schreibtisch/artnet2dmx.desktop" 2>/dev/null || true

if [ -f "$ICON_PNG" ] || [ -f "$ICON_SVG" ]; then
    echo "[*] Entferne App-Icons..."
    rm -f "$ICON_PNG" "$ICON_SVG"
fi

if command -v update-desktop-database &> /dev/null; then
    update-desktop-database "$HOME/.local/share/applications" &> /dev/null || true
fi
if command -v gtk-update-icon-cache &> /dev/null; then
    gtk-update-icon-cache -f -t "$HOME/.local/share/icons/hicolor" &> /dev/null || true
fi

# 3. Programmordner löschen
if [ -d "$INSTALL_DIR" ]; then
    echo "[*] Lösche Programmordner: $INSTALL_DIR"
    rm -rf "$INSTALL_DIR"
else
    echo "[i] Programmordner $INSTALL_DIR existiert bereits nicht mehr."
fi

echo "=================================================================="
echo "  [OK] artnet2dmx wurde vollständig deinstalliert!"
echo "=================================================================="
