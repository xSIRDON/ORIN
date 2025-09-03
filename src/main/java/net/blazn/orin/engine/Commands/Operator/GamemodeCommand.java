package net.blazn.orin.engine.Commands.Operator;

import net.blazn.orin.engine.Managers.PermissionsManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Managers.WatchdogManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class GamemodeCommand implements CommandExecutor {

    private final RankManager rankManager;
    private final PermissionsManager permissionsManager;
    private final WatchdogManager watchdogManager;
    private final JavaPlugin plugin;
    private final List<String> allowedRanks = Arrays.asList("OWNER", "DEVELOPER", "ADMIN", "SRMOD");

    public GamemodeCommand(JavaPlugin plugin, RankManager rankManager, PermissionsManager permissionsManager, WatchdogManager watchdogManager) {
        this.rankManager = rankManager;
        this.permissionsManager = permissionsManager;
        this.watchdogManager = watchdogManager;
        this.plugin = plugin;
        plugin.getCommand("gamemode").setExecutor(this);
        plugin.getCommand("gmc").setExecutor(this);
        plugin.getCommand("gms").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player targetPlayer = null;
        GameMode targetMode = null;

        // Determine the gamemode based on command label or arguments
        if (label.equalsIgnoreCase("gmc")) {
            targetMode = GameMode.CREATIVE;
        } else if (label.equalsIgnoreCase("gms")) {
            targetMode = GameMode.SURVIVAL;
        }

        // If first argument is a gamemode
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("creative")) {
                targetMode = GameMode.CREATIVE;
            } else if (args[0].equalsIgnoreCase("survival")) {
                targetMode = GameMode.SURVIVAL;
            } else {
                // If argument is a player name
                targetPlayer = Bukkit.getPlayer(args[0]);
                if (targetPlayer == null) {
                    sender.sendMessage(ChatUtil.notOnline);
                    return true;
                }
            }
        }

        // If second argument exists, it's a player
        if (args.length > 1) {
            targetPlayer = Bukkit.getPlayer(args[1]);

            if (targetPlayer == null) {
                sender.sendMessage(ChatUtil.notOnline);
                return true;
            }
        }

        // If no player specified, assume sender is the target
        if (targetPlayer == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" + ChatUtil.white + " /gamemode <creative/survival> <player>");
                return true;
            }
            targetPlayer = (Player) sender;
        }

        // If no gamemode determined, send usage message
        if (targetMode == null) {
            sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" + ChatUtil.white + " /gamemode <creative/survival> [player]");
            return true;
        }

        // ✅ Watchdog checks
        if (watchdogManager.isInWatchdog(targetPlayer)) {
            sender.sendMessage(ChatUtil.darkRed + "❌ " + ChatUtil.red +
                    "You cannot change gamemode of players in watchdog mode.");
            return true;
        }
        if (sender instanceof Player && watchdogManager.isInWatchdog((Player) sender)) {
            sender.sendMessage(ChatUtil.darkRed + "❌ " + ChatUtil.red +
                    "You cannot use gamemode commands while in watchdog mode.");
            return true;
        }

        // If sender is console, allow gamemode change for any player
        if (!(sender instanceof Player)) {
            changeGamemode(sender, targetPlayer, targetMode);
            return true;
        }

        // If sender is a player, check rank permissions
        Player player = (Player) sender;
        String playerRank = rankManager.getRank(player);
        String targetRank = rankManager.getRank(targetPlayer);

        // Permission check
        if (!allowedRanks.contains(playerRank)) {
            player.sendMessage(ChatUtil.noPermission);
            return true;
        }

        // Prevent lower-ranked players from changing gamemode of higher-ranked ones
        if (!permissionsManager.canModify(playerRank, targetRank)) {
            player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You cannot change this player's gamemode.");
            return true;
        }

        // Perform gamemode change
        changeGamemode(player, targetPlayer, targetMode);
        return true;
    }

    /**
     * Changes the gamemode of a target player.
     */
    private void changeGamemode(CommandSender sender, Player target, GameMode gameMode) {
        target.setGameMode(gameMode);

        if (sender.equals(target)) {
            target.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " Your gamemode has been set to" +
                    ChatUtil.darkGray + ": " + ChatUtil.bgold + gameMode.name().toUpperCase());
        } else {
            target.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " Your gamemode has been set to" +
                    ChatUtil.darkGray + ": " + ChatUtil.bgold + gameMode.name().toUpperCase());
            sender.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " You set " + target.getDisplayName() +
                    "'s" + ChatUtil.white + " gamemode to" + ChatUtil.darkGray + ": " + ChatUtil.bgold + gameMode.name().toUpperCase());
        }
    }
}
