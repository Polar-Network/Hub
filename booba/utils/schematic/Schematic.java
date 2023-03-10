package net.polar.utils.schematic;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.batch.BatchOption;
import net.minestom.server.instance.batch.ChunkBatch;
import net.minestom.server.instance.batch.RelativeBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Represents a schematic file which can be manipulated in the world.
 */
public record Schematic(
        Point size,
        Point offset,
        Block[] palette,
        byte[] blocks
) {


    public Schematic {
        palette = Arrays.copyOf(palette, palette.length);
        blocks = Arrays.copyOf(blocks, blocks.length);
    }

    /**
     * @return a copy of the palette array
     */
    @Override
    public Block[] palette() {
        return Arrays.copyOf(palette, palette.length);
    }

    /**
     * @return a copy of the blocks array
     */
    @Override
    public byte[] blocks() {
        return Arrays.copyOf(blocks, blocks.length);
    }

    /**
     *
     * @param rotation The rotation to apply.
     * @return The size of the schematic after rotation.
     */
    public Point size(SchematicRotation rotation) {
        final Point rotatedPos = rotatePos(size, rotation);

        // get abs vec (no negative size)
        return new Vec(Math.abs(rotatedPos.blockX()), Math.abs(rotatedPos.blockY()), Math.abs(rotatedPos.blockZ()));
    }

    /**
     * Rotates the offset position by the given rotation.
     *
     * @param rotation The rotation to apply.
     * @return The rotated position.
     */
    public Point offset(SchematicRotation rotation) {
        return rotatePos(offset, rotation);
    }


    /**
     * Applies the schematic to an instance at the given start point.
     * @param instance The instance to apply the schematic to.
     * @param startPoint The start point to apply the schematic at.
     * @param loadChunks Whether to load chunks before applying the schematic.
     * @param rotation The rotation to apply to the schematic.
     * @param blockModifier A function to modify each block before it is placed.
     * @return  A {@link RelativeBlockBatch} which represents the schematic file at its offset.
     */
    public @NotNull RelativeBlockBatch applyToInstance(@NotNull Instance instance, @NotNull Point startPoint, boolean loadChunks, @NotNull SchematicRotation rotation, @Nullable Function<Block, Block> blockModifier) {
        RelativeBlockBatch batch = build(rotation, blockModifier);
        if (loadChunks) {
            Point min = startPoint.add(offset());
            Point max = min.add(size());
            int chunkXStart = min.chunkX();
            int chunkXSize = max.chunkX() - min.chunkX() + 1;
            int chunkZStart = min.chunkZ();
            int chunkZSize = max.chunkZ() - min.chunkZ() + 1;
            ArrayList<CompletableFuture<Chunk>> chunksToLoad = new ArrayList<>();
            for (int i = chunkXStart; i < chunkXStart + chunkXSize; i++) {
                for (int j = chunkZStart; j < chunkZStart + chunkZSize; j++) {
                    chunksToLoad.add(instance.loadOptionalChunk(i, j));
                }
            }
            CompletableFuture.allOf(chunksToLoad.toArray(new CompletableFuture[0])).thenRun(() -> batch.apply(instance, startPoint, null)).thenRun(() -> {
                for (int i = chunkXStart; i < chunkXStart + chunkXSize; i++) {
                    for (int j = chunkZStart; j < chunkZStart + chunkZSize; j++) {
                        Chunk chunk = instance.getChunk(i, j);
                        if(chunk != null && chunk.isLoaded() && chunk.getViewers().isEmpty()) {
                            instance.unloadChunk(chunk);
                        }
                    }
                }
            });
        } else {
            batch.apply(instance, startPoint, null);
        }
        return batch;
    }

    /**
     * Convert the schematic into a {@link RelativeBlockBatch} which can be applied to an instance.
     * The schematic can be rotated around its {@link #offset()} before placement.
     *
     * @param rotation      The rotation to apply to the schematic.
     * @param blockModifier If present, called on each individual block before it is placed.
     * @return A {@link RelativeBlockBatch} which represents the schematic file at its offset.
     */
    public @NotNull RelativeBlockBatch build(@NotNull SchematicRotation rotation, @Nullable Function<Block, Block> blockModifier) {
        RelativeBlockBatch batch = new RelativeBlockBatch(new BatchOption().setCalculateInverse(true));
        apply(rotation, (pos, block) -> batch.setBlock(pos, blockModifier == null ? block : blockModifier.apply(block)));
        return batch;
    }


    /**
     * Fetches the used chunks using reflection. Warning: This is a hacky solution and may break in the future and is very slow
     * @param rotation The rotation to apply to the schematic.
     * @return The used chunks
     */
    public @NotNull long[] getAffectedChunksReflection(@NotNull SchematicRotation rotation, @NotNull Point startPoint) {
        final AbsoluteBlockBatch batch = build(rotation, null).toAbsoluteBatch(startPoint.blockX(), startPoint.blockY(), startPoint.blockZ());
        final Field field;
        long[] chunks = new long[0];
        try {
            field = batch.getClass().getDeclaredField("chunkBatchesMap");
            boolean accessible = field.canAccess(batch);
            field.setAccessible(true);
            Long2ObjectMap<ChunkBatch> chunkBatchesMap = (Long2ObjectMap<ChunkBatch>) field.get(batch);
            field.setAccessible(accessible);
            chunks = chunkBatchesMap.keySet().toLongArray();
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return chunks;
    }


    /**
     * Apply the schematic directly given a rotation. The applicator function will be called for each block in the schematic.
     * <p>
     * Note: The {@link Point} passed to `applicator` is relative to the {@link #offset()}.
     *
     * @param rotation   The rotation to apply before placement.
     * @param applicator The function to call for each block in the schematic.
     */
    public void apply(@NotNull SchematicRotation rotation, @NotNull BiConsumer<Point, Block> applicator) {
        ByteBuffer blocks = ByteBuffer.wrap(this.blocks);
        for (int y = 0; y < size().y(); y++) {
            for (int z = 0; z < size.z(); z++) {
                for (int x = 0; x < size.x(); x++) {
                    int blockVal = Utils.readVarInt(blocks);
                    Block b = palette[blockVal];

                    if (b == null || b.isAir()) {
                        continue;
                    }

                    Vec blockPos = new Vec(x + offset.x(), y + offset.y(), z + offset.z());
                    applicator.accept(rotatePos(blockPos, rotation), rotateBlock(b, rotation));
                }
            }
        }
    }

    private static @NotNull Point rotatePos(@NotNull Point point, @NotNull SchematicRotation rotation) {
        return switch (rotation) {
            case NONE -> point;
            case CLOCKWISE_90 -> new Vec(-point.z(), point.y(), point.x());
            case CLOCKWISE_180 -> new Vec(-point.x(), point.y(), -point.z());
            case CLOCKWISE_270 -> new Vec(point.z(), point.y(), -point.x());
        };
    }

    /**
     * Rotates the block based on the rotation
     * @param block The block to rotate
     * @param rotation The rotation to apply
     * @return The rotated block
     */
    private static @NotNull Block rotateBlock(@NotNull Block block, @NotNull SchematicRotation rotation) {
        if (rotation == SchematicRotation.NONE) return block;

        Block newBlock = block;

        if (block.getProperty("facing") != null) {
            newBlock = rotateFacing(block, rotation);
        }
        if (block.getProperty("north") != null) {
            newBlock = rotateFence(block, rotation);
        }

        return newBlock;
    }


    /**
     * Rotates blocks that have a "facing" property
     */
    private static Block rotateFacing(Block block, SchematicRotation rotation) {
        return switch (rotation) {
            case NONE -> block;
            case CLOCKWISE_90 -> block.withProperty("facing", rotate90(block.getProperty("facing")));
            case CLOCKWISE_180 -> block.withProperty("facing", rotate90(rotate90(block.getProperty("facing"))));
            case CLOCKWISE_270 ->
                    block.withProperty("facing", rotate90(rotate90(rotate90(block.getProperty("facing")))));
        };
    }

    /**
     * Rotates fences, walls and glass panes
     * (blocks that have "north" "east" "south" "west" properties)
     */
    private static Block rotateFence(Block block, SchematicRotation rotation) {
        return switch (rotation) {
            case NONE -> block;
            case CLOCKWISE_90 -> block.withProperties(Map.of(
                    "north", block.getProperty("west"),
                    "east", block.getProperty("north"),
                    "south", block.getProperty("east"),
                    "west", block.getProperty("south")
            ));
            case CLOCKWISE_180 -> block.withProperties(Map.of(
                    "north", block.getProperty("south"),
                    "east", block.getProperty("west"),
                    "south", block.getProperty("north"),
                    "west", block.getProperty("east")
            ));
            case CLOCKWISE_270 -> block.withProperties(Map.of(
                    "north", block.getProperty("east"),
                    "east", block.getProperty("south"),
                    "south", block.getProperty("west"),
                    "west", block.getProperty("north")
            ));
        };
    }

    private static String rotate90(String in) {
        return switch (in) {
            case "north" -> "east";
            case "east" -> "south";
            case "south" -> "west";
            default -> "north";
        };
    }
}