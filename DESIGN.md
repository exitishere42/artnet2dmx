# Leitfaden: Modernes UI- & UX-Design für eigene Software-Projekte

Dieser Leitfaden erklärt Schritt für Schritt, wie hochwertige, moderne Benutzeroberflächen (UI) mit exzellenter Benutzerführung (UX) gestaltet und im Code umgesetzt werden – unabhängig vom konkreten Inhalt oder Layout der Anwendung. 

Er dient als universelles Dokumentations- und Nachschlagewerk für andere Entwickler und Designer, die ihre eigenen Programme mit demselben ästhetischen und professionellen Qualitätsstandard aufbauen möchten.

---

## 1. Grundphilosophie: Warum professionelles UI-Design entscheidend ist

Ein gutes UI ist mehr als nur „hübsch aussehen“. Es erfüllt konkrete Aufgaben:
1. **Reduzierung kognitiver Belastung**: Der Benutzer muss auf einen Blick verstehen, was wichtig ist und wo er interagieren kann.
2. **Plattformunabhängige Konsistenz**: Die Anwendung sieht auf Windows, Linux und macOS exakt gleich hochwertig aus.
3. **Visuelle Haptik & Feedback**: Jede Interaktion (Hover, Klick, Drag) gibt unmittelbare, klare Rückmeldung.

---

## 2. Das Farbsystem (Material Design 2 Dark Standard)

Verwende für dunkle Benutzeroberflächen **niemals rein schwarze Hintergründe (`#000000`)**. Rein schwarz auf rein weiß führt zu starkem Überstrahlen (*Halation Effect*) und ermüdet die Augen.

### A. Das Prinzip der Höhenebenen (Elevation Surfaces)
Verwende ein gestuftes Graustufensystem. Elemente, die weiter „oben“ liegen (Karten, Popups, App-Bars), werden schrittweise leicht aufgehellt:

| Ebene / Elevation | Hex-Code | Verwendungszweck |
| :--- | :--- | :--- |
| **0dp Base** | `#121212` | Haupt-Hintergrund der gesamten Anwendung. |
| **1dp Card / Sheet** | `#1E1E1E` | Inhaltskarten, Container, Hauptbereiche. |
| **2dp Control Box** | `#232323` | Eingabefelder, Buttons, Header-Leisten, ComboBoxen. |
| **4dp Floating/Chip**| `#272727` | Status-Chips, Dropdown-Menüs, kleine Badges. |
| **8dp Modal/Tooltip**| `#2E2E2E` | Tooltips, Dialogfenster, Overlays. |

### B. Akzent- & Interaktionsfarben
Wähle gezielte, reduzierte Akzentfarben für Interaktionen:

- **Haupt-Akzent (Primary Teal/Cyan)**: `#03DAC6`
  - Leicht entsättigt, damit es auf dunklem Grund nicht „flimmert“. Verwendung für aktives Feedback, Konturen und Fokus-Ränder.
- **Interaktions-Zustände für Bedienelemente (z. B. Zieh-Griffe / Drag Handles)**:
  - **Ruhezustand**: `#8E8E8E` (gut sichtbares, neutrales Grau)
  - **Hover (Maus drüber)**: `#00E5FF` (leuchtendes Cyan für sofortiges Feedback)
  - **Pressed (Gedrückt / Ziehen)**: `#00FF9A` (Neon-Mintgrün für physische Klick-Bestätigung)
- **Fehler & Stopp**: `#CF6679` (angenehmes Material-Rot statt schrillem Signalrot)
- **Textfarben (mit Transparenz/Deckkraft)**:
  - **Hohe Betonung**: `#E1E1E1` (87% Deckkraft)
  - **Mittlere Betonung**: `#9E9E9E` (60% Deckkraft)
  - **Deaktiviert**: `#616161` (38% Deckkraft)

---

## 3. Gestaltungsregeln für Controls & Interaktions-Elemente

### 1. Das Konzept der Zieh-Pille (Drag Handle)
Wenn Bereiche in der Software verschiebbar oder in der Höhe anpassbar sind:
- Verwende eine zentrierte, abgerundete Kapsel/Pille (z. B. `52 x 4 px`, `border-radius: 2px`).
- Vermeide unscharfe Glow- oder Schatteneffekte (`dropshadow`), da diese auf hochauflösenden Bildschirmen oft schwammig wirken.
- Setze auf gestochen scharfe Vektor-Farbwechsel (`#8E8E8E` → `#00E5FF` → `#00FF9A`).

### 2. Mathematisch symmetrisches Raster
- Alle Abstände (Paddings, Margins, Trennlücken) sollten Vielfache eines festen Rasters sein (z. B. 4px, 8px, 12px, 16px).
- Wenn der Trennbalken zwischen zwei Modulen 12px hoch ist, sollte auch die Lücke zum oberen Header exakt 12px betragen. Das sorgt für visuelle Ruhe.

### 3. Vektor-Iconografie & Strikter Emoji-Verzicht
- **Verwende niemals System-Emojis in Software-UIs** (z. B. ⚙️, 🚀, 🛑). Betriebssysteme (Windows, Linux, macOS) rendern Emojis vollkommen unterschiedlich, was das Design zerstört.
- Verwende stattdessen saubere Vektor-Icons (z. B. aus dem **Lucide Icon Set** auf [lucide.dev](https://lucide.dev)). Rendere diese als Vektorpfade auf Canvases oder SVGs.

---

## 4. Technische Umsetzung & Architektur im Code (Best Practices)

Egal welche Programmiersprache oder welches Framework du nutzt (JavaFX, Web/React, Qt, Flutter):

### Step 1: Zentrale Theme-Klasse anlegen
Hartcodierte Farbcodes verstreut im Code sind ein Wartungs-Albtraum. Erstelle eine zentrale Theme-Klasse oder CSS-Variablen:

```java
public final class UITheme {
    public static final String COLOR_BG = "#121212";
    public static final String COLOR_SURFACE = "#1E1E1E";
    public static final String COLOR_CONTROL = "#232323";
    public static final String COLOR_PRIMARY = "#03DAC6";
    public static final String COLOR_HOVER = "#00E5FF";
    public static final String COLOR_PRESSED = "#00FF9A";
}
```

### Step 2: Komponenten-Wrapper für einheitliche Karten
Packe funktionale Einheiten in Karten-Container mit abgerundeten Ecken (`border-radius: 4px`), dezentem Rand (`#2C2C2C`) und einheitlichem Innenabstand (`Insets`).

### Step 3: Schutzgrenzen für flexible Layouts
Setze bei anpassbaren Modulen stets minimale und maximale Größen-Limits (`minHeight` / `minWidth`). Das verhindert zuverlässig, dass Bedienelemente versehentlich unsichtbar oder unbenutzbar werden.

---

## 5. Checkliste für das eigene Projekt (Definition of Done)

- [ ] **Keine rein schwarzen Hintergründe**: Verwendet das Stufensystem von `#121212` bis `#2E2E2E`.
- [ ] **Einheitliches Raster**: Abstände (Margins/Paddings) folgen einem festen System (z. B. 12px / 16px).
- [ ] **Klares Feedback**: Alle Buttons, Slider und Trennlinien ändern beim Drüberfahren (Hover) und Klicken (Pressed) sichtbar die Farbe.
- [ ] **Keine Emojis**: Alle Icons basieren auf einheitlichen Vektor-Grafiken (Lucide).
- [ ] **Gut lesbare Texte**: Kontraste erfüllen Mindestanforderungen für ermüdungsfreies Arbeiten.
