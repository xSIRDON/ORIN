package net.blazn.orin.engine.Commands.Operator;

import net.blazn.orin.engine.Managers.PermissionsManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Managers.VanishManager;
import net.blazn.orin.engine.Managers.WatchdogManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class VanishCommand implements CommandExecutor {

    private final RankManager rankManager;
    private final PermissionsManager permissionsManager;
    private final WatchdogManager watchdogManager;
    private final VanishManager vanishManager;
    private final JavaPlugin plugin;
    private final List<String> allowedRanks = Arrays.asList("OWNER", "DEVELOPER", "ADMIN", "MOD");

    public VanishCommand(JavaPlugin plugin,
                         RankManager rankManager,
                         PermissionsManager permissionsManager,
                         WatchdogManager watchdogManager,
                         VanishManager vanishManager) {
        this.rankManager = rankManager;
        this.permissionsManager = permissionsManager;
        this.watchdogManager = watchdogManager;
        this.vanishManager = vanishManager;
        this.plugin = plugin;
        plugin.getCommand("vanish").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // No arguments → toggle vanish for sender
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" + ChatUtil.white + " /vanish <player>");
                return true;
            }

            Player player = (Player) sender;
            String rank = rankManager.getRank(player);

            if (!allowedRanks.contains(rank.toUpperCase())) {
                player.sendMessage(ChatUtil.noPermission);
                return true;
            }

            if (watchdogManager.isInWatchdog(player)) {
                player.sendMessage(ChatUtil.darkRed + "❌ " + ChatUtil.red + "You cannot vanish while in watchdog mode.");
                return true;
            }

            vanishManager.toggleVanish(player);
            //player.sendMessage(ChatUtil.green + "✔ You toggled your vanish state.");
            return true;
        }

        // With arguments → toggle vanish for target
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatUtil.notOnline);
            return true;
        }

        // Prevent server from being vanished

        if (watchdogManager.isInWatchdog(target)) {
            sender.sendMessage(ChatUtil.darkRed + "❌ " + ChatUtil.red + "You cannot vanish players in watchdog mode.");
            return true;
        }

        // Console sender → can only vanish other players
        if (!(sender instanceof Player)) {
            if (vanishManager.isVanished(target)) {
                sender.sendMessage(ChatUtil.red + "❌ " + target.getName() + " is already vanished.");
            } else {
                vanishManager.toggleVanish(target);
                sender.sendMessage(ChatUtil.green + "✔ " + target.getName() + " has been vanished.");
            }
            return true;
        }

        // Player sender → rank & permission checks
        Player player = (Player) sender;
        String playerRank = rankManager.getRank(player);
        String targetRank = rankManager.getRank(target);

        if (!allowedRanks.contains(playerRank.toUpperCase())) {
            player.sendMessage(ChatUtil.noPermission);
            return true;
        }

        if (!permissionsManager.canModify(playerRank, targetRank)) {
            player.sendMessage(ChatUtil.darkRed + "❌ " + ChatUtil.red + "You cannot modify this player's vanish state.");
            return true;
        }

        if (watchdogManager.isInWatchdog(player)) {
            player.sendMessage(ChatUtil.darkRed + "❌ " + ChatUtil.red + "You cannot vanish while in watchdog mode.");
            return true;
        }

        // Toggle vanish with feedback
        if (vanishManager.isVanished(target)) {
            vanishManager.toggleVanish(target);
            player.sendMessage(ChatUtil.gold + "👁 " + ChatUtil.white + target.getDisplayName() + ChatUtil.white + " is now visible.");
            if (!player.equals(target)) target.sendMessage(ChatUtil.gold + "👁" + ChatUtil.white + " Vanish" + ChatUtil.darkGray + ": " + ChatUtil.bred + "DISABLED");;
        } else {
            vanishManager.toggleVanish(target);
            player.sendMessage(ChatUtil.gold + "👁 " + ChatUtil.white + target.getDisplayName() + ChatUtil.white + " has been vanished.");
            if (!player.equals(target)) target.sendMessage(ChatUtil.gold + "👁" + ChatUtil.white + " Vanish" + ChatUtil.darkGray + ": " + ChatUtil.bgreen + "ENABLED");;
        }

        return true;
    }
}
