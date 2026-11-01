package de.turnier;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

/**
 * Keeps one unbreakable, tournament-bound Riptide II trident with every
 * registered tournament participant while a round is active.
 */
public final class TridentService implements Listener {

    private static final int RIPTIDE_LEVEL = 2;

    private final TurnierPlugin plugin;
    private final TournamentManager manager;
    private final NamespacedKey marker;

    public TridentService(TurnierPlugin plugin, TournamentManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.marker = new NamespacedKey(plugin, "tournament_riptide_trident");
    }

    public boolean isTournamentTrident(ItemStack item) {
        if (item == null || item.getType() != Material.TRIDENT || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(marker, PersistentDataType.BYTE);
    }

    /** Gives the item if missing, or repairs its enchantment if already present. */
    public void give(Player player) {
        if (player == null || !player.isOnline()) return;

        PlayerInventory inventory = player.getInventory();
        ItemStack existing = null;
        ItemStack[] contents = inventory.getContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (!isTournamentTrident(item)) continue;
            if (existing == null) {
                existing = item;
                repair(existing);
            } else {
                inventory.setItem(slot, null);
            }
        }

        if (existing != null) {
            if (isTournamentTrident(inventory.getItemInOffHand())) {
                // The offhand is included in getContents on supported Paper versions.
                // No additional copy is needed.
            }
            return;
        }

        ItemStack trident = createTrident();
        Map<Integer, ItemStack> leftovers = inventory.addItem(trident);
        if (!leftovers.isEmpty()) {
            if (inventory.getItemInOffHand().getType() == Material.AIR) {
                inventory.setItemInOffHand(leftovers.values().iterator().next());
            } else {
                plugin.getLogger().warning("Konnte Turnier-Dreizack nicht an " + player.getName()
                        + " geben: Inventar und Nebenhand sind voll.");
                plugin.msg(player, "&cDein Inventar ist voll. Schaffe Platz, damit du deinen Turnier-Dreizack erhalten kannst.");
            }
        }
    }

    private ItemStack createTrident() {
        ItemStack item = new ItemStack(Material.TRIDENT);
        repair(item);
        return item;
    }

    private void repair(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.displayName(Component.text("Turnier-Dreizack"));
        meta.lore(List.of(Component.text("Riptide II"), Component.text("Unzerstörbar")));
        meta.addEnchant(Enchantment.RIPTIDE, RIPTIDE_LEVEL, true);
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(marker, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    private boolean isParticipant(Player player) {
        if (!manager.isActive() || !manager.isRoundActive()) return false;
        TournamentManager.PlayerData data = manager.get(player.getUniqueId());
        return data != null && data.team != null && !data.team.isBlank();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isTournamentTrident(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            plugin.msg(event.getPlayer(), "&cDeinen Turnier-Dreizack kannst du nicht wegwerfen.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isTournamentTrident);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && isParticipant(player)) give(player);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        boolean protectedCurrent = isTournamentTrident(event.getCurrentItem());
        boolean protectedCursor = isTournamentTrident(event.getCursor());
        boolean protectedHotbar = event.getHotbarButton() >= 0
                && isTournamentTrident(player.getInventory().getItem(event.getHotbarButton()));
        boolean protectedOffhandSwap = event.getClick() == ClickType.SWAP_OFFHAND
                && isTournamentTrident(player.getInventory().getItemInOffHand());

        if (protectedCurrent || protectedCursor || protectedHotbar || protectedOffhandSwap) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isTournamentTrident(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }
}
