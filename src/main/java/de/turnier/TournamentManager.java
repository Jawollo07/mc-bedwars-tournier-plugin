package de.turnier;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.scheduler.BukkitTask;

/** Hält alle Turnierdaten und speichert sie in plugins/Turnier/data.yml. */
public final class TournamentManager {

    public static final class TeamData {
        public String name;
        public int points;
        public int wins;
        public int bedsDestroyed;
        public String bedwarsTeam;

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
    private int totalRounds = 3;
    private String champion;
    private String arenaName;
    private BukkitTask pendingSave;
    private final Set<String> bedsDestroyedThisRound = new LinkedHashSet<>();
    private final Set<String> placementsThisRound = new LinkedHashSet<>();
    private final List<String> eliminatedThisRound = new ArrayList<>();
    private final Map<String, UUID> bedBreakersThisRound = new HashMap<>();

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
    public void setRound(int v) { round = Math.max(0, v); }
    public int getTotalRounds() { return totalRounds; }
    public void setTotalRounds(int v) { totalRounds = Math.max(1, Math.min(100, v)); }
    public String getChampion() { return champion; }
    public void setChampion(String v) { champion = v; }
    public String getArenaName() { return arenaName; }
    public void setArenaName(String v) { arenaName = v; }

    // ----------------------------------------------------------------- Teams

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public TeamData findTeam(String name) {
        return name == null ? null : teams.get(key(name));
    }

    public TeamData createTeam(String name) {
        if (!validTeamName(name) || teams.containsKey(key(name))) return null;
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
        if (t == null || !validTeamName(newName)) return false;
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
                .thenComparing(Comparator.comparingInt((TeamData t) -> t.wins).reversed())
                .thenComparing(Comparator.comparingInt((TeamData t) -> t.bedsDestroyed).reversed())
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

    /** Clears per-round tracking without resetting tournament-wide totals. */
    public void beginRoundScoring() {
        bedsDestroyedThisRound.clear();
        placementsThisRound.clear();
        eliminatedThisRound.clear();
        bedBreakersThisRound.clear();
    }

    private TeamData findTeamByBedwarsName(String bedwarsTeamName) {
        if (bedwarsTeamName == null) return null;
        for (TeamData team : teams.values()) {
            if (team.bedwarsTeam != null && team.bedwarsTeam.equalsIgnoreCase(bedwarsTeamName)) return team;
        }
        return null;
    }

    private static String roundKey(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    /** Adds placement points once per team per round. */
    public void awardPlacement(String bedwarsTeamName, int placement) {
        awardPlacement(findTeamByBedwarsName(bedwarsTeamName), placement);
    }

    private void awardPlacement(TeamData team, int placement) {
        if (!active || round < 1 || team == null || placement < 1 || placement > 3) return;
        String k = key(team.name);
        if (!placementsThisRound.add(k)) return;
        String path = placement == 1 ? "points.first" : placement == 2 ? "points.second" : "points.third";
        int fallback = placement == 1 ? 10 : placement == 2 ? 6 : 3;
        team.points = saturatedAdd(team.points, configuredPoints(path, fallback));
        if (placement == 1 && team.wins < Integer.MAX_VALUE) team.wins++;
        scheduleSave();
    }

    /** Records elimination order: first eliminated is third, second is second in a three-team match. */
    public void recordElimination(String bedwarsTeamName, Player causingPlayer) {
        if (!scoringOpen()) return;
        TeamData eliminated = findTeamByBedwarsName(bedwarsTeamName);
        if (eliminated == null) return;
        String k = key(eliminated.name);
        if (eliminatedThisRound.contains(k)) return;
        eliminatedThisRound.add(k);
        int placement = eliminatedThisRound.size() == 1 ? 3 : eliminatedThisRound.size() == 2 ? 2 : 0;
        if (placement > 0) awardPlacement(bedwarsTeamName, placement);

        String bedKey = roundKey(bedwarsTeamName);
        UUID bedBreaker = bedKey == null ? null : bedBreakersThisRound.get(bedKey);
        if (bedKey != null && bedsDestroyedThisRound.contains(bedKey) && causingPlayer != null
                && !causingPlayer.getUniqueId().equals(bedBreaker)) {
            awardExtra(causingPlayer, configuredPoints("points.bedless-elimination", 1));
        }
        scheduleSave();
    }

    /** A bed scores only once per owning BedWars team in a round. */
    public void awardBed(Player breaker, String bedwarsOwnerTeam) {
        if (!scoringOpen() || breaker == null || bedwarsOwnerTeam == null) return;
        String k = roundKey(bedwarsOwnerTeam);
        if (k == null || !bedsDestroyedThisRound.add(k)) return;
        if (breaker.getUniqueId() != null) bedBreakersThisRound.put(k, breaker.getUniqueId());
        award(breaker, configuredPoints("points.bed", 3), false);
        TeamData scoringTeam = findTeam(register(breaker).team);
        if (scoringTeam != null && scoringTeam.bedsDestroyed < Integer.MAX_VALUE) scoringTeam.bedsDestroyed++;
    }

    /** Adds the survival bonus to teams whose beds were never destroyed this round. */
    public void awardBedSurvivalBonuses() {
        if (!active || round < 1) return;
        for (TeamData team : teams.values()) {
            if (team.bedwarsTeam == null) continue;
            String k = roundKey(team.bedwarsTeam);
            if (k != null && !bedsDestroyedThisRound.contains(k)) {
                team.points = saturatedAdd(team.points, configuredPoints("points.bed-survived", 2));
            }
        }
        scheduleSave();
    }

    public boolean wasBedDestroyed(String bedwarsTeamName) {
        String k = roundKey(bedwarsTeamName);
        return k != null && bedsDestroyedThisRound.contains(k);
    }

    private void awardExtra(Player player, int points) {
        if (player == null || points <= 0) return;
        PlayerData data = register(player);
        data.points = saturatedAdd(data.points, points);
        TeamData team = findTeam(data.team);
        if (team != null) team.points = saturatedAdd(team.points, points);
        scheduleSave();
    }

    private static boolean validTeamName(String name) {
        return name != null && name.matches("[\\p{L}\\p{N}_-]{1,32}");
    }

    /** Adds points without allowing integer overflow to wrap the leaderboard. */
    public static int saturatedAdd(int current, int amount) {
        long result = (long) current + amount;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, result));
    }

