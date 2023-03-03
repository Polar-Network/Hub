package net.polar.player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.particle.Particle;
import net.minestom.server.particle.ParticleCreator;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import net.polar.Hub;
import net.polar.database.PlayerData;
import net.polar.utils.ChatColor;
import net.polar.utils.Cuboid;
import org.jetbrains.annotations.NotNull;

import java.util.*;


@SuppressWarnings("UnstableApiUsage")
public class HubPlayer extends Player {

    public static final ItemStack SHOW_PLAYERS_ITEM = ItemStack.builder(Material.RED_DYE)
            .displayName(ChatColor.color("<green>Show Players"))
            .lore(List.of(Component.empty(), ChatColor.color("<gray>Click to show players")))
            .build();

    public static final ItemStack HIDE_PLAYERS_ITEM = ItemStack.builder(Material.LIME_DYE)
            .displayName(ChatColor.color("<red>Hide Players"))
            .lore(List.of(Component.empty(), ChatColor.color("<gray>Click to hide players")))
            .build();

    public static final ItemStack RESET_PARKOUR_ITEM = ItemStack.builder(Material.TNT_MINECART)
            .displayName(ChatColor.color("<red>Reset Parkour"))
            .lore(List.of(Component.empty(), ChatColor.color("<gray>Click to reset your parkour progress")))
            .build();

    public static final ItemStack EXIT_PARKOUR_ITEM = ItemStack.builder(Material.IRON_DOOR)
            .displayName(ChatColor.color("<red>Exit Parkour"))
            .lore(List.of(Component.empty(), ChatColor.color("<gray>Click to exit parkour")))
            .build();

    public static final ItemStack LAST_CHECKPOINT_ITEM = ItemStack.builder(Material.REDSTONE_TORCH)
            .displayName(ChatColor.color("<green>Last Checkpoint"))
            .lore(List.of(Component.empty(), ChatColor.color("<gray>Click to teleport to your last checkpoint")))
            .build();

    private final PlayerData data;

    public HubPlayer(
            @NotNull UUID uuid,
            @NotNull String username,
            @NotNull PlayerConnection playerConnection,
            @NotNull PlayerData data
    ) {
        super(uuid, username, playerConnection);
        this.data = data;
    }

    private long lastDoubleJumpTime = 0L; // Last time the player double jumped, on join it's 0
    private boolean doubleJumpEnabled = true;
    private boolean inNormalParkour = false;
    private boolean inElytraParkour = false;
    private boolean hidPlayers = false;

    private long parkourStartTime = 0L;
    private int lastCheckpoint = 0;
    private long checkpointStartTime = 0L;

    private long resetLock = 0L;

    private boolean particleLock = true;

    @Override
    public void tick(long time) {
        super.tick(time);
        if (((System.currentTimeMillis() - lastDoubleJumpTime) >= Hub.getConfig().getInt("double-jump.delay", 1) * 1000)) {
            this.setAllowFlying(doubleJumpEnabled);
        }
        tickParkour();
    }


