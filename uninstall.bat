@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul 2>&1

set "INSTALL_DIR=%USERPROFILE%\artnet2dmx"
set "SM_LNK=%APPDATA%\Microsoft\Windows\Start Menu\Programs\artnet2dmx.lnk"

echo ==================================================================
echo   artnet2dmx - Windows Uninstaller
echo ==================================================================

:: 1. Startmenue- und Desktop-Verknuepfungen loeschen
if exist "%SM_LNK%" (
    echo [*] Entferne Startmenue-Eintrag...
    del /f /q "%SM_LNK%"
)

for /f "usebackq tokens=*" %%d in (`powershell -NoProfile -Command "[Environment]::GetFolderPath('Desktop')"`) do (
    if exist "%%d\artnet2dmx.lnk" (
        echo [*] Entferne Desktop-Verknuepfung...
        del /f /q "%%d\artnet2dmx.lnk"
    )
)

:: 2. Programmordner %USERPROFILE%\artnet2dmx loeschen
if exist "%INSTALL_DIR%" (
    echo [*] Loesche Programmordner: %INSTALL_DIR%
    :: Falls das Skript direkt aus %INSTALL_DIR% gestartet wurde, kurz nach %TEMP% wechseln
    cd /d "%TEMP%"
    rmdir /s /q "%INSTALL_DIR%"
) else (
    echo [i] Programmordner %INSTALL_DIR% wurde bereits entfernt.
)

echo ==================================================================
echo   [OK] artnet2dmx wurde vollstaendig deinstalliert!
echo ==================================================================
pause
