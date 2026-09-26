#!/usr/bin/env bash
# ==============================================================================
# artnet2dmx - Java 21 / JavaFX Launcher Script
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 1. Java 21+ prüfen
if ! command -v java &> /dev/null; then
    echo "[!] FEHLER: java wurde nicht gefunden."
    echo "    Bitte installieren Sie OpenJDK 21:"
    echo "    Debian/Ubuntu: sudo apt update && sudo apt install -y openjdk-21-jre"
    echo "    Arch Linux:    sudo pacman -S jre21-openjdk"
    echo "    Fedora:        sudo dnf install java-21-openjdk"
    exit 1
fi

JAR_FILE="$SCRIPT_DIR/artnet2dmx-1.0.0-all.jar"
if [ ! -f "$JAR_FILE" ]; then
    # Fallback Suche nach beliebiger all.jar im Verzeichnis oder target/
    JAR_FILE=$(ls "$SCRIPT_DIR"/artnet2dmx*all.jar "$SCRIPT_DIR"/target/artnet2dmx*all.jar 2>/dev/null | head -n 1)
fi

if [ ! -f "$JAR_FILE" ]; then
    echo "[!] FEHLER: artnet2dmx Fat JAR nicht gefunden."
    echo "    Bitte bauen Sie das Projekt mit: mvn clean package"
    exit 1
fi

# 2. Linux-Berechtigungen für serielle Schnittstellen prüfen
if [ "$(id -u)" -ne 0 ]; then
    USER_GROUPS=$(id -Gn)
    if ! echo "$USER_GROUPS" | grep -E -q '\b(dialout|tty|uucp)\b'; then
        echo "------------------------------------------------------------------"
        echo "[HINWEIS] Möglicherweise fehlen Ihnen die Rechte für /dev/ttyUSB0."
        echo "          Ihr Benutzer '$(whoami)' ist nicht in der Gruppe 'dialout'."
        echo "          Falls der Adapter nicht geöffnet werden kann, führen Sie aus:"
        echo "          sudo usermod -a -G dialout $(whoami)"
        echo "------------------------------------------------------------------"
    fi
fi

# 3. Starten der JavaFX Anwendung
exec java -jar "$JAR_FILE" "$@"
