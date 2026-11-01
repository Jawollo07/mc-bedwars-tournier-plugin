package de.turnier;

import de.turnier.TournamentManager.PlayerData;
import de.turnier.TournamentManager.TeamData;
import de.marcely.bedwars.api.BedwarsAPI;
import de.marcely.bedwars.api.arena.Arena;
import de.marcely.bedwars.api.arena.ArenaStatus;
import de.marcely.bedwars.api.arena.AddPlayerCause;
import de.marcely.bedwars.api.arena.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BwTurnierCommand implements TabExecutor {

    private final TurnierPlugin plugin;
    private final TournamentManager m;

    public BwTurnierCommand(TurnierPlugin plugin, TournamentManager manager) {
        this.plugin = plugin;
        this.m = manager;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (!s.hasPermission(TurnierPlugin.ADMIN_PERMISSION)) {
            plugin.msg(s, "&cDu hast keine Berechtigung.");
            return true;
        }
        String sub = a.length > 0 ? a[0].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "on" -> {
                m.setAutoScoring(true);
                plugin.msg(s, "&aAutomatische Wertung aktiviert.");
            }
            case "off" -> {
                m.setAutoScoring(false);
                plugin.msg(s, "&cAutomatische Wertung deaktiviert.");
            }
            case "debug" -> {
                if (a.length < 2 || !(a[1].equalsIgnoreCase("on") || a[1].equalsIgnoreCase("off"))) {
                    plugin.msg(s, "&eDebug-Modus: &6/bwturnier debug <on|off>");
                    return true;
                }
                boolean enabled = a[1].equalsIgnoreCase("on");
                plugin.getConfig().set("debug", enabled);
                plugin.saveConfig();
                plugin.msg(s, enabled ? "&aDebug-Logging aktiviert." : "&eDebug-Logging deaktiviert.");
            }
            case "status" -> {
                plugin.msg(s, "&7Turnier aktiv: &e" + m.isActive());
                plugin.msg(s, "&7Automatische Wertung: &e" + m.isAutoScoring());
                plugin.msg(s, "&7Runde: &e" + m.getRound() + "/" + m.getTotalRounds());
                plugin.msg(s, "&7Arena: &e" + (m.getArenaName() == null ? "nicht gesetzt" : m.getArenaName()));
                plugin.msg(s, "&7Status: &e" + (!m.isActive() ? "nicht gestartet" : m.isRoundActive() ? "Runde läuft" : "Pause zwischen Runden"));
            }
            case "start" -> start(s, a);
            case "next", "round" -> startNextRound(s);
            case "pause", "roundend" -> pause(s);
            case "stats" -> {
                if (a.length < 2) { plugin.msg(s, "&c/bwturnier stats <Team>"); return true; }
                TeamData t = m.findTeam(a[1]);
                if (t == null) { plugin.msg(s, "&cTeam nicht gefunden."); return true; }
                int kills = 0, beds = 0;
                for (PlayerData p : m.membersOf(t)) { kills += p.kills; beds += p.beds; }
                TurnierPlugin.raw(s, "&8&m---------- &6Statistik " + t.name + " &8&m----------");
                TurnierPlugin.raw(s, "&7Punkte: &e" + t.points);
                TurnierPlugin.raw(s, "&7Siege: &a" + t.wins);
                TurnierPlugin.raw(s, "&7Kills: &c" + kills);
                TurnierPlugin.raw(s, "&7Bettzerstörungen: &d" + beds);
                TurnierPlugin.raw(s, "&7Vom Team zerstörte Betten: &d" + t.bedsDestroyed);
            }
            case "announce" -> {
                TurnierPlugin.broadcastRaw("&8&m---------- &6BEDWARS-TURNIER &8&m----------");
                for (TeamData t : m.sortedTeams()) {
                    TurnierPlugin.broadcastRaw("&e" + t.name + " &8- &6" + t.points + " Punkte");
                }
                TurnierPlugin.broadcastRaw("&8&m---------------------------------------");
            }
            case "win" -> {
                if (a.length < 2) { plugin.msg(s, "&c/bwturnier win <Team>"); return true; }
                TeamData t = m.findTeam(a[1]);
                if (t == null) { plugin.msg(s, "&cTeam nicht gefunden."); return true; }
                int points = Math.max(0, plugin.getConfig().getInt("points.first", 10));
                t.points = TournamentManager.saturatedAdd(t.points, points);
                if (t.wins < Integer.MAX_VALUE) t.wins++;
                plugin.broadcastPrefixed("&aManueller Sieg für Team &e" + t.name + "&a.");
            }
            case "finish", "ceremony", "siegerehrung" -> finish(s);
            default -> {
                plugin.msg(s, "&6/bwturnier start [Runden]|next|pause|ceremony|announce|stats <Team>|win <Team>|on|off|status");
                return true;
            }
        }
        m.save();
        return true;
    }

    private void start(CommandSender s, String[] args) {
        if (m.isActive()) { plugin.msg(s, "&cDas Turnier läuft bereits."); return; }
        int rounds = 3;
        if (args.length >= 2) {
            try { rounds = Integer.parseInt(args[1]); }
            catch (NumberFormatException ex) { plugin.msg(s, "&cVerwendung: /bwturnier start [AnzahlRunden]"); return; }
        }
        if (rounds < 1 || rounds > 100) { plugin.msg(s, "&cDie Rundenzahl muss zwischen 1 und 100 liegen."); return; }
        if (m.teams().size() < 2) { plugin.msg(s, "&cFür ein BedWars-Turnier müssen mindestens zwei Turnierteams existieren."); return; }
        m.setActive(true);
        m.setRound(0);
        m.setTotalRounds(rounds);
        m.setRoundActive(false);
        m.setChampion(null);
        m.save();
        plugin.broadcastPrefixed("&aTurnier vorbereitet: &e" + rounds + " Runden&a. Nutze &e/bwturnier next &azum Start.");
    }

    private void startNextRound(CommandSender s) {
        if (!m.isActive()) { plugin.msg(s, "&cStarte zuerst /bwturnier start [Runden]."); return; }
        if (m.isRoundActive()) { plugin.msg(s, "&cEine Runde läuft bereits."); return; }
        if (m.getRound() >= m.getTotalRounds()) {
            plugin.msg(s, "&eAlle Runden sind gespielt. Nutze /bwturnier ceremony."); return;
        }
        if (Bukkit.getPluginManager().getPlugin("MBedwars") == null) {
            plugin.msg(s, "&cMBedwars ist nicht geladen."); return;
        }
        if (m.getArenaName() == null) { plugin.msg(s, "&cSetze zuerst /turnier arena <Arena>."); return; }

        final Arena arena;
        try {
            arena = BedwarsAPI.getGameAPI().getArenaByExactName(m.getArenaName());
        } catch (RuntimeException ex) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "[Turnier] MBedwars-Arena konnte nicht abgefragt werden.", ex);
            plugin.msg(s, "&cFehler beim Abrufen der MBedwars-Arena. Details stehen in der Konsole.");
            return;
        }
        if (arena == null) { plugin.msg(s, "&cArena nicht gefunden: &e" + m.getArenaName()); return; }
        if (arena.getStatus() != ArenaStatus.LOBBY) {
            plugin.msg(s, "&cDie Arena muss in der Lobby sein. Status: &e" + arena.getStatus()); return;
        }

        debugLog("Starte Vorbereitung: arena=" + arena.getName() + ", tournamentRound="
                + (m.getRound() + 1) + "/" + m.getTotalRounds());

        Map<String, Integer> onlineByTeam = new java.util.LinkedHashMap<>();
        Map<String, Integer> joinedByTeam = new java.util.LinkedHashMap<>();
        Map<String, String> mappedNames = new java.util.HashMap<>();
        List<String> failures = new ArrayList<>();
        int assigned = 0;
        int offline = 0;
        int skippedUnassigned = 0;

        for (PlayerData data : m.allPlayers()) {
            if (data.team == null || data.team.isBlank()) {
                skippedUnassigned++;
                continue;
            }

            TeamData tournamentTeam = m.findTeam(data.team);
            if (tournamentTeam == null) {
                failures.add(data.name + " [" + data.id + "]: Turnierteam '" + data.team + "' existiert nicht mehr.");
                continue;
            }

            Player player = Bukkit.getPlayer(data.id);
            if (player == null) {
                offline++;
                debugLog("Spieler offline/UUID nicht online: name=" + data.name + ", uuid=" + data.id
                        + ", tournamentTeam=" + tournamentTeam.name);
                continue;
            }

            String teamKey = tournamentTeam.name.toLowerCase(Locale.ROOT);
            onlineByTeam.merge(teamKey, 1, Integer::sum);

            if (tournamentTeam.bedwarsTeam == null || tournamentTeam.bedwarsTeam.isBlank()) {
                failures.add(player.getName() + " [" + data.id + "]: Für Turnierteam '"
                        + tournamentTeam.name + "' ist keine MBedwars-Teamfarbe konfiguriert.");
                continue;
            }

            Team bedwarsTeam = Team.getByName(tournamentTeam.bedwarsTeam);
            if (bedwarsTeam == null) {
                failures.add(player.getName() + " [" + data.id + "]: MBedwars-Teamfarbe '"
                        + tournamentTeam.bedwarsTeam + "' ist ungültig.");
                continue;
            }
            if (!arena.isTeamEnabled(bedwarsTeam)) {
                failures.add(player.getName() + " [" + data.id + "]: MBedwars-Team '"
                        + bedwarsTeam.getConfigName() + "' ist in Arena '" + arena.getName() + "' deaktiviert.");
                continue;
            }

            String mappedKey = bedwarsTeam.getConfigName().toLowerCase(Locale.ROOT);
            String previousTeam = mappedNames.putIfAbsent(mappedKey, tournamentTeam.name);
            if (previousTeam != null && !previousTeam.equalsIgnoreCase(tournamentTeam.name)) {
                failures.add("Mapping-Konflikt: Turnierteams '" + previousTeam + "' und '"
                        + tournamentTeam.name + "' verwenden beide MBedwars-Team '" + bedwarsTeam.getConfigName() + "'.");
                continue;
            }

            try {
                Arena currentArena = BedwarsAPI.getGameAPI().getArenaByPlayer(player);
                if (currentArena == arena) {
                    if (arena.moveToTeamDuringLobby(player, bedwarsTeam)) {
                        assigned++;
                        joinedByTeam.merge(teamKey, 1, Integer::sum);
                        debugLog("Spieler umgeteilt: " + player.getName() + " [" + data.id + "] -> "
                                + tournamentTeam.name + " / " + bedwarsTeam.getConfigName());
                    } else {
                        failures.add(player.getName() + " [" + data.id + "]: MBedwars konnte den Spieler in der Lobby nicht in Team '"
                                + bedwarsTeam.getConfigName() + "' verschieben.");
                    }
                } else if (currentArena != null) {
                    failures.add(player.getName() + " [" + data.id + "]: Spieler befindet sich bereits in einer anderen MBedwars-Arena '"
                            + currentArena.getName() + "'.");
                } else {
                    var issue = arena.addPlayer(player, bedwarsTeam, AddPlayerCause.PLUGIN);
                    if (issue == null) {
                        assigned++;
                        joinedByTeam.merge(teamKey, 1, Integer::sum);
                        debugLog("Spieler hinzugefügt: " + player.getName() + " [" + data.id + "] -> "
                                + tournamentTeam.name + " / " + bedwarsTeam.getConfigName());
                    } else {
                        failures.add(player.getName() + " [" + data.id + "]: MBedwars addPlayer lehnte den Beitritt ab: "
                                + issue + ".");
                    }
                }
            } catch (RuntimeException ex) {
                failures.add(player.getName() + " [" + data.id + "]: Ausnahme beim MBedwars-Beitritt: "
                        + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "[Turnier] Fehler beim Zuordnen von " + player.getName() + " [" + data.id + "]", ex);
            }
        }

        List<String> teamsWithoutPlayers = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : onlineByTeam.entrySet()) {
            if (joinedByTeam.getOrDefault(entry.getKey(), 0) == 0) {
                teamsWithoutPlayers.add(entry.getKey());
            }
        }
        long participatingTeams = joinedByTeam.values().stream().filter(count -> count > 0).count();

        debugLog("Zuordnungsübersicht: erfolgreich=" + assigned + ", offline=" + offline
                + ", ohneTurnierteam=" + skippedUnassigned + ", onlineTeams=" + onlineByTeam.size()
                + ", erfolgreicheTeams=" + participatingTeams + ", fehler=" + failures.size());

        for (String failure : failures) {
            plugin.getLogger().warning("[Turnier] Zuordnungsproblem: " + failure);
        }

        if (assigned == 0 || participatingTeams < 2 || !teamsWithoutPlayers.isEmpty()) {
            plugin.getLogger().warning("[Turnier] Rundenstart abgebrochen: Es müssen mindestens zwei Turnierteams"
                    + " Spieler erfolgreich in die Arena bringen; jedes Team mit Online-Spielern braucht mindestens"
                    + " einen erfolgreichen Beitritt. Erfolgreich=" + assigned + ", Teams=" + participatingTeams
                    + ", Teams ohne Spieler=" + teamsWithoutPlayers);
            plugin.msg(s, "&cRunde nicht gestartet. Erfolgreich zugeordnet: &e" + assigned
                    + " &7| Teams: &e" + participatingTeams + " &7| Offline: &e" + offline);
            plugin.msg(s, "&7Details und konkrete Fehlerursachen stehen in der Serverkonsole.");
            return;
        }

        if (!failures.isEmpty() || offline > 0 || skippedUnassigned > 0) {
            plugin.msg(s, "&eHinweis: " + failures.size() + " Zuordnungsproblem(e), " + offline
                    + " offline. Die Runde startet mit den erfolgreich zugeordneten Spielern.");
        }

        try {
            arena.setStatus(ArenaStatus.RUNNING);
        } catch (RuntimeException ex) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "[Turnier] MBedwars konnte Arena '" + arena.getName() + "' nicht starten.", ex);
            plugin.msg(s, "&cMBedwars konnte die Runde nicht starten. Details stehen in der Konsole.");
            return;
        }

        m.beginRoundScoring();
        m.setRound(m.getRound() + 1);
        m.setRoundActive(true);
        m.save();
        debugLog("Runde erfolgreich gestartet: round=" + m.getRound() + ", players=" + assigned);
        plugin.broadcastPrefixed("&6Runde " + m.getRound() + "/" + m.getTotalRounds()
                + " &awurde gestartet! &7Arena: &e" + arena.getName() + " &8| &7Spieler: &e" + assigned);
    }

    private void debugLog(String message) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Turnier-Debug] " + message);
        }
    }

    private void pause(CommandSender s) {
        if (!m.isActive()) { plugin.msg(s, "&cEs läuft kein Turnier."); return; }
        if (m.isRoundActive()) {
            if (Bukkit.getPluginManager().getPlugin("MBedwars") == null || m.getArenaName() == null) {
                plugin.msg(s, "&cDer Status der laufenden MBedwars-Runde kann nicht sicher geprüft werden. Die Runde bleibt aktiv.");
                return;
            }
            final Arena arena;
            try {
                arena = BedwarsAPI.getGameAPI().getArenaByExactName(m.getArenaName());
            } catch (RuntimeException ex) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "[Turnier] Arena-Status konnte vor der Pause nicht geprüft werden.", ex);
                plugin.msg(s, "&cArena-Status konnte nicht geprüft werden. Die Runde bleibt aktiv.");
                return;
            }
            if (arena == null) {
                plugin.msg(s, "&cDie konfigurierte Turnierarena wurde nicht gefunden. Die Runde bleibt aktiv.");
                return;
            }
            if (arena.getStatus() == ArenaStatus.RUNNING) {
                plugin.msg(s, "&cDie Runde läuft noch. Beende zuerst das MBedwars-Match.");
                return;
            }
            m.setRoundActive(false);
            m.save();
        }
        plugin.broadcastPrefixed("&eTurnierpause nach Runde &6" + m.getRound() + "&e. Wertung und Statistiken bleiben erhalten.");
        announce();
    }

    private void announce() {
        TurnierPlugin.broadcastRaw("&8&m---------- &6ZWISCHENSTAND &8&m----------");
        int rank = 0;
        for (TeamData team : m.sortedTeams()) {
            rank++;
            TurnierPlugin.broadcastRaw("&e" + rank + ". &6" + team.name + " &8- &f" + team.points + " Punkte &7(" + team.wins + " Siege)");
        }
        TurnierPlugin.broadcastRaw("&8&m---------------------------------------");
    }

    private void finish(CommandSender s) {
        if (!m.isActive()) {
            plugin.msg(s, "&cEs läuft kein Turnier.");
            return;
        }
        if (m.isRoundActive()) {
            plugin.msg(s, "&cDie Runde läuft noch. Beende zuerst das Match und pausiere danach.");
            return;
        }
        m.setActive(false);
        m.setRoundActive(false);

        List<TeamData> sorted = m.sortedTeams();
        if (sorted.isEmpty()) {
            plugin.broadcastPrefixed("&cTurnier beendet, aber es gibt keine Teams.");
            return;
        }
        TeamData winner = sorted.get(0);
        int highest = winner.points;
        long ties = sorted.stream().filter(t -> t.points == highest).count();

        if (ties > 1) {
            m.setChampion(null);
            TurnierPlugin.broadcastRaw("&8&m---------- &6&lSIEGEREHRUNG &8&m----------");
            TurnierPlugin.broadcastRaw("&eGleichstand auf Platz 1 mit &6" + highest + " Punkten:");
            for (TeamData team : sorted) {
                if (team.points != highest) break;
                TurnierPlugin.broadcastRaw("&6&lPlatz 1: &e" + team.name);
            }
            TurnierPlugin.broadcastRaw("&7Ein eindeutiger Champion kann nicht automatisch festgelegt werden.");
            TurnierPlugin.broadcastRaw("&8&m---------------------------------------");
            m.save();
            return;
        }
        m.setChampion(winner.name);
        TurnierPlugin.broadcastRaw("&8&m---------- &6&lSIEGEREHRUNG &8&m----------");
        TurnierPlugin.broadcastRaw("&6&l1. Platz: &e" + winner.name + " &7- &f" + highest + " Punkte");
        int podium = 1;
        for (int i = 1; i < sorted.size() && podium < 3; i++) {
            TeamData team = sorted.get(i);
            if (ties > 1 && team.points == highest) continue;
            podium++;
            String medal = podium == 2 ? "&7&l2. Platz: " : "&c&l3. Platz: ";
            TurnierPlugin.broadcastRaw(medal + "&e" + team.name + " &7- &f" + team.points + " Punkte");
        }
        TurnierPlugin.broadcastRaw("&8&m---------------------------------------");
        plugin.broadcastPrefixed("&aDas Turnier ist beendet. Glückwunsch an alle Teams!");
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 1) {
            return TurnierPlugin.complete(List.of("start", "next", "round", "pause", "roundend", "finish",
                    "ceremony", "siegerehrung", "announce", "stats", "win", "on", "off", "debug", "status"), a[0]);
        }
        if (a.length == 2 && a[0].equalsIgnoreCase("start")) {
            return TurnierPlugin.complete(List.of("1", "2", "3", "4", "5"), a[1]);
        }
        if (a.length == 2 && (a[0].equalsIgnoreCase("stats") || a[0].equalsIgnoreCase("win"))) {
            List<String> names = new ArrayList<>();
            for (TeamData t : m.teams()) names.add(t.name);
            return TurnierPlugin.complete(names, a[1]);
        }
        return List.of();
    }
}
