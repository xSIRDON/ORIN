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

public class ClearCommand implements CommandExecutor {

    private final RankManager rankManager;
    private final PermissionsManager permissionsManager;
    private final JavaPlugin plugin;
    private final List<String> allowedRanks = Arrays.asList("OWNER", "DEVELOPER", "ADMIN");

    public ClearCommand(JavaPlugin plugin, RankManager rankManager, PermissionsManager permissionsManager) {
        this.rankManager = rankManager;
        this.permissionsManager = permissionsManager;
        this.plugin = plugin;
        plugin.getCommand("clear").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player target = null;

        // If sender is console, require a player argument
        if (!(sender instanceof Player)) {
            if (args.length == 0) {
                sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" + ChatUtil.white + " /clear <player>");
                return true;
            }

            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatUtil.notOnline);
                return true;
            }

            clearInventory(sender, target);
            return true;
        }

        // If sender is a player
        Player player = (Player) sender;
        String playerRank = rankManager.getRank(player);

        // Permission check
        if (!allowedRanks.contains(playerRank)) {
            player.sendMessage(ChatUtil.noPermission);
            return true;
        }

        // If no arguments, clear sender's own inventory
        if (args.length == 0) {
            target = player;
        } else {
            // If argument is provided, attempt to clear another player's inventory
            target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                player.sendMessage(ChatUtil.notOnline);
                return true;
            }

            String targetRank = rankManager.getRank(target);
            if (!permissionsManager.canModify(playerRank, targetRank)) {
                player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You cannot clear this player's inventory.");
                return true;
            }
        }

        // Clear inventory
        clearInventory(player, target);
        return true;
    }

    /**
     * Clears the inventory of a target player.
     */
    private void clearInventory(CommandSender sender, Player target) {
        target.getInventory().clear();

        // Notify target
        target.sendMessage(ChatUtil.green + "✔ " + ChatUtil.white + "Your inventory has been cleared.");

        // Notify sender if clearing another player's inventory
        if (!sender.equals(target)) {
            sender.sendMessage(ChatUtil.green + "✔ " + ChatUtil.white + "You cleared " + target.getDisplayName() +
                    "'s" + ChatUtil.white + " inventory.");
        }
    }
}
