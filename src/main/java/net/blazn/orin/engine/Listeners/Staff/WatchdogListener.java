package net.blazn.orin.engine.Listeners.Staff;

import net.blazn.orin.engine.Managers.WatchdogManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WatchdogListener implements Listener {

    private final JavaPlugin plugin;
    private final WatchdogManager watchdogManager;

    public WatchdogListener(JavaPlugin plugin, WatchdogManager watchdogManager) {
        this.plugin = plugin;
        this.watchdogManager = watchdogManager;
    }

    /**
     * ✅ Disables all Watchdog players when the plugin is disabled.
     */
    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        watchdogManager.disableAllWatchdogs();
    }

    /**
     * ✅ Hides Watchdog players upon joining to maintain stealth mode.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();

        // ✅ Hide join message if the joining player is in Watchdog Mode
        if (watchdogManager.isInWatchdog(joiningPlayer)) {
            event.setJoinMessage(null);

            // Hide Watchdog player from all other players
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.hidePlayer(plugin, joiningPlayer);
            }

            // Restore Watchdog mode (flight, tracker)
            joiningPlayer.setAllowFlight(true);
            joiningPlayer.setFlying(true);
            joiningPlayer.getInventory().clear();
            joiningPlayer.getInventory().setItem(0, getPlayerTrackerCompass());

            joiningPlayer.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " You are still in Watchdog mode.");
        }

        // ✅ Hide ALL active Watchdog players from the newly joined player
        for (Player watchdogPlayer : watchdogManager.getAllWatchdogs()) {
            joiningPlayer.hidePlayer(plugin, watchdogPlayer);
        }
    }

    /**
     * ✅ Prevents Watchdog players from appearing in quit messages.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (watchdogManager.isInWatchdog(player)) {
            event.setQuitMessage(null); // Hide quit message
        }
    }

    /**
     * ✅ Opens the Player Tracker menu when a Watchdog player right-clicks the compass.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (watchdogManager.isInWatchdog(player) && item != null && item.getType() == Material.COMPASS) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getDisplayName().equals(ChatUtil.bgold + "Player Tracker")) {
                event.setCancelled(true);
                openPlayerMenu(player);
            }
        }
    }

    /**
     * ✅ Opens the GUI to select a player for teleportation.
     */
    private void openPlayerMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, ChatUtil.bgold + "ᴡᴀᴛᴄʜᴅᴏɢ");

        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(player)) continue;
            if (slot >= 27) break; // Limit to 27 slots

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(target);
                skullMeta.setDisplayName(ChatUtil.gold + target.getDisplayName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatUtil.gray + "Click to teleport to " + ChatUtil.white + target.getDisplayName());
                skullMeta.setLore(lore);
                skull.setItemMeta(skullMeta);
            }
            menu.setItem(slot++, skull);
        }

        player.openInventory(menu);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
    }

    /**
     * ✅ Handles teleportation when selecting a player in the Watchdog menu.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatUtil.bgold + "ᴡᴀᴛᴄʜᴅᴏɢ")) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem != null && clickedItem.getType() == Material.PLAYER_HEAD) {
            SkullMeta skullMeta = (SkullMeta) clickedItem.getItemMeta();
            if (skullMeta != null && skullMeta.hasOwner()) {
                Player target = Bukkit.getPlayer(skullMeta.getOwningPlayer().getUniqueId());

                if (target != null) {
                    player.closeInventory();
                    player.teleport(target.getLocation());
                    player.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " Teleported to" +
                            ChatUtil.darkGray + ": " + ChatUtil.gold + target.getDisplayName());
                } else {
                    player.sendMessage(ChatUtil.notOnline);
                }
            }
        }
    }

    /**
     * ✅ Returns the Watchdog Tracker Compass.
     */
    private ItemStack getPlayerTrackerCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatUtil.bgold + "Player Tracker");
            meta.setLore(List.of(ChatUtil.gray + "Right-click to select a player to teleport."));
            compass.setItemMeta(meta);
        }
        return compass;
    }
}
