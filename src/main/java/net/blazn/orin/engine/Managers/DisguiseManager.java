package net.blazn.orin.engine.Managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.papermc.paper.connection.PlayerConnection;
import net.blazn.orin.Main;
import net.blazn.orin.engine.Utils.ChatUtil;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class DisguiseManager {

    private static final String PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SKIN_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";

    private final JavaPlugin plugin;
    private final NameManager nameManager;
    private final RankManager rankManager;

    // Store original player data so we can restore it
    private final Map<UUID, PlayerProfile> originalProfiles = new HashMap<>();
    private final Map<UUID, String> originalNames = new HashMap<>();
    private final Map<UUID, String> originalRanks = new HashMap<>();

    // Skin & name pools
    private final List<String> disguiseSkins = Arrays.asList(
            "Notch", "jeb_", "Dinnerbone", "Technoblade", "Herobrine", "CaptainSparklez", "DanTDM", "StampyCat",
            "Dream", "GeorgeNotFound", "Sapnap", "BadBoyHalo", "TommyInnit", "Tubbo", "Ranboo", "WilburSoot",
            "Philza", "Illumina", "Antfrost", "Vikkstar123", "MumboJumbo", "Grian", "EthosLab", "Xisumavoid",
            "BdoubleO100", "Zombey", "IBallisticSquid", "JeromeASF", "SSundee", "PopularMMOs", "LDShadowLady",
            "SkyDoesMinecraft", "BajanCanadian", "Logdotzip", "PrestonPlayz", "TheDiamondMinecart", "AntVenom",
            "ChocoTacoMC", "FlamingoMC", "ItsFunneh", "FGTeeV", "MojangStaff", "Mango", "Alex", "Steve", "HerobrineOfficial",
            "CreeperGuy", "EndermanKing", "ZombieLord", "SkeletonSniper", "SlimeMaster", "PiglinKing", "VillagerBob",
            "IronGolemX", "WitherBoss", "DragonSlayer", "ObsidianKnight", "NetherExplorer", "GhastHunter", "BlazeMage",
            "ShulkerBoxer", "MooshroomMike", "AxolotlLad", "ParrotPete", "WolfRider", "CatNapper", "FoxFury",
            "BeeKeeper", "GlowSquid", "LlamaLarry", "StriderSam", "AncientGuardian", "EnderWatcher", "EnderWizard",
            "AncientSteve", "BlockySteve", "PixelHero", "DiamondMiner", "RedstoneEngineer", "PotionMaster", "NetherWalker",
            "CaveDweller", "MountainKing", "OceanExplorer", "SkyPirate", "ForestElf", "IceWizard", "DesertNomad",
            "JungleScout", "SwampShaman", "MagmaKnight", "WitherSlayer", "DragonTamer", "VoidWalker", "BlockWizard",
            "TorchBearer", "PickaxePro", "LuckyMiner", "BuilderBob", "CraftyCarl", "EpicEthan", "FierceFelix", "PixelPanda",
            "CrazyCat", "MysticMoose", "TurboTiger", "SneakySnake", "BouncyBunny", "RapidRabbit", "DaringDuck"
    );

    private final List<String> disguiseNames = Arrays.asList(
            // Plain letter names (~100)
            "PixelRunner", "CraftyFox", "SkyWalker", "BlockHunter", "StoneMage",
            "ForestWanderer", "IronBear", "SilentMiner", "NightCrafter", "CloudRider",
            "TorchBearer", "OneLuckyBuilder", "OceanScout", "iMountainClimber", "xShadowSeekerx",
            "SnowWalker", "yoRiverTamer", "LeafWatcher", "WindChaser", "SunCrafter",
            "PixelMage", "iBlockNomad", "StoneSeeker", "ForestKnight", "CubeHunter",
            "NetherExplorer", "MineSmasher", "SkyHarvester", "RedstoneRover", "CaveScout",
            "SnowMiner", "aDesertWalker", "OceanBuilder", "StormRyder", "xXFlameCasterXx",
            "xIceCrafter", "ShadowWalker", "TorchSeeker", "LuckyMiner", "VoidTamer",
            "SkyRanger", "ForestHunter", "PixelCrafter", "BlockShaper", "StoneNomad",
            "CloudMage", "NightWanderer", "SunWalker", "LeafRider", "WindSeeker",
            "MountainHunter", "RiverCrafter", "BlockWizard", "PixelRanger", "ForestNomad",
            "StoneHunter", "CaveBuilder", "SkyNomad", "OceanCrafter", "TorchHunter",
            "FlameWanderer", "IceRanger", "ShadowCrafter", "LuckyRider", "VoidWalker",
            "CloudHunter", "NightNomad", "SunCrafter", "LeafHunter", "WindWalker",
            "MountainNomad", "RiverHunter", "BlockSeeker", "PixelShaper", "ForestRanger",
            "StoneWalker", "CaveNomad", "SkySeeker", "OceanHunter", "TorchCrafter",
            "FlameRanger", "IceHunter", "ShadowSeeker", "LuckyWalker", "VoidRanger",
            "CloudCrafter", "NightHunter", "SunNoMaD", "LEAFSEEKA", "WindRanger",
            "MountainWalker", "RiverRanger", "BlockHunterX", "PixelNomadX", "ForestSeeker",
            "StoneRanger", "CaveWalker", "SkyHunter", "OceanNomad", "TorchSeekerX",

            // Names with numbers (~100)
            "BlockMaster42", "PixelKing77", "CraftyGamer88", "SkyRider123", "StoneCrafter99",
            "ForestSeeker69", "IronNomad101", "SilentWalker007", "NightHunter420", "CloudRanger900",
            "TorchBearer321", "LuckyMiner777", "OceanExplorer42", "MountainTamer69", "ShadowRider88",
            "SnowWalker007", "RiverCrafter123", "LeafHunter420", "WindSeeker999", "SunCrafter101",
            "PixelMage777", "BlockNomad123", "StoneSeeker69", "ForestKnight88", "CubeHunter900",
            "NetherExplorer007", "MineSmasher420", "SkyHarvester101", "RedstoneRover777", "CaveScout123",
            "SnowMiner999", "DesertWalker420", "OceanBuilder007", "StormRider777", "FlameCaster123",
            "IceCrafter69", "ShadowWalker420", "TorchSeeker900", "LuckyMiner007", "VoidTamer777",
            "SkyRanger123", "ForestHunter420", "PixelCrafter007", "BlockShaper777", "StoneNomad123",
            "CloudMage420", "NightWanderer007", "SunWalker777", "LeafRider123", "WindSeeker420",
            "MountainHunter007", "RiverCrafter777", "BlockWizard123", "PixelRanger420", "ForestNomad007",
            "StoneHunter777", "CaveBuilder123", "SkyNomad420", "OceanCrafter007", "TorchHunter777",
            "FlameWanderer123", "IceRanger420", "ShadowCrafter007", "LuckyRider777", "VoidWalker123",
            "CloudHunter420", "NightNomad007", "SunCrafter777", "LeafHunter123", "WindWalker420",
            "MountainNomad007", "RiverHunter777", "BlockSeeker123", "PixelShaper420", "ForestRanger007",
            "StoneWalker777", "CaveNomad123", "SkySeeker420", "OceanHunter007", "TorchCrafter777",

            // Mixed/underscores/leet style (~100)
            "Block_Buster88", "Redstone_Engineer42", "Epic_Miner777", "Sky_Pirate420", "Forest_Elf99",
            "Ice_Wizard420", "Swamp_Shaman666", "Magma_Knight007", "Ender_Watcher007", "Ancient_Steve123",
            "Pixel_Hero777", "Diamond_Miner420", "Redstone_King88", "Cave_Dweller123", "Mountain_King007",
            "Ocean_Explorer69", "Sky_Knight9000", "Forest_Hunter777", "Ice_Mage420", "Desert_Ranger123",
            "Jungle_Scout88", "Swamp_Warrior007", "Magma_Mage9001", "Dragon_Knight123", "Ender_Knight69",
            "Pixel_Ninja777", "Crafty_Gamer420", "Epic_Builder123", "Lucky_Blocker007", "Redstone_King9000",
            "Block_Legend69", "Cube_Master123", "Minecrafter_X777", "Block_Crafter420", "Diamond_Hunter007",
            "Nether_King123", "Sky_Blocker69", "Epic_Crafter9000", "Pixel_Warrior777", "Ender_Slayer420",
            "Creeper_Crusher123", "Zombie_Slayer007", "Potion_Wizard69", "Torch_Master9001", "Sneaky_Steve123",
            "Mighty_Miner777", "Diamond_Digger420", "Block_Brawler007", "Pixel_Slayer9000", "Forest_Warrior123",
            "Stone_Crafter777", "Cave_Hunter420", "Sky_Explorer007", "Ocean_Walker123", "Torch_Bearer777",
            "Flame_Hunter420", "Ice_Walker007", "Shadow_Knight123", "Lucky_Miner777", "Void_Crafter420",
            "Cloud_Wizard007", "Night_Ranger123", "Sun_Walker777", "Leaf_Rider420", "Wind_Hunter007",
            "Mountain_Walker123", "River_Ranger777", "Block_Hunter420", "Pixel_Nomad007", "Forest_Seeker123",
            "Stone_Ranger777", "Cave_Walker420", "Sky_Hunter007", "Ocean_Nomad123", "Torch_Seeker777"
    );

    private final Random random = new Random();

    public DisguiseManager(JavaPlugin plugin, NameManager nameManager, RankManager rankManager) {
        this.plugin = plugin;
        this.nameManager = nameManager;
        this.rankManager = rankManager;
    }

    /**
     * Disguise a player: random skin, random nickname, visual lower rank
     */
    public void disguise(Player player) {
        UUID uuid = player.getUniqueId();

        if (isDisguised(uuid)) {
            player.sendMessage(ChatUtil.darkRed + "❌ " + ChatUtil.white + "You are already disguised.");
            return;
        }

        // Save original data
        originalProfiles.put(uuid, player.getPlayerProfile());
        originalNames.put(uuid, nameManager.getDisplayName(player));
        originalRanks.put(uuid, rankManager.getRank(player));

        // Pick a random skin
        String skinOwner = disguiseSkins.get(random.nextInt(disguiseSkins.size()));

        // Pick a random disguise name that is not taken
        String fakeName;
        int attempts = 0;
        do {
            fakeName = disguiseNames.get(random.nextInt(disguiseNames.size()));
            attempts++;
            if (attempts > 50) { // fail-safe to prevent infinite loops
                player.sendMessage(ChatUtil.darkRed + "❌ " + ChatUtil.white + "Could not find a unique disguise name.");
                return;
            }
        } while (nameManager.isDisguiseNameTaken(fakeName));

        // Pick a fake visual rank (lower than theirs)
        String fakeRank = rankManager.getRandomLowerRank(player);

        // Apply skin
        PlayerProfile profile = player.getPlayerProfile();
        profile.setProperties(getTextureProperty(skinOwner));
        player.setPlayerProfile(profile);

        // Apply fake name and save to database
        player.setDisplayName(fakeName);
        nameManager.setDisguiseName(uuid, fakeName);

        // Apply visual rank
        rankManager.setFakeRank(player, fakeRank);

        player.sendMessage(ChatUtil.bgreen + "✔" + ChatUtil.white + " You are now disguised.");
    }


    /**
     * Restore original skin, name, and rank
     */
    public void undisguise(Player player) {
        UUID uuid = player.getUniqueId();

        if (!isDisguised(uuid)) return;

        // Restore skin
        PlayerProfile originalProfile = originalProfiles.remove(uuid);
        player.setPlayerProfile(originalProfile);

        // Restore original name
        nameManager.clearDisguiseName(uuid);
        String originalName = originalNames.remove(uuid);
        if (originalName != null) {
            player.setDisplayName(originalName);
            nameManager.setNickname(uuid, originalName);
        }

        // Restore visual rank
        String originalRank = originalRanks.remove(uuid);
        rankManager.clearFakeRank(player);
        if (originalRank != null) {
            rankManager.setFakeRank(player, originalRank);
        }

        // ✅ Clear any disguise flags in RankManager
        rankManager.clearFakeRank(player);

        player.sendMessage(ChatUtil.bgreen + "✔" + ChatUtil.white + " You are no longer disguised.");
    }

    /**
     * Check if a player is currently disguised
     */
    public boolean isDisguised(UUID uuid) {
        return originalProfiles.containsKey(uuid);
    }

    /**
     * Gets Mojang skin property for a given username
     */
    private Collection<ProfileProperty> getTextureProperty(String targetSkin) {
        final String profileResponse = makeRequest(PROFILE_URL + targetSkin);
        final JsonObject profileObject = JsonParser.parseString(profileResponse).getAsJsonObject();
        final String uuid = profileObject.get("id").getAsString();

        final String skinResponse = makeRequest(SKIN_URL.formatted(uuid));
        final JsonObject skinObject = JsonParser.parseString(skinResponse)
                .getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();

        final String value = skinObject.get("value").getAsString();
        final String signature = skinObject.get("signature").getAsString();

        return List.of(new ProfileProperty("textures", value, signature));
    }

    private String makeRequest(String url) {
        try {
            HttpClient httpClient = HttpClient.newBuilder().build();
            HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void setNametag(Player player, String newName) {
    }


    /**
     * Restore disguise from database (if player reconnects while disguised)
     */
    public void restoreDisguiseOnLogin(Player player) {
        UUID uuid = player.getUniqueId();
        String disguiseName = nameManager.getDisguiseName(uuid);
        if (disguiseName != null) {
            player.setDisplayName(disguiseName);
            nameManager.setDisguiseName(uuid, disguiseName);
        }
    }
}
