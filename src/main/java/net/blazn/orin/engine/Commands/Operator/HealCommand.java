package net.blazn.orin.engine.Commands.Operator;

import net.blazn.orin.engine.Managers.PermissionsManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class HealCommand implements CommandExecutor {

    private final RankManager rankManager;
    private final PermissionsManager permissionsManager;
    private final JavaPlugin plugin;
    private final List<String> allowedRanks = Arrays.asList("OWNER", "DEVELOPER", "ADMIN", "SRMOD");

    public HealCommand(JavaPlugin plugin, RankManager rankManager, PermissionsManager permissionsManager) {
        this.rankManager = rankManager;
        this.permissionsManager = permissionsManager;
        this.plugin = plugin;
        plugin.getCommand("heal").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // If no arguments, ensure sender is a player before healing themselves
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" + ChatUtil.white + " /heal <player>");
                return true;
            }

            Player player = (Player) sender;
            String playerRank = rankManager.getRank(player);

            // ✅ Add permission check here
            if (!allowedRanks.contains(playerRank.toUpperCase())) {
                player.sendMessage(ChatUtil.noPermission);
                return true;
            }

            healPlayer(player, player);
            return true;
        }

        // Try to get target player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatUtil.notOnline);
            return true;
        }

        // If sender is console, allow healing without restrictions
        if (!(sender instanceof Player)) {
            healPlayer(sender, target);
            return true;
        }

        // If sender is a player, apply rank permissions
        Player player = (Player) sender;
        String playerRank = rankManager.getRank(player);
        String targetRank = rankManager.getRank(target);

        // Permission check
        if (!allowedRanks.contains(playerRank.toUpperCase())) {
            player.sendMessage(ChatUtil.noPermission);
            return true;
        }

        // Prevent lower-ranked players from healing higher-ranked ones
        if (!permissionsManager.canModify(playerRank, targetRank)) {
            player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You cannot heal this player.");
            return true;
        }

        // Perform healing
        healPlayer(player, target);
        return true;
    }

    /**
     * Heals the given target player.
     */
    private void healPlayer(CommandSender sender, Player target) {
        target.setHealth(20.0);
        target.setFoodLevel(20);
        target.setSaturation(10.0f);
        target.setFireTicks(0);

        // Remove all negative potion effects
        for (PotionEffect effect : target.getActivePotionEffects()) {
            target.removePotionEffect(effect.getType());
        }

        target.sendMessage(ChatUtil.green + "❤" + ChatUtil.white + " You have been healed.");

        if (!sender.equals(target)) {
            sender.sendMessage(ChatUtil.green + "❤" + ChatUtil.white + " You have healed" + ChatUtil.darkGray + ": " + target.getDisplayName());
        }
    }
}
