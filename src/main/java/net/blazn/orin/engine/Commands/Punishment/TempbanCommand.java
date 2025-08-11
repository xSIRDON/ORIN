package net.blazn.orin.engine.Commands.Punishment;

import net.blazn.orin.engine.Managers.NameManager;
import net.blazn.orin.engine.Managers.PunishmentManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TempbanCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final RankManager rankManager;
    private final NameManager nameManager;
    private final PunishmentManager punishmentManager;

    public TempbanCommand(JavaPlugin plugin, RankManager rankManager, NameManager nameManager, PunishmentManager punishmentManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.nameManager = nameManager;
        this.punishmentManager = punishmentManager;
        plugin.getCommand("tempban").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof org.bukkit.entity.Player) {
            sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" +
                    ChatUtil.white + " /ban <player>");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" +
                    ChatUtil.white + " /tempban <player> <duration> <reason>");
            //sender.sendMessage(ChatUtil.gray + "Example: /tempban Steve 7d Rule violation");
            return true;
        }

        String targetName = args[0];
        String durationString = args[1];
        String reason = String.join(" ", args).replace(targetName, "").replace(durationString, "").trim();

        if (!nameManager.playerExists(targetName)) {
            sender.sendMessage(ChatUtil.doesNotExist);
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();
        String targetDispaly = nameManager.getDisplayName(targetUUID);
        String rankPrefix = rankManager.getRankColor(rankManager.getRank(targetUUID));

        long durationMillis = parseDuration(durationString);

        if (durationMillis <= 0) {
            sender.sendMessage("§fInvalid duration! §cUse" + ChatUtil.gray +":" +
                    ChatUtil.green + " m §8(§fminutes§8)§7, §ah §8(§fhours§8)§7, §ad §8(§fdays§8)§7, " +
                    "§aw §8(§fweeks§8)§7, §amo §8(§fmonths§8)§7, §ay §8(§fyears§8)");
            //sender.sendMessage(ChatUtil.gold + "Example" + ChatUtil.gray +":" +
            //        " 30m, 2h, 3d, 1w, 6mo, 1y");
            return true;
        }

        if (punishmentManager.isBanned(targetUUID)) {
            sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " That player is already banned.");
            return true;
        }

        if (punishmentManager.isTempBanned(targetUUID)) {
            sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " That player is already temp-banned.");
            return true;
        }

        punishmentManager.tempBanPlayer(target.getName(), "CONSOLE", reason, durationMillis);

        sender.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " Temporarily banned " + rankPrefix + targetDispaly +
                ChatUtil.white + " for " + ChatUtil.bred + durationString.toUpperCase() + ChatUtil.white + " due to" + ChatUtil.darkGray + ": " + ChatUtil.yellow + reason);

        return true;
    }

    /**
     * ✅ Parses duration into milliseconds
     */
    private long parseDuration(String input) {
        Pattern pattern = Pattern.compile("(\\d+)([mhdwMy])");
        Matcher matcher = pattern.matcher(input);

        long totalMillis = 0;

        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "m": totalMillis += value * 60 * 1000L; break;       // Minutes
                case "h": totalMillis += value * 60 * 60 * 1000L; break;  // Hours
                case "d": totalMillis += value * 24 * 60 * 60 * 1000L; break; // Days
                case "w": totalMillis += value * 7 * 24 * 60 * 60 * 1000L; break; // Weeks
                case "M": totalMillis += value * 30 * 24 * 60 * 60 * 1000L; break; // Months (approx.)
                case "y": totalMillis += value * 365 * 24 * 60 * 60 * 1000L; break; // Years (approx.)
                default: return -1;
            }
        }
        return totalMillis;
    }
}
