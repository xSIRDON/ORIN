package net.blazn.orin.engine.Commands.Staff;

import net.blazn.orin.engine.Managers.PunishmentManager;
import net.blazn.orin.engine.Managers.NameManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class ClearHistoryCommand implements CommandExecutor {

    private final PunishmentManager punishmentManager;
    private final RankManager rankManager;
    private final NameManager nameManager;
    private static final List<String> ALLOWED_RANKS = List.of("OWNER", "DEVELOPER", "ADMIN");

    public ClearHistoryCommand(JavaPlugin plugin, PunishmentManager punishmentManager, RankManager rankManager, NameManager nameManager) {
        this.punishmentManager = punishmentManager;
        this.rankManager = rankManager;
        this.nameManager = nameManager;
        plugin.getCommand("clearhistory").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ": " + ChatUtil.white + "/clearhistory <player>");
            return true;
        }

        // ✅ Ensure sender has permission (Console is always allowed)
        if (sender instanceof Player player && !ALLOWED_RANKS.contains(rankManager.getRank(player))) {
            sender.sendMessage(ChatUtil.noPermission);
            return true;
        }

        // ✅ Retrieve target player
        String targetName = args[0];
        if (!nameManager.playerExists(targetName)) {
            sender.sendMessage(ChatUtil.doesNotExist);
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();
        String playerName = nameManager.getDisplayName(targetUUID);
        String rankPrefix = rankManager.getRankColor(rankManager.getRank(targetUUID));

        // ✅ Clear history in database
        punishmentManager.clearHistory(targetUUID);
        sender.sendMessage(ChatUtil.green + "✔ " + ChatUtil.white + "Cleared punishment history for " +
                ChatUtil.darkGray + rankPrefix + playerName);
        return true;
    }
}
