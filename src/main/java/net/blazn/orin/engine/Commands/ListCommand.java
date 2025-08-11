package net.blazn.orin.engine.Commands;

import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Managers.WatchdogManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ListCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final RankManager rankManager;
    private final WatchdogManager watchdogManager;

    private final Set<String> staffRanks = Set.of("OWNER", "DEVELOPER", "ADMIN", "SRMOD", "MOD");

    public ListCommand(JavaPlugin plugin, RankManager rankManager, WatchdogManager watchdogManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.watchdogManager = watchdogManager;
        plugin.getCommand("list").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        List<Player> allOnline = new ArrayList<>(Bukkit.getOnlinePlayers());

        // Get sender rank
        String senderRank = (sender instanceof Player p) ? rankManager.getRank(p) : "OWNER";
        boolean isStaff = staffRanks.contains(senderRank);

        // Separate normal players and watchdog players
        List<Player> visiblePlayers = allOnline.stream()
                .filter(p -> !watchdogManager.isInWatchdog(p))
                .toList();

        List<Player> watchdogPlayers = allOnline.stream()
                .filter(watchdogManager::isInWatchdog)
                .toList();

        boolean usePrefixes = rankManager.shouldUseChatPrefixes();

        List<String> visibleNames = visiblePlayers.stream()
                .map(p -> {
                    String rank = rankManager.getRank(p);
                    String name = p.getDisplayName();
                    if (usePrefixes) {
                        String prefix = rankManager.getRankPrefix(rank);
                        return ChatUtil.swapAmp(prefix) + ChatUtil.white + ChatUtil.stripSec(name);
                    } else {
                        return p.getDisplayName();
                    }
                })
                .collect(Collectors.toList());

        List<String> watchdogNames = watchdogPlayers.stream()
                .map(p -> {
                    String rank = rankManager.getRank(p);
                    String name = p.getDisplayName();
                    if (usePrefixes) {
                        String prefix = rankManager.getRankPrefix(rank);
                        return ChatUtil.swapAmp(prefix) + ChatUtil.white + ChatUtil.stripSec(name);
                    } else {
                        return p.getDisplayName();
                    }
                })
                .collect(Collectors.toList());

        // Send Online Players section
        sender.sendMessage(" ");
        sender.sendMessage(ChatUtil.darkGray + "─── " +ChatUtil.darkGreen + "Online Players " +
                ChatUtil.gray + "(" + ChatUtil.white + visibleNames.size() + ChatUtil.gray + ")" + ChatUtil.darkGray + " ───");
        if (!visibleNames.isEmpty()) {
            sender.sendMessage(ChatUtil.white + String.join(ChatUtil.darkGray + ", " + ChatUtil.white, visibleNames));
        } else {
            sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " No visible players online.");
        }

        // Send Watchdog Mode section — only if staff
        if (isStaff) {
            if (!watchdogNames.isEmpty()) {
                sender.sendMessage(" ");
                sender.sendMessage(ChatUtil.darkGray + "─── " + ChatUtil.bred + "Watchdog Mode " +
                        ChatUtil.gray + "(" + ChatUtil.white + watchdogNames.size() + ChatUtil.gray + ")" + ChatUtil.darkGray + " ───");
                sender.sendMessage(ChatUtil.gray + String.join(ChatUtil.darkGray + ", " + ChatUtil.gray, watchdogNames));
            }
        }

        sender.sendMessage(" ");
        return true;
    }
}
