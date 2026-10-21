package de.turnier;

import de.turnier.TournamentManager.PlayerData;
import de.turnier.TournamentManager.TeamData;
import de.marcely.bedwars.api.BedwarsAPI;
import de.marcely.bedwars.api.arena.Arena;
import de.marcely.bedwars.api.arena.ArenaStatus;
import de.marcely.bedwars.api.arena.AddPlayerCause;
import de.marcely.bedwars.api.arena.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class TurnierCommand implements TabExecutor {

    private final TurnierPlugin plugin;
    private final TournamentManager m;

    public TurnierCommand(TurnierPlugin plugin, TournamentManager manager) {
        this.plugin = plugin;
        this.m = manager;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0) {
            help(s);
            return true;
        }
        if (!s.hasPermission(TurnierPlugin.ADMIN_PERMISSION)) {
            plugin.msg(s, "&cDu hast keine Berechtigung.");
            return true;
        }

        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "sudo" -> sudo(s, a);
            case "team" -> team(s, a);
            case "arena" -> arena(s, a);
            case "match" -> match(s, a);
            case "spieler" -> spieler(s, a);
            case "add", "remove", "set" -> points(s, a);
            case "reset" -> reset(s, a);
            case "resetall" -> {
                m.resetAllPoints();
                plugin.msg(s, "&aAlle Spieler- und Teampunkte wurden zurückgesetzt.");
            }
            case "top" -> top(s);
            default -> plugin.msg(s, "&cUnbekannter Befehl. Nutze /turnier für Hilfe.");
        }
        m.save();
        return true;
    }

    private void help(CommandSender s) {
        plugin.msg(s, "&eTurnierverwaltung – Hilfe");
        for (String line : List.of(
                "/turnier team create <Name>",
                "/turnier team delete <Name>",
                "/turnier team rename <Alt> <Neu>",
                "/turnier team list",
                "/turnier team bind <Turnierteam> <MBedwars-Farbe>",
                "/turnier arena <MBedwars-Arena>",
                "/turnier match prepare|start|stop",
                "/turnier team info <Name>",
                "/turnier spieler assign <Spieler> <Team>",
                "/turnier spieler remove <Spieler>",
                "/turnier spieler info <Spieler>",
                "/turnier add <spieler|team> <Name> <Punkte>",
                "/turnier remove <spieler|team> <Name> <Punkte>",
                "/turnier set <spieler|team> <Name> <Punkte>",
                "/turnier reset <spieler|team> <Name>",
                "/turnier top",
                "/turnier resetall",
                "/bwturnier start|round|roundend|finish|announce|stats|win|on|off|status",
                "/turnierboard <on|off|status>",
                "/turnier sudo <OnlineSpieler> <Befehl>")) {
            TurnierPlugin.raw(s, "&6" + line);
        }
    }

    // ------------------------------------------------------------------ sudo

    private void sudo(CommandSender s, String[] a) {
        if (a.length < 2) {
            plugin.msg(s, "&c/turnier sudo <OnlineSpieler> <Befehl>");
            return;
        }
        if (a.length < 3) {
            plugin.msg(s, "&cBitte gib einen Befehl an.");
            return;
        }
        Player target = Bukkit.getPlayerExact(a[1]);
        if (target == null) {
            plugin.msg(s, "&cDer Zielspieler muss online sein.");
            return;
        }
        String cmd = String.join(" ", Arrays.copyOfRange(a, 2, a.length));
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        target.performCommand(cmd);
        plugin.msg(s, "&aBefehl &e/" + cmd + " &awurde als &e" + target.getName() + " &aausgeführt.");
    }

    // ----------------------------------------------------------------- Teams

    private void team(CommandSender s, String[] a) {
        String sub = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "bind" -> {
                if (a.length < 4) { plugin.msg(s, "&c/turnier team bind <Turnierteam> <MBedwars-Farbe>"); return; }
                TeamData t = m.findTeam(a[2]);
                if (t == null) { plugin.msg(s, "&cTurnierteam nicht gefunden."); return; }
                Team bedwarsTeam = Team.getByName(a[3]);
                if (bedwarsTeam == null) { plugin.msg(s, "&cUnbekannte MBedwars-Teamfarbe."); return; }
                t.bedwarsTeam = bedwarsTeam.getConfigName();
                plugin.msg(s, "&aTurnierteam &e" + t.name + " &aist jetzt mit MBedwars-Team &e" + t.bedwarsTeam + " &averknüpft.");
            }
            case "create" -> {
                if (a.length < 3) { plugin.msg(s, "&c/turnier team create <Name>"); return; }
                if (m.createTeam(a[2]) == null) {
                    plugin.msg(s, "&cDieses Team existiert bereits.");
                    return;
                }
                plugin.msg(s, "&aTeam &e" + a[2] + " &aerstellt.");
            }
            case "delete" -> {
                if (a.length < 3) { plugin.msg(s, "&c/turnier team delete <Name>"); return; }
                TeamData t = m.findTeam(a[2]);
                if (t == null) { plugin.msg(s, "&cTeam nicht gefunden."); return; }
                m.deleteTeam(t);
                plugin.msg(s, "&aTeam &e" + t.name + " &awurde gelöscht. Mitglieder bleiben registriert, sind aber keinem Team zugeordnet.");
            }
            case "rename" -> {
                if (a.length < 4) { plugin.msg(s, "&c/turnier team rename <Alt> <Neu>"); return; }
                TeamData t = m.findTeam(a[2]);
                if (t == null) { plugin.msg(s, "&cAltes Team nicht gefunden."); return; }
                String old = t.name;
                if (!m.renameTeam(t, a[3])) {
                    plugin.msg(s, "&cDer neue Teamname existiert bereits.");
                    return;
                }
                plugin.msg(s, "&aTeam &e" + old + " &awurde in &e" + a[3] + " &aumbenannt.");
            }
            case "list" -> {
                TurnierPlugin.raw(s, "&8&m---------- &6Turnierteams &8&m----------");
                if (m.teams().isEmpty()) {
                    TurnierPlugin.raw(s, "&7Noch keine Teams angelegt.");
                    return;
                }
                for (TeamData t : m.teams()) {
                    TurnierPlugin.raw(s, "&e" + t.name + " &8- &6" + t.points + " Punkte");
                }
            }
            case "info" -> {
                if (a.length < 3) { plugin.msg(s, "&c/turnier team info <Name>"); return; }
                TeamData t = m.findTeam(a[2]);
                if (t == null) { plugin.msg(s, "&cTeam nicht gefunden."); return; }
                List<PlayerData> members = m.membersOf(t);
                int kills = 0, beds = 0;
                for (PlayerData p : members) { kills += p.kills; beds += p.beds; }
                TurnierPlugin.raw(s, "&8&m---------- &6Team " + t.name + " &8&m----------");
                TurnierPlugin.raw(s, "&7Punkte: &e" + t.points);
                TurnierPlugin.raw(s, "&7Siege: &a" + t.wins);
                TurnierPlugin.raw(s, "&7Kills: &c" + kills);
                TurnierPlugin.raw(s, "&7Bettzerstörungen: &d" + beds);
                TurnierPlugin.raw(s, "&7Mitglieder: &f" + members.size());
                for (PlayerData p : members) TurnierPlugin.raw(s, "&8- &f" + p.name);
            }
            default -> plugin.msg(s, "&cUnbekannte Teamaktion.");
        }
    }


    private void arena(CommandSender s, String[] a) {
        if (a.length < 2) { plugin.msg(s, "&c/turnier arena <MBedwars-Arena>"); return; }
        if (Bukkit.getPluginManager().getPlugin("MBedwars") == null) { plugin.msg(s, "&cMBedwars ist nicht geladen."); return; }
        Arena arena = BedwarsAPI.getGameAPI().getArenaByExactName(a[1]);
        if (arena == null) { plugin.msg(s, "&cMBedwars-Arena nicht gefunden."); return; }
        m.setArenaName(arena.getName());
        plugin.msg(s, "&aTurnierarena gesetzt: &e" + arena.getName());
    }

    private void match(CommandSender s, String[] a) {
        String sub = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "";
        if (Bukkit.getPluginManager().getPlugin("MBedwars") == null) { plugin.msg(s, "&cMBedwars ist nicht geladen."); return; }
        if (m.getArenaName() == null) { plugin.msg(s, "&cSetze zuerst /turnier arena <Name>."); return; }
        Arena arena = BedwarsAPI.getGameAPI().getArenaByExactName(m.getArenaName());
        if (arena == null) { plugin.msg(s, "&cGespeicherte MBedwars-Arena nicht gefunden: &e" + m.getArenaName()); return; }
        switch (sub) {
            case "prepare" -> {
                if (arena.getStatus() != ArenaStatus.LOBBY) { plugin.msg(s, "&cArena muss in der Lobby sein."); return; }
                int added = 0, failed = 0;
                for (PlayerData data : m.allPlayers()) {
                    // Nicht zugeordnete registrierte Spieler gehören nicht zur Turnieraufstellung.
                    if (data.team == null) continue;
                    Player p = Bukkit.getPlayer(data.id);
                    if (p == null) { failed++; continue; }
                    TeamData tournamentTeam = m.findTeam(data.team);
                    Team bedwarsTeam = tournamentTeam == null || tournamentTeam.bedwarsTeam == null ? null : Team.getByName(tournamentTeam.bedwarsTeam);
                    if (tournamentTeam == null || bedwarsTeam == null || !arena.isTeamEnabled(bedwarsTeam)) { failed++; continue; }
                    if (BedwarsAPI.getGameAPI().getArenaByPlayer(p) == arena) {
                        if (arena.moveToTeamDuringLobby(p, bedwarsTeam)) added++; else failed++;
                    } else {
                        var issue = arena.addPlayer(p, bedwarsTeam, AddPlayerCause.PLUGIN);
                        if (issue == null) added++; else failed++;
                    }
                }
                plugin.msg(s, "&aTurnieraufstellung verarbeitet. &eZugeordnet: " + added + " &8| &cNicht zugeordnet: " + failed);
                if (failed > 0) plugin.msg(s, "&7Prüfe Online-Status, Teamzuordnung, Teamfarbe und freie Plätze.");
            }
            case "start" -> {
                if (!m.isActive()) { plugin.msg(s, "&cStarte zuerst das Turnier mit /bwturnier start."); return; }
                if (arena.getStatus() != ArenaStatus.LOBBY) { plugin.msg(s, "&cArena muss in der Lobby sein."); return; }
                try {
                    arena.setStatus(ArenaStatus.RUNNING);
                } catch (IllegalStateException ex) {
                    plugin.msg(s, "&cMBedwars konnte die Runde nicht starten: &f" + ex.getMessage());
                    return;
                }
                if (!m.isRoundActive()) { m.setRound(m.getRound() + 1); m.setRoundActive(true); }
                plugin.broadcastPrefixed("&6Turnierrunde " + m.getRound() + " &awurde in Arena &e" + arena.getName() + " &agestartet.");
            }
            case "stop" -> {
                if (!arena.endMatch(null)) { plugin.msg(s, "&cMBedwars konnte das Match nicht beenden."); return; }
                m.setRoundActive(false);
                plugin.broadcastPrefixed("&eTurnierrunde " + m.getRound() + " &7in Arena &e" + arena.getName() + " &7beendet.");
            }
            default -> plugin.msg(s, "&e/turnier match prepare|start|stop");
        }
    }

    // --------------------------------------------------------------- Spieler

    private void spieler(CommandSender s, String[] a) {
        String sub = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "assign" -> {
                if (a.length < 4) { plugin.msg(s, "&c/turnier spieler assign <Spieler> <Team>"); return; }
                TeamData t = m.findTeam(a[3]);
                if (t == null) { plugin.msg(s, "&cDieses Turnierteam existiert nicht."); return; }
                PlayerData p = m.findPlayer(a[2]);
                if (p == null) {
                    plugin.msg(s, "&cSpieler unbekannt. Er muss den Server mindestens einmal betreten haben.");
                    return;
                }
                p.team = t.name;
                plugin.msg(s, "&a" + p.name + " wurde Team &e" + t.name + " &azugeteilt.");
            }
            case "remove" -> {
                if (a.length < 3) { plugin.msg(s, "&c/turnier spieler remove <Spieler>"); return; }
                PlayerData p = m.findPlayer(a[2]);
                if (p == null) { plugin.msg(s, "&cSpieler nicht gefunden."); return; }
                p.team = null;
                plugin.msg(s, "&a" + p.name + " wurde aus seinem Team entfernt.");
            }
            case "info" -> {
                if (a.length < 3) { plugin.msg(s, "&c/turnier spieler info <Spieler>"); return; }
                PlayerData p = m.findPlayer(a[2]);
                if (p == null) { plugin.msg(s, "&cSpieler nicht gefunden."); return; }
                TurnierPlugin.raw(s, "&8&m---------- &6Spieler " + p.name + " &8&m----------");
                TurnierPlugin.raw(s, "&7Persönliche Punkte: &e" + p.points);
                TurnierPlugin.raw(s, "&7Turnierteam: " + (p.team != null ? "&e" + p.team : "&cKeinem Team zugeordnet"));
                TurnierPlugin.raw(s, "&7Kills: &c" + p.kills);
                TurnierPlugin.raw(s, "&7Bettzerstörungen: &d" + p.beds);
            }
            default -> plugin.msg(s, "&cUnbekannte Spieleraktion.");
        }
    }

    // ---------------------------------------------------------------- Punkte

    private void points(CommandSender s, String[] a) {
        String op = a[0].toLowerCase(Locale.ROOT);
        if (a.length < 2 || !(a[1].equalsIgnoreCase("spieler") || a[1].equalsIgnoreCase("team"))) {
            plugin.msg(s, "&cTyp muss spieler oder team sein.");
            return;
        }
        if (a.length < 3) { plugin.msg(s, "&cName fehlt."); return; }
        if (a.length < 4) { plugin.msg(s, "&cPunktezahl fehlt."); return; }

        int pts;
        try {
            pts = Integer.parseInt(a[3]);
        } catch (NumberFormatException e) {
            plugin.msg(s, "&cDie Punktezahl muss eine ganze Zahl sein.");
            return;
        }
        if (pts < 0) {
            plugin.msg(s, "&cDie Punktezahl darf nicht negativ sein.");
            return;
        }

        if (a[1].equalsIgnoreCase("team")) {
            TeamData t = m.findTeam(a[2]);
            if (t == null) { plugin.msg(s, "&cTeam nicht gefunden."); return; }
            t.points = apply(op, t.points, pts);
            plugin.msg(s, "&aTeam &e" + t.name + " &ahat jetzt &6" + t.points + " Punkte.");
        } else {
            PlayerData p = m.findPlayer(a[2]);
            if (p == null) { plugin.msg(s, "&cSpieler nicht gefunden."); return; }
            p.points = apply(op, p.points, pts);
            plugin.msg(s, "&aSpieler &e" + p.name + " &ahat jetzt &6" + p.points + " Punkte.");
        }
    }

    private static int apply(String op, int current, int pts) {
        return switch (op) {
            case "add" -> TournamentManager.saturatedAdd(current, pts);
            case "remove" -> Math.max(0, current - pts);
            default -> pts;
        };
    }

    private void reset(CommandSender s, String[] a) {
        String type = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "";
        if (!type.equals("team") && !type.equals("spieler")) {
            plugin.msg(s, "&cVerwendung: /turnier reset <spieler|team> <Name>");
            return;
        }
        if (a.length < 3) {
            plugin.msg(s, "&c/turnier reset " + type + " <Name>");
            return;
        }
        if (type.equals("team")) {
            TeamData t = m.findTeam(a[2]);
            if (t == null) { plugin.msg(s, "&cTeam nicht gefunden."); return; }
            t.points = 0;
            plugin.msg(s, "&aPunkte von Team &e" + t.name + " &aauf 0 gesetzt.");
        } else {
            PlayerData p = m.findPlayer(a[2]);
            if (p == null) { plugin.msg(s, "&cSpieler nicht gefunden."); return; }
            p.points = 0;
            plugin.msg(s, "&aPunkte von &e" + p.name + " &aauf 0 gesetzt.");
        }
    }

    private void top(CommandSender s) {
        TurnierPlugin.raw(s, "&8&m---------- &6BEDWARS-TURNIER &8&m----------");
        for (TeamData t : m.sortedTeams()) {
            TurnierPlugin.raw(s, "&e" + t.name + " &8- &6" + t.points + " Punkte");
        }
        TurnierPlugin.raw(s, "&8&m---------------------------------------");
    }

    // ----------------------------------------------------------- Tab-Complete

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        if (!s.hasPermission(TurnierPlugin.ADMIN_PERMISSION)) return List.of();

        if (a.length == 1) {
            return TurnierPlugin.complete(List.of("team", "spieler", "arena", "match", "add", "remove", "set",
                    "reset", "resetall", "top", "sudo"), a[0]);
        }
        String first = a[0].toLowerCase(Locale.ROOT);
        switch (first) {
            case "team" -> {
                if (a.length == 2) return TurnierPlugin.complete(List.of("create", "delete", "rename", "list", "info", "bind"), a[1]);
                String sub = a[1].toLowerCase(Locale.ROOT);
                if (a.length == 3 && List.of("delete", "rename", "info", "bind").contains(sub)) {
                    return TurnierPlugin.complete(teamNames(), a[2]);
                }
            }
            case "arena" -> { if (a.length == 2 && Bukkit.getPluginManager().getPlugin("MBedwars") != null) return TurnierPlugin.complete(BedwarsAPI.getGameAPI().getArenas().stream().map(Arena::getName).toList(), a[1]); }
            case "match" -> { if (a.length == 2) return TurnierPlugin.complete(List.of("prepare", "start", "stop"), a[1]); }
            case "spieler" -> {
                if (a.length == 2) return TurnierPlugin.complete(List.of("assign", "remove", "info"), a[1]);
                if (a.length == 3) return TurnierPlugin.complete(playerNames(), a[2]);
                if (a.length == 4 && a[1].equalsIgnoreCase("assign")) return TurnierPlugin.complete(teamNames(), a[3]);
            }
            case "add", "remove", "set", "reset" -> {
                if (a.length == 2) return TurnierPlugin.complete(List.of("spieler", "team"), a[1]);
                if (a.length == 3) {
                    return TurnierPlugin.complete(a[1].equalsIgnoreCase("team") ? teamNames() : playerNames(), a[2]);
                }
            }
            case "sudo" -> {
                if (a.length == 2) {
                    return TurnierPlugin.complete(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), a[1]);
                }
            }
            default -> { }
        }
        return List.of();
    }

    private List<String> teamNames() {
        List<String> list = new ArrayList<>();
        for (TeamData t : m.teams()) list.add(t.name);
        return list;
    }

    private List<String> playerNames() {
        List<String> list = new ArrayList<>();
        for (PlayerData p : m.allPlayers()) list.add(p.name);
        return list;
    }
}
