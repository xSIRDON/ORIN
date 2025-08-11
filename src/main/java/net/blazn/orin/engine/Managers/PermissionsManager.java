package net.blazn.orin.engine.Managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class PermissionsManager {

    private final Map<String, Integer> rankHierarchy = new HashMap<>();
    private final FileConfiguration config;

    public PermissionsManager(FileConfiguration config) {
        this.config = config;
        loadRankHierarchy();
    }

    /**
     * ✅ Loads rank hierarchy from `ranks:` in `config.yml`.
     */
    private void loadRankHierarchy() {
        if (!config.contains("ranks")) {
            config.set("ranks.OWNER", 10);
            config.set("ranks.DEVELOPER", 10);
            config.set("ranks.ADMIN", 8);
            config.set("ranks.SRMOD", 7);
            config.set("ranks.MOD", 6);
            config.set("ranks.VIP", 5);
            config.set("ranks.BUILDER", 4);
            config.set("ranks.EMERALD", 3);
            config.set("ranks.PLATINUM", 2);
            config.set("ranks.DIAMOND", 1);
            config.set("ranks.MEMBER", 0);
        }

        for (String rank : config.getConfigurationSection("ranks").getKeys(false)) {
            rankHierarchy.put(rank.toUpperCase(), config.getInt("ranks." + rank));
        }
    }

    /**
     * ✅ Gets the hierarchy level of a rank.
     */
    public int getRankLevel(String rank) {
        return rankHierarchy.getOrDefault(rank.toUpperCase(), -1);
    }

    /**
     * ✅ Checks if a player has permission to **kick** another player based on rank.
     */
    public boolean canKick(String senderRank, String targetRank) {
        int senderLevel = getRankLevel(senderRank);
        int targetLevel = getRankLevel(targetRank);
        if (senderLevel >= 10) {
            return true;
        } else {
            // Sender can modify target only if their rank level is higher
            return senderLevel > targetLevel;
        }
    }

    /**
     * ✅ Checks if a player has permission to **ban** another player based on rank.
     */
    public boolean canBan(String senderRank, String targetRank) {
        int senderLevel = getRankLevel(senderRank);
        int targetLevel = getRankLevel(targetRank);
        if (senderLevel >= 10) {
            return true;
        } else {
            // Sender can modify target only if their rank level is higher
            return senderLevel > targetLevel;
        }
    }

    /**
     * ✅ Checks if a player has permission to **mute** another player based on rank.
     */
    public boolean canMute(String senderRank, String targetRank) {
        int senderLevel = getRankLevel(senderRank);
        int targetLevel = getRankLevel(targetRank);
        if (senderLevel >= 10) {
            return true;
        } else {
            // Sender can modify target only if their rank level is higher
            return senderLevel > targetLevel;
        }
    }

    /**
     * ✅ Checks if a player has permission to **warn** another player based on rank.
     */
    public boolean canWarn(String senderRank, String targetRank) {
        int senderLevel = getRankLevel(senderRank);
        int targetLevel = getRankLevel(targetRank);
        if (senderLevel >= 10) {
            return true;
        } else {
            // Sender can modify target only if their rank level is higher
            return senderLevel > targetLevel;
        }
    }

    /**
     * ✅ Checks if a player has permission to **change another player's rank**.
     */
    public boolean canChangeRank(String senderRank, String targetRank) {
        if (Objects.equals(senderRank, "OWNER") || Objects.equals(senderRank, "DEVELOPER")) {
            return true;
        }
        return getRankLevel(senderRank) > getRankLevel(targetRank); // Can only change lower ranks
    }

    /**
     * ✅ Determines if a player has **higher join priority** when the server is full.
     */
    public boolean hasJoinPriority(Player player, RankManager rankManager) {
        return getRankLevel(rankManager.getRank(player)) >= 3; // Emerald+ gets join priority
    }

    /**
     * ✅ Finds the **lowest-ranked player online** (for auto-kicking when full).
     */
    public Player getLowestRankedPlayer(List<Player> onlinePlayers, RankManager rankManager) {
        return onlinePlayers.stream()
                .min(Comparator.comparingInt(p -> getRankLevel(rankManager.getRank(p))))
                .orElse(null);
    }

    /**
     * Checks if a player of a given rank can modify another player's settings.
     *
     * @param senderRank The rank of the sender (the one executing the command).
     * @param targetRank The rank of the target (the one being modified).
     * @return true if the sender can modify the target, false otherwise.
     */
    public boolean canModify(String senderRank, String targetRank) {
        int senderLevel = getRankLevel(senderRank);
        int targetLevel = getRankLevel(targetRank);
        if (senderLevel >= 10) {
            return true;
        } else {
            // Sender can modify target only if their rank level is higher
            return senderLevel > targetLevel;
        }
    }

    /**
     * ✅ Checks if a player **can teleport** to another player.
     *
     * - Lower-ranked players (MOD, SRMOD) **CAN teleport** to higher-ups.
     * - Lower-ranked players **CANNOT teleport higher-ups to them**.
     *
     * @param senderRank The rank of the sender (executing teleport).
     * @param targetRank The rank of the target (being teleported).
     * @return true if allowed, false otherwise.
     */
    public boolean canTeleport(String senderRank, String targetRank, boolean isTeleportingToTarget) {
        int senderLevel = getRankLevel(senderRank);
        int targetLevel = getRankLevel(targetRank);

        if (senderLevel >= 10) {
            return true; // Owners & Developers can teleport anyone anywhere
        }

        if (isTeleportingToTarget) {
            return senderLevel <= targetLevel; // Lower ranks CAN teleport TO higher ranks
        } else {
            return senderLevel > targetLevel; // Lower ranks CANNOT teleport higher ranks to them
        }
    }

    public boolean isStaff(String senderRank) {
        int senderLevel = getRankLevel(senderRank);
        if (senderLevel >= getRankLevel("MOD")) {
            return true;
        } else {
            return false;
        }
    }
}
