package net.blazn.orin.engine.Commands.Operator;

import net.blazn.orin.engine.Managers.PermissionsManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.UUID;

public class GodmodeCommand implements CommandExecutor {

    private final RankManager rankManager;
    private final PermissionsManager permissionsManager;
    private final JavaPlugin plugin;
    private final List<String> allowedRanks = Arrays.asList("OWNER", "DEVELOPER", "ADMIN", "SRMOD");
    private final HashSet<UUID> godModePlayers = new HashSet<>();

    public GodmodeCommand(JavaPlugin plugin, RankManager rankManager, PermissionsManager permissionsManager) {
        this.rankManager = rankManager;
        this.permissionsManager = permissionsManager;
        this.plugin = plugin;
        plugin.getCommand("godmode").setExecutor(this);
        plugin.getCommand("god").setExecutor(this); // Alias
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // If no arguments, ensure sender is a player before toggling their own godmode
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" + ChatUtil.white + " /godmode <player>");
                return true;
            }
            Player player = (Player) sender;
            toggleGodMode(player, player);
            return true;
        }

        // Try to get target player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatUtil.notOnline);
            return true;
        }

        // If sender is console, allow toggling godmode without restrictions
        if (!(sender instanceof Player)) {
            toggleGodMode(sender, target);
            return true;
        }

        // If sender is a player, apply rank permissions
        Player player = (Player) sender;
        String playerRank = rankManager.getRank(player);
        String targetRank = rankManager.getRank(target);

        // Permission check
        if (!allowedRanks.contains(playerRank)) {
            player.sendMessage(ChatUtil.noPermission);
            return true;
        }

        // Prevent lower-ranked players from modifying godmode of higher-ranked ones
        if (!permissionsManager.canModify(playerRank, targetRank)) {
            player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You cannot modify this player's godmode.");
            return true;
        }

        // Perform godmode toggle
        toggleGodMode(player, target);
        return true;
    }

    /**
     * Toggles godmode for the given target player.
     */
    private void toggleGodMode(CommandSender sender, Player target) {
        boolean isGod = godModePlayers.contains(target.getUniqueId());

        if (isGod) {
            godModePlayers.remove(target.getUniqueId());
            target.setInvulnerable(false);
        } else {
            godModePlayers.add(target.getUniqueId());
            target.setHealth(20);
            target.setFoodLevel(20);
            target.setInvulnerable(true);
        }

        String status = isGod ? ChatUtil.bred + "DISABLED" : ChatUtil.bgreen + "ENABLED";
        target.sendMessage(ChatUtil.bgold + "⚡" + ChatUtil.white + " Godmode" + ChatUtil.darkGray + ": " + status);

        if (!sender.equals(target)) {
            sender.sendMessage(ChatUtil.bgold + "⚡" + ChatUtil.white + " You have " + status + ChatUtil.white + " godmode for" + ChatUtil.darkGray + ": " + target.getDisplayName());
        }
    }
}
