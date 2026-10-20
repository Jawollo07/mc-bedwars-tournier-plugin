# Turnier – BedWars-Turnierverwaltung für Paper 26.3

Eigenständiges Paper-Plugin (kein Skript, kein SkBee nötig). Benötigt Java 25.

## Bauen
    gradle build        # Ergebnis: build/libs/turnier-1.0.0.jar
(Alternativ Gradle-Wrapper mit `gradle wrapper` erzeugen.)

## Befehle
/turnier, /bwturnier, /turnierboard – Rechte: `turnier.admin` (Standard: OP).
Daten: plugins/Turnier/data.yml, Einstellungen: plugins/Turnier/config.yml.

## MBedwars-Anbindung
Getestet gegen die API von MBedwars 5.5.9.1 (`libs/MBedwars-5_5_9_1.jar`, nur zum Kompilieren).
Automatisch gewertet werden (nur bei gestartetem Turnier, laufender Runde und `/bwturnier on`):
- Kill: `points.kill` für Spieler und sein Turnierteam
- Bettzerstörung durch einen Spieler: `points.bed`
- Rundensieg: `points.win` einmal pro Turnierteam mit Gewinnern (bei Gleichstand nichts)


## Stability notes

- Paper API is pinned to a specific build so the same source does not silently resolve a different API on later builds.
- Tournament standings are retained across restarts, but an in-progress tournament round is deliberately not resumed after a server restart. Start the tournament and its next round explicitly.
- Automatic score changes are coalesced and saved shortly after scoring events; the plugin also flushes its data when disabled.
- New team names must be 1–32 letters or digits, underscores, or hyphens. Existing saved team names are retained when loading older data.
- Build and runtime compatibility should be verified against the exact Paper build and MBedwars 5.5.9.1 JAR deployed on the server.
