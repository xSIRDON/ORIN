package net.blazn.orin.engine.Commands.Staff;

import net.blazn.orin.engine.Managers.NameManager;
import net.blazn.orin.engine.Managers.PermissionsManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RankCommand implements CommandExecutor {

    private final RankManager rankManager;
    private final NameManager nameManager;
    private final PermissionsManager permissionsManager;
    private final JavaPlugin plugin;
    private final List<String> allowedRanks = Arrays.asList("OWNER", "DEVELOPER", "ADMIN");

    public RankCommand(JavaPlugin plugin, RankManager rankManager, NameManager nameManager, PermissionsManager permissionsManager) {
        this.rankManager = rankManager;
        this.nameManager = nameManager;
        this.permissionsManager = permissionsManager;
        this.plugin = plugin;
        plugin.getCommand("rank").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Allow console execution
        if (!(sender instanceof Player)) {
            if (args.length < 2) {
                sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" + ChatUtil.white + " /rank <player> [rank]");
                return true;
            }
            handleRankChange(sender, args[0], args[1]);
            return true;
        }

        Player player = (Player) sender;

        // Check if player has an admin+ rank
        String playerRank = rankManager.getRank(player);
        if (!allowedRanks.contains(playerRank) && !player.isOp()) {
            player.sendMessage(ChatUtil.noPermission);
            return true;
        }

        // Open GUI if only 1 argument is provided
        if (args.length == 1) {
            openRankGUI(player, args[0]);
            return true;
        }

        // Set rank if 2 arguments provided
        if (args.length == 2) {
            player.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" + ChatUtil.white + " /rank <player>");
            return true;
        }

        player.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" + ChatUtil.white + " /rank <player>");
        return true;
    }

    /**
     * Handles setting the rank for both online and offline players.
     */
    private void handleRankChange(CommandSender sender, String targetName, String rankInput) {
        final String rank = rankInput.toUpperCase();

        // ✅ Retrieve ranks properly
        List<String> availableRanks = rankManager.getRanks();

        if (!availableRanks.contains(rank)) {
            StringBuilder rankList = new StringBuilder();

            for (String r : availableRanks) {
                String formattedRank = r.equalsIgnoreCase("DEVELOPER") ? ChatUtil.rainbowBold("DEVELOPER") : rankManager.getRankColor(r) + r;
                rankList.append(formattedRank).append(ChatUtil.darkGray).append(", ");
            }

            // ✅ Remove last comma and space
            if (rankList.length() > 2) {
                rankList.setLength(rankList.length() - 2);
            }

            sender.sendMessage(ChatUtil.red + "Invalid rank. Available: " + rankList);
            return;
        }

        // ✅ Get sender's rank (console has highest permission)
        String senderRank = (sender instanceof Player) ? rankManager.getRank((Player) sender) : "OWNER";

        // ✅ Get target's current rank
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = offlineTarget.getUniqueId();

        // ✅ Check if the player exists in the database BEFORE getting their rank
        if (!nameManager.playerExists(targetUUID)) {
            sender.sendMessage(ChatUtil.doesNotExist);
            return;
        }

        String targetRank = rankManager.getRank(targetUUID);

        // ✅ Check if sender has permission to change this rank
        if (!permissionsManager.canChangeRank(senderRank, targetRank) && sender instanceof Player && !((Player) sender).isOp()) {
            sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You do not have permission to modify that player's rank.");
            return;
        }

        // ✅ Determine the formatted rank name
        String formattedRank = rank.equalsIgnoreCase("DEVELOPER")
                ? ChatUtil.rainbowBold("DEVELOPER")
                : rankManager.getRankColor(rank) + rank;

        // ✅ If target is online, update immediately
        Player target = offlineTarget.getPlayer();
        if (target != null) {
            sender.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " Successfully set "
                    + nameManager.getDisplayName(target) + "'s" + ChatUtil.white + " rank to"
                    + ChatUtil.darkGray + ": " + formattedRank);
            target.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " Your rank has been set to"
                    + ChatUtil.darkGray + ": " + formattedRank);
            rankManager.setRank(target, rank);
            return;
        }

        // ✅ Handle offline players asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // ✅ Ensure the player exists before attempting to set rank
            if (!nameManager.playerExists(targetUUID)) {
                sender.sendMessage(ChatUtil.doesNotExist);
                return;
            }

            // ✅ Update rank for known offline players
            sender.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " Successfully set "
                    + nameManager.getDisplayName(targetUUID) + "'s" + ChatUtil.white + " rank to"
                    + ChatUtil.darkGray + ": " + formattedRank);
            rankManager.setRank(targetUUID, rank);
        });
    }


    /**
     * Opens a single-page rank selection GUI with a red-stained glass border.
     */
    private void openRankGUI(Player player, String targetName) {
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = offlineTarget.getUniqueId();

        // ✅ Check if the target player exists in the database BEFORE opening the GUI
        if (!nameManager.playerExists(targetUUID)) {
            player.sendMessage(ChatUtil.doesNotExist);
            return;
        }

        // ✅ Ensure UUID is stored before opening GUI
        rankManager.storeTargetUUID(player, targetUUID);

        // ✅ Get the sender's rank level
        int playerRankLevel = permissionsManager.getRankLevel(rankManager.getRank(player));
        int targetRankLevel = permissionsManager.getRankLevel(rankManager.getRank(targetUUID));

        String senderRank = rankManager.getRank(player);
        String targetRank = rankManager.getRank(targetUUID);

        if (!permissionsManager.canChangeRank(senderRank, targetRank) && !player.isOp()) {
            player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You do not have permission to modify that player's rank.");
            return;
        }

        // ✅ Filter ranks based on sender's permissions
        List<String> allRanks = rankManager.getRanks();
        List<String> filteredRanks = allRanks.stream()
                .filter(rank -> {
                    int rankLevel = permissionsManager.getRankLevel(rank);
                    if (playerRankLevel >= 10 || player.isOp()) return true; // Owners & Developers see all ranks
                    return playerRankLevel > rankLevel && !(rank.equals("OWNER") || rank.equals("DEVELOPER") || rank.equals("ADMIN"));
                })
                .toList();

        int rankCount = filteredRanks.size();

        // ✅ Calculate GUI size based on the number of ranks (minimum 3 rows, maximum 6 rows)
        int rows = Math.min(6, Math.max(3, (int) Math.ceil(rankCount / 7.0) + 2));
        int inventorySize = rows * 9;

        Inventory gui = Bukkit.createInventory(null, inventorySize, ChatUtil.bgold + "Select a Rank");

        // ✅ Create Red-Stained Glass Pane for border
        ItemStack borderItem = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderItem.getItemMeta();
        borderMeta.setDisplayName(ChatColor.RED + " ");
        borderItem.setItemMeta(borderMeta);

        // ✅ Place border in all outer slots
        for (int i = 0; i < inventorySize; i++) {
            if (i < 9 || i >= inventorySize - 9 || i % 9 == 0 || (i + 1) % 9 == 0) {
                gui.setItem(i, borderItem);
            }
        }

        // ✅ Dynamically center rank items within available slots
        int middleStart = (inventorySize - (9 * (rows - 2))) / 2;
        int startSlot = middleStart + 1;
        int rowItems = 7; // Fit 7 items per row
        int slot = startSlot;

        for (String rank : filteredRanks) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();

            // ✅ Ensure DEVELOPER shows in rainbow
            if (rank.equalsIgnoreCase("DEVELOPER")) {
                meta.setDisplayName(ChatUtil.rainbowBold("DEVELOPER"));
            } else {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', rankManager.getRankColor(rank) + rank));
            }
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Click to assign " + targetName + " this rank."));
            item.setItemMeta(meta);

            // ✅ Place rank item
            gui.setItem(slot, item);
            slot++;

            // ✅ Move to the next row when row is filled
            if ((slot - startSlot) % rowItems == 0) {
                slot += (9 - rowItems); // Skip to the next row (avoiding border)
            }
        }

        player.openInventory(gui);
    }
}
