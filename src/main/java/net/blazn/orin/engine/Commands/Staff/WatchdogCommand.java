package net.blazn.orin.engine.Commands.Staff;

import net.blazn.orin.engine.Managers.PermissionsManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Managers.WatchdogManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WatchdogCommand implements CommandExecutor {

    private final WatchdogManager watchdogManager;
    private final RankManager rankManager;
    private final PermissionsManager permissionsManager;

    public WatchdogCommand(JavaPlugin plugin, WatchdogManager watchdogManager,
                           RankManager rankManager, PermissionsManager permissionsManager) {
        this.watchdogManager = watchdogManager;
        this.rankManager = rankManager;
        this.permissionsManager = permissionsManager;
        plugin.getCommand("watchdog").setExecutor(this);
        plugin.getCommand("wd").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtil.onlyPlayers);
            return true;
        }
        Player player = (Player) sender;
        if (!permissionsManager.isStaff(rankManager.getRank(player))) {
            sender.sendMessage(ChatUtil.noPermission);
            return true;
        }

        watchdogManager.toggleWatchdog(player);
        return true;
    }
}
