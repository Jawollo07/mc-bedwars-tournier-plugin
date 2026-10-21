# Turnier – BedWars-Turnierverwaltung für Paper

Ein Paper-Plugin zur Verwaltung von BedWars-Turnieren mit eigenen Turnierteams, Spielerzuordnungen, Punkten, Statistiken, Rundensteuerung und einem persönlichen Sidebar-Scoreboard. Optional bindet es **MBedwars** an, um Kills, Bettzerstörungen und Rundensiege automatisch zu werten.

> **Projektstatus:** Die hier dokumentierten Funktionen entsprechen dem Quellcode dieses Branches. Vor dem produktiven Einsatz bitte den Build und die Integration auf deinem konkreten Paper-/MBedwars-Server testen.

## Inhaltsverzeichnis

- [Funktionen](#funktionen)
- [Voraussetzungen](#voraussetzungen)
- [Installation](#installation)
- [Schnellstart](#schnellstart)
- [Befehlsübersicht](#befehlsübersicht)
- [Befehle im Detail](#befehle-im-detail)
- [Automatische MBedwars-Wertung](#automatische-mbedwars-wertung)
- [Konfiguration](#konfiguration)
- [Speicherung und Neustarts](#speicherung-und-neustarts)
- [Berechtigungen](#berechtigungen)
- [Build aus dem Quellcode](#build-aus-dem-quellcode)
- [Fehlerbehebung](#fehlerbehebung)
- [Technische Hinweise und Grenzen](#technische-hinweise-und-grenzen)

## Funktionen

- **Turnierteams:** Teams erstellen, umbenennen, löschen und anzeigen.
- **Spielerzuordnung:** Spieler dauerhaft einem Turnierteam zuordnen; die Zuordnung ist unabhängig von den Teams des BedWars-Plugins.
- **Punkteverwaltung:** Punkte für einzelne Spieler oder ganze Turnierteams addieren, abziehen, setzen oder zurücksetzen.
- **Statistiken:** Team- und Spielerpunkte sowie erfasste Kills und Bettzerstörungen einsehen.
- **Turniersteuerung:** Turnier starten, einzelne Runden beginnen und beenden sowie das Turnier abschließen.
- **Rangliste und Bekanntgabe:** Teams nach Punktestand sortiert anzeigen und die Rangliste im Chat bekannt geben.
- **Sidebar-Scoreboard:** Spieler sehen ihre persönlichen Punkte und die Punkte ihres Turnierteams.
- **Optionale automatische Wertung:** Mit MBedwars werden Kills, gültige Bettzerstörungen und Rundensiege ausschließlich in der ausgewählten Turnierarena während einer aktiven Turnierrunde erfasst.
- **Persistente Daten:** Teamstände und Spielerdaten bleiben über Serverneustarts hinweg gespeichert.
- **Kein Skript/SkBee erforderlich:** Das Plugin ist als Java-Plugin für Paper aufgebaut.

## Voraussetzungen

| Komponente | Anforderung |
|---|---|
| Minecraft-Server | Paper mit der im Projekt konfigurierten API-Version |
| Java | Java 25 |
| Gradle zum Selbstbauen | Gradle 9.x; der CI-Build verwendet Gradle 9.2.1 |
| MBedwars | Optional; nur für die automatische BedWars-Wertung |
| MBedwars-JAR im Git-Repository | **Nicht erforderlich** |

Die Paper-API und die MBedwars-API werden beim Build über Maven-Repositories aufgelöst. Es muss keine MBedwars-JAR manuell in `libs/` abgelegt oder ins Git-Repository eingecheckt werden.

## Installation

1. Lade die fertige `turnier-1.0.0.jar` aus dem Build-Artefakt oder baue das Projekt selbst (siehe [Build aus dem Quellcode](#build-aus-dem-quellcode)).
2. Kopiere die JAR in das `plugins/`-Verzeichnis deines Paper-Servers.
3. Falls du automatische Wertung verwenden willst, installiere zusätzlich MBedwars in einer mit der verwendeten API kompatiblen Version.
4. Starte den Server neu. Prüfe beim Start die Konsole auf Meldungen des Plugins.
5. Öffne `plugins/Turnier/config.yml` und passe Präfix, Scoreboard und Punktwerte an.
6. Lege deine Turnierteams an und ordne Spieler zu.

**Hinweis:** Die Plugin-JAR enthält die Turnierverwaltung, nicht Paper oder MBedwars selbst. MBedwars ist zur Laufzeit optional; ohne MBedwars funktionieren die Verwaltungsbefehle weiterhin, automatische Kills-/Bett-/Sieg-Wertung ist dann aber nicht verfügbar.

## Schnellstart: komplettes Turnier mit drei Runden

Dieser Ablauf steuert Vorbereitung, Rundenstarts, Pausen und Siegerehrung über das Plugin. Für den automatischen Arena-Start und die automatische Wertung muss MBedwars installiert und kompatibel sein.

### 1. Einmalige Vorbereitung

Lege Teams an und ordne jeden Spieler zu. Spieler müssen den Server bereits mindestens einmal betreten haben.

```text
/turnier team create Rot
/turnier team create Blau
/turnier spieler assign Jannik Rot
/turnier spieler assign Alex Blau
/turnier team bind Rot RED
/turnier team bind Blau BLUE
/turnier arena Bedwars1
/bwturnier on
```

Ersetze Beispielnamen durch deine tatsächlichen Team-, Spieler-, Farb- und Arenanamen. Die MBedwars-Farben müssen in der Arena aktiviert sein.

### 2. Turnier starten

```text
/bwturnier start 3
/bwturnier next
```

Der erste Befehl richtet ein Turnier mit drei Runden ein. Der zweite ordnet die registrierten Spieler ihren MBedwars-Teams zu und startet Runde 1. Wenn zugeordnete Spieler offline sind, Teamfarben fehlen oder Plätze nicht verfügbar sind, startet die Runde nicht und das Plugin meldet die Fehlerzahl.

### 3. Pause nach Runde 1

Wenn MBedwars die Runde beendet, schließt das Plugin die Turnierrunde automatisch und pausiert die Wertung. Warte, bis die Arena wieder in der Lobby ist. Für Zwischenstand und Status:

```text
/bwturnier pause
/bwturnier status
```

Die Punkte und Statistiken bleiben erhalten. Die Pause beendet kein laufendes Match gewaltsam.

### 4. Runde 2 und Runde 3

Sobald die Arena wieder in der Lobby ist, startet dieser Befehl jeweils die nächste Runde:

```text
/bwturnier next
```

Nach dem Ende von Runde 2 kannst du wieder /bwturnier pause für den Zwischenstand verwenden. Danach /bwturnier next für Runde 3. Der Rundenzähler wird automatisch verwaltet.

### 5. Siegerehrung

Nach Ende der dritten Runde und sobald kein Match mehr läuft:

```text
/bwturnier ceremony
```

Das Plugin beendet das Turnier und verkündet die Rangliste und die Top-Platzierungen auf Basis der Gesamtpunkte. Bei Gleichstand auf Platz 1 wird kein eindeutiger Champion gespeichert; der Gleichstand wird angekündigt. /bwturnier finish bleibt als Alias verfügbar.

**Kurzfassung:** Teams und Spieler zuordnen → Arena setzen → /bwturnier start 3 → /bwturnier next → nach jeder Runde warten, bis die Arena zurück in der Lobby ist → /bwturnier next → nach Runde 3 /bwturnier ceremony.

## Befehlsübersicht

### `/turnier` – Teams, Spieler und Punkte

| Befehl | Zweck |
|---|---|
| `/turnier` | Zeigt die Hilfe mit den verfügbaren Befehlen |
| `/turnier team create <Name>` | Erstellt ein Turnierteam |
| `/turnier team delete <Name>` | Löscht ein Team; seine Mitglieder bleiben registriert, sind danach aber keinem Team zugeordnet |
| `/turnier team rename <Alt> <Neu>` | Benennt ein Team um und aktualisiert die Zuordnungen |
| `/turnier team list` | Zeigt alle Teams und ihre Punkte |
| `/turnier team info <Name>` | Zeigt Punkte, Siege, Kills, Bettzerstörungen und Mitglieder |
| `/turnier spieler assign <Spieler> <Team>` | Ordnet einen bekannten Spieler einem Team zu |
| `/turnier spieler remove <Spieler>` | Entfernt die Teamzuordnung eines Spielers |
| `/turnier spieler info <Spieler>` | Zeigt die gespeicherten Spielerstatistiken |
| `/turnier add spieler <Name> <Punkte>` | Addiert Punkte zum Spieler |
| `/turnier add team <Name> <Punkte>` | Addiert Punkte zum Team |
| `/turnier remove spieler <Name> <Punkte>` | Zieht Punkte vom Spieler ab, mindestens bis 0 |
| `/turnier remove team <Name> <Punkte>` | Zieht Punkte vom Team ab, mindestens bis 0 |
| `/turnier set spieler <Name> <Punkte>` | Setzt die Spielerpunkte auf einen Wert |
| `/turnier set team <Name> <Punkte>` | Setzt die Teampunkte auf einen Wert |
| `/turnier reset spieler <Name>` | Setzt nur die Punkte dieses Spielers auf 0 |
| `/turnier reset team <Name>` | Setzt nur die Punkte dieses Teams auf 0 |
| `/turnier resetall` | Setzt alle Spieler- und Teampunkte auf 0 |
| `/turnier top` | Zeigt die Rangliste nach Teampunkten |
| `/turnier sudo <OnlineSpieler> <Befehl>` | Führt einen Befehl als Online-Spieler aus |

**Namensregeln für neue Teams:** 1–32 Buchstaben, Ziffern, Unterstriche (`_`) oder Bindestriche (`-`). Teamnamen werden bei der Suche unabhängig von Groß-/Kleinschreibung behandelt.

**Spielerzuordnung:** Ein Spieler muss dem Server mindestens einmal beigetreten sein, damit er in der Turnierverwaltung bekannt ist. Die Zuordnung zum Turnierteam erfolgt über die hier gespeicherten Daten und nicht automatisch anhand eines MBedwars-Teams.

**Punkte:** Die Punktbefehle akzeptieren nur nichtnegative ganze Zahlen. Das Addieren begrenzt den Wert auf den Integer-Bereich, statt bei einem Überlauf negativ zu werden. `resetall` setzt Punkte zurück, nicht die Teams, Spielerzuordnungen oder erfassten Kill-/Bett-Zähler.

**Vorsicht bei `sudo`:** Der Befehl führt den angegebenen Befehl mit den Rechten des Zielspielers aus. Verwende ihn nur, wenn du genau verstehst, welchen Befehl du ausführst.

### `/bwturnier` – Ablauf und Wertung

| Befehl | Zweck |
|---|---|
| `/bwturnier start [Runden]` | Bereitet ein Turnier mit standardmäßig 3 Runden oder der angegebenen Rundenzahl (1–100) vor |
| `/bwturnier next` (Alias: `round`) | Ordnet Spieler zu und startet die nächste MBedwars-Runde automatisch |
| `/bwturnier pause` (Alias: `roundend`) | Zeigt den Zwischenstand und pausiert die Turnierwertung zwischen Runden |
| `/bwturnier ceremony` (Alias: `finish`, `siegerehrung`) | Beendet das Turnier und verkündet die Siegerehrung mit Rangliste |
| `/bwturnier announce` | Gibt die Rangliste im Chat bekannt |
| `/bwturnier stats <Team>` | Zeigt die Statistiken eines Teams |
| `/bwturnier win <Team>` | Vergibt manuell die konfigurierte Siegpunktzahl und zählt einen Teamsieg |
| `/bwturnier on` | Aktiviert die automatische Wertung |
| `/bwturnier off` | Deaktiviert die automatische Wertung |
| `/bwturnier status` | Zeigt Turnierstatus, Wertungsstatus und Rundennummer |

Der Turnierstart legt die Rundenzahl fest; `/bwturnier next` bereitet Spieler vor und startet die nächste Arena-Runde. Automatische Wertung ist nur aktiv, wenn alle vier Bedingungen erfüllt sind:

1. Das Turnier wurde mit `/bwturnier start` gestartet.
2. Eine Runde wurde mit `/bwturnier round` gestartet und noch nicht beendet.
3. Die automatische Wertung ist mit `/bwturnier on` aktiviert.
4. Die MBedwars-Arena wurde mit `/turnier arena <Arena>` als Turnierarena ausgewählt.

### `/turnierboard` – Scoreboard

| Befehl | Zweck |
|---|---|
| `/turnierboard on` | Aktiviert das Turnier-Scoreboard für alle Spieler |
| `/turnierboard off` | Deaktiviert es und stellt das zuvor verwendete Scoreboard nach Möglichkeit wieder her |
| `/turnierboard status` | Zeigt, ob das Scoreboard global aktiviert ist |

Das Scoreboard zeigt den konfigurierten Titel, den Spielernamen, das zugeordnete Turnierteam, die persönlichen Punkte und die Teampunkte. Die Aktualisierung erfolgt standardmäßig alle 40 Ticks (etwa 2 Sekunden bei 20 TPS).

## Automatische MBedwars-Wertung

Die Anbindung verwendet die veröffentlichte MBedwars-API `de.marcely.bedwars:API:5.5.9`. Beim Start versucht das Plugin, sich über den vorgesehenen Addon-Mechanismus von MBedwars zu registrieren. Die Events werden über die MBedwars-API verarbeitet.

| Ereignis | Wertung |
|---|---|
| Spieler eliminiert einen anderen Spieler | Der Killer erhält `points.kill`; das zugeordnete Turnierteam erhält dieselbe Punktzahl |
| Spieler zerstört ein Bett | Bei spielerverursachter, nicht abgebrochener Bettzerstörung erhält der Spieler `points.bed`; sein Turnierteam erhält dieselbe Punktzahl |
| MBedwars-Runde endet mit Gewinnern | Das mit dem Siegerteam verknüpfte Turnierteam erhält einmal `points.win` und einen Teamsieg, auch wenn die Gewinner offline sind; ohne Mapping wird anhand der Online-Gewinner zugeordnet |
| Runde endet unentschieden | Keine automatischen Sieg-Punkte |

Die Spielerstatistik speichert Kills und Bettzerstörungen separat. Die persönlichen Punkte und die Teampunkte werden unabhängig voneinander gezählt. Wenn ein Spieler keinem Turnierteam zugeordnet ist, kann er persönliche Punkte und Statistiken sammeln, aber kein Turnierteam erhält seine Punkte.

### Aktivieren oder deaktivieren

In der Konfiguration:

```yaml
mbedwars:
  enabled: true
```

Zusätzlich muss die Wertung im Turnierablauf mit `/bwturnier on` aktiviert sein und die richtige Arena mit `/turnier arena <Arena>` ausgewählt werden. Nur Ereignisse aus dieser Arena zählen; andere öffentliche MBedwars-Matches beeinflussen das Turnier nicht. Mit `/bwturnier off` wird die Wertung pausiert, ohne die vorhandenen Punktestände zu löschen.

Wenn MBedwars nicht installiert ist, überspringt das Plugin die Integration. Wenn MBedwars vorhanden ist, die Addon-Registrierung aber fehlschlägt, prüfe die Serverkonsole und die API-Kompatibilität.

## Konfiguration

Die Standardkonfiguration wird nach dem ersten Start unter `plugins/Turnier/config.yml` erstellt.

```yaml
# Präfix vor Nachrichten; &-Farbcodes werden unterstützt
prefix: "&8[&6Turnier&8] &7"

scoreboard:
  title: "&6&lBEDWARS"
  # Aktualisierungsintervall in Ticks (20 Ticks = 1 Sekunde)
  refresh-ticks: 40

# Punkte pro Ereignis bei automatischer Wertung
points:
  kill: 1
  bed: 5
  win: 10

# Optionale Integration mit MBedwars
mbedwars:
  enabled: true
```

| Einstellung | Bedeutung | Standard |
|---|---|---|
| `prefix` | Präfix vor Plugin-Nachrichten | `&8[&6Turnier&8] &7` |
| `scoreboard.title` | Titel des Sidebar-Scoreboards | `&6&lBEDWARS` |
| `scoreboard.refresh-ticks` | Intervall zwischen Scoreboard-Aktualisierungen; 20 Ticks entsprechen ungefähr einer Sekunde | `40` |
| `points.kill` | Punkte für einen gültigen Kill | `1` |
| `points.bed` | Punkte für eine gültige Bettzerstörung | `5` |
| `points.win` | Punkte für einen Rundensieg pro Turnierteam mit Gewinnern | `10` |
| `mbedwars.enabled` | Aktiviert/deaktiviert den Versuch, MBedwars anzubinden | `true` |

Negative automatische Punktwerte werden beim Werten auf 0 begrenzt und im Log gemeldet. Nach Änderungen an der Konfiguration empfiehlt sich ein Serverneustart, damit alle Einstellungen sicher neu geladen werden.

## Speicherung und Neustarts

Die Daten liegen im Verzeichnis:

```text
plugins/
└── Turnier/
    ├── config.yml
    └── data.yml
```

- `config.yml` enthält die Einstellungen.
- `data.yml` enthält Turnierstatus, Rundennummer, Champion, Teams und Spielerwerte.
- Spieler werden über ihre UUID gespeichert; der angezeigte Name wird aktualisiert, wenn sie erneut beitreten.
- Teamnamen, Punkte, Siege, Zuordnungen und Spielerstatistiken bleiben nach einem Neustart erhalten.
- Änderungen an automatischen Wertungen werden gesammelt gespeichert; beim Deaktivieren des Plugins werden Daten ebenfalls gespeichert.
- Die Speicherung erfolgt über eine temporäre Datei und einen Dateiaustausch, um das Risiko einer teilweise geschriebenen Datendatei zu verringern.

**Wichtig zum Neustart:** Eine laufende Turnierrunde wird nach einem Prozessneustart absichtlich nicht fortgesetzt. Die Punktestände bleiben erhalten, aber du musst das Turnier und anschließend die nächste Runde erneut starten. Starte nach einem Neustart bei Bedarf ein neues Turnier mit `/bwturnier start 3` und starte anschließend die erste Runde mit `/bwturnier next`.

Vor manuellen Änderungen an `data.yml` sollte eine Sicherungskopie angelegt werden. Bearbeite die Datei nicht, während der Server sie gleichzeitig speichert.

## Berechtigungen

| Berechtigung | Zweck | Standard |
|---|---|---|
| `turnier.admin` | Zugriff auf Turnierverwaltung, Ablaufsteuerung und Scoreboard-Befehle | OP |

Gib diese Berechtigung nur vertrauenswürdigen Personen. Die Befehle erlauben unter anderem das Ändern von Punkteständen, das Beenden eines Turniers und die Ausführung von Befehlen als Online-Spieler.

## Build aus dem Quellcode

### Voraussetzungen

- Git
- Java Development Kit (JDK) 25
- Gradle 9.x (die GitHub-Actions-Konfiguration verwendet Gradle 9.2.1)
- Netzwerkzugriff auf Maven Central, das Paper-Maven-Repository und das MBedwars-Maven-Repository

### Bauen

```sh
git clone https://github.com/Jawollo07/mc-bedwars-tournier-plugin.git
cd mc-bedwars-tournier-plugin
gradle --no-daemon clean build
```

Die fertige Plugin-JAR wird hier abgelegt:

```text
build/libs/turnier-1.0.0.jar
```

Die MBedwars-API wird als Maven-Abhängigkeit eingebunden. **Du musst keine JAR herunterladen, umbenennen oder in `libs/` kopieren.** Die API wird nicht als Ersatz für das eigentliche MBedwars-Server-Plugin ausgeliefert.

### Tests

```sh
gradle --no-daemon clean test
```

Die Tests prüfen unter anderem die sichere Addition von Punkten und Grenzfälle wie Überläufe oder negative Werte. Ein erfolgreicher Build sollte vor der Veröffentlichung geprüft werden; außerdem sollte die resultierende JAR auf einem Testserver mit der konkret eingesetzten Paper- und MBedwars-Version ausprobiert werden.

### CI

GitHub Actions baut das Projekt mit Java 25 und Gradle 9.2.1 bei Pull Requests. Der Workflow lädt die gebaute Plugin-JAR als Artefakt hoch.

## Fehlerbehebung

### Das Plugin wird beim Start nicht geladen

1. Prüfe, ob du Java 25 verwendest.
2. Prüfe die Fehlermeldung direkt vor bzw. nach `Enabling Turnier` in der Serverkonsole.
3. Stelle sicher, dass du eine für deine Paper-Version gebaute JAR verwendest.
4. Teste einen frischen Build mit `gradle --no-daemon clean build`.

### Automatische Wertung funktioniert nicht

1. Ist MBedwars installiert und wird es vom Server erfolgreich gestartet?
2. Steht `mbedwars.enabled: true` in `plugins/Turnier/config.yml`?
3. Zeigt `/bwturnier status` ein aktives Turnier und die richtige Runde?
4. Hast du `/bwturnier on` ausgeführt?
5. Ist der Spieler einem Turnierteam zugeordnet, wenn auch Team-Punkte erwartet werden?
6. Gibt es im Log eine Warnung über fehlgeschlagene MBedwars-Addon-Registrierung oder eine inkompatible API?

### Ein Spieler lässt sich nicht zuordnen

Der Spieler muss dem Server mindestens einmal beigetreten sein, damit das Plugin seine UUID und seinen Namen kennt. Prüfe außerdem die Schreibweise des Spielernamens und ob das gewünschte Turnierteam mit `/turnier team list` angezeigt wird.

### Das Scoreboard wird nicht angezeigt

- Prüfe `/turnierboard status`.
- Aktiviere es mit `/turnierboard on`.
- Prüfe `scoreboard.title` und `scoreboard.refresh-ticks` in der Konfiguration.
- Andere Plugins können ebenfalls Scoreboards verwalten und mit der Anzeige konkurrieren.

### Punktestand nach Neustart unerwartet

Die Team- und Spielerpunktstände werden gespeichert, die laufende Runde jedoch nicht fortgesetzt. Prüfe `plugins/Turnier/data.yml` und fertige vor manuellen Korrekturen eine Sicherung an.

## Technische Hinweise und Grenzen

- Turnierteams sind eine eigene Ebene und werden nicht automatisch aus MBedwars-Teams importiert oder synchronisiert.
- Der Rundensieg wird pro Turnierteam nur einmal gezählt, auch wenn mehrere Mitglieder dieses Teams als Gewinner gemeldet werden.
- Bei Gleichstand in MBedwars werden keine automatischen Sieg-Punkte vergeben.
- Beim Turnierende zählt der gespeicherte **Teampunktestand**. Bei Gleichstand wird kein Champion gespeichert.
- `/bwturnier win <Team>` ist eine manuelle Wertung und addiert die konfigurierte Siegpunktzahl zusätzlich zum vorhandenen Stand.
- `/turnier resetall` setzt nur Punkte zurück; Kill-/Bett-Zähler, Siege, Teams und Spielerzuordnungen bleiben bestehen.
- Turnierteams werden nicht automatisch aus MBedwars-Teams importiert; die Zuordnung erfolgt explizit mit `/turnier team bind <Turnierteam> <Farbe>`. Ein automatischer Neustart einer unterbrochenen Runde ist nicht implementiert.
- Das Plugin ist nicht als Ersatz für MBedwars gedacht; es verwaltet die Turnierwertung neben dem eigentlichen BedWars-Spielbetrieb.

## Mitwirken

Fehlerberichte und Verbesserungsvorschläge sind willkommen. Bitte füge bei einem Fehlerbericht hinzu:

- Paper- und Minecraft-Version
- Java-Version (`java -version`)
- verwendete MBedwars-Version (falls installiert)
- vollständige relevante Fehlermeldung aus der Serverkonsole
- Schritte, mit denen sich der Fehler reproduzieren lässt

Repository: https://github.com/Jawollo07/mc-bedwars-tournier-plugin



### Turnier und MBedwars gemeinsam steuern

Die Turnierverwaltung kann eine MBedwars-Arena als Turnierarena speichern und Turnierteams mit MBedwars-Farbteams verknüpfen. Die Zuordnung wird in `plugins/Turnier/data.yml` gespeichert.

1. Turnierteams und Spieler anlegen/zuordnen:
   - `/turnier team create <Turnierteam>`
   - `/turnier spieler assign <Spieler> <Turnierteam>`
2. MBedwars-Arena festlegen:
   - `/turnier arena <Arena>`
3. Jedes Turnierteam mit einer MBedwars-Farbe verbinden:
   - `/turnier team bind <Turnierteam> <Farbe>` (z. B. RED oder BLUE, abhängig von der MBedwars-Konfiguration)
4. Spieler anhand der gespeicherten Turnierzuordnung in die Arena bringen:
   - `/turnier match prepare`
5. Turnier und Arena gemeinsam starten:
   - `/bwturnier start`
   - `/turnier match start`
6. Match beenden:
   - `/turnier match stop`

`prepare` benötigt online befindliche Spieler, eine Arena im Lobby-Status, aktivierte passende MBedwars-Teams und freie Plätze. Spieler, die nicht zugeordnet werden konnten, werden in einer Fehlerzahl ausgewiesen. Vor einem echten Turnierablauf bitte auf einer Testarena prüfen.

Diese Integration steuert die ausgewählte lokale Arena über die öffentliche API. Sie ändert nicht automatisch alle MBedwars-Serverkonfigurationen oder globale Shop-/Upgrade-/Spawner-Einstellungen. Solche Optionen hängen von den konkreten Konfigurations- und Speichermöglichkeiten der installierten MBedwars-Version ab.

## MBedwars direkt verwalten

Wenn MBedwars installiert ist, stellt Turnier zusätzlich `/mbwctl` bereit. Der Befehl nutzt die offizielle MBedwars-API; eine lokale `Mbedwars.jar` muss nicht ins Repository gelegt werden.

| Befehl | Wirkung |
|---|---|
| `/mbwctl status` | Zeigt API-Version, geladene Arenen und Turnierstatus |
| `/mbwctl arenas` | Listet geladene Arenen mit Status und Spielerzahl |
| `/mbwctl arena <Name>` | Zeigt Status, Spieler, Zuschauer und aktivierte MBedwars-Teams |
| `/mbwctl team <Arena> <Farbe> <on|off>` | Aktiviert/deaktiviert ein MBedwars-Team; nur bei gestoppter Arena |
| `/mbwctl team <Arena> <Farbe> <on|off>` | Aktiviert/deaktiviert ein MBedwars-Farbteam; nur bei gestoppter Arena |
| `/mbwctl start <Arena>` | Fordert den Start einer Arena aus dem Lobby-Status an |
| `/mbwctl stop <Arena>` | Beendet das Match in einer Arena |
| `/mbwctl join <Arena> [Spieler]` | Fügt einen Online-Spieler einer Arena-Lobby hinzu; ohne Spielernamen muss der Ausführende ein Spieler sein |
| `/mbwctl kick <Spieler>` | Entfernt einen Spieler aus seiner lokalen MBedwars-Arena |
| `/mbwctl reload` | Zeigt den Sicherheitshinweis zum MBedwars-Reload |
| `/mbwctl reload confirm` | Bestätigt den MBedwars-Reload. **Achtung:** MBedwars beendet laufende Arenen und wirft deren Spieler heraus. |

Alle `/mbwctl`-Befehle erfordern `turnier.admin`. Die Aktionen sind auf die Funktionen beschränkt, die die offizielle MBedwars-API anbietet. Die Arena muss passend konfiguriert und geladen sein; der API-Start kann bei fehlenden Spielern oder ungültigem Arena-Zustand fehlschlagen.

> Hinweis: Die Turnier-Rundensteuerung (`/bwturnier`) ist weiterhin vom eigentlichen Arena-Zustand getrennt. Starte/beende daher sowohl den Turnierabschnitt als auch die gewünschte MBedwars-Arena passend zum Ablauf. Die automatische Wertung bezieht sich ausschließlich auf die hier ausgewählte Turnierarena. Turnierteams müssen explizit mit MBedwars-Farbteams verknüpft sein; das Plugin importiert oder synchronisiert keine Teamdefinitionen automatisch.
