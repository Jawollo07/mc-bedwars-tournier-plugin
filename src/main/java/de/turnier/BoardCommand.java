package de.turnier;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

public final class BoardCommand implements TabExecutor {

    private final TurnierPlugin plugin;
    private final TournamentManager manager;
    private final ScoreboardService scoreboards;

    public BoardCommand(TurnierPlugin plugin, TournamentManager manager, ScoreboardService scoreboards) {
        this.plugin = plugin;
        this.manager = manager;
        this.scoreboards = scoreboards;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        String sub = a.length > 0 ? a[0].toLowerCase() : "";
        switch (sub) {
            case "on" -> {
                manager.setBoardEnabled(true);
                scoreboards.refreshAll();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    plugin.msg(p, "&aDas Turnier-Scoreboard wurde global aktiviert.");
                }
                plugin.msg(s, "&aScoreboard global aktiviert.");
            }
            case "off" -> {
                manager.setBoardEnabled(false);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    scoreboards.remove(p);
                    plugin.msg(p, "&cDas Turnier-Scoreboard wurde global deaktiviert.");
                }
                plugin.msg(s, "&cScoreboard global deaktiviert.");
            }
            case "status" -> plugin.msg(s, "&7Scoreboard global: &e" + manager.isBoardEnabled());
            default -> {
                plugin.msg(s, "&e/turnierboard <on|off|status>");
                return true;
            }
        }
        manager.save();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        return a.length == 1 ? TurnierPlugin.complete(List.of("on", "off", "status"), a[0]) : List.of();
    }
}