    private int configuredPoints(String path, int fallback) {
        int value = plugin.getConfig().getInt(path, fallback);
        if (value < 0) {
            plugin.getLogger().warning("Negative point value in config at '" + path + "'; using 0.");
            return 0;
        }
        return value;
    }

    private void scheduleSave() {
        if (pendingSave != null) return;
        pendingSave = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingSave = null;
            save();
        }, 40L);
    }

    // ------------------------------------------- API für automatische Wertung

    public boolean scoringOpen() {
        return autoScoring && active && roundActive;
    }

    /** Für spätere Anbindung an ein BedWars-Plugin: Kill gutschreiben. */
    public void awardKill(Player killer) {
        award(killer, configuredPoints("points.kill", 1), true);
    }

    /** Für spätere Anbindung an ein BedWars-Plugin: Bettzerstörung gutschreiben. */
    public void awardBed(Player breaker) {
        award(breaker, configuredPoints("points.bed", 3), false);
    }

    /** Rundensieg: das gemappte MBedwars-Siegerteam zählt auch dann, wenn seine Spieler offline sind. */
    public void awardWin(String bedwarsTeamName, Collection<Player> winners) {
        if (!scoringOpen()) return;
        java.util.Set<TeamData> winningTeams = new java.util.LinkedHashSet<>();

        if (bedwarsTeamName != null) {
            for (TeamData team : teams.values()) {
                if (team.bedwarsTeam != null && team.bedwarsTeam.equalsIgnoreCase(bedwarsTeamName)) {
                    winningTeams.add(team);
                    break;
                }
            }
        }

        // Fallback für Turnierteams ohne explizites MBedwars-Mapping.
        if (winningTeams.isEmpty() && winners != null) {
            for (Player p : winners) {
                if (p == null) continue;
                TeamData t = findTeam(register(p).team);
                if (t != null) winningTeams.add(t);
            }
        }

        if (winningTeams.isEmpty()) return;
        for (TeamData t : winningTeams) awardPlacement(t, 1);
        awardBedSurvivalBonuses();
    }

    private void award(Player p, int pts, boolean kill) {
        if (!scoringOpen() || p == null) return;
        PlayerData d = register(p);
        d.points = saturatedAdd(d.points, pts);
        if (kill) {
            if (d.kills < Integer.MAX_VALUE) d.kills++;
        } else if (d.beds < Integer.MAX_VALUE) {
            d.beds++;
        }
        TeamData t = findTeam(d.team);
        if (t != null) t.points = saturatedAdd(t.points, pts);
        scheduleSave();
    }

    // ------------------------------------------------------------ Speicherung

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);

        // A process restart cannot safely resume a live BedWars round. Preserve standings,
        // but require an administrator to start the tournament and round again.
        active = false;
        roundActive = false;
        autoScoring = y.getBoolean("state.auto-scoring", true);
        boardEnabled = y.getBoolean("state.board-enabled", true);
        round = y.getInt("state.round", 0);
        totalRounds = Math.max(1, Math.min(100, y.getInt("state.total-rounds", 3)));
        champion = y.getString("state.champion");
        arenaName = y.getString("state.arena");

        teams.clear();
        for (Map<?, ?> raw : y.getMapList("teams")) {
            Object n = raw.get("name");
            if (n == null) continue;
            TeamData t = new TeamData(String.valueOf(n));
            t.points = num(raw.get("points"));
            t.wins = num(raw.get("wins"));
            t.bedsDestroyed = num(raw.get("beds-destroyed"));
            Object bedwarsTeam = raw.get("bedwars-team");
            t.bedwarsTeam = bedwarsTeam == null ? null : String.valueOf(bedwarsTeam);
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
        y.set("state.total-rounds", totalRounds);
        y.set("state.champion", champion);
        y.set("state.arena", arenaName);

        List<Map<String, Object>> list = new ArrayList<>();
        for (TeamData t : teams.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", t.name);
            m.put("points", t.points);
            m.put("wins", t.wins);
            m.put("beds-destroyed", t.bedsDestroyed);
            if (t.bedwarsTeam != null) m.put("bedwars-team", t.bedwarsTeam);
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

        if (pendingSave != null) {
            pendingSave.cancel();
            pendingSave = null;
        }
        File temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                throw new IOException("Plugin-Datenverzeichnis konnte nicht erstellt werden");
            }
            y.save(temporary);
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "data.yml konnte nicht sicher gespeichert werden", e);
        } finally {
            try {
                Files.deleteIfExists(temporary.toPath());
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Temporäre Datendatei konnte nicht entfernt werden", e);
            }
        }
    }

    private static int num(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }
}
