package net.blazn.orin.engine.Commands;

import net.blazn.orin.engine.Managers.NameManager;
import net.blazn.orin.engine.Managers.PermissionsManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Managers.WatchdogManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class ListCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final RankManager rankManager;
    private final NameManager nameManager;
    private final PermissionsManager permissionsManager;
    private final WatchdogManager watchdogManager;

    private final Set<String> staffRanks = Set.of("OWNER", "DEVELOPER", "ADMIN", "MOD", "HELPER");

    public ListCommand(JavaPlugin plugin, RankManager rankManager, NameManager nameManager, PermissionsManager permissionsManager, WatchdogManager watchdogManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.nameManager = nameManager;
        this.permissionsManager = permissionsManager;
        this.watchdogManager = watchdogManager;
        plugin.getCommand("list").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        List<Player> allOnline = new ArrayList<>(Bukkit.getOnlinePlayers());

        // Get sender rank
        String senderRank = (sender instanceof Player p) ? rankManager.getRank(p) : "OWNER";
        boolean isStaff = staffRanks.contains(senderRank);

        boolean usePrefixes = rankManager.shouldUseChatPrefixes();

        List<String> visibleNames = allOnline.stream()
                .filter(p -> !watchdogManager.isInWatchdog(p))
                .map(p -> formatPlayerName(p, senderRank, usePrefixes))
                .collect(Collectors.toList());

        List<String> watchdogNames = allOnline.stream()
                .filter(watchdogManager::isInWatchdog)
                .map(p -> formatPlayerName(p, senderRank, usePrefixes))
                .collect(Collectors.toList());

        // Send Online Players section
        sender.sendMessage(" ");
        sender.sendMessage(ChatUtil.darkGray + "─── " + ChatUtil.darkGreen + "Online Players " +
                ChatUtil.gray + "(" + ChatUtil.white + visibleNames.size() + ChatUtil.gray + ")" + ChatUtil.darkGray + " ───");
        if (!visibleNames.isEmpty()) {
            sender.sendMessage(ChatUtil.white + String.join(ChatUtil.darkGray + ", " + ChatUtil.white, visibleNames));
        } else {
            sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " No visible players online.");
        }

        // Send Watchdog Mode section — only if staff
        if (isStaff && !watchdogNames.isEmpty()) {
            sender.sendMessage(" ");
            sender.sendMessage(ChatUtil.darkGray + "─── " + ChatUtil.bred + "Watchdog Mode " +
                    ChatUtil.gray + "(" + ChatUtil.white + watchdogNames.size() + ChatUtil.gray + ")" + ChatUtil.darkGray + " ───");
            sender.sendMessage(ChatUtil.gray + String.join(ChatUtil.darkGray + ", " + ChatUtil.gray, watchdogNames));
        }

        sender.sendMessage(" ");
        return true;
    }

    private String formatPlayerName(Player player, String viewerRank, boolean usePrefixes) {
        String displayName = nameManager.getDisplayName(player); // current display name (disguised or normal)
        String rank = rankManager.getRank(player);

        // Only show parentheses if viewer is HELPER+ and the player is currently disguised
        if (permissionsManager.getRankLevel(viewerRank) >= permissionsManager.getRankLevel("HELPER")
                && rankManager.isDisguised(player)) {

            // Use nickname if exists, otherwise real name
            String realOrNickname = nameManager.getNickname(player);
            String playerRank = rankManager.getRank(player.getUniqueId());
            if (realOrNickname == null || realOrNickname.isEmpty()) {
                realOrNickname = rankManager.getRankPrefix(playerRank) + player.getName(); // fallback to real name
            }

            displayName = displayName + ChatUtil.gray + " (" + ChatUtil.white + realOrNickname + ChatUtil.gray + ")";
        }

        if (usePrefixes) {
            String prefix = rankManager.getRankPrefix(rank);
            return ChatUtil.swapAmp(prefix) + ChatUtil.white + ChatUtil.stripSec(displayName);
        } else {
            return displayName;
        }
    }
}
