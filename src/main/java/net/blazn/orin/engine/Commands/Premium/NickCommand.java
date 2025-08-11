package net.blazn.orin.engine.Commands.Premium;

import net.blazn.orin.engine.Managers.NameManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Managers.PermissionsManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class NickCommand implements CommandExecutor {

    private final NameManager nameManager;
    private final RankManager rankManager;
    private final PermissionsManager permissionsManager;

    public NickCommand(NameManager nameManager, RankManager rankManager, PermissionsManager permissionsManager) {
        this.nameManager = nameManager;
        this.rankManager = rankManager;
        this.permissionsManager = permissionsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtil.onlyPlayers);
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        boolean hasNick = nameManager.hasStoredNickname(uuid); // ✅ Direct DB Check

        // ✅ If args are empty, check if player has a nickname in the database
        if (args.length == 0) {
            if (!hasNick) {
                player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " Usage" +
                        ChatUtil.darkGray + ":" + ChatUtil.white + " /nick <name> (or /nick to reset if you have one)");
            } else {
                resetNickname(player);
            }
            return true;
        }

        // ✅ /nick <newName>
        String newNick = ChatColor.translateAlternateColorCodes('&', String.join(" ", args));

        if (newNick.length() > 16) {
            player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " Nickname must be 16 characters or less.");
            return true;
        }

        // ✅ Ensure nickname is unique (no duplicates)
        if (isNickTaken(newNick)) {
            player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " This nickname is already in use!");
            return true;
        }

        // ✅ Check if player has permission to nick (Emerald+)
        String rank = rankManager.getRank(player);
        if (permissionsManager.getRankLevel(rank) < 5) {
            player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " Must be at least EMERALD to use this command.");
            return true;
        }

        // ✅ Set the new nickname
        player.setDisplayName(newNick);
        nameManager.setNickname(uuid, newNick);
        rankManager.updateDisplayName(player, rank);

        player.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " Your nickname is now" + ChatUtil.darkGray + ": " + rankManager.getRankColor(rank) + newNick);
        return true;
    }

    private void resetNickname(Player player) {
        UUID uuid = player.getUniqueId();
        nameManager.removeNickname(uuid);
        rankManager.updateDisplayName(player, rankManager.getRank(player));

        player.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " Your nickname has been reset!");
    }

    private boolean isNickTaken(String nickname) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (nameManager.getDisplayName(p.getUniqueId()).equalsIgnoreCase(nickname)) {
                return true;
            }
        }
        return false;
    }
}
