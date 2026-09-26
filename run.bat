@echo off
setlocal
cd /d "%~dp0"

echo ==================================================================
echo   Starte artnet2dmx (JavaFX 21 LTS)
echo ==================================================================

if exist "target\artnet2dmx-1.0.0-all.jar" (
    java -jar target\artnet2dmx-1.0.0-all.jar %*
) else (
    echo Fat JAR nicht gefunden. Starte via Maven exec:java...
    call mvn exec:java -Dexec.args="%*"
)
