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
