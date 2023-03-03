package net.polar.parkour;


import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.polar.Hub;
import net.polar.utils.Cuboid;
import net.polar.utils.MultiLineHologram;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ParkourManager {

    private static final Pattern NUMBERS = Pattern.compile("[0-9]+"); // Used to check if a string is a number.

    private final Map<Integer, Point> normalParkourCheckPoints = new HashMap<>();
    private final Point normalParkourStartPoint;
    private final Point normalParkourEndPoint;
    private final Point normalParkourResetPoint;

    private final Map<Integer, Cuboid> elytraParkourCheckPoints = new HashMap<>();
    private final Point elytraParkourStartPoint;
    private final Point elytraParkourEndPoint;
    private final double elytraParkourBoost;
    private final int elytraParkourDeathY;
    private final Point elytraParkourResetPoint;


    public ParkourManager(File localDir, InstanceContainer instance) {

        File normalFile = new File(localDir, "parkour.yml");
        File elytraFile = new File(localDir, "elytra-parkour.yml");
        YamlDocument normal = null;
        YamlDocument elytra = null;
        try {
            if (!normalFile.exists()) {
                final InputStream stream = Hub.class.getClassLoader().getResourceAsStream("parkour.yml");
                if (stream == null) {
                    throw new NullPointerException("Could not find parkour.yml in resources!");
                }
                Files.copy(stream, normalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                stream.close();
            }
            if (!elytraFile.exists()) {
                final InputStream stream = Hub.class.getClassLoader().getResourceAsStream("elytra-parkour.yml");
                if (stream == null) {
                    throw new NullPointerException("Could not find elytra-parkour.yml in resources!");
                }
                Files.copy(stream, elytraFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                stream.close();
            }
            normal = YamlDocument.create(normalFile);
            elytra = YamlDocument.create(elytraFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (normal == null) throw new NullPointerException("Could not load normal parkour file!");
        if (elytra == null) throw new NullPointerException("Could not load elytra parkour file!");

        // Normal Parkour
        {
            Section start = normal.getSection("start");
            Pos startPoint = new Pos(
                    start.getDouble("x", 0.0),
                    start.getDouble("y", 0.0),
                    start.getDouble("z", 0.0)
            );
            Block block = Block.fromNamespaceId(start.getString("block", "minecraft:stone"));
            if (block == null) block = Block.STONE;
            this.normalParkourStartPoint = startPoint;
            new MultiLineHologram(start.getStringList("holograms")).create(startPoint.add(0, 2, 0), instance);
            instance.setBlock(startPoint, block);


            Section end = normal.getSection("end");
            Pos endPoint = new Pos(
                    end.getDouble("x", 0.0),
                    end.getDouble("y", 0.0),
                    end.getDouble("z", 0.0)
            );
            block = Block.fromNamespaceId(end.getString("block", "minecraft:stone"));
            if (block == null) block = Block.STONE;
            this.normalParkourEndPoint = endPoint;
            new MultiLineHologram(end.getStringList("holograms")).create(endPoint.add(0, 2, 0), instance);
            instance.setBlock(endPoint, block);

            Section reset = normal.getSection("reset");
            normalParkourResetPoint = new Vec(
                    reset.getDouble("x", 0.0),
                    reset.getDouble("y", 0.0),
                    reset.getDouble("z", 0.0)
            );

            Section checkpoints = normal.getSection("checkpoints");
            Block checkpointBlock = Block.fromNamespaceId(checkpoints.getString("block", "minecraft:stone"));
            if (checkpointBlock == null) checkpointBlock = Block.STONE;
            Block finalCheckpointBlock = checkpointBlock;
            checkpoints.getKeys().stream().filter(key -> NUMBERS.matcher(key.toString()).matches()).forEach(key -> {
                Section checkpoint = checkpoints.getSection(key.toString());
                Pos checkpointPos = new Pos(
                        checkpoint.getDouble("x", 0.0),
                        checkpoint.getDouble("y", 0.0),
                        checkpoint.getDouble("z", 0.0)
                );
                this.normalParkourCheckPoints.put(Integer.parseInt(key.toString()), checkpointPos);
                new MultiLineHologram(checkpoint.getStringList("holograms")).create(checkpointPos.add(0, 2, 0), instance);
                instance.setBlock(checkpointPos, finalCheckpointBlock);
            });
        }

        // Elytra
        {
            Section start = elytra.getSection("start");
            Pos startPoint = new Pos(
                    start.getDouble("x", 0.0),
                    start.getDouble("y", 0.0),
                    start.getDouble("z", 0.0)
            );
            Block block = Block.fromNamespaceId(start.getString("block", "minecraft:stone"));
            if (block == null) block = Block.STONE;
            this.elytraParkourStartPoint = startPoint;
            new MultiLineHologram(start.getStringList("holograms")).create(startPoint.add(0, 2, 0), instance);
            instance.setBlock(startPoint, block);

            Section end = elytra.getSection("end");
            Pos endPoint = new Pos(
                    end.getDouble("x", 0.0),
                    end.getDouble("y", 0.0),
                    end.getDouble("z", 0.0)
            );
            block = Block.fromNamespaceId(end.getString("block", "minecraft:stone"));
            if (block == null) block = Block.STONE;
            this.elytraParkourEndPoint = endPoint;
            new MultiLineHologram(end.getStringList("holograms")).create(endPoint.add(0, 2, 0), instance);
            instance.setBlock(endPoint, block);

            Section reset = elytra.getSection("reset");
            elytraParkourResetPoint = new Vec(
                    reset.getDouble("x", 0.0),
                    reset.getDouble("y", 0.0),
                    reset.getDouble("z", 0.0)
            );

            Section checkpoints = elytra.getSection("checkpoints");

            this.elytraParkourBoost = checkpoints.getDouble("boost", 1.5);
            this.elytraParkourDeathY = checkpoints.getInt("death-y", 0);
            final int areaSize = checkpoints.getInt("area-size", 2);
            checkpoints.getKeys().stream().filter(key -> NUMBERS.matcher(key.toString()).matches()).forEach(key -> {
                Section checkpoint = checkpoints.getSection(key.toString());
                int x = checkpoint.getInt("x", 0);
                int y = checkpoint.getInt("y", 0);
                int z = checkpoint.getInt("z", 0);

                Vec center = new Vec(x, y, z);
                Vec topLeft = center.withY(y + areaSize).withX(x - areaSize).withZ(z - areaSize);
                Vec bottomRight = center.withY(y - areaSize).withX(x + areaSize).withZ(z + areaSize);
                Vec topRight = center.withY(y + areaSize).withX(x + areaSize).withZ(z - areaSize);
                Vec bottomLeft = center.withY(y - areaSize).withX(x - areaSize).withZ(z + areaSize);

                Cuboid square = new Cuboid(topLeft, bottomRight, bottomLeft, topRight);
                this.elytraParkourCheckPoints.put(Integer.parseInt(key.toString()), square);
            });
        }
    }

    public Point getNormalParkourStartPoint() {
        return normalParkourStartPoint;
    }

    public Point getNormalParkourEndPoint() {
        return normalParkourEndPoint;
    }

    public Point getNormalParkourCheckpoint(int checkpoint) {return normalParkourCheckPoints.get(checkpoint) == null ? normalParkourEndPoint : normalParkourCheckPoints.get(checkpoint);}
    public int getNormalParkourCheckpointCount() {
        return normalParkourCheckPoints.size();
    }

    public Point getElytraParkourStartPoint() {
        return elytraParkourStartPoint;
    }

    public Point getElytraParkourEndPoint() {
        return elytraParkourEndPoint;
    }

    public Cuboid getElytraParkourCheckpoint(int checkpoint) {return elytraParkourCheckPoints.get(checkpoint) == null ? new Cuboid(elytraParkourEndPoint, elytraParkourEndPoint) : elytraParkourCheckPoints.get(checkpoint);}

    public int getElytraParkourCheckpointCount() {
        return elytraParkourCheckPoints.size();
    }

    public double getElytraParkourBoost() {
        return elytraParkourBoost;
    }

    public int getElytraParkourDeathY() {
        return elytraParkourDeathY;
    }

    public Point normalParkourResetPoint() {
        return normalParkourResetPoint;
    }

    public Point elytraParkourResetPoint() {
        return elytraParkourResetPoint;
    }
}
