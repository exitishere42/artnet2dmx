#!/usr/bin/env bash
# ==============================================================================
# artnet2dmx - Automatischer Linux Uninstaller
# Entfernt den Programmordner (~/artnet2dmx), den Desktop-Eintrag im App-Dash
# sowie die installierten Icons rückstandslos.
# ==============================================================================

set -e

INSTALL_DIR="${INSTALL_DIR:-$HOME/artnet2dmx}"
ICON_PNG="$HOME/.local/share/icons/hicolor/256x256/apps/artnet2dmx.png"
ICON_SVG="$HOME/.local/share/icons/hicolor/scalable/apps/artnet2dmx.svg"
DESKTOP_APP="$HOME/.local/share/applications/artnet2dmx.desktop"
DESKTOP_SHORTCUT="$HOME/Desktop/artnet2dmx.desktop"
DESKTOP_SHORTCUT_DE="$HOME/Schreibtisch/artnet2dmx.desktop"

echo "=================================================================="
echo "  artnet2dmx - Linux Uninstaller"
echo "=================================================================="

# 1. Laufende Instanzen beenden (falls offen)
if pgrep -f "artnet2dmx-1.0.0-all.jar" &> /dev/null || pgrep -f "artnet2dmx-x86_64.AppImage" &> /dev/null; then
    echo "[*] Beende laufende artnet2dmx Instanz..."
    pkill -f "artnet2dmx-1.0.0-all.jar" 2>/dev/null || true
    pkill -f "artnet2dmx-x86_64.AppImage" 2>/dev/null || true
fi

# 2. Desktop-Entries und Icons entfernen
echo "[*] Entferne Desktop-Entry und App-Icons..."
rm -f "$DESKTOP_APP" "$DESKTOP_SHORTCUT" "$DESKTOP_SHORTCUT_DE"
rm -f "$ICON_PNG" "$ICON_SVG"

if command -v update-desktop-database &> /dev/null; then
    update-desktop-database "$HOME/.local/share/applications" &> /dev/null || true
fi
if command -v gtk-update-icon-cache &> /dev/null; then
    gtk-update-icon-cache -f -t "$HOME/.local/share/icons/hicolor" &> /dev/null || true
fi

# 3. Programmordner entfernen
if [ -d "$INSTALL_DIR" ]; then
    echo "[*] Lösche Programmordner: $INSTALL_DIR"
    rm -rf "$INSTALL_DIR"
else
    echo "[i] Programmordner $INSTALL_DIR war bereits gelöscht."
fi

echo "=================================================================="
echo "  [OK] artnet2dmx wurde vollständig deinstalliert!"
echo "=================================================================="
