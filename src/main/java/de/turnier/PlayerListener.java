package de.turnier;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {

    private final TurnierPlugin plugin;
    private final TournamentManager manager;
    private final ScoreboardService scoreboards;

    public PlayerListener(TurnierPlugin plugin, TournamentManager manager, ScoreboardService scoreboards) {
        this.plugin = plugin;
        this.manager = manager;
        this.scoreboards = scoreboards;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var player = e.getPlayer();
        manager.register(player);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) scoreboards.update(player);
        }, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        scoreboards.forget(e.getPlayer());
    }
}
