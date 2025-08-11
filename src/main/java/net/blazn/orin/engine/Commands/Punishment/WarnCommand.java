package net.blazn.orin.engine.Commands.Punishment;

import net.blazn.orin.engine.Managers.PunishmentManager;
import net.blazn.orin.engine.Managers.PermissionsManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Managers.NameManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class WarnCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final RankManager rankManager;
    private final NameManager nameManager;
    private final PermissionsManager permissionsManager;
    private final PunishmentManager punishmentManager;
    private final List<String> allowedRanks = Arrays.asList("OWNER", "DEVELOPER", "ADMIN", "SRMOD", "MOD");

    public WarnCommand(JavaPlugin plugin, RankManager rankManager, NameManager nameManager, PermissionsManager permissionsManager, PunishmentManager punishmentManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.nameManager = nameManager;
        this.permissionsManager = permissionsManager;
        this.punishmentManager = punishmentManager;
        plugin.getCommand("warn").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // ✅ Ensure correct usage
        if (args.length < 2) {
            sender.sendMessage(ChatUtil.red + "Usage" +
                    ChatUtil.darkGray + ":" + ChatUtil.white + " /warn <player> <reason>");
            return true;
        }

        String targetName = args[0];

        // ✅ Check if the player exists in the database using NameManager
        if (!nameManager.playerExists(targetName)) {
            sender.sendMessage(ChatUtil.doesNotExist);
            return true;
        }

        // ✅ Retrieve target player (can be offline)
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        // ✅ Get correct display name using NameManager
        String targetDisplayName = nameManager.getDisplayName(target.getUniqueId());

        // ✅ Determine sender's rank (Console = "OWNER")
        String senderRank = (sender instanceof Player) ? rankManager.getRank((Player) sender) : "SERVER";
        String senderDisplayName = (sender instanceof Player) ? sender.getName() : "SERVER";

        // ✅ Determine target's rank
        String targetRank = (target.isOnline()) ? rankManager.getRank(target.getPlayer()) : "UNKNOWN";
        String targetRankColor = rankManager.getRankColor(targetRank);

        // ✅ Prevent lower-ranked staff from warning higher-ranked players (except console)
        if (sender instanceof Player && !permissionsManager.canModify(senderRank, targetRank)) {
            sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You cannot warn this player.");
            return true;
        }

        // ✅ Build & sanitize reason
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        if (reason.isEmpty()) {
            sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" +
                    ChatUtil.white + " /warn <player> <reason>");
            return true;
        }

        // ✅ Log warning in database
        punishmentManager.warnPlayer(targetName, senderDisplayName, reason);
        Bukkit.broadcastMessage(ChatUtil.darkRed + "⚠ " + targetRankColor + targetDisplayName +
                ChatUtil.white + " has been warned for" + ChatUtil.darkGray + ": " +
                ChatUtil.yellow + reason);

        return true;
    }
}
