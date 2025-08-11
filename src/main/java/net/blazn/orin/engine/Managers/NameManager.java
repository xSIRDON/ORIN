package net.blazn.orin.engine.Managers;

import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
                     "CREATE TABLE IF NOT EXISTS player_names (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16), nickname VARCHAR(32))")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error creating player_names table: " + e.getMessage());
        }
    }

    /**
     * Gets a player's current display name.
     */
    public String getDisplayName(Player player) {
        return player.getDisplayName(); // Directly return the live display name
    }

    /**
     * Gets a player's display name (nickname or real name) even if they're offline.
     */
    public String getDisplayName(UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getDisplayName();
        }

        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT nickname, name FROM player_names WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nickname = rs.getString("nickname");
                String realName = rs.getString("name");

                // ✅ If nickname is set, return it, otherwise return real name
                if (nickname != null && !nickname.isEmpty()) {
                    return nickname;
                }
                if (realName != null && !realName.isEmpty()) {
                    return realName;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error retrieving display name: " + e.getMessage());
        }

        return "Unknown Player"; // Default fallback
    }

    /**
     * Updates a player's display name (nickname and real name) in the database.
     *
     * @param uuid        The UUID of the player.
     * @param nickname    The new nickname of the player. Can be null or empty if not applicable.
     * @param realName    The new real name of the player. Should not be null or empty.
     * @return            True if the update was successful, false otherwise.
     */
    public boolean updateDisplayName(UUID uuid, String nickname, String realName) {
        String sql = "UPDATE player_names SET nickname = ?, name = ? WHERE uuid = ?";
        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nickname);
            ps.setString(2, realName);
            ps.setString(3, uuid.toString());
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error updating display name: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a player has a stored nickname in the database.
     *
     * @param uuid The UUID of the player.
     * @return True if the player has a nickname, false otherwise.
     */
    public boolean hasStoredNickname(UUID uuid) {
        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT nickname FROM player_names WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nickname = rs.getString("nickname");
                return nickname != null && !nickname.isEmpty(); // ✅ Returns true if nickname exists
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error checking stored nickname: " + e.getMessage());
        }
        return false; // ✅ Return false if there's an error or no nickname
    }

    public String getStoredNickname(UUID uuid) {
        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT nickname FROM player_names WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("nickname");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error retrieving stored nickname: " + e.getMessage());
        }
        return null; // Return null if no nickname is set
    }

    /**
     * Gets a player's real username (not their nickname).
     */
    public String getRealName(UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }

        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name FROM player_names WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error retrieving real name: " + e.getMessage());
        }

        return "Unknown Player";
    }

    /**
     * Sets or updates a player's nickname.
     */
    public void setNickname(UUID uuid, String nickname) {
        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE player_names SET nickname = ? WHERE uuid = ?")) {
            ps.setString(1, nickname);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error setting nickname: " + e.getMessage());
        }
    }

    /**
     * Removes a player's nickname (resets to their real name).
     */
    public void removeNickname(UUID uuid) {
        setNickname(uuid, null);
    }

    /**
     * Stores a player's real name in the database (if they're new).
     */
    public void registerPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        try (Connection conn = sqlManager.getConnection();
             PreparedStatement check = conn.prepareStatement("SELECT name FROM player_names WHERE uuid = ?");
             PreparedStatement insert = conn.prepareStatement("INSERT INTO player_names (uuid, name, nickname) VALUES (?, ?, ?)")) {

            check.setString(1, uuid.toString());
            ResultSet rs = check.executeQuery();

            if (!rs.next()) { // If the player is not in the database, insert them
                insert.setString(1, uuid.toString());
                insert.setString(2, player.getName());
                insert.setNull(3, java.sql.Types.VARCHAR); // Ensure `nickname` starts as NULL
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error registering player: " + e.getMessage());
        }
    }

    /**
     * Checks if a player exists in the database.
     */
    public boolean playerExists(UUID uuid) {
        Connection conn = sqlManager.getConnection();
        if (conn == null) {
            plugin.getLogger().severe("❌ Cannot check player existence: Database connection is NULL!");
            return false;
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM player_names WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next(); // ✅ Returns true if player exists
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQL Error in playerExists(): " + e.getMessage());
        }
        return false; // ✅ Return false if there's an error
    }

    /**
     * ✅ Checks if a player exists in the database by their name.
     *
     * @param playerName The player's in-game name.
     * @return True if the player exists, false otherwise.
     */
    public boolean playerExists(String playerName) {
        // ✅ Check if the player is currently online
        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return true; // ✅ Player is online, they exist
        }

        // ✅ Check the database for an offline player
        try (Connection conn = sqlManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM player_names WHERE name = ?")) {
            ps.setString(1, playerName);
            ResultSet rs = ps.executeQuery();
            return rs.next(); // ✅ Returns true if player exists in database
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQL Error in playerExists(): " + e.getMessage());
        }
        return false; // ✅ Return false if there's an error or no match
    }

}
