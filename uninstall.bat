@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul 2>&1

set "INSTALL_DIR=%USERPROFILE%\artnet2dmx"

echo ==================================================================
echo   artnet2dmx - Windows Uninstaller
echo ==================================================================

:: 1. Startmenue- und Desktop-Verknuepfungen loeschen
echo [*] Entferne Startmenue- und Desktop-Verknuepfungen...
powershell -NoProfile -Command ^
    "$smPath = Join-Path $env:APPDATA 'Microsoft\Windows\Start Menu\Programs\artnet2dmx.lnk'; " ^
    "$dtPath = Join-Path ([Environment]::GetFolderPath('Desktop')) 'artnet2dmx.lnk'; " ^
    "if (Test-Path $smPath) { Remove-Item -Path $smPath -Force }; " ^
    "if (Test-Path $dtPath) { Remove-Item -Path $dtPath -Force }"

:: 2. Programmordner loeschen (auch falls das Skript aus dem Ordner selbst gestartet wurde)
if exist "%INSTALL_DIR%" (
    echo [*] Loesche Programmordner: %INSTALL_DIR%
    cd /d "%USERPROFILE%"
    rmdir /s /q "%INSTALL_DIR%" 2>nul
) else (
    echo [i] Programmordner %INSTALL_DIR% war bereits geloescht.
)

echo ==================================================================
echo   [OK] artnet2dmx wurde vollstaendig deinstalliert!
echo ==================================================================
pause
