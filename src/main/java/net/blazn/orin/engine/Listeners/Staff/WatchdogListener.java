package net.blazn.orin.engine.Listeners.Staff;

import net.blazn.orin.engine.Managers.NameManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Managers.WatchdogManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class WatchdogListener implements Listener {

    private final JavaPlugin plugin;
    private final RankManager rankManager;
    private final NameManager nameManager;
    private final WatchdogManager watchdogManager;

    private final Map<Player, Long> lastMessageTime = new HashMap<>();
    private final long MESSAGE_COOLDOWN = 5000; // 5 seconds

    public WatchdogListener(JavaPlugin plugin, RankManager rankManager, NameManager nameManager, WatchdogManager watchdogManager) {
        this.rankManager = rankManager;
        this.nameManager = nameManager;
        this.plugin = plugin;
        this.watchdogManager = watchdogManager;
    }

    private boolean canSendMessage(Player player) {
        long now = System.currentTimeMillis();
        long last = lastMessageTime.getOrDefault(player, 0L);
        if (now - last >= MESSAGE_COOLDOWN) {
            lastMessageTime.put(player, now);
            return true;
        }
        return false;
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

            joiningPlayer.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " You are still in watchdog mode.");
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
                // ✅ Add disguised lore if the player is disguised
                if (rankManager.isDisguised(target)) {
                    String disguise = nameManager.getNickname(target.getUniqueId());
                    if (disguise != null && !disguise.isEmpty()) {
                        lore.add(ChatUtil.gray + "Disguised player: " + ChatUtil.white + disguise + ChatUtil.gray);
                    }
                }
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
     * ✅ Prevents Watchdog players from breaking blocks.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (watchdogManager.isInWatchdog(player)) {
            event.setCancelled(true);
            if (canSendMessage(player)) {
                player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You cannot break blocks while in watchdog mode.");
            }
        }
    }

    /**
     * ✅ Prevents Watchdog players from placing blocks.
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (watchdogManager.isInWatchdog(player)) {
            event.setCancelled(true);
            if (canSendMessage(player)) {
                player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You cannot place blocks while in watchdog mode.");
            }
        }
    }

    /**
     * ✅ Prevents Watchdog players from changing gamemode.
     */
    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (watchdogManager.isInWatchdog(player)) {
            event.setCancelled(true);
            //player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.white + " You cannot change gamemode while in watchdog mode.");
        }
    }

    /**
     * ✅ Prevents Watchdog players from dropping items.
     */
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (watchdogManager.isInWatchdog(player)) {
            event.setCancelled(true);
            if (canSendMessage(player)) {
                player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You cannot drop items while in watchdog mode.");
            }
        }
    }

    /**
     * ✅ Prevents Watchdog players from picking up items.
     * Use EntityPickupItemEvent for 1.13+, PlayerPickupItemEvent for older versions.
     */
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && watchdogManager.isInWatchdog(player)) {
            event.setCancelled(true);
            //player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.white + " You cannot pick up items while in watchdog mode.");
        }
    }

    /**
     * ✅ Prevents Watchdog players from moving items around in their own inventory.
     */
    @EventHandler
    public void onInventoryClickGeneral(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (watchdogManager.isInWatchdog(player)) {
            // Already handled separately for the Watchdog GUI, so skip that
            if (event.getView().getTitle().equals(ChatUtil.bgold + "ᴡᴀᴛᴄʜᴅᴏɢ")) return;
            event.setCancelled(true);
            if (canSendMessage(player)) {
                player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You cannot drop items while in watchdog mode.");
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
