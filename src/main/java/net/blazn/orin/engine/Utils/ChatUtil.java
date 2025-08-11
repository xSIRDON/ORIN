package net.blazn.orin.engine.Utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChatUtil {

    // Colors
    public static String aqua = "§b", baqua = "§b§l";
    public static String black = "§0", bblack = "§0§l";
    public static String blue = "§9", bblue = "§9§l";
    public static String darkAqua = "§3", bdarkAqua = "§3§l";
    public static String darkBlue = "§1", bdarkBlue = "§1§l";
    public static String darkGray = "§8", bdarkGray = "§8§l";
    public static String darkGreen = "§2", bdarkGreen = "§2§l";
    public static String darkPurple = "§5", bdarkPurple = "§5§l";
    public static String darkRed = "§4", bdarkRed = "§4§l";
    public static String gold = "§6", bgold = "§6§l";
    public static String gray = "§7", bgray = "§7§l";
    public static String green = "§a", bgreen = "§a§l";
    public static String lightPurple = "§d", blightPurple = "§d§l";
    public static String red = "§c", bred = "§c§l";
    public static String white = "§f", bwhite = "§f§l";
    public static String yellow = "§e", byellow = "§e§l";

    public static String grayBar() {
        return ChatColor.GRAY + "┃ ";
    }

    // Formatting
    public static String bold = "§l", italic = "§o", underline = "§n";

    // Prefixes (loaded from config)
    public static String serverPrefix, staffPrefix, warningPrefix;

    // Common messages
    public static String noPermission, onlyPlayers, notOnline, doesNotExist;

    // Load config values
    public static void loadConfig(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        boolean save = false;

        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("server-prefix", "&8[&3&lORIN&8] &f");
        defaults.put("staff-prefix", "&8[&c&lSTAFF&8] &f");
        defaults.put("warning-prefix", "&8[&c&lWARNING&8] &f");

        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            if (!config.contains(entry.getKey())) {
                config.set(entry.getKey(), entry.getValue());
                save = true;
            }
        }

        if (save) plugin.saveConfig();

        serverPrefix = swapAmp(config.getString("server-prefix"));
        staffPrefix = swapAmp(config.getString("staff-prefix"));
        warningPrefix = swapAmp(config.getString("warning-prefix"));

        noPermission = darkRed + "❌" + red + " You do not have permission to do that.";
        onlyPlayers = darkRed + "❌" + red + " This command can only be used by players.";
        notOnline = darkRed + "❌" + red + " That player is not online.";
        doesNotExist = darkRed + "❌" + red + " That player does not exist.";
    }

    public static String fakeCommand(String input) {
        return ChatUtil.red + "Unknown or incomplete command, see below for error\n" + ChatUtil.underline + input + ChatUtil.red + " <--" + ChatUtil.italic + "[HERE]";
    }

    // Color formatting
    public static String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    // Rainbow text
    public static String rainbow(String input) {
        String[] colors = {darkRed, red, gold, yellow, green, darkGreen, aqua, darkAqua, blue, darkBlue, lightPurple, darkPurple};
        return applyColor(input, colors);
    }

    public static String rainbowBold(String text) {
        ChatColor[] colors = {
                ChatColor.DARK_RED, ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW,
                ChatColor.GREEN, ChatColor.DARK_GREEN, ChatColor.AQUA, ChatColor.DARK_AQUA,
                ChatColor.BLUE, ChatColor.DARK_BLUE, ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE
        };

        StringBuilder formatted = new StringBuilder(ChatColor.BOLD.toString()); // Start with bold formatting
        int colorIndex = 0;

        text = ChatColor.stripColor(text); // Remove any existing formatting

        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                formatted.append(c); // Preserve spaces
                continue;
            }

            formatted.append(colors[colorIndex % colors.length]).append(ChatColor.BOLD).append(c);
            colorIndex++;
        }

        return formatted.toString();
    }



    //public static String rainbowBold(String input) {
    //    String[] colors = {bdarkRed, bred, bgold, byellow, bgreen, bdarkGreen, baqua, bdarkAqua, bblue, bdarkBlue, blightPurple, bdarkPurple};
    //    return applyColor(input, colors);
    //}

    private static String applyColor(String input, String[] colors) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            result.append(colors[i % colors.length]).append(input.charAt(i));
        }
        return result.toString();
    }

    // Color stripping and swapping
    public static String stripAmp(String input) {
        if (input == null) return ""; // Prevents NullPointerException
        return input.replaceAll("&([a-fA-F0-9klmnor])", "");
    }

    public static String stripSec(String input) {
        if (input == null) return ""; // Prevents NullPointerException
        return input.replaceAll("§[a-fA-F0-9klmnor]", "");
    }

    public static String swapAmp(String input) {
        if (input == null) return ""; // Prevents NullPointerException
        return input.replace('&', '§');
    }

    public static String swapSec(String input) {
        if (input == null) return ""; // Prevents NullPointerException
        return input.replace('§', '&');
    }
}
