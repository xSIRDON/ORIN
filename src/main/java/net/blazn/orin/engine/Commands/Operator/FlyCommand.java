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

public class FlyCommand implements CommandExecutor {

    private final RankManager rankManager;
    private final PermissionsManager permissionsManager;
    private final JavaPlugin plugin;
    private final List<String> allowedRanks = Arrays.asList("OWNER", "DEVELOPER", "ADMIN", "SRMOD");

    public FlyCommand(JavaPlugin plugin, RankManager rankManager, PermissionsManager permissionsManager) {
        this.rankManager = rankManager;
        this.permissionsManager = permissionsManager;
        this.plugin = plugin;
        plugin.getCommand("fly").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // If no arguments, ensure sender is a player before toggling their own flight
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" + ChatUtil.white + " /fly <player>");
                return true;
            }
            Player player = (Player) sender;
            toggleFlight(player, player);
            return true;
        }

        // Try to get target player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatUtil.notOnline);
            return true;
        }

        // If sender is console, allow toggling flight without restrictions
        if (!(sender instanceof Player)) {
            toggleFlight(sender, target);
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

        // Prevent lower-ranked players from modifying flight of higher-ranked ones
        if (!permissionsManager.canModify(playerRank, targetRank)) {
            player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You cannot modify this player's flight.");
            return true;
        }

        // Perform flight toggle
        toggleFlight(player, target);
        return true;
    }

    /**
     * Toggles flight mode for the given target player.
     */
    private void toggleFlight(CommandSender sender, Player target) {
        boolean isFlying = target.getAllowFlight();

        target.setAllowFlight(!isFlying);
        target.setFlying(!isFlying);

        String status = isFlying ? ChatUtil.bred + "DISABLED" : ChatUtil.bgreen + "ENABLED";
        target.sendMessage(ChatUtil.gold + "✈" + ChatUtil.white + " Flight mode" + ChatUtil.darkGray + ": " + status);

        if (!sender.equals(target)) {
            sender.sendMessage(ChatUtil.gold + "✈" + ChatUtil.white + " You have " + status + ChatUtil.white + " flight for" + ChatUtil.darkGray + ": " + target.getDisplayName());
        }
    }
}
