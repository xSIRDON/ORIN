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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FreezeCommand implements CommandExecutor {

    private final RankManager rankManager;
    private final PermissionsManager permissionsManager;
    private final JavaPlugin plugin;
    private final List<String> allowedRanks = Arrays.asList("OWNER", "DEVELOPER", "ADMIN");

    private final Set<UUID> frozenPlayers;

    public FreezeCommand(JavaPlugin plugin, RankManager rankManager, PermissionsManager permissionsManager, Set<UUID> frozenPlayers) {
        this.rankManager = rankManager;
        this.permissionsManager = permissionsManager;
        this.plugin = plugin;
        this.frozenPlayers = frozenPlayers;

        plugin.getCommand("freeze").setExecutor(this);
        plugin.getCommand("unfreeze").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        boolean freezeAction = cmd.getName().equalsIgnoreCase("freeze");

        if ((sender instanceof Player player)) {
            String playerRank = rankManager.getRank(player);
            if (!allowedRanks.contains(playerRank.toUpperCase())) {
                sender.sendMessage(ChatUtil.noPermission);
                return true;
            }
        }

        if (args.length == 0) {
            sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ": " + ChatUtil.white + "/" + label + " <player|all>");
            return true;
        }

        if (args[0].equalsIgnoreCase("all")) {
            if (freezeAction) {
                // Freeze logic: skip sender, OPs, staff
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (sender instanceof Player playerSender) {
                        if (p.equals(playerSender)) continue;
                        if (p.isOp()) continue;
                        String targetRank = rankManager.getRank(p);
                        if (allowedRanks.contains(targetRank.toUpperCase())) continue;
                    }
                    freezePlayer(sender, p);
                }
            } else {
                // Unfreeze logic: only unfreeze players who are actually frozen
                for (UUID uuid : new HashSet<>(frozenPlayers)) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        unfreezePlayer(sender, p);
                    }
                }
            }
            sender.sendMessage((freezeAction ? ChatUtil.bblue + "❄" : ChatUtil.bgreen + "✔")
                    + ChatUtil.white + " All players have been " + (freezeAction ? "frozen" : "unfrozen") + ".");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatUtil.notOnline);
            return true;
        }

        if (sender instanceof Player player) {
            String playerRank = rankManager.getRank(player);
            String targetRank = rankManager.getRank(target);

            if (!allowedRanks.contains(playerRank.toUpperCase())) {
                sender.sendMessage(ChatUtil.noPermission);
                return true;
            }

            if (!permissionsManager.canModify(playerRank, targetRank)) {
                sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You cannot freeze this player.");
                return true;
            }
        }

        if (freezeAction) freezePlayer(sender, target);
        else unfreezePlayer(sender, target);

        return true;
    }

    private void freezePlayer(CommandSender sender, Player target) {
        if (frozenPlayers.contains(target.getUniqueId())) {
            sender.sendMessage(ChatUtil.darkRed + "❌ " + target.getDisplayName() + ChatUtil.white + " is already frozen.");
            return;
        }

        frozenPlayers.add(target.getUniqueId());
        target.setWalkSpeed(0f);
        target.setFlySpeed(0f);
        target.sendMessage(ChatUtil.bblue + "❄" + ChatUtil.white + " You have been frozen.");
        if (!sender.equals(target)) {
            sender.sendMessage(ChatUtil.bblue + "❄" + ChatUtil.white + " You froze " + target.getDisplayName());
        }
    }

    private void unfreezePlayer(CommandSender sender, Player target) {
        if (!frozenPlayers.contains(target.getUniqueId())) {
            sender.sendMessage(ChatUtil.darkRed + "❌ " +  target.getDisplayName() + ChatUtil.white + " is not frozen.");
            return;
        }

        frozenPlayers.remove(target.getUniqueId());
        target.setWalkSpeed(0.2f);
        target.setFlySpeed(0.1f);
        target.sendMessage(ChatUtil.bgreen + "✔" + ChatUtil.white + " You have been unfrozen.");
        if (!sender.equals(target)) {
            sender.sendMessage(ChatUtil.bgreen + "✔" + ChatUtil.white + " You unfroze " + target.getDisplayName());
        }
    }

    public Set<UUID> getFrozenPlayers() {
        return frozenPlayers;
    }
}
