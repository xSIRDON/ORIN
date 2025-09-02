package net.blazn.orin.engine.Managers;

import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.UUID;

public class NameManager {

    private final SQLManager sqlManager;
    private final JavaPlugin plugin;

    public NameManager(JavaPlugin plugin, SQLManager sqlManager) {
        this.plugin = plugin;
        this.sqlManager = sqlManager;
        setupDatabase();
    }

    private void setupDatabase() {
        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS player_names (" +
                             "uuid VARCHAR(36) PRIMARY KEY, " +
                             "name VARCHAR(16), " +
                             "nickname VARCHAR(32), " +
                             "disguise_name VARCHAR(32))")) { // Added disguise_name
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error creating player_names table: " + e.getMessage());
        }
    }

    /**
     * Gets a player's display name (disguise > nickname > real name)
     */
    public String getDisplayName(Player player) {
        return getDisplayName(player.getUniqueId());
    }

    public String getDisplayName(UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getDisplayName();
        }

        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT disguise_name, nickname, name FROM player_names WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String disguise = rs.getString("disguise_name");
                String nickname = rs.getString("nickname");
                String realName = rs.getString("name");

                if (disguise != null && !disguise.isEmpty()) return disguise;
                if (nickname != null && !nickname.isEmpty()) return nickname;
                if (realName != null && !realName.isEmpty()) return realName;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error retrieving display name: " + e.getMessage());
        }

        return "Unknown Player";
    }

    /**
     * Gets a player's nickname (ignores disguise, returns null if none)
     */
    public String getNickname(Player player) {
        return getNickname(player.getUniqueId());
    }

    public String getNickname(UUID uuid) {
        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT nickname FROM player_names WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nickname = rs.getString("nickname");
                return (nickname != null && !nickname.isEmpty()) ? nickname : null;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error retrieving nickname: " + e.getMessage());
        }
        return null;
    }

    /**
     * Sets or updates a player's nickname
     */
    public void setNickname(UUID uuid, String nickname) {
        updateField(uuid, "nickname", nickname);
    }

    /**
     * Removes a player's nickname
     */
    public void removeNickname(UUID uuid) {
        setNickname(uuid, null);
    }

    /**
     * Sets a player's disguise name
     */
    public void setDisguiseName(UUID uuid, String disguiseName) {
        updateField(uuid, "disguise_name", disguiseName);
    }

    /**
     * Retrieves a player's current disguise name from the database.
     * Returns null if no disguise is set.
     */
    public String getDisguiseName(UUID uuid) {
        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT disguise_name FROM player_names WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String disguise = rs.getString("disguise_name");
                return (disguise != null && !disguise.isEmpty()) ? disguise : null;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error retrieving disguise name: " + e.getMessage());
        }
        return null;
    }

    /**
     * Clears a player's disguise
     */
    public void clearDisguiseName(UUID uuid) {
        setDisguiseName(uuid, null);
    }

    /**
     * Gets a player's real username
     */
    public String getRealName(UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) return onlinePlayer.getName();

        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name FROM player_names WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error retrieving real name: " + e.getMessage());
        }

        return "Unknown Player";
    }

    /**
     * Checks if a player has a stored nickname
     */
    public boolean hasStoredNickname(UUID uuid) {
        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT nickname FROM player_names WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nickname = rs.getString("nickname");
                return nickname != null && !nickname.isEmpty();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error checking stored nickname: " + e.getMessage());
        }
        return false;
    }

    /**
     * Gets stored nickname
     */
    public String getStoredNickname(UUID uuid) {
        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT nickname FROM player_names WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("nickname");
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error retrieving stored nickname: " + e.getMessage());
        }
        return null;
    }

    /**
     * Registers a player in the database
     */
    public void registerPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        try (Connection conn = sqlManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1️⃣ Create table if it doesn't exist
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS player_names (" +
                            "uuid VARCHAR(36) PRIMARY KEY, " +
                            "name VARCHAR(16), " +
                            "nickname VARCHAR(32), " +
                            "disguise_name VARCHAR(32))");

            // 2️⃣ Ensure disguise_name column exists
            ResultSet rs = conn.getMetaData().getColumns(null, null, "player_names", "disguise_name");
            if (!rs.next()) {
                stmt.executeUpdate("ALTER TABLE player_names ADD COLUMN disguise_name VARCHAR(32) NULL");
            }

            // 3️⃣ Check if player exists
            try (PreparedStatement check = conn.prepareStatement("SELECT name FROM player_names WHERE uuid = ?")) {
                check.setString(1, uuid.toString());
                ResultSet rsCheck = check.executeQuery();
                if (!rsCheck.next()) {
                    // Insert player
                    try (PreparedStatement insert = conn.prepareStatement(
                            "INSERT INTO player_names (uuid, name, nickname, disguise_name) VALUES (?, ?, ?, ?)")) {
                        insert.setString(1, uuid.toString());
                        insert.setString(2, player.getName());
                        insert.setNull(3, Types.VARCHAR);
                        insert.setNull(4, Types.VARCHAR);
                        insert.executeUpdate();
                    }
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error registering player: " + e.getMessage());
        }
    }

    /**
     * Checks if a player exists
     */
    public boolean playerExists(UUID uuid) {
        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM player_names WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQL Error in playerExists(): " + e.getMessage());
            return false;
        }
    }

    public boolean playerExists(String playerName) {
        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null) return true;

        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM player_names WHERE name = ?")) {
            ps.setString(1, playerName);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQL Error in playerExists(): " + e.getMessage());
            return false;
        }
    }

    /**
     * Utility method to update a single field
     */
    private void updateField(UUID uuid, String field, String value) {
        String sql = "UPDATE player_names SET " + field + " = ? WHERE uuid = ?";
        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (value != null) {
                ps.setString(1, value);
            } else {
                ps.setNull(1, Types.VARCHAR);
            }
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error updating " + field + ": " + e.getMessage());
        }
    }

    /**
     * Checks if a nickname is already in use by another player.
     */
    public boolean isNicknameTaken(String nickname) {
        if (nickname == null || nickname.isEmpty()) return false;

        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT uuid FROM player_names WHERE nickname = ?")) {
            ps.setString(1, nickname);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error checking nickname uniqueness: " + e.getMessage());
        }
        return false;
    }

    /**
     * Checks if a disguise name is already in use by another player.
     */
    public boolean isDisguiseNameTaken(String disguiseName) {
        if (disguiseName == null || disguiseName.isEmpty()) return false;

        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT uuid FROM player_names WHERE disguise_name = ?")) {
            ps.setString(1, disguiseName);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error checking disguise name uniqueness: " + e.getMessage());
        }
        return false;
    }

    /**
     * Sets a player's nickname if it's not already taken.
     */
    public boolean trySetNickname(UUID uuid, String nickname) {
        if (isNicknameTaken(nickname)) return false;
        setNickname(uuid, nickname);
        return true;
    }

    /**
     * Sets a player's disguise name if it's not already taken.
     */
    public boolean trySetDisguiseName(UUID uuid, String disguiseName) {
        if (isDisguiseNameTaken(disguiseName)) return false;
        setDisguiseName(uuid, disguiseName);
        return true;
    }

}
