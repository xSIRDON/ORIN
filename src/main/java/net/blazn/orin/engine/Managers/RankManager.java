package net.blazn.orin.engine.Managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.mojang.authlib.GameProfile;
import net.blazn.orin.Main;
import net.blazn.orin.engine.Utils.ChatUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class RankManager {

    private FileConfiguration ranksConfig;

    //private final ProtocolManager protocolManager;
    private final SQLManager sqlManager;
    private final NameManager nameManager;
    private final JavaPlugin plugin;
    private List<String> ranks;
    private final Map<UUID, UUID> playerTargetMap = new HashMap<>();
    private final Map<UUID, String> fakeRanks = new HashMap<>();

    public RankManager(JavaPlugin plugin, SQLManager sqlManager, NameManager nameManager) {
        this.plugin = plugin;
        //this.protocolManager = protocolManager;
        this.sqlManager = sqlManager;
        this.nameManager = nameManager;

        loadRanksFile();
        initializeDefaultRanks();
        loadRanksFromConfig();
        migrateRanks();
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    private void loadRanksFile() {
        File ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        if (!ranksFile.exists()) {
            plugin.saveResource("ranks.yml", false);
        }

        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
    }

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
            defaultRanks.put("DEVELOPER", 9);
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

                if (!getRanks().contains(storedRank)) {
                    setRank(UUID.fromString(uuid), "MEMBER");
                    plugin.getLogger().warning("⛔ Invalid rank '" + storedRank + "' found for " + uuid + ", setting to MEMBER.");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error migrating ranks: " + e.getMessage());
        }
    }

    // ------------------------
    // 🔹 Fake Rank Methods
    // ------------------------

    public void setFakeRank(Player player, String fakeRank) {
        fakeRanks.put(player.getUniqueId(), fakeRank.toUpperCase());
        updateDisplayName(player, fakeRank);
    }

    public void clearFakeRank(Player player) {
        fakeRanks.remove(player.getUniqueId());
        String realRank = getRank(player);
        updateDisplayName(player, realRank);
    }

    public String getDisplayRank(Player player) {
        return fakeRanks.getOrDefault(player.getUniqueId(), getRank(player));
    }

    /**
     * Returns the fake rank of a player if they are disguised,
     * or null if they have no fake rank.
     */
    public String getFakeRank(Player player) {
        return fakeRanks.get(player.getUniqueId());
    }

    public boolean isDisguised(Player player) {
        UUID uuid = player.getUniqueId();

        // Check SQL database via NameManager
        String disguiseName = nameManager.getDisguiseName(uuid);
        return disguiseName != null && !disguiseName.isEmpty();
    }

    // ------------------------
    // 🔹 Rank Handling
    // ------------------------

    public String getRank(Player player) {
        return getRank(player.getUniqueId());
    }

    public String getRank(UUID uuid) {
        Connection conn = sqlManager.getConnection();
        if (conn == null) {
            plugin.getLogger().severe("❌ Cannot fetch rank: Database connection is NULL!");
            return "MEMBER";
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
        return "MEMBER";
    }

    /** 🔹 Returns a random lower rank than the player's real rank, but never HELPER+ */
    public String getRandomLowerRank(Player player) {
        String currentRank = getRank(player);
        int currentLevel = getRankLevel(currentRank);
        int helperLevel = getRankLevel("HELPER"); // ceiling for disguises

        List<String> eligible = new ArrayList<>();
        for (String rank : getRanks()) {
            int rankLevel = getRankLevel(rank);
            // Only allow ranks lower than player's rank AND below HELPER
            if (rankLevel < currentLevel && rankLevel < helperLevel) {
                eligible.add(rank);
            }
        }

        if (eligible.isEmpty()) {
            // fallback: lowest possible rank below HELPER
            for (String rank : getRanks()) {
                if (getRankLevel(rank) < helperLevel) {
                    eligible.add(rank);
                }
            }
        }

        if (eligible.isEmpty()) {
            return currentRank; // extreme fallback: keep their own rank
        }

        Collections.shuffle(eligible);
        return eligible.get(0);
    }

    public int getRankLevel(String rank) {
        return ranksConfig.getInt("ranks." + rank.toUpperCase(), 0);
    }

    public void setRank(Player target, String rank) {
        if (!ranks.contains(rank.toUpperCase())) {
            return;
        }
        setRank(target.getUniqueId(), rank);
        updateDisplayName(target, rank);
    }

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

    // ------------------------
    // 🔹 Display
    // ------------------------

//    public void updateDisplayName(Player player, String rank) {
//        String displayName = nameManager.getDisplayName(player);
//
//        if (displayName == null || displayName.isEmpty()) {
//            plugin.getLogger().severe("❌ Display name for " + player.getName() + " is NULL or empty!");
//            return;
//        }
//
//        // Strip color codes for clean formatting
//        String strippedName = ChatColor.stripColor(displayName);
//        String rankColor = getRankColor(rank);
//
//        String formattedName;
//        if (rank.equalsIgnoreCase("DEVELOPER")) {
//            formattedName = ChatUtil.rainbowBold(strippedName); // Rainbow format
//        } else {
//            formattedName = rankColor + strippedName;
//        }
//
//        // ✅ Add ping next to their name
//        int ping = player.getPing();
//        String pingColor = (ping <= 80) ? ChatUtil.green : (ping <= 150) ? ChatUtil.yellow : ChatUtil.red;
//        String pingSuffix = ChatUtil.gray + " (" + pingColor + ping + "ms" + ChatUtil.gray + ")";
//
//        // Handle chat prefixes if enabled
//        boolean usePrefix = shouldUseChatPrefixes();
//        String prefix = usePrefix ? getRankPrefix(rank) : "";
//
//        // Combine everything
//        String finalTag = ChatColor.WHITE + prefix + strippedName;
//        if (plugin.getConfig().getBoolean("tablist.show-ping")) {
//            finalTag += pingSuffix;
//        }
//
//        // ✅ Store in ProtocolLib map
//        Main.getPlayerTags().put(player.getUniqueId(), finalTag);
//
//        // Optional: update player's chat display
//        player.setDisplayName(formattedName);
//        updateNameTag(player, rank, rankColor);
//    }

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

        // ✅ Add ping next to their name in tablist
        int ping = player.getPing();
        String pingColor = (ping <= 80) ? ChatUtil.green : (ping <= 150) ? ChatUtil.yellow : ChatUtil.red;
        String pingSuffix = ChatUtil.gray + " (" + pingColor + ping + "ms" + ChatUtil.gray + ")";

        // Check if config wants chat prefixes
        boolean usePrefix = shouldUseChatPrefixes();
        if (usePrefix) {
            String prefix = getRankPrefix(rank); // assumes you already added this method
            if (plugin.getConfig().getBoolean("tablist.show-ping")) {
                player.setPlayerListName(ChatColor.WHITE + ChatUtil.stripSec(prefix + strippedName) + pingSuffix);
            } else {
                player.setPlayerListName(ChatColor.WHITE + ChatUtil.stripSec(prefix + strippedName));
            }
        } else {
            if (plugin.getConfig().getBoolean("tablist.show-ping")) {
                player.setPlayerListName(formattedName + pingSuffix);
            } else {
                player.setPlayerListName(formattedName);
            }
        }

        //Main.getPlayerTags().put(player.getUniqueId(), formattedName);
        updateNameTag(player, rank, rankColor);
        //setNametag(player, formattedName);
        //changeName(player, ChatUtil.stripSec(formattedName));
    }

    public void startPingTabUpdater() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                // Use fake rank if set, otherwise real rank
                String rank = fakeRanks.getOrDefault(p.getUniqueId(), getRank(p));
                updateDisplayName(p, rank);
            }
        }, 0L, 40L);
    }

    private void updateNameTag(Player player, String rank, String rankColor) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "rank_" + rank.toUpperCase();

        // Get or create the team
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            plugin.getLogger().info("🔹 Created new team: " + teamName);
        }

        // Remove player from all other teams
        for (Team t : scoreboard.getTeams()) {
            if (t.hasEntry(player.getName())) {
                t.removeEntry(player.getName());
            }
        }

        // Set the team prefix
        team.setPrefix(rankColor != null ? rankColor : "§7");

        // Safely set the team color for visibility
        if (rank.equalsIgnoreCase("DEVELOPER")) {
            // Developers use rainbow, so default to WHITE to avoid crashes
            team.setColor(ChatColor.WHITE);
        } else {
            ChatColor chatColor = ChatColor.WHITE; // fallback
            if (rankColor != null && !rankColor.isEmpty()) {
                String stripped = rankColor.replace("§", "");
                if (!stripped.isEmpty()) {
                    ChatColor temp = ChatColor.getByChar(stripped.charAt(0));
                    if (temp != null) chatColor = temp;
                }
            }
            team.setColor(chatColor);
        }

        // Add player to the team
        team.addEntry(player.getName());

        // Optional log for debugging
        // plugin.getLogger().info("✅ Updated nametag for " + player.getName() + " with rank: " + rank);
    }

    public String getRankPrefix(String rank) {
        if (ranksConfig.getBoolean("prefixes")) {
            String rawPrefix = ranksConfig.getString("rank-prefixes." + rank.toUpperCase(), "");
            return ChatUtil.swapAmp(rawPrefix);
        } else {
            return getRankColor(rank);
        }
    }

    public String getRankColor(String rank) {
        String rawColor = ranksConfig.getString("rank-colors." + rank.toUpperCase(), "&7");
        String formattedColor = ChatUtil.swapAmp(rawColor);

        if (formattedColor.contains("§l")) {
            formattedColor = formattedColor.replaceAll("(§l)+", "§l");
        }

        return formattedColor;
    }

    public String getChatColor(String rank) {
        String raw = ranksConfig.getString("rank-chat-colors." + rank.toUpperCase(), "&f");
        return ChatUtil.swapAmp(raw);
    }

    public List<String> getRanks() {
        if (!ranksConfig.isConfigurationSection("ranks")) {
            plugin.getLogger().severe("⛔ No ranks found in config! Using default.");
            return Arrays.asList("OWNER", "DEVELOPER", "ADMIN", "MOD", "HELPER", "YOUTUBE", "VIP", "MVP", "ELITE", "PRO", "MEMBER");
        }

        return new ArrayList<>(ranksConfig.getConfigurationSection("ranks").getKeys(false));
    }

    public void storeTargetUUID(Player player, UUID targetUUID) {
        if (targetUUID == null) {
            return;
        }
        playerTargetMap.put(player.getUniqueId(), targetUUID);
    }

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
