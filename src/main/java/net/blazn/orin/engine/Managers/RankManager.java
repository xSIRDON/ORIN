package net.blazn.orin.engine.Managers;

import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class RankManager {

    private FileConfiguration ranksConfig;

    private final SQLManager sqlManager;
    private final NameManager nameManager;
    private final JavaPlugin plugin;
    private List<String> ranks;
    private final Map<UUID, UUID> playerTargetMap = new HashMap<>(); // 🔹 Persistent UUID Storage

    public RankManager(JavaPlugin plugin, SQLManager sqlManager, NameManager nameManager) {
        this.plugin = plugin;
        this.sqlManager = sqlManager;
        this.nameManager = nameManager;

        loadRanksFile();
        initializeDefaultRanks();
        loadRanksFromConfig();
        migrateRanks(); // Ensures old ranks get converted if renamed.
    }


    public JavaPlugin getPlugin() {
        return plugin;
    }

    private void loadRanksFile() {
        File ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        if (!ranksFile.exists()) {
            plugin.saveResource("ranks.yml", false); // auto-copy default if packaged
        }

        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
    }

    /**
     * Loads the list of ranks from config.yml correctly.
     */
    private void loadRanksFromConfig() {
        FileConfiguration config = ranksConfig;

        if (!config.isConfigurationSection("ranks")) {
            plugin.getLogger().severe("⛔ No ranks found in config! Using default.");
            return;
        }

        ranks = new ArrayList<>(config.getConfigurationSection("ranks").getKeys(false));
    }

    private void initializeDefaultRanks() {
        if (!ranksConfig.contains("ranks")) {
            Map<String, Integer> defaultRanks = new LinkedHashMap<>();
            defaultRanks.put("OWNER", 10);
            defaultRanks.put("DEVELOPER", 10);
            defaultRanks.put("ADMIN", 8);
            defaultRanks.put("MOD", 7);
            defaultRanks.put("HELPER", 6);
            defaultRanks.put("YOUTUBE", 6);
            defaultRanks.put("VIP", 5);
            defaultRanks.put("BUILDER", 4);
            defaultRanks.put("MVP", 3);
            defaultRanks.put("ELITE", 2);
            defaultRanks.put("PRO", 1);
            defaultRanks.put("MEMBER", 0);

            for (Map.Entry<String, Integer> entry : defaultRanks.entrySet()) {
                ranksConfig.set("ranks." + entry.getKey(), entry.getValue());
            }
        }

        if (!ranksConfig.contains("rank-colors")) {
            ranksConfig.set("rank-colors.OWNER", "&4&l");
            ranksConfig.set("rank-colors.DEVELOPER", "&6&l");
            ranksConfig.set("rank-colors.ADMIN", "&d&l");
            ranksConfig.set("rank-colors.MOD", "&1&l");
            ranksConfig.set("rank-colors.HELPER", "&e&l");
            ranksConfig.set("rank-colors.YOUTUBE", "&c&l");
            ranksConfig.set("rank-colors.VIP", "&5");
            ranksConfig.set("rank-colors.BUILDER", "&9");
            ranksConfig.set("rank-colors.MVP", "&a");
            ranksConfig.set("rank-colors.ELITE", "&b");
            ranksConfig.set("rank-colors.PRO", "&3");
            ranksConfig.set("rank-colors.MEMBER", "&7");
        }

        if (!ranksConfig.contains("rank-prefixes")) {
            ranksConfig.set("rank-prefixes.OWNER", "\uE809 ");
            ranksConfig.set("rank-prefixes.DEVELOPER", "\uE811 ");
            ranksConfig.set("rank-prefixes.ADMIN", "\uE800 ");
            ranksConfig.set("rank-prefixes.MOD", "\uE807 ");
            ranksConfig.set("rank-prefixes.HELPER", "\uE803 ");
            ranksConfig.set("rank-prefixes.YOUTUBE", "\uE816 ");
            ranksConfig.set("rank-prefixes.VIP", "\uE80F ");
            ranksConfig.set("rank-prefixes.BUILDER", "\uE802 ");
            ranksConfig.set("rank-prefixes.MVP", "\uE804 ");
            ranksConfig.set("rank-prefixes.ELITE", "\uE814 ");
            ranksConfig.set("rank-prefixes.PRO", "\uE80B ");
            ranksConfig.set("rank-prefixes.MEMBER", "&7");
        }

        if (!ranksConfig.contains("rank-chat-colors")) {
            ranksConfig.set("rank-chat-colors.OWNER", "&b");
            ranksConfig.set("rank-chat-colors.DEVELOPER", "&b");
            ranksConfig.set("rank-chat-colors.ADMIN", "&b");
            ranksConfig.set("rank-chat-colors.MOD", "&b");
            ranksConfig.set("rank-chat-colors.HELPER", "&b");
            ranksConfig.set("rank-chat-colors.YOUTUBE", "&a");
            ranksConfig.set("rank-chat-colors.VIP", "&a");
            ranksConfig.set("rank-chat-colors.BUILDER", "&a");
            ranksConfig.set("rank-chat-colors.MVP", "&e");
            ranksConfig.set("rank-chat-colors.ELITE", "&e");
            ranksConfig.set("rank-chat-colors.PRO", "&e");
            ranksConfig.set("rank-chat-colors.MEMBER", "&f");
        }

        saveRanksFile();
    }
    /**
     * Ensures stored player ranks are valid after renaming.
     */
    public void migrateRanks() {
        Connection conn = sqlManager.getConnection();
        if (conn == null) {
            plugin.getLogger().severe("❌ Skipping rank migration: Database connection is null.");
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT uuid, `rank` FROM ranks");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                String storedRank = rs.getString("rank");

                // ✅ Ensure rank exists in config
                if (!getRanks().contains(storedRank)) {
                    setRank(UUID.fromString(uuid), "MEMBER");
                    plugin.getLogger().warning("⛔ Invalid rank '" + storedRank + "' found for " + uuid + ", setting to MEMBER.");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error migrating ranks: " + e.getMessage());
        }
    }

    /**
     * Sets the rank of an online player.
     */
    public void setRank(Player target, String rank) {
        if (!ranks.contains(rank.toUpperCase())) {
            return;
        }
        setRank(target.getUniqueId(), rank);
        updateDisplayName(target, rank);
    }

    /**
     * Sets the rank of an offline player.
     */
    public void setRank(UUID uuid, String rank) {
        if (!getRanks().contains(rank.toUpperCase())) {
            plugin.getLogger().severe("❌ Attempted to set an invalid rank: " + rank);
            return;
        }

        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("REPLACE INTO ranks (uuid, `rank`) VALUES (?, ?)")) {

            if (conn == null || conn.isClosed()) {
                plugin.getLogger().severe("❌ Database connection is null or closed!");
                return;
            }

            ps.setString(1, uuid.toString());
            ps.setString(2, rank.toUpperCase());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                plugin.getLogger().info("✅ Rank successfully updated in SQL for " + uuid + " to " + rank);

                // 🔹 Check if player is online and update name immediately
                Player onlinePlayer = Bukkit.getPlayer(uuid);
                if (onlinePlayer != null) {
                    updateDisplayName(onlinePlayer, rank);
                }
            } else {
                plugin.getLogger().severe("❌ Rank update failed for " + uuid);
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQL Error in setRank(): " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Gets the rank of an online player.
     */
    public String getRank(Player player) {
        return getRank(player.getUniqueId());
    }

    /**
     * Gets the rank of an offline player using UUID.
     */
    public String getRank(UUID uuid) {
        Connection conn = sqlManager.getConnection();
        if (conn == null) {
            plugin.getLogger().severe("❌ Cannot fetch rank: Database connection is NULL!");
            return "MEMBER"; // Return default rank if DB is down
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT `rank` FROM ranks WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("rank");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQL Error in getRank(): " + e.getMessage());
        }
        return "MEMBER"; // Default rank
    }

    public void updateDisplayName(Player player, String rank) {
        String displayName = nameManager.getDisplayName(player);

        File ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        FileConfiguration ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);

        if (displayName == null || displayName.isEmpty()) {
            plugin.getLogger().severe("❌ Display name for " + player.getName() + " is NULL or empty!");
            return;
        }

        // Strip color codes for clean formatting
        String strippedName = ChatColor.stripColor(displayName);
        String rankColor = getRankColor(rank);

        String formattedName;
        if (rank.equalsIgnoreCase("DEVELOPER")) {
            formattedName = ChatUtil.rainbowBold(strippedName); // Rainbow format
        } else {
            formattedName = rankColor + strippedName;
        }

        // Set player display name (used in chat)
        player.setDisplayName(formattedName);

        // Check if config wants chat prefixes
        boolean usePrefix = shouldUseChatPrefixes();
        if (usePrefix) {
            String prefix = getRankPrefix(rank); // assumes you already added this method
            player.setPlayerListName(ChatColor.WHITE + ChatUtil.stripSec(prefix + strippedName));
        } else {
            player.setPlayerListName(formattedName);
        }

        updateNameTag(player, rank, rankColor);
    }


    private void updateNameTag(Player player, String rank, String rankColor) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "rank_" + rank.toUpperCase();

        // ✅ Get or create the correct team
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            plugin.getLogger().info("🔹 Created new team: " + teamName);
        }

        // ✅ Remove player from other teams
        for (Team t : scoreboard.getTeams()) {
            if (t.hasEntry(player.getName())) {
                t.removeEntry(player.getName());
            }
        }

        // ✅ Apply color and prevent white nametags
        team.setPrefix(rankColor);

        // 🔹 Set the correct team color (Minecraft requires this for visibility)
        ChatColor chatColor = ChatColor.getByChar(rankColor.replace("§", "").charAt(0));
        if (chatColor != null) {
            team.setColor(chatColor);
        } else {
            team.setColor(ChatColor.WHITE); // Default if something goes wrong
        }

        // ✅ Add player to the correct team
        team.addEntry(player.getName());

        //plugin.getLogger().info("✅ Updated nametag for " + player.getName() + " with rank: " + rank + " and color: " + chatColor);
    }

    /**
     * Gets a player's formatted display name with their rank, even if they're offline.
     */
    public String getFormattedDisplayName(UUID uuid) {
        String rank = getRank(uuid);
        String displayName = nameManager.getDisplayName(uuid);

        if (displayName == null || displayName.isEmpty()) {
            displayName = "Unknown"; // Safety fallback
        }

        return rank.equalsIgnoreCase("DEVELOPER")
                ? ChatUtil.rainbowBold(displayName)
                : getRankColor(rank) + displayName;
    }

    public String getRankPrefix(String rank) {
        String rawPrefix = ranksConfig.getString("rank-prefixes." + rank.toUpperCase(), "");
        return ChatUtil.swapAmp(rawPrefix);
    }

    public String getRankColor(String rank) {
        FileConfiguration config = ranksConfig;

        if (!config.isConfigurationSection("rank-colors")) {
            plugin.getLogger().info("⛔ rank-colors section missing, initializing defaults...");

            // Default rank colors (some with bold)
            Map<String, String> defaultColors = new HashMap<>();
            defaultColors.put("OWNER", "&4&l");
            defaultColors.put("DEVELOPER", "&6&l");
            defaultColors.put("ADMIN", "&d&l");
            defaultColors.put("MOD", "&1&l");
            defaultColors.put("HELPER", "&e&l");
            defaultColors.put("YOUTUBE", "&c&l");
            defaultColors.put("VIP", "&5");
            defaultColors.put("BUILDER", "&9");
            defaultColors.put("MVP", "&a");
            defaultColors.put("ELITE", "&b");
            defaultColors.put("PRO", "&3");
            defaultColors.put("MEMBER", "&7");

            // Write defaults to config only once
            for (Map.Entry<String, String> entry : defaultColors.entrySet()) {
                if (!config.contains("rank-colors." + entry.getKey())) {
                    config.set("rank-colors." + entry.getKey(), entry.getValue());
                }
            }

            saveRanksFile();
        }

        // ✅ Ensure correct color and bold formatting
        String rawColor = ranksConfig.getString("rank-colors." + rank.toUpperCase(), "&7");
        String formattedColor = ChatUtil.swapAmp(rawColor);

        // ✅ Prevent duplicate `§l` but keep bold where intended
        if (formattedColor.contains("§l")) {
            formattedColor = formattedColor.replaceAll("(§l)+", "§l"); // Ensure only one bold code
        }

        return formattedColor;
    }

    public String getChatColor(String rank) {
        String raw = ranksConfig.getString("rank-chat-colors." + rank.toUpperCase(), "&f");
        return ChatUtil.swapAmp(raw);
    }

    /**
     * Retrieves all available ranks from config.yml.
     */
    public List<String> getRanks() {
        if (!ranksConfig.isConfigurationSection("ranks")) {
            plugin.getLogger().severe("⛔ No ranks found in config! Using default.");
            return Arrays.asList("OWNER", "DEVELOPER", "ADMIN", "SRMOD", "MOD", "VIP", "BUILDER", "EMERALD", "PLATINUM", "DIAMOND", "MEMBER");
        }

        return new ArrayList<>(ranksConfig.getConfigurationSection("ranks").getKeys(false));
    }

    /**
     * Stores the target UUID when opening the rank GUI.
     */
    public void storeTargetUUID(Player player, UUID targetUUID) {
        if (targetUUID == null) {
            //player.sendMessage(ChatUtil.serverPrefix + ChatUtil.red + "❌ Target UUID is null, cannot store."); //Debugging
            return;
        }
        playerTargetMap.put(player.getUniqueId(), targetUUID);
        //player.sendMessage(ChatUtil.serverPrefix + ChatUtil.green + "✅ Target UUID stored for: " + targetUUID.toString()); //Debugging
    }

    /**
     * Retrieves the stored target UUID.
     */
    public UUID getStoredTargetUUID(Player player) {
        return playerTargetMap.get(player.getUniqueId());
    }

    public void removeStoredTargetUUID(Player player) {
        playerTargetMap.remove(player.getUniqueId());
    }

    private void saveRanksFile() {
        try {
            ranksConfig.save(new File(plugin.getDataFolder(), "ranks.yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save ranks.yml: " + e.getMessage());
        }
    }

    public boolean shouldUseChatPrefixes() {
        return ranksConfig.getBoolean("prefixes", false);
    }

}
