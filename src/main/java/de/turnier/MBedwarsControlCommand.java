package de.turnier;

import de.marcely.bedwars.api.BedwarsAPI;
import de.marcely.bedwars.api.arena.Arena;
import de.marcely.bedwars.api.arena.ArenaStatus;
import de.marcely.bedwars.api.arena.AddPlayerCause;
import de.marcely.bedwars.api.arena.AddPlayerIssue;
import de.marcely.bedwars.api.arena.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Administrative bridge between Turnier and the official MBedwars API.
 * Only uses public API operations; no command dispatch or local MBedwars JAR.
 */
public final class MBedwarsControlCommand implements TabExecutor {
    private final TurnierPlugin plugin;

    public MBedwarsControlCommand(TurnierPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(TurnierPlugin.ADMIN_PERMISSION)) {
            plugin.msg(sender, "&cDu hast keine Berechtigung.");
            return true;
        }
        if (!available(sender)) return true;
        if (args.length == 0) {
            help(sender);
            return true;
        }

        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "status" -> status(sender);
                case "arenas", "list" -> listArenas(sender);
                case "arena" -> arenaInfo(sender, args);
                case "team" -> team(sender, args);
                case "start" -> setRunning(sender, args);
                case "stop" -> stopArena(sender, args);
                case "join" -> joinArena(sender, args);
                case "kick" -> kickPlayer(sender, args);
                case "reload" -> reload(sender, args);
                default -> help(sender);
            }
        } catch (RuntimeException | LinkageError ex) {
            plugin.getLogger().warning("MBedwars-Steuerung fehlgeschlagen: " + ex);
            plugin.msg(sender, "&cMBedwars-Aktion fehlgeschlagen. Details stehen im Serverlog.");
        }
        return true;
    }

    private boolean available(CommandSender sender) {
        if (Bukkit.getPluginManager().getPlugin("MBedwars") == null) {
            plugin.msg(sender, "&cMBedwars ist nicht installiert oder nicht geladen.");
            return false;
        }
        try {
            if (BedwarsAPI.getGameAPI() == null) {
                plugin.msg(sender, "&cDie MBedwars-API ist noch nicht bereit.");
                return false;
            }
        } catch (RuntimeException | LinkageError ex) {
            plugin.msg(sender, "&cDie MBedwars-API ist nicht verfügbar.");
            return false;
        }
        return true;
    }

    private void status(CommandSender sender) {
        plugin.msg(sender, "&eMBedwars &8| &7API-Version: &f" + BedwarsAPI.getAPIVersion());
        plugin.msg(sender, "&7Arenen geladen: &e" + BedwarsAPI.getGameAPI().getArenas().size());
        plugin.msg(sender, "&7Turnier: " + (plugin.manager().isActive() ? "&aaktiv" : "&cbeendet/inaktiv")
                + " &8| &7Runde: &e" + plugin.manager().getRound()
                + " &8| &7Automatische Wertung: " + (plugin.manager().isAutoScoring() ? "&aAN" : "&cAUS"));
    }

    private void listArenas(CommandSender sender) {
        var arenas = BedwarsAPI.getGameAPI().getArenas();
        plugin.msg(sender, "&eMBedwars-Arenen (&f" + arenas.size() + "&e):");
        if (arenas.isEmpty()) {
            plugin.msg(sender, "&7Keine Arenen geladen.");
            return;
        }
        for (Arena arena : arenas) {
            plugin.msg(sender, "&f" + arena.getName() + " &8| &e" + arena.getStatus()
                    + " &8| &7Spieler: &f" + arena.getPlayers().size());
        }
    }

    private void arenaInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.msg(sender, "&cVerwendung: /mbwctl arena <Name>");
            return;
        }
        Arena arena = findArena(args[1]);
        if (arena == null) {
            plugin.msg(sender, "&cArena nicht gefunden: &f" + args[1]);
            return;
        }
        plugin.msg(sender, "&eArena &f" + arena.getName() + " &8| &e" + arena.getStatus());
        plugin.msg(sender, "&7Spieler: &f" + arena.getPlayers().size()
                + " &8| &7Zuschauer: &f" + arena.getSpectators().size());
        plugin.msg(sender, "&7Aktive Teams: &f" + arena.getEnabledTeams().size());
        for (Team team : arena.getEnabledTeams()) {
            plugin.msg(sender, "&8- &f" + team.getConfigName());
        }
    }

    private void team(CommandSender sender, String[] args) {
        if (args.length < 4 || !(args[3].equalsIgnoreCase("on") || args[3].equalsIgnoreCase("off"))) {
            plugin.msg(sender, "&cVerwendung: /mbwctl team <Arena> <Farbe> <on|off>");
            return;
        }
        Arena arena = findArena(args[1]);
        if (arena == null) { plugin.msg(sender, "&cArena nicht gefunden: &f" + args[1]); return; }
        if (arena.getStatus() != ArenaStatus.STOPPED) {
            plugin.msg(sender, "&cTeams können nur geändert werden, wenn die Arena gestoppt ist.");
            return;
        }
        Team team = Team.getByName(args[2]);
        if (team == null) { plugin.msg(sender, "&cUnbekannte MBedwars-Teamfarbe: &f" + args[2]); return; }
        boolean enabled = args[3].equalsIgnoreCase("on");
        if (arena.isTeamEnabled(team) == enabled) {
            plugin.msg(sender, "&eTeam &f" + team.getConfigName() + (enabled ? " &eist bereits aktiviert." : " &eist bereits deaktiviert."));
            return;
        }
        if (!arena.setTeamEnabled(team, enabled)) {
            plugin.msg(sender, "&cMBedwars konnte den Teamstatus nicht ändern. Prüfe die Arena-Konfiguration.");
            return;
        }
        plugin.msg(sender, "&aTeam &e" + team.getConfigName() + (enabled ? " &awurde aktiviert." : " &awurde deaktiviert."));
    }
    private void setRunning(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.msg(sender, "&cVerwendung: /mbwctl start <Arena>");
            return;
        }
        Arena arena = findArena(args[1]);
        if (arena == null) {
            plugin.msg(sender, "&cArena nicht gefunden.");
            return;
        }
        if (arena.getStatus() != ArenaStatus.LOBBY) {
            plugin.msg(sender, "&cArena muss in der Lobby sein. Status: &e" + arena.getStatus());
            return;
        }
        arena.setStatus(ArenaStatus.RUNNING);
        plugin.msg(sender, "&aStart angefordert für Arena &e" + arena.getName() + "&a.");
    }

    private void stopArena(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.msg(sender, "&cVerwendung: /mbwctl stop <Arena>");
            return;
        }
        Arena arena = findArena(args[1]);
        if (arena == null) {
            plugin.msg(sender, "&cArena nicht gefunden.");
            return;
        }
        if (!arena.endMatch(null)) {
            plugin.msg(sender, "&cMBedwars konnte die Arena nicht beenden. Status: &e" + arena.getStatus());
            return;
        }
        plugin.msg(sender, "&aMatch in Arena &e" + arena.getName() + " &awurde beendet.");
    }

    private void joinArena(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.msg(sender, "&cVerwendung: /mbwctl join <Arena> [Spieler]");
            return;
        }
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                plugin.msg(sender, "&cSpieler muss online sein.");
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            plugin.msg(sender, "&cAls Konsole bitte einen Spieler angeben: /mbwctl join <Arena> <Spieler>");
            return;
        }
        Arena arena = findArena(args[1]);
        if (arena == null) {
            plugin.msg(sender, "&cArena nicht gefunden.");
            return;
        }
        if (arena.getStatus() != ArenaStatus.LOBBY) {
            plugin.msg(sender, "&cSpieler können über diesen Befehl nur einer Lobby beitreten.");
            return;
        }
        AddPlayerIssue issue = arena.addPlayer(target, null, AddPlayerCause.PLUGIN);
        if (issue != null) {
            plugin.msg(sender, "&cMBedwars konnte " + target.getName() + " nicht hinzufügen: &f" + issue);
            return;
        }
        plugin.msg(sender, "&a" + target.getName() + " wurde Arena &e" + arena.getName() + " &ahinzugefügt.");
    }

    private void kickPlayer(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.msg(sender, "&cVerwendung: /mbwctl kick <Spieler>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.msg(sender, "&cSpieler muss online sein.");
            return;
        }
        Arena arena = BedwarsAPI.getGameAPI().getArenaByPlayer(target);
        if (arena == null || !arena.kickPlayer(target)) {
            plugin.msg(sender, "&cDer Spieler spielt in keiner lokalen MBedwars-Arena.");
            return;
        }
        plugin.msg(sender, "&a" + target.getName() + " wurde aus Arena &e" + arena.getName() + " &aentfernt.");
    }

    private void reload(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            plugin.msg(sender, "&cAchtung: MBedwars-Reload wirft Spieler aus laufenden Arenen.");
            plugin.msg(sender, "&eZum Bestätigen: /mbwctl reload confirm");
            return;
        }
        if (!BedwarsAPI.reload(sender)) {
            plugin.msg(sender, "&cMBedwars konnte nicht neu geladen werden.");
            return;
        }
        plugin.msg(sender, "&aMBedwars-Reload wurde angefordert.");
    }

    private Arena findArena(String name) {
        return BedwarsAPI.getGameAPI().getArenaByExactName(name);
    }

    private void help(CommandSender sender) {
        plugin.msg(sender, "&eMBedwars-Steuerung");
        for (String line : List.of(
                "/mbwctl status",
                "/mbwctl arenas",
                "/mbwctl arena <Name>",
                "/mbwctl start <Arena>",
                "/mbwctl stop <Arena>",
                "/mbwctl join <Arena> [Spieler]",
                "/mbwctl kick <Spieler>",
                "/mbwctl reload confirm")) {
            TurnierPlugin.raw(sender, "&6" + line);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(TurnierPlugin.ADMIN_PERMISSION)) return List.of();
        if (args.length == 1) return TurnierPlugin.complete(
                List.of("status", "arenas", "arena", "team", "start", "stop", "join", "kick", "reload"), args[0]);
        if (args.length == 2 && List.of("arena", "team", "start", "stop", "join").contains(args[0].toLowerCase(Locale.ROOT))) {
            if (Bukkit.getPluginManager().getPlugin("MBedwars") == null) return List.of();
            try {
                return TurnierPlugin.complete(BedwarsAPI.getGameAPI().getArenas().stream().map(Arena::getName).toList(), args[1]);
            } catch (RuntimeException | LinkageError ignored) {
                return List.of();
            }
        }
        if (args.length == 2 && List.of("kick").contains(args[0].toLowerCase(Locale.ROOT))
                || args.length == 3 && args[0].equalsIgnoreCase("join")) {
            return TurnierPlugin.complete(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),
                    args[args.length - 1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("team")) {
            return TurnierPlugin.complete(java.util.Arrays.stream(Team.values()).map(Team::getConfigName).toList(), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("team")) {
            return TurnierPlugin.complete(List.of("on", "off"), args[3]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reload")) {
            return TurnierPlugin.complete(List.of("confirm"), args[1]);
        }
        return new ArrayList<>();
    }
}
