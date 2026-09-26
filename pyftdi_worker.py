#!/usr/bin/env python3
"""
artnet2dmx - pyftdi DMX Hardware Worker
Empfängt 512-Byte DMX-Frames über stdin und sendet sie direkt über die pyftdi Bibliothek
an den FTDI FT232R Chip (JMS USB2DMX PRO) mit echter Hardware-Break/MAB-Generierung.
"""
import sys
import time
import os

# Suche pyftdi in venv oder System
venv_paths = [
    '/home/regie/old_artnet2dmx/.venv/lib/python3.12/site-packages',
    '/home/regie/old_artnet2dmx/.venv/lib/python3.11/site-packages',
    os.path.expanduser('~/.local/lib/python3.12/site-packages'),
]
for p in venv_paths:
    if os.path.exists(p) and p not in sys.path:
        sys.path.insert(0, p)

try:
    from pyftdi.ftdi import Ftdi
except ImportError as e:
    sys.stderr.write(f"[pyftdi-worker] FEHLER: pyftdi nicht gefunden: {e}\n")
    sys.stderr.flush()
    sys.exit(1)

url = sys.argv[1] if len(sys.argv) > 1 and sys.argv[1] and not sys.argv[1].startswith('tty') and sys.argv[1] != 'Auto-Detect' else 'ftdi://ftdi:232/1'
sys.stderr.write(f"[pyftdi-worker] Oeffne {url}...\n")
sys.stderr.flush()

ftdi = Ftdi()
try:
    ftdi.open_from_url(url)
except Exception as e:
    try:
        ftdi.open_from_url('ftdi://ftdi:232/1')
    except Exception as e2:
        sys.stderr.write(f"[pyftdi-worker] FEHLER beim Oeffnen von {url}: {e2}\n")
        sys.stderr.flush()
        sys.exit(2)

ftdi.reset()
ftdi.set_baudrate(250000)
ftdi.set_line_property(bits=8, stopbit=2, parity='N', break_=False)

def micro_sleep(seconds: float):
    target = time.perf_counter() + seconds
    while time.perf_counter() < target:
        pass

def read_exact(stream, n):
    buf = bytearray(n)
    pos = 0
    while pos < n:
        chunk = stream.read(n - pos)
        if not chunk:
            return None
        buf[pos:pos+len(chunk)] = chunk
        pos += len(chunk)
    return bytes(buf)

sys.stderr.write("[pyftdi-worker] READY\n")
sys.stderr.flush()

stdin = sys.stdin.buffer
count = 0

try:
    while True:
        # Genau 512 Bytes für einen DMX-Universe-Frame einlesen
        frame = read_exact(stdin, 512)
        if not frame:
            break

        payload = b'\x00' + frame

        # 1. DMX Break (~180 µs TX Low)
        ftdi.set_break(True)
        micro_sleep(0.000180)

        # 2. DMX Mark After Break (~30 µs TX High)
        ftdi.set_break(False)
        micro_sleep(0.000030)

        # 3. Nutzdaten schreiben
        ftdi.write_data(payload)
        count += 1

except Exception as e:
    sys.stderr.write(f"[pyftdi-worker] Fehler im Sendebetrieb: {e}\n")
finally:
    try:
        # Nullframe senden vor Beenden
        ftdi.set_break(True)
        time.sleep(0.000180)
        ftdi.set_break(False)
        time.sleep(0.000030)
        ftdi.write_data(b'\x00' + bytes(512))
        ftdi.close()
    except Exception:
        pass
    sys.stderr.write(f"[pyftdi-worker] Beendet nach {count} Frames.\n")
    sys.stderr.flush()
