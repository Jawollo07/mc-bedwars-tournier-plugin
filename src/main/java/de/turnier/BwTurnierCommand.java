package de.turnier;

import de.turnier.TournamentManager.PlayerData;
import de.turnier.TournamentManager.TeamData;
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
            case "start" -> {
                if (m.isActive()) { plugin.msg(s, "&cDas Turnier läuft bereits."); return true; }
                m.setActive(true);
                m.setRound(0);
                m.setRoundActive(false);
                m.setChampion(null);
                plugin.broadcastPrefixed("&aDas BedWars-Turnier wurde gestartet!");
            }
            case "round" -> {
                if (!m.isActive()) { plugin.msg(s, "&cStarte zuerst das Turnier."); return true; }
                if (m.isRoundActive()) { plugin.msg(s, "&cDie aktuelle Runde läuft bereits."); return true; }
                m.setRound(m.getRound() + 1);
                m.setRoundActive(true);
                plugin.broadcastPrefixed("&6Turnierrunde " + m.getRound() + " &awurde gestartet!");
            }
            case "roundend" -> {
                if (!m.isRoundActive()) { plugin.msg(s, "&cEs läuft keine Turnierrunde."); return true; }
                m.setRoundActive(false);
                plugin.broadcastPrefixed("&eTurnierrunde " + m.getRound() + " &7wurde beendet.");
            }
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
            case "finish" -> finish(s);
            default -> {
                plugin.msg(s, "&6/bwturnier start|round|roundend|finish|announce|stats <Team>|win <Team>|on|off|status");
                return true;
            }
        }
        m.save();
        return true;
    }

    private void finish(CommandSender s) {
        if (!m.isActive()) {
            plugin.msg(s, "&cEs läuft kein Turnier.");
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
        TurnierPlugin.broadcastRaw("&8&m---------- &6&lTURNIER BEENDET &8&m----------");
        TurnierPlugin.broadcastRaw("&eSieger: &6&l" + winner.name);
        TurnierPlugin.broadcastRaw("&ePunkte: &6" + highest);
        TurnierPlugin.broadcastRaw("&8&m---------------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 1) {
            return TurnierPlugin.complete(List.of("start", "round", "roundend", "finish", "announce",
                    "stats", "win", "on", "off", "status"), a[0]);
        }
        if (a.length == 2 && (a[0].equalsIgnoreCase("stats") || a[0].equalsIgnoreCase("win"))) {
            List<String> names = new ArrayList<>();
            for (TeamData t : m.teams()) names.add(t.name);
            return TurnierPlugin.complete(names, a[1]);
        }
        return List.of();
    }
}
