package net.blazn.orin.engine.Managers;

import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

public class SQLManager {

    private Connection connection;
    private final JavaPlugin plugin;
    private long lastReconnectionAttempt = 0; // 🔹 Prevents spam reconnect attempts

    public SQLManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupDatabase();
    }

    /**
     * ✅ Sets up the database connection and initializes tables.
     */
    private void setupDatabase() {
        FileConfiguration config = plugin.getConfig();

        // ✅ Ensure database settings exist
        String host = config.getString("database.host", "localhost");
        String port = config.getString("database.port", "3306");
        String db = config.getString("database.name", "minecraft");
        String user = config.getString("database.user", "root");
        String pass = config.getString("database.password", "password");

        if (host == null || port == null || db == null || user == null || pass == null) {
            plugin.getLogger().severe("❌ Database configuration is missing values!");
            return;
        }

        try {
            // ✅ Improved MySQL connection URL with better settings
            String url = "jdbc:mysql://" + host + ":" + port + "/" + db +
                    "?useSSL=false&allowPublicKeyRetrieval=true&autoReconnect=true" +
                    "&characterEncoding=utf8&tcpKeepAlive=true&socketTimeout=30000";

            connection = DriverManager.getConnection(url, user, pass);

            if (connection == null || connection.isClosed()) {
                plugin.getLogger().severe("❌ Database connection failed!");
                return;
            }

            plugin.getLogger().info("✅ Successfully connected to the database.");

            // ✅ Ensure tables exist
            executeUpdate("CREATE TABLE IF NOT EXISTS ranks (uuid VARCHAR(36) PRIMARY KEY, `rank` VARCHAR(20) NOT NULL)");
            executeUpdate("CREATE TABLE IF NOT EXISTS player_names (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16) NOT NULL, nickname VARCHAR(32) DEFAULT NULL)");

            // ✅ Ensure all required columns exist
            ensureTableColumnExists("player_names", "nickname", "VARCHAR(32) DEFAULT NULL");

        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Could not connect to database: " + e.getMessage());
            connection = null; // Prevent further invalid queries
        }
    }

    /**
     * ✅ Ensures the database connection is active, otherwise reconnects (with delay to prevent spam).
     */
    private boolean ensureConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                return true;
            }

            // 🔹 Prevent constant reconnect spam
            long now = System.currentTimeMillis();
            if (now - lastReconnectionAttempt < 5000) { // Only retry every 5 seconds
                return false;
            }
            lastReconnectionAttempt = now;

            plugin.getLogger().warning("⛔ Database connection was closed. Reconnecting...");
            setupDatabase();

            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Database reconnection failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Executes a SQL update (CREATE, INSERT, DELETE, etc.).
     */
    public void executeUpdate(String sql) {
        if (!ensureConnection()) return;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQL Error: " + e.getMessage());
        }
    }

    /**
     * ✅ Ensures a specific column exists in a table.
     */
    private void ensureTableColumnExists(String tableName, String columnName, String columnType) {
        if (!ensureConnection()) return;

        try (PreparedStatement checkColumn = connection.prepareStatement("SHOW COLUMNS FROM " + tableName + " LIKE ?")) {
            checkColumn.setString(1, columnName);
            ResultSet rs = checkColumn.executeQuery();

            if (!rs.next()) {
                plugin.getLogger().info("✅ Fixing missing column '" + columnName + "' in table '" + tableName + "'.");
                executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error ensuring column exists: " + e.getMessage());
        }
    }

    /**
     * ✅ Gets the database connection.
     */
    public Connection getConnection() {
        if (!ensureConnection()) {
            plugin.getLogger().severe("❌ Database connection is NULL! Reconnecting...");
            setupDatabase();
        }
        return connection;
    }


    /**
     * ✅ Closes the database connection properly.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("✅ Database connection closed successfully.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error closing database connection: " + e.getMessage());
        }
    }
}
