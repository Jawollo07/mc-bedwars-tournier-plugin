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
            case "status" -> {
                plugin.msg(s, "&7Turnier aktiv: &e" + m.isActive());
                plugin.msg(s, "&7Automatische Wertung: &e" + m.isAutoScoring());
                plugin.msg(s, "&7Runde: &e" + m.getRound());
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
                int points = Math.max(0, plugin.getConfig().getInt("points.win", 10));
                t.points = TournamentManager.saturatedAdd(t.points, points);
                if (t.wins < Integer.MAX_VALUE) t.wins++;
                plugin.broadcastPrefixed("&aManueller Sieg für Team &e" + t.name + "&a.");
            }
            case "finish", "ceremony", "siegerehrung" -> finish(s);
            default -> {
                plugin.msg(s, "&6/bwturnier start|round|roundend|finish|announce|stats <Team>|win <Team>|on|off|status");
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
        if (m.teams().isEmpty()) { plugin.msg(s, "&cErstelle zuerst mindestens ein Turnierteam."); return; }
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
        Arena arena = BedwarsAPI.getGameAPI().getArenaByExactName(m.getArenaName());
        if (arena == null) { plugin.msg(s, "&cArena nicht gefunden: &e" + m.getArenaName()); return; }
        if (arena.getStatus() != ArenaStatus.LOBBY) {
            plugin.msg(s, "&cDie Arena muss in der Lobby sein. Status: &e" + arena.getStatus()); return;
        }

        int assigned = 0;
        int failed = 0;
        for (PlayerData data : m.allPlayers()) {
            if (data.team == null) continue;
            Player player = Bukkit.getPlayer(data.id);
            if (player == null) { failed++; continue; }
            TeamData tournamentTeam = m.findTeam(data.team);
            Team bedwarsTeam = tournamentTeam == null || tournamentTeam.bedwarsTeam == null
                    ? null : Team.getByName(tournamentTeam.bedwarsTeam);
            if (bedwarsTeam == null || !arena.isTeamEnabled(bedwarsTeam)) { failed++; continue; }
            if (BedwarsAPI.getGameAPI().getArenaByPlayer(player) == arena) {
                if (arena.moveToTeamDuringLobby(player, bedwarsTeam)) assigned++; else failed++;
            } else {
                var issue = arena.addPlayer(player, bedwarsTeam, AddPlayerCause.PLUGIN);
                if (issue == null) assigned++; else failed++;
            }
        }
        if (assigned == 0 || failed > 0) {
            plugin.msg(s, "&cRunde nicht gestartet. Zugeordnet: &e" + assigned + " &8| &cFehler: &e" + failed);
            plugin.msg(s, "&7Prüfe Online-Status, Teamfarben und freie Arena-Plätze."); return;
        }
        try {
            arena.setStatus(ArenaStatus.RUNNING);
        } catch (IllegalStateException ex) {
            plugin.msg(s, "&cMBedwars konnte die Runde nicht starten: &f" + ex.getMessage()); return;
        }
        m.setRound(m.getRound() + 1);
        m.setRoundActive(true);
        m.save();
        plugin.broadcastPrefixed("&6Runde " + m.getRound() + "/" + m.getTotalRounds()
                + " &awurde gestartet! &7Arena: &e" + arena.getName() + " &8| &7Spieler: &e" + assigned);
    }

    private void pause(CommandSender s) {
        if (!m.isActive()) { plugin.msg(s, "&cEs läuft kein Turnier."); return; }
        if (m.isRoundActive()) {
            if (Bukkit.getPluginManager().getPlugin("MBedwars") != null && m.getArenaName() != null) {
                Arena arena = BedwarsAPI.getGameAPI().getArenaByExactName(m.getArenaName());
                if (arena != null && arena.getStatus() == ArenaStatus.RUNNING) {
                    plugin.msg(s, "&cDie Runde läuft noch. Beende zuerst das MBedwars-Match."); return;
                }
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
            plugin.broadcastPrefixed("&eDas Turnier ist beendet. Es gibt einen Gleichstand bei " + highest + " Punkten.");
            plugin.broadcastPrefixed("&7Bitte entscheide den Sieger manuell.");
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
                    "ceremony", "siegerehrung", "announce", "stats", "win", "on", "off", "status"), a[0]);
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
