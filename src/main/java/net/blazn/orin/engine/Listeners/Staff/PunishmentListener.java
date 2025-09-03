package net.blazn.orin.engine.Listeners.Staff;

import net.blazn.orin.engine.Managers.PunishmentManager;
import net.blazn.orin.engine.Managers.NameManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class PunishmentListener implements Listener {

    private final PunishmentManager punishmentManager;
    private final NameManager nameManager;
    private final RankManager rankManager;
    private final JavaPlugin plugin;

    public PunishmentListener(JavaPlugin plugin, RankManager rankManager, PunishmentManager punishmentManager, NameManager nameManager) {
        this.rankManager = rankManager;
        this.punishmentManager = punishmentManager;
        this.nameManager = nameManager;
        this.plugin = plugin;
    }

    /** ✅ Prevents banned players from joining the server. */
    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (punishmentManager.isBanned(playerUUID)) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, formatBanMessage(playerUUID, true));
        } else if (punishmentManager.isTempBanned(playerUUID)) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, formatBanMessage(playerUUID, false));
        }
    }

    /** ✅ Sends appropriate ban messages. */
    private String formatBanMessage(UUID playerUUID, boolean permanent) {
        String reason = punishmentManager.getBanReason(playerUUID);
        String expiry = permanent ? "PERMANENTLY BANNED" : "TEMPORARILY BANNED";
        String appealURL = plugin.getConfig().getString("server-website") + "/appeal";

        return "§4⛔ §cYou have been§8: §f§l" + expiry + " §4⛔\n" +
                "§6§lReason§8: §f" + reason + (permanent ? "" : "\n§e§lExpires§8: §f" +
                punishmentManager.getBanExpiry(playerUUID).replace("/", ChatUtil.gray + "/" + ChatUtil.white)) + "\n" +
                "§5§lAppeal at§8: §f" + appealURL;
    }

    /** ✅ Re-applies mute status when a player joins. */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (punishmentManager.isMuted(playerUUID)) {
            player.sendMessage("§4⛔ §cYou are permanently muted. §4⛔");
        } else if (punishmentManager.isTempMuted(playerUUID)) {
            player.sendMessage("§4⛔ §cYou are temporarily muted.");
            player.sendMessage("§e§lExpires§8: §f" +
                    punishmentManager.getMuteExpiry(playerUUID).replace("/", ChatUtil.gray + "/" + ChatUtil.white));
        }
    }

    /** ✅ Prevents muted players from chatting. */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (punishmentManager.isMuted(playerUUID)) {
            event.setCancelled(true);
            player.sendMessage("§4❌ §cYou cannot chat while muted.");
        } else if (punishmentManager.isTempMuted(playerUUID)) {
            event.setCancelled(true);
            player.sendMessage("§4❌ §cYou cannot chat while muted.");
        }
    }

    /** ✅ Handles punishment menu selections. */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player staff = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        String menuTitle = event.getView().getTitle();

        if (clickedItem == null || clickedItem.getType() == Material.AIR || clickedItem.getType() == Material.RED_STAINED_GLASS_PANE) {
            return;
        }

        // ✅ Prevent moving window/pane items
        Set<Material> protectedItems = Set.of(
                Material.WHITE_STAINED_GLASS_PANE,
                Material.ORANGE_STAINED_GLASS_PANE,
                Material.MAGENTA_STAINED_GLASS_PANE,
                Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE,
                Material.LIME_STAINED_GLASS_PANE,
                Material.PINK_STAINED_GLASS_PANE,
                Material.GRAY_STAINED_GLASS_PANE,
                Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                Material.CYAN_STAINED_GLASS_PANE,
                Material.PURPLE_STAINED_GLASS_PANE,
                Material.BLUE_STAINED_GLASS_PANE,
                Material.BROWN_STAINED_GLASS_PANE,
                Material.GREEN_STAINED_GLASS_PANE,
                Material.RED_STAINED_GLASS_PANE,
                Material.BLACK_STAINED_GLASS_PANE
        );

        if (protectedItems.contains(clickedItem.getType())) {
            event.setCancelled(true);
            return;
        }

        if (menuTitle.equals(ChatUtil.bred + "Select Kick Reason")) {
            handleKickSelection(staff, clickedItem);
        } else if (menuTitle.equals(ChatUtil.bred + "Select Ban Reason")) {
            handleBanReasonSelection(staff, clickedItem);
        } else if (menuTitle.equals(ChatUtil.bred + "Select Ban Duration")) {
            handleBanDurationSelection(staff, clickedItem);
        } else if (menuTitle.equals(ChatUtil.bred + "Select Mute Reason")) {
            handleMuteSelection(staff, clickedItem);
        } else if (menuTitle.equals(ChatUtil.bred + "Select Mute Duration")) {
            handleMuteDurationSelection(staff, clickedItem);
        }

        event.setCancelled(true);
    }

    private void handleKickSelection(Player staff, ItemStack clickedItem) {
        UUID targetUUID = punishmentManager.getPendingPunishmentTarget(staff, PunishmentManager.PunishmentType.KICK);
        if (targetUUID == null) {
            staff.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " No target found for kick.");
            return;
        }

        String reason = ChatUtil.stripSec(clickedItem.getItemMeta().getDisplayName());
        executeKick(staff, targetUUID, reason);
        staff.closeInventory();
    }

    private void handleBanReasonSelection(Player staff, ItemStack clickedItem) {
        UUID targetUUID = punishmentManager.getPendingPunishmentTarget(staff, PunishmentManager.PunishmentType.BAN);
        if (targetUUID == null) {
            staff.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " No target found for ban.");
            return;
        }

        punishmentManager.setPendingPunishment(PunishmentManager.PunishmentType.BAN, targetUUID, ChatUtil.stripSec(clickedItem.getItemMeta().getDisplayName()));
        openBanDurationMenu(staff);
    }

    private void handleBanDurationSelection(Player staff, ItemStack clickedItem) {
        UUID targetUUID = punishmentManager.getPendingPunishmentTarget(staff, PunishmentManager.PunishmentType.BAN);
        if (targetUUID == null) {
            staff.sendMessage(ChatUtil.doesNotExist);
            return;
        }

        String durationText = ChatUtil.stripSec(clickedItem.getItemMeta().getDisplayName());
        long duration = parsePunishmentDuration(durationText);
        String reason = punishmentManager.getPendingPunishment(targetUUID, PunishmentManager.PunishmentType.BAN);

        if (reason == null || reason.equals("PENDING")) {
            staff.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " Ban reason not set.");
            return;
        }

        executeBan(staff, targetUUID, reason, duration, durationText);
        staff.closeInventory();
    }

    private void handleMuteDurationSelection(Player staff, ItemStack clickedItem) {
        UUID targetUUID = punishmentManager.getPendingPunishmentTarget(staff, PunishmentManager.PunishmentType.MUTE);
        if (targetUUID == null) {
            staff.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " No target found for mute.");
            return;
        }

        String durationText = ChatUtil.stripSec(clickedItem.getItemMeta().getDisplayName());
        long duration = parsePunishmentDuration(durationText);
        String reason = punishmentManager.getPendingPunishment(targetUUID, PunishmentManager.PunishmentType.MUTE);

        if (reason == null || reason.equals("PENDING")) {
            staff.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " Mute reason not set.");
            return;
        }

        executeMute(staff, targetUUID, reason, duration, durationText);
        staff.closeInventory();
    }

    private void handleMuteSelection(Player staff, ItemStack clickedItem) {
        UUID targetUUID = punishmentManager.getPendingPunishmentTarget(staff, PunishmentManager.PunishmentType.MUTE);

        if (targetUUID == null) {
            staff.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " No target found for mute.");
            return;
        }

        String reason = ChatUtil.stripSec(clickedItem.getItemMeta().getDisplayName());

        // ✅ Store the reason and open the mute duration menu
        punishmentManager.setPendingPunishment(PunishmentManager.PunishmentType.MUTE, targetUUID, reason);
        openMuteDurationMenu(staff);
    }


    private void executeKick(Player staff, UUID targetUUID, String reason) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);

        if (target.isOnline()) {
            punishmentManager.kickPlayer(target.getPlayer(), staff.getName(), reason);
            broadcastPunishment(targetUUID, "KICK", reason);
        } else {
            staff.sendMessage(ChatUtil.notOnline);
        }

        punishmentManager.clearPendingPunishment(targetUUID, PunishmentManager.PunishmentType.KICK);
    }

    private void executeBan(Player staff, UUID targetUUID, String reason, long duration, String durationText) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        String staffName = staff.getName();

        if (duration == -1) {
            punishmentManager.banPlayer(target.getName(), staffName, reason);
        } else {
            punishmentManager.tempBanPlayer(target.getName(), staffName, reason, duration * 1000);
        }

        broadcastPunishment(targetUUID, "BAN", reason + ChatUtil.gray + " (" + ChatUtil.white + (duration == -1 ? "Permanent" : durationText) + ChatUtil.gray + ")");
        punishmentManager.clearPendingPunishment(targetUUID, PunishmentManager.PunishmentType.BAN);
    }

    private void executeMute(Player staff, UUID targetUUID, String reason, long duration, String durationText) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        String staffName = staff.getName();

        if (duration == -1) {
            punishmentManager.mutePlayer(target.getName(), staffName, reason);
        } else {
            punishmentManager.tempMutePlayer(target.getName(), staffName, reason, duration * 1000);
        }

        Bukkit.broadcastMessage(ChatUtil.bred + "⛏ " + ChatUtil.darkGray + "| " + nameManager.getDisplayName(targetUUID) +
                ChatUtil.darkRed + " has been muted for" + ChatUtil.darkGray + ": " +
                ChatUtil.gold + reason + ChatUtil.gray + " (" + ChatUtil.white + (duration == -1 ? "Permanent" : durationText) +
                ChatUtil.gray + ")");

        punishmentManager.clearPendingPunishment(targetUUID, PunishmentManager.PunishmentType.MUTE);
    }

    private void broadcastPunishment(UUID targetUUID, String type, String reason) {
        String targetName = nameManager.getDisplayName(targetUUID);
        String rankPrefix = rankManager.getRankColor(rankManager.getRank(targetUUID));

        if (type.equals("KICK")) {
            Bukkit.broadcastMessage(ChatUtil.bred + "⛏ " + ChatUtil.darkGray + "| " + rankPrefix + targetName +
                    ChatUtil.darkRed + " has been " + type.toLowerCase() + "ed for" + ChatUtil.darkGray + ": " +
                    ChatUtil.gold + reason);
        } else if (type.equals("BAN")) {
            Bukkit.broadcastMessage(ChatUtil.bred + "⛏ " + ChatUtil.darkGray + "| " + rankPrefix + targetName +
                    ChatUtil.darkRed + " has been " + type.toLowerCase() + "ned for" + ChatUtil.darkGray + ": " +
                    ChatUtil.gold + reason);
        }
    }

    private void openBanDurationMenu(Player player) {
        Inventory durationMenu = Bukkit.createInventory(null, 9, ChatUtil.bred + "Select Ban Duration");

        List<String> durations = plugin.getConfig().getStringList("punishments.durations"); // ✅ Fetch durations from config.yml

        for (String duration : durations) {
            ItemStack item = new ItemStack(Material.CLOCK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatUtil.byellow + duration);
                meta.setLore(List.of(ChatUtil.gray + "Click to select this ban duration."));
                item.setItemMeta(meta);
            }
            durationMenu.addItem(item);
        }

        player.openInventory(durationMenu);
    }

    private void openMuteDurationMenu(Player player) {
        Inventory durationMenu = Bukkit.createInventory(null, 9, ChatUtil.bred + "Select Mute Duration");

        List<String> durations = plugin.getConfig().getStringList("punishments.durations"); // ✅ Fetch durations from config.yml

        for (String duration : durations) {
            ItemStack item = new ItemStack(Material.CLOCK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatUtil.byellow + duration);
                meta.setLore(List.of(ChatUtil.gray + "Click to select this mute duration."));
                item.setItemMeta(meta);
            }
            durationMenu.addItem(item);
        }

        player.openInventory(durationMenu);
    }

    private long parsePunishmentDuration(String durationText) {
        FileConfiguration config = plugin.getConfig();
        List<String> durations = config.getStringList("punishments.durations");

        // ✅ Create a mapping of durations to seconds
        Map<String, Long> durationMap = new HashMap<>();
        durationMap.put("30 Minutes", 1800L);
        durationMap.put("1 Hour", 3600L);
        durationMap.put("12 Hours", 43200L);
        durationMap.put("1 Day", 86400L);
        durationMap.put("1 Week", 604800L);
        durationMap.put("2 Weeks", 1209600L);
        durationMap.put("1 Month", 2592000L);
        durationMap.put("1 Year", 31536000L);
        durationMap.put("Permanent", -1L);

        return durationMap.getOrDefault(durationText, 0L); // ✅ Return the corresponding duration or 0 if invalid
    }

}
