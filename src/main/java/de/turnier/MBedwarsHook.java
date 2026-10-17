package de.turnier;

import de.marcely.bedwars.api.event.arena.ArenaBedBreakEvent;
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

    public MBedwarsHook(TournamentManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(PlayerKillPlayerEvent e) {
        Player killer = e.getKiller();
        if (killer == null || killer.equals(e.getDamaged())) return;
        manager.awardKill(killer);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedBreak(ArenaBedBreakEvent e) {
        if (!e.isPlayerCaused()) return;
        if (e.getResult() == ArenaBedBreakEvent.Result.CANCEL) return;
        Player breaker = e.getPlayer();
        if (breaker == null) return;
        manager.awardBed(breaker);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRoundEnd(RoundEndEvent e) {
        if (e.isTie()) return;
        manager.awardWin(e.getWinners());
    }
}
