package net.blazn.orin.engine.Listeners;

import net.blazn.orin.engine.Commands.Staff.StaffCommand;
import net.blazn.orin.engine.Managers.NameManager;
import net.blazn.orin.engine.Managers.PermissionsManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatListener implements Listener {

    private final RankManager rankManager;
    private final NameManager nameManager;
    private final PermissionsManager permissionsManager;
    private final JavaPlugin plugin;
    private final Map<String, String> rankChatColors = new HashMap<>();

    public ChatListener(JavaPlugin plugin, RankManager rankManager, NameManager nameManager, PermissionsManager permissionsManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.nameManager = nameManager;
        this.permissionsManager = permissionsManager;
        loadRankChatColors(); // Load rank chat colors from config
    }

    /**
     * Loads all rank chat colors from config.yml dynamically.
     */
    private void loadRankChatColors() {
        File ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(ranksFile);
        if (!config.contains("rank-chat-colors")) {
            plugin.getLogger().warning("No rank chat colors found in config.yml. Generating defaults...");
            setupDefaultRankColors(config);
        }

        rankChatColors.clear();
        for (String rank : rankManager.getRanks()) {
            String color = config.getString("rank-chat-colors." + rank.toUpperCase(), "§7"); // Default gray
            rankChatColors.put(rank.toUpperCase(), color);
        }
    }

    /**
     * Generates default rank chat colors if missing from config.yml.
     */
    private void setupDefaultRankColors(FileConfiguration config) {
        List<String> ranks = rankManager.getRanks();

        for (String rank : ranks) {
            config.set("rank-chat-colors." + rank.toUpperCase(), "§7"); // Default gray
        }

        plugin.saveConfig();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String rank = rankManager.getRank(player);
        String displayName = nameManager.getDisplayName(player);
        FileConfiguration config = plugin.getConfig();

        if (config.getString("server-gamemode").equalsIgnoreCase("Hub")) {
            String rankColor = rankChatColors.getOrDefault(rank.toUpperCase(), "&7");
            boolean usePrefixFormat = rankManager.shouldUseChatPrefixes();
            String staffMessage = "";
            
            if (StaffCommand.isStaffChatEnabled(player)) {
                event.setCancelled(true);
                if (usePrefixFormat) {
                    if (!rank.equals("MEMBER")) {
                        String prefix = rankManager.getRankPrefix(rank);
                        staffMessage = ChatUtil.staffPrefix + ChatUtil.swapAmp(prefix) + ChatUtil.white + ChatUtil.stripSec(displayName) + ChatUtil.darkGray + ": " + ChatUtil.aqua + event.getMessage();
                    } else {
                        String prefix = rankManager.getRankPrefix(rank);
                        staffMessage = ChatUtil.staffPrefix + ChatUtil.swapAmp(prefix) + ChatUtil.gray + ChatUtil.stripSec(displayName) + ChatUtil.darkGray + ": " + ChatUtil.aqua + event.getMessage();
                    }
                } else {
                    staffMessage = ChatUtil.staffPrefix + ChatUtil.bgold + displayName + ChatUtil.darkGray + ": " + ChatUtil.aqua + event.getMessage();
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (permissionsManager.getRankLevel(rankManager.getRank(p)) >= 6) {
                        p.sendMessage(staffMessage);
                    }
                }
                return;
            }

            String formattedChat;
            if (usePrefixFormat) {
                String prefix = rankManager.getRankPrefix(rank);
                formattedChat = ChatUtil.swapAmp(prefix) + ChatUtil.white + ChatUtil.stripSec(displayName) + ChatUtil.darkGray + ": " + ChatUtil.swapAmp(rankColor) + event.getMessage();
            } else {
                String chatFormat = config.getString("chat.format", "{displayname}&8: {message}");
                chatFormat = chatFormat
                        .replace("{displayname}", displayName)
                        .replace("{message}", ChatUtil.swapAmp(rankColor) + event.getMessage());
                formattedChat = ChatUtil.swapAmp(chatFormat);
            }

            event.setFormat(formattedChat);
        }
    }

    @EventHandler
    public void onBlockedCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String rank = rankManager.getRank(player);
        int playerRankLevel = permissionsManager.getRankLevel(rank);

        // Commands to block if rank is below HELPER (level 3 for example)
        List<String> blockedCommands = List.of("/kick", "/ban", "/mute", "/rank", "/tempban", "/tempmute", "/gmc", "/gamemode", "/heal",
                "/staff", "/history", "/wd", "/watchdog", "/gms", "/warn", "/unban", "/unmute", "/setmotd", "/teleport", "/tp", "/kill", "/clear",
                "/fly", "/godmode", "/god");

        // Extract the first word of the command (e.g., "/kick", not "/kick Bob")
        String input = event.getMessage().toLowerCase().trim();
        String firstWord = input.split("\\s+")[0];

        // If it's a blocked command and the player's rank is below HELPER
        if (blockedCommands.contains(firstWord) && playerRankLevel < permissionsManager.getRankLevel("HELPER")) {
            event.setCancelled(true);
            player.sendMessage(ChatUtil.fakeCommand(firstWord));
        }
    }

}
