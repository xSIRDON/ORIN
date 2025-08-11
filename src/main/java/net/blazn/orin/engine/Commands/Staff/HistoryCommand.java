package net.blazn.orin.engine.Commands.Staff;

import net.blazn.orin.engine.Managers.NameManager;
import net.blazn.orin.engine.Managers.PunishmentManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class HistoryCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final PunishmentManager punishmentManager;
    private final RankManager rankManager;
    private final NameManager nameManager;
    private final List<String> allowedRanks = Arrays.asList("OWNER", "DEVELOPER", "ADMIN", "SRMOD", "MOD");

    public HistoryCommand(JavaPlugin plugin, PunishmentManager punishmentManager, RankManager rankManager, NameManager nameManager) {
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
        this.rankManager = rankManager;
        this.nameManager = nameManager;
        plugin.getCommand("history").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // ✅ Validate arguments
        if (args.length < 1) {
            sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" + ChatUtil.white + " /history <player>");
            return true;
        }

        // ✅ Console check - Console is always allowed
        boolean isConsole = !(sender instanceof Player);
        if (!isConsole) {
            Player player = (Player) sender;
            String playerRank = rankManager.getRank(player);

            // ✅ Ensure player has permission (Moderator+)
            if (!allowedRanks.contains(playerRank)) {
                player.sendMessage(ChatUtil.noPermission);
                return true;
            }
        }

        String targetName = args[0];

        // ✅ Check if the player exists in the database
        if (!nameManager.playerExists(targetName)) {
            sender.sendMessage(ChatUtil.doesNotExist);
            return true;
        }

        // ✅ Retrieve target player's UUID & Display Name from database
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();
        String displayName = nameManager.getDisplayName(targetUUID); // ✅ Get Display Name (nickname or real name)
        String rankPrefix = rankManager.getRankColor(rankManager.getRank(targetUUID));

        // ✅ Get punishment history from the database
        Map<String, List<String>> history = punishmentManager.getPunishmentHistory(targetUUID);

        // ✅ Check if player has no punishments at all
        boolean hasPunishments = history.values().stream().anyMatch(list -> !list.isEmpty());

        if (!hasPunishments) {
            sender.sendMessage(ChatUtil.green + "✔ " + rankPrefix + displayName + ChatUtil.white + " has no punishment history.");
            return true;
        }

        // ✅ Display categorized punishment history
        sender.sendMessage(ChatUtil.darkGray + "─── " + ChatUtil.yellow + "Punishment History " +
                ChatUtil.darkGray + "| " + rankPrefix + displayName + ChatUtil.darkGray + " ───");

        for (Map.Entry<String, List<String>> categoryEntry : history.entrySet()) {
            String category = categoryEntry.getKey();
            List<String> punishments = categoryEntry.getValue();

            if (!punishments.isEmpty()) {
                sender.sendMessage( ChatUtil.bgold + category.replace("_", " ") + ChatUtil.darkGray + " [" + ChatUtil.white + punishments.size() + ChatUtil.darkGray + "]");
                for (String punishment : punishments) {
                    sender.sendMessage(ChatUtil.darkGray + "  - " + ChatUtil.white + punishment);
                }
            }
        }

        return true;
    }
}
