package net.polar;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.event.EventListener;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.timer.ExecutionType;
import net.polar.command.ReloadCommand;
import net.polar.command.TPSCommand;
import net.polar.database.MongoDB;
import net.polar.event.listeners.*;
import net.polar.gui.Gui;
import net.polar.parkour.ParkourManager;
import net.polar.player.HubPlayerProvider;
import net.polar.profiler.ServerProfiler;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

public final class Hub {

    private static final Hub instance = new Hub();
    private static final String LOCAL_PATH = URLDecoder.decode(Hub.class.getProtectionDomain().getCodeSource().getLocation().getPath(), StandardCharsets.UTF_8);
    private static final File LOCAL_FILE = new File(LOCAL_PATH.substring(0, LOCAL_PATH.lastIndexOf("/")));
    private static YamlDocument config;
    private static YamlDocument selectorConfig;
    private static InstanceContainer world;

    private static MongoDB database;
    private static Gui serverSelectorGui;
    private static ItemStack serverSelectorItem;
    private static int wumpusEntityId;

    private static ParkourManager parkourManager;

    private static Team team;

    private Hub() {}

    private void onEnable() {
        MinecraftServer.setBrandName(config.getString("server.brand", "Minestom"));
        database = SetupImpl.loadDatabase(config);
        SetupImpl.pasteSpawn(config, LOCAL_FILE, world);
        parkourManager = new ParkourManager(LOCAL_FILE, world);
        team = MinecraftServer.getTeamManager().createTeam("players");
        team.setCollisionRule(TeamsPacket.CollisionRule.NEVER);
        serverSelectorItem = SetupImpl.createServerSelectorItem(selectorConfig);
        serverSelectorGui = SetupImpl.createServerSelectorGui(selectorConfig);
        MinecraftServer.getSchedulerManager().buildTask(() -> serverSelectorGui = SetupImpl.createServerSelectorGui(selectorConfig)).repeat(Duration.ofSeconds(3)).executionType(ExecutionType.ASYNC).schedule();
        wumpusEntityId = SetupImpl.createWumpus(config, world).getEntityId();
        registerListeners(
                new SpawnListener(),
                new ServerListPingListener(),
                new JumpListener(),
                new InteractionListener(),
                new AttackListener(),
                new InventoryPreClickListener(),
                new ItemInteractionListener(),
                new DisconnectListener(),
                new BlockBreakListener(),
                new PlayerChatListener(),
                new PlayerDropItemListener(),
                new DamageListener()
        );
        registerCommands(
                new ReloadCommand(),
                new TPSCommand()
        );
    }

    private void onDisable() {
        database.close();
    }

    private static void registerListeners(@NotNull EventListener<?>... listeners) {
        Arrays.stream(listeners).forEach(listener -> MinecraftServer.getGlobalEventHandler().addListener(listener));
    }

    private static void registerCommands(@NotNull Command... commands) {
        Arrays.stream(commands).forEach(command -> MinecraftServer.getCommandManager().register(command));
    }

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();
        SetupImpl.loadConfig(LOCAL_FILE);
        SetupImpl.loadWorld(config);
        String[] host = config.getString("server.host", "0.0.0.0:25565").split(":");
        instance.onEnable();
        MinecraftServer.getSchedulerManager().buildShutdownTask(instance::onDisable);
        ServerProfiler.init();
        MinecraftServer.getConnectionManager().setPlayerProvider(new HubPlayerProvider());
        SetupImpl.checkProxyAndOnlineMode(config);
        server.start(host[0], Integer.parseInt(host[1]));
    }
    public static YamlDocument getConfig() {
        return config;
    }
    static void setConfig(YamlDocument config) {Hub.config = config;}

    public static InstanceContainer getWorld() {
        return world;
    }
    static void setWorld(InstanceContainer world) {Hub.world = world;}

    public static int getWumpusEntityId() {
        return wumpusEntityId;
    }
    static void setSelectorConfig(YamlDocument selectorConfig) {Hub.selectorConfig = selectorConfig;}

    public static Gui getServerSelectorGui() {
        return serverSelectorGui;
    }

    public static ItemStack getServerSelectorItem() {
        return serverSelectorItem;
    }

    public static Team getTeam() {
        return team;
    }

    public static MongoDB getDatabase() {
        return database;
    }

    public static ParkourManager getParkourManager() {
        return parkourManager;
    }
}
