# Turnier – BedWars-Turnierverwaltung für Paper 26.3

Eigenständiges Paper-Plugin (kein Skript, kein SkBee nötig). Benötigt Java 25.

## Bauen

```sh
gradle clean build
```

Ergebnis: `build/libs/turnier-1.0.0.jar`.

Die Paper- und MBedwars-APIs werden beim Build über Maven-Repositories bezogen. Es muss keine MBedwars-JAR in `libs/` abgelegt oder ins Repository eingecheckt werden.

## Befehle

- `/turnier`, `/bwturnier`, `/turnierboard`
- Berechtigung: `turnier.admin` (Standard: OP)
- Daten: `plugins/Turnier/data.yml`
- Einstellungen: `plugins/Turnier/config.yml`

## MBedwars-Anbindung

Die Integration verwendet die veröffentlichte MBedwars-API (`de.marcely.bedwars:API:5.5.9`) und registriert das Plugin beim Start als MBedwars-Addon. MBedwars bleibt eine optionale Server-Abhängigkeit: Ohne installiertes MBedwars startet die Turnierverwaltung weiter, die automatische Wertung bleibt dann deaktiviert.

Automatisch gewertet wird nur, wenn ein Turnier aktiv ist, eine Runde läuft und die automatische Wertung eingeschaltet ist:

- Kill: `points.kill` für Spieler und dessen Turnierteam
- Bettzerstörung durch einen Spieler: `points.bed`
- Rundensieg: `points.win` einmal pro Turnierteam mit Gewinnern; bei Gleichstand keine Siegpunkte

## Stability notes

- Paper API is pinned to a specific build so the same source does not silently resolve a different API on later builds.
- Tournament standings are retained across restarts, but an in-progress tournament round is deliberately not resumed after a server restart. Start the tournament and its next round explicitly.
- Automatic score changes are coalesced and saved shortly after scoring events; the plugin also flushes its data when disabled.
- New team names must be 1–32 letters or digits, underscores, or hyphens. Existing saved team names are retained when loading older data.
