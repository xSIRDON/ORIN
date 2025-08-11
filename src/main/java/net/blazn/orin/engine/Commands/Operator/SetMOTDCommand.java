package net.blazn.orin.engine.Commands.Operator;

import net.blazn.orin.engine.Managers.MOTDManager;
import net.blazn.orin.engine.Managers.NameManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.List;
import java.util.Properties;

public class SetMOTDCommand implements CommandExecutor {

    private final RankManager rankManager;
    private final NameManager nameManager;
    private final MOTDManager motdManager;
    private final JavaPlugin plugin;

    private static final List<String> ALLOWED_RANKS = List.of("OWNER", "DEVELOPER", "ADMIN");

    public SetMOTDCommand(JavaPlugin plugin, RankManager rankManager, NameManager nameManager, MOTDManager motdManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.nameManager = nameManager;
        this.motdManager = motdManager;
        plugin.getCommand("setmotd").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length < 1) {
            sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ": " + ChatUtil.white + "/setmotd <message> (use \\n for line breaks)");
            return true;
        }

        if (sender instanceof Player player && !ALLOWED_RANKS.contains(rankManager.getRank(player))) {
            sender.sendMessage(ChatUtil.noPermission);
            return true;
        }

        String raw = String.join(" ", args);
        String formatted = ChatColor.translateAlternateColorCodes('&', raw.replace("\\n", "\n"));

        // Update MotdManager
        motdManager.setMotd(formatted);

        // Update server.properties file
        try {
            File file = new File("server.properties");
            Properties props = new Properties();

            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            }

            props.setProperty("motd", formatted.replace("\n", "\\n"));

            try (FileOutputStream out = new FileOutputStream(file)) {
                props.store(out, null);
            }

            sender.sendMessage(ChatUtil.green + "✔ " + ChatUtil.white + "Updated the server MOTD to:");
            for (String line : formatted.split("\n")) {
                sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.RESET + line);
            }

        } catch (IOException e) {
            sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " Failed to update server.properties");
            e.printStackTrace();
        }

        return true;
    }
}
