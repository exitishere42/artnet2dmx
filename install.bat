@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul 2>&1

set "REPO=exitishere42/artnet2dmx"
set "INSTALL_DIR=%USERPROFILE%\artnet2dmx"
set "JAR_NAME=artnet2dmx-1.0.4-all.jar"
set "RELEASE_URL=https://github.com/%REPO%/releases/latest/download"
set "RAW_URL=https://raw.githubusercontent.com/%REPO%/main"

echo ==================================================================
echo   artnet2dmx - Windows Installer
echo   Zielordner: %INSTALL_DIR%
echo ==================================================================

:: ------------------------------------------------------------------------------
:: 1. Pruefen ob Java 21+ installiert ist (wenn ja -> skip, wenn nein -> installieren)
:: ------------------------------------------------------------------------------
set "JAVA_OK=0"
where java >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=*" %%v in ('powershell -NoProfile -Command "$v = (java -version 2>&1 | Select-Object -First 1); if ($v -match '\"(\d+)') { [int]$Matches[1] } else { 0 }"') do (
        if %%v GEQ 21 set "JAVA_OK=1"
    )
)

if "%JAVA_OK%"=="1" (
    echo [OK] Java 21+ ist bereits installiert - ueberspringe Java-Installation.
) else (
    echo [*] Java 21+ nicht gefunden. Installiere neuestes OpenJDK 21 LTS...
    where winget >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        winget install --id EclipseAdoptium.Temurin.21.JRE -e --accept-source-agreements --accept-package-agreements
    ) else (
        echo [*] Lade OpenJDK 21 MSI direkt von Adoptium herunter...
        powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/installer/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse' -OutFile '$env:TEMP\temurin21.msi'; Start-Process msiexec.exe -ArgumentList '/i', '$env:TEMP\temurin21.msi', '/passive', '/norestart' -Wait"
    )
    echo [OK] Java 21+ Installation abgeschlossen.
)

:: ------------------------------------------------------------------------------
:: 2. Programmdateien nach %USERPROFILE%\artnet2dmx herunterladen
:: ------------------------------------------------------------------------------
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"

echo [*] Lade artnet2dmx und Logo herunter...
powershell -NoProfile -Command ^
    "$ProgressPreference = 'SilentlyContinue'; " ^
    "Invoke-WebRequest -Uri '%RELEASE_URL%/%JAR_NAME%' -OutFile '%INSTALL_DIR%\%JAR_NAME%'; " ^
    "Invoke-WebRequest -Uri '%RAW_URL%/artnet2dmx.ico' -OutFile '%INSTALL_DIR%\artnet2dmx.ico'; " ^
    "Invoke-WebRequest -Uri '%RAW_URL%/artnet2dmx.png' -OutFile '%INSTALL_DIR%\artnet2dmx.png'; " ^
    "Invoke-WebRequest -Uri '%RAW_URL%/uninstall.bat' -OutFile '%INSTALL_DIR%\uninstall.bat'"

if not exist "%INSTALL_DIR%\%JAR_NAME%" (
    echo [!] Fehler beim Herunterladen von %JAR_NAME%.
    pause
    exit /b 1
)

:: Startskript im Zielordner anlegen
(
echo @echo off
echo cd /d "%%~dp0"
echo for /f "delims=" %%%%f in ^('dir /b /o:-d "%%~dp0artnet2dmx-*-all.jar" "%%~dp0artnet2dmx.jar" 2^^^>nul'^) do ^(
echo     start "" javaw -jar "%%~dp0%%%%f" %%*
echo     exit /b 0
echo ^)
echo start "" javaw -jar "%%~dp0%JAR_NAME%" %%*
) > "%INSTALL_DIR%\run.bat"

:: ------------------------------------------------------------------------------
:: 3. Startmenue- und Desktop-Verknuepfung mit Logo erstellen
:: ------------------------------------------------------------------------------
echo [*] Erstelle Startmenue- und Desktop-Eintrag mit Logo...
powershell -NoProfile -Command ^
    "$ws = New-Object -ComObject WScript.Shell; " ^
    "$javaw = (Get-Command javaw -ErrorAction SilentlyContinue).Source; " ^
    "if (-not $javaw) { $javaw = 'javaw.exe' }; " ^
    "$smPath = Join-Path $env:APPDATA 'Microsoft\Windows\Start Menu\Programs\artnet2dmx.lnk'; " ^
    "$dtPath = Join-Path ([Environment]::GetFolderPath('Desktop')) 'artnet2dmx.lnk'; " ^
    "foreach ($lnkPath in @($smPath, $dtPath)) { " ^
    "  $s = $ws.CreateShortcut($lnkPath); " ^
    "  $s.TargetPath = $javaw; " ^
    "  $s.Arguments = '-jar \"' + '%INSTALL_DIR%\%JAR_NAME%' + '\"'; " ^
    "  $s.WorkingDirectory = '%INSTALL_DIR%'; " ^
    "  $s.IconLocation = '%INSTALL_DIR%\artnet2dmx.ico,0'; " ^
    "  $s.Description = 'artnet2dmx - Art-Net 4 to USB-DMX512 Hardware Bridge'; " ^
    "  $s.Save(); " ^
    "}"

echo ==================================================================
echo   [OK] Installation erfolgreich abgeschlossen!
echo   Ordner:      %INSTALL_DIR%
echo   Startmenue:  artnet2dmx (mit Logo direkt im Startmenue & Desktop)
echo ==================================================================
pause
