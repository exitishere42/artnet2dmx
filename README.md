<div align="center">

<img src="artnet2dmx.png" alt="artnet2dmx Logo" width="128" height="128" />

```text
               _              _   ____      _               
    __ _ _ __| |_ _ __   ___| |_|___ \  __| |_ __ _____  __
   / _` | '__| __| '_ \ / _ \ __| __) |/ _` | '_ ` _ \ \/ /
  | (_| | |  | |_| | | |  __/ |_ / __/| (_| | | | | | >  < 
   \__,_|_|   \__|_| |_|\___|\__|_____|\__,_|_| |_| |_/_/\_\
                                                            
         [ ART-NET 4 TO USB-DMX512 HARDWARE BRIDGE ]
```

**High-Performance Real-Time Art-Net 4 to USB-DMX512 Hardware Bridge & Live Visualizer built with Java 21 LTS & JavaFX 21**

</div>

---

## About the Project

**artnet2dmx** receives live Art-Net 4 (`ArtDMX`) network packets over UDP and translates them in real time into hardware-timed DMX512 signals for USB-to-DMX interfaces (such as FTDI FT232R OpenDMX adapters, Enttec DMX USB Pro, or virtual serial ports).

Visually and structurally, **artnet2dmx** features a clean Material Design 2 Dark Theme interface with live telemetry charts and a 512-channel DMX visualizer, making it the ideal companion to [**sound2artnet**](https://github.com/exitishere42/sound2artnet), QLC+, SoundSwitch, or GrandMA.

```text
+--------------------+      +-------------------------+      +--------------------------+
|   ART-NET 4 INPUT  |      |   DMX512 BRIDGE CORE    |      |    USB-DMX HARDWARE      |
|                    |      |                         |      |                          |
|  * UDP Port 6454   | ===> |  * 15-Bit Port-Address  | ===> |  * jna-ftdi (libusb-1.0) |
|  * Unicast / Bcast |      |  * Continuous Buffer    |      |  * pyftdi / serial       |
|  * sound2artnet    |      |  * 10-44 Hz Frame Gen   |      |  * enttec-pro / dummy    |
|  * QLC+ / Lighting |      |  * Zero-Blackout Hold   |      |  * 250 kBaud / 8N2       |
+--------------------+      +-------------------------+      +------------+-------------+
                                                                          |
                                                                          | DMX512 (3/5-Pin XLR)
                                                                          v
                            +-----------------------------------------------------------+
                            |                 DMX512 LIGHTING FIXTURES                  |
                            |                                                           |
                            |  [ Moving Heads ]   [ RGBW PARs ]   [ Strobes & Dimmers ] |
                            +-----------------------------------------------------------+
```

---

## Features

### 1. Native Pure-Java USB-to-DMX Hardware Control (`jusb2dmx`)
- **Direct `libusb-1.0` & JNA Driver (`jna-ftdi`)**: Communicates directly with FTDI FT232R-based OpenDMX adapters (e.g. JMS USB2DMX PRO, Enttec OpenDMX) via native USB control transfers (`SIO_SET_BAUD_RATE`, `SIO_SET_DATA` with `BREAK_ON`/`BREAK_OFF`) — **100% pure Java without Python subprocesses or virtual COM port latency**.
- **Continuous Frame Buffer**: Decouples incoming network packet rates from the hardware DMX output loop, guaranteeing rock-solid refresh rates (`10–44 Hz`) and preventing fixture flickering or blackouts.
- **Multi-Backend Support**: Choose between `jna-ftdi`, `pyftdi`, `serial` (`jSerialComm`), `enttec-pro`, and `dummy` (simulation mode).

```text
+---------------+------------------------+-------------------------+--------------------+
|  DRIVER       |  BACKEND TECHNOLOGY    |  HARDWARE TARGET        |  LATENCY / TIMING  |
+---------------+------------------------+-------------------------+--------------------+
|  jna-ftdi     |  JNA + libusb-1.0      |  FTDI FT232R / OpenDMX  |  Ultra-Low (Native)|
|  pyftdi       |  Python + pyftdi       |  FTDI USB Adapters      |  Low (Subprocess)  |
|  serial       |  jSerialComm (UART)    |  COM / ttyUSB0 OpenDMX  |  Medium (OS UART)  |
|  enttec-pro   |  jSerialComm (Packet)  |  Enttec DMX USB Pro     |  Hardware-Timed    |
|  dummy        |  In-Memory Simulation  |  No Hardware Required   |  Instant (Testing) |
+---------------+------------------------+-------------------------+--------------------+
```

### 2. Art-Net 4 Network Receiver
- **Standard-Compliant `ArtDMX` (`0x5000`) Parser**: Listens on UDP port `6454` (`0.0.0.0`) for incoming Art-Net packets.
- **Flexible Universe Filtering**: Filter for a specific Art-Net universe (`0–15`) or set to `-1` (Omni mode) to process all incoming universes.
- **Auto-Detection & Port Scanner**: Automatically detects connected FTDI and serial DMX interfaces at startup or via the integrated one-click `Scan` button.

### 3. Live Telemetry & 512-Channel DMX Visualizer
- **Dual Real-Time History Charts**: Live area line charts for **Art-Net Input (`pkt/s`)** in Material Teal (`#03DAC6`) and **DMX512 Output (`fps`)** in Signal Green (`#00E676`), plus total packet counters and active driver status.
- **Interactive 512-Channel Bar Graph**: High-FPS hardware-accelerated JavaFX canvas displaying 32 DMX channels simultaneously in the viewport with live values (`0–255`), smooth scrolling, and quick-jump range buttons (`1-32`, `33-64`, `65-96`, `97-128`, `129-256`, `257-512`).
- **Resizable Split-Pane Layout**: Custom Material Design drag-handle pills allow dynamic vertical resizing between the telemetry charts, control deck, and DMX channel visualizer.
- **Automatic Persistence**: All settings (driver, port, universe, target FPS) are automatically saved to `config.json`.

---

## Installation & Quick Start (GitHub Release)

### 1. Automatic Installer (Recommended)
Download [`install.sh`](https://github.com/exitishere42/artnet2dmx/releases/latest/download/install.sh) (Linux) or [`install.bat`](https://github.com/exitishere42/artnet2dmx/releases/latest/download/install.bat) (Windows) from the [**Releases**](https://github.com/exitishere42/artnet2dmx/releases/latest) page:
- Automatically installs the application into `~/artnet2dmx` (e.g., `/home/regie/artnet2dmx` on Linux or `%USERPROFILE%\artnet2dmx` on Windows).
- Checks if **Java 21+** is already installed — skips installation if present, or automatically installs the latest OpenJDK 21 LTS if missing.
- Creates a **Desktop Entry / Start Menu App** with the **artnet2dmx** logo so you can launch it directly from your application dash.

**Linux One-Line Install:**
```bash
curl -fsSL https://github.com/exitishere42/artnet2dmx/releases/latest/download/install.sh | bash
```

**Windows Install:**
Download and run `install.bat` from the [latest release](https://github.com/exitishere42/artnet2dmx/releases/latest).

### 2. Standalone Linux AppImage
Download [`artnet2dmx-x86_64.AppImage`](https://github.com/exitishere42/artnet2dmx/releases/latest/download/artnet2dmx-x86_64.AppImage) from the [latest release](https://github.com/exitishere42/artnet2dmx/releases/latest):
```bash
chmod +x artnet2dmx-x86_64.AppImage
./artnet2dmx-x86_64.AppImage
```

### 3. Uninstallation (`uninstall.sh` / `uninstall.bat`)
To completely remove **artnet2dmx** (including `~/artnet2dmx`, icons, and the Desktop/Start Menu entry):

**Linux One-Line Uninstall:**
```bash
curl -fsSL https://github.com/exitishere42/artnet2dmx/releases/latest/download/uninstall.sh | bash
```

**Windows Uninstall:**
Download and run [`uninstall.bat`](https://github.com/exitishere42/artnet2dmx/releases/latest/download/uninstall.bat) from the [latest release](https://github.com/exitishere42/artnet2dmx/releases/latest) (or run `%USERPROFILE%\artnet2dmx\uninstall.bat`).

---

## Build from Source

### Requirements
- **Java 21 LTS** or newer
- **Apache Maven 3.8+**

### Run Application (Development)
On Windows (batch launcher):
```cmd
run.bat
```

Or directly via Maven:
```bash
mvn exec:java
```

### Rebuild AppImage (Linux / WSL)
```bash
mvn clean package -DskipTests
./build-appimage.sh
```

### Compile & Run Tests
```bash
mvn clean test
```

### Build Executable Fat JAR
```bash
mvn clean package
```
The standalone fat JAR will be created at `target/artnet2dmx-1.0.0-all.jar`.

### Headless CLI Mode (No GUI)
```bash
java -jar target/artnet2dmx-1.0.0-all.jar --cli --driver jna-ftdi --universe 0 --fps 35
```

---

## Author

```text
+-------------------------------------------------------------------+
|  Programmed by:     exitishere42                                  |
|  GitHub:            https://github.com/exitishere42               |
|  Repository:        https://github.com/exitishere42/artnet2dmx    |
+-------------------------------------------------------------------+
```
