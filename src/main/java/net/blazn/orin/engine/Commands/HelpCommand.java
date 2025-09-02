package net.blazn.orin.engine.Commands;

import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HelpCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final RankManager rankManager;
    private final List<String> generalCommands;
    private final List<String> staffCommands;
    private final List<String> opCommands;
    private final List<String> punishmentCommands;
    private final List<String> adminCommands;
    private final List<String> premiumCommands;
    private final int COMMANDS_PER_PAGE = 8;
    private final List<String> staffRanks = Arrays.asList("OWNER", "DEVELOPER", "ADMIN", "SRMOD", "MOD");
    private final List<String> adminRanks = Arrays.asList("OWNER", "DEVELOPER", "ADMIN");

    public HelpCommand(JavaPlugin plugin, RankManager rankManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.generalCommands = new ArrayList<>();
        this.staffCommands = new ArrayList<>();
        this.opCommands = new ArrayList<>();
        this.punishmentCommands = new ArrayList<>();
        this.adminCommands = new ArrayList<>();
        this.premiumCommands = new ArrayList<>();
        loadCommands();

        plugin.getCommand("help").setExecutor(this);
    }

    /**
     * ✅ Loads the categorized commands from plugin.yml.
     */
    private void loadCommands() {
        // General Commands
        addCommand(generalCommands, "/help", "Shows this help menu.");
        addCommand(generalCommands, "/list", "Shows list of online players.");
        addCommand(generalCommands, "/msg <player> [msg]", "Sends private message to another player.");
        addCommand(generalCommands, "/ping", "Shows your connection to the server.");

        // Staff Commands (MOD+)
        addCommand(staffCommands, "/staff", "Enable staff chat mode.");
        addCommand(staffCommands, "/watchdog", "Enables watchdog mode.");
        addCommand(staffCommands, "/history <player>", "View a player's punishment history."); // ✅ ADDED

        // Punishment Commands (MOD+)
        addCommand(punishmentCommands, "/ban <player>", "Permanently bans player from server.");
        //addCommand(punishmentCommands, "/tempban <player> [reason] [time]", "Temporarily bans player from server.");
        addCommand(punishmentCommands, "/kick <player>", "Kick player from server.");
        addCommand(punishmentCommands, "/mute <player>", "Permanently mutes player.");
        //addCommand(punishmentCommands, "/tempmute <player> [reason] [time]", "Temporarily mutes player.");
        addCommand(punishmentCommands, "/warn <player> [reason]", "Warns player.");

        // Admin Rank Commands (ADMIN+)
        addCommand(adminCommands, "/rank <player> [rank]", "Manage player ranks.");
        addCommand(adminCommands, "/clearhistory [player]", "Clear a player's punishment history.");
        addCommand(adminCommands, "/unban <player>", "Unbans player from server.");
        addCommand(adminCommands, "/unmute <player>", "Unmutes player.");
        addCommand(adminCommands, "/broadcast <message>", "Broadcast server message.");

        // Premium Commands
        addCommand(premiumCommands, "/disguise", "Disguise as another player.");
        addCommand(premiumCommands, "/nick <name>", "Change your nickname.");
        addCommand(premiumCommands, "/fly [player]", "Toggle flight for yourself or others.");

        // Operator Commands (ADMIN+)
        addCommand(opCommands, "/clear [player]", "Clear inventory.");
        addCommand(opCommands, "/freeze [player|all]", "Freeze server or player.");
        addCommand(opCommands, "/unfreeze [player|all]", "Unfreeze server or player.");
        addCommand(opCommands, "/godmode [player]", "Toggle god mode.");
        addCommand(opCommands, "/gamemode <mode> [player]", "Change gamemode.");
        addCommand(opCommands, "/heal [player]", "Heal yourself or others.");
        addCommand(opCommands, "/kill [player]", "Kill yourself or others.");
        addCommand(opCommands, "/tp <player|coords>", "Teleport players or coordinates.");
        addCommand(opCommands, "/setmotd <message>", "Set server message of the day.");

    }

    private void addCommand(List<String> list, String command, String description) {
        list.add(ChatUtil.yellow + command + ChatUtil.gray + " - " + ChatUtil.white + description);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> selectedCommands = generalCommands;
        String category = "General Commands";
        int page = 1;

        // ✅ Console has full access
        boolean isConsole = !(sender instanceof Player);
        String senderRank = isConsole ? "OWNER" : rankManager.getRank((Player) sender);

        if (args.length > 0) {
            String arg = args[0].toLowerCase();

            // ✅ Determine category with permissions check
            switch (arg) {
                case "staff":
                    if (!isConsole && !staffRanks.contains(senderRank)) {
                        sender.sendMessage(ChatUtil.noPermission);
                        return true;
                    }
                    selectedCommands = staffCommands;
                    category = "Staff Commands";
                    break;
                case "punishment":
                    if (!isConsole && !staffRanks.contains(senderRank)) {
                        sender.sendMessage(ChatUtil.noPermission);
                        return true;
                    }
                    selectedCommands = punishmentCommands;
                    category = "Punishment Commands";
                    break;
                case "op":
                    if (!isConsole && !adminRanks.contains(senderRank)) {
                        sender.sendMessage(ChatUtil.noPermission);
                        return true;
                    }
                    selectedCommands = opCommands;
                    category = "Operator Commands";
                    break;
                case "admin":
                    if (!isConsole && !adminRanks.contains(senderRank)) {
                        sender.sendMessage(ChatUtil.noPermission);
                        return true;
                    }
                    selectedCommands = adminCommands;
                    category = "Admin Commands";
                    break;
                case "premium":
                    selectedCommands = premiumCommands;
                    category = "Premium Commands";
                    break;
                default:
                    try {
                        page = Integer.parseInt(arg);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " Invalid help category or page number.");
                        return true;
                    }
                    break;
            }

            // ✅ Check for page number if two arguments are given
            if (args.length > 1) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " Invalid page number.");
                    return true;
                }
            }
        }

        sendHelpPage(sender, page, selectedCommands, category);
        return true;
    }

    /**
     * ✅ Sends a paginated help page to the sender.
     */
    private void sendHelpPage(CommandSender sender, int page, List<String> commands, String title) {
        int totalPages = (int) Math.ceil((double) commands.size() / COMMANDS_PER_PAGE);
        if (page < 1 || page > totalPages) {
            sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " Invalid page number. Max: " + totalPages);
            return;
        }

        sender.sendMessage(ChatUtil.darkGray + "─── " + ChatUtil.bred + title + ChatUtil.gray +
                " (" + ChatUtil.white + page + "/" + totalPages + ChatUtil.gray + ") " + ChatUtil.darkGray + "───");

        int start = (page - 1) * COMMANDS_PER_PAGE;
        int end = Math.min(start + COMMANDS_PER_PAGE, commands.size());

        for (int i = start; i < end; i++) {
            sender.sendMessage(commands.get(i));
        }
    }
}
