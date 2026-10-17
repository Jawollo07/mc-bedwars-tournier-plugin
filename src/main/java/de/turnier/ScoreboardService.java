package de.turnier;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Zeigt jedem Spieler ein eigenes Sidebar-Scoreboard (ersetzt FastBoard aus SkBee). */
public final class ScoreboardService {

    private static final String OBJECTIVE = "turnier";
    private static final int LINES = 7;

    private final TurnierPlugin plugin;
    private final TournamentManager manager;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private BukkitTask task;

    public ScoreboardService(TurnierPlugin plugin, TournamentManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void start() {
        long ticks = Math.max(1L, plugin.getConfig().getLong("scoreboard.refresh-ticks", 40L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 20L, ticks);
    }

    public void stop() {
        if (task != null) task.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) remove(p);
        boards.clear();
    }

    public void refreshAll() {
        for (Player p : Bukkit.getOnlinePlayers()) update(p);
    }

    public void forget(Player p) {
        boards.remove(p.getUniqueId());
    }

    /** Entfernt das Turnier-Scoreboard und zeigt wieder das Standard-Scoreboard. */
    public void remove(Player p) {
        Scoreboard board = boards.remove(p.getUniqueId());
        if (board != null && p.getScoreboard() == board) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void update(Player p) {
        if (!manager.isBoardEnabled()) {
            remove(p);
            return;
        }

        Scoreboard board = boards.computeIfAbsent(p.getUniqueId(), id -> createBoard());
        if (p.getScoreboard() != board) p.setScoreboard(board);

        Objective obj = board.getObjective(OBJECTIVE);
        if (obj == null) return;
        obj.displayName(TurnierPlugin.color(plugin.getConfig().getString("scoreboard.title", "&6&lBEDWARS")));

        TournamentManager.PlayerData data = manager.register(p);
        TournamentManager.TeamData team = manager.findTeam(data.team);

        String[] lines = new String[LINES]; // von oben nach unten
        lines[0] = "&8&m----------------";
        lines[1] = "&7Spieler:";
        lines[2] = "&f" + p.getName();
        lines[3] = "&7Turnierteam:";
        lines[4] = team != null ? "&e" + team.name : "&cKein Team";
        lines[5] = "&7Punkte: &e" + data.points;
        lines[6] = "&7Team-Punkte: &e" + (team != null ? String.valueOf(team.points) : "-");

        for (int i = 0; i < LINES; i++) {
            var score = obj.getScore(entry(i));
            score.customName(TurnierPlugin.color(lines[i]));
            score.setScore(LINES - i);
        }
    }

    private Scoreboard createBoard() {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective(OBJECTIVE, Criteria.DUMMY, Component.text("BEDWARS"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(NumberFormat.blank()); // rote Zahlen rechts ausblenden
        return board;
    }

    /** Eindeutiger, unsichtbarer Eintragsname pro Zeile. */
    private static String entry(int line) {
        return "\u00a7" + Integer.toHexString(line);
    }
}
