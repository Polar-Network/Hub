package net.polar;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.server.world.DimensionType;
import net.polar.database.MongoDB;
import net.polar.gui.Gui;
import net.polar.gui.GuiClickable;
import net.polar.utils.ChatColor;
import net.polar.utils.NPC;
import net.polar.utils.schematic.Schematic;
import net.polar.utils.schematic.SchematicReader;
import net.polar.utils.schematic.SchematicRotation;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

import static net.polar.Hub.getWorld;

/**
 * Just done to avoid clogging up the main class.
 */
final class SetupImpl {

    private SetupImpl() {}

    static void loadWorld(YamlDocument config) {
        DimensionType dimension = DimensionType.builder(NamespaceID.from(Key.key("minecraft:fullbright")))
                .ambientLight(2.0f)
                .build();
        MinecraftServer.getDimensionTypeManager().addDimension(dimension);
        Hub.setWorld(MinecraftServer.getInstanceManager().createInstanceContainer(dimension));
        MinecraftServer.getGlobalEventHandler().addListener(PlayerLoginEvent.class, event -> {
            event.setSpawningInstance(getWorld());
            event.getPlayer().setRespawnPoint(
                    new Pos(
                            config.getDouble("server.spawn.x", 0.0),
                            config.getDouble("server.spawn.y", 0.0),
                            config.getDouble("server.spawn.z", 0.0)
                    )
            );
        });
    }

