package de.turnier;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Hält alle Turnierdaten und speichert sie in plugins/Turnier/data.yml. */
public final class TournamentManager {

    public static final class TeamData {
        public String name;
        public int points;
        public int wins;

        TeamData(String name) {
            this.name = name;
        }
    }

    public static final class PlayerData {
        public final UUID id;
        public String name;
        public String team; // Anzeigename des Teams oder null
        public int points;
        public int kills;
        public int beds;

        PlayerData(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private final TurnierPlugin plugin;
    private final File file;

    private final Map<String, TeamData> teams = new LinkedHashMap<>(); // Schlüssel: Kleinschreibung
    private final Map<UUID, PlayerData> players = new HashMap<>();

    private boolean active;
    private boolean roundActive;
    private boolean autoScoring = true;
    private boolean boardEnabled = true;
    private int round;
    private String champion;

    public TournamentManager(TurnierPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    // ---------------------------------------------------------------- Zustand

    public boolean isActive() { return active; }
    public void setActive(boolean v) { active = v; }
    public boolean isRoundActive() { return roundActive; }
    public void setRoundActive(boolean v) { roundActive = v; }
    public boolean isAutoScoring() { return autoScoring; }
    public void setAutoScoring(boolean v) { autoScoring = v; }
    public boolean isBoardEnabled() { return boardEnabled; }
    public void setBoardEnabled(boolean v) { boardEnabled = v; }
    public int getRound() { return round; }
    public void setRound(int v) { round = v; }
    public String getChampion() { return champion; }
    public void setChampion(String v) { champion = v; }

    // ----------------------------------------------------------------- Teams

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public TeamData findTeam(String name) {
        return name == null ? null : teams.get(key(name));
    }

    public TeamData createTeam(String name) {
        if (teams.containsKey(key(name))) return null;
        TeamData t = new TeamData(name);
        teams.put(key(name), t);
        return t;
    }

    public void deleteTeam(TeamData t) {
        teams.remove(key(t.name));
        for (PlayerData p : players.values()) {
            if (p.team != null && p.team.equalsIgnoreCase(t.name)) p.team = null;
        }
    }

    /** @return false, wenn der neue Name schon belegt ist */
    public boolean renameTeam(TeamData t, String newName) {
        TeamData other = findTeam(newName);
        if (other != null && other != t) return false;
        String oldName = t.name;
        t.name = newName;
        Map<String, TeamData> copy = new LinkedHashMap<>();
        for (TeamData x : teams.values()) copy.put(key(x.name), x);
        teams.clear();
        teams.putAll(copy);
        for (PlayerData p : players.values()) {
            if (p.team != null && p.team.equalsIgnoreCase(oldName)) p.team = newName;
        }
        return true;
    }

    public Collection<TeamData> teams() {
        return teams.values();
    }

    /** Teams nach Punkten absteigend. */
    public List<TeamData> sortedTeams() {
        List<TeamData> list = new ArrayList<>(teams.values());
        list.sort(Comparator.comparingInt((TeamData t) -> t.points).reversed()
                .thenComparing(t -> t.name, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public List<PlayerData> membersOf(TeamData t) {
        List<PlayerData> list = new ArrayList<>();
        for (PlayerData p : players.values()) {
            if (p.team != null && p.team.equalsIgnoreCase(t.name)) list.add(p);
        }
        list.sort(Comparator.comparing(p -> p.name, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    // --------------------------------------------------------------- Spieler

    public PlayerData register(Player p) {
        PlayerData d = players.computeIfAbsent(p.getUniqueId(), id -> new PlayerData(id, p.getName()));
        d.name = p.getName();
        return d;
    }

    public PlayerData get(UUID id) {
        return players.get(id);
    }

    /** Sucht einen bekannten Spieler (mindestens einmal gejoint) nach Namen. */
    public PlayerData findPlayer(String name) {
        for (PlayerData p : players.values()) {
            if (p.name.equalsIgnoreCase(name)) return p;
        }
        return null;
    }

    public Collection<PlayerData> allPlayers() {
        return players.values();
    }

    public void resetAllPoints() {
        for (PlayerData p : players.values()) p.points = 0;
        for (TeamData t : teams.values()) t.points = 0;
    }

    // ------------------------------------------- API für automatische Wertung

    public boolean scoringOpen() {
        return autoScoring && active && roundActive;
    }

    /** Für spätere Anbindung an ein BedWars-Plugin: Kill gutschreiben. */
    public void awardKill(Player killer) {
        award(killer, plugin.getConfig().getInt("points.kill", 1), true);
    }

    /** Für spätere Anbindung an ein BedWars-Plugin: Bettzerstörung gutschreiben. */
    public void awardBed(Player breaker) {
        award(breaker, plugin.getConfig().getInt("points.bed", 5), false);
    }

    /** Rundensieg: jedes Turnierteam mit mindestens einem Gewinner bekommt einmal die Sieg-Punkte. */
    public void awardWin(Collection<Player> winners) {
        if (!scoringOpen()) return;
        int pts = plugin.getConfig().getInt("points.win", 10);
        java.util.Set<TeamData> winningTeams = new java.util.LinkedHashSet<>();
        for (Player p : winners) {
            TeamData t = findTeam(register(p).team);
            if (t != null) winningTeams.add(t);
        }
        for (TeamData t : winningTeams) {
            t.points += pts;
            t.wins++;
        }
    }

    private void award(Player p, int pts, boolean kill) {
        if (!scoringOpen()) return;
        PlayerData d = register(p);
        d.points += pts;
        if (kill) d.kills++; else d.beds++;
        TeamData t = findTeam(d.team);
        if (t != null) t.points += pts;
    }

    // ------------------------------------------------------------ Speicherung

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);

        active = y.getBoolean("state.active", false);
        roundActive = y.getBoolean("state.round-active", false);
        autoScoring = y.getBoolean("state.auto-scoring", true);
        boardEnabled = y.getBoolean("state.board-enabled", true);
        round = y.getInt("state.round", 0);
        champion = y.getString("state.champion");

        teams.clear();
        for (Map<?, ?> raw : y.getMapList("teams")) {
            Object n = raw.get("name");
            if (n == null) continue;
            TeamData t = new TeamData(String.valueOf(n));
            t.points = num(raw.get("points"));
            t.wins = num(raw.get("wins"));
            teams.put(key(t.name), t);
        }

        players.clear();
        ConfigurationSection ps = y.getConfigurationSection("players");
        if (ps != null) {
            for (String k : ps.getKeys(false)) {
                ConfigurationSection s = ps.getConfigurationSection(k);
                if (s == null) continue;
                try {
                    UUID id = UUID.fromString(k);
                    PlayerData d = new PlayerData(id, s.getString("name", k));
                    d.team = s.getString("team");
                    d.points = s.getInt("points");
                    d.kills = s.getInt("kills");
                    d.beds = s.getInt("beds");
                    players.put(id, d);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Ungültige UUID in data.yml: " + k);
                }
            }
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();

        y.set("state.active", active);
        y.set("state.round-active", roundActive);
        y.set("state.auto-scoring", autoScoring);
        y.set("state.board-enabled", boardEnabled);
        y.set("state.round", round);
        y.set("state.champion", champion);

        List<Map<String, Object>> list = new ArrayList<>();
        for (TeamData t : teams.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", t.name);
            m.put("points", t.points);
            m.put("wins", t.wins);
            list.add(m);
        }
        y.set("teams", list);

        for (PlayerData p : players.values()) {
            String base = "players." + p.id;
            y.set(base + ".name", p.name);
            y.set(base + ".team", p.team);
            y.set(base + ".points", p.points);
            y.set(base + ".kills", p.kills);
            y.set(base + ".beds", p.beds);
        }

        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "data.yml konnte nicht gespeichert werden", e);
        }
    }

    private static int num(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }
}
