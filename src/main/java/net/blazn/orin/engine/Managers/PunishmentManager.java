package net.blazn.orin.engine.Managers;

import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class PunishmentManager {

    private final SQLManager sqlManager;
    private final JavaPlugin plugin;
    private static final int MAX_REASON_LENGTH = 255;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");

    private final EnumMap<PunishmentType, Map<UUID, String>> pendingPunishments = new EnumMap<>(PunishmentType.class);

    public PunishmentManager(JavaPlugin plugin, SQLManager sqlManager) {
        this.plugin = plugin;
        this.sqlManager = sqlManager;
        createPunishmentTable();
        for (PunishmentType type : PunishmentType.values()) {
            pendingPunishments.put(type, new HashMap<>());
        }
    }

    private void createPunishmentTable() {
        String sql = "CREATE TABLE IF NOT EXISTS punishments (" +
                "uuid VARCHAR(36) NOT NULL," +
                "name VARCHAR(16) NOT NULL," +
                "type ENUM('WARN', 'KICK', 'MUTE', 'TEMP_MUTE', 'UNMUTE', 'BAN', 'TEMP_BAN', 'UNBAN') NOT NULL," +
                "reason VARCHAR(255) NOT NULL," +
                "staff VARCHAR(32) NOT NULL," +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "expires TIMESTAMP NULL DEFAULT NULL," +
                "banned TINYINT(1) DEFAULT 0," +
                "tempbanned TINYINT(1) DEFAULT 0," +
                "muted TINYINT(1) DEFAULT 0," +
                "tempmuted TINYINT(1) DEFAULT 0," +
                "kick_count INT DEFAULT 0," +
                "mute_count INT DEFAULT 0," +
                "warn_count INT DEFAULT 0" +
                ")";

        sqlManager.executeUpdate(sql);
    }

    /** ✅ Generic punishment handler */
    private void applyPunishment(String targetName, String staffName, String type, String reason, Long duration) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();
        reason = sanitizeReason(reason);
        Timestamp expiry = (duration != null) ? new Timestamp(System.currentTimeMillis() + duration) : null;

        String sql = "INSERT INTO punishments (uuid, name, type, reason, staff, expires, banned, tempbanned, muted, tempmuted) " +
                "VALUES (?, ?, ?, ?, ?, ?, " +
                "(CASE WHEN ? = 'BAN' THEN 1 ELSE 0 END), " +
                "(CASE WHEN ? = 'TEMP_BAN' THEN 1 ELSE 0 END), " +
                "(CASE WHEN ? = 'MUTE' THEN 1 ELSE 0 END), " +
                "(CASE WHEN ? = 'TEMP_MUTE' THEN 1 ELSE 0 END))";

        try (Connection conn = sqlManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, targetUUID.toString());
            stmt.setString(2, targetName);
            stmt.setString(3, type);
            stmt.setString(4, reason);
            stmt.setString(5, staffName);
            stmt.setTimestamp(6, expiry);
            stmt.setString(7, type);
            stmt.setString(8, type);
            stmt.setString(9, type);
            stmt.setString(10, type);
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ Failed to apply punishment: " + e.getMessage());
        }

        if (target.isOnline()) {
            Player player = target.getPlayer();
            assert player != null;

            // ✅ Different messages for bans, tempbans, and kicks
            String kickMessage;
            if (type.equalsIgnoreCase("BAN")) {
                kickMessage = "§4⛔ §cYou have been§8: §f§lPERMANENTLY BANNED §4⛔\n" +
                        "§6§lReason§8: §f" + reason + "\n" +
                        "§5§lAppeal at§8: §f" + plugin.getConfig().getString("server-website") + "/appeal";
                player.kickPlayer(kickMessage);
            } else if (type.equalsIgnoreCase("TEMP_BAN")) {
                kickMessage = "§4⛔ §cYou have been§8: §f§lTEMPORARILY BANNED §4⛔\n" +
                        "§6§lReason§8: §f" + reason + "\n" +
                        "§e§lExpires§8: §f" + dateFormat.format(expiry) + "\n" +
                        "§5§lAppeal at§8: §f" + plugin.getConfig().getString("server-website") + "/appeal";
                player.kickPlayer(kickMessage);
            } else if (type.equalsIgnoreCase("KICK")){ // ✅ Kicks
                kickMessage = "§4⛔ §cYou have been§8: §f§lKICKED §4⛔\n" +
                        "§6§lReason§8: §f" + reason;
                player.kickPlayer(kickMessage);
            }
        }
    }


    /** ✅ Ban player */
    public void banPlayer(String targetName, String staffName, String reason) {
        applyPunishment(targetName, staffName, "BAN", reason, null);
    }

    /** ✅ Temp-ban player */
    public void tempBanPlayer(String targetName, String staffName, String reason, long duration) {
        applyPunishment(targetName, staffName, "TEMP_BAN", reason, duration);
    }

    /** ✅ Kick player */
    public void kickPlayer(Player target, String staffName, String reason) {
        applyPunishment(target.getName(), staffName, "KICK", reason, null);
    }

    /** ✅ Mute player */
    public void mutePlayer(String targetName, String staffName, String reason) {
        applyPunishment(targetName, staffName, "MUTE", reason, null);
    }

    /** ✅ Temp-mute player */
    public void tempMutePlayer(String targetName, String staffName, String reason, long duration) {
        applyPunishment(targetName, staffName, "TEMP_MUTE", reason, duration);
    }

    /** ✅ Unban player */
    public void unbanPlayer(String targetName, String staffName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();

        try (Connection conn = sqlManager.getConnection()) {
            conn.setAutoCommit(false); // ✅ Begin transaction

            // ✅ Step 1: Update punishment table to remove active ban statuses
            String updateSQL = "UPDATE punishments SET banned = 0, tempbanned = 0 WHERE uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateSQL)) {
                stmt.setString(1, targetUUID.toString());
                stmt.executeUpdate();
            }

            // ✅ Step 2: Log the unban event for historical records
            String insertSQL = "INSERT INTO punishments (uuid, name, type, reason, staff) VALUES (?, ?, 'UNBAN', ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
                stmt.setString(1, targetUUID.toString());
                stmt.setString(2, targetName);
                stmt.setString(3, "N/A");
                stmt.setString(4, staffName);
                stmt.executeUpdate();
            }

            conn.commit(); // ✅ Commit transaction

            // ✅ Log to console
            Bukkit.getLogger().info("✔ " + targetName + " has been unbanned by " + staffName);

        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ Failed to unban player: " + e.getMessage());
        }
    }

    /** ✅ Unmute player */
    public void unmutePlayer(String targetName, String staffName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();

        try (Connection conn = sqlManager.getConnection()) {
            conn.setAutoCommit(false); // ✅ Begin transaction

            // ✅ Step 1: Update punishment table to remove active mute statuses
            String updateSQL = "UPDATE punishments SET muted = 0, tempmuted = 0 WHERE uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateSQL)) {
                stmt.setString(1, targetUUID.toString());
                stmt.executeUpdate();
            }

            // ✅ Step 2: Log the unmute event for historical records
            String insertSQL = "INSERT INTO punishments (uuid, name, type, reason, staff) VALUES (?, ?, 'UNMUTE', ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
                stmt.setString(1, targetUUID.toString());
                stmt.setString(2, targetName);
                stmt.setString(3, "N/A");
                stmt.setString(4, staffName);
                stmt.executeUpdate();
            }

            conn.commit(); // ✅ Commit transaction

            // ✅ Notify console
            Bukkit.getLogger().info("✔ " + targetName + " has been unmuted by " + staffName);

            // ✅ Notify player if online
            if (target.isOnline()) {
                Player player = target.getPlayer();
                if (player != null) {
                    player.sendMessage(ChatUtil.serverPrefix + ChatUtil.green + "✔ " + ChatUtil.white + "You have been unmuted.");
                }
            }

        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ Failed to unmute player: " + e.getMessage());
        }
    }


    /** ✅ Warn player */
    public void warnPlayer(String targetName, String staffName, String reason) {
        applyPunishment(targetName, staffName, "WARN", reason, null);
    }

    /** ✅ Checks if a player is currently banned */
    public boolean isBanned(UUID playerUUID) {
        return checkPunishmentStatus(playerUUID, "banned");
    }

    /** ✅ Checks if a player is currently temp banned */
    public boolean isTempBanned(UUID playerUUID) {
        return checkPunishmentStatus(playerUUID, "tempbanned");
    }

    /** ✅ Checks if a player is currently muted */
    public boolean isMuted(UUID playerUUID) {
        return checkPunishmentStatus(playerUUID, "muted");
    }

    /** ✅ Checks if a player is temporarily muted */
    public boolean isTempMuted(UUID playerUUID) {
        return checkPunishmentStatus(playerUUID, "tempmuted");
    }

    private boolean checkPunishmentStatus(UUID playerUUID, String column) {
        String sql = "SELECT " + column + " FROM punishments WHERE uuid = ? AND " + column + " = 1 LIMIT 1";

        try (Connection conn = sqlManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failed to check punishment status: " + e.getMessage());
        }
        return false;
    }

    /** ✅ Clears expired temp punishments */
    public void cleanupExpiredPunishments() {
        sqlManager.executeUpdate("DELETE FROM punishments WHERE expires IS NOT NULL AND expires < NOW()");
    }

    /** ✅ Handles pending punishments */
    public void setPendingPunishment(PunishmentType type, UUID targetUUID, String reason) {
        pendingPunishments.get(type).put(targetUUID, reason);
    }

    public String getPendingPunishment(UUID targetUUID, PunishmentType type) {
        return pendingPunishments.get(type).get(targetUUID);
    }

    /** ✅ Clears a pending punishment */
    public void clearPendingPunishment(UUID targetUUID, PunishmentType type) {
        pendingPunishments.get(type).remove(targetUUID);
    }

    /** ✅ Retrieves the target UUID for a staff member's pending punishment */
    public UUID getPendingPunishmentTarget(Player staff, PunishmentType type) {
        return pendingPunishments.get(type).entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    /** ✅ Sanitizes reason */
    private String sanitizeReason(String reason) {
        return reason.length() > MAX_REASON_LENGTH ? reason.substring(0, MAX_REASON_LENGTH - 3) + "..." : reason.replace("'", "’");
    }

    public enum PunishmentType {
        BAN, TEMP_BAN, MUTE, TEMP_MUTE, KICK, WARN
    }

    /** ✅ Retrieves the most recent ban reason for a player */
    public String getBanReason(UUID playerUUID) {
        String sql = "SELECT reason FROM punishments WHERE uuid = ? AND type IN ('BAN', 'TEMP_BAN') ORDER BY timestamp DESC LIMIT 1";

        try (Connection conn = sqlManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("reason") : "Unknown";
        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ Failed to fetch ban reason: " + e.getMessage());
        }
        return "Unknown";
    }

    /** ✅ Gets ban expiry for a temp-banned player */
    public String getBanExpiry(UUID playerUUID) {
        String sql = "SELECT expires FROM punishments WHERE uuid = ? AND type = 'TEMP_BAN' AND expires > NOW() ORDER BY expires DESC LIMIT 1";

        try (Connection conn = sqlManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp expiry = rs.getTimestamp("expires");
                return expiry != null ? dateFormat.format(expiry) : "N/A";
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ Failed to fetch ban expiry: " + e.getMessage());
        }
        return "N/A";
    }

    /** ✅ Gets mute expiry for a temp-muted player */
    public String getMuteExpiry(UUID playerUUID) {
        String sql = "SELECT expires FROM punishments WHERE uuid = ? AND type = 'TEMP_MUTE' AND expires > NOW() ORDER BY expires DESC LIMIT 1";

        try (Connection conn = sqlManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp expiry = rs.getTimestamp("expires");
                return expiry != null ? dateFormat.format(expiry) : "N/A";
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ Failed to fetch mute expiry: " + e.getMessage());
        }
        return "N/A";
    }

    public Map<String, List<String>> getPunishmentHistory(UUID playerUUID) {
        Map<String, List<String>> history = new LinkedHashMap<>();
        history.put("BANS", new ArrayList<>());
        history.put("TEMP_BANS", new ArrayList<>());
        history.put("UNBANS", new ArrayList<>());
        history.put("MUTES", new ArrayList<>());
        history.put("TEMP_MUTES", new ArrayList<>());
        history.put("UNMUTES", new ArrayList<>());
        history.put("KICKS", new ArrayList<>());
        history.put("WARNS", new ArrayList<>());

        String sql = "SELECT type, reason, staff, timestamp, expires FROM punishments WHERE uuid = ? ORDER BY timestamp DESC";
        SimpleDateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yy");

        try (Connection conn = sqlManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String type = rs.getString("type");
                String reason = rs.getString("reason");
                String staff = rs.getString("staff");
                Timestamp timestamp = rs.getTimestamp("timestamp");
                Timestamp expires = rs.getTimestamp("expires");

                String formattedDate = shortDateFormat.format(timestamp);
                String finalDate = formattedDate.replace("/", ChatUtil.gray + "/" + ChatUtil.white);
                String formattedExpires = null;
                String finalExpires = null;
                if (expires != null) {
                    formattedExpires = shortDateFormat.format(expires);
                    finalExpires = formattedExpires.replace("/", ChatUtil.gray + "/" + ChatUtil.white);
                }
                String formattedEntry = ChatUtil.white + finalDate + ChatUtil.darkGray + " | " +
                        ChatUtil.darkGreen + "Reason" + ChatUtil.darkGray + ": " + ChatUtil.white + reason +
                        ChatUtil.darkGray + " | " + ChatUtil.darkGreen + "By" + ChatUtil.darkGray + ": " + ChatUtil.white + staff;

                if (expires != null && ("TEMP_BAN".equals(type) || "TEMP_MUTE".equals(type))) {
                    formattedEntry += ChatUtil.darkGray + " | " + ChatUtil.darkGreen + "Expires" + ChatUtil.darkGray + ": " +
                            ChatUtil.white + finalExpires;
                }

                switch (type) {
                    case "BAN":
                        history.get("BANS").add(formattedEntry);
                        break;
                    case "TEMP_BAN":
                        history.get("TEMP_BANS").add(formattedEntry);
                        break;
                    case "UNBAN":
                        history.get("UNBANS").add(formattedEntry);
                        break;
                    case "MUTE":
                        history.get("MUTES").add(formattedEntry);
                        break;
                    case "TEMP_MUTE":
                        history.get("TEMP_MUTES").add(formattedEntry);
                        break;
                    case "UNMUTE":
                        history.get("UNMUTES").add(formattedEntry);
                        break;
                    case "KICK":
                        history.get("KICKS").add(formattedEntry);
                        break;
                    case "WARN":
                        history.get("WARNS").add(formattedEntry);
                        break;
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ Failed to fetch punishment history: " + e.getMessage());
        }
        return history;
    }

    /**
     * ✅ Clears all punishments for a given player.
     * @param playerUUID The UUID of the player whose history is being cleared.
     */
    public void clearHistory(UUID playerUUID) {
        String sql = "DELETE FROM punishments WHERE uuid = ?";

        try (Connection conn = sqlManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                Bukkit.getLogger().info("✅ Cleared punishment history for UUID: " + playerUUID);
            } else {
                Bukkit.getLogger().info("❌ No punishment history found for UUID: " + playerUUID);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ Failed to clear punishment history: " + e.getMessage());
        }
    }
}