    static void loadConfig(File localFolder) {
        final File config = new File(localFolder, "config.yml");
        final File selectorConfig = new File(localFolder, "selector.yml");
        if (!config.exists()) {
            try {
                final InputStream stream = Hub.class.getClassLoader().getResourceAsStream("config.yml");
                if (stream == null) {
                    throw new NullPointerException("Could not find config.yml in resources!");
                }
                Files.copy(stream, config.toPath(), StandardCopyOption.REPLACE_EXISTING);
                stream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Hub.setConfig(YamlDocument.create(config));
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        if (!selectorConfig.exists()) {
            try {
                final InputStream stream = Hub.class.getClassLoader().getResourceAsStream("selector.yml");
                if (stream == null) {
                    throw new NullPointerException("Could not find selector.yml in resources!");
                }
                Files.copy(stream, selectorConfig.toPath(), StandardCopyOption.REPLACE_EXISTING);
                stream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Hub.setSelectorConfig(YamlDocument.create(selectorConfig));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void checkProxyAndOnlineMode(YamlDocument config) {
        boolean onlineMode = config.getBoolean("server.online-mode", true);
        boolean proxyEnabled = config.getBoolean("server.proxy.enabled", false);
        String proxyToken = config.getString("server.proxy.secret");
        if (onlineMode && proxyEnabled) {
            MinecraftServer.LOGGER.warn(ChatColor.color("<red>Online mode is enabled, but proxy is also enabled. Setting the server back to offline mode."));
            onlineMode = false;
        }
        if (proxyToken == null && proxyEnabled) {
            MinecraftServer.LOGGER.warn(ChatColor.color("<red>Proxy is enabled, but no secret token was provided. Disabling proxy."));
            proxyEnabled = false;
        }

        if (onlineMode) {
            MojangAuth.init();
            MinecraftServer.LOGGER.info(ChatColor.color("<green>Online mode is enabled. This server is running in online (secure) mode"));
        }
        else {
            MinecraftServer.LOGGER.info(ChatColor.color("<green>Online mode is disabled. This server is running in offline (insecure) mode"));
        }

        if (proxyEnabled) {
            VelocityProxy.enable(proxyToken);
            MinecraftServer.LOGGER.info(ChatColor.color("<green>Proxy is enabled. This server is now acting like a backend server for a proxy"));
        }
        else {
            MinecraftServer.LOGGER.info(ChatColor.color("<green>Proxy is disabled. This server is now acting like a standalone server"));
        }
    }

    static NPC createWumpus(YamlDocument config, InstanceContainer instance) {
        Section discord = config.getSection("discord");
        Pos location = new Pos(
                new Vec(
                    discord.getDouble("npc.x", 0.0),
                    discord.getDouble("npc.y", 0.0),
                    discord.getDouble("npc.z", 0.0)
                ),
                discord.getFloat("npc.yaw", 0.0f),
                discord.getFloat("npc.pitch", 0.0f)
        );
        List<String> holograms = discord.getStringList("holograms");
        NPC npc = new NPC("", PlayerSkin.fromUuid("970c7a49-7282-4722-91df-a0130c0f6b57"), holograms);
        npc.properlySpawn(location, instance);
        return npc;
    }

    static void pasteSpawn(YamlDocument config, File parentDir, Instance instance) {
        MinecraftServer.LOGGER.info(ChatColor.color("<green>Building spawn..."));
        Section schematic = config.getSection("server").getSection("schematic");
        Point startPoint = new Vec(
                schematic.getDouble("placement.x", 0.0),
                schematic.getDouble("placement.y", 0.0),
                schematic.getDouble("placement.z", 0.0)
        );
        SchematicRotation rotation = SchematicRotation.valueOf(schematic.getString("placement.rotation", "NONE").toUpperCase());
        String schematicName = schematic.getString("name", "spawn.schem");
        final File schematicFile = new File(parentDir, schematicName);
        try {
            Schematic schem = SchematicReader.read(schematicFile.toPath());
            ChunkUtils.optionalLoadAll(instance, schem.getAffectedChunksReflection(rotation, startPoint), null).join();
            schem.build(rotation, null).apply(instance, startPoint, null);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        MinecraftServer.LOGGER.info(ChatColor.color("<green>Spawn built!"));
    }

    static ItemStack createServerSelectorItem(YamlDocument config) {
        Section item = config.getSection("item");
        Material material = Material.fromNamespaceId(item.getString("material", "PAPER"));
        if (material == null) material = Material.COMPASS;

        Component name = ChatColor.color(item.getString("name", "<green>Server Selector"));
        List<Component> lore = ChatColor.color(item.getStringList("lore"));
        return ItemStack.builder(material).displayName(name).lore(lore).build();
    }

    static Gui createServerSelectorGui(YamlDocument config) {
        int rows = config.getInt("size", 6);
        InventoryType type = switch (rows) {
            case 1 -> InventoryType.CHEST_1_ROW;
            case 2 -> InventoryType.CHEST_2_ROW;
            case 3 -> InventoryType.CHEST_3_ROW;
            case 4 -> InventoryType.CHEST_4_ROW;
            case 5 -> InventoryType.CHEST_5_ROW;
            default -> InventoryType.CHEST_6_ROW;
        };
        Gui gui = new Gui(type, "<green>Server Selector");
        Section inv = config.getSection("inventory");
        inv.getKeys().forEach(k -> {
            Section section = inv.getSection(k.toString());
            JsonObject pingServer = pingServer(section.getString("ping-server", "hypixel.net"));
            int online = pingServer.get("players").getAsJsonObject().get("online").getAsInt();
            int max = pingServer.get("players").getAsJsonObject().get("max").getAsInt();
            Material material = Material.fromNamespaceId(section.getString("material", "PAPER"));
            if (material == null) material = Material.COMPASS;

            Component name = ChatColor.color(section.getString("name", "<green>Server Selector"));
            List<String> loreStrings = section.getStringList("lore").stream()
                    .map(s -> s.replace("%online%", String.valueOf(online)))
                    .map(s -> s.replace("%max%", String.valueOf(max)))
                    .collect(Collectors.toList());
            List<Component> lore = ChatColor.color(loreStrings);
            String server = section.getString("server", "lobby");

            GuiClickable clickable = new GuiClickable(
                    ItemStack.builder(material)
                            .displayName(name)
                            .lore(lore)
                            .build(),
                    (player, clickType) -> {
                        player.sendMessage(ChatColor.color("<green>Connecting to <yellow>" + server));
                        player.sendToServer(server);
                    }
            );
            int slot = Integer.parseInt(k.toString());
            gui.addClickable(slot, clickable);
        });
        return gui;
    }

    static MongoDB loadDatabase(YamlDocument config) {
        Section database = config.getSection("database");
        String host = database.getString("host", "mongodb://localhost:27017");
        String name = database.getString("name", "hub");
        String collection = database.getString("collection", "players");
        return new MongoDB(host, name, collection);
    }

    private static JsonObject pingServer(String host) {
        try  {
            URL url = new URL("https://api.minetools.eu/ping/" + host);

            URLConnection connection = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            reader.close();
            return object;
        } catch (IOException e) {
            JsonObject object = new JsonObject();
            object.addProperty("max", 0);
            object.addProperty("online", 0);
            return object;
        }
    }
}
