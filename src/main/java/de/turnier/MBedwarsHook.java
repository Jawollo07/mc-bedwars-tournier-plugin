package de.turnier;

import de.marcely.bedwars.api.event.arena.ArenaBedBreakEvent;
import de.marcely.bedwars.api.arena.Arena;
import de.marcely.bedwars.api.event.arena.RoundEndEvent;
import de.marcely.bedwars.api.event.player.PlayerKillPlayerEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Automatische Wertung über die MBedwars-API.
 * Diese Klasse wird nur geladen, wenn MBedwars installiert ist.
 */
public final class MBedwarsHook implements Listener {

    private final TournamentManager manager;
    private final TurnierPlugin plugin;

    public MBedwarsHook(TurnierPlugin plugin, TournamentManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(PlayerKillPlayerEvent e) {
        if (!isTournamentArena(e.getArena())) return;
        Player killer = e.getKiller();
        if (killer == null || killer.equals(e.getDamaged())) return;
        manager.awardKill(killer);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedBreak(ArenaBedBreakEvent e) {
        if (!isTournamentArena(e.getArena())) return;
        if (!e.isPlayerCaused()) return;
        if (e.getResult() == ArenaBedBreakEvent.Result.CANCEL) return;
        Player breaker = e.getPlayer();
        if (breaker == null) return;
        manager.awardBed(breaker);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRoundEnd(RoundEndEvent e) {
        if (!isConfiguredTournamentArena(e.getArena()) || !manager.isActive() || !manager.isRoundActive()) return;
        if (!e.isTie()) {
            manager.awardWin(e.getWinnerTeam() == null ? null : e.getWinnerTeam().getConfigName(), e.getWinners());
        } else {
            plugin.broadcastPrefixed("&eRunde " + manager.getRound() + " endete unentschieden. Es gibt keine Sieg-Punkte.");
        }
        manager.setRoundActive(false);
        manager.save();
        plugin.broadcastPrefixed("&aRunde " + manager.getRound() + "/" + manager.getTotalRounds()
                + " beendet. Nutze &e/bwturnier pause &azum Zwischenstand oder &e/bwturnier next &afür die nächste Runde.");
    }

    /**
     * Wertet ausschließlich die explizit konfigurierte Turnierarena.
     * So verändern normale öffentliche BedWars-Runden nicht versehentlich die Turnierwertung.
     */
    private boolean isTournamentArena(Arena arena) {
        return manager.scoringOpen() && isConfiguredTournamentArena(arena);
    }

    private boolean isConfiguredTournamentArena(Arena arena) {
        String configured = manager.getArenaName();
        return configured != null && arena != null && configured.equalsIgnoreCase(arena.getName());
    }
}
