package de.turnier;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class TurnierPlugin extends JavaPlugin {

    public static final String ADMIN_PERMISSION = "turnier.admin";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private TournamentManager manager;
    private ScoreboardService scoreboards;
    private TurnierBedwarsAddon bedwarsAddon;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        manager = new TournamentManager(this);
        manager.load();

        scoreboards = new ScoreboardService(this, manager);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, manager, scoreboards), this);

        register("turnier", new TurnierCommand(this, manager));
        register("bwturnier", new BwTurnierCommand(this, manager));
        register("turnierboard", new BoardCommand(this, manager, scoreboards));

        hookMBedwars();

        // Bei /reload bereits online befindliche Spieler erfassen
        for (Player p : Bukkit.getOnlinePlayers()) {
            manager.register(p);
        }
        scoreboards.start();
    }

    @Override
    public void onDisable() {
        if (scoreboards != null) {
            scoreboards.stop();
        }
        if (manager != null) {
            manager.save();
        }
        if (bedwarsAddon != null && bedwarsAddon.isRegistered()) {
            bedwarsAddon.unregister();
        }
    }

    private void hookMBedwars() {
        if (!getConfig().getBoolean("mbedwars.enabled", true)) return;
        if (getServer().getPluginManager().getPlugin("MBedwars") == null) {
            getLogger().info("MBedwars nicht gefunden – automatische Wertung ist nicht aktiv.");
            return;
        }

        try {
            bedwarsAddon = new TurnierBedwarsAddon(this);
            if (!bedwarsAddon.register()) {
                getLogger().warning("MBedwars hat das Turnier-Addon nicht registriert; automatische Wertung bleibt deaktiviert.");
                bedwarsAddon = null;
                return;
            }

            getServer().getPluginManager().registerEvents(new MBedwarsHook(manager), this);
            getLogger().info("MBedwars-Addon registriert; automatische Wertung ist aktiv.");
        } catch (Exception | LinkageError t) {
            getLogger().warning("MBedwars-Anbindung fehlgeschlagen: " + t);
            if (bedwarsAddon != null && bedwarsAddon.isRegistered()) {
                bedwarsAddon.unregister();
            }
            bedwarsAddon = null;
        }
    }

    private void register(String name, org.bukkit.command.TabExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().severe("Befehl fehlt in der plugin.yml: " + name);
            return;
        }
        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);
    }

    public TournamentManager manager() {
        return manager;
    }

    /** Wandelt &-Farbcodes in eine Component um. */
    public static Component color(String text) {
        return LEGACY.deserialize(text);
    }

    public String prefix() {
        return getConfig().getString("prefix", "&8[&6Turnier&8] &7");
    }

    /** Nachricht mit Präfix. */
    public void msg(CommandSender to, String text) {
        to.sendMessage(color(prefix() + text));
    }

    /** Nachricht ohne Präfix. */
    public static void raw(CommandSender to, String text) {
        to.sendMessage(color(text));
    }

    public void broadcastPrefixed(String text) {
        Bukkit.broadcast(color(prefix() + text));
    }

    public static void broadcastRaw(String text) {
        Bukkit.broadcast(color(text));
    }

    public static List<String> complete(Collection<String> options, String typed) {
        String t = typed.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(t)).sorted().toList();
    }
}
