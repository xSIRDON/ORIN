package net.blazn.orin.engine.Managers;

import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class WatchdogManager {

    private final JavaPlugin plugin;
    private final NameManager nameManager;
    private final TablistManager tablistManager;
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, Location> savedLocations = new HashMap<>();
    private final Map<UUID, GameMode> savedGameModes = new HashMap<>();
    private final Set<UUID> watchdogPlayers = new HashSet<>();

    public WatchdogManager(JavaPlugin plugin, NameManager nameManager, TablistManager tablistManager) {
        this.plugin = plugin;
        this.nameManager = nameManager;
        this.tablistManager = tablistManager;
    }

    /**
     * ✅ Toggles Watchdog Mode for a player.
     */
    public void toggleWatchdog(Player player) {
        if (isInWatchdog(player)) {
            disableWatchdog(player);
        } else {
            enableWatchdog(player);
        }
    }

    /**
     * ✅ Enables Watchdog Mode for a player.
     */
    public void enableWatchdog(Player player) {
        UUID playerId = player.getUniqueId();
        FileConfiguration config = plugin.getConfig();

        // Save inventory & location
        savedInventories.put(playerId, player.getInventory().getContents());
        savedLocations.put(playerId, player.getLocation());
        savedGameModes.put(playerId, player.getGameMode());

        // Clear inventory & enable flight
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.getInventory().clear();
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setGameMode(GameMode.SURVIVAL);
        } else {
            player.getInventory().clear();
        }

        // Hide player from others
        Bukkit.getOnlinePlayers().forEach(p -> p.hidePlayer(plugin, player));

        // Give player a player tracker compass
        player.getInventory().setItem(0, getPlayerTrackerCompass());

        // Mark player as in Watchdog Mode
        watchdogPlayers.add(playerId);

        player.sendMessage(ChatUtil.green + "✔" + ChatUtil. white + " You are now in Watchdog mode.");

        // Broadcast fake quit message
        String quitMessage = config.getString("messages.quit.format", "&c{displayname} left the game.");
        quitMessage = quitMessage.replace("{displayname}", nameManager.getDisplayName(player));
        Bukkit.broadcastMessage(ChatUtil.swapAmp(quitMessage));
        tablistManager.setTablistForAll("&b&lᴏʀɪɴ ɴᴇᴛᴡᴏʀᴋ\n&7ᴏɴʟɪɴᴇ ᴘʟᴀʏᴇʀѕ&8: &f" + (Bukkit.getOnlinePlayers().size()-1) + "\n ", " \n&eᴏʀɪɴ.ᴍᴏᴅᴅᴇᴅ.ꜰᴜɴ");
    }

    /**
     * ✅ Disables Watchdog Mode for a player.
     */
    public void disableWatchdog(Player player) {
        UUID playerId = player.getUniqueId();
        FileConfiguration config = plugin.getConfig();

        // Restore inventory
        if (savedInventories.containsKey(playerId)) {
            player.getInventory().setContents(savedInventories.get(playerId));
            savedInventories.remove(playerId);
        }

        // Restore location
        if (savedLocations.containsKey(playerId)) {
            player.teleport(savedLocations.get(playerId));
            savedLocations.remove(playerId);
        }

        // Restore gamemode
        if (savedGameModes.containsKey(playerId)) {
            player.setGameMode(savedGameModes.get(playerId));
            savedGameModes.remove(playerId);
        } else {
            player.setGameMode(GameMode.SURVIVAL);
        }

        // Reset flight (only if not in creative)
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }

        // Show player to all players
        Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(plugin, player));

        // Remove player from Watchdog Mode
        watchdogPlayers.remove(playerId);

        player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.white + " You have exited Watchdog mode.");

        // Broadcast fake join message
        String joinMessage = config.getString("messages.join.format", "&a{displayname} joined the game.");
        joinMessage = joinMessage.replace("{displayname}", nameManager.getDisplayName(player));
        Bukkit.broadcastMessage(ChatUtil.swapAmp(joinMessage));
        tablistManager.setTablistForAll("&b&lᴏʀɪɴ ɴᴇᴛᴡᴏʀᴋ\n&7ᴏɴʟɪɴᴇ ᴘʟᴀʏᴇʀѕ&8: &f" + Bukkit.getOnlinePlayers().size() + "\n ", " \n&eᴏʀɪɴ.ᴍᴏᴅᴅᴇᴅ.ꜰᴜɴ");
    }

    /**
     * ✅ Returns whether a player is in Watchdog Mode.
     */
    public boolean isInWatchdog(Player player) {
        return watchdogPlayers.contains(player.getUniqueId());
    }

    /**
     * ✅ Disables Watchdog Mode for all players.
     */
    public void disableAllWatchdogs() {
        new HashSet<>(watchdogPlayers).forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                disableWatchdog(player);
            }
        });
    }

    /**
     * ✅ Returns the Watchdog Tracker Compass.
     */
    private ItemStack getPlayerTrackerCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatUtil.bgold + "Player Tracker");
            meta.setLore(Collections.singletonList(ChatUtil.gray + "Right-click to select a player to teleport."));
            compass.setItemMeta(meta);
        }
        return compass;
    }

    public Set<Player> getAllWatchdogs() {
        Set<Player> watchdogPlayersSet = new HashSet<>();
        for (UUID uuid : watchdogPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                watchdogPlayersSet.add(player);
            }
        }
        return watchdogPlayersSet;
    }
}
