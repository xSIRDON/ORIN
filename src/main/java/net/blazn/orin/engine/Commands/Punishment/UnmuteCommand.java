package net.blazn.orin.engine.Commands.Punishment;

import net.blazn.orin.engine.Managers.*;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class UnmuteCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final RankManager rankManager;
    private final NameManager nameManager;
    private final PermissionsManager permissionsManager;
    private final PunishmentManager punishmentManager;

    public UnmuteCommand(JavaPlugin plugin, RankManager rankManager, NameManager nameManager, PermissionsManager permissionsManager, PunishmentManager punishmentManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.nameManager = nameManager;
        this.permissionsManager = permissionsManager;
        this.punishmentManager = punishmentManager;
        plugin.getCommand("unmute").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // ✅ Ensure correct usage
        if (args.length < 1) {
            sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" +
                    ChatUtil.white + " /unmute <player>");
            return true;
        }

        // ✅ Ensure sender is ADMIN+
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String senderRank = rankManager.getRank(player);

            if (permissionsManager.getRankLevel(senderRank) < permissionsManager.getRankLevel("ADMIN")) {
                sender.sendMessage(ChatUtil.noPermission);
                return true;
            }
        }

        String targetName = args[0];

        // ✅ Check if player exists in the database using NameManager
        if (!nameManager.playerExists(targetName)) {
            sender.sendMessage(ChatUtil.doesNotExist);
            return true;
        }

        // ✅ Retrieve target player (offline players included)
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();

        // ✅ Get correct display name using NameManager
        String targetDisplayName = nameManager.getDisplayName(targetUUID);
        String rankPrefix = rankManager.getRankColor(rankManager.getRank(targetUUID));

        // ✅ Ensure the player is actually muted
        if (!punishmentManager.isMuted(targetUUID) && !punishmentManager.isTempMuted(targetUUID)) {
            sender.sendMessage(ChatUtil.red + "❌ " + rankPrefix + targetDisplayName + ChatUtil.white + " is not muted.");
            return true;
        }

        // ✅ Unmute the player
        String staffName = (sender instanceof Player) ? sender.getName() : "SERVER";
        punishmentManager.unmutePlayer(targetName, staffName);

        // ✅ Announce unmute
        sender.sendMessage(ChatUtil.green + "✔ " + rankPrefix + targetDisplayName +
                ChatUtil.white + " has been unmuted.");

        return true;
    }
}
