# Slyrex (Fabric, nur Client) – Minecraft 1.21.11

Alle Slyrex-Module in **einem Menü**. Öffnen mit der **rechten Shift-Taste** oder über den
**Slyrex**-Knopf oben links im Pausemenü. Jedes Modul lässt sich einzeln an-/ausschalten,
hat ein ⚙-Einstellungsfenster und kann mit ❤ zu den Favoriten.

Benötigt: Fabric Loader ≥ 0.17 und Fabric API. Die alten Einzel-Mods (`auktionsmod`, `begleiter`)
bitte aus dem mods-Ordner entfernen – sie sind hier schon enthalten.

## Module

| Modul | Was es macht |
|---|---|
| **Auktionen** (deine Auktions-Mod) | Item in der Hand → Taste **O** → Auktion starten, Gebote per `/pay` werden erkannt, HUD, Rangliste, Rückzahlungen, Gewinner-Animation. ⚙ = Design (gilt für alle Slyrex-Fenster). |
| **Begleiter** (deine Begleiter-Mod) | Begleiter folgt dir, Dialoge, Skin-Übernahme, Tsundere-Modus. **K** = Einstellungen, **G** = reden. |
| **Totem-Zähler** | HUD mit Totems im Inventar und Totem-Pops anderer Spieler (Auto-Reset, Reset beim Tod). |
| **Fight-Presets** | Inventar-Layout speichern, im Inventar werden fehlende Items als Schatten und eine „fehlt“-Liste angezeigt. Export/Import per Code. |
| **Invsee-Zähler** | Zählt bis zu 10 Items (Standard: Totems, End-Kristalle) in fremden Inventaren, z.B. `/invsee`. Eigenes Inventar zählt nicht. |
| **Zweiter Chat** | Bis zu 3 extra Chat-Boxen mit Regeln (enthält / beginnt mit / Regex) und Vorlagen (Server, Teleport, Privat, Clan, Erwähnungen). |
| **Adblocker** | Blendet Links, Stream-Werbung, Handel und TP-Werbung aus. Echte TPA-Anfragen bleiben sichtbar, Ränge als Ausnahme. |
| **Fake-Check** | Warnt, wenn Namen/Clan-Tags bekannten Einträgen ähneln (`S1yrex` ≈ `Slyrex`) und fragt vor `/pay`, `/tpa` … an solche Namen nach. Prüft **nicht**, ob ein Account echt ist. |
| **Streamer-Modus** | F3 zeigt keine Koordinaten mehr, Koordinaten im Chat werden versteckt, roter Hinweis im HUD. |
| **Fadenkreuz** | Eigene Farben (1–5), Verlauf oder Farbwechsel, Größe/Abstand/Dicke, Farbe bei Ziel in Reichweite und nach einem Treffer. |
| **DuraGuard** | Warnung bei 5/10/20 % Haltbarkeit (Hand + Rüstung), optional werden Werkzeuge darunter blockiert. |
| **Sound-Muffler** | Lautstärke einzelner Sounds (stumm … 100 %), Suche, Vorhören. |
| **Rename-Studio** | Farbverlauf + Fett/Kursiv mit Live-Vorschau, Ausgabe als `&#RRGGBB`, `&x&R&R…` oder MiniMessage, direkt als `/rename`-Befehl in den Chat. |
| **Rechner** | Taschenrechner mit `k`/`m`/`b`-Kürzeln (`1,5m - 250k`), Knopf **±** neben dem Inventar. |

**HUD-Editor** (im Menü oben rechts): HUD-Elemente mit der Maus verschieben, rastet an Raster,
Rändern und Bildschirmmitte ein, Rechtsklick setzt ein Element zurück.

Einstellungen: `config/slyrex/` (`slyrex.json`, `auktion.json`, `begleiter.json`).

## Bauen

```bash
./gradlew build
```

Die fertige Datei liegt danach in `build/libs/slyrex-mod-1.0.0.jar`.