    public void startNormalParkour() {
        if (inNormalParkour) return;
        resetLock = System.currentTimeMillis() + 1000;
        doubleJumpEnabled = false;
        this.setAllowFlying(false);
        this.setFlying(false);
        this.inNormalParkour = true;
        this.parkourStartTime = System.currentTimeMillis();
        this.checkpointStartTime = System.currentTimeMillis();
        giveParkourItems();
        sendMessage(ChatColor.color("<green>You have started the normal parkour!"));
        playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.PLAYER, 1, 1));
    }

    public void startElytraParkour() {
        if (inElytraParkour) return;
        resetLock = System.currentTimeMillis() + 1000;
        doubleJumpEnabled = false;
        this.setAllowFlying(false);
        this.setFlying(false);
        this.inElytraParkour = true;
        this.parkourStartTime = System.currentTimeMillis();
        this.checkpointStartTime = System.currentTimeMillis();
        this.lastCheckpoint = 0;
        giveElytraParkourItems();
        sendMessage(ChatColor.color("<green>You have started the elytra parkour!"));
        playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.PLAYER, 1, 1));
    }

    public void resetCurrentParkour() {
        if (!inNormalParkour && !inElytraParkour) return;
        if (System.currentTimeMillis() < resetLock) return;
        doubleJumpEnabled = false;
        this.setAllowFlying(false);
        this.setFlying(false);
        this.parkourStartTime = System.currentTimeMillis();
        this.checkpointStartTime = System.currentTimeMillis();
        this.lastCheckpoint = 0;
        sendMessage(ChatColor.color("<green>You have reset the parkour!"));
        playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.PLAYER, 1, 1));
        if (inNormalParkour) {
            giveParkourItems();
            teleport(new Pos(Hub.getParkourManager().normalParkourResetPoint()));
        }
        else if (inElytraParkour) {
            giveElytraParkourItems();
            teleport(new Pos(Hub.getParkourManager().elytraParkourResetPoint()));
        }
        resetLock = System.currentTimeMillis() + 1000;
    }

    public void teleportToLastCheckpoint() {
        if (!inNormalParkour && !inElytraParkour) return;
        doubleJumpEnabled = false;
        this.setAllowFlying(false);
        this.setFlying(false);
        giveParkourItems();
        sendMessage(ChatColor.color("<green>You have teleported to your last checkpoint!"));
        playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.PLAYER, 1, 1));
        if (inNormalParkour) {
            teleport(new Pos(Hub.getParkourManager().getNormalParkourCheckpoint(lastCheckpoint).add(0, 1,0)));
        }
        else if (inElytraParkour) {
            resetCurrentParkour();
        }
    }

    public void exitCurrentParkour() {
        lastDoubleJumpTime = System.currentTimeMillis();
        doubleJumpEnabled = true;
        Point respawnPoint = getRespawnPoint();
        if (inNormalParkour) {
            respawnPoint = Hub.getParkourManager().normalParkourResetPoint();
            inNormalParkour = false;
        }
        else if (inElytraParkour) {
            inElytraParkour = false;
            respawnPoint = Hub.getParkourManager().elytraParkourResetPoint();
        }
        returnHubItems();
        parkourStartTime = 0L;
        lastCheckpoint = 0;
        teleport(new Pos(respawnPoint));
    }

    public void doubleJump() {
        if (!doubleJumpEnabled) return;
        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
            YamlDocument config = Hub.getConfig();
            if (!config.getBoolean("double-jump.enabled", true)) return;
            this.setFlying(false);
            this.setAllowFlying(false);

            int multiplier = config.getInt("double-jump.multiplier", 200);
            int y = config.getInt("double-jump.velocity-y", 20);
            this.setVelocity(this.getPosition().direction().mul(multiplier).withY(y));
            this.lastDoubleJumpTime = System.currentTimeMillis();

            if (config.getBoolean("double-jump.particles.enabled")) {
                int amount = config.getInt("double-jump.particles.amount", 10);
                Particle particle = Particle.fromNamespaceId(config.getString("double-jump.particles.type", "happy_villager"));
                if (particle == null) particle = Particle.HAPPY_VILLAGER;

                final ParticlePacket packet = ParticleCreator.createParticlePacket(
                        particle, this.getPosition().x(), this.getPosition().y(), this.getPosition().z(), 0, 0,0, amount
                );

                if (config.getBoolean("double-jump.particles.show-to-others")) {
                    this.sendPacketToViewersAndSelf(packet);
                }
                else {
                    this.sendPacket(packet);
                }
            }
            if (config.getBoolean("double-jump.sound.enabled")) {
                int pitch = config.getInt("double-jump.sound.pitch", 1);
                int volume = config.getInt("double-jump.sound.volume", 1);
                SoundEvent sound = SoundEvent.fromNamespaceId(config.getString("double-jump.sound.type", "entity.player.levelup"));
                if (sound == null) sound = SoundEvent.ENTITY_PLAYER_LEVELUP;

                this.playSound(Sound.sound(
                        sound,
                        Sound.Source.PLAYER,
                        volume,
                        pitch
                ));
            }
        });
    }


    public void togglePlayerVisibility() {
        updateViewerRule(entity -> {
            if (entity.getEntityId() == Hub.getWumpusEntityId()) return true;
            if (entity.getEntityType() == EntityType.ARMOR_STAND) return true;
            return hidPlayers;
        });
        if (hidPlayers) {
            this.hidPlayers = false;
            sendMessage(ChatColor.color("<green>Players are now visible!"));
            inventory.setItemStack(8, HIDE_PLAYERS_ITEM);
        }
        else {
            this.hidPlayers = true;
            sendMessage(ChatColor.color("<red>Players are now hidden!"));
            inventory.setItemStack(8, SHOW_PLAYERS_ITEM);
        }
    }

    public void sendToServer(String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        sendPluginMessage("BungeeCord", out.toByteArray());
    }

    public PlayerData getData() {
        return data;
    }

    private void giveParkourItems() {
        inventory.clear();
        inventory.setItemStack(3, RESET_PARKOUR_ITEM);
        inventory.setItemStack(5, EXIT_PARKOUR_ITEM);
        inventory.setItemStack(7, LAST_CHECKPOINT_ITEM);
    }

    private void giveElytraParkourItems() {
        inventory.clear();
        setChestplate(ItemStack.of(Material.ELYTRA));
        inventory.setItemStack(8, RESET_PARKOUR_ITEM);
        inventory.setItemStack(5, EXIT_PARKOUR_ITEM);
    }

    private void returnHubItems() {
        inventory.clear();
        inventory.setItemStack(0, Hub.getServerSelectorItem());
        inventory.setItemStack(8, hidPlayers ? SHOW_PLAYERS_ITEM : HIDE_PLAYERS_ITEM);
    }

    private void tickParkour() {
        if (getPosition().sameBlock(Hub.getParkourManager().getNormalParkourStartPoint())) {
            if (inElytraParkour) {
                sendMessage(ChatColor.color("<red>You cannot start a normal parkour while in an elytra parkour!"));
                return;
            }
            if (inNormalParkour) {
                resetCurrentParkour();
                return;
            }
            startNormalParkour();
        }
        if (getPosition().sameBlock(Hub.getParkourManager().getElytraParkourStartPoint())) {
            if (inNormalParkour) {
                sendMessage(ChatColor.color("<red>You cannot start an elytra parkour while in a normal parkour!"));
                return;
            }
            if (inElytraParkour) {
                resetCurrentParkour();
                return;
            }
            startElytraParkour();
        }
        if (!inNormalParkour && !inElytraParkour) return;
        long timeElapsed = System.currentTimeMillis() - parkourStartTime;

        long minutes = timeElapsed / 1000 / 60;
        long seconds = timeElapsed / 1000 % 60;
        long milliseconds = timeElapsed % 1000;
        sendActionBar(ChatColor.color("<green>Time Elapsed: <white>" + minutes + ":" + seconds + ":" + milliseconds));
        checkNormalParkour();
        checkElytraParkour();
    }

    private void checkElytraParkour() {
        if (!inElytraParkour) return;
        playElytraParkourCheckpointParticles();
        int yLevel  = Hub.getParkourManager().getElytraParkourDeathY();
        if (getPosition().y() <= yLevel) {
            resetCurrentParkour();
            sendMessage(ChatColor.color("<red>You fell too far!"));
            return;
        }

        long timeElapsed = System.currentTimeMillis() - parkourStartTime;
        long minutes = timeElapsed / 1000 / 60;
        long seconds = timeElapsed / 1000 % 60;
        long milliseconds = timeElapsed % 1000;

        if (!particleLock) {
            sendPacketToViewersAndSelf(
                    ParticleCreator.createParticlePacket(
                            Particle.FIREWORK,
                            getPosition().x(),
                            getPosition().y(),
                            getPosition().z(),
                            0, 0, 0, 100
                    )
            );
        }

        if (getPosition().sameBlock(Hub.getParkourManager().getElytraParkourEndPoint())) {
            int total = Hub.getParkourManager().getElytraParkourCheckpointCount();
            if (lastCheckpoint != total) {
                sendMessage(ChatColor.color("<red>You did not reach the end of the parkour!"));
                return;
            }
            StringBuilder builder = new StringBuilder();
            builder.append("<green>You finished the parkour in <white>").append(minutes).append(":").append(seconds).append(":").append(milliseconds);
            if (data.getBestElytraParkourTime() == 0 || data.getBestElytraParkourTime() > timeElapsed) {
                data.setBestParkourTime(timeElapsed);
                builder.append("<gold><bold> (New Record!)");
            }
            else {
                long bestMinutes = data.getBestElytraParkourTime() / 1000 / 60;
                long bestSeconds = data.getBestElytraParkourTime() / 1000 % 60;
                long bestMilliseconds = data.getBestElytraParkourTime() % 1000;
                builder.append("<gold><bold> (Best: ").append(bestMinutes).append(":").append(bestSeconds).append(":").append(bestMilliseconds).append(")");
            }
            playSound(Sound.sound(
                    SoundEvent.ENTITY_PLAYER_LEVELUP,
                    Sound.Source.PLAYER,
                    1,
                    1
            ));
            exitCurrentParkour();
            sendMessage(ChatColor.color(builder.toString()));
            return;
        }
        if (Hub.getParkourManager().getElytraParkourCheckpoint(lastCheckpoint + 1).contains(getPosition())) {
            StringBuilder builder = new StringBuilder();
            builder.append("<green>Checkpoint reached in <white>").append(minutes).append(":").append(seconds).append(":").append(milliseconds);
            if (data.getElytraParkourCheckPoint(lastCheckpoint + 1) == -1L ||(System.currentTimeMillis() - checkpointStartTime < data.getElytraParkourCheckPoint(lastCheckpoint + 1))) {
                builder.append("<gold><bold> (New Record! Previous: ");
                if (data.getElytraParkourCheckPoint(lastCheckpoint + 1) == -1L) {
                    builder.append("<red>N/A");
                }
                else {
                    long bestMinutes = data.getElytraParkourCheckPoint(lastCheckpoint + 1) / 1000 / 60;
                    long bestSeconds = data.getElytraParkourCheckPoint(lastCheckpoint + 1) / 1000 % 60;
                    long bestMilliseconds = data.getElytraParkourCheckPoint(lastCheckpoint + 1) % 1000;
                    builder.append(bestMinutes).append(":").append(bestSeconds).append(":").append(bestMilliseconds);
                }
                data.setElytraParkourCheckPoint(lastCheckpoint + 1, System.currentTimeMillis() - checkpointStartTime);
            }
            else {
                long bestMinutes = data.getElytraParkourCheckPoint(lastCheckpoint + 1) / 1000 / 60;
                long bestSeconds = data.getElytraParkourCheckPoint(lastCheckpoint + 1) / 1000 % 60;
                long bestMilliseconds = data.getElytraParkourCheckPoint(lastCheckpoint + 1) % 1000;
                builder.append("<gold><bold> (Best: ").append(bestMinutes).append(":").append(bestSeconds).append(":").append(bestMilliseconds);
            }
            checkpointStartTime = System.currentTimeMillis();
            lastCheckpoint++;
            unlockParticles();
            double boostMultiplier = Hub.getParkourManager().getElytraParkourBoost();
            setVelocity(position.direction().mul(boostMultiplier));
            playSound(Sound.sound(
                    SoundEvent.ENTITY_FIREWORK_ROCKET_LAUNCH,
                    Sound.Source.PLAYER,
                    1,
                    3
            ));
            sendMessage(ChatColor.color(builder.toString()));
        }
    }

    private void unlockParticles() {
        particleLock = false;
        MinecraftServer.getSchedulerManager().scheduleTask(() -> particleLock = true, TaskSchedule.millis(1000), TaskSchedule.stop());
    }

    private void checkNormalParkour() {
        if (!inNormalParkour) return;
        long timeElapsed = System.currentTimeMillis() - parkourStartTime;
        long minutes = timeElapsed / 1000 / 60;
        long seconds = timeElapsed / 1000 % 60;
        long milliseconds = timeElapsed % 1000;
        if (getPosition().sameBlock(Hub.getParkourManager().getNormalParkourEndPoint())) {
            int total = Hub.getParkourManager().getNormalParkourCheckpointCount();
            if (lastCheckpoint != total) {
                sendMessage(ChatColor.color("<red>You did not reach the end of the parkour!"));
                return;
            }
            StringBuilder builder = new StringBuilder();
            builder.append("<green>You finished the parkour in <white>").append(minutes).append(":").append(seconds).append(":").append(milliseconds);
            if (data.getBestParkourTime() == 0 || data.getBestParkourTime() > timeElapsed) {
                data.setBestParkourTime(timeElapsed);
                builder.append("<gold><bold> (New Record!)");
            }
            else {
                long bestMinutes = data.getBestParkourTime() / 1000 / 60;
                long bestSeconds = data.getBestParkourTime() / 1000 % 60;
                long bestMilliseconds = data.getBestParkourTime() % 1000;
                builder.append("<gold><bold> (Best: ").append(bestMinutes).append(":").append(bestSeconds).append(":").append(bestMilliseconds).append(")");
            }
            playSound(Sound.sound(
                    SoundEvent.ENTITY_PLAYER_LEVELUP,
                    Sound.Source.PLAYER,
                    1,
                    1
            ));
            exitCurrentParkour();
            sendMessage(ChatColor.color(builder.toString()));
            return;
        }
        if (this.getPosition().sameBlock(Hub.getParkourManager().getNormalParkourCheckpoint(lastCheckpoint + 1))) {
            StringBuilder builder = new StringBuilder();
            builder.append("<green>Checkpoint reached in <white>").append(minutes).append(":").append(seconds).append(":").append(milliseconds);
            if (data.getParkourCheckPoint(lastCheckpoint + 1) == -1L ||(System.currentTimeMillis() - checkpointStartTime < data.getParkourCheckPoint(lastCheckpoint + 1))) {
                builder.append("<gold><bold> (New Record! Previous: ");
                if (data.getParkourCheckPoint(lastCheckpoint + 1) == -1L) {
                    builder.append("<red>N/A");
                }
                else {
                    long bestMinutes = data.getParkourCheckPoint(lastCheckpoint + 1) / 1000 / 60;
                    long bestSeconds = data.getParkourCheckPoint(lastCheckpoint + 1) / 1000 % 60;
                    long bestMilliseconds = data.getParkourCheckPoint(lastCheckpoint + 1) % 1000;
                    builder.append(bestMinutes).append(":").append(bestSeconds).append(":").append(bestMilliseconds);
                }
                data.setParkourCheckPoint(lastCheckpoint + 1, System.currentTimeMillis() - checkpointStartTime);
            }
            else {
                long bestMinutes = data.getParkourCheckPoint(lastCheckpoint + 1) / 1000 / 60;
                long bestSeconds = data.getParkourCheckPoint(lastCheckpoint + 1) / 1000 % 60;
                long bestMilliseconds = data.getParkourCheckPoint(lastCheckpoint + 1) % 1000;
                builder.append("<gold><bold> (Best: ").append(bestMinutes).append(":").append(bestSeconds).append(":").append(bestMilliseconds);
            }
            checkpointStartTime = System.currentTimeMillis();
            lastCheckpoint++;
            playSound(Sound.sound(
                    SoundEvent.ENTITY_PLAYER_LEVELUP,
                    Sound.Source.PLAYER,
                    1,
                    3
            ));
            sendMessage(ChatColor.color(builder.toString()));
        }
    }

    private void playElytraParkourCheckpointParticles() {
        Cuboid square = Hub.getParkourManager().getElytraParkourCheckpoint(lastCheckpoint + 1);
        Point center = square.center();
        sendPacket(
                ParticleCreator.createParticlePacket(
                        Particle.DRIPPING_LAVA,
                        center.x(), center.y(), center.z(),
                        0, 0, 0, 100
                )
        );
    }

}
